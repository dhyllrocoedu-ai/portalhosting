package com.portalhost.player

import kotlinx.serialization.Serializable

@Serializable
data class WhitelistEntry(val uuid: String, val name: String)

@Serializable
data class OpEntry(
    val uuid: String,
    val name: String,
    val level: Int = 4,
    val bypassesPlayerLimit: Boolean = false,
)

@Serializable
data class BannedPlayerEntry(
    val uuid: String,
    val name: String,
    val created: String? = null,
    val source: String? = null,
    val expires: String? = null,
    val reason: String? = null,
)

@Serializable
data class BannedIpEntry(
    val ip: String,
    val created: String? = null,
    val source: String? = null,
    val expires: String? = null,
    val reason: String? = null,
)
