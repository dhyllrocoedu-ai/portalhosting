package com.portalhost.world

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import java.io.File
import java.io.RandomAccessFile

class RegionFileIndexTest {

    @Test
    fun chunkCoordRegionMath() {
        val c = ChunkCoord(33, -2)
        assertEquals(1, c.regionX)
        assertEquals(-1, c.regionZ)
        assertEquals(1, c.localX)
        assertEquals(30, c.localZ)
    }

    @Test
    fun indexFile_allPresent() {
        val tmp = File.createTempFile("region_all_present", ".mca")
        tmp.deleteOnExit()
        writeLocationTable(tmp) { _ -> Triple(1, 1, false) }

        val index = RegionFileIndex().indexFile(tmp, 0, 0)!!
        assertEquals(1024, index.chunks.size)
        assertTrue(index.chunks.values.all { it.generated })
        assertTrue(index.chunks[ChunkCoord(0, 0)]!!.generated)
        assertTrue(index.chunks[ChunkCoord(31, 31)]!!.generated)
    }

    @Test
    fun indexFile_allAbsent() {
        val tmp = File.createTempFile("region_all_absent", ".mca")
        tmp.deleteOnExit()
        writeLocationTable(tmp) { _ -> Triple(0, 0, true) }

        val index = RegionFileIndex().indexFile(tmp, 0, 0)!!
        assertEquals(1024, index.chunks.size)
        assertTrue(index.chunks.values.none { it.generated })
        assertFalse(index.chunks[ChunkCoord(0, 0)]!!.generated)
    }

    @Test
    fun indexFile_mixed() {
        val tmp = File.createTempFile("region_mixed", ".mca")
        tmp.deleteOnExit()
        writeLocationTable(tmp) { i ->
            if (i % 2 == 0) Triple(i + 1, 1, false)
            else Triple(0, 0, true)
        }

        val index = RegionFileIndex().indexFile(tmp, 0, 0)!!
        val evenPresent = index.chunks[ChunkCoord(0, 0)]!!
        val oddAbsent = index.chunks[ChunkCoord(1, 0)]!!
        assertTrue(evenPresent.generated)
        assertFalse(oddAbsent.generated)
    }

    @Test
    fun indexDirectory_filtersAndIndexes() {
        val dir = File.createTempFile("region_dir", "")
        dir.delete()
        dir.mkdirs()
        dir.deleteOnExit()

        writeLocationTable(File(dir, "r.0.0.mca")) { _ -> Triple(1, 1, false) }
        writeLocationTable(File(dir, "r.-1.2.mca")) { _ -> Triple(2, 1, false) }
        File(dir, "level.dat").writeText("not a region")
        File(dir, "r.foo.bar.mca").writeText("also not")

        val results = RegionFileIndex().indexDirectory(dir)
        assertEquals(2, results.size)
        val rxs = results.map { it.regionX }.toSet()
        val rzs = results.map { it.regionZ }.toSet()
        assertTrue(0 in rxs && -1 in rxs)
        assertTrue(0 in rzs && 2 in rzs)
    }

    private fun writeLocationTable(file: File, perEntry: (Int) -> Triple<Int, Int, Boolean>) {
        val bytes = ByteArray(1024 * 4)
        for (i in 0 until 1024) {
            val (offset, count, absent) = perEntry(i)
            val effectiveOffset = if (absent) 0x800000.toInt() or offset else offset
            val base = i * 4
            bytes[base] = ((effectiveOffset shr 16) and 0xFF).toByte()
            bytes[base + 1] = ((effectiveOffset shr 8) and 0xFF).toByte()
            bytes[base + 2] = (effectiveOffset and 0xFF).toByte()
            bytes[base + 3] = count.toByte()
        }
        RandomAccessFile(file, "rw").use { raf ->
            raf.write(bytes)
            raf.setLength(1024L * 4)
        }
    }
}
