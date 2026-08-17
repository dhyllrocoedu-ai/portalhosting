package com.portalhost.world

import java.io.ByteArrayInputStream
import java.io.RandomAccessFile
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater

/**
 * Reads and decompresses a single Anvil chunk from a region (.mca) file.
 *
 * The chunk payload layout is: [4-byte big-endian length][1-byte compression type][compressed NBT].
 * Compression types: 1 = gzip, 2 = zlib, 3 = uncompressed.
 */
object RegionChunkReader {

    const val SECTOR_BYTES = 4096
    private const val MAX_CHUNK_BYTES = 8 * 1024 * 1024

    /**
     * Reads the raw chunk payload (length prefix + compression byte + NBT bytes)
     * starting at the given sector offset of a region file. Returns null on any
     * invalid or truncated data.
     */
    fun readPayload(raf: RandomAccessFile, sectorOffset: Int, sectorCount: Int): ByteArray? {
        if (sectorOffset <= 0 || sectorCount <= 0) return null
        val position = sectorOffset.toLong() * SECTOR_BYTES
        if (position < 0 || position + 4 > raf.length()) return null
        val lengthBuf = ByteArray(4)
        // Retry up to 3 times — the server may be mid-write when we read
        // the location table, causing the 4-byte length prefix to be stale.
        for (attempt in 1..3) {
            try {
                raf.seek(position)
                raf.readFully(lengthBuf)
                val length =
                    ((lengthBuf[0].toInt() and 0xFF) shl 24) or
                        ((lengthBuf[1].toInt() and 0xFF) shl 16) or
                        ((lengthBuf[2].toInt() and 0xFF) shl 8) or
                        (lengthBuf[3].toInt() and 0xFF)
                if (length <= 0 || length > MAX_CHUNK_BYTES) return null
                if (position + 4 + length > raf.length()) return null
                val payload = ByteArray(length)
                raf.readFully(payload)
                return payload
            } catch (_: Exception) {
                if (attempt < 3) {
                    raf.seek(position)
                    Thread.sleep(20L * attempt)
                } else return null
            }
        }
        return null
    }

    /**
     * Parses a raw chunk payload into its root NBT compound. Returns null on
     * invalid length, unknown compression, or unparseable NBT.
     */
    fun parseChunk(chunkData: ByteArray): NbtTag.NbtCompound? {
        if (chunkData.size < 5) return null
        val length =
            ((chunkData[0].toInt() and 0xFF) shl 24) or
                ((chunkData[1].toInt() and 0xFF) shl 16) or
                ((chunkData[2].toInt() and 0xFF) shl 8) or
                (chunkData[3].toInt() and 0xFF)
        val compression = chunkData[4].toInt() and 0xFF
        if (length <= 0 || length > MAX_CHUNK_BYTES) return null
        val payload = chunkData.copyOfRange(5, minOf(chunkData.size, 5 + length))

        val nbtBytes = when (compression) {
            1 -> gunzip(payload)
            2 -> inflate(payload)
            3 -> payload
            else -> return null
        } ?: return null

        return NbtParser.parse(nbtBytes)
    }

    fun inflate(data: ByteArray): ByteArray? {
        return try {
            val inflater = Inflater()
            inflater.setInput(data)
            val out = ByteArray(1024 * 1024)
            var used = 0
            while (!inflater.finished() && used < NbtParser.MAX_PAYLOAD) {
                val read = inflater.inflate(out, used, out.size - used)
                if (read == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                    if (out.size - used < 64 * 1024) {
                        throw IllegalStateException("Inflate stalled")
                    }
                }
                used += read
            }
            inflater.end()
            out.copyOf(used)
        } catch (_: Exception) {
            null
        }
    }

    fun gunzip(data: ByteArray): ByteArray? {
        return try {
            val gzip = GZIPInputStream(ByteArrayInputStream(data), 64 * 1024)
            gzip.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }
}
