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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.portalhost.marketplace.MarketplacePreferences
import com.portalhost.marketplace.MarketplaceRepository
import com.portalhost.marketplace.formatDownloads
import com.portalhost.marketplace.isProjectCompatible
import com.portalhost.model.MarketplaceFilters
import com.portalhost.model.MarketplaceUiState
import com.portalhost.model.ModrinthProject
import com.portalhost.model.ModrinthVersion
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerInstallTarget
import com.portalhost.server.ServerManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.awt.Desktop
import java.io.File
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val repository: MarketplaceRepository = koinInject()
    val marketplacePreferences: MarketplacePreferences = koinInject()
    val serverManager: ServerManager = koinInject()

    var uiState by remember { mutableStateOf<MarketplaceUiState>(MarketplaceUiState.Initial) }
    var filters by remember { mutableStateOf(marketplacePreferences.loadFilters()) }
    var selectedProject by remember { mutableStateOf<ModrinthProject?>(null) }
    var projectVersions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var selectedVersion by remember { mutableStateOf<ModrinthVersion?>(null) }
    var detailSheetVisible by remember { mutableStateOf(false) }
    var installModalVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTarget by remember { mutableStateOf<ServerInstallTarget?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var currentOffset by remember { mutableStateOf(0) }

    val servers: List<ServerConfig> = serverManager.servers.value.values.toList()
    val serversDirBase = File(File(System.getProperty("user.home"), ".portalhost"), "servers")

    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 4 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        val success = uiState as? MarketplaceUiState.Success ?: return@LaunchedEffect
        if (success.hasMore && !isDownloading) {
            val nextOffset = success.projects.size
            loadMoreProjects(repository, filters, nextOffset, scope) { result ->
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
            val hasContent = filters.query.isNotBlank() ||
                filters.version != null ||
                filters.loader != null ||
                filters.projectType != null ||
                filters.categories.isNotEmpty()
            if (hasContent) {
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
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
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
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
                                    onClick = {
                                        selectedProject = project
                                        projectVersions = emptyList()
                                        selectedVersion = null
                                        detailSheetVisible = true
                                        scope.launch {
                                            repository.getProjectVersions(project.id).onSuccess { versions ->
                                                projectVersions = versions
                                                selectedVersion = versions.firstOrNull()
                                            }
                                        }
                                    },
                                    onInstallClick = {
                                        selectedProject = project
                                        projectVersions = emptyList()
                                        selectedVersion = null
                                        detailSheetVisible = true
                                        scope.launch {
                                            repository.getProjectVersions(project.id).onSuccess { versions ->
                                                projectVersions = versions
                                                selectedVersion = versions.firstOrNull()
                                            }
                                        }
                                    },
                                    formatDownloads = { formatDownloads(it) }
                                )
                            }

                            if (state.hasMore) {
                                item {
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

    if (detailSheetVisible && selectedProject != null) {
        MarketplaceDetailSheet(
            project = selectedProject!!,
            versions = projectVersions,
            selectedVersion = selectedVersion,
            onVersionSelect = { version -> selectedVersion = version },
            onInstallClick = {
                if (servers.isNotEmpty()) {
                    detailSheetVisible = false
                    installModalVisible = true
                }
            },
            onChangelogClick = { url ->
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(url))
                    } else {
                        Runtime.getRuntime().exec(arrayOf("xdg-open", url))
                    }
                } catch (e: Exception) {
                    try {
                        Runtime.getRuntime().exec(arrayOf("xdg-open", url))
                    } catch (_: Exception) {
                        try {
                            Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", url))
                        } catch (_: Exception) { }
                    }
                }
            },
            onDismiss = {
                detailSheetVisible = false
                selectedProject = null
                projectVersions = emptyList()
                selectedVersion = null
            },
            sheetState = detailSheetState
        )
    }

    if (installModalVisible) {
        InstallTargetModal(
            targets = getInstallTargets(selectedProject, selectedVersion, servers),
            selectedTarget = selectedTarget,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            getInstallPath = { target ->
                val server = servers.find { it.id == target.serverId }
                val serverDir = server?.name?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: target.serverName
                File(serversDirBase, "$serverDir/${target.folderHint}").absolutePath
            },
            onTargetSelect = { target -> selectedTarget = target },
            onInstall = {
                selectedTarget?.let {
                    isDownloading = true
                    downloadProgress = 0f
                }
            },
            onCancel = {
                installModalVisible = false
                isDownloading = false
                selectedTarget = null
            }
        )
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

private fun getInstallTargets(
    project: ModrinthProject?,
    version: ModrinthVersion?,
    servers: List<ServerConfig>
): List<ServerInstallTarget> {
    if (project == null) return emptyList()

    return servers.map { server ->
        val serverLoader = server.serverType.name.lowercase()
        val compatibilityVersion = version?.gameVersions?.firstOrNull { it.startsWith("1.") }
            ?: server.version
        val compatible = isProjectCompatible(project, compatibilityVersion, serverLoader)
        val folderHint = getSuggestedFolder(project, version)

        ServerInstallTarget(
            serverId = server.id,
            serverName = server.name,
            serverVersion = server.version,
            serverType = server.serverType.name,
            compatible = compatible,
            folderHint = folderHint
        )
    }
}

private fun getSuggestedFolder(project: ModrinthProject, version: ModrinthVersion?): String {
    val primaryLoader = version?.loaders?.firstOrNull()?.lowercase()
        ?: project.loaders.firstOrNull()?.lowercase()
    return when {
        primaryLoader in listOf("paper", "spigot", "purpur", "folia") -> "plugins"
        primaryLoader in listOf("forge", "neoforge") -> "mods"
        primaryLoader in listOf("fabric", "quilt") -> "mods"
        project.projectType == "datapack" -> "datapacks"
        project.projectType == "resourcepack" -> "resourcepacks"
        project.projectType == "shader" -> "shaderpacks"
        else -> "plugins"
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