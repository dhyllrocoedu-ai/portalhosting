package com.portalhost.desktop.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Deterministic Minecraft-style head icon based on player name hash
private val HEAD_SKINS = listOf(
    // Steve
    listOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 2, 2, 2, 2, 2, 2, 0),
        intArrayOf(0, 2, 3, 2, 2, 3, 2, 0),
        intArrayOf(0, 2, 2, 2, 2, 2, 2, 0),
        intArrayOf(0, 0, 4, 4, 4, 4, 0, 0),
        intArrayOf(0, 0, 4, 4, 4, 4, 0, 0),
        intArrayOf(0, 0, 5, 5, 5, 5, 0, 0),
    ),
    // Alex
    listOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 6, 6, 6, 6, 0, 0),
        intArrayOf(0, 6, 3, 6, 6, 3, 6, 0),
        intArrayOf(0, 6, 6, 6, 6, 6, 6, 0),
        intArrayOf(0, 0, 4, 4, 4, 4, 0, 0),
        intArrayOf(0, 0, 4, 4, 4, 4, 0, 0),
        intArrayOf(0, 0, 5, 5, 5, 5, 0, 0),
    ),
    // Zombie
    listOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 7, 7, 7, 7, 7, 7, 0),
        intArrayOf(0, 7, 8, 7, 7, 8, 7, 0),
        intArrayOf(0, 7, 7, 7, 7, 7, 7, 0),
        intArrayOf(0, 0, 4, 4, 4, 4, 0, 0),
        intArrayOf(0, 0, 4, 4, 4, 4, 0, 0),
        intArrayOf(0, 0, 7, 7, 7, 7, 0, 0),
    ),
    // Skeleton
    listOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 9, 9, 9, 9, 9, 9, 0),
        intArrayOf(0, 9, 8, 9, 9, 8, 9, 0),
        intArrayOf(0, 9, 9, 9, 9, 9, 9, 0),
        intArrayOf(0, 0, 8, 8, 8, 8, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
    ),
)

private val SKIN_COLORS = listOf(
    Color(0x00000000), // 0: Transparent (skip)
    Color(0xFFB87C4C), // 1: Skin (Steve)
    Color(0xFF553D2A), // 2: Hair (brown)
    Color(0xFF000000), // 3: Eyes
    Color(0xFF6B4226), // 4: Mouth/shirt dark
    Color(0xFF4A2A14), // 5: Shirt
    Color(0xFFCC6633), // 6: Hair (red/orange, Alex)
    Color(0xFF7A7A7A), // 7: Zombie/Skeleton skin
    Color(0xFF000000), // 8: Eyes (dark)
    Color(0xFFE8E8E0), // 9: Skeleton bone
)

@Composable
fun MinecraftHeadIcon(player: String, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val skinIndex = remember(player) {
        val hash = player.hashCode()
        if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash) % HEAD_SKINS.size
    }
    val skin = HEAD_SKINS[skinIndex]
    val pixelSize = size.value / 8

    Canvas(modifier = modifier.size(size)) {
        for (row in skin.indices) {
            for (col in skin[row].indices) {
                val colorIndex = skin[row][col]
                if (colorIndex > 0 && colorIndex < SKIN_COLORS.size) {
                    drawRect(
                        color = SKIN_COLORS[colorIndex],
                        topLeft = Offset(col * pixelSize, row * pixelSize),
                        size = Size(pixelSize, pixelSize),
                    )
                }
            }
        }
    }
}
