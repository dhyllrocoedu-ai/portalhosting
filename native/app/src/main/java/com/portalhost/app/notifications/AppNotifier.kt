package com.portalhost.app.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.portalhost.app.PortalHostApp
import com.portalhost.app.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class AppNotice(
    val message: String,
    val success: Boolean = true
)

/**
 * Unified in-app + system notification for result/status events.
 * The in-app channel is a SharedFlow of [AppNotice] collected by the
 * top-level SnackbarHost; the system channel reuses PortalHostApp.CHANNEL_SERVER.
 */
class AppNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val TAG = "AppNotifier"

    private val _notices = MutableSharedFlow<AppNotice>(extraBufferCapacity = 8)
    val notices: SharedFlow<AppNotice> = _notices.asSharedFlow()

    private var nextId = 2000

    /**
     * Post a notice both in-app (global snackbar) and as a system notification.
     * @param systemOnly when true, only the system notification is posted and the
     * caller shows its own in-app snackbar (e.g. marketplace detail screen).
     */
    fun notify(message: String, success: Boolean = true, title: String? = null, systemOnly: Boolean = false) {
        if (!systemOnly) {
            _notices.tryEmit(AppNotice(message, success))
        }
        val t = title ?: if (success) "PortalHost" else "PortalHost Error"
        postSystemNotification(t, message)
    }

    private fun postSystemNotification(title: String, text: String) {
        if (!canPost()) return
        try {
            val id = nextId++
            val notification = NotificationCompat.Builder(appContext, PortalHostApp.CHANNEL_SERVER)
                .setSmallIcon(R.drawable.portal_host_logo)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .build()
            val nm = appContext.getSystemService(NotificationManager::class.java)
            nm.notify(id, notification)
        } catch (e: Exception) {
            Log.w(TAG, "system notification failed: ${e.message}")
        }
    }

    private fun canPost(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
}
