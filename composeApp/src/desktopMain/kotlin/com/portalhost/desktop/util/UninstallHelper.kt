package com.portalhost.desktop.util

import com.portalhost.filesystem.FileSystem
import com.portalhost.filesystem.defaultDataDir
import com.portalhost.preferences.Preferences
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object UninstallHelper {
    private const val DISPLAY_NAME = "PortalHost"

    private val DATA_ITEMS = listOf("servers", "jdks", "playit", "backups", "temp", "portalhost.db")

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

    /**
     * The Windows installer removes the whole installation folder on uninstall.
     * If the user's data directory is inside that folder, move the data folders
     * to the default data location first so nothing is lost. Returns the target
     * data directory, or null if the data was already somewhere safe.
     */
    fun preserveData(preferences: Preferences): File? {
        val installDir = File(System.getProperty("java.home")).parentFile ?: return null
        val dataDir = FileSystem(preferences).getAppDirBlocking()
        val installAbs = installDir.absolutePath.replace('\\', '/').trimEnd('/')
        val dataAbs = dataDir.absolutePath.replace('\\', '/').trimEnd('/')
        val insideInstall = dataAbs == installAbs || dataAbs.startsWith("$installAbs/")
        if (!insideInstall) return null

        val target = defaultDataDir()
        target.mkdirs()
        var allMoved = true
        var anyData = false
        for (name in DATA_ITEMS) {
            val src = File(dataDir, name)
            if (!src.exists()) continue
            anyData = true
            val dest = File(target, name)
            val ok = if (dest.exists()) {
                false
            } else {
                moveBestEffort(src, dest)
            }
            if (!ok) allMoved = false
        }
        if (anyData && allMoved) {
            preferences.dataDirectory.value = target.absolutePath
        }
        return target
    }

    private fun moveBestEffort(src: File, dest: File): Boolean {
        try {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return true
        } catch (_: Exception) {
            return try {
                src.copyRecursively(dest, overwrite = true)
                src.deleteRecursively()
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun uninstall(productCode: String) {
        val psCommand = "Start-Process msiexec -ArgumentList '/x $productCode /qb' -Verb RunAs"
        Runtime.getRuntime().exec(arrayOf("powershell", "-Command", psCommand))
    }
}
