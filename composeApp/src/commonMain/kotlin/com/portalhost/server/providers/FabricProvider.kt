package com.portalhost.server.providers

import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.model.ServerBuild
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.Result

class FabricProvider : ServerProvider {
    override val id = "fabric"
    override val name = "Fabric"
    override val supportedTypes = setOf(ServerType.FABRIC)
    
    private val metaUrl = "https://meta.fabricmc.net/v2"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class FabricVersion(
        val version: String,
        val stable: Boolean,
    )
    
    @Serializable
    data class FabricLoader(
        val loader: Loader,
    )
    
    @Serializable
    data class Loader(
        val version: String,
        val stable: Boolean,
    )
    
    @Serializable
    data class FabricInstaller(
        val maven: String,
        val url: String,
    )

    override suspend fun fetchVersions(): Result<List<ServerVersion>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$metaUrl/versions/game")
            val response = url.readText()
            val versions = json.decodeFromString<List<FabricVersion>>(response)
            
            Result.success(versions
                .filter { it.stable }
                .map { ServerVersion(it.version, it.stable, null) }
                .sortedByDescending { it.version }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> = withContext(Dispatchers.IO) {
        try {
            // Get latest stable loader
            val loaderUrl = URL("$metaUrl/versions/loader/$version")
            val loaderResponse = loaderUrl.readText()
            val loader = json.decodeFromString<FabricLoader>(loaderResponse)
            
            // Get installer
            val installerUrl = URL("$metaUrl/versions/installer")
            val installerResponse = installerUrl.readText()
            val installer = json.decodeFromString<FabricInstaller>(installerResponse)
            
            Result.success(listOf(ServerBuild(
                id = "fabric-${loader.loader.version}",
                url = installer.url,
                sha256 = null,
                size = 0
            )))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadBuild(build: ServerBuild, destination: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            destination.parentFile?.mkdirs()
            val url = URL(build.url)
            url.openStream().use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(destination)
        } catch (e: Exception) {
            destination.delete()
            Result.failure(e)
        }
    }
}
