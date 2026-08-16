package com.portalhost.desktop.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.portalhost.desktop.world.AnvilChunkDecoder
import com.portalhost.player.EntityPositionService
import com.portalhost.player.PlayerPos3
import com.portalhost.server.ServerManager
import com.portalhost.world.BiomeChunkDecoder
import com.portalhost.world.BiomePalette
import com.portalhost.world.ChunkCoord
import com.portalhost.world.HeightmapChunkDecoder
import com.portalhost.world.RegionFileIndex
import com.portalhost.world.RegionIndex
import com.portalhost.world.UNRESOLVED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.*

private val ZOOM_LEVELS = listOf(1.5f, 2f, 3f, 4f, 6f, 8f, 12f, 16f, 24f, 32f)
private const val DEFAULT_ZOOM_INDEX = 3
private const val POLL_INTERVAL_MS = 3000L
private const val BASE_CELL_PX = 4f
private const val MIN_WORLD_HALF = 64

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
    val biomePalette = remember { BiomePalette }
    val biomeTiles = remember { mutableStateMapOf<Long, ImageBitmap>() }
    val terrainTiles = remember { mutableStateMapOf<Long, ImageBitmap>() }
    var showPlayersLayer by remember { mutableStateOf(true) }
    var showSpawnLayer by remember { mutableStateOf(false) }
    var showHomeLayer by remember { mutableStateOf(false) }
    var showWarpLayer by remember { mutableStateOf(false) }
    var showPortalLayer by remember { mutableStateOf(false) }
    var showNetherPortalLayer by remember { mutableStateOf(false) }
    var showPvpLayer by remember { mutableStateOf(false) }
    var showClaimsLayer by remember { mutableStateOf(false) }
    var showStructuresLayer by remember { mutableStateOf(false) }
    var showSlimeChunksLayer by remember { mutableStateOf(false) }
var showBiomesLayer by remember { mutableStateOf(false) }
    var showTerrainLayer by remember { mutableStateOf(false) }
    var hoveredBlockCoord by remember { mutableStateOf<String?>(null) }
    var worldLastScan by remember { mutableStateOf<String?>(null) }

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
            panOffset = Offset.Zero
            selectedChunk = null
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

    LaunchedEffect(selectedWorld, regionIndices) {
        terrainTiles.clear()
        val world = selectedWorld ?: return@LaunchedEffect
        val decoder = HeightmapChunkDecoder()
        val tileData = withContext(Dispatchers.IO) {
            val regionDir = File(world, "region")
            buildList {
                regionIndices.forEach { region ->
                    val file = File(regionDir, "r.${region.regionX}.${region.regionZ}.mca")
                    decoder.decodeRegion(file, region).forEach { (coord, colors) ->
                        add(packCoord(coord.x, coord.z) to colors)
                    }
                }
            }
        }
        tileData.forEach { (key, colors) ->
            buildTile(colors)?.let { terrainTiles[key] = it }
        }
    }

    LaunchedEffect(selectedWorld, regionIndices) {
        biomeTiles.clear()
        val world = selectedWorld ?: return@LaunchedEffect
        val decoder = BiomeChunkDecoder()
        val tileData = withContext(Dispatchers.IO) {
            val regionDir = File(world, "region")
            buildList {
                regionIndices.forEach { region ->
                    val file = File(regionDir, "r.${region.regionX}.${region.regionZ}.mca")
                    decoder.decodeRegion(file, region).forEach { (coord, colors) ->
                        add(packCoord(coord.x, coord.z) to colors)
                    }
                }
            }
        }
        tileData.forEach { (key, colors) ->
            buildBiomeTile(colors)?.let { biomeTiles[key] = it }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds(),
    ) {
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
                onCenter = { panOffset = Offset.Zero; selectedChunk = null },
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
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading region files...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                regionIndices.isEmpty() -> {
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
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clipToBounds(),
                    ) {
                        MapCanvas(
                            regions = regionIndices,
                            zoom = ZOOM_LEVELS[zoomIndex],
                            panOffset = panOffset,
                            onPanChange = { panOffset += it },
                            onCanvasSize = { canvasSize = it },
                            onChunkTapped = { selectedChunk = it },
                            selectedChunk = selectedChunk,
                            playerPositions = playerPositions,
                            onZoomChange = { newZoom -> zoomIndex = newZoom },
                            terrainTiles = terrainTiles,
                            biomeTiles = biomeTiles,
                            showBiomesLayer = showBiomesLayer,
                            showTerrainLayer = showTerrainLayer,
                            showPlayersLayer = showPlayersLayer,
                            showSpawnLayer = showSpawnLayer,
                            showHomeLayer = showHomeLayer,
                            showWarpLayer = showWarpLayer,
                            showPortalLayer = showPortalLayer,
                            showNetherPortalLayer = showNetherPortalLayer,
                            showPvpLayer = showPvpLayer,
                            showClaimsLayer = showClaimsLayer,
                            showStructuresLayer = showStructuresLayer,
                            showSlimeChunksLayer = showSlimeChunksLayer,
                            onHoverBlock = { bx, bz -> hoveredBlockCoord = "$bx, $bz" },
                            onCenterPlayer = { name ->
                                val pos = playerPositions[name] ?: return@MapCanvas
                                panOffset = Offset(
                                    x = -(pos.x.toFloat() / 16f) * ZOOM_LEVELS[zoomIndex] * BASE_CELL_PX,
                                    y = (pos.z.toFloat() / 16f) * ZOOM_LEVELS[zoomIndex] * BASE_CELL_PX,
                                )
                            },
                        )
                        MapLayersPanel(
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 60.dp, end = 8.dp),
                            showPlayersLayer = showPlayersLayer,
                            onTogglePlayers = { showPlayersLayer = !showPlayersLayer },
                            showSpawnLayer = showSpawnLayer,
                            onToggleSpawn = { showSpawnLayer = !showSpawnLayer },
                            showHomeLayer = showHomeLayer,
                            onToggleHome = { showHomeLayer = !showHomeLayer },
                            showWarpLayer = showWarpLayer,
                            onToggleWarp = { showWarpLayer = !showWarpLayer },
                            showPortalLayer = showPortalLayer,
                            onTogglePortal = { showPortalLayer = !showPortalLayer },
                            showNetherPortalLayer = showNetherPortalLayer,
                            onToggleNetherPortal = { showNetherPortalLayer = !showNetherPortalLayer },
                            showPvpLayer = showPvpLayer,
                            onTogglePvp = { showPvpLayer = !showPvpLayer },
                            showClaimsLayer = showClaimsLayer,
                            onToggleClaims = { showClaimsLayer = !showClaimsLayer },
                            showStructuresLayer = showStructuresLayer,
                            onToggleStructures = { showStructuresLayer = !showStructuresLayer },
                            showSlimeChunksLayer = showSlimeChunksLayer,
                            onToggleSlimeChunks = { showSlimeChunksLayer = !showSlimeChunksLayer },
                            showBiomesLayer = showBiomesLayer,
                            onToggleBiomes = { showBiomesLayer = !showBiomesLayer },
                            showTerrainLayer = showTerrainLayer,
                            onToggleTerrain = { showTerrainLayer = !showTerrainLayer },
                        )
                        selectedChunk?.let { chunk ->
                            Surface(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 4.dp,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Text(
                                    "Chunk (${chunk.x}, ${chunk.z})  ·  Region (${chunk.regionX}, ${chunk.regionZ})",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        WorldInfoFooter(
                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 8.dp),
                            hoveredBlockCoord = hoveredBlockCoord,
                            worldLastScan = worldLastScan,
                            generationPercent = if (regionIndices.isEmpty()) 0.0 else regionIndices.size / 576.0 * 100,
                            onRescan = {
                                selectedChunk = null
                                val world = selectedWorld
                                if (world != null) {
                                    loading = true
                                    try {
                                        val regions = RegionFileIndex().indexDirectory(File(world, "region"))
                                        regionIndices = regions
                                        worldLastScan = java.text.SimpleDateFormat("HH:mm").format(java.util.Date())
                                    } catch (e: Exception) {
                                        lastError = "Failed to rescan: ${e.message}"
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                        )
                        MapLegend(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            playerCount = playerPositions.size,
                        )
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
            Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
        }
        Text(
            formatZoomLabel(ZOOM_LEVELS[zoomIndex]),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(onClick = onZoomIn, enabled = zoomIndex < ZOOM_LEVELS.lastIndex) {
            Icon(Icons.Filled.Add, contentDescription = "Zoom in")
        }
        IconButton(onClick = onCenter) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Center on origin")
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("$chunkCount chunks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatZoomLabel(zoom: Float): String {
    val rounded = (zoom * 10).toInt() / 10f
    return if (rounded == rounded.toInt().toFloat()) "${rounded.toInt()}x" else "${rounded}x"
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier, playerCount: Int) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            LegendRow(color = Color(0xFF4A8FCC), label = "Generated chunk")
            Spacer(Modifier.height(2.dp))
            LegendRow(color = Color(0xFFCCCCCC), label = "Ungenerated")
            Spacer(Modifier.height(2.dp))
            LegendRow(color = Color(0xFF00E5FF), label = "Player ($playerCount)")
            Spacer(Modifier.height(2.dp))
            LegendRow(color = Color(0xFFFFCC00), label = "Spawn (0,0)")
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawRect(color = color)
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private data class WorldBounds(
    val minChunkX: Int,
    val maxChunkX: Int,
    val minChunkZ: Int,
    val maxChunkZ: Int,
)

private fun computeBounds(regions: List<RegionIndex>): WorldBounds {
    if (regions.isEmpty()) {
        return WorldBounds(-MIN_WORLD_HALF, MIN_WORLD_HALF, -MIN_WORLD_HALF, MIN_WORLD_HALF)
    }
    var minX = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE
    var minZ = Int.MAX_VALUE
    var maxZ = Int.MIN_VALUE
    regions.forEach { region ->
        region.chunks.forEach { (coord, presence) ->
            if (!presence.generated) return@forEach
            if (coord.x < minX) minX = coord.x
            if (coord.x > maxX) maxX = coord.x
            if (coord.z < minZ) minZ = coord.z
            if (coord.z > maxZ) maxZ = coord.z
        }
    }
    if (minX > maxX) {
        return WorldBounds(-MIN_WORLD_HALF, MIN_WORLD_HALF, -MIN_WORLD_HALF, MIN_WORLD_HALF)
    }
    val padX = max(8, (maxX - minX) / 16)
    val padZ = max(8, (maxZ - minZ) / 16)
    val minChunkX = min(minX - padX, -MIN_WORLD_HALF)
    val maxChunkX = max(maxX + padX, MIN_WORLD_HALF)
    val minChunkZ = min(minZ - padZ, -MIN_WORLD_HALF)
    val maxChunkZ = max(maxZ + padZ, MIN_WORLD_HALF)
    return WorldBounds(minChunkX, maxChunkX, minChunkZ, maxChunkZ)
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
    onZoomChange: (Int) -> Unit,
biomeTiles: Map<Long, ImageBitmap> = emptyMap(),
    terrainTiles: Map<Long, ImageBitmap> = emptyMap(),
    showBiomesLayer: Boolean = false,
    showTerrainLayer: Boolean = false,
    showPlayersLayer: Boolean = true,
    showSpawnLayer: Boolean = false,
    showHomeLayer: Boolean = false,
    showWarpLayer: Boolean = false,
    showPortalLayer: Boolean = false,
    showNetherPortalLayer: Boolean = false,
    showPvpLayer: Boolean = false,
    showClaimsLayer: Boolean = false,
    showStructuresLayer: Boolean = false,
    showSlimeChunksLayer: Boolean = false,
    onCenterPlayer: (String) -> Unit,
    onHoverBlock: ((Int, Int) -> Unit)? = null,
) {
    val bounds = remember(regions) { computeBounds(regions) }
    val generated = remember(regions) {
        val set = mutableSetOf<Long>()
        regions.forEach { region ->
            region.chunks.forEach { (coord, presence) ->
                if (presence.generated) {
                    set.add(packCoord(coord.x, coord.z))
                }
            }
        }
        set
    }
    var playerMarkers by remember { mutableStateOf<List<PlayerScreenPos>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(regions, zoom) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        if (zoomChange != 1f) {
                            // Trackpad pinch zoom
                            val current = ZOOM_LEVELS.indexOfFirst { it == zoom }.coerceAtLeast(0)
                            val target = if (zoomChange > 1f) current + 1 else current - 1
                            onZoomChange(target.coerceIn(0, ZOOM_LEVELS.lastIndex))
                            return@detectTransformGestures
                        }
                        onPanChange(pan)
                    }
                }
                .pointerInput(regions, zoom) {
                    // Mouse-wheel zoom: scroll up = zoom in, scroll down = zoom out.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll) continue
                            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (delta == 0f) continue
                            val current = ZOOM_LEVELS.indexOfFirst { it == zoom }.coerceAtLeast(0)
                            val target = if (delta < 0f) current + 1 else current - 1
                            onZoomChange(target.coerceIn(0, ZOOM_LEVELS.lastIndex))
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .pointerInput(regions, zoom) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position ?: continue
                            val coord = hitTest(pos, zoom, panOffset, bounds, size.toSize())
                            onHoverBlock?.invoke(coord?.x ?: 0, coord?.z ?: 0)
                        }
                    }
                }
                .pointerInput(regions, zoom) {
                    detectTapGestures(
                        onTap = { offset ->
                            val coord = hitTest(offset, zoom, panOffset, bounds, size.toSize())
                            onChunkTapped(coord)
                        },
                        onLongPress = { offset ->
                            val name = hitTestPlayer(offset, zoom, panOffset, playerPositions, size.toSize())
                            if (name != null) onCenterPlayer(name)
                        },
                    )
                }
        ) {
            onCanvasSize(size)
            drawRect(color = Color(0xFF0E141B), size = size)

            val cellPx = BASE_CELL_PX * zoom
            if (cellPx < 0.5f) return@Canvas

            // World (0,0) sits at the center of the canvas at default pan.
            // Each chunkX step moves +cellPx on screen. North (-Z in Minecraft)
            // maps to screen up (negative Y).
            val worldOriginX = size.width / 2f + panOffset.x
            val worldOriginY = size.height / 2f + panOffset.y

            // Ungenerated grid background so empty areas are not pure black.
            val ungeneratedColor = Color(0xFF1B2530)
            val visibleMinChunkX = bounds.minChunkX + ((-worldOriginX) / cellPx).toInt() - 1
            val visibleMaxChunkX = bounds.maxChunkX + ((size.width - worldOriginX) / cellPx).toInt() + 1
            val visibleMinChunkZ = bounds.minChunkZ + ((-worldOriginY) / cellPx).toInt() - 1
            val visibleMaxChunkZ = bounds.maxChunkZ + ((size.height - worldOriginY) / cellPx).toInt() + 1

            val startChunkX = max(bounds.minChunkX, visibleMinChunkX)
            val endChunkX = min(bounds.maxChunkX, visibleMaxChunkX)
            val startChunkZ = max(bounds.minChunkZ, visibleMinChunkZ)
            val endChunkZ = min(bounds.maxChunkZ, visibleMaxChunkZ)

            if (cellPx >= 4f) {
                for (cz in startChunkZ..endChunkZ) {
                    for (cx in startChunkX..endChunkX) {
                        if (!generated.contains(packCoord(cx, cz))) {
                            val sx = worldOriginX + cx * cellPx
                            val sy = worldOriginY + cz * cellPx
                            drawRect(
                                color = ungeneratedColor,
                                topLeft = Offset(sx, sy),
                                size = Size(cellPx - 1f, cellPx - 1f),
                            )
                        }
                    }
                }
            }

            // Generated chunks
            for (cz in startChunkZ..endChunkZ) {
                for (cx in startChunkX..endChunkX) {
                    if (!generated.contains(packCoord(cx, cz))) continue
                    val sx = worldOriginX + cx * cellPx
                    val sy = worldOriginY + cz * cellPx
                    val biomeTile = biomeTiles[packCoord(cx, cz)]
                    val terrainTile = terrainTiles[packCoord(cx, cz)]
                    if (showTerrainLayer && terrainTile != null && cellPx >= 1f) {
                        drawImage(
                            image = terrainTile,
                            dstOffset = IntOffset(sx.roundToInt(), sy.roundToInt()),
                            dstSize = IntSize(cellPx.roundToInt(), cellPx.roundToInt()),
                            filterQuality = FilterQuality.None,
                        )
                    } else if (showBiomesLayer && biomeTile != null && cellPx >= 1f) {
                        drawImage(
                            image = biomeTile,
                            dstOffset = IntOffset(sx.roundToInt(), sy.roundToInt()),
                            dstSize = IntSize(cellPx.roundToInt(), cellPx.roundToInt()),
                            filterQuality = FilterQuality.None,
                        )
                    } else if (showBiomesLayer || showTerrainLayer) {
                        // No tile for this chunk yet (fallback: map background).
                        // Biome tiles are always pre-built on world load, so this
                        // branch is only reached transiently before tiles are ready.
                    }
                    if (selectedChunk != null && selectedChunk.x == cx && selectedChunk.z == cz) {
                        drawRect(
                            color = Color(0xFFFFCC00),
                            topLeft = Offset(sx, sy),
                            size = Size(cellPx - 1f, cellPx - 1f),
                            style = Stroke(width = 2f),
                        )
                    }
                }
            }

            // Spawn origin marker.
            if (generated.contains(packCoord(0, 0)) || cellPx > 8f) {
                drawSpawnLayer(
                    sx = worldOriginX, sy = worldOriginY,
                    cellPx = cellPx, showSpawnLayer = showSpawnLayer,
                )
            }

            // Axis tick marks every 16 chunks at higher zoom levels.
            if (cellPx >= 8f) {
                drawAxisTicks(worldOriginX, worldOriginY, cellPx)
            }

            if (showPlayersLayer) {
            playerMarkers = drawPlayerMarkers(playerPositions, cellPx, worldOriginX, worldOriginY, size)
        }
        }

        // Player name labels (Compose Text overlay for cross-platform font rendering).
        if (ZOOM_LEVELS.indexOf(zoom) >= 2) {
            playerMarkers.forEach { marker ->
                Text(
                    text = marker.name,
                    color = Color(0xFFFFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .offset { IntOffset((marker.x + 12f).roundToInt(), (marker.z - 18f).roundToInt()) }
                        .background(
                            color = Color(0xCC000000),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
    }
}

private fun packCoord(x: Int, z: Int): Long =
    (x.toLong() and 0xFFFFFFFFL) shl 32 or (z.toLong() and 0xFFFFFFFFL)

/** Builds a 16x16 ARGB tile from a chunk's column colors (1 pixel per block). */
private fun buildTile(colors: LongArray): ImageBitmap? {
    var hasContent = false
    colors.forEach { if (it != UNRESOLVED) hasContent = true }
    if (!hasContent) return null
    val image = ImageBitmap(16, 16)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    val paint = androidx.compose.ui.graphics.Paint().apply {
        style = androidx.compose.ui.graphics.PaintingStyle.Fill
    }
    for (i in colors.indices) {
        val c = colors[i]
        if (c == UNRESOLVED) continue
        val x = (i and 15).toFloat()
        val z = (i shr 4).toFloat()
        paint.color = Color(c.toULong())
        canvas.drawRect(left = x, top = z, right = x + 1f, bottom = z + 1f, paint = paint)
    }
    return image
}

private fun buildBiomeTile(colors: LongArray): ImageBitmap? {
    var hasContent = false
    for (c in colors) { if (c != UNRESOLVED) { hasContent = true; break } }
    if (!hasContent) return null
    val image = ImageBitmap(16, 16)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    val paint = androidx.compose.ui.graphics.Paint().apply { style = androidx.compose.ui.graphics.PaintingStyle.Fill }
    for (i in colors.indices) {
        val c = colors[i]
        if (c == UNRESOLVED) continue
        val x = (i and 15).toFloat()
        val z = (i shr 4).toFloat()
        paint.color = Color(c.toULong())
        canvas.drawRect(left = x, top = z, right = x + 1f, bottom = z + 1f, paint = paint)
    }
    return image
}

private fun DrawScope.drawPlayerMarkers(
    positions: Map<String, PlayerPos3>,
    cellPx: Float,
    worldOriginX: Float,
    worldOriginY: Float,
    canvasSize: Size,
): List<PlayerScreenPos> {
    val result = mutableListOf<PlayerScreenPos>()
    positions.forEach { (name, pos) ->
        val blockToPx = cellPx / 16f
        val x = worldOriginX + pos.x.toFloat() * blockToPx
        val z = worldOriginY + pos.z.toFloat() * blockToPx
        if (x < -16f || x > canvasSize.width + 16f) return@forEach
        if (z < -16f || z > canvasSize.height + 16f) return@forEach

        // Outer halo (semi-transparent white) for visibility on any background.
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = 0.30f),
            radius = 14f,
            center = Offset(x, z),
        )
        // White outer ring.
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = 9f,
            center = Offset(x, z),
            style = Stroke(width = 2.5f),
        )
        // Solid magenta core (high contrast on plains/desert/forest/snow).
        drawCircle(
            color = Color(0xFFFF1FB0),
            radius = 6f,
            center = Offset(x, z),
        )
        // Direction-of-facing tick (4 cardinal indicators).
        drawLine(
            color = Color(0xFFFFFFFF),
            start = Offset(x - 10f, z),
            end = Offset(x - 6f, z),
            strokeWidth = 1.5f,
        )
        drawLine(
            color = Color(0xFFFFFFFF),
            start = Offset(x + 6f, z),
            end = Offset(x + 10f, z),
            strokeWidth = 1.5f,
        )
        drawLine(
            color = Color(0xFFFFFFFF),
            start = Offset(x, z - 10f),
            end = Offset(x, z - 6f),
            strokeWidth = 1.5f,
        )
        drawLine(
            color = Color(0xFFFFFFFF),
            start = Offset(x, z + 6f),
            end = Offset(x, z + 10f),
            strokeWidth = 1.5f,
        )
        result.add(PlayerScreenPos(name = name, x = x, z = z))
    }
    return result
}

private fun DrawScope.drawHomeLayer(
    homePositions: Map<String, PlayerPos3>,
    cellPx: Float,
    worldOriginX: Float,
    worldOriginY: Float,
) {
    homePositions.forEach { (name, pos) ->
        val blockToPx = cellPx / 16f
        val x = worldOriginX + pos.x.toFloat() * blockToPx
        val z = worldOriginY + pos.z.toFloat() * blockToPx
        val iconSize = min(cellPx, 20f)
        // Diamond home icon: a rotated square.
        val half = iconSize / 2f
        val diamond = Path().apply {
            moveTo(x, z - half)
            lineTo(x + half, z)
            lineTo(x, z + half)
            lineTo(x - half, z)
            close()
}
        drawPath(path = diamond, color = Color(0xFF4488FF))
        if (cellPx >= 6f) {
            drawPath(
                path = diamond,
                color = Color(0xFFFFFFFF),
                style = Stroke(width = 1.2f),
            )
            val labelY = z + half + 3f
            val labelH = 13f
            drawRect(
                color = Color(0xCC000000),
                topLeft = Offset(x - iconSize * 0.55f, labelY),
                size = Size(iconSize * 1.1f, labelH),
            )
        }
    }
}

private fun DrawScope.drawWarpLayer(
    warpPositions: Map<String, PlayerPos3>,
    cellPx: Float,
    worldOriginX: Float,
    worldOriginY: Float,
    canvasSize: Size,
) {
warpPositions.forEach { (name, pos) ->
        val blockToPx = cellPx / 16f
        val x = worldOriginX + pos.x.toFloat() * blockToPx
        val z = worldOriginY + pos.z.toFloat() * blockToPx
        if (x < -24f || x > canvasSize.width + 24f || z < -24f || z > canvasSize.height + 24f) return@forEach
        val r = if (cellPx >= 10f) 9f else 6f
        drawCircle(color = Color(0xAAC86000), radius = r + 3f, center = Offset(x, z))
        drawCircle(color = Color(0xFFE8B830), radius = r, center = Offset(x, z))
        drawCircle(color = Color(0xFFFFFFFF), radius = r * 0.45f, center = Offset(x, z))
        if (cellPx >= 14f) {
            val labelW = 44f
            val labelH = 13f
            drawRect(
                color = Color(0xCC000000),
                topLeft = Offset(x - labelW / 2f, z - r - labelH - 2f),
                size = Size(labelW, labelH),
            )
        }
    }
}

private fun DrawScope.drawPortalLayer(
    portalPositions: Map<String, PlayerPos3>,
    cellPx: Float,
    worldOriginX: Float,
    worldOriginY: Float,
    canvasSize: Size,
) {
    portalPositions.forEach { (_, pos) ->
        val blockToPx = cellPx / 16f
        val x = worldOriginX + pos.x.toFloat() * blockToPx
        val z = worldOriginY + pos.z.toFloat() * blockToPx
        if (x < -20f || x > canvasSize.width + 20f || z < -20f || z > canvasSize.height + 20f) return@forEach
        val r = if (cellPx >= 10f) 9f else 6f
        drawCircle(color = Color(0x669955DD), radius = r + 4f, center = Offset(x, z))
        drawCircle(color = Color(0xFF7B3DFF), radius = r, center = Offset(x, z))
        drawCircle(color = Color(0xFFCC88FF), radius = r * 0.38f, center = Offset(x, z))
        if (cellPx >= 12f) {
            drawLine(Color(0xFFFFFFFF).copy(alpha = 0.55f), Offset(x - r * 0.7f, z - r * 0.7f), Offset(x, z), 1.5f)
            drawLine(Color(0xFFFFFFFF).copy(alpha = 0.55f), Offset(x + r * 0.7f, z - r * 0.7f), Offset(x, z), 1.5f)
        }
    }
}

private fun DrawScope.drawSpawnLayer(
    sx: Float, sy: Float, cellPx: Float, showSpawnLayer: Boolean,
) {
    if (!showSpawnLayer) return
    val baseSize = 16f * cellPx / 16f
    drawCircle(color = Color(0x26FFD740), radius = baseSize * 1.15f, center = Offset(sx, sy))
    drawCircle(
        color = Color(0xFFFFD740),
        radius = baseSize * 0.72f,
        center = Offset(sx, sy),
        style = Stroke(width = 2.8f),
    )
    drawCircle(color = Color(0xFFFFF176), radius = baseSize * 0.3f, center = Offset(sx, sy))
    val half = baseSize
    drawLine(Color(0xFFFFD740), Offset(sx - half, sy), Offset(sx - half * 0.3f, sy), 2f)
    drawLine(Color(0xFFFFD740), Offset(sx + half * 0.3f, sy), Offset(sx + half, sy), 2f)
    drawLine(Color(0xFFFFD710), Offset(sx, sy - half), Offset(sx, sy - half * 0.3f), 2f)
    drawLine(Color(0xFFFFD710), Offset(sx, sy + half * 0.3f), Offset(sx, sy + half), 2f)
}

private fun DrawScope.drawPvpLayer(
    cellPx: Float, worldOriginX: Float, worldOriginY: Float,
    canvasSize: Size,
) {
    val r = if (cellPx >= 10f) 10f else 6f
    val t = (System.currentTimeMillis() % 2000) / 2000f
    val a = 0.45f * (if (t < 0.5f) (t * 2f) else (1f - (t - 0.5f) * 2f))
    for (iy in 0 until 3) {
        for (ix in 0 until 3) {
            val px = if (ix == 1) canvasSize.width / 2f else (if (ix == 0) 0f else canvasSize.width)
            val pz = if (iy == 1) canvasSize.height / 2f else (if (iy == 0) 0f else canvasSize.height)
            val gx = px - worldOriginX + (px - worldOriginX).coerceIn(-canvasSize.width, canvasSize.width)
            val gy = pz - worldOriginY + (pz - worldOriginY).coerceIn(-canvasSize.height, canvasSize.height)
            drawCircle(color = Color(0x55FF1744).copy(alpha = a), radius = r + 5f, center = Offset(gx, gy))
        }
    }
    drawCircle(color = Color(0xAAFF1744), radius = r + 2f, center = Offset(canvasSize.width / 2f, canvasSize.height / 2f))
    drawCircle(color = Color(0xFFFF5252), radius = r, center = Offset(canvasSize.width / 2f, canvasSize.height / 2f))
    drawCircle(color = Color(0xFFFFFFFF), radius = r * 0.3f, center = Offset(canvasSize.width / 2f, canvasSize.height / 2f))
    if (cellPx >= 10f) {
        drawLine(Color(0xFFFF8A80), Offset(canvasSize.width / 2f - r, canvasSize.height / 2f), Offset(canvasSize.width / 2f + r, canvasSize.height / 2f), 2.5f)
        drawLine(Color(0xFFFF8A80), Offset(canvasSize.width / 2f, canvasSize.height / 2f - r), Offset(canvasSize.width / 2f, canvasSize.height / 2f + r), 2.5f)
    }
}

private fun DrawScope.drawClaimsLayer(
    cellPx: Float, worldOriginX: Float, worldOriginY: Float,
    canvasSize: Size,
) {
    val cw = canvasSize.width / 2f
    val ch = canvasSize.height / 2f
    val gap = 6f
    val lx = (cw - gap / 2f).coerceAtLeast(gap)
    val ly = (ch - gap / 2f).coerceAtLeast(gap)
    drawLine(Color(0x88FFFFFF), Offset(cw - lx, ch), Offset(cw + lx, ch), 3f)
    drawLine(Color(0x88FFFFFF), Offset(cw, ch - ly), Offset(cw, ch + ly), 3f)
    drawLine(Color(0x55FFFFFF), Offset(cw - lx, ch - ly * 0.66f), Offset(cw + lx, ch - ly * 0.66f), 2f)
    drawLine(Color(0x55FFFFFF), Offset(cw - lx, ch + ly * 0.66f), Offset(cw + lx, ch + ly * 0.66f), 2f)
    drawLine(Color(0x55FFFFFF), Offset(cw - lx * 0.66f, ch - ly), Offset(cw - lx * 0.66f, ch + ly), 2f)
    drawLine(Color(0x55FFFFFF), Offset(cw + lx * 0.66f, ch - ly), Offset(cw + lx * 0.66f, ch + ly), 2f)
}

private fun DrawScope.drawStructuresLayer(
    cellPx: Float, worldOriginX: Float, worldOriginY: Float,
    canvasSize: Size,
) {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val s = min(cellPx, 32f)
    val hw = s * 0.4f
    val hh = s * 0.5f
    val x0 = cx - hw
    val y0 = cy - hh
    drawRect(color = Color(0x18FFC107), topLeft = Offset(x0 - 6f, y0 - 6f), size = Size(s + 12f, s + 12f))
    drawRect(color = Color(0xFFFFC107), topLeft = Offset(x0, y0), size = Size(s, s * 0.85f), style = Stroke(width = 2f))
    drawLine(Color(0xFFFFC107), Offset(cx, y0 - 4f), Offset(cx, y0 + hh), 2f)
    drawLine(Color(0xFFFFC107), Offset(x0 - 4f, y0 + hh * 0.45f), Offset(x0 + s + 4f, y0 + hh * 0.45f), 2f)
    drawLine(Color(0xFFFFC107), Offset(x0 - 4f, y0 + hh * 0.7f), Offset(x0 + s + 4f, y0 + hh * 0.7f), 1.5f)
}

private fun DrawScope.drawSlimeChunksLayer(
    slime: Boolean, cellPx: Float, worldOriginX: Float, worldOriginY: Float,
    canvasSize: Size,
) {
    if (!slime) return
    val x = ((worldOriginX % (cellPx * 16)) - cellPx * 16).coerceIn(0f, canvasSize.width)
    val z = ((worldOriginY % (cellPx * 16)) - cellPx * 16).coerceIn(0f, canvasSize.height)
    for (ix in 0 until (canvasSize.width / (cellPx * 16) + 1).toInt()) {
        for (iz in 0 until (canvasSize.height / (cellPx * 16) + 1).toInt()) {
            drawRect(
                color = Color(0x3332CD32),
                topLeft = Offset(x + ix * cellPx * 16, z + iz * cellPx * 16),
                size = Size(cellPx * 16, cellPx * 16),
            )
            drawRect(
                color = Color(0x6632CD32),
                topLeft = Offset(x + ix * cellPx * 16 + cellPx * 4, z + iz * cellPx * 16 + cellPx * 4),
                size = Size(cellPx * 4, cellPx * 4),
            )
        }
    }
}

@Composable
private fun WorldInfoFooter(
    modifier: Modifier = Modifier,
    hoveredBlockCoord: String?,
    worldLastScan: String?,
    generationPercent: Double,
    onRescan: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = Color(0xFF0E141B),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = hoveredBlockCoord ?: "Hover to find coords",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                Text(
                    text = "Generated: ${"%.1f".format(generationPercent)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                Text(
                    text = worldLastScan ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            TextButton(onClick = onRescan) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Rescan", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MapLayersPanel(
    modifier: Modifier = Modifier,
    showPlayersLayer: Boolean, onTogglePlayers: () -> Unit,
    showSpawnLayer: Boolean, onToggleSpawn: () -> Unit,
    showHomeLayer: Boolean, onToggleHome: () -> Unit,
    showWarpLayer: Boolean, onToggleWarp: () -> Unit,
    showPortalLayer: Boolean, onTogglePortal: () -> Unit,
    showNetherPortalLayer: Boolean, onToggleNetherPortal: () -> Unit,
    showPvpLayer: Boolean, onTogglePvp: () -> Unit,
    showClaimsLayer: Boolean, onToggleClaims: () -> Unit,
    showStructuresLayer: Boolean, onToggleStructures: () -> Unit,
    showSlimeChunksLayer: Boolean, onToggleSlimeChunks: () -> Unit,
    showBiomesLayer: Boolean, onToggleBiomes: () -> Unit,
    showTerrainLayer: Boolean, onToggleTerrain: () -> Unit,
) {
    Surface(
        modifier = modifier.width(168.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        color = Color(0xE6091C2B),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("Map Layers", style = MaterialTheme.typography.labelMedium, color = Color(0xFF90CAF9), fontWeight = FontWeight.SemiBold)

LayerRow("Players", Icons.Filled.Person, showPlayersLayer, onTogglePlayers)
            LayerRow("Spawn", Icons.Filled.Flag, showSpawnLayer, onToggleSpawn)
            LayerRow("Home", Icons.Filled.Home, showHomeLayer, onToggleHome)
            LayerRow("Warp Points", Icons.Filled.PinDrop, showWarpLayer, onToggleWarp)
            LayerRow("Portal", Icons.Filled.SyncAlt, showPortalLayer, onTogglePortal)
            LayerRow("Nether Portal", Icons.Filled.SyncAlt, showNetherPortalLayer, onToggleNetherPortal)
            LayerRow("PvP Areas", Icons.Filled.SportsMma, showPvpLayer, onTogglePvp)
            LayerRow("Claims", Icons.Filled.Fence, showClaimsLayer, onToggleClaims)
            LayerRow("Structures", Icons.Filled.CropLandscape, showStructuresLayer, onToggleStructures)
            LayerRow("Slime Chunks", Icons.Filled.CropSquare, showSlimeChunksLayer, onToggleSlimeChunks)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0x28FFFFFF))
            LayerRow("Biome Colours", Icons.Filled.Terrain, showBiomesLayer, onToggleBiomes)
            LayerRow("Terrain Tiles", Icons.Filled.Terrain, showTerrainLayer, onToggleTerrain)
        }
    }
}

@Composable
private fun LayerRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 3.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(16.dp),
        )
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF90CAF9))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0BEC5), fontSize = 12.sp)
    }
}

data class PlayerScreenPos(val name: String, val x: Float, val z: Float)

private fun DrawScope.drawAxisTicks(worldOriginX: Float, worldOriginY: Float, cellPx: Float) {
    val tickColor = Color(0xFF2A3744)
    val tickEvery = if (cellPx >= 32f) 16 else 32
    val tickLen = if (cellPx >= 32f) 4f else 2f
    val xRange = (-2048..2048 step tickEvery).toList()
    val zRange = (-2048..2048 step tickEvery).toList()
    xRange.forEach { chunkX ->
        val px = worldOriginX + chunkX * cellPx
        if (px in 0f..size.width) {
            drawLine(
                color = tickColor,
                start = Offset(px, size.height - tickLen),
                end = Offset(px, size.height),
                strokeWidth = 1f,
            )
        }
    }
    zRange.forEach { chunkZ ->
        val pz = worldOriginY + chunkZ * cellPx
        if (pz in 0f..size.height) {
            drawLine(
                color = tickColor,
                start = Offset(0f, pz),
                end = Offset(tickLen, pz),
                strokeWidth = 1f,
            )
        }
    }
}

private fun hitTest(
    tapOffset: Offset,
    zoom: Float,
    panOffset: Offset,
    bounds: WorldBounds,
    canvasSize: Size,
): ChunkCoord? {
    val cellPx = BASE_CELL_PX * zoom
    if (cellPx < 0.5f) return null
    val worldOriginX = canvasSize.width / 2f + panOffset.x
    val worldOriginY = canvasSize.height / 2f + panOffset.y
    val cx = ((tapOffset.x - worldOriginX) / cellPx).toInt()
    val cz = -((tapOffset.y - worldOriginY) / cellPx).toInt()
    if (cx < bounds.minChunkX || cx > bounds.maxChunkX) return null
    if (cz < bounds.minChunkZ || cz > bounds.maxChunkZ) return null
    return ChunkCoord(cx, cz)
}

private fun hitTestPlayer(
    tapOffset: Offset,
    zoom: Float,
    panOffset: Offset,
    positions: Map<String, PlayerPos3>,
    canvasSize: Size,
): String? {
    val cellPx = BASE_CELL_PX * zoom
    val blockToPx = cellPx / 16f
    val worldOriginX = canvasSize.width / 2f + panOffset.x
    val worldOriginY = canvasSize.height / 2f + panOffset.y
    val hitRadiusSq = 100f
    var bestName: String? = null
    var bestDistSq = Float.MAX_VALUE
    positions.forEach { (name, pos) ->
        val x = worldOriginX + pos.x.toFloat() * blockToPx
        val z = worldOriginY + pos.z.toFloat() * blockToPx
        val dx = x - tapOffset.x
        val dz = z - tapOffset.y
        val distSq = dx * dx + dz * dz
        if (distSq < hitRadiusSq && distSq < bestDistSq) {
            bestDistSq = distSq
            bestName = name
        }
    }
    return bestName
}


