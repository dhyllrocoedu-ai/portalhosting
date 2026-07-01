package com.portalhost.app.ui.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val id: String = "",
    val name: String = "My Server",
    val jarPath: String = "",
    val jarName: String = "server.jar",
    val serverType: String = "custom",
    val mcVersion: String = "",
    val minRam: String = "512M",
    val maxRam: String = "2G",
    val port: Int = 25565,
    val gamemode: String = "survival",
    val difficulty: String = "easy",
    val motd: String = "A Minecraft Server",
    val eulaAccepted: Boolean = false,
    val autoRestart: Boolean = false
)
