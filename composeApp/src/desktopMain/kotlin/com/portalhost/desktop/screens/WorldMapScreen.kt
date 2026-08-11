package com.portalhost.desktop.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.player.EntityPositionService
import com.portalhost.player.PlayerPos3
import com.portalhost.server.ServerManager
import com.portalhost.world.ChunkCoord
import com.portalhost.world.RegionFileIndex
import com.portalhost.world.RegionIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File

private val ZOOM_LEVELS = listOf(1f, 2f, 4f, 8f, 16f)
private const val DEFAULT_ZOOM_INDEX = 1
private const val POLL_INTERVAL_MS = 3000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldMapScreen(serverId: String, onBack: () -> Unit = {}) {
    val serverManager = koinInject<ServerManager>()
    val serverDir = serverManager.getServerDir(serverId)
    val config = serverManager.servers.value[serverId]
    val entityService = remember { EntityPositionService() }

    var worldDirs by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedWorld by remember { mutableStateOf<File?>(null) }
    var regionIndices by remember { mutableStateOf<List<RegionIndex>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var zoomIndex by remember { mutableStateOf(DEFAULT_ZOOM_INDEX) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var selectedChunk by remember { mutableStateOf<ChunkCoord?>(null) }
    var playerPositions by remember { mutableStateOf<Map<String, PlayerPos3>>(emptyMap()) }
    var showWorldDropdown by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }

    val serverStates by serverManager.serverStates.collectAsState()
    val state = serverStates[serverId]
    val playerNames = state?.players ?: emptyList()

    LaunchedEffect(serverDir) {
        withContext(Dispatchers.IO) {
            val candidates = listOf("world", "world_nether", "world_the_end")
                .map { File(serverDir, it) }
                .filter { it.isDirectory }
            worldDirs = candidates
            selectedWorld = candidates.firstOrNull()
        }
    }

    LaunchedEffect(selectedWorld) {
        val world = selectedWorld ?: return@LaunchedEffect
        loading = true
        lastError = null
        try {
            val regions = withContext(Dispatchers.IO) {
                val regionDir = File(world, "region")
                if (regionDir.isDirectory) {
                    RegionFileIndex().indexDirectory(regionDir)
                } else emptyList()
            }
            regionIndices = regions
        } catch (e: Exception) {
            lastError = "Failed to read region files: ${e.message}"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(serverId, config?.rconPort, config?.rconPassword, playerNames, selectedWorld) {
        if (config?.rconEnabled != true || config.rconPassword.isNullOrBlank()) return@LaunchedEffect
        while (isActive) {
            try {
                val positions = entityService.positions(
                    host = "localhost",
                    port = config.rconPort,
                    password = config.rconPassword,
                    names = playerNames,
                )
                playerPositions = positions
            } catch (_: Exception) {
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            WorldMapTopBar(
                title = config?.name ?: serverDir.name,
                onBack = onBack,
                worlds = worldDirs,
                selectedWorld = selectedWorld,
                showDropdown = showWorldDropdown,
                onToggleDropdown = { showWorldDropdown = !showWorldDropdown },
                onSelectWorld = {
                    selectedWorld = it
                    showWorldDropdown = false
                    selectedChunk = null
                },
            )
            Toolbar(
                zoomIndex = zoomIndex,
                onZoomIn = { zoomIndex = (zoomIndex + 1).coerceAtMost(ZOOM_LEVELS.lastIndex) },
                onZoomOut = { zoomIndex = (zoomIndex - 1).coerceAtLeast(0) },
                onCenter = {
                    panOffset = Offset.Zero
                    val first = playerPositions.values.firstOrNull()
                    if (first != null) {
                        panOffset = Offset(
                            x = -((first.x / 16.0).toFloat()) * (canvasSize.width / (regionIndices.size.toFloat().coerceAtLeast(1f))),
                            y = -((first.z / 16.0).toFloat()) * (canvasSize.height / (regionIndices.size.toFloat().coerceAtLeast(1f))),
                        )
                    }
                },
                onRefresh = {
                    selectedChunk = null
                    val world = selectedWorld
                    if (world != null) {
                        loading = true
                        try {
                            val regions = RegionFileIndex().indexDirectory(File(world, "region"))
                            regionIndices = regions
                        } catch (e: Exception) {
                            lastError = "Failed to read region files: ${e.message}"
                        } finally {
                            loading = false
                        }
                    }
                },
                chunkCount = regionIndices.sumOf { it.chunks.values.count { c -> c.generated } },
            )
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading region files...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (regionIndices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No region files", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            lastError ?: "world/region/ directory not found or empty",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    MapCanvas(
                        regions = regionIndices,
                        zoom = ZOOM_LEVELS[zoomIndex],
                        panOffset = panOffset,
                        onPanChange = { panOffset += it },
                        onCanvasSize = { canvasSize = it },
                        onChunkTapped = { selectedChunk = it },
                        selectedChunk = selectedChunk,
                        playerPositions = playerPositions,
                    )
                    selectedChunk?.let { chunk ->
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Text(
                                "Selected: chunk (${chunk.x}, ${chunk.z}) · region (${chunk.regionX}, ${chunk.regionZ})",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldMapTopBar(
    title: String,
    onBack: () -> Unit,
    worlds: List<File>,
    selectedWorld: File?,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
    onSelectWorld: (File) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("World Map · $title", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(selectedWorld?.name ?: "no world", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Box {
                TextButton(onClick = onToggleDropdown) {
                    Text("World: ${selectedWorld?.name ?: "—"}")
                }
                DropdownMenu(expanded = showDropdown, onDismissRequest = onToggleDropdown) {
                    worlds.forEach { world ->
                        DropdownMenuItem(
                            text = { Text(world.name) },
                            onClick = { onSelectWorld(world) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Toolbar(
    zoomIndex: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenter: () -> Unit,
    onRefresh: () -> Unit,
    chunkCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onZoomOut, enabled = zoomIndex > 0) {
            Icon(Icons.Default.Remove, contentDescription = "Zoom out")
        }
        Text("${ZOOM_LEVELS[zoomIndex].toInt()}x", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = onZoomIn, enabled = zoomIndex < ZOOM_LEVELS.lastIndex) {
            Icon(Icons.Default.Add, contentDescription = "Zoom in")
        }
        IconButton(onClick = onCenter) {
            Icon(Icons.Default.MyLocation, contentDescription = "Center on player")
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("$chunkCount chunks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MapCanvas(
    regions: List<RegionIndex>,
    zoom: Float,
    panOffset: Offset,
    onPanChange: (Offset) -> Unit,
    onCanvasSize: (Size) -> Unit,
    onChunkTapped: (ChunkCoord?) -> Unit,
    selectedChunk: ChunkCoord?,
    playerPositions: Map<String, PlayerPos3>,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(regions) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    if (zoomChange != 1f) return@detectTransformGestures
                    onPanChange(pan)
                }
            }
            .pointerInput(regions) {
                detectTapGestures { offset ->
                    val coord = hitTest(regions, offset, zoom, panOffset, Size(size.width.toFloat(), size.height.toFloat()))
                    onChunkTapped(coord)
                }
            }
    ) {
        onCanvasSize(size)
        drawRect(color = Color(0xFF101820), size = size)
        val cellPx = (6f * zoom).coerceAtLeast(1f)
        val visible = mutableMapOf<ChunkCoord, Boolean>()
        regions.forEach { region ->
            region.chunks.forEach { (coord, presence) ->
                if (!presence.generated) return@forEach
                val centerX = (region.regionX * ChunkCoord.REGION_SIZE + coord.localX) * cellPx + panOffset.x + size.width / 2f
                val centerZ = (region.regionZ * ChunkCoord.REGION_SIZE + coord.localZ) * cellPx + panOffset.y + size.height / 2f
                if (centerX < -cellPx || centerX > size.width + cellPx) return@forEach
                if (centerZ < -cellPx || centerZ > size.height + cellPx) return@forEach
                visible[coord] = true
                drawRect(
                    color = Color(0xFF4A8FCC).copy(alpha = 0.55f),
                    topLeft = Offset(centerX, centerZ),
                    size = Size(cellPx - 1f, cellPx - 1f),
                )
                if (selectedChunk == coord) {
                    drawRect(
                        color = Color(0xFFFFCC00),
                        topLeft = Offset(centerX, centerZ),
                        size = Size(cellPx - 1f, cellPx - 1f),
                        style = Stroke(width = 2f),
                    )
                }
            }
        }
        drawPlayerMarkers(playerPositions, cellPx, panOffset, size)
    }
}

private fun DrawScope.drawPlayerMarkers(
    positions: Map<String, PlayerPos3>,
    cellPx: Float,
    panOffset: Offset,
    canvasSize: Size,
) {
    positions.forEach { (_, pos) ->
        val x = (pos.x.toFloat() / 16f) * cellPx + panOffset.x + canvasSize.width / 2f
        val z = (pos.z.toFloat() / 16f) * cellPx + panOffset.y + canvasSize.height / 2f
        if (x < -8f || x > canvasSize.width + 8f) return@forEach
        if (z < -8f || z > canvasSize.height + 8f) return@forEach
        drawCircle(color = Color(0xFF00E5FF), radius = 5f, center = Offset(x, z))
    }
}

private fun hitTest(
    regions: List<RegionIndex>,
    tapOffset: Offset,
    zoom: Float,
    panOffset: Offset,
    canvasSize: Size,
): ChunkCoord? {
    val cellPx = (6f * zoom).coerceAtLeast(1f)
    val cx = (tapOffset.x - panOffset.x - canvasSize.width / 2f) / cellPx
    val cz = (tapOffset.y - panOffset.y - canvasSize.height / 2f) / cellPx
    val regionX = cx.toInt() / ChunkCoord.REGION_SIZE
    val regionZ = cz.toInt() / ChunkCoord.REGION_SIZE
    val region = regions.firstOrNull { it.regionX == regionX && it.regionZ == regionZ } ?: return null
    val localX = ((cx.toInt() % ChunkCoord.REGION_SIZE) + ChunkCoord.REGION_SIZE) % ChunkCoord.REGION_SIZE
    val localZ = ((cz.toInt() % ChunkCoord.REGION_SIZE) + ChunkCoord.REGION_SIZE) % ChunkCoord.REGION_SIZE
    val coord = ChunkCoord(
        x = region.regionX * ChunkCoord.REGION_SIZE + localX,
        z = region.regionZ * ChunkCoord.REGION_SIZE + localZ,
    )
    return region.chunks[coord]?.takeIf { it.generated }?.let { coord }
}
