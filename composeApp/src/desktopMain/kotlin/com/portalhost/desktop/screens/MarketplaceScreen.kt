package com.portalhost.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.portalhost.marketplace.MarketplacePreferences
import com.portalhost.marketplace.MarketplaceRepository
import com.portalhost.marketplace.formatDownloads
import com.portalhost.model.MarketplaceFilters
import com.portalhost.model.MarketplaceUiState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    onNavigateToDetail: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val repository: MarketplaceRepository = koinInject()
    val marketplacePreferences: MarketplacePreferences = koinInject()

    var uiState by remember { mutableStateOf<MarketplaceUiState>(MarketplaceUiState.Initial) }
    var filters by remember { mutableStateOf(marketplacePreferences.loadFilters()) }
    var currentOffset by remember { mutableStateOf(0) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val listState = rememberLazyGridState()

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
            val nextOffset = success.projects.size
            loadMoreProjects(repository, filters, nextOffset, scope) { result ->
                isLoadingMore = false
                result.onSuccess { searchResult ->
                    uiState = MarketplaceUiState.Success(
                        projects = success.projects + searchResult.projects,
                        totalHits = searchResult.totalHits,
                        hasMore = searchResult.offset + searchResult.limit < searchResult.totalHits
                    )
                }
            }
        }
    }

    var initialSearchFired by remember { mutableStateOf(false) }
    LaunchedEffect(filters) {
        if (!initialSearchFired && uiState is MarketplaceUiState.Initial) {
            uiState = MarketplaceUiState.Loading
            searchProjects(repository, filters, scope) { result ->
                result.onSuccess { searchResult ->
                    currentOffset = searchResult.projects.size
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
        initialSearchFired = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add-ons",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MarketplaceFiltersBar(
                filters = filters,
                onFilterChange = { newFilters ->
                    filters = newFilters
                    marketplacePreferences.updateFilters(newFilters)
                    isLoadingMore = false
                    scope.launch {
                        searchProjects(repository, filters, scope) { result ->
                            result.onSuccess { searchResult ->
                                currentOffset = searchResult.projects.size
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
                },
                onSearch = { query ->
                    filters = filters.copy(query = query)
                    marketplacePreferences.updateFilters(filters)
                    if (query.isBlank() && filters.version == null && filters.loader == null &&
                        filters.projectType == null && filters.categories.isEmpty()) {
                        uiState = MarketplaceUiState.Initial
                        return@MarketplaceFiltersBar
                    }
                    uiState = MarketplaceUiState.Loading
                    isLoadingMore = false
                    scope.launch {
                        searchProjects(repository, filters, scope) { result ->
                            result.onSuccess { searchResult ->
                                currentOffset = searchResult.projects.size
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
                },
                onClear = {
                    filters = MarketplaceFilters()
                    marketplacePreferences.updateFilters(filters)
                    uiState = MarketplaceUiState.Initial
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (val state = uiState) {
                is MarketplaceUiState.Initial -> {
                    InitialContent(
                        onSearch = {
                            isLoadingMore = false
                            scope.launch {
                                searchProjects(repository, filters, scope) { result ->
                                    result.onSuccess { searchResult ->
                                        currentOffset = searchResult.projects.size
                                        uiState = MarketplaceUiState.Success(
                                            projects = searchResult.projects,
                                            totalHits = searchResult.totalHits,
                                            hasMore = searchResult.offset + searchResult.limit < searchResult.totalHits
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is MarketplaceUiState.Loading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(420.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(6) {
                            MarketplaceCardSkeleton()
                        }
                    }
                }

                is MarketplaceUiState.Success -> {
                    if (state.projects.isEmpty()) {
                        EmptyResults(
                            onClearFilters = {
                                filters = MarketplaceFilters()
                                marketplacePreferences.updateFilters(filters)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyVerticalGrid(
                            state = listState,
                            columns = GridCells.Adaptive(420.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = "${state.totalHits} results",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            items(
                                items = state.projects,
                                key = { it.id }
                            ) { project ->
                                MarketplaceCard(
                                    project = project,
                                    onClick = { onNavigateToDetail(project.id) },
                                    onInstallClick = { onNavigateToDetail(project.id) },
                                    formatDownloads = { formatDownloads(it) }
                                )
                            }

                            if (state.hasMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is MarketplaceUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = {
                            isLoadingMore = false
                            scope.launch {
                                searchProjects(repository, filters, scope) { result ->
                                    result.onSuccess { searchResult ->
                                        currentOffset = searchResult.projects.size
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
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

}

private suspend fun searchProjects(
    repository: MarketplaceRepository,
    filters: MarketplaceFilters,
    scope: kotlinx.coroutines.CoroutineScope,
    callback: (Result<com.portalhost.model.ModrinthSearchResult>) -> Unit
) {
    repository.searchProjects(
        query = filters.query,
        version = filters.version,
        loader = filters.loader,
        projectType = filters.projectType,
        categories = filters.categories,
        sort = filters.sort,
        offset = 0,
        limit = 20
    ).let(callback)
}

private fun loadMoreProjects(
    repository: MarketplaceRepository,
    filters: MarketplaceFilters,
    offset: Int,
    scope: kotlinx.coroutines.CoroutineScope,
    callback: (Result<com.portalhost.model.ModrinthSearchResult>) -> Unit
) {
    scope.launch {
        repository.searchProjects(
            query = filters.query,
            version = filters.version,
            loader = filters.loader,
            projectType = filters.projectType,
            categories = filters.categories,
            sort = filters.sort,
            offset = offset,
            limit = 20
        ).let(callback)
    }
}

@Composable
private fun InitialContent(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Store,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Discover Add-ons",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Search for plugins, mods, datapacks, and more from Modrinth",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Use the search bar and filters above to find add-ons",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyResults(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try adjusting your filters or search terms",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onClearFilters) {
            Text("Clear filters")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Failed to load",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}