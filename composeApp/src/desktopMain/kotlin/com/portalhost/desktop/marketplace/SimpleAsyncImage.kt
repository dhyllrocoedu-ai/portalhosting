package com.portalhost.desktop.marketplace

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.net.HttpURLConnection
import java.net.URL

private const val MAX_CACHE_SIZE = 150
private const val MAX_IMAGE_DIMENSION = 960

object SimpleImageCache {
    private val cache = object : LinkedHashMap<String, ImageBitmap>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean =
            size > MAX_CACHE_SIZE
    }

    @Synchronized
    fun get(url: String): ImageBitmap? = cache[url]

    @Synchronized
    fun put(url: String, bitmap: ImageBitmap) {
        cache[url] = bitmap
    }

    @Synchronized
    fun clear() = cache.clear()
}

private fun decodeAndScale(bytes: ByteArray, maxDimension: Int): ImageBitmap? {
    return try {
        val image = SkiaImage.makeFromEncoded(bytes) ?: return null
        val largest = maxOf(image.width, image.height)
        if (largest <= maxDimension) {
            return image.toComposeImageBitmap()
        }
        val scale = maxDimension.toDouble() / largest
        val targetWidth = (image.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (image.height * scale).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(targetWidth, targetHeight)
        val src = Rect.makeLTRB(0f, 0f, image.width.toFloat(), image.height.toFloat())
        val dst = Rect.makeLTRB(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
        surface.canvas.drawImageRect(image, src, dst, SamplingMode.LINEAR, null, false)
        surface.makeImageSnapshot().toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@Composable
fun SimpleAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: @Composable () -> Unit = { Box(modifier = Modifier.size(24.dp)) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) } },
    errorIcon: @Composable () -> Unit = { Icon(Icons.Default.ImageNotSupported, contentDescription = null, modifier = Modifier.size(48.dp)) }
) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = url) {
        bitmap = null
        loading = false
        error = false

        if (url.isNullOrBlank()) {
            return@LaunchedEffect
        }

        val cached = SimpleImageCache.get(url)
        if (cached != null) {
            bitmap = cached
            return@LaunchedEffect
        }

        loading = true
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "PortalHost/5.0")
                connection.instanceFollowRedirects = true

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    val composeBitmap = decodeAndScale(bytes, MAX_IMAGE_DIMENSION)
                    if (composeBitmap != null) {
                        SimpleImageCache.put(url, composeBitmap)
                        bitmap = composeBitmap
                    } else {
                        error = true
                    }
                } else {
                    error = true
                }
            } catch (e: Exception) {
                error = true
            } finally {
                loading = false
            }
        }
    }

    when {
        bitmap != null -> {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        loading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) { placeholder() }
        }
        error -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) { errorIcon() }
        }
        else -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) { placeholder() }
        }
    }
}
