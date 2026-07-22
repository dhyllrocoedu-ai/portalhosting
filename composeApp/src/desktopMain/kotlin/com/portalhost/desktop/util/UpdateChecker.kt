package com.portalhost.desktop.util

import com.portalhost.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.net.HttpURLConnection
import java.net.URL

private val logger = KotlinLogging.logger {}

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

sealed class UpdateResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateResult()
    object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/dhyllrocoedu-ai/portalhosting/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "PortalHost/${BuildConfig.VERSION_NAME}")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode == 403) {
                return@withContext UpdateResult.Error("GitHub API rate limited. Try again later.")
            }
            if (responseCode != 200) {
                logger.warn { "GitHub API returned $responseCode" }
                return@withContext UpdateResult.Error("GitHub API returned HTTP $responseCode")
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val root = json.parseToJsonElement(body).jsonObject
            val tagName = root["tag_name"]?.jsonPrimitive?.content?.removePrefix("v") ?: ""
            val htmlUrl = root["html_url"]?.jsonPrimitive?.content ?: ""
            val releaseNotes = root["body"]?.jsonPrimitive?.content?.take(200) ?: ""

            if (tagName.isBlank()) {
                return@withContext UpdateResult.Error("Invalid release data from GitHub")
            }

            val currentVersion = BuildConfig.VERSION_NAME
            if (isNewerVersion(tagName, currentVersion)) {
                UpdateResult.UpdateAvailable(
                    UpdateInfo(
                        latestVersion = tagName,
                        downloadUrl = htmlUrl,
                        releaseNotes = releaseNotes,
                    )
                )
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            logger.warn { "Update check failed: ${e.message}" }
            UpdateResult.Error("Network error: ${e.message ?: "Unknown error"}")
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
