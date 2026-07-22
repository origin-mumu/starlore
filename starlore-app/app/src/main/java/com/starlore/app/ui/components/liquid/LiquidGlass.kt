package com.starlore.app.ui.components.liquid

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

/**
 * Starlore adapter for Kyant Backdrop's Liquid Glass catalog recipes.
 * Based on AndroidLiquidGlass-kmp's Apache-2.0 LiquidButton and LiquidBottomTabs examples.
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    cornerRadius: Dp,
    surfaceColor: Color = Color.Unspecified,
    tint: Color = Color.Unspecified,
    blurRadius: Dp = 8.dp,
    lensRadius: Dp = 20.dp,
    shadow: Boolean = true
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedRectangle(cornerRadius) },
    shadow = if (shadow) {
        {
            Shadow(
                radius = 22.dp,
                color = Color.Black.copy(alpha = .1f),
                offset = DpOffset(0.dp, 7.dp)
            )
        }
    } else null,
    innerShadow = { InnerShadow(radius = 3.dp, alpha = .14f) },
    highlight = { Highlight.Default.copy(alpha = .8f) },
    effects = {
        vibrancy()
        blur(blurRadius.toPx())
        lens(lensRadius.toPx(), lensRadius.toPx(), chromaticAberration = true)
    },
    onDrawSurface = {
        if (tint != Color.Unspecified) {
            drawRect(tint, blendMode = BlendMode.Hue)
            drawRect(tint.copy(alpha = .58f))
        }
        if (surfaceColor != Color.Unspecified) drawRect(surfaceColor)
    }
)

fun Modifier.liquidGlassCapsule(
    backdrop: Backdrop,
    surfaceColor: Color = Color.Unspecified,
    tint: Color = Color.Unspecified,
    blurRadius: Dp = 8.dp,
    lensRadius: Dp = 20.dp,
    shadow: Boolean = true
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { Capsule() },
    shadow = if (shadow) {
        { Shadow(radius = 22.dp, color = Color.Black.copy(alpha = .1f), offset = DpOffset(0.dp, 7.dp)) }
    } else null,
    innerShadow = { InnerShadow(radius = 3.dp, alpha = .14f) },
    highlight = { Highlight.Default.copy(alpha = .8f) },
    effects = {
        vibrancy()
        blur(blurRadius.toPx())
        lens(lensRadius.toPx(), lensRadius.toPx(), chromaticAberration = true)
    },
    onDrawSurface = {
        if (tint != Color.Unspecified) {
            drawRect(tint, blendMode = BlendMode.Hue)
            drawRect(tint.copy(alpha = .58f))
        }
        if (surfaceColor != Color.Unspecified) drawRect(surfaceColor)
    }
)

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp? = null,
    surfaceColor: Color = Color.Unspecified,
    tint: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.08f else 1f,
        animationSpec = spring(dampingRatio = .62f, stiffness = 360f),
        label = "liquidGlassPress"
    )
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.then(
            if (cornerRadius == null) {
                Modifier.liquidGlassCapsule(backdrop, surfaceColor, tint)
            } else {
                Modifier.liquidGlass(backdrop, cornerRadius, surfaceColor, tint)
            }
        ).clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
