package com.portalhost.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GrassGreen = Color(0xFF5D8B3C)
private val GrassBrown = Color(0xFF8B5E3C)
private val DirtBrown = Color(0xFF6B3E1E)
private val StoneGray = Color(0xFF7A7A7A)
private val IronGray = Color(0xFFB0B0B0)
private val WoodBrown = Color(0xFF8B6914)
private val WoodLight = Color(0xFFC4A44A)
private val CmdBlack = Color(0xFF1A1A2E)
private val CmdPurple = Color(0xFF7B2D8B)
private val CmdCyan = Color(0xFF00E5FF)
private val RedStone = Color(0xFFCC3333)
private val GoldBlock = Color(0xFFE6B800)
private val GoldDark = Color(0xFFCC9900)
private val WaterBlue = Color(0xFF3366CC)
private val SkinTan = Color(0xFFD2A77A)
private val SkinDark = Color(0xFF8B5E3C)
private val EyeBlue = Color(0xFF3388FF)

@Composable
fun GrassIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PixelArt(modifier = modifier, size = size, pixels = listOf(
        listOf(0, 0, 0, 1, 1, 0, 0, 0),
        listOf(0, 0, 1, 1, 1, 1, 0, 0),
        listOf(0, 1, 1, 2, 2, 1, 1, 0),
        listOf(0, 1, 1, 2, 2, 1, 1, 0),
        listOf(0, 0, 2, 2, 2, 2, 0, 0),
        listOf(0, 0, 2, 2, 2, 2, 0, 0),
        listOf(0, 0, 2, 2, 2, 2, 0, 0),
        listOf(0, 0, 0, 2, 2, 0, 0, 0),
    ), colors = mapOf(0 to Color.Transparent, 1 to GrassGreen, 2 to DirtBrown))
}

@Composable
fun CraftingIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PixelArt(modifier = modifier, size = size, pixels = listOf(
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
        listOf(1, 1, 2, 1, 1, 2, 1, 1),
        listOf(1, 2, 2, 3, 3, 2, 2, 1),
        listOf(1, 1, 2, 2, 2, 2, 1, 1),
        listOf(1, 1, 2, 2, 2, 2, 1, 1),
        listOf(1, 2, 2, 3, 3, 2, 2, 1),
        listOf(1, 1, 2, 1, 1, 2, 1, 1),
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
    ), colors = mapOf(0 to Color.Transparent, 1 to WoodBrown, 2 to WoodLight, 3 to StoneGray))
}

@Composable
fun PickaxeIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PixelArt(modifier = modifier, size = size, pixels = listOf(
        listOf(0, 0, 0, 0, 0, 0, 0, 1),
        listOf(0, 0, 0, 0, 0, 0, 1, 0),
        listOf(0, 0, 0, 0, 0, 1, 0, 0),
        listOf(0, 0, 1, 1, 1, 2, 0, 0),
        listOf(0, 1, 3, 3, 3, 2, 0, 0),
        listOf(1, 3, 3, 3, 2, 0, 0, 0),
        listOf(0, 0, 0, 0, 1, 0, 0, 0),
        listOf(0, 0, 0, 1, 0, 0, 0, 0),
    ), colors = mapOf(0 to Color.Transparent, 1 to WoodBrown, 2 to StoneGray, 3 to IronGray))
}

@Composable
fun PlayerIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PixelArt(modifier = modifier, size = size, pixels = listOf(
        listOf(0, 0, 0, 1, 1, 0, 0, 0),
        listOf(0, 0, 1, 1, 1, 1, 0, 0),
        listOf(0, 0, 1, 2, 2, 1, 0, 0),
        listOf(0, 0, 1, 1, 1, 1, 0, 0),
        listOf(0, 1, 3, 1, 1, 3, 1, 0),
        listOf(1, 3, 3, 3, 3, 3, 3, 1),
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
    ), colors = mapOf(0 to Color.Transparent, 1 to SkinTan, 2 to EyeBlue, 3 to SkinDark))
}

@Composable
fun ChestIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PixelArt(modifier = modifier, size = size, pixels = listOf(
        listOf(0, 0, 0, 0, 0, 0, 0, 0),
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
        listOf(0, 1, 2, 2, 2, 2, 1, 0),
        listOf(0, 1, 2, 1, 1, 2, 1, 0),
        listOf(0, 1, 2, 1, 1, 2, 1, 0),
        listOf(0, 1, 2, 2, 2, 2, 1, 0),
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
        listOf(0, 0, 0, 0, 0, 0, 0, 0),
    ), colors = mapOf(0 to Color.Transparent, 1 to WoodBrown, 2 to GoldBlock))
}

@Composable
fun CmdBlockIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PixelArt(modifier = modifier, size = size, pixels = listOf(
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
        listOf(1, 1, 1, 1, 1, 1, 1, 1),
        listOf(1, 1, 2, 2, 2, 2, 1, 1),
        listOf(1, 1, 2, 3, 3, 2, 1, 1),
        listOf(1, 1, 2, 3, 3, 2, 1, 1),
        listOf(1, 1, 2, 2, 2, 2, 1, 1),
        listOf(1, 1, 1, 1, 1, 1, 1, 1),
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
    ), colors = mapOf(0 to Color.Transparent, 1 to CmdBlack, 2 to CmdPurple, 3 to CmdCyan))
}

@Composable
fun RedstoneIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PixelArt(modifier = modifier, size = size, pixels = listOf(
        listOf(0, 0, 0, 0, 0, 0, 0, 0),
        listOf(0, 0, 0, 1, 1, 0, 0, 0),
        listOf(0, 0, 0, 1, 1, 0, 0, 0),
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
        listOf(0, 1, 1, 1, 1, 1, 1, 0),
        listOf(0, 0, 0, 1, 1, 0, 0, 0),
        listOf(0, 0, 0, 1, 1, 0, 0, 0),
        listOf(0, 0, 0, 0, 0, 0, 0, 0),
    ), colors = mapOf(0 to Color.Transparent, 1 to RedStone))
}

@Composable
fun WaterIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PixelArt(modifier = modifier, size = size, pixels = listOf(
        listOf(0, 0, 1, 0, 1, 0, 1, 0),
        listOf(0, 1, 0, 1, 0, 1, 0, 0),
        listOf(0, 0, 1, 0, 1, 0, 1, 0),
        listOf(0, 0, 0, 1, 0, 1, 0, 0),
        listOf(0, 0, 1, 0, 1, 0, 1, 0),
        listOf(0, 1, 0, 1, 0, 1, 0, 0),
        listOf(0, 0, 1, 0, 1, 0, 1, 0),
        listOf(0, 0, 0, 1, 0, 0, 0, 0),
    ), colors = mapOf(0 to Color.Transparent, 1 to WaterBlue))
}

// Generic 8x8 pixel art renderer
@Composable
private fun PixelArt(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    pixels: List<List<Int>>,
    colors: Map<Int, Color>
) {
    val rows = pixels.size
    val cols = pixels.firstOrNull()?.size ?: rows
    val pixelSize = size / maxOf(rows, cols).toFloat()

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            pixels.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    row.forEach { colorIdx ->
                        val color = colors[colorIdx] ?: Color.Transparent
                        Box(
                            modifier = Modifier
                                .size(pixelSize)
                                .then(
                                    if (color != Color.Transparent) Modifier.background(color)
                                    else Modifier
                                )
                        )
                    }
                }
            }
        }
    }
}
