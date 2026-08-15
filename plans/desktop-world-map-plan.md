# Desktop World Map — Native Renderer Plan

> **Status:** Planning document — no code changes.
> **Date:** 2026-08-15
> **Goal:** Replace/extend the current chunk-grid World Map with a real top-down terrain renderer (1 px per block, 16×16 px per chunk) built entirely on the in-repo `world/` parsers. **Not** BlueMap, and no external web-map service or network calls.

---

## 1. Existing building blocks (already in the repo)

All in `composeApp/src/commonMain/kotlin/com/portalhost/world/`:

| File | API | What it gives us |
|------|-----|------------------|
| `RegionFileIndex.kt` | `indexDirectory(regionDir): List<RegionIndex>` | Every `r.<x>.<z>.mca` file → `RegionIndex(regionX, regionZ, chunks: Map<ChunkCoord, ChunkPresence>, fileLastModified)`. `ChunkPresence` has the raw location-table `sectorOffset`/`sectorCount` so we can seek straight to a chunk payload. |
| `ChunkCoord.kt` | `data class ChunkCoord(x, z)` + `REGION_SIZE`/`CHUNKS_PER_REGION` consts | Coordinate math. |
| `NbtParser.kt` | `parse(bytes): NbtTag.NbtCompound?` | Full NBT reader — `NbtIntArray`, `NbtLongArray`, `NbtList`, `NbtCompound`, `NbtByteArray`. Needed to decode block palettes + heightmaps. |
| `ChunkBiomeReader.kt` | `biomeName(chunkData): String?` | One biome name per chunk from an Anvil payload (legacy ≤1.17 `Level.Biomes` and modern 1.18+ `sections[].biomes`). Handles gzip/zlib/raw. Good cheap layer; not per-block. |
| `ChunkDecoder.kt` | `interface ChunkDecoder { decodeBiome(raf, sectorOffset, sectorCount): BiomeId? }` + `NoOpChunkDecoder` | The extension seam for decoding on top of a `RandomAccessFile`. Desktop's `AnvilChunkDecoder` already implements biome decoding here. |

Desktop UI today: `desktop/screens/WorldMapScreen.kt` draws one rect per chunk (`BASE_CELL_PX * zoom`), colored by biome when the toggle is on, plus RCON player markers.

---

## 2. Design decision

Build a **block-level raster renderer**:

- **1 block = 1 pixel.** Each chunk becomes a 16×16 pixel tile.
- **Two render modes** (toolbar toggle, default = Terrain):
  1. **Terrain** — color each block by its top-most visible block (heightmap-based; `minecraft:water` → blue, sand → tan, grass → green, stone → grey, etc.). Produces an actual recognizable map.
  2. **Biome** — current behavior, kept as a fast fallback: one color per chunk via `ChunkBiomeReader`.
- **Tile cache:** decoded 16×16 tiles are stored as `ImageBitmap`s keyed by `packCoord(x, z)` (the same `(x.toLong() shl 32) or z` key already used in `WorldMapScreen`), so pan/zoom/re-composition never re-decodes a chunk.
- **Async + bounded:** decode only visible chunks on `Dispatchers.IO`, LRU the tile cache (e.g. 10 000 tiles), single in-flight decode job per region, cancel on pan.

### Why not BlueMap
BlueMap is a heavyweight third-party renderer (own web server, tiles, memory-mapped region access, multi-GB texture atlases). We already ship the parsers; a 16×16/block raster is ~1 KB per chunk of decoded output and runs in-process with no external process, no HTTP, no install step.

---

## 3. Region/chunk payload plumbing (reuse + small new piece)

1. **Locate the world** — `serverManager.getServerDir(serverId)` then pick `world` / `world_nether` / `world_the_end` (already done in `WorldMapScreen`).
2. **List regions** — `RegionFileIndex().indexDirectory(File(world, "region"))` → bounds via the existing `computeBounds(regions)` logic.
3. **Read one chunk payload** (new helper, e.g. `RegionChunkReader`):
   - From `ChunkPresence.sectorOffset` (in 4 KiB sectors) seek `raf.seek(sectorOffset.toLong() * 4096)`.
   - Read `[4-byte BE length][1-byte compression][NBT]`, cap length at `ChunkBiomeReader.MAX_CHUNK_BYTES` (8 MiB).
   - Decompress gzip(1)/zlib(2)/raw(3) — same code as `ChunkBiomeReader.inflate`/`gunzip`, factor into a shared helper.
4. **Extend `ChunkDecoder`** (new impl `HeightmapChunkDecoder`, sibling of desktop's `AnvilChunkDecoder`) exposing:
   - `decodeBlocks(nbt): IntArray(256)` — surface block IDs per column,
   - `decodeHeightmap(nbt): IntArray(256)` — top block Y per column.

### Parsing details for the terrain layer
- Root compound `Level` (≤1.17) or top-level (1.18+): `sections: List<Compound>` each with `Y` (signed), `block_states.palette` + `block_states.data` (`NbtLongArray`), and `Heightmaps.MOTION_BLOCKING` (`NbtLongArray`, 256 entries packed 9-bit).
- Build `name -> (block, paletteIndex)`; for each section, walk bottom-up; keep the highest non-air block per column (air / `minecraft:air` / `cave_air` / `void_air` are transparent for heightmap purposes). The `MOTION_BLOCKING` heightmap gives the same top Y directly — prefer it, use section scan as fallback.
- Color table (static `BlockColorTable`): default by block name/namespace prefix (water→`#3F76E4`, sand→`#E7D79B`, grass_block→`#7CBD6B`, stone→`#8A8A8A`, snow→`#F4F4F4`, netherrack→`#8B3D37`, end stone→`#CFDBC4`, …); unknown → neutral grey; ungenerated chunk tile → the existing dark grid color.

---

## 4. Rendering / UI changes (desktop only)

Keep the existing `WorldMapScreen` structure (top bar, toolbar, canvas, legend, player markers) and swap the per-chunk rect loop for tile blitting:

- `Canvas.drawImage(tile, dstOffset, dstSize)` with `cellPx = 16f * zoom` for the terrain mode (`drawRect` remains for biome mode).
- Zoom levels adjust: block-level zoom means 16 px/block base tile; existing `ZOOM_LEVELS` of 1.5×–32× map to 24 px → 512 px per chunk tile.
- Tile decode job: `LaunchedEffect(visibleChunkRange, world)` → `withContext(Dispatchers.IO)` decode only in-view chunks → publish to a `mutableStateMapOf<Long, ImageBitmap>`.
- Culling: reuse the existing `visibleMin/MaxChunkX/Z` computation to bound work and skip off-screen tiles.
- Keep the biome toggle; it flips the decoder used (`HeightmapChunkDecoder` vs existing biome path).
- Keep RCON player markers, spawn marker, axis ticks, tap-to-inspect, and world dropdown unchanged.

---

## 5. Performance targets

- **Startup:** region index of a 5 000-chunk world < 300 ms (location-table-only read; already proven).
- **First paint:** visible tiles decode within ~1–2 s on a mid-range desktop.
- **Scrolling:** tile cache means no re-decode for already-visited areas; only new tiles decode (LRU evicts oldest).
- **Memory:** decoded tiles held as 16×16 ARGB `ImageBitmap`s (~1 KB each); 10 000-tile LRU ≈ 10 MB worst case.
- **Never block the UI thread:** all region reads + NBT + decode on `Dispatchers.IO`; `Canvas` only composites ready tiles.

---

## 6. Files to touch (when implementation starts)

| File | Change |
|------|--------|
| `commonMain/.../world/RegionChunkReader.kt` *(new)* | Read + decompress a single chunk payload from `RandomAccessFile` (shared inflate/gunzip pulled out of `ChunkBiomeReader`). |
| `commonMain/.../world/BlockColorTable.kt` *(new)* | Block name → ARGB color map + unknown fallback. |
| `commonMain/.../world/HeightmapChunkDecoder.kt` *(new)* | `ChunkDecoder` impl: surface blocks + `MOTION_BLOCKING` heightmap → 16×16 tile colors. |
| `commonMain/.../world/ChunkDecoder.kt` | Optional: widen interface or add a `decodeSurfaceColors(raf, sectorOffset, sectorCount): IntArray?`. |
| `commonMain/.../world/ChunkBiomeReader.kt` | Extract `inflate`/`gunzip` into the shared reader (behavior-neutral). |
| `desktopMain/.../screens/WorldMapScreen.kt` | Tile blitting mode, tile-cache state, IO decode effect, toolbar toggle. |
| Tests | `RegionChunkReaderTest`, `HeightmapChunkDecoderTest`, `BlockColorTableTest` — seed a tiny synthetic `.mca` (or reuse fixture worlds) and assert 16×16 outputs, mirroring the existing `NbtParserTest`/`ChunkBiomeReaderTest` style. |

---

## 7. Out of scope (this iteration)

- Underground / caves / vertical cross-sections.
- Full world > 2 048-chunk radius (culled by bounds + LRU already).
- Entity rendering beyond the existing RCON player markers.
- Per-chunk web tiles / external map integration (i.e. no BlueMap).
