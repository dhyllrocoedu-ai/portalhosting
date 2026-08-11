package com.portalhost.desktop.world

import com.portalhost.world.BiomeId
import com.portalhost.world.ChunkBiomeReader
import com.portalhost.world.ChunkCoord
import com.portalhost.world.ChunkDecoder
import com.portalhost.world.RegionIndex
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

class AnvilChunkDecoder : ChunkDecoder {

    private data class CachedBiome(val name: String, val color: Long)

    private val cache = ConcurrentHashMap<String, CachedBiome?>()

    override suspend fun decodeBiome(
        raf: RandomAccessFile,
        sectorOffset: Int,
        sectorCount: Int,
    ): BiomeId? {
        val payload = readPayload(raf, sectorOffset, sectorCount) ?: return null
        val name = ChunkBiomeReader.biomeName(payload) ?: return null
        return BiomeId(id = 0, name = name, color = BiomePalette.argbFor(name))
    }

    /**
     * Decodes biomes for every generated chunk in a region, caching by
     * (file path, lastModified, chunk coord). Returns decoded chunks.
     */
    fun decodeRegion(file: File, region: RegionIndex): List<Pair<ChunkCoord, BiomeId>> {
        if (!file.isFile) return emptyList()
        val cachePrefix = "${file.absolutePath}:${file.lastModified()}"
        val result = mutableListOf<Pair<ChunkCoord, BiomeId>>()
        try {
            RandomAccessFile(file, "r").use { raf ->
                region.chunks.forEach { (coord, presence) ->
                    if (!presence.generated) return@forEach
                    val key = "$cachePrefix:${coord.x}:${coord.z}"
                    val cached = cache[key]
                    val biome = cached ?: decodeFromRaf(raf, presence.sectorOffset, presence.sectorCount)
                    if (biome != null) {
                        if (cached == null) cache[key] = biome
                        result.add(coord to BiomeId(id = 0, name = biome.name, color = biome.color))
                    }
                }
            }
        } catch (_: Exception) {
            // Skip unreadable region files; the map falls back to the flat color.
        }
        return result
    }

    fun clearCache() = cache.clear()

    private fun decodeFromRaf(
        raf: RandomAccessFile,
        sectorOffset: Int,
        sectorCount: Int,
    ): CachedBiome? {
        val payload = readPayload(raf, sectorOffset, sectorCount) ?: return null
        val name = ChunkBiomeReader.biomeName(payload) ?: return null
        return CachedBiome(name = name, color = BiomePalette.argbFor(name))
    }

    private fun readPayload(raf: RandomAccessFile, sectorOffset: Int, sectorCount: Int): ByteArray? {
        if (sectorOffset <= 0 || sectorCount <= 0) return null
        val position = sectorOffset.toLong() * SECTOR_BYTES
        if (position < 0 || position + 4 > raf.length()) return null
        raf.seek(position)
        val lengthBuf = ByteArray(4)
        raf.readFully(lengthBuf)
        val length =
            ((lengthBuf[0].toInt() and 0xFF) shl 24) or
                ((lengthBuf[1].toInt() and 0xFF) shl 16) or
                ((lengthBuf[2].toInt() and 0xFF) shl 8) or
                (lengthBuf[3].toInt() and 0xFF)
        if (length <= 0 || length > MAX_CHUNK_LENGTH) return null
        if (position + 4 + length > raf.length()) return null
        val payload = ByteArray(length)
        raf.readFully(payload)
        return payload
    }

    companion object {
        private const val SECTOR_BYTES = 4096
        private const val MAX_CHUNK_LENGTH = 8 * 1024 * 1024
    }
}
