package com.starlore.app.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
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