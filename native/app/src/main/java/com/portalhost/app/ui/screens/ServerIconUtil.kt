package com.portalhost.app.ui.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

fun saveServerIcon(contentResolver: ContentResolver, uri: Uri, destFile: File): Boolean {
    return try {
        contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input) ?: return false
            val resized = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
            destFile.outputStream().use { output ->
                resized.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (resized != bitmap) resized.recycle()
            bitmap.recycle()
            true
        } ?: false
    } catch (e: Exception) { false }
}
