package com.portalhost.desktop.util

import com.portalhost.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private val logger = KotlinLogging.logger {}

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val changelog: List<ChangelogEntry> = emptyList(),
)

@Serializable
data class ChangelogEntry(
    val version: String = "",
    val date: String = "",
    val desktop: List<String> = emptyList(),
    val mobile: List<String> = emptyList(),
)

sealed class UpdateResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateResult()
    object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateChecker {
    private const val WEBSITE_LATEST_URL = "https://portalhost.pages.dev/latest.json"
    private const val WEBSITE_CHANGELOG_URL = "https://portalhost.pages.dev/changelog.json"
    private const val GITHUB_API_URL = "https://api.github.com/repos/dhyllrocoedu-ai/portalhosting/releases/latest"
    private const val GITHUB_HTML_URL = "https://github.com/dhyllrocoedu-ai/portalhosting/releases/latest"
    private const val GITHUB_DOWNLOAD_URL = "https://github.com/dhyllrocoedu-ai/portalhosting/releases/latest/download"
    private const val CACHE_DURATION_MS = 3_600_000L // 1 hour
    private val json = Json { ignoreUnknownKeys = true }

    var githubToken: String? = null
        private set

    private var lastCheckTime = 0L
    private var cachedResult: UpdateResult? = null

    suspend fun checkForUpdate(token: String? = null, forceCheck: Boolean = false): UpdateResult = withContext(Dispatchers.IO) {
        token?.takeIf { it.isNotBlank() }?.let { githubToken = it }

        if (!forceCheck && cachedResult != null && System.currentTimeMillis() - lastCheckTime < CACHE_DURATION_MS) {
            return@withContext cachedResult!!
        }

        val websiteResult = tryWebsiteCheck()
        if (websiteResult != null) {
            cachedResult = websiteResult
            lastCheckTime = System.currentTimeMillis()
            return@withContext websiteResult
        }

        val githubResult = tryGithubApiCheck()
        cachedResult = githubResult
        lastCheckTime = System.currentTimeMillis()
        githubResult
    }

    suspend fun fetchChangelog(): List<ChangelogEntry> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(WEBSITE_CHANGELOG_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "PortalHost/${BuildConfig.VERSION_NAME}")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != 200) {
                connection.disconnect()
                return@withContext emptyList()
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val root = json.parseToJsonElement(body).jsonObject
            val versions = root["versions"]?.jsonObject ?: return@withContext emptyList()

            versions.entries.mapNotNull { (key, value) ->
                try {
                    val obj = value.jsonObject
                    val platform = obj["platform"]?.jsonPrimitive?.content ?: "desktop"
                    if (platform != "desktop") return@mapNotNull null
                    ChangelogEntry(
                        version = key,
                        date = obj["date"]?.jsonPrimitive?.content ?: "",
                        desktop = obj["desktop"]?.let { json.decodeFromJsonElement<List<String>>(it) } ?: emptyList(),
                        mobile = obj["mobile"]?.let { json.decodeFromJsonElement<List<String>>(it) } ?: emptyList(),
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            logger.warn { "Failed to fetch changelog: ${e.message}" }
            emptyList()
        }
    }

    suspend fun downloadUpdate(
        url: String,
        destination: File,
        onProgress: (downloaded: Long, total: Long, speed: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            destination.parentFile?.mkdirs()
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "PortalHost/${BuildConfig.VERSION_NAME}")
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            if (connection.responseCode != 200) {
                return@withContext Result.failure(Exception("HTTP ${connection.responseCode}"))
            }

            val contentLength = connection.contentLengthLong.coerceAtLeast(0)
            var downloaded = 0L
            var lastTime = System.currentTimeMillis()
            var lastBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(65536)
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            val speed = ((downloaded - lastBytes) * 1000 / elapsed)
                            onProgress(downloaded, contentLength, speed)
                            lastTime = now
                            lastBytes = downloaded
                        }

                        bytesRead = input.read(buffer)
                    }
                    onProgress(downloaded, contentLength, 0)
                }
            }
            connection.disconnect()

            Result.success(destination)
        } catch (e: Exception) {
            logger.warn { "Download failed: ${e.message}" }
            destination.delete()
            Result.failure(e)
        }
    }

    private suspend fun tryWebsiteCheck(): UpdateResult? {
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
            val msiSize = root["msi"]?.jsonObject?.get("size")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            val exeUrl = root["exe"]?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
            val exeSize = root["exe"]?.jsonObject?.get("size")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            val date = root["date"]?.jsonPrimitive?.content ?: ""

            val downloadUrl = if (msiUrl.isNotBlank()) msiUrl else exeUrl
            val fileSize = if (msiUrl.isNotBlank()) msiSize else exeSize
            val releaseNotes = "Released $date. See website for full changelog."

            if (version.isBlank() || downloadUrl.isBlank()) {
                logger.warn { "Invalid data in latest.json" }
                return null
            }

            val currentVersion = BuildConfig.VERSION_NAME
            if (isNewerVersion(version, currentVersion)) {
                val changelog = try { fetchChangelog() } catch (_: Exception) { emptyList() }
                return UpdateResult.UpdateAvailable(
                    UpdateInfo(
                        latestVersion = version,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes,
                        changelog = changelog.filter { it.version == version || it.version == "v$version" },
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
                        downloadUrl = "$GITHUB_DOWNLOAD_URL/v$tagName/PortalHost-$tagName.msi",
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
                        downloadUrl = "$GITHUB_DOWNLOAD_URL/v$tagName/PortalHost-$tagName.msi",
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