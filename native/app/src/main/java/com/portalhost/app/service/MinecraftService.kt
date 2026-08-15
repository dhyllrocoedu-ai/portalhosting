package com.portalhost.app.service

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.portalhost.app.MainActivity
import com.portalhost.app.PortalHostApp
import com.portalhost.app.R
import com.portalhost.app.server.ServerStatus
import kotlinx.coroutines.*
import java.util.Locale

/**
 * Foreground service that keeps the server process alive in the background
 * and shows live stats (RAM/TPS/players/uptime) in the notification.
 *
 * Server lifecycle is managed externally (AppNavigation) — this service
 * only provides the foreground notification + notification action buttons.
 */
class MinecraftService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notificationJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        // Ensure notification channel exists (dual-safety with PortalHostApp.onCreate())
        val channel = android.app.NotificationChannel(
            PortalHostApp.CHANNEL_SERVER,
            "Minecraft Server",
            android.app.NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Minecraft server status and background keep-alive notifications"
            setShowBadge(false)
        }
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.createNotificationChannel(channel)
        Log.i(TAG, "Notification channel created/verified in onCreate")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val manager = ServerManagerHolder.manager

        when (action) {
            ACTION_FOREGROUND -> {
                acquireWakeLock()
                try {
                    Log.i(TAG, "Calling startForeground()…")
                    startForeground(NOTIFICATION_ID, buildNotification("Starting..."))
                    Log.i(TAG, "startForeground() succeeded")
                } catch (e: Exception) {
                    Log.e(TAG, "startForeground failed: sdk=${Build.VERSION.SDK_INT} pnGranted=${
                        try {
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        } catch (_: Exception) { "?" }
                    } channelExists=${
                        try {
                            getSystemService(android.app.NotificationManager::class.java)
                                .getNotificationChannel(PortalHostApp.CHANNEL_SERVER) != null
                        } catch (_: Exception) { "?" }
                    }", e)
                    releaseWakeLock()
                    stopSelf()
                    return START_NOT_STICKY
                }
                startNotificationUpdater()
            }

            ACTION_STOP, ACTION_NOTIFICATION_STOP -> {
                notificationJob?.cancel()
                serviceScope.launch {
                    try {
                        manager?.stop()
                    } catch (e: Exception) {
                        Log.e(TAG, "error stopping server", e)
                    }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }

            ACTION_NOTIFICATION_RESTART -> {
                serviceScope.launch { manager?.restart() }
            }

            null -> {
                // START_STICKY recreation — system restarted the service after kill
                acquireWakeLock()
                Log.i(TAG, "null intent (START_STICKY recreation)")
                try {
                    startForeground(NOTIFICATION_ID, buildNotification("Reconnecting..."))
                } catch (e: Exception) {
                    Log.e(TAG, "startForeground (null intent) failed: sdk=${Build.VERSION.SDK_INT}", e)
                    releaseWakeLock()
                    stopSelf()
                    return START_NOT_STICKY
                }
                startNotificationUpdater()
            }
        }

        return START_STICKY
    }

    private fun startNotificationUpdater() {
        notificationJob?.cancel()
        notificationJob = serviceScope.launch {
            var lastText = ""
            var lastStatusText = ""
            var lastNotifyTime = 0L
            while (isActive) {
                val manager = ServerManagerHolder.manager ?: break
                val state = manager.state.value
                val stats = manager.processStats.value

                val statusText = when (state.status) {
                    ServerStatus.ONLINE -> "Online"
                    ServerStatus.STARTING -> "Starting..."
                    ServerStatus.STOPPING -> "Stopping..."
                    ServerStatus.STOPPED -> "Stopped"
                    ServerStatus.CRASHED -> "Crashed"
                    ServerStatus.OFFLINE -> "Offline"
                }
                val ram = stats.ramFormatted
                val maxRam = stats.maxRamFormatted
                val tps = "%.1f".format(Locale.US, stats.tps)
                val players = state.players.size
                val text = "$statusText • ${players}P • ${ram}/${maxRam} • ${tps}TPS"

                val now = System.currentTimeMillis()
                val statusChanged = statusText != lastStatusText
                if (text != lastText && (statusChanged || now - lastNotifyTime >= 3000)) {
                    lastText = text
                    lastStatusText = statusText
                    lastNotifyTime = now
                    val notification = buildLiveNotification(
                        title = "Minecraft Server • $statusText",
                        text = text,
                        uptimeMs = state.uptimeSeconds * 1000
                    )
                    try {
                        withContext(Dispatchers.Main) {
                            val nm = getSystemService(android.app.NotificationManager::class.java)
                            nm.notify(NOTIFICATION_ID, notification)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "notification update failed: ${e.message}")
                    }
                }
                delay(1000)
            }
            Log.i(TAG, "notification updater stopped (manager=null)")
        }
    }

    private fun buildLiveNotification(
        title: String,
        text: String,
        uptimeMs: Long
    ): android.app.Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MinecraftService::class.java).setAction(ACTION_NOTIFICATION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val restartIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MinecraftService::class.java).setAction(ACTION_NOTIFICATION_RESTART),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, PortalHostApp.CHANNEL_SERVER)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.portal_host_logo)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis() - uptimeMs)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Restart", restartIntent)
            .build()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PortalHost:ServerWakeLock").apply {
                acquire()
                Log.i(TAG, "WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.apply {
                if (isHeld) {
                    release()
                    Log.i(TAG, "WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release WakeLock: ${e.message}")
        }
        wakeLock = null
    }

    override fun onDestroy() {
        notificationJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        ServerManagerHolder.manager?.dispose()
        ServerManagerHolder.manager = null
        super.onDestroy()
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, PortalHostApp.CHANNEL_SERVER)
            .setContentTitle("Minecraft Server")
            .setContentText(text)
            .setSmallIcon(R.drawable.portal_host_logo)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    object ServerManagerHolder {
        var manager: com.portalhost.app.server.ServerManager? = null
    }

    companion object {
        const val TAG = "MinecraftService"
        const val ACTION_FOREGROUND = "com.portalhost.action.FOREGROUND"
        const val ACTION_STOP = "com.portalhost.action.STOP_SERVICE"
        const val ACTION_NOTIFICATION_STOP = "com.portalhost.action.NOTIFICATION_STOP"
        const val ACTION_NOTIFICATION_RESTART = "com.portalhost.action.NOTIFICATION_RESTART"
        const val NOTIFICATION_ID = 1001
    }
}
