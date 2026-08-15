package com.portalhost.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

class HeightmapChunkDecoderTest {

    private val decoder = HeightmapChunkDecoder()

    @Test
    fun resolvesSurfaceColorsFromWorldSurfaceHeightmap() {
        val section = section(
            y = 4,
            palette = listOf("minecraft:grass_block", "minecraft:water"),
            blockData = IntArray(4096).also { it[0] = 1 }, // column (0,0) at localY 0 = water
        )
        val heights = IntArray(256) { 64 } // world Y 64 -> section 4, localY 0
        val root = chunkRoot(sections = listOf(section), heightmap = heights)

        val colors = decoder.surfaceColors(zlibChunk(root))

        assertEquals(
            BlockColorTable.colorFor("minecraft:water"),
            colors?.get(0),
        )
        assertEquals(
            BlockColorTable.colorFor("minecraft:grass_block"),
            colors?.get(1),
        )
        assertEquals(256, colors?.size)
    }

    @Test
    fun fallsBackToTopDownScanWithoutHeightmap() {
        val section = section(
            y = 4,
            palette = listOf("minecraft:air", "minecraft:stone"),
            blockData = IntArray(4096).also { it[(5 shl 8) or 1] = 1 }, // column (1,0) localY 5 = stone
        )
        val root = chunkRoot(sections = listOf(section), heightmap = null)

        val colors = decoder.surfaceColors(zlibChunk(root))

        assertEquals(UNRESOLVED, colors?.get(0)) // all air -> transparent
        assertEquals(BlockColorTable.colorFor("minecraft:stone"), colors?.get(1))
    }

    @Test
    fun returnsNullWithoutSections() {
        assertNull(decoder.surfaceColors(zlibChunk(chunkRoot(emptyList(), null))))
    }

    @Test
    fun handlesNegativeWorldYHeightmap() {
        // Section Y = -1 (world Y -16..-1). Heightmap value -1 -> section -1, localY 15.
        val section = section(
            y = -1,
            palette = listOf("minecraft:sand"),
            blockData = null,
        )
        val heights = IntArray(256) { -1 }
        val root = chunkRoot(sections = listOf(section), heightmap = heights)

        val colors = decoder.surfaceColors(zlibChunk(root))

        assertEquals(BlockColorTable.colorFor("minecraft:sand"), colors?.get(0))
    }

    // ---- NBT builders (same byte-level layout as ChunkBiomeReaderTest) ----

    private fun chunkRoot(sections: List<ByteArray>, heightmap: IntArray?): ByteArray {
        val root = ByteArrayOutputStream()
        root.write(TAG_COMPOUND); root.write(shortBytes(0))
        root.write(TAG_LIST); root.write(nbtStringBytes("sections"))
        root.write(TAG_COMPOUND); root.write(intBytes(sections.size))
        sections.forEach { root.write(it) }
        if (heightmap != null) {
            root.write(TAG_COMPOUND); root.write(nbtStringBytes("Heightmaps"))
            root.write(TAG_LONG_ARRAY); root.write(nbtStringBytes("WORLD_SURFACE"))
            val packed = packBits(heightmap, 9)
            root.write(intBytes(packed.size)); packed.forEach { root.write(longBytes(it)) }
            root.write(TAG_END) // Heightmaps
        }
        root.write(TAG_INT); root.write(nbtStringBytes("dataVersion")); root.write(intBytes(3955))
        root.write(TAG_END) // root
        return root.toByteArray()
    }

    private fun section(y: Int, palette: List<String>, blockData: IntArray?): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(TAG_INT); out.write(nbtStringBytes("Y")); out.write(intBytes(y))
        out.write(TAG_COMPOUND); out.write(nbtStringBytes("block_states"))
        out.write(TAG_LIST); out.write(nbtStringBytes("palette"))
        out.write(TAG_COMPOUND); out.write(intBytes(palette.size))
        palette.forEach { name ->
            out.write(TAG_STRING); out.write(nbtStringBytes("Name")); out.write(nbtStringBytes(name))
            out.write(TAG_END) // palette entry
        }
        if (blockData != null) {
            val bits = maxOf(4, 32 - Integer.numberOfLeadingZeros(palette.size - 1))
            val packed = packBits(blockData, bits)
            out.write(TAG_LONG_ARRAY); out.write(nbtStringBytes("data"))
            out.write(intBytes(packed.size)); packed.forEach { out.write(longBytes(it)) }
        }
        out.write(TAG_END) // block_states
        out.write(TAG_END) // section
        return out.toByteArray()
    }

    private fun packBits(values: IntArray, bits: Int): LongArray {
        val mask = (1L shl bits) - 1L
        val longs = LongArray((values.size * bits + 63) / 64)
        var bitOffset = 0
        for (v in values) {
            val longIndex = bitOffset shr 6
            val bitIndex = bitOffset and 63
            longs[longIndex] = longs[longIndex] or ((v.toLong() and mask) shl bitIndex)
            if (bitIndex + bits > 64) {
                longs[longIndex + 1] = longs[longIndex + 1] or ((v.toLong() and mask) ushr (64 - bitIndex))
            }
            bitOffset += bits
        }
        return longs
    }

    private fun zlibChunk(nbt: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(nbt)
        deflater.finish()
        val compressed = ByteArray(1024 * 1024)
        val len = deflater.deflate(compressed)
        deflater.end()

        val chunk = ByteArray(5 + len)
        chunk[0] = (len shr 24).toByte()
        chunk[1] = (len shr 16).toByte()
        chunk[2] = (len shr 8).toByte()
        chunk[3] = len.toByte()
        chunk[4] = 2 // zlib
        System.arraycopy(compressed, 0, chunk, 5, len)
        return chunk
    }

    private fun nbtStringBytes(s: String): ByteArray {
        val b = s.toByteArray()
        return shortBytes(b.size) + b
    }

    private fun shortBytes(v: Int): ByteArray = byteArrayOf((v shr 8).toByte(), v.toByte())

    private fun intBytes(v: Int): ByteArray =
        byteArrayOf((v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte())

    private fun longBytes(v: Long): ByteArray {
        val out = ByteArray(8)
        for (i in 0 until 8) out[i] = (v shr (56 - 8 * i)).toByte()
        return out
    }

    private val TAG_STRING = 8
    private val TAG_LIST = 9
    private val TAG_COMPOUND = 10
    private val TAG_INT = 3
    private val TAG_LONG_ARRAY = 12
    private val TAG_END = 0
}
