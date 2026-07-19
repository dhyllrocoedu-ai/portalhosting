package com.portalhost.desktop

import com.portalhost.model.ServerStatus
import com.portalhost.server.ServerManager
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

class SystemTrayManager(
    private val serverManager: ServerManager,
    private val onShowWindow: () -> Unit,
    private val onExit: () -> Unit,
) {
    private var trayIcon: TrayIcon? = null
    private var isSupported = false

    init {
        isSupported = SystemTray.isSupported()
        if (!isSupported) {
            logger.warn { "System tray is not supported on this platform" }
        }
    }

    fun install() {
        if (!isSupported) return
        if (trayIcon != null) return

        try {
            val tray = SystemTray.getSystemTray()
            val image = createTrayImage()

            val popup = PopupMenu()

            val showItem = MenuItem("Show PortalHost")
            showItem.addActionListener { onShowWindow() }
            popup.add(showItem)

            popup.addSeparator()

            val stopAllItem = MenuItem("Stop All Servers")
            stopAllItem.addActionListener {
                runBlocking {
                    val runningServers = serverManager.servers.value.entries
                        .filter { (id, _) ->
                            val state = serverManager.serverStates.value[id]
                            state?.status == ServerStatus.RUNNING || state?.status == ServerStatus.STARTING
                        }
                    for ((id, _) in runningServers) {
                        serverManager.stopServer(id)
                    }
                }
            }
            popup.add(stopAllItem)

            popup.addSeparator()

            val quitItem = MenuItem("Quit")
            quitItem.addActionListener { onExit() }
            popup.add(quitItem)

            val icon = TrayIcon(image, "Portal Host", popup)
            icon.setImageAutoSize(true)
            icon.addActionListener { onShowWindow() }

            tray.add(icon)
            trayIcon = icon
            logger.info { "System tray icon installed" }
            icon.displayMessage("Portal Host", "Application minimized to tray", TrayIcon.MessageType.INFO)
        } catch (e: Exception) {
            logger.error(e) { "Failed to install system tray icon" }
        }
    }

    fun remove() {
        val icon = trayIcon ?: return
        try {
            SystemTray.getSystemTray().remove(icon)
            trayIcon = null
            logger.info { "System tray icon removed" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to remove system tray icon" }
        }
    }

    private fun createTrayImage(): Image {
        return try {
            val resource = javaClass.getResourceAsStream("/icons/icon.png")
            if (resource != null) {
                ImageIO.read(resource)
            } else {
                createDefaultImage()
            }
        } catch (_: Exception) {
            createDefaultImage()
        }
    }

    private fun createDefaultImage(): Image {
        val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = java.awt.Color(64, 128, 255)
        g.fillRect(0, 0, 16, 16)
        g.dispose()
        return img
    }
}
