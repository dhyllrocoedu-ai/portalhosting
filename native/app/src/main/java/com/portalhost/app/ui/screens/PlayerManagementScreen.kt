package com.portalhost.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PAGE_SIZE = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerManagementScreen(
    players: List<String>,
    isOnline: Boolean,
    onCommand: (String) -> Unit,
    onBack: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    val totalPages = ((players.size - 1) / PAGE_SIZE).coerceAtLeast(0) + 1
    val pagePlayers = players.drop(page * PAGE_SIZE).take(PAGE_SIZE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Management (${players.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (players.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        Text("No players online", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(pagePlayers, key = { it }) { player ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Player icon
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                // Player name
                                Text(
                                    player,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                // Action buttons
                                if (isOnline) {
                                    ActionChip("Kick") { onCommand("/kick $player") }
                                    Spacer(Modifier.width(4.dp))
                                    ActionChip("Ban") { onCommand("/ban $player") }
                                    Spacer(Modifier.width(4.dp))
                                    ActionChip("OP") { onCommand("/op $player") }
                                }
                            }
                        }
                    }
                }

                // Pagination footer
                if (totalPages > 1) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (page > 0) page-- }, enabled = page > 0) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                            }
                            Text("Page ${page + 1} of $totalPages", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { if (page < totalPages - 1) page++ }, enabled = page < totalPages - 1) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
