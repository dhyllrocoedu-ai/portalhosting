package com.portalhost.preferences

import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.prefs.Preferences

class Preferences {
    private val settings = PreferencesSettings(Preferences.userRoot().node("com/portalhost"))

    var theme: MutableStateFlow<String> = stringPref("theme", "system")
    var language: MutableStateFlow<String> = stringPref("language", "en")
    var autoCheckUpdates: MutableStateFlow<Boolean> = boolPref("autoCheckUpdates", true)
    var showConsoleColors: MutableStateFlow<Boolean> = boolPref("showConsoleColors", true)
    var maxConsoleLines: MutableStateFlow<Int> = intPref("maxConsoleLines", 5000)
    var defaultMemoryMin: MutableStateFlow<Int> = intPref("defaultMemoryMin", 1024)
    var defaultMemoryMax: MutableStateFlow<Int> = intPref("defaultMemoryMax", 4096)
    var autoBackupEnabled: MutableStateFlow<Boolean> = boolPref("autoBackupEnabled", false)
    var backupIntervalHours: MutableStateFlow<Int> = intPref("backupIntervalHours", 6)
    var rconEnabledByDefault: MutableStateFlow<Boolean> = boolPref("rconEnabledByDefault", false)
    var serverAutoRestart: MutableStateFlow<Boolean> = boolPref("serverAutoRestart", false)
    var confirmServerDelete: MutableStateFlow<Boolean> = boolPref("confirmServerDelete", true)
    var showAdvancedSettings: MutableStateFlow<Boolean> = boolPref("showAdvancedSettings", false)
    var logLevel: MutableStateFlow<String> = stringPref("logLevel", "INFO")
    var tunnelUrl: MutableStateFlow<String> = stringPref("tunnelUrl", "")
    var dataDirectory: MutableStateFlow<String> = stringPref("dataDirectory", "")
    var windowWidth: MutableStateFlow<Int> = intPref("windowWidth", 1200)
    var windowHeight: MutableStateFlow<Int> = intPref("windowHeight", 800)
    var windowX: MutableStateFlow<Int> = intPref("windowX", -1)
    var windowY: MutableStateFlow<Int> = intPref("windowY", -1)

    fun resetToDefaults() {
        theme.value = "system"
        language.value = "en"
        autoCheckUpdates.value = true
        showConsoleColors.value = true
        maxConsoleLines.value = 5000
        defaultMemoryMin.value = 1024
        defaultMemoryMax.value = 4096
        autoBackupEnabled.value = false
        backupIntervalHours.value = 6
        rconEnabledByDefault.value = false
        serverAutoRestart.value = false
        confirmServerDelete.value = true
        showAdvancedSettings.value = false
        logLevel.value = "INFO"
        dataDirectory.value = ""
        windowWidth.value = 1200
        windowHeight.value = 800
        windowX.value = -1
        windowY.value = -1
    }

    private fun stringPref(key: String, default: String): MutableStateFlow<String> {
        val flow = MutableStateFlow(settings.getString(key, default))
        return PersistedFlow(flow) { settings.putString(key, it) }
    }

    private fun intPref(key: String, default: Int): MutableStateFlow<Int> {
        val flow = MutableStateFlow(settings.getInt(key, default))
        return PersistedFlow(flow) { settings.putInt(key, it) }
    }

    private fun boolPref(key: String, default: Boolean): MutableStateFlow<Boolean> {
        val flow = MutableStateFlow(settings.getBoolean(key, default))
        return PersistedFlow(flow) { settings.putBoolean(key, it) }
    }
}

private class PersistedFlow<T>(
    private val flow: MutableStateFlow<T>,
    private val onSet: (T) -> Unit
) : MutableStateFlow<T> by flow {
    override var value: T
        get() = flow.value
        set(v) {
            flow.value = v
            onSet(v)
        }

    override suspend fun emit(value: T) {
        flow.emit(value)
        onSet(value)
    }

    override fun tryEmit(value: T): Boolean {
        val result = flow.tryEmit(value)
        if (result) onSet(value)
        return result
    }

    override fun compareAndSet(expect: T, update: T): Boolean {
        val result = flow.compareAndSet(expect, update)
        if (result) onSet(update)
        return result
    }
}
