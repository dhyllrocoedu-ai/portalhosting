package com.portalhost.desktop.world

import java.io.File
import kotlin.math.*

/**
 * Simplified cubiomes-style biome fallback.
 *
 * Reads the seed from level.dat and generates biome colours at any (x, z)
 * using a simplified 1.18+ climate model (temperature / humidity noise
 * seeded by the world seed).  This is used when region-file decoding comes
 * back empty — e.g. the server holds an exclusive write lock on .mca files.
 *
 * References:
 *   https://github.com/Cubitect/cubiomes
 *   https://minecraft.wiki/w/Biome
 */
class CubiomesFallback(private val levelDat: File) {

    /** World seed extracted from level.dat. 0 = not available. */
val seed: Long = runCatching { LevelDatReader.readSeed(levelDat) ?: 0L }.getOrDefault(0L)

    private val tempNoise: PerlinNoise by lazy { PerlinNoise(seed xor TEMP_SEED) }
    private val humidNoise: PerlinNoise by lazy { PerlinNoise(seed xor HUMID_SEED) }
    private val contNoise: PerlinNoise by lazy { PerlinNoise(seed xor CONT_SEED) }

    /**
     * Returns the biome ARGB colour for the centre of the given chunk, or
     * null if the seed could not be determined.
     */
    fun biomeColorAtChunk(chunkX: Int, chunkZ: Int): Long? {
        if (seed == 0L) return null
        val wx = chunkX * 16 + 8
        val wz = chunkZ * 16 + 8
        return biomeColor(wx, wz)
    }

    fun biomeColorAtBlock(blockX: Int, blockZ: Int): Long? {
        if (seed == 0L) return null
        return biomeColor(blockX, blockZ)
    }

    private fun biomeColor(x: Int, z: Int): Long {
        val t = ((tempNoise.noise2D(x * 0.0025, z * 0.0025) + 1.0) * 0.5).coerceIn(0.0, 1.0)
        val h = ((humidNoise.noise2D(x * 0.0025 + 100.0, z * 0.0025 + 100.0) + 1.0) * 0.5).coerceIn(0.0, 1.0)
        val c = ((contNoise.noise2D(x * 0.0015 + 200.0, z * 0.0015 + 200.0) + 1.0) * 0.5).coerceIn(0.0, 1.0)
        return BiomeLookup.color(t, h, c)
    }

    private companion object {
        private const val TEMP_SEED = 0x4A6C8F12L
        private const val HUMID_SEED = 0x9B3D15E7L
        private const val CONT_SEED = 0x7E2A4F8CL
    }
}

/** 2-D Perlin noise with octave fractal Brownian motion. */
private class PerlinNoise(seed: Long) {
    private val rng = XorShift(seed)
    private val perm = IntArray(512) { i -> rng.nextUShort().toInt() and 255 }

    fun noise2D(x: Double, y: Double): Double {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val xf = x - xi
        val yf = y - yi
        val u = fade(xf)
        val v = fade(yf)

        val n00 = grad(perm[(perm[xi and 255] + yi) and 255], xf, yf)
        val n10 = grad(perm[(perm[(xi + 1) and 255] + yi) and 255], xf - 1.0, yf)
        val n01 = grad(perm[(perm[xi and 255] + yi + 1) and 255], xf, yf - 1.0)
        val n11 = grad(perm[(perm[(xi + 1) and 255] + yi + 1) and 255], xf - 1.0, yf - 1.0)

        val nx0 = lerp(n00, n10, u)
        val nx1 = lerp(n01, n11, u)
        return lerp(nx0, nx1, v)
    }

    private fun fade(t: Double) = t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
    private fun lerp(a: Double, b: Double, t: Double) = a + t * (b - a)

    private fun grad(hash: Int, x: Double, y: Double): Double {
        val h = hash and 7
        val u = if (h < 4) x else y
        val v = if (h < 4) y else x
        return (if (h and 1 == 0) u else -u) + (if (h and 2 == 0) v else -v)
    }
}

/** XorShift64* PRNG for reproducible per-seed noise. */
private class XorShift(var state: Long) {
    fun next(): Long {
        state = state xor (state ushr 12)
        state = (state xor (state shl 25)) xor (state shl 27)
        return (state * 0x2545F4914F6CDD1DL)
    }
    fun nextUShort(): Int = (next() ushr 16).toInt() and 0xFFFF
}

/**
 * Simplified biome lookup using temperature/humidity/continentalness axes.
 * Based on the cubiomes/amidst biome climate grid.
 */
private object BiomeLookup {
    private const val OCEAN = 0
    private const val PLAINS = 1
    private const val DESERT = 2
    private const val MOUNTAINS = 3
    private const val FOREST = 4
    private const val TAIGA = 5
    private const val SWAMP = 6
    private const val RIVER = 7
    private const val NETHER_WASTES = 8
    private const val THE_END = 9
    private const val FROZEN_OCEAN = 10
    private const val FROZEN_RIVER = 11
    private const val SNOW_PLAINS = 12
    private const val MUSHROOM_FIELDS = 13
    private const val BEACH = 14
    private const val DARK_FOREST = 15
    private const val SNOWY_TAIGA = 16
    private const val JUNGLE = 17
    private const val SPARSE_JUNGLE = 18
    private const val BADLANDS = 19
    private const val SAVANNA = 20
    private const val ICE_SPIKES = 21
    private const val WARM_OCEAN = 22
    private const val LUKEWARM_OCEAN = 23
    private const val COLD_OCEAN = 24
    private const val DEEP_OCEAN = 25
    private const val DEEP_LUKEWARM_OCEAN = 26
    private const val DEEP_COLD_OCEAN = 27
    private const val DEEP_FROZEN_OCEAN = 28
    private const val STONY_SHORE = 29
    private const val SUNFLOWER_PLAINS = 30
    private const val WINDSWEPT_HILLS = 31
    private const val FOREST_FLOWER = 32
    private const val TAIGA_OLD = 33
    private const val RIVER_OLD = 34
    private const val BIRCH_FOREST = 35
    private const val TALL_BIRCH_FOREST = 36
    private const val OLD_GROWTH_BIRCH_FOREST = 37
    private const val OLD_GROWTH_PINE_TAIGA = 38
    private const val OLD_GROWTH_SPRUCE_TAIGA = 39
    private const val WETLAND = 40
    private const val CHERRY_GROVE = 41
    private const val PALE_GARDEN = 42

    private val BIOME_COLORS: IntArray = intArrayOf(
        0xFF4287F5.toInt(), // OCEAN
        0xFF8DBF3F.toInt(), // PLAINS
        0xFFFCAC56.toInt(), // DESERT
        0xFF777777.toInt(), // MOUNTAINS
        0xFF2D5E1E.toInt(), // FOREST
        0xFF376330.toInt(), // TAIGA
        0xFF5B756D.toInt(), // SWAMP
        0xFF4466CC.toInt(), // RIVER
        0xFF321212.toInt(), // NETHER
        0xFF8080FF.toInt(), // THE_END
        0xFF335B8E.toInt(), // FROZEN_OCEAN
        0xFF335B8E.toInt(), // FROZEN_RIVER
        0xFFB0B0B0.toInt(), // SNOW_PLAINS
        0xFFFF00FF.toInt(), // MUSHROOM_FIELDS
        0xFFE8D6A0.toInt(), // BEACH
        0xFF0B0D17.toInt(), // DARK_FOREST
        0xFF336462.toInt(), // SNOWY_TAIGA
        0xFF0B6217.toInt(), // JUNGLE
        0xFF1A6622.toInt(), // SPARSE_JUNGLE
        0xFFD1A846.toInt(), // BADLANDS
        0xFFB0A040.toInt(), // SAVANNA
        0xFFD6E8FF.toInt(), // ICE_SPIKES
        0xFF018FFE.toInt(), // WARM_OCEAN
        0xFF45ADC0.toInt(), // LUKEWARM_OCEAN
        0xFF6397C8.toInt(), // COLD_OCEAN
        0xFF1D2D6E.toInt(), // DEEP_OCEAN
        0xFF1D6B7A.toInt(), // DEEP_LUKEWARM_OCEAN
        0xFF2A5C8A.toInt(), // DEEP_COLD_OCEAN
        0xFF284D7A.toInt(), // DEEP_FROZEN_OCEAN
        0xFFA2A28C.toInt(), // STONY_SHORE
        0xFFC5B246.toInt(), // SUNFLOWER_PLAINS
        0xFF7A8078.toInt(), // WINDSWEPT_HILLS
        0xFF6B8E3A.toInt(), // FLOWER_FOREST
        0xFF217629.toInt(), // TAIGA_OLD
        0xFF4478FF.toInt(), // RIVER_OLD (unused)
        0xFF8DB344.toInt(), // BIRCH_FOREST
        0xFFB0DC68.toInt(), // TALL_BIRCH_FOREST
        0xFFBDC678.toInt(), // OLD_GROWTH_BIRCH_FOREST
        0xFF387B3D.toInt(), // OLD_GROWTH_PINE_TAIGA
        0xFF688B3A.toInt(), // OLD_GROWTH_SPRUCE_TAIGA
        0xFF648C96.toInt(), // WETLAND
        0xFFF0A0C0.toInt(), // CHERRY_GROVE
        0xFFB8B8C0.toInt(), // PALE_GARDEN
    )

    fun color(temp: Double, humidity: Double, continental: Double): Long {
        val biome = classify(temp, humidity, continental)
        return BIOME_COLORS[biome].toULong().toLong()
    }

    private fun classify(t: Double, h: Double, c: Double): Int {
        // Mushroom fields: very low temperature, very high humidity, near ocean
        if (t < 0.25 && h > 0.75 && c < 0.45) return MUSHROOM_FIELDS

        // Nether-like: extreme heat + low humidity (not a real Overworld biome,
        // but prevents unreasonable bright-yellow in hot/arid zones)
        if (t > 0.9 && h < 0.2) return DESERT

        // Ice / snow: very cold regardless of humidity
        if (t < 0.22) {
            return if (h > 0.55) ICE_SPIKES else SNOW_PLAINS
        }
        if (t < 0.35) {
            return if (h > 0.6) SNOWY_TAIGA else SNOW_PLAINS
        }

        // Cold temperate
        return if (t < 0.55) {
            when {
                h > 0.7 -> TAIGA
                h > 0.45 -> FOREST
                c < 0.35 -> RIVER
                else -> PLAINS
            }
        } else if (t < 0.75) {
            when {
                h > 0.8 -> SWAMP
                h > 0.6 -> JUNGLE
                h > 0.45 -> FOREST_FLOWER
                c < 0.35 -> PLAINS
                c > 0.65 -> MOUNTAINS
                else -> FOREST
            }
        } else {
            when {
                h > 0.7 -> SWAMP
                h > 0.5 -> PLAINS
                h > 0.3 -> SAVANNA
                else -> DESERT
            }
        }
    }
}