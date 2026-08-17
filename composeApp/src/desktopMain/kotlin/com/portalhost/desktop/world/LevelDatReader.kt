package com.portalhost.desktop.world

import com.portalhost.world.NbtParser
import com.portalhost.world.NbtTag
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Reads the world seed from a Minecraft `level.dat` file.
 *
 * The file is a gzip-compressed NBT compound with layout:
 *   root (compound)
 *   └── Data (compound)
 *       ├── Seed (long)          ← older versions (< 1.16)
 *       ├── WorldGenSettings (compound)
 *       │   └── seed (long)      ← newer versions (1.16+)
 *       └── (other fields)
 *
 * Returns null if the file is missing, corrupt, or has no recognizable seed.
 */
object LevelDatReader {

    fun readSeed(levelDat: File): Long? {
        if (!levelDat.isFile) return null
        return try {
            val root = GZIPInputStream(levelDat.inputStream()).use { gz ->
                NbtParser.parse(gz.readBytes())
            } ?: return null

            val data = (root.children["Data"] as? NbtTag.NbtCompound) ?: return null
            val worldGen = (data.children["WorldGenSettings"] as? NbtTag.NbtCompound)
            val modernSeed = worldGen?.children?.get("seed") as? NbtTag.NbtLong
            if (modernSeed != null) return modernSeed.value
            val legacySeed = data.children["Seed"] as? NbtTag.NbtLong
            legacySeed?.value
        } catch (_: Exception) {
            null
        }
    }
}