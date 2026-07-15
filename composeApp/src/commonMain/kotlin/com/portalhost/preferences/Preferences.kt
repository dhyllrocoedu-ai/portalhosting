package com.portalhost.preferences

import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.prefs.Preferences

class Preferences {
    private val settings = PreferencesSettings(Preferences.userRoot().node("com/portalhost"))

    var theme: MutableStateFlow<String> = MutableStateFlow(settings.getString("theme", "system"))
    var language: MutableStateFlow<String> = MutableStateFlow(settings.getString("language", "en"))
    var autoCheckUpdates: MutableStateFlow<Boolean> = MutableStateFlow(settings.getBoolean("autoCheckUpdates", true))
    var showConsoleColors: MutableStateFlow<Boolean> = MutableStateFlow(settings.getBoolean("showConsoleColors", true))
    var maxConsoleLines: MutableStateFlow<Int> = MutableStateFlow(settings.getInt("maxConsoleLines", 5000))
    var defaultMemoryMin: MutableStateFlow<Int> = MutableStateFlow(settings.getInt("defaultMemoryMin", 1024))
    var defaultMemoryMax: MutableStateFlow<Int> = MutableStateFlow(settings.getInt("defaultMemoryMax", 4096))
    var autoBackupEnabled: MutableStateFlow<Boolean> = MutableStateFlow(settings.getBoolean("autoBackupEnabled", false))
    var backupIntervalHours: MutableStateFlow<Int> = MutableStateFlow(settings.getInt("backupIntervalHours", 6))
    var rconEnabledByDefault: MutableStateFlow<Boolean> = MutableStateFlow(settings.getBoolean("rconEnabledByDefault", false))
    var serverAutoRestart: MutableStateFlow<Boolean> = MutableStateFlow(settings.getBoolean("serverAutoRestart", false))
    var confirmServerDelete: MutableStateFlow<Boolean> = MutableStateFlow(settings.getBoolean("confirmServerDelete", true))
    var showAdvancedSettings: MutableStateFlow<Boolean> = MutableStateFlow(settings.getBoolean("showAdvancedSettings", false))
    var logLevel: MutableStateFlow<String> = MutableStateFlow(settings.getString("logLevel", "INFO"))
    var tunnelUrl: MutableStateFlow<String> = MutableStateFlow(settings.getString("tunnelUrl", ""))
    var dataDirectory: MutableStateFlow<String> = MutableStateFlow(settings.getString("dataDirectory", ""))

    fun resetToDefaults() {
        settings.putString("theme", "system")
        theme.value = "system"
        settings.putString("language", "en")
        language.value = "en"
        settings.putBoolean("autoCheckUpdates", true)
        autoCheckUpdates.value = true
        settings.putBoolean("showConsoleColors", true)
        showConsoleColors.value = true
        settings.putInt("maxConsoleLines", 5000)
        maxConsoleLines.value = 5000
        settings.putInt("defaultMemoryMin", 1024)
        defaultMemoryMin.value = 1024
        settings.putInt("defaultMemoryMax", 4096)
        defaultMemoryMax.value = 4096
        settings.putBoolean("autoBackupEnabled", false)
        autoBackupEnabled.value = false
        settings.putInt("backupIntervalHours", 6)
        backupIntervalHours.value = 6
        settings.putBoolean("rconEnabledByDefault", false)
        rconEnabledByDefault.value = false
        settings.putBoolean("serverAutoRestart", false)
        serverAutoRestart.value = false
        settings.putBoolean("confirmServerDelete", true)
        confirmServerDelete.value = true
        settings.putBoolean("showAdvancedSettings", false)
        showAdvancedSettings.value = false
        settings.putString("logLevel", "INFO")
        logLevel.value = "INFO"
        settings.putString("dataDirectory", "")
        dataDirectory.value = ""
    }
}
