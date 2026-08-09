package com.portalhost.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.portalhost.app.AndroidUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Handles the in-app update flow:
 *   1. Streams the new APK from the website (latest.json's android.url) into
 *      the app's cache directory under `update/`.
 *   2. Once downloaded, hands the file to the system installer via FileProvider
 *      so the user gets the standard Android "Install" dialog without leaving
 *      the app.
 *
 * The downloaded APK is stored as `portalhost-v{version}.apk` so each cached
 * file is uniquely named (no collisions between hotfixes on the same tag).
 */
object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val UPDATE_DIR = "update"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.MINUTES)
        .followRedirects(true)
        .build()

    private fun cacheFile(context: Context, version: String): File {
        val dir = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
        val safe = version.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "portalhost-v$safe.apk")
    }

    /**
     * Download the APK. Calls [onProgress] with a fraction 0f..1f as bytes
     * arrive. Returns the saved file on success, or a failed Result with the
     * exception (e.g. HTTP error, IO error, content-length unknown).
     */
    suspend fun download(
        context: Context,
        info: AndroidUpdateInfo,
        onProgress: ((Float) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val target = cacheFile(context, info.latestVersion)
        try {
            Log.i(TAG, "Downloading ${info.latestVersion} from ${info.downloadUrl} -> ${target.absolutePath}")
            val request = Request.Builder()
                .url(info.downloadUrl)
                .header("User-Agent", "PortalHost/${com.portalhost.app.BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code} from ${info.downloadUrl}")
                    )
                }
                val body = response.body ?: return@withContext Result.failure(
                    Exception("Empty response body")
                )
                val contentLength = body.contentLength()
                target.parentFile?.mkdirs()
                var downloaded = 0L
                var lastProgressReport = 0L
                body.byteStream().use { input ->
                    FileOutputStream(target).use { output ->
                        val buf = ByteArray(32768)
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                            downloaded += read
                            if (contentLength > 0 &&
                                (downloaded - lastProgressReport > 65536 || downloaded == contentLength)
                            ) {
                                lastProgressReport = downloaded
                                onProgress?.invoke(downloaded.toFloat() / contentLength.toFloat())
                            }
                        }
                    }
                }
                if (contentLength > 0) {
                    onProgress?.invoke(1f)
                }
                Log.i(TAG, "Downloaded ${target.length()} bytes to ${target.absolutePath}")
                Result.success(target)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            runCatching { if (target.exists()) target.delete() }
            Result.failure(e)
        }
    }

    /**
     * Hand the cached APK to the system installer. Uses FileProvider to grant
     * temporary URI permission. On Android 8+ (Oreo) the user must have
     * "Install unknown apps" enabled for our package — if not, the system
     * will show a permission dialog before the install dialog.
     */
    fun install(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Open the update URL in the user's browser (fallback if install fails). */
    fun openInBrowser(context: Context, info: AndroidUpdateInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
