package com.portalhost.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class NbtParserTest {

    @Test
    fun parsesCompoundWithPrimitives() {
        val root = byteArrayOf(
            0x0A.toByte(),
            0x00, 0x00,
            TAG_INT.toByte(), 0x00, 0x01, 'v'.code.toByte(),
            0x7F, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            TAG_STRING.toByte(), 0x00, 0x04, 'n'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'e'.code.toByte(),
            0x00, 0x02, 'h'.code.toByte(), 'i'.code.toByte(),
            TAG_END.toByte(),
        )
        val compound = NbtParser.parse(root)
        assertNotNull(compound)
        assertEquals(2147483647, (compound!!.children["v"] as NbtTag.NbtInt).value)
        assertEquals("hi", (compound.children["name"] as NbtTag.NbtString).value)
    }

    @Test
    fun parsesByteArrayAndIntArray() {
        val root = ByteArrayOutputStream()
        root.write(0x0A); root.write(byteArrayOf(0, 0))
        root.write(TAG_BYTE_ARRAY); root.write(byteArrayOf(0, 3)); root.write("arr".toByteArray())
        root.write(byteArrayOf(0, 0, 0, 2, 1, 2))
        root.write(TAG_INT_ARRAY); root.write(byteArrayOf(0, 4)); root.write("ints".toByteArray())
        root.write(byteArrayOf(0, 0, 0, 1, 0, 0, 0, 5))
        root.write(TAG_END)
        val compound = NbtParser.parse(root.toByteArray())!!
        val arr = (compound.children["arr"] as NbtTag.NbtByteArray).value
        assertTrue(arr.contentEquals(byteArrayOf(1, 2)))
        val ints = (compound.children["ints"] as NbtTag.NbtIntArray).value
        assertTrue(ints.contentEquals(intArrayOf(5)))
    }

    @Test
    fun parsesNestedListOfCompounds() {
        val root = ByteArrayOutputStream()
        root.write(0x0A); root.write(byteArrayOf(0, 0))
        root.write(TAG_LIST); root.write(byteArrayOf(0, 5)); root.write("items".toByteArray())
        root.write(TAG_COMPOUND); root.write(byteArrayOf(0, 0, 0, 2))
        // item 0
        root.write(TAG_STRING); root.write(byteArrayOf(0, 2)); root.write("id".toByteArray())
        root.write(byteArrayOf(0, 1)); root.write("a".toByteArray())
        root.write(TAG_END)
        // item 1
        root.write(TAG_STRING); root.write(byteArrayOf(0, 2)); root.write("id".toByteArray())
        root.write(byteArrayOf(0, 1)); root.write("b".toByteArray())
        root.write(TAG_END)
        root.write(TAG_END) // end list
        root.write(TAG_END) // end root

        val compound = NbtParser.parse(root.toByteArray())!!
        val list = compound.children["items"] as NbtTag.NbtList
        assertEquals(2, list.tags.size)
        val first = list.tags[0] as NbtTag.NbtCompound
        assertEquals("a", (first.children["id"] as NbtTag.NbtString).value)
    }

    @Test
    fun returnsNullOnMalformedInput() {
        assertNull(NbtParser.parse(byteArrayOf(0x0A, 0x7F, 0xFF.toByte(), 0x00, 0x00)))
        assertNull(NbtParser.parse(byteArrayOf()))
        assertNull(NbtParser.parse(byteArrayOf(0x0A, 0x00, 0x00, 0x03, 0x00, 0x00)))
    }

    private val TAG_END = 0
    private val TAG_INT = 3
    private val TAG_STRING = 8
    private val TAG_LIST = 9
    private val TAG_COMPOUND = 10
    private val TAG_BYTE_ARRAY = 7
    private val TAG_INT_ARRAY = 11
}
