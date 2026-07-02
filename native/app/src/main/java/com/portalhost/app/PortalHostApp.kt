package com.portalhost.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PortalHostApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // v3 — IMPORTANCE_LOW (no sound) + new ID so existing installs get the silent channel
        val channel = NotificationChannel(
            CHANNEL_SERVER,
            "Minecraft Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Minecraft server status and background keep-alive notifications"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        lateinit var instance: PortalHostApp
            private set

        const val CHANNEL_SERVER = "minecraft_server_v3"
    }
}
