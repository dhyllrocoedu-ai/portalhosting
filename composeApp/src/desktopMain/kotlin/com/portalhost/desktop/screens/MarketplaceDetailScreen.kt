package com.portalhost.desktop.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portalhost.desktop.marketplace.SimpleAsyncImage
import com.portalhost.marketplace.MarketplaceRepository
import com.portalhost.marketplace.formatDownloads
import com.portalhost.marketplace.isProjectCompatible
import com.portalhost.model.ModrinthProject
import com.portalhost.model.ModrinthVersion
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerInstallTarget
import com.portalhost.server.ServerManager
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.compose.koinInject
import java.awt.Desktop
import java.net.URI

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

    var project by remember { mutableStateOf<ModrinthProject?>(null) }
    var versions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedVersion by remember { mutableStateOf<ModrinthVersion?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    val servers: List<ServerConfig> = serverManager.servers.value.values.toList()

    LaunchedEffect(projectId) {
        isLoading = true
        repository.getProject(projectId)
            .onSuccess { p ->
                project = p
                repository.getProjectVersions(projectId)
                    .onSuccess { vs ->
                        versions = vs
                        selectedVersion = vs.firstOrNull()
                    }
                    .onFailure { e ->
                        logger.warn { "Failed to load versions for $projectId: ${e.message}" }
                    }
            }
            .onFailure { e -> error = e.message }
        isLoading = false
    }

    val tabs = listOf("Description", "Versions", "Changelog")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = project?.title ?: "Project Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                val filteredVersions = versions

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        SimpleAsyncImage(
                            url = p.iconUrl,
                            contentDescription = p.title,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = p.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        p.author?.let {
                            Text(
                                text = "by $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text(p.projectType.replaceFirstChar { it.uppercase() }) }
                            )
                            p.loaders.firstOrNull()?.let { loader ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(loader.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

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

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        TabRow(selectedTabIndex = selectedTabIndex) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title) }
                                )
                            }
                        }

                        when (selectedTabIndex) {
                            0 -> DescriptionTab(p)
                            1 -> VersionsTab(
                                versions = filteredVersions,
                                selectedVersion = selectedVersion,
                                onVersionSelect = { selectedVersion = it }
                            )
                            2 -> ChangelogTab(selectedVersion)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                selectedVersion?.let { v ->
                                    Text(
                                        text = v.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${v.gameVersions.joinToString(", ")} \u2022 ${v.loaders.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isDownloading) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.width(200.dp)
                                )
                            } else {
                                Button(
                                    onClick = {
                                        if (servers.isNotEmpty() && selectedVersion != null) {
                                            isDownloading = true
                                            downloadProgress = 0f
                                            scope.launch {
                                                val target = servers.firstOrNull()
                                                if (target != null) {
                                                    val file = selectedVersion!!.files.firstOrNull()
                                                    if (file != null) {
                                                        downloadProgress = 1f
                                                    }
                                                }
                                                isDownloading = false
                                            }
                                        }
                                    },
                                    enabled = selectedVersion != null && servers.isNotEmpty()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Install to Server")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DescriptionTab(project: ModrinthProject) {
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
            MarkdownBody(body)
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
    lines.forEach { line ->
        when {
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

@Composable
private fun VersionsTab(
    versions: List<ModrinthVersion>,
    selectedVersion: ModrinthVersion?,
    onVersionSelect: (ModrinthVersion) -> Unit
) {
    if (versions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No versions available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        versions.take(50).forEach { version ->
            VersionDetailRow(
                version = version,
                isSelected = selectedVersion?.id == version.id,
                onClick = { onVersionSelect(version) }
            )
        }
    }
}

@Composable
private fun VersionDetailRow(
    version: ModrinthVersion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun ChangelogTab(selectedVersion: ModrinthVersion?) {
    if (selectedVersion == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        modifier = Modifier.clickable {
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
