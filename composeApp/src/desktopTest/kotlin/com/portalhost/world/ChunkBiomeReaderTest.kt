package com.portalhost.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

class ChunkBiomeReaderTest {

    @Test
    fun decodesLegacyLevelBiomes() {
        val biomes = ByteArray(256) { 1 } // plains
        val levelPayload = ByteArrayOutputStream()
        levelPayload.write(TAG_BYTE_ARRAY)
        levelPayload.write(nbtStringBytes("Biomes"))
        levelPayload.write(intBytes(biomes.size))
        levelPayload.write(biomes)
        levelPayload.write(TAG_END)

        val root = ByteArrayOutputStream()
        root.write(TAG_COMPOUND); root.write(shortBytes(0))
        root.write(TAG_COMPOUND); root.write(nbtStringBytes("Level"))
        root.write(levelPayload.toByteArray())
        root.write(TAG_END)

        val chunk = zlibChunk(root.toByteArray())
        assertEquals("minecraft:plains", ChunkBiomeReader.biomeName(chunk))
    }

    @Test
    fun decodesModernPalettedContainer() {
        val sectionPayload = ByteArrayOutputStream()
        sectionPayload.write(TAG_INT); sectionPayload.write(nbtStringBytes("Y")); sectionPayload.write(intBytes(4))
        sectionPayload.write(TAG_COMPOUND); sectionPayload.write(nbtStringBytes("biomes"))
        // palettes: [[{Name:"minecraft:plains"}]]
        sectionPayload.write(TAG_LIST); sectionPayload.write(nbtStringBytes("palettes"))
        sectionPayload.write(TAG_LIST)
        sectionPayload.write(intBytes(1))
        // palette 0 is a list of compounds with one entry
        sectionPayload.write(TAG_COMPOUND)
        sectionPayload.write(intBytes(1))
        sectionPayload.write(TAG_STRING); sectionPayload.write(nbtStringBytes("Name")); sectionPayload.write(nbtStringBytes("minecraft:plains"))
        sectionPayload.write(TAG_END) // entry compound
        sectionPayload.write(TAG_END) // palette 0 list
        sectionPayload.write(TAG_END) // palettes list
        // data: [0L] -> single long, one bit per entry, all index 0
        sectionPayload.write(TAG_LONG_ARRAY); sectionPayload.write(nbtStringBytes("data"))
        sectionPayload.write(intBytes(1))
        sectionPayload.write(longBytes(0L))
        sectionPayload.write(TAG_END) // biomes compound
        sectionPayload.write(TAG_END) // section compound

        val root = ByteArrayOutputStream()
        root.write(TAG_COMPOUND); root.write(shortBytes(0))
        root.write(TAG_LIST); root.write(nbtStringBytes("sections"))
        root.write(TAG_COMPOUND)
        root.write(intBytes(1))
        root.write(sectionPayload.toByteArray())
        root.write(TAG_END) // sections list
        root.write(TAG_INT); root.write(nbtStringBytes("dataVersion")); root.write(intBytes(3955))
        root.write(TAG_END) // root

        val chunk = zlibChunk(root.toByteArray())
        assertEquals("minecraft:plains", ChunkBiomeReader.biomeName(chunk))
    }

    @Test
    fun decodesUncompressedPayload() {
        val root = ByteArrayOutputStream()
        root.write(TAG_COMPOUND); root.write(shortBytes(0))
        root.write(TAG_COMPOUND); root.write(nbtStringBytes("Level"))
        root.write(TAG_BYTE_ARRAY); root.write(nbtStringBytes("Biomes"))
        root.write(intBytes(256))
        root.write(ByteArray(256) { 7 }) // river at sample index 128
        root.write(TAG_END)
        root.write(TAG_END)

        val nbt = root.toByteArray()
        val chunk = ByteArray(5 + nbt.size)
        chunk[0] = (nbt.size shr 24).toByte()
        chunk[1] = (nbt.size shr 16).toByte()
        chunk[2] = (nbt.size shr 8).toByte()
        chunk[3] = nbt.size.toByte()
        chunk[4] = 3 // uncompressed
        System.arraycopy(nbt, 0, chunk, 5, nbt.size)

        assertEquals("minecraft:river", ChunkBiomeReader.biomeName(chunk))
    }

    @Test
    fun returnsNullOnGarbage() {
        assertNull(ChunkBiomeReader.biomeName(byteArrayOf(0, 0, 0, 0)))
        assertNull(ChunkBiomeReader.biomeName(byteArrayOf(0, 0, 0, 5, 2, 1, 2, 3, 4, 5)))
        assertNull(ChunkBiomeReader.biomeName(ByteArray(1000)))
    }

    @Test
    fun legacyMapsDesertAndSwampIds() {
        val biomes = ByteArray(256) { 2 } // desert
        val root = ByteArrayOutputStream()
        root.write(TAG_COMPOUND); root.write(shortBytes(0))
        root.write(TAG_COMPOUND); root.write(nbtStringBytes("Level"))
        root.write(TAG_BYTE_ARRAY); root.write(nbtStringBytes("Biomes"))
        root.write(intBytes(biomes.size)); root.write(biomes)
        root.write(TAG_END); root.write(TAG_END)

        assertEquals("minecraft:desert", ChunkBiomeReader.biomeName(zlibChunk(root.toByteArray())))
    }

    private fun zlibChunk(nbt: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(nbt)
        deflater.finish()
        val compressed = ByteArray(64 * 1024)
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

    private val TAG_BYTE_ARRAY = 7
    private val TAG_STRING = 8
    private val TAG_LIST = 9
    private val TAG_COMPOUND = 10
    private val TAG_INT = 3
    private val TAG_LONG_ARRAY = 12
    private val TAG_END = 0
}
