package com.portalhost.server.providers

import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.model.ServerBuild
import java.io.File

interface ServerProvider {
    val id: String
    val name: String
    val supportedTypes: Set<ServerType>
    
    suspend fun fetchVersions(): Result<List<ServerVersion>>
    suspend fun fetchBuilds(version: String): Result<List<ServerBuild>>
    suspend fun downloadBuild(build: ServerBuild, destination: File): Result<File>
}
