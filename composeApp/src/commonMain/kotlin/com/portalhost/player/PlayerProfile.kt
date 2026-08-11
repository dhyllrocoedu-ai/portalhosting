package com.portalhost.player

data class NameChange(
    val name: String,
    val changedAt: Long? = null,
)

data class PlayerProfile(
    val uuid: String,
    val currentName: String,
    val nameHistory: List<NameChange> = emptyList(),
    val firstSeen: Long? = null,
    val lastSeen: Long? = null,
    val skinUrl: String? = null,
    val skinUrlCachedAt: Long? = null,
)
