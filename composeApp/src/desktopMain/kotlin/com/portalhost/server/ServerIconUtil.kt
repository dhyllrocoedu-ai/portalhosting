package com.portalhost.server

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun saveServerIcon(sourceFile: File, destFile: File): Boolean {
    return try {
        destFile.parentFile?.mkdirs()
        val img = ImageIO.read(sourceFile) ?: return false
        val resized = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
        val g2d = resized.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.drawImage(img, 0, 0, 64, 64, null)
        g2d.dispose()
        ImageIO.write(resized, "png", destFile)
        true
    } catch (_: Exception) {
        false
    }
}

fun loadServerIcon(iconFile: File): ImageBitmap? {
    return try {
        if (!iconFile.exists()) return null
        val bytes = iconFile.readBytes()
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}

fun getServerIconFile(serverDir: File): File {
    return File(serverDir, "server-icon.png")
}
