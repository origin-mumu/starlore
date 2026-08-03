package com.starlore.app.theme

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starlore.app.feature.settings.util.SettingsManager
import com.starlore.app.feature.settings.util.SettingsViewModel
import com.starlore.glasense.theme.GlasenseColors
import com.starlore.glasense.theme.GlasenseDefaultPalette
import com.starlore.glasense.theme.GlasenseWhitePalette
import com.starlore.glasense.theme.GlasenseDarkPalette
import com.starlore.glasense.theme.GlasenseGreenPalette
import com.starlore.glasense.theme.GlasenseBluePalette
import com.starlore.glasense.theme.GlasensePinkPalette
import com.starlore.glasense.theme.GlasenseSpecs
import com.starlore.glasense.theme.GlasenseSpecsStandard
import com.starlore.glasense.theme.GlasenseSpecsVariant
import com.starlore.glasense.theme.GlasenseTheme
import com.starlore.glasense.theme.StarloreTheme

val AppColors: GlasenseColors
    @Composable
    get() = GlasenseTheme.colors

val AppSpecs: GlasenseSpecs
    @Composable
    get() = GlasenseTheme.specs

/** Color token used by material effects; the actual page canvas is drawn by [appPageBackground]. */
val AppPageColor: Color
    @Composable
    get() = AppColors.pageBackground

fun Modifier.appPageBackground(): Modifier = composed {
    val enabled = SettingsManager.isMacaronBackgroundState.value
    val dark = GlasenseTheme.darkTheme
    val solid = AppColors.pageBackground
    if (!enabled) {
        background(solid)
    } else {
        val base = if (dark) Color(0xFF0B1020) else Color(0xFFF9FAFD)
        val colors = if (dark) {
            listOf(
                Color(0xFF243C9C),
                Color(0xFF5846B8),
                Color(0xFF234F77),
                Color(0xFF3C2F82),
                Color(0xFF315E91),
                Color(0xFF493795)
            )
        } else {
            listOf(
                Color(0x307AA7FF),
                Color(0x286D8EFF),
                Color(0x247FD6FF),
                Color(0x20B9A7FF),
                Color(0x247D6BFF),
                Color(0x60FCFDFF)
            )
        }
        val intensity = if (dark) .26f else .18f
        drawWithCache {
            // Cache the six large brushes until size or theme inputs change. Previously
            // these gradients and their temporary lists were rebuilt on every scroll frame.
            val radius = size.width * .92f
            val centers = arrayOf(
                Offset(size.width * .08f, size.height * .88f),
                Offset(size.width * .78f, size.height * .94f),
                Offset(size.width * .42f, size.height * 1.08f),
                Offset(size.width * 1.02f, size.height * .82f),
                Offset(size.width * .18f, size.height * 1.18f),
                Offset(size.width * .64f, size.height * 1.24f)
            )
            val brushes = Array(colors.size) { index ->
                val color = colors[index]
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to color.copy(alpha = color.alpha * intensity),
                        1f to color.copy(alpha = 0f)
                    ),
                    center = centers[index],
                    radius = radius
                )
            }
            onDrawBehind {
                drawRect(base)
                brushes.forEachIndexed { index, brush ->
                    drawCircle(
                        brush = brush,
                        radius = radius,
                        center = centers[index]
                    )
                }
            }
        }
    }
}

@Composable
fun GlasenseTheme(
    settingsViewModel: SettingsViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val mode = settingsViewModel.colorMode.intValue
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (mode) {
        0 -> false // Light
        1 -> true  // Dark
        2 -> isSystemDark // System (Automatic)
        else -> isSystemDark
    }
    val resolvedTheme = if (isDark) StarloreTheme.DARK else StarloreTheme.WHITE
    val liquidGlass = settingsViewModel.isLiquidGlass.value

    val baseGlasenseColors = if (isDark) {
        GlasenseDarkPalette
    } else {
        GlasenseColors(
            background = Color(0xFFFAFBFF),
            activeTrack = Color(0xFF4F6EF7),
            inactiveTrack = Color(0xFF787880).copy(.25f),
            activeThumb = Color.White,
            inactiveThumb = Color.White,
            pageBackground = Color(0xFFF9FAFD),
            cardBackground = Color(0xFFFEFEFF),
            elevatedPageBackground = Color(0xFFF4F6FA),
            elevatedCardBackground = Color(0xFFFEFEFF),
            scrimLight = Color.Black.copy(alpha = 0.025f),
            scrimNormal = Color.Black.copy(alpha = 0.05f),
            scrimMedium = Color.Black.copy(alpha = 0.1f),
            scrimBold = Color.Black.copy(alpha = 0.2f),
            primary = Color(0xFF4F6EF7),
            onPrimary = Color(0xFFF8FAFF),
            content = Color(0xFF0E1B3A),
            contentVariant = Color(0xFF53617A),
            highlightText = Color(0xFFFFD60A),
            error = Color(0xFFFF453A),
            onError = Color.White,
            segmentedControlIndicator = Color.White
        )
    }

    val themePrimary = if (isDark) Color(0xFF8DA2FF) else Color(0xFF4F6EF7)

    val glasenseColors = baseGlasenseColors.copy(
        background = if (isDark) Color(0xFF0B1020) else baseGlasenseColors.background,
        pageBackground = if (isDark) Color(0xFF0B1020) else baseGlasenseColors.pageBackground,
        cardBackground = if (isDark) Color(0xFF151C31) else baseGlasenseColors.cardBackground,
        elevatedPageBackground = if (isDark) Color(0xFF11182B) else baseGlasenseColors.elevatedPageBackground,
        elevatedCardBackground = if (isDark) Color(0xFF202A46) else baseGlasenseColors.elevatedCardBackground,
        content = if (isDark) Color(0xFFF1F4FF) else baseGlasenseColors.content,
        contentVariant = if (isDark) Color(0xFFB5BED3) else baseGlasenseColors.contentVariant,
        primary = themePrimary,
        activeTrack = themePrimary,
        onPrimary = if (isDark) Color(0xFF10162A) else Color(0xFFF8FAFF)
    )

    val glasenseSpecs = GlasenseSpecsVariant

    val liteMode = SettingsManager.isLiteModeState.value

    val glasenseSettings = remember(liquidGlass, liteMode) {
        GlasenseSettings(
            liquidGlass = liquidGlass,
            liteMode = liteMode,
            dynamicColor = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = resolvedTheme != StarloreTheme.DARK
            windowInsetsController.isAppearanceLightNavigationBars = resolvedTheme != StarloreTheme.DARK
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
        }
    }

    GlasenseTheme(
        theme = resolvedTheme,
        colors = glasenseColors,
        specs = glasenseSpecs
    ) {
        CompositionLocalProvider(
            LocalGlasenseSettings provides glasenseSettings
        ) {
            content()
        }
    }
}
