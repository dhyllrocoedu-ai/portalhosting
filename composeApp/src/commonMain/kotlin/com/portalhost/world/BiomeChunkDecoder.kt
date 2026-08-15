package com.portalhost.world

import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Decodes per-column biome surface colours for an Anvil chunk by reading the
 * per-section `sections[i].biomes` paletted container.
 *
 * Biome colours come from [BiomePalette] (desktop module) resolved by registry
 * name. Reg/modded names fall through to a neutral grey.
 *
 * Only chunks that have at least one section with a valid biome palette resolve.
 * Columns whose section is unparseable or has no palette entry come back as
 * [UNRESOLVED].
 */
class BiomeChunkDecoder(private val biomePalette: BiomePalette = BiomePalette) {

    private val cache = ConcurrentHashMap<String, LongArray?>()

    fun clearCache(full: Boolean = true) {
        if (!full) return
        cache.clear()
    }

    fun decodeRegion(file: File, region: RegionIndex): List<Pair<ChunkCoord, LongArray>> {
        val cachePrefix = "${file.absolutePath}:${file.lastModified()}"
        val result = mutableListOf<Pair<ChunkCoord, LongArray>>()

        // Sequential per-region (the RAF handle owns the file); individual
        // chunk reads within a region are at independent sector offsets.
        RandomAccessFile(file, "r").use { raf ->
            region.chunks.forEach { (coord, presence) ->
                if (!presence.generated) return@forEach
                val key = "$cachePrefix:${coord.x}:${coord.z}"
                val cached = cache[key]
                val colors = cached ?: decodeFromRaf(raf, presence.sectorOffset, presence.sectorCount)
                colors?.let {
                    if (cached == null) cache[key] = it
                    result += coord to it
                }
            }
        }
        return result
    }

    private fun decodeFromRaf(raf: RandomAccessFile, sectorOffset: Int, sectorCount: Int): LongArray? {
        val payload = RegionChunkReader.readPayload(raf, sectorOffset, sectorCount) ?: return null
        return surfaceColors(payload)
    }

    fun surfaceColors(chunkData: ByteArray): LongArray? {
        val root = RegionChunkReader.parseChunk(chunkData) ?: return null
        return surfaceColors(root)
    }

    fun surfaceColors(root: NbtTag.NbtCompound): LongArray? {
        val level = root.children["Level"] as? NbtTag.NbtCompound
        val sectionsTag = (level?.children?.get("sections") ?: root.children["sections"]) as? NbtTag.NbtList
        val sections = sectionsTag?.tags.orEmpty().mapNotNull { it as? NbtTag.NbtCompound }
        if (sections.isEmpty()) return null

        val colors = LongArray(256) { UNRESOLVED }

        outer@ for (section in sections) {
            val biomesTag = (section.children["biomes"] as? NbtTag.NbtCompound ?: continue@outer)
            val palette = (biomesTag.children["palette"] as? NbtTag.NbtList)?.tags.orEmpty()
                .mapNotNull { (it as? NbtTag.NbtCompound)?.children?.get("Name") as? NbtTag.NbtString }
            if (palette.isEmpty()) continue@outer

            val data: LongArray?
            val bits: Int
            if (biomesTag.children.containsKey("data")) {
                val raw = (biomesTag.children["data"] as? NbtTag.NbtLongArray)?.value ?: continue@outer
                data = raw
                bits = if (palette.size <= 1) 0 else maxOf(4, 32 - Integer.numberOfLeadingZeros(palette.size - 1))
            } else {
                data = null
                bits = 0 // uniform-single-biome shortcut
            }

            for (pos in 0 until 64) {
                val bx = pos and 3
                val by = (pos shr 2) and 3
                val bz = (pos shr 4) and 3
                // Map 4×4×4 section-local grid index to the full chunk's
                // column index: Y grows upwards, Z south→north in storage:
                // localY 0 = lowest section quarter → column's nybble 0;
                // z-order within the tile reversed so north (higher cz) maps first.
                val i = ((by shl 2) or (3 - bz)) shl 4 or bx
                if (i !in 0 until 256) continue

                val biomeName = if (data == null || bits == 0) {
                    palette[0].value
                } else {
                    val longIndex = (pos * bits) shr 6
                    if (longIndex >= data.size) palette[0].value else {
                        val bitIndex = (pos * bits) and 63
                        var idx: Long = (data[longIndex] ushr bitIndex)
                        if (bitIndex + bits > 64 && longIndex + 1 < data.size) {
                            idx = idx or (data[longIndex + 1] shl (64 - bitIndex))
                        }
                        val packed = (idx and ((1L shl bits) - 1)).toInt()
                        palette.getOrNull(packed)?.value ?: palette[0].value
                    }
                }

                // biomePalette.argbFor returns signed Int ARGB; store unsigned long.
                colors[i] = biomePalette.argbFor(biomeName).toULong().toLong()
            }
        }
        return colors
    }
}