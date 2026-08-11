package com.portalhost.world

import java.io.File
import java.io.RandomAccessFile

data class ChunkPresence(
    val generated: Boolean,
    val sectorOffset: Int,
    val sectorCount: Int,
)

data class RegionIndex(
    val regionX: Int,
    val regionZ: Int,
    val chunks: Map<ChunkCoord, ChunkPresence>,
    val fileLastModified: Long,
)

class RegionFileIndex {

    fun indexDirectory(regionDir: File): List<RegionIndex> {
        if (!regionDir.isDirectory) return emptyList()
        return regionDir.listFiles()
            ?.filter { REGION_FILE.matchEntire(it.name) != null }
            ?.mapNotNull { file ->
                val match = REGION_FILE.matchEntire(file.name) ?: return@mapNotNull null
                val rx = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val rz = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                indexFile(file, rx, rz)
            }
            ?.toList()
            ?: emptyList()
    }

    fun indexFile(file: File, regionX: Int, regionZ: Int): RegionIndex? {
        if (!file.isFile) return null
        val chunks = mutableMapOf<ChunkCoord, ChunkPresence>()
        try {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() < LOCATION_TABLE_BYTES) {
                    return RegionIndex(regionX, regionZ, chunks, file.lastModified())
                }
                val buf = ByteArray(LOCATION_TABLE_BYTES)
                raf.readFully(buf)
                for (i in 0 until ChunkCoord.CHUNKS_PER_REGION) {
                    val base = i * 4
                    val b0 = buf[base].toInt() and 0xFF
                    val b1 = buf[base + 1].toInt() and 0xFF
                    val b2 = buf[base + 2].toInt() and 0xFF
                    val b3 = buf[base + 3].toInt() and 0xFF
                    val offset = (b0 shl 16) or (b1 shl 8) or b2
                    val sectorCount = b3
                    val generated = offset != 0 && (offset and ABSENT_BIT_MASK) == 0
                    val chunkX = regionX * ChunkCoord.REGION_SIZE + (i % ChunkCoord.REGION_SIZE)
                    val chunkZ = regionZ * ChunkCoord.REGION_SIZE + (i / ChunkCoord.REGION_SIZE)
                    chunks[ChunkCoord(chunkX, chunkZ)] = ChunkPresence(
                        generated = generated,
                        sectorOffset = offset,
                        sectorCount = sectorCount,
                    )
                }
            }
        } catch (_: Exception) {
            return RegionIndex(regionX, regionZ, emptyMap(), file.lastModified())
        }
        return RegionIndex(regionX, regionZ, chunks, file.lastModified())
    }

    companion object {
        private val REGION_FILE = Regex("""r\.(-?\d+)\.(-?\d+)\.mca""")
        private const val LOCATION_TABLE_BYTES = 1024 * 4
        private const val ABSENT_BIT_MASK = 0x800000.toInt()
    }
}
