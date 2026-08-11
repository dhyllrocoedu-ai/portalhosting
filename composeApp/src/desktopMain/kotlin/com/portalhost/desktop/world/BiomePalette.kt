package com.portalhost.desktop.world

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Representative surface color per vanilla biome (1.20.x registry names).
 * Falls back to a neutral gray for unknown/modded biomes.
 */
object BiomePalette {

    private val NEUTRAL = Color(0xFF7A7A7A)

    private val BIOME_COLORS: Map<String, Color> = mapOf(
        "ocean" to Color(0xFF3F76E4),
        "deep_ocean" to Color(0xFF3F76E4),
        "warm_ocean" to Color(0xFF5BC6E8),
        "lukewarm_ocean" to Color(0xFF3F76E4),
        "deep_lukewarm_ocean" to Color(0xFF3F76E4),
        "cold_ocean" to Color(0xFF3F76E4),
        "deep_cold_ocean" to Color(0xFF3F76E4),
        "frozen_ocean" to Color(0xFF8DA6B0),
        "deep_frozen_ocean" to Color(0xFF8DA6B0),
        "river" to Color(0xFF3F76E4),
        "frozen_river" to Color(0xFF8DA6B0),

        "plains" to Color(0xFF91BD59),
        "sunflower_plains" to Color(0xFFB5DB88),
        "snowy_plains" to Color(0xFFF2F4F8),
        "snowy_mountains" to Color(0xFFF2F4F8),
        "ice_spikes" to Color(0xFFB4DCDC),
        "desert" to Color(0xFFDBD2A0),
        "desert_hills" to Color(0xFFDBD2A0),
        "desert_lakes" to Color(0xFFDBD2A0),
        "beach" to Color(0xFFDBD2A0),
        "snowy_beach" to Color(0xFFFAF0C0),
        "stone_shore" to Color(0xFFA2A284),
        "stony_shore" to Color(0xFFA2A284),

        "forest" to Color(0xFF79C05A),
        "flower_forest" to Color(0xFF79C05A),
        "birch_forest" to Color(0xFFB7D975),
        "birch_forest_hills" to Color(0xFFB7D975),
        "old_growth_birch_forest" to Color(0xFFB7D975),
        "dark_forest" to Color(0xFF507A32),
        "dark_forest_hills" to Color(0xFF507A32),
        "wooded_hills" to Color(0xFF79C05A),
        "windswept_hills" to Color(0xFF8AB360),
        "windswept_gravelly_hills" to Color(0xFF8AB360),
        "windswept_forest" to Color(0xFF79C05A),
        "windswept_savanna" to Color(0xFFBDB25F),
        "snowy_taiga" to Color(0xFF596651),
        "snowy_taiga_hills" to Color(0xFF596651),
        "snowy_taiga_mountains" to Color(0xFF596651),
        "taiga" to Color(0xFF596651),
        "taiga_hills" to Color(0xFF596651),
        "taiga_mountains" to Color(0xFF596651),
        "old_growth_pine_taiga" to Color(0xFF596651),
        "old_growth_spruce_taiga" to Color(0xFF596651),
        "giant_tree_taiga" to Color(0xFF596651),
        "giant_tree_taiga_hills" to Color(0xFF596651),
        "grove" to Color(0xFF8DB360),
        "snowy_slopes" to Color(0xFFF2F4F8),
        "jagged_peaks" to Color(0xFFD8D8D8),
        "frozen_peaks" to Color(0xFFE8EEF4),
        "stony_peaks" to Color(0xFFB0B0A0),
        "meadow" to Color(0xFF93C274),
        "wooded_mountains" to Color(0xFF79C05A),
        "mountain_edge" to Color(0xFF8AB360),

        "jungle" to Color(0xFF59C93C),
        "jungle_hills" to Color(0xFF59C93C),
        "jungle_edge" to Color(0xFF59C93C),
        "sparse_jungle" to Color(0xFF59C93C),
        "bamboo_jungle" to Color(0xFF59C93C),
        "bamboo_jungle_hills" to Color(0xFF59C93C),

        "swamp" to Color(0xFF6A7039),
        "swamp_hills" to Color(0xFF6A7039),
        "mangrove_swamp" to Color(0xFF2DB109),

        "savanna" to Color(0xFFBDB25F),
        "savanna_plateau" to Color(0xFFA79D64),
        "shattered_savanna" to Color(0xFFBDB25F),
        "shattered_savanna_plateau" to Color(0xFFA79D64),

        "badlands" to Color(0xFFD69E7A),
        "badlands_plateau" to Color(0xFFBF9040),
        "eroded_badlands" to Color(0xFFD69E7A),
        "wooded_badlands" to Color(0xFFBF9040),
        "wooded_badlands_plateau" to Color(0xFFBF9040),

        "mushroom_fields" to Color(0xFF8AB0D0),
        "mushroom_field_shore" to Color(0xFF8AB0D0),
        "cherry_grove" to Color(0xFFB1A6A4),

        "dripstone_caves" to Color(0xFF9CB168),
        "lush_caves" to Color(0xFF62C140),
        "deep_dark" to Color(0xFF1A1A1A),
        "the_void" to Color(0xFF0E0E0E),

        "nether_wastes" to Color(0xFFBF3B1C),
        "crimson_forest" to Color(0xFF971607),
        "warped_forest" to Color(0xFF1E8421),
        "soul_sand_valley" to Color(0xFF5C5C34),
        "basalt_deltas" to Color(0xFF685565),

        "the_end" to Color(0xFF0E0E0E),
        "small_end_islands" to Color(0xFF0E0E0E),
        "end_midlands" to Color(0xFF0E0E0E),
        "end_highlands" to Color(0xFF0E0E0E),
        "end_barrens" to Color(0xFF0E0E0E),
    )

    fun colorFor(biomeName: String): Color {
        val key = biomeName.removePrefix("minecraft:")
        return BIOME_COLORS[key] ?: NEUTRAL
    }

    fun argbFor(biomeName: String): Long = colorFor(biomeName).toArgb().toLong() and 0xFFFFFFFFL
}
