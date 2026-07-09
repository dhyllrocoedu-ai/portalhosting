package com.portalhost.app.ui.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

fun saveServerIcon(contentResolver: ContentResolver, uri: Uri, destFile: File): Boolean {
    return try {
        destFile.parentFile?.mkdirs()
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
        val resized = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        destFile.outputStream().use { output ->
            resized.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        if (resized != bitmap) resized.recycle()
        bitmap.recycle()
        android.util.Log.i("ServerIcon", "Saved icon to ${destFile.absolutePath} (${destFile.length()} bytes)")
        true
    } catch (e: Exception) {
        android.util.Log.e("ServerIcon", "Failed to save icon: ${e.message}", e)
        false
    }
}