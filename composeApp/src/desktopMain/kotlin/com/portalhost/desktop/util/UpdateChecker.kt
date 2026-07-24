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
    private const val WEBSITE_LATEST_URL = "https://portalhost.pages.dev/latest.json"
    private const val GITHUB_API_URL = "https://api.github.com/repos/dhyllrocoedu-ai/portalhosting/releases/latest"
    private const val GITHUB_HTML_URL = "https://github.com/dhyllrocoedu-ai/portalhosting/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }

    var githubToken: String? = null
        private set

    suspend fun checkForUpdate(token: String? = null): UpdateResult = withContext(Dispatchers.IO) {
        token?.takeIf { it.isNotBlank() }?.let { githubToken = it }
        
        // Primary: check website (public, no auth needed)
        val websiteResult = tryWebsiteCheck()
        if (websiteResult != null) return@withContext websiteResult
        
        // Fallback: GitHub API
        tryGithubApiCheck()
    }

    private fun tryWebsiteCheck(): UpdateResult? {
        try {
            val connection = URL(WEBSITE_LATEST_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "PortalHost/${BuildConfig.VERSION_NAME}")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != 200) {
                logger.warn { "Website latest.json returned ${connection.responseCode}" }
                connection.disconnect()
                return null
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val root = json.parseToJsonElement(body).jsonObject
            val version = root["version"]?.jsonPrimitive?.content ?: ""
            val msiUrl = root["msi"]?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
            val exeUrl = root["exe"]?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
            val date = root["date"]?.jsonPrimitive?.content ?: ""
            
            // Use MSI as primary download URL, fallback to EXE
            val downloadUrl = if (msiUrl.isNotBlank()) msiUrl else exeUrl
            val releaseNotes = "Released $date. See website for full changelog."

            if (version.isBlank() || downloadUrl.isBlank()) {
                logger.warn { "Invalid data in latest.json" }
                return null
            }

            val currentVersion = BuildConfig.VERSION_NAME
            if (isNewerVersion(version, currentVersion)) {
                return UpdateResult.UpdateAvailable(
                    UpdateInfo(
                        latestVersion = version,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes,
                    )
                )
            } else {
                return UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            logger.warn { "Website update check failed: ${e.message}" }
            return null
        }
    }

    private fun tryGithubApiCheck(): UpdateResult {
        try {
            val connection = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "PortalHost/${BuildConfig.VERSION_NAME}")
            
            githubToken?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode == 403) {
                return UpdateResult.Error("GitHub API rate limited. Try again later.")
            }
            if (responseCode == 404 || responseCode != 200) {
                logger.warn { "GitHub API returned $responseCode, trying HTML fallback" }
                return tryHtmlFallback()
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val root = json.parseToJsonElement(body).jsonObject
            val tagName = root["tag_name"]?.jsonPrimitive?.content?.removePrefix("v") ?: ""
            val htmlUrl = root["html_url"]?.jsonPrimitive?.content ?: ""
            val releaseNotes = root["body"]?.jsonPrimitive?.content?.take(200) ?: ""

            if (tagName.isBlank()) {
                return UpdateResult.Error("Invalid release data from GitHub")
            }

            val currentVersion = BuildConfig.VERSION_NAME
            return if (isNewerVersion(tagName, currentVersion)) {
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
            return tryHtmlFallback()
        }
    }

    private fun tryHtmlFallback(): UpdateResult {
        try {
            val connection = URL(GITHUB_HTML_URL).openConnection() as HttpURLConnection
            
            githubToken?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            
            connection.setRequestProperty("User-Agent", "PortalHost/${BuildConfig.VERSION_NAME}")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                connection.disconnect()
                return UpdateResult.Error("Unable to check for updates (HTTP $responseCode)")
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val tagMatch = Regex("""/releases/tag/v?([0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9]+)?)""").find(body)
            val notesMatch = Regex("""<h2[^>]*>([^<]+)</h2>""").find(body)

            val tagName = tagMatch?.groupValues?.getOrNull(1) ?: ""
            val releaseNotes = notesMatch?.groupValues?.getOrNull(1)?.take(200) ?: ""

            if (tagName.isBlank()) {
                return UpdateResult.Error("Unable to parse release info from GitHub")
            }

            val currentVersion = BuildConfig.VERSION_NAME
            return if (isNewerVersion(tagName, currentVersion)) {
                UpdateResult.UpdateAvailable(
                    UpdateInfo(
                        latestVersion = tagName,
                        downloadUrl = GITHUB_HTML_URL,
                        releaseNotes = releaseNotes,
                    )
                )
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            logger.warn { "HTML fallback update check failed: ${e.message}" }
            return UpdateResult.Error("Network error: ${e.message ?: "Unknown error"}")
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
