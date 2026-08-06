package com.portalhost.desktop.util

import com.portalhost.filesystem.FileSystem
import com.portalhost.filesystem.defaultDataDir
import com.portalhost.preferences.Preferences
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import javax.swing.JOptionPane

object UninstallHelper {
    private const val DISPLAY_NAME = "PortalHost"

    private val DATA_ITEMS = listOf("servers", "jdks", "playit", "backups", "temp", "portalhost.db")

    enum class PreserveResult {
        MOVED,
        NOT_NEEDED,
        FAILED
    }

    fun findProductCode(): String? {
        val paths = listOf(
            "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKLM\\SOFTWARE\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        )
        for (path in paths) {
            try {
                val proc = ProcessBuilder(
                    "reg", "query", path, "/s", "/f", DISPLAY_NAME
                ).redirectErrorStream(true).start()
                val out = proc.inputStream.bufferedReader().readText()
                proc.waitFor()
                if (proc.exitValue() != 0) continue
                var currentKey: String? = null
                for (line in out.lines()) {
                    val t = line.trim()
                    if (t.startsWith("HKEY_")) {
                        currentKey = t
                    } else if (t.startsWith("DisplayName") && t.contains(DISPLAY_NAME)) {
                        if (currentKey != null) {
                            val guid = currentKey.substringAfterLast("\\")
                            if (guid.startsWith("{") && guid.endsWith("}")) {
                                return guid
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
        return null
    }

    /** The folder the app was installed into (parent of the bundled runtime). */
    fun installDirectory(): File? {
        return File(System.getProperty("java.home")).parentFile
    }

    /** Case-insensitive check for whether [path] lives inside (or equals) [installDir]. */
    fun isPathInsideInstallDir(installDir: File, path: File): Boolean {
        val installAbs = normalize(installDir.absolutePath)
        val pathAbs = normalize(path.absolutePath)
        return pathAbs == installAbs || pathAbs.startsWith("$installAbs/")
    }

    /**
     * Moves the data directory out of the install folder when it lives inside it.
     *
     * Returns the new data directory, or null when the data was already outside
     * the install folder or when the move failed. When the data dir equals the
     * install dir, only the known data items are moved so the app files stay in
     * place.
     */
    fun moveDataDirOut(installDir: File, dataDir: File): File? {
        val installAbs = normalize(installDir.absolutePath)
        val dataAbs = normalize(dataDir.absolutePath)
        if (dataAbs != installAbs && !dataAbs.startsWith("$installAbs/")) return null
        val target = defaultDataDir()
        target.mkdirs()
        return if (dataAbs == installAbs) {
            val allMoved = DATA_ITEMS.all { name ->
                val src = File(dataDir, name)
                !src.exists() || moveBestEffort(src, File(target, name))
            }
            if (allMoved) target else null
        } else {
            val dest = File(target, dataDir.name)
            if (moveBestEffort(dataDir, dest)) dest else null
        }
    }

    /**
     * Called from Settings before the in-app uninstall. Moves the data directory
     * out of the install folder and repoints the preference only on success.
     */
    fun preserveData(preferences: Preferences): PreserveResult {
        val installDir = installDirectory() ?: return PreserveResult.FAILED
        val dataDir = FileSystem(preferences).getAppDirBlocking()
        if (!isPathInsideInstallDir(installDir, dataDir)) return PreserveResult.NOT_NEEDED
        val newDir = moveDataDirOut(installDir, dataDir)
        if (newDir == null) return PreserveResult.FAILED
        preferences.dataDirectory.value = newDir.absolutePath
        return PreserveResult.MOVED
    }

    /**
     * Runs before the app window (and before the database is opened). If the data
     * directory resolves inside the install folder, asks the user to move it out.
     * Returns the effective data directory to use, or null to keep the current one.
     */
    fun migrateDataDirBeforeStart(): String? {
        val installDir = installDirectory() ?: return null
        val prefs = java.util.prefs.Preferences.userRoot().node("com/portalhost")
        val custom = prefs.get("dataDirectory", "").takeIf { it.isNotBlank() }
        val dataDir = custom?.let { File(it) } ?: defaultDataDir()
        if (!isPathInsideInstallDir(installDir, dataDir)) return null

        val target = defaultDataDir()
        val moveNow = JOptionPane.showConfirmDialog(
            null,
            "Your Portal Host data (servers, JDKs, playit tunnels, backups and the database) " +
                "is stored inside the app's install folder:\n\n" +
                dataDir.absolutePath +
                "\n\nWhen Portal Host is uninstalled, that folder is deleted and ALL your data " +
                "would be lost.\n\nMove your data to:\n\n" +
                target.absolutePath +
                "\n\nMove it now?",
            "Portal Host - Data Folder Warning",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        ) == JOptionPane.YES_OPTION
        if (!moveNow) return null

        val newDir = moveDataDirOut(installDir, dataDir)
        if (newDir == null) {
            JOptionPane.showMessageDialog(
                null,
                "Could not move your data automatically. Please close this app and move the folder\n\n" +
                    dataDir.absolutePath +
                    "\n\nto\n\n" +
                    target.absolutePath +
                    "\n\nmanually before uninstalling Portal Host.",
                "Portal Host - Data Move Failed",
                JOptionPane.ERROR_MESSAGE
            )
            return null
        }
        prefs.put("dataDirectory", newDir.absolutePath)
        try {
            prefs.flush()
        } catch (_: Exception) { }
        return newDir.absolutePath
    }

    private fun moveBestEffort(src: File, dest: File): Boolean {
        if (!src.exists()) return true
        try {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return true
        } catch (_: Exception) {
            return try {
                if (src.isDirectory && dest.isDirectory) {
                    src.listFiles()?.forEach { child ->
                        if (!moveBestEffort(child, File(dest, child.name))) {
                            throw IllegalStateException("failed to move ${child.name}")
                        }
                    }
                    src.deleteRecursively()
                    true
                } else {
                    src.copyRecursively(dest, overwrite = true)
                    src.deleteRecursively()
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun normalize(path: String): String {
        return path.replace('\\', '/').trimEnd('/').lowercase()
    }

    /**
     * Runs the MSI uninstall on a background thread so the UI stays responsive.
     *
     * The sequence:
     * 1. Launch `msiexec /x` (elevated via UAC if needed) and wait for it to finish.
     * 2. Spawn a detached batch file that force-removes the install folder
     *    shortly afterwards. Because it is an OS process (not a JVM thread), it
     *    survives this app terminating.
     * 3. Exit the app so its files unlock and the detached cleanup can finish.
     */
    fun uninstall(productCode: String) {
        Thread {
            try {
                // Wait for the MSI uninstaller to complete (or give up after 3 min).
                // msiexec.exe directly (no PowerShell wrapper) avoids UAC prompt issues.
                val proc = ProcessBuilder(
                    "msiexec.exe", "/x", productCode, "/qb", "/norestart"
                ).redirectErrorStream(true).start()
                proc.waitFor(180, TimeUnit.SECONDS)
            } catch (_: Exception) { }

            finishDetachedCleanup()
        }.apply { isDaemon = false }.start()
    }

    /**
     * Portable/EXE install (no MSI product code in the registry): run the detached
     * force-cleanup of the install folder and close the app so its files unlock.
     */
    fun uninstallPortable() {
        Thread {
            finishDetachedCleanup()
        }.apply { isDaemon = false }.start()
    }

    private fun finishDetachedCleanup() {
        launchDetachedCleanup()

        // Give the detached cleanup a moment to start, then close the app so its
        // files unlock and the cleanup can remove the install folder.
        try { Thread.sleep(2000) } catch (_: InterruptedException) { }
        kotlin.system.exitProcess(0)
    }

    /**
     * Launches a hidden, detached batch file that force-removes the install
     * folder a few seconds after the app exits.
     */
    private fun launchDetachedCleanup() {
        try {
            val installDir = installDirectory() ?: return
            if (!installDir.exists()) return

            val tmpBat = File(System.getProperty("java.io.tmpdir"), "portalhost_uninstall_${System.currentTimeMillis()}.bat")
            val installPath = installDir.absolutePath.replace("\"", "\\\"")
            val startMenu = "${System.getenv("APPDATA")}\\Microsoft\\Windows\\Start Menu\\Programs\\PortalHost".replace("\"", "\\\"")
            val startMenuLnk = "${System.getenv("APPDATA")}\\Microsoft\\Windows\\Start Menu\\Programs\\PortalHost.lnk".replace("\"", "\\\"")

            tmpBat.writeText("""
                @echo off
                :: Wait for the parent process to exit and release file handles
                timeout /t 3 /nobreak >nul
                :: Kill any remaining PortalHost processes so files unlock
                taskkill /F /IM PortalHost.exe /T >nul 2>&1
                taskkill /F /IM java.exe /FI "WINDOWTITLE eq PortalHost*" /T >nul 2>&1
                timeout /t 2 /nobreak >nul
                :: Remove the install folder (program files only - never user data)
                rd /s /q "$installPath" 2>nul
                :: Remove Start Menu shortcuts
                if exist "$startMenu" rd /s /q "$startMenu" 2>nul
                if exist "$startMenuLnk" del /f /q "$startMenuLnk" 2>nul
                :: Self-delete this batch file
                del /f /q "%~f0" >nul 2>&1
            """.trimIndent())

            // Spawn the batch file detached via 'start /b' (no new window)
            Runtime.getRuntime().exec(arrayOf("cmd.exe", "/c", "start", "/b", tmpBat.absolutePath))
        } catch (_: Exception) { }
    }
}
