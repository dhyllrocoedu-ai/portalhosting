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
    data class FabricLoaderEntry(
        val loader: Loader,
    )
    
    @Serializable
    data class Loader(
        val version: String,
        val stable: Boolean,
    )
    
    @Serializable
    data class FabricInstallerEntry(
        val maven: String,
        val version: String,
        val url: String,
    )

    override suspend fun fetchVersions(): Result<List<ServerVersion>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$metaUrl/versions/game")
            val response = url.readTextWithTimeout()
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
            val loaderUrl = URL("$metaUrl/versions/loader/$version")
            val loaderResponse = loaderUrl.readTextWithTimeout()
            val loaders = json.decodeFromString<List<FabricLoaderEntry>>(loaderResponse)
            
            Result.success(loaders.map { entry ->
                val loaderVer = entry.loader.version
                ServerBuild(
                    id = "fabric-$loaderVer",
                    url = "$metaUrl/versions/loader/$version/$loaderVer/server/jar",
                    sha256 = null,
                    size = 0
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadBuild(build: ServerBuild, destination: File): Result<File> = withContext(Dispatchers.IO) {
        try {
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
            Result.success(destination)
        } catch (e: Exception) {
            destination.delete()
            Result.failure(e)
        }
    }
}
