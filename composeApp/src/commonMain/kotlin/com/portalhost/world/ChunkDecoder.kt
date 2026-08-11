package com.portalhost.world

import java.io.RandomAccessFile

data class BiomeId(
    val id: Int,
    val name: String,
    /** 0xAARRGGBB color in the low 32 bits. */
    val color: Long = 0L,
)

interface ChunkDecoder {
    suspend fun decodeBiome(
        raf: RandomAccessFile,
        sectorOffset: Int,
        sectorCount: Int,
    ): BiomeId?
}

class NoOpChunkDecoder : ChunkDecoder {
    override suspend fun decodeBiome(raf: RandomAccessFile, sectorOffset: Int, sectorCount: Int): BiomeId? = null
}
