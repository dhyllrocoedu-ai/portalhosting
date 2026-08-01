package com.portalhost.server

import com.portalhost.model.ServerBuild
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerType
import com.portalhost.filesystem.FileSystem
import com.portalhost.server.providers.ServerProvider
import com.portalhost.server.providers.ServerProviderRegistry
import com.portalhost.server.providers.ServerProviderRegistryInstance
import com.portalhost.server.providers.downloadToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.Result

private val logger = KotlinLogging.logger {}

class ServerDownloader(
    private val registry: ServerProviderRegistry = ServerProviderRegistryInstance.instance,
    private val fileSystem: FileSystem,
) {
    
    private val _downloadProgress = MutableStateFlow<Double>(0.0)
    val downloadProgress: StateFlow<Double> = _downloadProgress
    
    private val _currentStatus = MutableStateFlow<String>("")
    val currentStatus: StateFlow<String> = _currentStatus

    suspend fun downloadServer(config: ServerConfig): Result<File> = withContext(Dispatchers.IO) {
        _downloadProgress.value = 0.0
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

        _downloadProgress.value = 0.02
        _currentStatus.value = "Fetching builds for ${config.version}"
        val buildsResult = provider.fetchBuilds(config.version)
        
        return@withContext buildsResult.fold(
            onSuccess = { builds ->
                _downloadProgress.value = 0.06
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
        val isForge = config.source == com.portalhost.model.ServerSource.FORGE ||
            config.source == com.portalhost.model.ServerSource.NEOFORGE
        val serverDir = if (isForge) {
            File(fileSystem.getServersDirBlocking(), sanitizeFolderName(config.name)).apply { mkdirs() }
        } else {
            fileSystem.getServersDirBlocking()
        }
        val serverFile = File(serverDir, "${config.id}.jar")

        var lastError: Throwable? = null
        repeat(2) { attempt ->
            _downloadProgress.value = 0.1
            if (attempt > 0) {
                _currentStatus.value = "Download failed, retrying... (attempt ${attempt + 1}/2)"
            } else {
                _currentStatus.value = "Downloading ${build.id}..."
            }

            val result = provider.downloadBuild(build, serverFile) { downloaded, total ->
                val fraction = if (total > 0) downloaded.toDouble() / total else 0.0
                _downloadProgress.value = 0.1 + 0.6 * fraction
                _currentStatus.value = "Downloading ${formatBytes(downloaded)} / ${formatBytes(total)}"
            }

            if (result.isSuccess) {
                _downloadProgress.value = 0.7
                _currentStatus.value = "Download complete"

                if (isForge) {
                    _downloadProgress.value = 0.75
                    _currentStatus.value = "Running installer..."
                    val installResult = runForgeInstaller(serverFile, config.javaVersion)
                    if (installResult.isFailure) {
                        lastError = installResult.exceptionOrNull()
                        return@withContext Result.failure(installResult.exceptionOrNull()!!)
                    }
                    val installedJar = installResult.getOrThrow()
                    _downloadProgress.value = 1.0
                    _currentStatus.value = "Ready"
                    return@withContext Result.success(installedJar)
                }

                _downloadProgress.value = 1.0
                _currentStatus.value = "Ready"
                return@withContext Result.success(serverFile)
            }

            lastError = result.exceptionOrNull()
            if (lastError is IOException && attempt == 0) {
                _currentStatus.value = "Download failed: ${lastError.message}. Retrying..."
            }
        }

        _downloadProgress.value = 0.0
        _currentStatus.value = "Download failed: ${lastError?.message}"
        Result.failure(lastError ?: Exception("Download failed"))
    }

    private fun runForgeInstaller(installerJar: File, javaVersion: Int): Result<File> {
        val serverDir = installerJar.parentFile ?: return Result.failure(Exception("Invalid server directory"))
        return try {
            val jdkManager = com.portalhost.java.JdkManager()
            val javaExe = jdkManager.getJavaExecutable(javaVersion)
                ?: jdkManager.checkSystemJava(javaVersion)
                ?: return Result.failure(Exception("Java $javaVersion not found for Forge installer"))

            logger.info { "Running Forge/NeoForge installer: ${installerJar.name}" }
            val process = ProcessBuilder(
                javaExe.absolutePath,
                "-jar", installerJar.absolutePath,
                "--installServer"
            )
                .directory(serverDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exited = process.waitFor()
            if (exited != 0) {
                logger.error { "Forge installer failed (exit $exited): $output" }
                return Result.failure(Exception("Forge installer failed: ${output.takeLast(200)}"))
            }
            logger.info { "Forge installer completed successfully" }

            val serverJar = findServerJar(serverDir)
            val resultJar = serverJar ?: installerJar
            if (serverJar != null && serverJar.absolutePath != installerJar.absolutePath) {
                installerJar.delete()
            }
            Result.success(resultJar)
        } catch (e: Exception) {
            logger.error(e) { "Forge installer error" }
            Result.failure(e)
        }
    }

    private fun sanitizeFolderName(name: String): String {
        val sanitized = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return sanitized.ifBlank { "Server" }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        return String.format("%.1f MB", kb / 1024.0)
    }

    private fun findServerJar(dir: File): File? {
        val candidates = dir.listFiles { f ->
            f.isFile && f.extension == "jar" && !f.name.contains("installer")
        }?.sortedByDescending { it.lastModified() }
        return candidates?.firstOrNull()
    }

    // Download with progress tracking
    suspend fun downloadWithProgress(url: String, destination: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            URL(url).downloadToFile(destination = destination) { downloaded, total ->
                if (total > 0) {
                    _downloadProgress.value = downloaded.toDouble() / total
                }
            }
            Result.success(destination)
        } catch (e: Exception) {
            destination.delete()
            Result.failure(e)
        }
    }
}
