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
import androidx.compose.ui.draw.drawBehind
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
        val base = if (dark) Color(0xFF12131B) else Color(0xFFF8F5F3)
        val colors = if (dark) {
            listOf(Color(0xFF6C5078), Color(0xFF335F68), Color(0xFF675446), Color(0xFF3D4E72))
        } else {
            listOf(Color(0xFFFFB7AA), Color(0xFFF8DCA0), Color(0xFFAEDDCB), Color(0xFFAFC9F2))
        }
        val alpha = if (dark) .25f else .48f
        drawBehind {
            drawRect(base)
            val radius = size.maxDimension * .72f
            val centers = listOf(
                Offset(size.width * .08f, size.height * .12f),
                Offset(size.width * .92f, size.height * .28f),
                Offset(size.width * .18f, size.height * .76f),
                Offset(size.width * .88f, size.height * .92f)
            )
            colors.zip(centers).forEach { (color, center) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
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
            background = Color.White,
            activeTrack = Color(0xFF333333),
            inactiveTrack = Color(0xFF787880).copy(.25f),
            activeThumb = Color.White,
            inactiveThumb = Color.White,
            pageBackground = Color(0xFFF3F4F6), // clean light gray background
            cardBackground = Color.White,
            elevatedPageBackground = Color(0xFFF3F4F6),
            elevatedCardBackground = Color.White,
            scrimLight = Color.Black.copy(alpha = 0.025f),
            scrimNormal = Color.Black.copy(alpha = 0.05f),
            scrimMedium = Color.Black.copy(alpha = 0.1f),
            scrimBold = Color.Black.copy(alpha = 0.2f),
            primary = Color(0xFF333333),
            onPrimary = Color.White,
            content = Color.Black,
            contentVariant = Color.Black.copy(.5f),
            highlightText = Color(0xFFFFD60A),
            error = Color(0xFFFF453A),
            onError = Color.White,
            segmentedControlIndicator = Color.White
        )
    }

    val isCustomPrimaryColor = settingsViewModel.isCustomPrimaryColorEnabled.value
    val customPrimaryColorArgb = settingsViewModel.themePrimaryColor.intValue

    val themePrimary = if (isCustomPrimaryColor) {
        Color(customPrimaryColorArgb)
    } else {
        when (resolvedTheme) {
            StarloreTheme.DEFAULT -> Color(0xFFE85D2A)
            StarloreTheme.WHITE -> Color(0xFF333333)
            StarloreTheme.DARK -> Color(0xFF7B9AFF)
            StarloreTheme.GREEN -> Color(0xFF4A8C5C)
            StarloreTheme.BLUE -> Color(0xFF3B7DD8)
            StarloreTheme.PINK -> Color(0xFFD4638F)
        }
    }

    val glasenseColors = baseGlasenseColors.copy(
        primary = themePrimary,
        activeTrack = themePrimary
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
