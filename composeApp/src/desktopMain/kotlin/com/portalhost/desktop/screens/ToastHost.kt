package com.portalhost.desktop.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.uinotify.ToastManager
import com.portalhost.uinotify.ToastType
import org.koin.compose.koinInject

@Composable
fun ToastHost() {
    val toastManager = koinInject<ToastManager>()
    val toasts by toastManager.toasts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 48.dp, end = 48.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        toasts.forEach { toast ->
            ToastCard(toast = toast)
        }
    }
}

private data class ToastStyle(
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val iconTint: Color,
)

private fun styleForType(type: ToastType, colors: androidx.compose.material3.ColorScheme): ToastStyle = when (type) {
    ToastType.Info -> ToastStyle(
        icon = Icons.Filled.Info,
        containerColor = colors.surfaceVariant,
        contentColor = colors.onSurfaceVariant,
        iconTint = colors.primary,
    )
    ToastType.Success -> ToastStyle(
        icon = Icons.Filled.CheckCircle,
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        iconTint = colors.primary,
    )
    ToastType.Warning -> ToastStyle(
        icon = Icons.Filled.Warning,
        containerColor = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
        iconTint = colors.tertiary,
    )
    ToastType.Error -> ToastStyle(
        icon = Icons.Filled.Error,
        containerColor = colors.errorContainer,
        contentColor = colors.onErrorContainer,
        iconTint = colors.error,
    )
}

@Composable
private fun ToastCard(toast: com.portalhost.uinotify.Toast) {
    val style = styleForType(toast.type, MaterialTheme.colorScheme)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = style.containerColor,
            ),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.iconTint,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    toast.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = style.contentColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
