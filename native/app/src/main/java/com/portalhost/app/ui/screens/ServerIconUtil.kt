package com.portalhost.app.ui.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

fun saveServerIcon(contentResolver: ContentResolver, uri: Uri, destFile: File): Boolean {
    return try {
        destFile.parentFile?.mkdirs()

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null || bytes.isEmpty()) {
            Log.e("ServerIcon", "No bytes read from URI")
            return false
        }
        Log.i("ServerIcon", "Read ${bytes.size} bytes from URI")

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) {
            Log.i("ServerIcon", "Decoded bitmap: ${bitmap.width}x${bitmap.height} config=${bitmap.config}")

            // Center-crop to square, then resize to 64x64
            val size = minOf(bitmap.width, bitmap.height)
            val cropped = Bitmap.createBitmap(bitmap, (bitmap.width - size) / 2, (bitmap.height - size) / 2, size, size)
            val resized = Bitmap.createScaledBitmap(cropped, 64, 64, true)
            if (cropped != bitmap) cropped.recycle()
            bitmap.recycle()

            val safe = resized.copy(Bitmap.Config.ARGB_8888, false)
            resized.recycle()

            // Write via Android Skia PNG encoder
            FileOutputStream(destFile).use { out ->
                val ok = safe.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.fd.sync()
                if (!ok) {
                    Log.e("ServerIcon", "Bitmap.compress returned false")
                    safe.recycle()
                    return false
                }
            }
            safe.recycle()

            Log.i("ServerIcon", "Written PNG: ${destFile.length()} bytes")
            val magic = destFile.inputStream().use { it.readBytes().take(4).toByteArray() }
            val hex = magic.joinToString("") { "%02X".format(it) }
            Log.i("ServerIcon", "PNG magic: $hex (expect 89504E47)")
            return hex == "89504E47"
        }

        Log.w("ServerIcon", "Bitmap decode returned null")
        false
    } catch (e: Exception) {
        Log.e("ServerIcon", "Failed to save icon: ${e.message}", e)
        false
    }
}