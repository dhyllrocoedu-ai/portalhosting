package com.portalhost.desktop.util

object UninstallHelper {
    private const val DISPLAY_NAME = "PortalHost"

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

    fun uninstall(productCode: String) {
        val psCommand = "Start-Process msiexec -ArgumentList '/x $productCode /qb' -Verb RunAs"
        Runtime.getRuntime().exec(arrayOf("powershell", "-Command", psCommand))
    }
}
