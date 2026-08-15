package com.portalhost.desktop.world

import com.portalhost.world.BiomeId
import com.portalhost.world.BiomePalette
import com.portalhost.world.ChunkBiomeReader
import com.portalhost.world.ChunkCoord
import com.portalhost.world.ChunkDecoder
import com.portalhost.world.RegionChunkReader
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

    private fun readPayload(raf: RandomAccessFile, sectorOffset: Int, sectorCount: Int): ByteArray? =
        RegionChunkReader.readPayload(raf, sectorOffset, sectorCount)
}
