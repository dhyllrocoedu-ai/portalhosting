package com.portalhost.desktop.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

@Composable
fun rememberResourcePainter(path: String): Painter {
    val classLoader = Thread.currentThread().contextClassLoader
    return remember(path) {
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        val bytes = classLoader.getResourceAsStream(cleanPath)?.readAllBytes()
            ?: error("Resource $path not found")
        BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
    }
}
