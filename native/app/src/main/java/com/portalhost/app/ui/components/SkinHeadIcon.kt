package com.portalhost.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.portalhost.app.server.SkinService

@Composable
fun SkinHeadIcon(
    player: String,
    skinService: SkinService,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    var skinBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(player) {
        if (skinBitmap == null) {
            skinBitmap = skinService.fetch(player)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, Color(0xFF444444), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = skinBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "$player skin",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            MinecraftHeadIcon(player = player, modifier = Modifier.size(size * 0.85f))
        }
    }
}
