package com.starlore.glasense.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

object GlasenseThemeMode {
    const val LIGHT = 0
    const val DARK = 1
    const val SYSTEM = 2
}

enum class StarloreTheme {
    DEFAULT, WHITE, DARK, GREEN, BLUE, PINK
}

internal val LocalDarkTheme = compositionLocalOf { false }
internal val LocalStarloreTheme = compositionLocalOf { StarloreTheme.DEFAULT }

fun resolveDarkTheme(theme: StarloreTheme): Boolean {
    return theme == StarloreTheme.DARK
}


