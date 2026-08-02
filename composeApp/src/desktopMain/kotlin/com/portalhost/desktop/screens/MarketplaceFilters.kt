package com.portalhost.desktop.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.portalhost.model.MarketplaceFilters
import com.portalhost.model.MarketplaceSort

private val supportedVersions = listOf(
    "1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.2", "1.20.1",
    "1.19.4", "1.19.2", "1.18.2", "1.17.1", "1.16.5", "1.12.2"
)

private val supportedLoaders = listOf(
    "Paper" to "paper",
    "Spigot" to "spigot",
    "Purpur" to "purpur",
    "Folia" to "folia",
    "Forge" to "forge",
    "NeoForge" to "neoforge",
    "Fabric" to "fabric",
    "Quilt" to "quilt",
    "Vanilla" to "vanilla",
    "Datapack" to "datapack"
)

private val projectTypes = listOf(
    "Plugin" to "plugin",
    "Mod" to "mod",
    "Datapack" to "datapack",
    "Shader" to "shader",
    "Resource Pack" to "resourcepack"
)

private val categories = listOf(
    "admin", "chat", "economy", "anti-grief", "world-gen",
    "technology", "magic", "adventure", "decoration", "optimization",
    "fun", "utility", "library", "framework"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceFiltersBar(
    filters: MarketplaceFilters,
    onFilterChange: (MarketplaceFilters) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    var versionExpanded by remember { mutableStateOf(false) }
    var loaderExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    val hasActiveFilters = filters.version != null || filters.loader != null ||
            filters.projectType != null || filters.categories.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = { onFilterChange(filters.copy(query = it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search plugins, mods, datapacks...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (filters.query.isNotBlank()) {
                        IconButton(onClick = { onFilterChange(filters.copy(query = "")) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(filters.query)
                        focusManager.clearFocus()
                    }
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        if (hasActiveFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filters.version?.let { version ->
                    ActiveFilterChip(
                        label = "Version: $version",
                        onRemove = { onFilterChange(filters.copy(version = null)) }
                    )
                }
                filters.loader?.let { loader ->
                    ActiveFilterChip(
                        label = "Loader: ${loader.replaceFirstChar { it.uppercase() }}",
                        onRemove = { onFilterChange(filters.copy(loader = null)) }
                    )
                }
                filters.projectType?.let { type ->
                    ActiveFilterChip(
                        label = "Type: ${type.replaceFirstChar { it.uppercase() }}",
                        onRemove = { onFilterChange(filters.copy(projectType = null)) }
                    )
                }
                filters.categories.forEach { cat ->
                    ActiveFilterChip(
                        label = cat.replaceFirstChar { it.uppercase() },
                        onRemove = { onFilterChange(filters.copy(categories = filters.categories - cat)) }
                    )
                }
                TextButton(onClick = onClear) {
                    Text("Clear all")
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterDropdown(
                label = "Version",
                value = filters.version,
                expanded = versionExpanded,
                onExpandedChange = { versionExpanded = it },
                options = supportedVersions,
                onSelect = { index ->
                    val selectedVersion = supportedVersions[index]
                    onFilterChange(filters.copy(version = if (filters.version == selectedVersion) null else selectedVersion))
                    versionExpanded = false
                }
            )

            FilterDropdown(
                label = "Loader",
                value = filters.loader?.replaceFirstChar { it.uppercase() },
                expanded = loaderExpanded,
                onExpandedChange = { loaderExpanded = it },
                options = supportedLoaders.map { it.first },
                optionValues = supportedLoaders.map { it.second },
                onSelect = { index ->
                    val value = supportedLoaders[index].second
                    onFilterChange(filters.copy(loader = if (filters.loader == value) null else value))
                    loaderExpanded = false
                }
            )

            FilterDropdown(
                label = "Type",
                value = filters.projectType?.replaceFirstChar { it.uppercase() },
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
                options = projectTypes.map { it.first },
                optionValues = projectTypes.map { it.second },
                onSelect = { index ->
                    val value = projectTypes[index].second
                    onFilterChange(filters.copy(projectType = if (filters.projectType == value) null else value))
                    typeExpanded = false
                }
            )

            FilterDropdown(
                label = "Sort",
                value = filters.sort.displayName,
                expanded = sortExpanded,
                onExpandedChange = { sortExpanded = it },
                options = MarketplaceSort.entries.map { it.displayName },
                optionValues = MarketplaceSort.entries.map { it.apiValue },
                onSelect = { index ->
                    onFilterChange(filters.copy(sort = MarketplaceSort.entries[index]))
                    sortExpanded = false
                }
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    value: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    optionValues: List<String> = options,
    onSelect: (Int) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.width(130.dp)
    ) {
        OutlinedTextField(
            value = value ?: label,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(index) },
                    trailingIcon = if (optionValues[index] == value) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun ActiveFilterChip(
    label: String,
    onRemove: () -> Unit
) {
    FilterChip(
        selected = true,
        onClick = { },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        trailingIcon = {
            IconButton(onClick = onRemove, modifier = Modifier.height(16.dp)) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Remove filter",
                    modifier = Modifier.height(12.dp)
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.height(32.dp)
    )
}