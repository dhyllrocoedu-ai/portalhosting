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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.toSize
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
import kotlin.math.max
import kotlin.math.min

private val ZOOM_LEVELS = listOf(2f, 4f, 8f, 16f, 32f)
private const val DEFAULT_ZOOM_INDEX = 1
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
                            onCenterPlayer = { name ->
                                val pos = playerPositions[name] ?: return@MapCanvas
                                panOffset = Offset(
                                    x = -(pos.x.toFloat() / 16f) * ZOOM_LEVELS[zoomIndex] * BASE_CELL_PX,
                                    y = (pos.z.toFloat() / 16f) * ZOOM_LEVELS[zoomIndex] * BASE_CELL_PX,
                                )
                            },
                        )
                        selectedChunk?.let { chunk ->
                            Surface(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 4.dp,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Text(
                                    "Selected: chunk (${chunk.x}, ${chunk.z})  ·  block (${chunk.x * 16}, ${chunk.z * 16})  ·  region (${chunk.regionX}, ${chunk.regionZ})",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
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
            Icon(Icons.Default.Remove, contentDescription = "Zoom out")
        }
        Text("${ZOOM_LEVELS[zoomIndex].toInt()}x", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = onZoomIn, enabled = zoomIndex < ZOOM_LEVELS.lastIndex) {
            Icon(Icons.Default.Add, contentDescription = "Zoom in")
        }
        IconButton(onClick = onCenter) {
            Icon(Icons.Default.MyLocation, contentDescription = "Center on origin")
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("$chunkCount chunks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
        androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
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
    onCenterPlayer: (String) -> Unit,
) {
    val bounds = remember(regions) { computeBounds(regions) }
    val chunkSpanX = bounds.maxChunkX - bounds.minChunkX + 1
    val chunkSpanZ = bounds.maxChunkZ - bounds.minChunkZ + 1
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

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(regions) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    if (zoomChange != 1f) return@detectTransformGestures
                    onPanChange(pan)
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
                drawRect(
                    color = Color(0xFF4A8FCC).copy(alpha = 0.7f),
                    topLeft = Offset(sx, sy),
                    size = Size(cellPx - 1f, cellPx - 1f),
                )
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

        // Spawn marker at world origin (chunk 0,0)
        if (generated.contains(packCoord(0, 0)) || cellPx > 8f) {
            val spawnSize = max(cellPx, 6f)
            drawRect(
                color = Color(0xFFFFCC00),
                topLeft = Offset(worldOriginX - spawnSize / 2f, worldOriginY - spawnSize / 2f),
                size = Size(spawnSize, spawnSize),
                style = Stroke(width = 2.5f),
            )
            drawLine(
                color = Color(0xFFFFCC00),
                start = Offset(worldOriginX - spawnSize, worldOriginY),
                end = Offset(worldOriginX + spawnSize, worldOriginY),
                strokeWidth = 1.5f,
            )
            drawLine(
                color = Color(0xFFFFCC00),
                start = Offset(worldOriginX, worldOriginY - spawnSize),
                end = Offset(worldOriginX, worldOriginY + spawnSize),
                strokeWidth = 1.5f,
            )
        }

        // Axis tick marks every 16 chunks at higher zoom levels.
        if (cellPx >= 8f) {
            drawAxisTicks(worldOriginX, worldOriginY, cellPx)
        }

        drawPlayerMarkers(playerPositions, cellPx, worldOriginX, worldOriginY, size)
    }
}

private fun packCoord(x: Int, z: Int): Long =
    (x.toLong() and 0xFFFFFFFFL) shl 32 or (z.toLong() and 0xFFFFFFFFL)

private fun DrawScope.drawPlayerMarkers(
    positions: Map<String, PlayerPos3>,
    cellPx: Float,
    worldOriginX: Float,
    worldOriginY: Float,
    canvasSize: Size,
) {
    positions.forEach { (name, pos) ->
        // pos is in world blocks. Convert to chunks (16 blocks per chunk) then
        // to screen pixels. 1 block = cellPx / 16 px at the current zoom.
        val blockToPx = cellPx / 16f
        val x = worldOriginX + pos.x.toFloat() * blockToPx
        val z = worldOriginY + pos.z.toFloat() * blockToPx
        if (x < -8f || x > canvasSize.width + 8f) return@forEach
        if (z < -8f || z > canvasSize.height + 8f) return@forEach
        // Glow ring
        drawCircle(color = Color(0xFF00E5FF).copy(alpha = 0.35f), radius = 8f, center = Offset(x, z))
        drawCircle(color = Color(0xFF00E5FF), radius = 4f, center = Offset(x, z))
    }
}

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
    val cz = ((tapOffset.y - worldOriginY) / cellPx).toInt()
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
