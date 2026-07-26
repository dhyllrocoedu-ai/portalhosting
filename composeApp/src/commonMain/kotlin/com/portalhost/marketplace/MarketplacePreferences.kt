package com.portalhost.marketplace

import com.portalhost.model.MarketplaceFilters
import com.portalhost.model.MarketplaceSort
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.prefs.Preferences

class MarketplacePreferences {
    private val settings = PreferencesSettings(Preferences.userRoot().node("com/portalhost/marketplace"))

    private var _filtersFlow = MutableStateFlow(loadFilters())
    val filtersFlow = _filtersFlow

    fun loadFilters(): MarketplaceFilters {
        val raw = settings.getString("filters", "")
        return if (raw.isBlank()) {
            MarketplaceFilters()
        } else {
            deserializeFilters(raw)
        }
    }

    fun updateFilters(newFilters: MarketplaceFilters) {
        settings.putString("filters", serializeFilters(newFilters))
        _filtersFlow.value = newFilters
    }

    private fun serializeFilters(f: MarketplaceFilters): String {
        return listOf(
            f.query,
            f.version ?: "",
            f.loader ?: "",
            f.projectType ?: "",
            f.categories.joinToString(","),
            f.sort.apiValue
        ).joinToString("|")
    }

    private fun deserializeFilters(raw: String): MarketplaceFilters {
        val parts = raw.split("|")
        return MarketplaceFilters(
            query = parts.getOrElse(0) { "" },
            version = parts.getOrElse(1) { "" }.takeIf { it.isNotBlank() },
            loader = parts.getOrElse(2) { "" }.takeIf { it.isNotBlank() },
            projectType = parts.getOrElse(3) { "" }.takeIf { it.isNotBlank() },
            categories = parts.getOrElse(4) { "" }.split(",").filter { it.isNotBlank() }.toSet(),
            sort = MarketplaceSort.entries.find { it.apiValue == parts.getOrElse(5) { "downloads" } } ?: MarketplaceSort.Downloads
        )
    }
}