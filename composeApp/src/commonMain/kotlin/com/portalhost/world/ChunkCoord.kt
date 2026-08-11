package com.portalhost.world

data class ChunkCoord(val x: Int, val z: Int) {
    val regionX: Int get() = x shr 5
    val regionZ: Int get() = z shr 5
    val localX: Int get() = x and 31
    val localZ: Int get() = z and 31

    companion object {
        const val REGION_SIZE = 32
        const val CHUNKS_PER_REGION = REGION_SIZE * REGION_SIZE
    }
}
