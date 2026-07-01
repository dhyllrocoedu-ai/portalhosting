package com.portalhost.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.portalhost.app.MainActivity
import com.portalhost.app.PortalHostApp
import com.portalhost.app.R
import com.portalhost.app.server.ServerManager
import com.portalhost.app.server.ServerStatus
import kotlinx.coroutines.*
import java.util.Locale

class MinecraftService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notificationJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val manager = ServerManagerHolder.manager

        when (action) {
            ACTION_START -> {
                val jarPath = intent?.getStringExtra(EXTRA_JAR_PATH) ?: return START_NOT_STICKY
                val javaArgs = intent?.getStringArrayListExtra(EXTRA_JAVA_ARGS) ?: arrayListOf()
                val serverDir = intent?.getStringExtra(EXTRA_SERVER_DIR)

                startForeground(NOTIFICATION_ID, buildNotification("Starting..."))
                startNotificationUpdater()
                startServer(jarPath, javaArgs, serverDir)
            }

            ACTION_STOP -> {
                stopServer()
            }

            ACTION_NOTIFICATION_STOP -> {
                serviceScope.launch { manager?.stop() }
            }

            ACTION_NOTIFICATION_RESTART -> {
                serviceScope.launch { manager?.restart() }
            }
        }

        return START_STICKY
    }

    private fun startServer(jarPath: String, javaArgs: List<String>, serverDir: String?) {
        val manager = ServerManagerHolder.manager ?: return
        serviceScope.launch {
            manager.start(
                jarPath = jarPath,
                javaArgs = javaArgs,
                serverDir = serverDir ?: jarPath.substringBeforeLast("/")
            )
        }
    }

    private fun stopServer() {
        val manager = ServerManagerHolder.manager
        serviceScope.launch {
            manager?.stop()
        }
        notificationJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        var manager: ServerManager? = null
    }

    companion object {
        const val ACTION_START = "com.portalhost.action.START_SERVER"
        const val ACTION_STOP = "com.portalhost.action.STOP_SERVER"
        const val ACTION_NOTIFICATION_STOP = "com.portalhost.action.NOTIFICATION_STOP"
        const val ACTION_NOTIFICATION_RESTART = "com.portalhost.action.NOTIFICATION_RESTART"
        const val EXTRA_JAR_PATH = "jar_path"
        const val EXTRA_JAVA_ARGS = "java_args"
        const val EXTRA_SERVER_DIR = "server_dir"
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
