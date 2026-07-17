package com.portalhost.server.providers

import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.model.ServerBuild
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import kotlin.Result

class NeoForgeProvider : ServerProvider {
    override val id = "neoforge"
    override val name = "NeoForge"
    override val supportedTypes = setOf(ServerType.NEOFORGE)
    
    private val baseUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchVersions(): Result<List<ServerVersion>> {
        return try {
            val url = URL("$baseUrl/maven-metadata.xml")
            val response = url.readTextWithTimeout()
            val versions = parseMavenMetadata(response)
                .filter { it.startsWith("1.") }
                .map { ServerVersion(version = it, stable = !it.contains("-"), releaseDate = null) }
                .reversed()
            Result.success(versions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> {
        return try {
            val url = URL("$baseUrl/$version/maven-metadata.xml")
            val response = url.readTextWithTimeout()
            val builds = parseMavenMetadataBuilds(response)
                .filter { it.contains(version) && it.length > version.length }
                .map { build ->
                    ServerBuild(
                        id = build,
                        url = "$baseUrl/$build/neoforge-$build-installer.jar",
                        sha256 = null,
                        size = 0
                    )
                }
                .reversed()
            Result.success(builds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadBuild(build: ServerBuild, destination: File): Result<File> = runCatching {
        destination.parentFile?.mkdirs()
        val url = URL(build.url)
        val conn = url.openConnection()
        conn.connectTimeout = 30000
        conn.readTimeout = 300000
        conn.getInputStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
        destination
    }
    
    private fun parseMavenMetadata(xml: String): List<String> {
        // Simple XML parsing for <version> tags
        val versions = mutableListOf<String>()
        val pattern = "<version>([^<]+)</version>".toRegex()
        pattern.findAll(xml).forEach { match ->
            versions.add(match.groupValues[1])
        }
        return versions
    }
    
    private fun parseMavenMetadataBuilds(xml: String): List<String> {
        val builds = mutableListOf<String>()
        val pattern = "<version>([^<]+)</version>".toRegex()
        pattern.findAll(xml).forEach { match ->
            builds.add(match.groupValues[1])
        }
        return builds.distinct()
    }
}
