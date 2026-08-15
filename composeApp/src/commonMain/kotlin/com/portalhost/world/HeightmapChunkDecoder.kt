package com.portalhost.world

import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Decodes the top-down surface color (1 color per column, 256 per chunk) of an
 * Anvil chunk from its raw region-file payload.
 *
 * Uses the chunk's `Heightmaps.WORLD_SURFACE` (falls back to `MOTION_BLOCKING`)
 * to find the top block of each column, then resolves that block's name from the
 * per-section `block_states` paletted container and colors it via [BlockColorTable].
 * If no heightmap is present it falls back to a top-down section scan for the
 * first non-air block.
 */
class HeightmapChunkDecoder {

    private val cache = ConcurrentHashMap<String, LongArray?>()

    /**
     * Decodes surface colors for every generated chunk in a region, caching by
     * (file path, lastModified, chunk coord).
     */
    fun decodeRegion(file: File, region: RegionIndex): List<Pair<ChunkCoord, LongArray>> {
        if (!file.isFile) return emptyList()
        val cachePrefix = "${file.absolutePath}:${file.lastModified()}"
        val result = mutableListOf<Pair<ChunkCoord, LongArray>>()
        try {
            RandomAccessFile(file, "r").use { raf ->
                region.chunks.forEach { (coord, presence) ->
                    if (!presence.generated) return@forEach
                    val key = "$cachePrefix:${coord.x}:${coord.z}"
                    val cached = cache[key]
                    val colors = cached ?: decodeFromRaf(raf, presence.sectorOffset, presence.sectorCount)
                    if (colors != null) {
                        if (cached == null) cache[key] = colors
                        result.add(coord to colors)
                    }
                }
            }
        } catch (_: Exception) {
            // Skip unreadable region files; the map falls back to the flat color.
        }
        return result
    }

    fun clearCache() = cache.clear()

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

        val resolvers = HashMap<Int, BlockPaletteContainer>(sections.size)
        sections.forEach { section ->
            val y = (section.children["Y"] as? NbtTag.NbtInt)?.value ?: return@forEach
            val blockStates = section.children["block_states"] as? NbtTag.NbtCompound ?: return@forEach
            resolvers[y] = BlockPaletteContainer.parse(blockStates) ?: return@forEach
        }
        if (resolvers.isEmpty()) return null

        val colors = LongArray(256) { UNRESOLVED }

        val heights = readHeightmap(level, root)
        if (heights != null) {
            for (i in 0 until 256) {
                val worldY = heights[i]
                val resolver = resolvers[worldY shr 4] ?: continue
                val index = ((worldY and 15) shl 8) or (i and 240) or (i and 15)
                val name = resolver.blockName(index) ?: continue
                BlockColorTable.colorFor(name)?.let { colors[i] = it }
            }
            return colors
        }

        // Fallback: top-down scan for the first non-air block in each column.
        val sorted = resolvers.toSortedMap(compareByDescending { it })
        for (i in 0 until 256) {
            val bx = i and 15
            val bz = i shr 4
            column@ for ((_, resolver) in sorted) {
                for (localY in 15 downTo 0) {
                    val index = (localY shl 8) or (bz shl 4) or bx
                    val name = resolver.blockName(index) ?: continue
                    BlockColorTable.colorFor(name)?.let {
                        colors[i] = it
                        break@column
                    }
                }
            }
        }
        return colors
    }

    private fun readHeightmap(level: NbtTag.NbtCompound?, root: NbtTag.NbtCompound): IntArray? {
        val heightmaps = (level?.children?.get("Heightmaps") ?: root.children["Heightmaps"]) as? NbtTag.NbtCompound
            ?: return null
        val packed = (heightmaps.children["WORLD_SURFACE"] as? NbtTag.NbtLongArray)?.value
            ?: (heightmaps.children["MOTION_BLOCKING"] as? NbtTag.NbtLongArray)?.value
            ?: return null
        return unpackHeightmap(packed)
    }

    private fun unpackHeightmap(packed: LongArray): IntArray {
        val bits = (packed.size.toLong() * 64L / 256L).toInt().coerceIn(1, 31)
        val mask = (1L shl bits) - 1L
        val result = IntArray(256)
        var bitOffset = 0
        for (i in 0 until 256) {
            val longIndex = bitOffset shr 6
            val bitIndex = bitOffset and 63
            var value = packed[longIndex] ushr bitIndex
            if (bitIndex + bits > 64 && longIndex + 1 < packed.size) {
                value = value or (packed[longIndex + 1] shl (64 - bitIndex))
            }
            var v = (value and mask).toInt()
            // Heightmaps are 2's-complement in the bit width (negative world Y possible).
            if ((v and (1 shl (bits - 1))) != 0) v -= 1 shl bits
            result[i] = v
            bitOffset += bits
        }
        return result
    }

    /** Resolves block names inside a single section's `block_states` container. */
    private class BlockPaletteContainer(
        private val names: List<String?>,
        private val data: LongArray?,
        private val bits: Int,
    ) {
        fun blockName(index: Int): String? {
            if (names.size == 1) return names[0]
            if (data == null || bits <= 0) return null
            val value = unpackIndex(data, bits, index) ?: return null
            return names.getOrNull(value)
        }

        companion object {
            fun parse(container: NbtTag.NbtCompound): BlockPaletteContainer? {
                val palette = (container.children["palette"] as? NbtTag.NbtList)?.tags.orEmpty()
                    .mapNotNull { it as? NbtTag.NbtCompound }
                if (palette.isEmpty()) return null
                val names = palette.map { (it.children["Name"] as? NbtTag.NbtString)?.value }
                val data = (container.children["data"] as? NbtTag.NbtLongArray)?.value
                val bits = if (palette.size <= 1) {
                    0
                } else {
                    maxOf(4, 32 - Integer.numberOfLeadingZeros(palette.size - 1))
                }
                return BlockPaletteContainer(names, data, bits)
            }

            private fun unpackIndex(data: LongArray, bits: Int, index: Int): Int? {
                val bitOffset = index * bits
                val longIndex = bitOffset shr 6
                if (longIndex >= data.size) return null
                val bitIndex = bitOffset and 63
                val mask = (1L shl bits) - 1L
                var value = data[longIndex] ushr bitIndex
                if (bitIndex + bits > 64 && longIndex + 1 < data.size) {
                    value = value or (data[longIndex + 1] shl (64 - bitIndex))
                }
                return (value and mask).toInt()
            }
        }
    }

}

/** Sentinel for an unresolved (transparent) column in a surface-color array. */
const val UNRESOLVED = -1L
