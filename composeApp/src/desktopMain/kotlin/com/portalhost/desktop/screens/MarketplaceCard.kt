package com.portalhost.desktop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portalhost.desktop.marketplace.SimpleAsyncImage
import com.portalhost.desktop.util.rememberResourcePainter
import com.portalhost.model.ModrinthProject

@Composable
fun MarketplaceCard(
    project: ModrinthProject,
    onClick: () -> Unit,
    onInstallClick: () -> Unit,
    formatDownloads: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val enchantmentBg = rememberResourcePainter("/icons/Enchantment.png")
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val accentColor = project.color?.let { Color(it) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = enchantmentBg,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surfaceColor.copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        SimpleAsyncImage(
                            url = project.iconUrl,
                            contentDescription = project.title,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            project.author?.let {
                                Text(
                                    text = "by $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = project.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatDownloads(project.downloads),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "downloads",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatDownloads(project.followers),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "followers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.clickable { onInstallClick() },
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = accentColor
                                        ?: MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = "Install",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (accentColor != null) Color.White
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryChip(
                            label = project.projectType.replaceFirstChar { it.uppercase() },
                            isPrimary = true
                        )
                        project.loaders.firstOrNull()?.let { loader ->
                            CategoryChip(
                                label = loader.replaceFirstChar { it.uppercase() },
                                isPrimary = false
                            )
                        }
                        SideBadge(side = project.serverSide, label = "Server")
                        SideBadge(side = project.clientSide, label = "Client")
                    }
                }
            }
        }
    }
}

@Composable
private fun SideBadge(side: String, label: String) {
    val (bgColor, textColor) = when (side) {
        "required" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "optional" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (side) {
        "required" -> "\u2713"
        "optional" -> "\u25CB"
        else -> "\u2717"
    }
    SuggestionChip(
        onClick = { },
        label = {
            Text(
                text = "$icon $label",
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = Modifier.height(22.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = bgColor,
            labelColor = textColor
        ),
        border = null
    )
}

@Composable
fun CategoryChip(label: String, isPrimary: Boolean) {
    SuggestionChip(
        onClick = { },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = Modifier.height(22.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = if (isPrimary)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = if (isPrimary)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = null
    )
}
