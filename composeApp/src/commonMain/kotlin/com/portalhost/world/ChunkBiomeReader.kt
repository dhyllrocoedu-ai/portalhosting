package com.portalhost.world

/**
 * Decodes the biome of a single Anvil chunk from its raw region-file payload.
 *
 * Supports both the legacy (<= 1.17) `Level.Biomes` int/byte array and the modern
 * (1.18+) per-section `sections[].biomes` paletted container.
 */
object ChunkBiomeReader {

    /**
     * Returns the canonical biome registry name (e.g. "minecraft:plains") or null
     * if the chunk has no decodable biome data.
     */
    fun biomeName(chunkData: ByteArray): String? {
        val root = RegionChunkReader.parseChunk(chunkData) ?: return null
        return resolveBiome(root)
    }

    private fun resolveBiome(root: NbtTag.NbtCompound): String? {
        val level = root.children["Level"] as? NbtTag.NbtCompound

        val sections = (level?.children?.get("sections") ?: root.children["sections"]) as? NbtTag.NbtList
        if (sections != null) {
            var best: NbtTag.NbtCompound? = null
            var bestY = Int.MIN_VALUE
            for (tag in sections.tags) {
                val section = tag as? NbtTag.NbtCompound ?: continue
                val y = (section.children["Y"] as? NbtTag.NbtInt)?.value ?: continue
                if (section.children["biomes"] != null && y > bestY) {
                    bestY = y
                    best = section
                }
            }
            val container = best?.children?.get("biomes") as? NbtTag.NbtCompound
            return paletteContainerBiome(container)
        }

        val biomes = level?.children?.get("Biomes")
        val ids = when (biomes) {
            is NbtTag.NbtByteArray -> biomes.value.map { it.toInt() and 0xFF }
            is NbtTag.NbtIntArray -> biomes.value.toList()
            else -> return null
        }
        if (ids.isEmpty()) return null
        val id = ids.getOrNull(128) ?: ids.last()
        return LEGACY_BIOME_NAMES[id]
    }

    private fun paletteContainerBiome(container: NbtTag.NbtCompound?): String? {
        if (container == null) return null

        val paletteLists = when (val palettes = container.children["palettes"]) {
            is NbtTag.NbtList ->
                palettes.tags.mapNotNull { it as? NbtTag.NbtList }
            else -> {
                val single = container.children["palette"] as? NbtTag.NbtList
                single?.let { listOf(it) } ?: emptyList()
            }
        }
        val entries = paletteLists.map { list ->
            list.tags.mapNotNull { it as? NbtTag.NbtCompound }
        }
        if (entries.isEmpty() || entries.first().isEmpty()) return null

        val data = (container.children["data"] as? NbtTag.NbtLongArray)?.value
        if (data == null || data.isEmpty()) {
            return biomeNameOf(entries.first().first())
        }

        val size = 64 // 4 * 4 * 4 biome cubes per section
        val bitsPerEntry = (data.size.toLong() * 64L) / size.toLong()
        if (bitsPerEntry < 1 || bitsPerEntry > 32) return null

        val values = unpackBits(data, bitsPerEntry.toInt(), size)
        val index = values.lastOrNull() ?: return null
        return when {
            index >= 0 -> biomeNameOf(entries.first().getOrNull(index))
            else -> {
                val paletteIndex = -index - 1
                if (paletteIndex < entries.size) {
                    biomeNameOf(entries[paletteIndex].firstOrNull())
                } else null
            }
        }
    }

    private fun biomeNameOf(entry: NbtTag.NbtCompound?): String? {
        return (entry?.children?.get("Name") as? NbtTag.NbtString)?.value
    }

    private fun unpackBits(data: LongArray, bits: Int, count: Int): IntArray {
        val result = IntArray(count)
        val mask = if (bits >= 64) -1L else (1L shl bits) - 1L
        var bitOffset = 0
        for (i in 0 until count) {
            val longIndex = bitOffset shr 6
            val bitIndex = bitOffset and 63
            var value = data[longIndex] ushr bitIndex
            if (bitIndex + bits > 64 && longIndex + 1 < data.size) {
                value = value or (data[longIndex + 1] shl (64 - bitIndex))
            }
            result[i] = (value and mask).toInt()
            bitOffset += bits
        }
        return result
    }

    private val LEGACY_BIOME_NAMES: Map<Int, String> = mapOf(
        0 to "minecraft:ocean",
        1 to "minecraft:plains",
        2 to "minecraft:desert",
        3 to "minecraft:windswept_hills",
        4 to "minecraft:forest",
        5 to "minecraft:taiga",
        6 to "minecraft:swamp",
        7 to "minecraft:river",
        8 to "minecraft:nether_wastes",
        9 to "minecraft:the_end",
        10 to "minecraft:frozen_ocean",
        11 to "minecraft:frozen_river",
        12 to "minecraft:snowy_plains",
        13 to "minecraft:snowy_mountains",
        14 to "minecraft:mushroom_fields",
        15 to "minecraft:mushroom_field_shore",
        16 to "minecraft:beach",
        17 to "minecraft:desert_hills",
        18 to "minecraft:wooded_hills",
        19 to "minecraft:taiga_hills",
        20 to "minecraft:stony_shore",
        21 to "minecraft:jungle",
        22 to "minecraft:jungle_hills",
        23 to "minecraft:sparse_jungle",
        24 to "minecraft:deep_ocean",
        25 to "minecraft:stone_shore",
        26 to "minecraft:snowy_beach",
        27 to "minecraft:birch_forest",
        28 to "minecraft:birch_forest_hills",
        29 to "minecraft:dark_forest",
        30 to "minecraft:snowy_taiga",
        31 to "minecraft:snowy_taiga_hills",
        32 to "minecraft:old_growth_pine_taiga",
        33 to "minecraft:old_growth_spruce_taiga",
        34 to "minecraft:wooded_mountains",
        35 to "minecraft:savanna",
        36 to "minecraft:savanna_plateau",
        37 to "minecraft:badlands",
        38 to "minecraft:wooded_badlands",
        39 to "minecraft:badlands_plateau",
        40 to "minecraft:small_end_islands",
        41 to "minecraft:end_midlands",
        42 to "minecraft:end_highlands",
        43 to "minecraft:end_barrens",
        44 to "minecraft:warm_ocean",
        45 to "minecraft:lukewarm_ocean",
        46 to "minecraft:cold_ocean",
        47 to "minecraft:deep_lukewarm_ocean",
        48 to "minecraft:deep_cold_ocean",
        49 to "minecraft:deep_frozen_ocean",
        50 to "minecraft:the_void",
        127 to "minecraft:the_void",
    )
}
