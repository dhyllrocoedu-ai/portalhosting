package com.portalhost.app.server

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.portalhost.app.server.providers.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ServerDownloader {
    private val TAG = "ServerDownloader"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "PortalHost/1.0 (https://github.com/user/PortalHost)")
                .build()
            chain.proceed(request)
        }
        .build()

    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    fun getProvider(type: ServerType): ServerProvider = when (type) {
        ServerType.PAPER -> PaperProvider(client, json)
        ServerType.VANILLA -> VanillaProvider(client, json)
        ServerType.FABRIC -> FabricProvider(client, json)
        ServerType.FORGE -> ForgeProvider(client, json)
    }

    suspend fun download(
        url: String,
        destFile: File,
        sha256: String? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Downloading from: $url")
            destFile.parentFile?.mkdirs()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $url"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buf = ByteArray(32768)
                    var totalRead = 0L
                    var read: Int
                    var lastProgressReport = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        totalRead += read
                        if (contentLength > 0 && totalRead - lastProgressReport > 65536) {
                            lastProgressReport = totalRead
                            onProgress?.invoke(totalRead, contentLength)
                        }
                    }
                    if (contentLength > 0) {
                        onProgress?.invoke(totalRead, contentLength)
                    }
                }
            }

            Log.i(TAG, "Downloaded ${destFile.length()} bytes to ${destFile.absolutePath}")

            if (sha256 != null) {
                val actual = sha256sum(destFile)
                if (actual != sha256.lowercase()) {
                    destFile.delete()
                    return@withContext Result.failure(Exception("SHA-256 mismatch: expected $sha256, got $actual"))
                }
                Log.i(TAG, "SHA-256 verified")
            }

            destFile.setReadable(true)
            Result.success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            if (destFile.exists()) destFile.delete()
            Result.failure(e)
        }
    }

    private fun sha256sum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(32768)
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
