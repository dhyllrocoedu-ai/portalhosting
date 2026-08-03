package com.portalhost.app.ui.screens.marketplace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.marketplace.MarketplaceRepository
import com.portalhost.marketplace.formatDownloads
import com.portalhost.model.MarketplaceFilters
import com.portalhost.model.MarketplaceProjectType
import com.portalhost.model.MarketplaceSort
import com.portalhost.model.MarketplaceUiState
import com.portalhost.model.ModrinthProject
import com.portalhost.model.ModrinthSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MarketplaceScreen(
    onProjectClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { MarketplaceRepository() }

    var uiState by remember { mutableStateOf<MarketplaceUiState>(MarketplaceUiState.Initial) }
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var projectType by remember { mutableStateOf<String?>(null) }
    var loader by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(MarketplaceSort.Downloads) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(query) {
        delay(400)
        debouncedQuery = query
    }

    val filters = MarketplaceFilters(
        query = debouncedQuery,
        version = version,
        loader = loader,
        projectType = projectType,
        sort = sort
    )

    var initialSearchFired by remember { mutableStateOf(false) }
    LaunchedEffect(filters, projectType, loader, version, sort, debouncedQuery) {
        if (!initialSearchFired && uiState is MarketplaceUiState.Initial) {
            uiState = MarketplaceUiState.Loading
        }
        initialSearchFired = true
        val prev = uiState
        if (prev is MarketplaceUiState.Initial) {
            uiState = MarketplaceUiState.Loading
        }
        uiState = MarketplaceUiState.Loading
        scope.launch {
            repository.searchProjects(
                query = filters.query,
                version = filters.version,
                loader = filters.loader,
                projectType = filters.projectType,
                categories = filters.categories,
                sort = filters.sort,
                offset = 0,
                limit = 20
            ).onSuccess { searchResult ->
                uiState = MarketplaceUiState.Success(
                    projects = searchResult.projects,
                    totalHits = searchResult.totalHits,
                    hasMore = searchResult.offset + searchResult.limit < searchResult.totalHits
                )
            }.onFailure { e ->
                uiState = MarketplaceUiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            val lastVisibleItem = visibleItems.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 4 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        val success = uiState as? MarketplaceUiState.Success ?: return@LaunchedEffect
        if (success.hasMore && !isLoadingMore) {
            isLoadingMore = true
            scope.launch {
                repository.searchProjects(
                    query = filters.query,
                    version = filters.version,
                    loader = filters.loader,
                    projectType = filters.projectType,
                    categories = filters.categories,
                    sort = filters.sort,
                    offset = success.projects.size,
                    limit = 20
                ).onSuccess { searchResult ->
                    uiState = MarketplaceUiState.Success(
                        projects = success.projects + searchResult.projects,
                        totalHits = searchResult.totalHits,
                        hasMore = searchResult.offset + searchResult.limit < searchResult.totalHits
                    )
                }
                isLoadingMore = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search add-ons") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortMenu(
                sort = sort,
                onSelect = { sort = it },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = loader != null,
                onClick = { loader = if (loader == null) "fabric" else null },
                label = { Text(loader ?: "Loader", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
            FilterChip(
                selected = projectType != null,
                onClick = {
                    projectType = when (projectType) {
                        null -> MarketplaceProjectType.Plugin.apiValue
                        MarketplaceProjectType.Plugin.apiValue -> MarketplaceProjectType.Mod.apiValue
                        MarketplaceProjectType.Mod.apiValue -> MarketplaceProjectType.Datapack.apiValue
                        else -> null
                    }
                },
                label = { Text(projectType?.let { typeLabel(it) } ?: "Type") }
            )
        }

        Spacer(Modifier.height(8.dp))

        when (val state = uiState) {
            is MarketplaceUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MarketplaceUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Failed to load marketplace",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            uiState = MarketplaceUiState.Loading
                            scope.launch {
                                repository.searchProjects(
                                    query = filters.query,
                                    version = filters.version,
                                    loader = filters.loader,
                                    projectType = filters.projectType,
                                    categories = filters.categories,
                                    sort = filters.sort,
                                    offset = 0,
                                    limit = 20
                                ).onSuccess { searchResult ->
                                    uiState = MarketplaceUiState.Success(searchResult.projects, searchResult.totalHits, searchResult.offset + searchResult.limit < searchResult.totalHits)
                                }.onFailure { e ->
                                    uiState = MarketplaceUiState.Error(e.message ?: "Failed to load")
                                }
                            }
                        }) { Text("Retry") }
                    }
                }
            }
            is MarketplaceUiState.Success -> {
                if (state.projects.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No add-ons found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.projects, key = { it.id }) { project ->
                            MarketplaceProjectCard(
                                project = project,
                                onClick = { onProjectClick(project.id) }
                            )
                        }
                        if (state.hasMore && isLoadingMore) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }
            is MarketplaceUiState.Initial -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun SortMenu(
    sort: MarketplaceSort,
    onSelect: (MarketplaceSort) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FilterChip(
            selected = false,
            onClick = { open = true },
            label = { Text(sort.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MarketplaceSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onSelect(option)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
fun MarketplaceProjectCard(
    project: ModrinthProject,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MarketplaceAsyncImage(
                url = project.iconUrl,
                contentDescription = project.title,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    project.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(project.projectType.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }
                    )
                    if (project.loaders.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(project.loaders.joinToString(", "), fontSize = 10.sp) }
                        )
                    }
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                        formatDownloads(project.downloads),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun typeLabel(apiValue: String): String = when (apiValue) {
    "plugin" -> "Plugin"
    "mod" -> "Mod"
    "datapack" -> "Datapack"
    else -> apiValue.replaceFirstChar { it.uppercase() }
}
