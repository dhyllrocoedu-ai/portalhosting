package com.portalhost.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AndroidUpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseDate: String,
    val sizeBytes: Long,
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AndroidUpdateInfo) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    object Error : UpdateCheckResult()
}

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val LATEST_URL = "https://portalhost.pages.dev/latest.json"
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_URL)
                .header("User-Agent", "PortalHost/${BuildConfig.VERSION_NAME}")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "latest.json returned HTTP ${response.code}")
                return@withContext UpdateCheckResult.Error
            }
            val body = response.body?.string() ?: return@withContext UpdateCheckResult.Error
            val root = json.decodeFromString<LatestJson>(body)
            val entry = root.android ?: return@withContext UpdateCheckResult.Error
            if (entry.version.isBlank() || entry.url.isBlank()) {
                return@withContext UpdateCheckResult.Error
            }
            if (isNewerVersion(entry.version, BuildConfig.VERSION_NAME)) {
                UpdateCheckResult.UpdateAvailable(
                    AndroidUpdateInfo(
                        latestVersion = entry.version,
                        downloadUrl = entry.url,
                        releaseDate = entry.date,
                        sizeBytes = entry.size,
                    )
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkForUpdate: ${e.message}")
            UpdateCheckResult.Error
        }
    }

    @Serializable
    private data class LatestJson(
        val version: String = "",
        val date: String = "",
        val android: AndroidEntry? = null,
    )

    @Serializable
    private data class AndroidEntry(
        val version: String = "",
        val date: String = "",
        val url: String = "",
        val size: Long = 0L,
    )

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = clean(remote).split(".").mapNotNull { it.toIntOrNull() }
        val localParts = clean(local).split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun clean(version: String): String {
        return version.trim().removePrefix("v").substringBefore("-")
    }
}
