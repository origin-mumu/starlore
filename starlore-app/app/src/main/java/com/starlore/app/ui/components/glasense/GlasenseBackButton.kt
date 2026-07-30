package com.starlore.app.ui.components.glasense

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.starlore.app.R
import com.starlore.glasense.core.component.Icon

/**
 * Floating back control that shares the bottom navigation's liquid-glass material.
 */
@Composable
fun GlasenseBackButton(
    onClick: () -> Unit,
    backdrop: LayerBackdrop? = null,
    modifier: Modifier = Modifier
) {
    GlasenseNavigationButton(
        modifier = modifier,
        isActive = false,
        onClick = onClick,
        backdrop = backdrop,
        liquidGlass = true
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_forward_nav),
            contentDescription = stringResource(R.string.back),
            modifier = Modifier.size(26.dp)
        )
    }
}
