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
        try {
            val url = "$baseUrl/maven-metadata.xml"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            parseXmlVersions(body)
                .filter { it.startsWith("1.") }
                .distinct()
                .sortedByDescending { it }
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/$version/maven-metadata.xml"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            parseXmlVersions(body)
                .filter { it.contains(version) && it.length > version.length }
                .distinct()
                .sortedByDescending { it }
                .map { BuildInfo(it, it) }
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            emptyList()
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
