package com.portalhost.app.ui.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

fun saveServerIcon(contentResolver: ContentResolver, uri: Uri, destFile: File): Boolean {
    return try {
        destFile.parentFile?.mkdirs()

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null || bytes.isEmpty()) {
            android.util.Log.e("ServerIcon", "No bytes read from URI")
            return false
        }
        android.util.Log.i("ServerIcon", "Read ${bytes.size} bytes from URI")

        // Approach 1: Try to decode as Bitmap, resize to 64x64, re-encode as PNG
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) {
            android.util.Log.i("ServerIcon", "Decoded bitmap: ${bitmap.width}x${bitmap.height} config=${bitmap.config}")
            val resized = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
            destFile.outputStream().use { output ->
                resized.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (resized != bitmap) resized.recycle()
            bitmap.recycle()
            val saved = destFile.exists() && destFile.length() > 0
            android.util.Log.i("ServerIcon", "Saved resized PNG to ${destFile.absolutePath} (${destFile.length()} bytes, exists=$saved)")
            return saved
        }

        // Approach 2: If the image can't be decoded as Bitmap, save raw bytes
        // (works if the user selected a valid PNG that just couldn't be decoded)
        android.util.Log.w("ServerIcon", "Bitmap decode failed, saving raw bytes as fallback")
        destFile.outputStream().use { output ->
            output.write(bytes)
        }
        val saved = destFile.exists() && destFile.length() > 0
        android.util.Log.i("ServerIcon", "Saved raw bytes to ${destFile.absolutePath} (${destFile.length()} bytes, exists=$saved)")
        saved
    } catch (e: Exception) {
        android.util.Log.e("ServerIcon", "Failed to save icon: ${e.message}", e)
        false
    }
}