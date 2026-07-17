package com.starlore.app.theme

import androidx.compose.runtime.Composable
import com.starlore.app.feature.settings.util.SettingsManager
import com.starlore.glasense.theme.StarloreTheme
import com.starlore.glasense.theme.resolveDarkTheme

@Composable
fun isAppInDarkTheme(): Boolean {
    val mode = SettingsManager.colorModeState.intValue
    return resolveDarkTheme(StarloreTheme.entries.getOrElse(mode) { StarloreTheme.DEFAULT })
}