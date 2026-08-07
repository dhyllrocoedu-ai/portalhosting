package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class NeoForgeProvider(
    private val client: OkHttpClient,
    private val json: kotlinx.serialization.json.Json
) : ServerProvider {
    override val type = ServerType.NEOFORGE
    override val supportsBuilds = true
    private val TAG = "NeoForgeProvider"
    private val baseUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/maven-metadata.xml"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            parseXmlVersions(body)
                .filter { it.startsWith("1.") }
                .distinct()
                .sortedByDescending { it }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw ServerProviderException("Failed to load NeoForge versions from $url: ${e.message}", e)
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/$version/maven-metadata.xml"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            parseXmlVersions(body)
                .filter { it.contains(version) && it.length > version.length }
                .distinct()
                .sortedByDescending { it }
                .map { BuildInfo(it, it) }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw ServerProviderException("Failed to load NeoForge builds for $version from $url: ${e.message}", e)
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? {
        val fullVer = buildId.ifBlank {
            val builds = getBuildInfos(version)
            builds.firstOrNull()?.id ?: return null
        }
        val url = "$baseUrl/$fullVer/neoforge-$fullVer-installer.jar"
        return DownloadInfo(url, null, "neoforge-$fullVer-installer.jar")
    }

    private fun parseXmlVersions(xml: String): List<String> {
        val versions = mutableListOf<String>()
        val pattern = "<version>([^<]+)</version>".toRegex()
        pattern.findAll(xml).forEach { match ->
            versions.add(match.groupValues[1])
        }
        return versions
    }
}
