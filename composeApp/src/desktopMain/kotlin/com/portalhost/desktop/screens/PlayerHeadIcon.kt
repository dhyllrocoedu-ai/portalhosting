package com.portalhost.desktop.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Shows the real Mojang skin head (the 8x8 head region of the 64x64 skin texture)
 * when a skin bitmap is available, falling back to the deterministic pixel-art
 * [MinecraftHeadIcon] otherwise.
 */
@Composable
fun PlayerHeadIcon(
    playerName: String,
    skinBitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    if (skinBitmap != null) {
        Canvas(modifier = modifier.size(size)) {
            drawImage(
                image = skinBitmap,
                srcOffset = IntOffset(8, 8),
                srcSize = IntSize(8, 8),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.value.roundToInt(), size.value.roundToInt()),
            )
        }
    } else {
        MinecraftHeadIcon(player = playerName, modifier = modifier, size = size)
    }
}
