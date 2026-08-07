package com.portalhost.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.portalhost.app.R

@Composable
fun PortalHostLogo(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Image(
        painter = painterResource(R.drawable.portal_host_logo),
        contentDescription = "PortalHost",
        modifier = modifier.size(size)
    )
}
