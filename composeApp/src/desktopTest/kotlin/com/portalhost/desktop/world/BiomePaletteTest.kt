package com.portalhost.desktop.world

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BiomePaletteTest {

    @Test
    fun knownBiomesMapToDistinctColors() {
        val plains = BiomePalette.colorFor("minecraft:plains")
        val desert = BiomePalette.colorFor("minecraft:desert")
        val ocean = BiomePalette.colorFor("minecraft:ocean")
        val darkForest = BiomePalette.colorFor("minecraft:dark_forest")

        assertNotEquals(plains, desert)
        assertNotEquals(desert, ocean)
        assertNotEquals(ocean, darkForest)
        assertEquals(Color(0xFF91BD59), plains)
        assertEquals(Color(0xFFDBD2A0), desert)
    }

    @Test
    fun stripsMinecraftNamespace() {
        assertEquals(BiomePalette.colorFor("minecraft:plains"), BiomePalette.colorFor("plains"))
    }

    @Test
    fun unknownBiomeFallsBackToNeutralGray() {
        assertEquals(Color(0xFF7A7A7A), BiomePalette.colorFor("modded:weird_land"))
        assertEquals(Color(0xFF7A7A7A), BiomePalette.colorFor("minecraft:future_biome"))
    }

    @Test
    fun argbForIsAValid32BitColor() {
        val argb = BiomePalette.argbFor("minecraft:plains")
        assert(argb in 0..0xFFFFFFFFL)
        assertEquals(0xFF, (argb shr 24) and 0xFF)
    }
}
