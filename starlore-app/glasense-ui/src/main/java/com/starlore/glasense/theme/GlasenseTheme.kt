package com.starlore.glasense.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

object GlasenseTheme {
    val colors: GlasenseColors
        @Composable get() = LocalGlasenseColors.current

    val specs: GlasenseSpecs
        @Composable get() = LocalGlasenseSpecs.current

    val type: GlasenseType
        @Composable get() = LocalGlasenseType.current

    val darkTheme: Boolean
        @Composable get() = LocalDarkTheme.current

    val theme: StarloreTheme
        @Composable get() = LocalStarloreTheme.current
}

@Composable
fun GlasenseTheme(
    theme: StarloreTheme = StarloreTheme.DEFAULT,
    colors: GlasenseColors = when (theme) {
        StarloreTheme.DEFAULT -> GlasenseDefaultPalette
        StarloreTheme.WHITE -> GlasenseWhitePalette
        StarloreTheme.DARK -> GlasenseDarkPalette
        StarloreTheme.GREEN -> GlasenseGreenPalette
        StarloreTheme.BLUE -> GlasenseBluePalette
        StarloreTheme.PINK -> GlasensePinkPalette
    },
    specs: GlasenseSpecs = GlasenseSpecsStandard,
    type: GlasenseType = GlasenseTypeStandard,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalGlasenseColors provides colors,
        LocalGlasenseSpecs provides specs,
        LocalGlasenseType provides type,
        LocalGlasenseTextStyle provides type.body,
        LocalGlasenseContentColor provides colors.content,
        LocalDarkTheme provides (theme == StarloreTheme.DARK),
        LocalStarloreTheme provides theme
    ) {
        content()
    }
}