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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MinecraftService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START -> {
                val jarPath = intent?.getStringExtra(EXTRA_JAR_PATH) ?: return START_NOT_STICKY
                val javaArgs = intent?.getStringArrayListExtra(EXTRA_JAVA_ARGS) ?: arrayListOf()
                val serverDir = intent?.getStringExtra(EXTRA_SERVER_DIR)

                startForeground(NOTIFICATION_ID, buildNotification("Starting..."))
                startServer(jarPath, javaArgs, serverDir)
            }

            ACTION_STOP -> {
                stopServer()
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
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
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
        const val EXTRA_JAR_PATH = "jar_path"
        const val EXTRA_JAVA_ARGS = "java_args"
        const val EXTRA_SERVER_DIR = "server_dir"
        const val NOTIFICATION_ID = 1001
    }
}
