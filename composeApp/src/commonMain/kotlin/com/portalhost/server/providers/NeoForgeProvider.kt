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
    private val buildsCache = mutableMapOf<String, List<ServerBuild>>()
    private var cachedMetadataXml: String? = null

    override suspend fun fetchVersions(): Result<List<ServerVersion>> {
        return HttpCache.fetchWithCacheSuspend("$baseUrl/maven-metadata.xml")
            .map { response ->
                cachedMetadataXml = response
                val allVersions = parseMavenMetadata(response)
                val mcVersions = mutableSetOf<String>()
                for (v in allVersions) {
                    when {
                        Regex("""1\.\d+(\.\d+)?-.+""").matches(v) -> {
                            mcVersions.add(v.substringBefore('-'))
                        }
                        else -> {
                            val match = Regex("""^(\d+)\.(\d+)""").find(v)
                            if (match != null) {
                                mcVersions.add("1.${match.groupValues[1]}")
                            }
                        }
                    }
                }
                mcVersions
                    .map { ServerVersion(version = it, stable = !it.contains("-"), releaseDate = null) }
                    .sortedWith(compareByDescending { parseSemver(it.version) })
            }
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> {
        buildsCache[version]?.let { return Result.success(it) }
        val xml = cachedMetadataXml
        return if (xml != null) {
            Result.success(parseBuildsFromXml(xml, version))
        } else {
            HttpCache.fetchWithCacheSuspend("$baseUrl/maven-metadata.xml")
                .map { response ->
                    cachedMetadataXml = response
                    parseBuildsFromXml(response, version)
                }
        }
    }

    private fun parseBuildsFromXml(xml: String, version: String): List<ServerBuild> {
        val allVersions = parseMavenMetadataBuilds(xml)
        val neoPrefix = version.removePrefix("1.")
        val matchingBuilds = allVersions.filter { v ->
            v.startsWith("$version-") ||
                (neoPrefix.isNotEmpty() && neoPrefix.all { it.isDigit() || it == '.' } &&
                    v.startsWith(neoPrefix + "."))
        }
        val builds = matchingBuilds
            .map { build ->
                ServerBuild(
                    id = build,
                    url = "$baseUrl/$build/neoforge-$build-installer.jar",
                    sha256 = null,
                    size = 0
                )
            }
            .sortedWith(compareByDescending { parseSemver(it.id) })
        buildsCache[version] = builds
        return builds
    }

    override suspend fun downloadBuild(build: ServerBuild, destination: File): Result<File> = runCatching {
        destination.parentFile?.mkdirs()
        val url = URL(build.url)
        val conn = url.openConnection()
        conn.connectTimeout = 30000
        conn.readTimeout = 300000
        val contentLength = conn.contentLengthLong
        var downloaded = 0L
        conn.getInputStream().use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    bytesRead = input.read(buffer)
                }
            }
        }
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
