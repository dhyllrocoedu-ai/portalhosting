package com.portalhost.server.providers

import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.model.ServerBuild
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import java.security.MessageDigest
import kotlin.Result

class ForgeProvider : ServerProvider {
    override val id = "forge"
    override val name = "Forge"
    override val supportedTypes = setOf(ServerType.FORGE)
    
    private val baseUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge"
    private val json = Json { ignoreUnknownKeys = true }
    private val buildsCache = mutableMapOf<String, List<ServerBuild>>()

    override suspend fun fetchVersions(): Result<List<ServerVersion>> {
        return HttpCache.fetchWithCacheSuspend("$baseUrl/maven-metadata.xml")
            .map { response ->
                parseMavenMetadata(response)
                    .filter { it.startsWith("1.") }
                    .map { ServerVersion(version = it, stable = !it.contains("-"), releaseDate = null) }
                    .sortedWith(compareByDescending { parseSemver(it.version) })
            }
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> {
        buildsCache[version]?.let { return Result.success(it) }
        return HttpCache.fetchWithCacheSuspend("$baseUrl/$version/maven-metadata.xml")
            .map { response ->
                val builds = parseMavenMetadataBuilds(response)
                    .filter { it.contains(version) && it.length > version.length }
                    .map { build ->
                        ServerBuild(
                            id = build,
                            url = "$baseUrl/$build/forge-$build-installer.jar",
                            sha256 = null,
                            size = 0
                        )
                    }
                    .sortedWith(compareByDescending { parseSemver(it.id) })
                buildsCache[version] = builds
                builds
            }
    }

    override suspend fun downloadBuild(
        build: ServerBuild,
        destination: File,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)?
    ): Result<File> = runCatching {
        URL(build.url).downloadToFile(
            destination = destination,
            headers = mapOf("User-Agent" to USER_AGENT),
            onProgress = onProgress
        )
        destination
    }
    
    private fun parseMavenMetadata(xml: String): List<String> {
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
