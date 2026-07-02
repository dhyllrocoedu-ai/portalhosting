package com.portalhost.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.portalhost.app.MainActivity
import com.portalhost.app.PortalHostApp
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val manager = ServerManagerHolder.manager

        when (action) {
            ACTION_FOREGROUND -> {
                try {
                    startForeground(NOTIFICATION_ID, buildNotification("Starting..."))
                } catch (e: Exception) {
                    Log.e(TAG, "startForeground failed", e)
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
                try {
                    startForeground(NOTIFICATION_ID, buildNotification("Reconnecting..."))
                } catch (e: Exception) {
                    Log.e(TAG, "startForeground (null intent) failed", e)
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
            while (isActive) {
                val manager = ServerManagerHolder.manager ?: break
                val state = manager.state.value
                val stats = manager.processStats.value
                val notification = buildLiveNotification(state, stats)
                try {
                    withContext(Dispatchers.Main) {
                        val nm = getSystemService(android.app.NotificationManager::class.java)
                        nm.notify(NOTIFICATION_ID, notification)
                    }
                } catch (_: Exception) {}
                delay(5000)
            }
        }
    }

    private fun buildLiveNotification(
        state: com.portalhost.app.server.ServerState,
        stats: com.portalhost.app.server.ProcessStats
    ): android.app.Notification {
        val statusText = when (state.status) {
            ServerStatus.ONLINE -> "Online"
            ServerStatus.STARTING -> "Starting..."
            ServerStatus.STOPPING -> "Stopping..."
            ServerStatus.CRASHED -> "Crashed"
            ServerStatus.OFFLINE -> "Offline"
        }

        val uptime = formatDuration(state.uptimeSeconds)
        val ram = stats.ramFormatted
        val maxRam = stats.maxRamFormatted
        val tps = "%.1f".format(Locale.US, stats.tps)
        val players = state.players.size

        val text = "$statusText • ${players}P • ${ram}/${maxRam} • ${tps}TPS • ${uptime}"

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
            .setContentTitle("Minecraft Server")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
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

    override fun onDestroy() {
        notificationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, PortalHostApp.CHANNEL_SERVER)
            .setContentTitle("Minecraft Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
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

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%dh %dm".format(h, m)
    else if (m > 0) "%dm %ds".format(m, s)
    else "%ds".format(s)
}
