package com.portalhost.server

import com.portalhost.model.ServerBuild
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerType
import com.portalhost.filesystem.FileSystem
import com.portalhost.server.providers.ServerProvider
import com.portalhost.server.providers.ServerProviderRegistry
import com.portalhost.server.providers.ServerProviderRegistryInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import kotlin.Result

class ServerDownloader(
    private val registry: ServerProviderRegistry = ServerProviderRegistryInstance.instance,
    private val fileSystem: FileSystem,
) {
    
    private val _downloadProgress = MutableStateFlow<Double>(0.0)
    val downloadProgress: StateFlow<Double> = _downloadProgress
    
    private val _currentStatus = MutableStateFlow<String>("")
    val currentStatus: StateFlow<String> = _currentStatus

    suspend fun downloadServer(config: ServerConfig): Result<File> = withContext(Dispatchers.IO) {
        _currentStatus.value = "Finding provider for ${config.serverType}"
        
        val provider = registry.getProvidersForType(config.serverType)
            .firstOrNull { p -> 
                // Find provider that supports the source
                when (config.source) {
                    com.portalhost.model.ServerSource.PAPERMC -> p.id == "paper"
                    com.portalhost.model.ServerSource.FOLIA -> p.id == "folia"
                    com.portalhost.model.ServerSource.PURPUR -> p.id == "purpur"
                    com.portalhost.model.ServerSource.OFFICIAL -> p.id == "vanilla"
                    com.portalhost.model.ServerSource.FABRICMC -> p.id == "fabric"
                    com.portalhost.model.ServerSource.FORGE -> p.id == "forge"
                    com.portalhost.model.ServerSource.NEOFORGE -> p.id == "neoforge"
                    else -> true
                }
            } ?: return@withContext Result.failure(Exception("No provider found for ${config.source}"))

        _currentStatus.value = "Fetching builds for ${config.version}"
        val buildsResult = provider.fetchBuilds(config.version)
        
        return@withContext buildsResult.fold(
            onSuccess = { builds ->
                if (builds.isEmpty()) {
                    Result.failure(Exception("No builds found for version ${config.version}"))
                } else {
                    val build = if (config.buildId.isNotBlank()) {
                        builds.find { it.id == config.buildId } ?: builds.first()
                    } else {
                        builds.first()
                    }
                    downloadBuild(provider, build, config)
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun downloadBuild(
        provider: ServerProvider,
        build: ServerBuild,
        config: ServerConfig,
    ): Result<File> = withContext(Dispatchers.IO) {
        val serverFile = File(fileSystem.getServersDirBlocking(), "${config.id}.jar")
        _currentStatus.value = "Downloading ${build.id}..."
        _downloadProgress.value = 0.0
        
        val result = provider.downloadBuild(build, serverFile)
        
        if (result.isSuccess) {
            _downloadProgress.value = 1.0
            _currentStatus.value = "Download complete"
        } else {
            _currentStatus.value = "Download failed: ${result.exceptionOrNull()?.message}"
        }
        
        result
    }

    // Download with progress tracking
    suspend fun downloadWithProgress(url: String, destination: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            destination.parentFile?.mkdirs()
            val connection = URL(url).openConnection().apply {
                connectTimeout = 30000
                readTimeout = 300000
            } as java.net.HttpURLConnection
            
            val contentLength = connection.contentLengthLong
            var downloaded = 0L
            
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (contentLength > 0) {
                            _downloadProgress.value = downloaded.toDouble() / contentLength
                        }
                        bytesRead = input.read(buffer)
                    }
                }
            }
            
            Result.success(destination)
        } catch (e: Exception) {
            destination.delete()
            Result.failure(e)
        }
    }
}
