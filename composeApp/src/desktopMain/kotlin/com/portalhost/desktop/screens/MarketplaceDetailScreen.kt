package com.portalhost.desktop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.desktop.marketplace.HtmlBody
import com.portalhost.desktop.marketplace.SimpleAsyncImage
import com.portalhost.marketplace.MarketplaceRepository
import com.portalhost.marketplace.formatDownloads
import com.portalhost.marketplace.getSuggestedFolder
import com.portalhost.marketplace.isProjectCompatible
import com.portalhost.marketplace.isVersionCompatible
import com.portalhost.model.ModrinthGalleryItem
import com.portalhost.model.ModrinthProject
import com.portalhost.model.ModrinthVersion
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerInstallTarget
import com.portalhost.model.ServerType
import com.portalhost.server.ServerManager
import com.portalhost.server.providers.downloadToFile
import com.portalhost.uinotify.ToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.compose.koinInject
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URL

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceDetailScreen(
    projectId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val repository: MarketplaceRepository = koinInject()
    val serverManager: ServerManager = koinInject()
    val toastManager: ToastManager = koinInject()

    var project by remember { mutableStateOf<ModrinthProject?>(null) }
    var versions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedVersion by remember { mutableStateOf<ModrinthVersion?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var showInstallModal by remember { mutableStateOf(false) }
    var selectedTarget by remember { mutableStateOf<ServerInstallTarget?>(null) }

    // Version filter state
    var filterGameVersion by remember { mutableStateOf<String?>(null) }
    var filterLoader by remember { mutableStateOf<String?>(null) }
    var filterVersionType by remember { mutableStateOf<String?>(null) }
    var versionSearchQuery by remember { mutableStateOf("") }

    val servers: List<ServerConfig> = serverManager.servers.value.values.toList()

    // Compute accent color from project
    val accentColor = remember(project) {
        project?.color?.let { Color(it or 0xFF000000.toInt()) }
    }

    LaunchedEffect(projectId) {
        isLoading = true
        repository.getProject(projectId)
            .onSuccess { p ->
                project = p
                repository.getProjectVersions(projectId)
                    .onSuccess { vs ->
                        versions = vs
                    }
                    .onFailure { e ->
                        logger.warn { "Failed to load versions for $projectId: ${e.message}" }
                    }
            }
            .onFailure { e -> error = e.message }
        isLoading = false
    }

    val tabs = listOf("Description", "Versions", "Changelog")

    // Filter versions based on filter state
    val filteredVersions = remember(versions, filterGameVersion, filterLoader, filterVersionType, versionSearchQuery) {
        versions.filter { v ->
            (filterGameVersion == null || filterGameVersion in v.gameVersions) &&
            (filterLoader == null || filterLoader in v.loaders) &&
            (filterVersionType == null || v.versionType == filterVersionType) &&
            (versionSearchQuery.isBlank() || v.name.contains(versionSearchQuery, ignoreCase = true) ||
                v.versionNumber.contains(versionSearchQuery, ignoreCase = true))
        }
    }

    // Get unique filter values from all versions
    val allGameVersions = remember(versions) {
        versions.flatMap { it.gameVersions }.distinct().sortedDescending()
    }
    val allLoaders = remember(versions) {
        versions.flatMap { it.loaders }.distinct().sorted()
    }
    val allVersionTypes = remember(versions) {
        versions.map { it.versionType }.distinct().sorted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    project?.let { p ->
                        Text(
                            text = p.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } ?: Text("Project Details", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    project?.let { p ->
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            p.sourceUrl?.let { url ->
                                IconButton(onClick = { openInBrowser(url) }) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = "Source code")
                                }
                            }
                            p.discordUrl?.let { url ->
                                IconButton(onClick = { openInBrowser(url) }) {
                                    Icon(Icons.Default.Chat, contentDescription = "Discord")
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            project?.let { p ->
                BottomInstallBar(
                    selectedVersion = selectedVersion,
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    servers = servers,
                    projectType = p.projectType,
                    onInstallClick = {
                        if (servers.isNotEmpty() && selectedVersion != null) {
                            selectedTarget = null
                            showInstallModal = true
                        }
                    },
                    accentColor = accentColor
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Failed to load: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            project != null -> {
                val p = project!!
                val gallery = p.gallery?.sortedBy { it.ordering ?: 0 } ?: emptyList<ModrinthGalleryItem>()

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Left sidebar
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        // Project header with icon
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SimpleAsyncImage(
                                url = p.iconUrl,
                                contentDescription = p.title,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = p.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            p.author?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "by $it",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Category chips
                            if (p.categories.isNotEmpty()) {
                                HorizontalScrollableChips(p.categories)
                                Spacer(Modifier.height(12.dp))
                            }

                            // Project type + loaders
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CategoryChip(
                                    label = p.projectType.replaceFirstChar { it.uppercase() },
                                    isPrimary = true
                                )
                                p.loaders.firstOrNull()?.let { loader ->
                                    CategoryChip(
                                        label = loader.replaceFirstChar { it.uppercase() },
                                        isPrimary = false
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))

                        // Stats
                        StatRow("Downloads", formatDownloads(p.downloads))
                        StatRow("Followers", formatDownloads(p.followers))
                        Spacer(Modifier.height(4.dp))
                        SideRow("Server", p.serverSide)
                        SideRow("Client", p.clientSide)

                        p.license?.let { lic ->
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "License",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = lic.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        // Links
                        listOfNotNull(
                            "Source" to p.sourceUrl,
                            "Issues" to p.issuesUrl,
                            "Wiki" to p.wikiUrl,
                            "Discord" to p.discordUrl
                        ).forEach { (label, url) ->
                            if (url != null) {
                                ExternalLink(label, url)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }

                    // Main content area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Gallery carousel
                        if (gallery.isNotEmpty()) {
                            GalleryCarousel(gallery = gallery)
                            HorizontalDivider()
                        }

                        // Tab row
                        PrimaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal) }
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTabIndex) {
                                0 -> DescriptionTab(project = p, accentColor = accentColor)
                                1 -> VersionsTab(
                                    versions = filteredVersions,
                                    allVersions = versions,
                                    selectedVersion = selectedVersion,
                                    onVersionSelect = { selectedVersion = it },
                                    onFilterChange = { gameVer, loader, type ->
                                        filterGameVersion = gameVer
                                        filterLoader = loader
                                        filterVersionType = type
                                    },
                                    filterGameVersion = filterGameVersion,
                                    filterLoader = filterLoader,
                                    filterVersionType = filterVersionType,
                                    versionSearchQuery = versionSearchQuery,
                                    onSearchChange = { versionSearchQuery = it },
                                    allGameVersions = allGameVersions,
                                    allLoaders = allLoaders,
                                    allVersionTypes = allVersionTypes,
                                    accentColor = accentColor
                                )
                                2 -> ChangelogTab(
                                    selectedVersion = selectedVersion,
                                    accentColor = accentColor
                                )
                            }
                        }
                    }
                }

                val installTargets: List<ServerInstallTarget> = servers.map { s ->
                    ServerInstallTarget(
                        serverId = s.id,
                        serverName = s.name,
                        serverVersion = s.version,
                        serverType = s.serverType.name.lowercase().replaceFirstChar { it.uppercase() },
                        compatible = selectedVersion?.let { isVersionCompatible(it, s.version, serverTypeToLoader(s.serverType)) }
                            ?: isProjectCompatible(p, s.version, serverTypeToLoader(s.serverType)),
                        folderHint = getSuggestedFolder(p, serverTypeToLoader(s.serverType))
                    )
                }

                if (showInstallModal) {
                    InstallTargetModal(
                        targets = installTargets,
                        selectedTarget = selectedTarget,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        getInstallPath = { target ->
                            File(serverManager.getServerDir(target.serverId), target.folderHint).absolutePath
                        },
                        onTargetSelect = { selectedTarget = it },
                        onInstall = {
                            val target = selectedTarget ?: return@InstallTargetModal
                            val version = selectedVersion ?: return@InstallTargetModal
                            val file = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull() ?: return@InstallTargetModal
                            scope.launch(Dispatchers.IO) {
                                isDownloading = true
                                downloadProgress = 0f
                                try {
                                    val destDir = File(serverManager.getServerDir(target.serverId), target.folderHint)
                                    destDir.mkdirs()
                                    val destFile = File(destDir, file.filename)
                                    URL(file.url).downloadToFile(
                                        destination = destFile,
                                        headers = mapOf("User-Agent" to "PortalHost/5.0.69")
                                    ) { downloaded, total ->
                                        if (total > 0) {
                                            downloadProgress = (downloaded.toDouble() / total).toFloat()
                                        }
                                    }
                                    isDownloading = false
                                    showInstallModal = false
                                    toastManager.success("Installed ${file.filename} to ${target.serverName}")
                                } catch (e: Exception) {
                                    isDownloading = false
                                    logger.error(e) { "Failed to install marketplace file" }
                                    toastManager.error("Failed to install: ${e.message}")
                                }
                            }
                        },
                        onCancel = { showInstallModal = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryCarousel(gallery: List<ModrinthGalleryItem>) {
    val scrollState = rememberScrollState()
    val itemWidth = 320.dp
    val itemHeight = 200.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .horizontalScroll(scrollState)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.width(16.dp))
            gallery.forEach { image ->
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(itemHeight)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    SimpleAsyncImage(
                        url = image.url,
                        contentDescription = image.title ?: "Gallery image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        }
    }
}

@Composable
private fun HorizontalScrollableChips(categories: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.take(10).forEach { category ->
            CategoryChip(
                label = category.replaceFirstChar { it.uppercase() },
                isPrimary = false
            )
        }
    }
}

@Composable
private fun DescriptionTab(project: ModrinthProject, accentColor: androidx.compose.ui.graphics.Color?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = project.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        project.body?.let { body ->
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            HtmlBody(body)
        }
        project.bodyUrl?.let { url ->
            Spacer(Modifier.height(12.dp))
            ExternalLink("View full description", url)
        }
    }
}

@Composable
private fun MarkdownBody(markdown: String) {
    val lines = markdown.split("\n")
    var inList = false
    var inCodeBlock = false
    var codeBlockContent = ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { line ->
            when {
                line.startsWith("```") -> {
                    if (!inCodeBlock) {
                        inCodeBlock = true
                        codeBlockContent = ""
                    } else {
                        inCodeBlock = false
                        Text(
                            text = codeBlockContent.trimEnd(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                inCodeBlock -> {
                    codeBlockContent += line + "\n"
                }
                line.startsWith("### ") -> {
                    inList = false
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                }
                line.startsWith("## ") -> {
                    inList = false
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                }
                line.startsWith("# ") -> {
                    inList = false
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    if (!inList) { inList = true; Spacer(Modifier.height(4.dp)) }
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text("\u2022  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = line.removePrefix("- ").removePrefix("* "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.isBlank() -> { inList = false; Spacer(Modifier.height(8.dp)) }
                else -> {
                    inList = false
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionsTab(
    versions: List<ModrinthVersion>,
    allVersions: List<ModrinthVersion>,
    selectedVersion: ModrinthVersion?,
    onVersionSelect: (ModrinthVersion) -> Unit,
    onFilterChange: (String?, String?, String?) -> Unit,
    filterGameVersion: String?,
    filterLoader: String?,
    filterVersionType: String?,
    versionSearchQuery: String,
    onSearchChange: (String) -> Unit,
    allGameVersions: List<String>,
    allLoaders: List<String>,
    allVersionTypes: List<String>,
    accentColor: androidx.compose.ui.graphics.Color?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = versionSearchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search versions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (versionSearchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true
                )
                FilterChip(
                    selected = filterGameVersion != null || filterLoader != null || filterVersionType != null,
                    onClick = { },
                    label = { Text("Filters") },
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Filter dropdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (allGameVersions.isNotEmpty()) {
                    VersionFilterDropdown(
                        label = "Game Version",
                        value = filterGameVersion,
                        options = allGameVersions,
                        onSelect = { selected ->
                            onFilterChange(selected, filterLoader, filterVersionType)
                        }
                    )
                }
                if (allLoaders.isNotEmpty()) {
                    VersionFilterDropdown(
                        label = "Loader",
                        value = filterLoader,
                        options = allLoaders,
                        onSelect = { selected ->
                            onFilterChange(filterGameVersion, selected, filterVersionType)
                        }
                    )
                }
                if (allVersionTypes.isNotEmpty()) {
                    VersionFilterDropdown(
                        label = "Type",
                        value = filterVersionType,
                        options = allVersionTypes.map { it.replaceFirstChar { it.uppercase() } },
                        optionValues = allVersionTypes,
                        onSelect = { selected ->
                            onFilterChange(filterGameVersion, filterLoader, selected)
                        }
                    )
                }
            }
        }

        // Version count
        Text(
            text = "Showing ${versions.size} of ${allVersions.size} versions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
        )

        // Version list
        if (versions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No versions match the current filters", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            versions.forEach { version ->
                VersionDetailRow(
                    version = version,
                    isSelected = selectedVersion?.id == version.id,
                    onClick = { onVersionSelect(version) },
                    accentColor = accentColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionFilterDropdown(
    label: String,
    value: String?,
    options: List<String>,
    optionValues: List<String>? = null,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val values = optionValues ?: options

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.width(160.dp)
    ) {
        OutlinedTextField(
            value = value ?: label,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All", fontWeight = if (value == null) FontWeight.SemiBold else FontWeight.Normal) },
                onClick = { onSelect(null); expanded = false }
            )
            options.forEachIndexed { index, option ->
                val optionValue = values[index]
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(optionValue); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun ChangelogTab(selectedVersion: ModrinthVersion?, accentColor: androidx.compose.ui.graphics.Color?) {
    if (selectedVersion == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Select a version to view its changelog", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = selectedVersion.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Released ${selectedVersion.datePublished.take(10)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        if (selectedVersion.changelog != null) {
            MarkdownBody(selectedVersion.changelog)
        } else if (selectedVersion.changelogUrl != null) {
            Text(
                text = "Changelog available externally",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            ExternalLink("View changelog", selectedVersion.changelogUrl)
        } else {
            Text(
                text = "No changelog available for this version",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VersionDetailRow(
    version: ModrinthVersion,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color? = null
) {
    val effectiveAccent = accentColor ?: MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(if (isSelected) 1.dp else 0.dp, if (isSelected) effectiveAccent else Color.Transparent, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Thin left accent strip when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(effectiveAccent, RoundedCornerShape(topStart = 8.dp, topEnd = 0.dp))
                        .padding(bottom = 8.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = version.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                VersionTypeChip(type = version.versionType)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = version.gameVersions.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    version.loaders.forEach { loader ->
                        Text(
                            text = loader,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "${formatDownloads(version.downloads)} downloads \u2022 ${version.datePublished.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BottomInstallBar(
    selectedVersion: ModrinthVersion?,
    isDownloading: Boolean,
    downloadProgress: Float,
    servers: List<ServerConfig>,
    projectType: String,
    onInstallClick: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color?
) {
    val effectiveAccent = accentColor ?: MaterialTheme.colorScheme.primary
    val isClientSide = isClientSideProject(projectType)
    val hasSelection = selectedVersion != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                if (hasSelection)
                    effectiveAccent.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .border(if (hasSelection) 1.dp else 0.dp, if (hasSelection) effectiveAccent.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedVersion?.let { _ ->
                // Tiny "✓ Selected" indicator using the accent color
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(effectiveAccent)
                        .padding(end = 4.dp)
                )
                Text(
                    text = "Ready to install",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (hasSelection) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else effectiveAccent
                )
            } ?: Text(
                text = "Select a version to install",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isClientSide) {
                Text(
                    text = "This is a client-side addon (${projectType.replaceFirstChar { it.uppercase() }}) — cannot be installed to a server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (isDownloading) {
            LinearProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier.width(200.dp),
                color = effectiveAccent
            )
        } else {
            Button(
                onClick = onInstallClick,
                enabled = selectedVersion != null && servers.isNotEmpty() && !isClientSide,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isClientSide) MaterialTheme.colorScheme.surfaceContainerHighest else effectiveAccent,
                    contentColor = if (isClientSide) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isClientSide) "Client-side Only" else "Install to Server")
            }
        }
    }
}

private fun isClientSideProject(projectType: String): Boolean {
    val pt = projectType.lowercase()
    return pt in listOf("shader", "resourcepack", "modpack")
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SideRow(label: String, side: String) {
    val (icon, color) = when (side) {
        "required" -> "\u2713" to MaterialTheme.colorScheme.primary
        "optional" -> "\u25CB" to MaterialTheme.colorScheme.tertiary
        else -> "\u2717" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$icon $side",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ExternalLink(label: String, url: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(url))
                    } else {
                        Runtime.getRuntime().exec(arrayOf("xdg-open", url))
                    }
                } catch (_: Exception) {
                    try { Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", url)) } catch (_: Exception) { }
                }
            }
    ) {
        Icon(
            Icons.Default.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun VersionTypeChip(type: String) {
    val (bgColor, label) = when (type.lowercase()) {
        "release" -> MaterialTheme.colorScheme.primaryContainer to "Release"
        "beta" -> MaterialTheme.colorScheme.tertiaryContainer to "Beta"
        "alpha" -> MaterialTheme.colorScheme.errorContainer to "Alpha"
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to type.replaceFirstChar { it.uppercase() }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

private fun openInBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            Runtime.getRuntime().exec(arrayOf("xdg-open", url))
        }
    } catch (_: Exception) {
        try { Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", url)) } catch (_: Exception) { }
    }
}

private fun serverTypeToLoader(type: ServerType): String = when (type) {
    ServerType.PAPER -> "paper"
    ServerType.VANILLA -> "vanilla"
    ServerType.FABRIC -> "fabric"
    ServerType.FORGE -> "forge"
    ServerType.NEOFORGE -> "neoforge"
    ServerType.PURPUR -> "purpur"
    ServerType.FOLIA -> "folia"
}
