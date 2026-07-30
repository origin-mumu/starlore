package com.starlore.app.ui.components.glasense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.NavigationButtonActiveColors
import com.starlore.app.theme.NavigationButtonNormalColors
import com.starlore.app.ui.components.glasense.material.MaterialRecipes
import com.starlore.app.ui.components.glasense.material.rememberMaterialRenderEffectOrNull
import com.starlore.app.ui.components.liquid.LiquidGlassButton
import com.starlore.glasense.theme.GlasenseTheme

/**
 * A custom navigation button with active and inactive states.
 *
 * @param modifier The modifier to be applied to the button.
 * @param isActive Whether the button is currently active.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param backdrop The backdrop layer for the glassmorphism effect.
 * @param content The content to be displayed inside the button.
 */
@Composable
fun GlasenseNavigationButton(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    onClick: () -> Unit,
    backdrop: LayerBackdrop? = null,
    liquidGlass: Boolean = false,
    content: @Composable () -> Unit
) {
    val tint = AppColors.primary

    if (liquidGlass && backdrop != null) {
        LiquidGlassButton(
            onClick = onClick,
            backdrop = backdrop,
            modifier = modifier
                .dropShadow(
                    Capsule(),
                    androidx.compose.ui.graphics.shadow.Shadow(
                        12.dp,
                        Color.Black.copy(alpha = 0.16f),
                        0.dp,
                        DpOffset(0.dp, 4.dp)
                    )
                )
                .fillMaxHeight(),
            tint = if (isActive) tint else Color.Unspecified,
            surfaceColor = if (isActive) Color.Unspecified else AppColors.cardBackground.copy(alpha = .22f)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
        }
        return
    }

    val materialEffect =
        rememberMaterialRenderEffectOrNull(
            if (liquidGlass) MaterialRecipes.thin() else MaterialRecipes.appBar()
        )
    val backdropModifier = if (backdrop != null) materialEffect?.let { renderEffect ->
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            shadow = {
                Shadow(
                    radius = 14.dp,
                    color = Color.Black.copy(alpha = 0.14f),
                    offset = DpOffset(0.dp, 5.dp)
                )
            },
            innerShadow = null,
            highlight = {
                if (liquidGlass) Highlight.Default.copy(
                    style = HighlightStyle.Default(
                        angle = 90f
                    )
                ) else null
            },
            effects = {
                padding = if (liquidGlass) 8f.dp.toPx() * 2 else 32f.dp.toPx() * 2
                if (!isActive) {
                    effect(renderEffect)
                    blur(if (liquidGlass) 8f.dp.toPx() else 32f.dp.toPx(), TileMode.Clamp)
                    if (liquidGlass) lens(16f.dp.toPx(), 48f.dp.toPx())
                }
                if (isActive) {
                    if (liquidGlass) {
                        blur(8f.dp.toPx(), TileMode.Clamp)
                        lens(16f.dp.toPx(), 48f.dp.toPx())
                    }
                }
            },
            onDrawSurface = {
                if (liquidGlass && isActive) {
                    drawRect(tint, blendMode = BlendMode.Hue, alpha = .8f)
                    drawRect(tint.copy(alpha = 0.7f))
                }
                if (isActive && !liquidGlass) {
                    drawRect(tint)
                }
            }
        )
    } else null
    val resolvedBackdropModifier = backdropModifier ?: Modifier
        .dropShadow(
            Capsule(),
            androidx.compose.ui.graphics.shadow.Shadow(
                14.dp,
                Color.Black.copy(alpha = 0.14f),
                0.dp,
                DpOffset(0.dp, 5.dp)
            )
        )
        .clip(Capsule())
        .background(if (isActive) tint else GlasenseTheme.colors.cardBackground)

    val finalModifier =
        Modifier
            .fillMaxSize()
            .then(resolvedBackdropModifier)
            .then(if (!liquidGlass) Modifier.glasenseHighlight(100.dp) else Modifier)

    // The base button with shape, click handling, shadow, and colors.
    GlasenseButton(
        shape = Capsule(),
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight(),
        colors = if (isActive) NavigationButtonActiveColors.primary() else NavigationButtonNormalColors.primary(),
        animated = false
    ) {
        // Box to apply the background modifier and center the content.
        Box(modifier = finalModifier, contentAlignment = Alignment.Center) {
            content()
        }
    }
}
