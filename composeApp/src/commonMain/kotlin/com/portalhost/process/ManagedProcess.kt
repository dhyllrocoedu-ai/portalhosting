package com.portalhost.process

import java.io.File

data class ManagedProcess(
    val pid: Int,
    val command: List<String>,
    val workingDir: File,
    val osPid: Long = 0L
)
