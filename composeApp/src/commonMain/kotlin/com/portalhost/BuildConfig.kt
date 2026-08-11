package com.portalhost

object BuildConfig {
    val VERSION_NAME: String = runCatching {
        val resource = BuildConfig::class.java.classLoader.getResourceAsStream("version.txt")
        if (resource != null) {
            val text = resource.bufferedReader().use { it.readText() }.trim()
            if (text.isNotBlank()) text else "0.0.0"
        } else {
            "0.0.0"
        }
    }.getOrDefault("0.0.0")

    val DISPLAY_NAME: String = "PortalHost v$VERSION_NAME"
}
