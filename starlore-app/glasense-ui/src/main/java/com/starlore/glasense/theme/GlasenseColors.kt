package com.starlore.glasense.theme

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.starlore.glasense.theme.tokens.Blue500
import com.starlore.glasense.theme.tokens.Green500
import com.starlore.glasense.theme.tokens.Red500
import com.starlore.glasense.theme.tokens.Yellow500
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Immutable
data class GlasenseColors(
    val background: Color,

    // for switches
    val activeTrack: Color,
    val inactiveTrack: Color,
    val activeThumb: Color,
    val inactiveThumb: Color,

    // scrim
    val scrimLight: Color,
    val scrimNormal: Color,
    val scrimMedium: Color,
    val scrimBold: Color,

    // for hierarchical cards
    val pageBackground: Color,
    val cardBackground: Color,

    // for bottom sheets
    val elevatedPageBackground: Color,
    val elevatedCardBackground: Color,

    val primary: Color,
    val onPrimary: Color,

    // main font colors
    val content: Color,
    val contentVariant: Color,

    val highlightText: Color,

    // error color
    val error: Color,
    val onError: Color,

    // for segmented control
    val segmentedControlBackground: Color = scrimNormal,
    val onSegmentedControlBackground: Color = contentVariant,
    val segmentedControlIndicator: Color,
    val onSegmentedControlIndicator: Color = content
)

val GlasenseDefaultPalette = GlasenseColors(
    background = Color(0xFFFFFCF7),
    activeTrack = Color(0xFFE85D2A),
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color(0xFFF9F6F0),
    cardBackground = Color.White,
    elevatedPageBackground = Color(0xFFF9F6F0),
    elevatedCardBackground = Color.White,
    scrimLight = Color.Black.copy(alpha = 0.025f),
    scrimNormal = Color.Black.copy(alpha = 0.05f),
    scrimMedium = Color.Black.copy(alpha = 0.1f),
    scrimBold = Color.Black.copy(alpha = 0.2f),
    primary = Color(0xFFE85D2A),
    onPrimary = Color.White,
    content = Color(0xFF1A1410),
    contentVariant = Color(0xFF1A1410).copy(.6f),
    highlightText = Color(0xFFFFD60A),
    error = Color(0xFFFF453A),
    onError = Color.White,
    segmentedControlIndicator = Color.White
)

val GlasenseWhitePalette = GlasenseColors(
    background = Color(0xFFF5F5F5),
    activeTrack = Color(0xFF333333),
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color(0xFFEDEDED),
    cardBackground = Color.White,
    elevatedPageBackground = Color(0xFFEDEDED),
    elevatedCardBackground = Color.White,
    scrimLight = Color.Black.copy(alpha = 0.02f),
    scrimNormal = Color.Black.copy(alpha = 0.04f),
    scrimMedium = Color.Black.copy(alpha = 0.08f),
    scrimBold = Color.Black.copy(alpha = 0.16f),
    primary = Color(0xFF333333),
    onPrimary = Color.White,
    content = Color(0xFF1A1A1A),
    contentVariant = Color(0xFF1A1A1A).copy(.6f),
    highlightText = Color(0xFFFFD60A),
    error = Color(0xFFFF453A),
    onError = Color.White,
    segmentedControlIndicator = Color.White
)

val GlasenseDarkPalette = GlasenseColors(
    background = Color(0xFF0F1117),
    activeTrack = Color(0xFF7B9AFF),
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color(0xFF07090C),
    cardBackground = Color(0xFF1B1D26),
    elevatedPageBackground = Color(0xFF1B1D26),
    elevatedCardBackground = Color(0xFF2C2F3D),
    scrimLight = Color.White.copy(alpha = 0.05f),
    scrimNormal = Color.White.copy(alpha = 0.1f),
    scrimMedium = Color.White.copy(alpha = 0.2f),
    scrimBold = Color.White.copy(alpha = 0.4f),
    primary = Color(0xFF7B9AFF),
    onPrimary = Color(0xFF0F1117),
    content = Color(0xFFF0F0F2),
    contentVariant = Color(0xFFF0F0F2).copy(.6f),
    highlightText = Color(0xFFFFD60A),
    error = Color(0xFFFF453A),
    onError = Color.White,
    segmentedControlIndicator = Color(0xFF2C2F3D)
)

val GlasenseGreenPalette = GlasenseColors(
    background = Color(0xFFF0F7EE),
    activeTrack = Color(0xFF4A8C5C),
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color(0xFFE2EFE0),
    cardBackground = Color.White,
    elevatedPageBackground = Color(0xFFE2EFE0),
    elevatedCardBackground = Color.White,
    scrimLight = Color.Black.copy(alpha = 0.025f),
    scrimNormal = Color.Black.copy(alpha = 0.05f),
    scrimMedium = Color.Black.copy(alpha = 0.1f),
    scrimBold = Color.Black.copy(alpha = 0.2f),
    primary = Color(0xFF4A8C5C),
    onPrimary = Color.White,
    content = Color(0xFF1A3A2D),
    contentVariant = Color(0xFF1A3A2D).copy(.6f),
    highlightText = Color(0xFFFFD60A),
    error = Color(0xFFFF453A),
    onError = Color.White,
    segmentedControlIndicator = Color.White
)

val GlasenseBluePalette = GlasenseColors(
    background = Color(0xFFEEF3F8),
    activeTrack = Color(0xFF3B7DD8),
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color(0xFFDFE9F2),
    cardBackground = Color.White,
    elevatedPageBackground = Color(0xFFDFE9F2),
    elevatedCardBackground = Color.White,
    scrimLight = Color.Black.copy(alpha = 0.025f),
    scrimNormal = Color.Black.copy(alpha = 0.05f),
    scrimMedium = Color.Black.copy(alpha = 0.1f),
    scrimBold = Color.Black.copy(alpha = 0.2f),
    primary = Color(0xFF3B7DD8),
    onPrimary = Color.White,
    content = Color(0xFF1A2A3E),
    contentVariant = Color(0xFF1A2A3E).copy(.6f),
    highlightText = Color(0xFFFFD60A),
    error = Color(0xFFFF453A),
    onError = Color.White,
    segmentedControlIndicator = Color.White
)

val GlasensePinkPalette = GlasenseColors(
    background = Color(0xFFFDF2F6),
    activeTrack = Color(0xFFD4638F),
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color(0xFFF9E3ED),
    cardBackground = Color.White,
    elevatedPageBackground = Color(0xFFF9E3ED),
    elevatedCardBackground = Color.White,
    scrimLight = Color.Black.copy(alpha = 0.025f),
    scrimNormal = Color.Black.copy(alpha = 0.05f),
    scrimMedium = Color.Black.copy(alpha = 0.1f),
    scrimBold = Color.Black.copy(alpha = 0.2f),
    primary = Color(0xFFD4638F),
    onPrimary = Color.White,
    content = Color(0xFF2D1B24),
    contentVariant = Color(0xFF2D1B24).copy(.6f),
    highlightText = Color(0xFFFFD60A),
    error = Color(0xFFFF453A),
    onError = Color.White,
    segmentedControlIndicator = Color.White
)

val GlasenseLightPalette = GlasenseDefaultPalette

internal val LocalGlasenseColors = staticCompositionLocalOf { GlasenseDefaultPalette }

data class OklchColor(
    val l: Float,
    val c: Float,
    val h: Float,
    val alpha: Float = 1f
) {
    fun toColor(): Color {
        val hRad = h * PI / 180.0

        val a = (c * cos(hRad)).toFloat()
        val b = (c * sin(hRad)).toFloat()

        return Color(
            colorSpace = ColorSpaces.Oklab,
            red = l,
            green = a,
            blue = b,
            alpha = alpha
        ).convert(ColorSpaces.Srgb)
    }
}

fun Color.toOklch(): OklchColor {
    val oklab = this.convert(ColorSpaces.Oklab)

    val l = oklab.component1()
    val a = oklab.component2()
    val b = oklab.component3()
    val alpha = oklab.alpha

    val c = hypot(a.toDouble(), b.toDouble()).toFloat()

    var h = (atan2(b.toDouble(), a.toDouble()) * 180.0 / PI).toFloat()
    if (h < 0f) {
        h += 360f
    }

    return OklchColor(l, c, h, alpha)
}

fun Color.purify(factor: Float = 1f): Color {
    val oklch = this.toOklch()

    val purifiedL = if (oklch.l == 1f) 1f else (oklch.l - 0.75f) * (1f - factor) + 0.75f
    val purifiedC = (oklch.c - 0.164f) * (1f - factor) + 0.164f

    return OklchColor(purifiedL, purifiedC, oklch.h, oklch.alpha).toColor()
}

fun Color.printOklch() {
    val oklch = this.toOklch()
    Log.d(
        "GlasenseColors",
        "Color: $this, Oklch: L=${oklch.l}, C=${oklch.c}, H=${oklch.h}, alpha=${oklch.alpha}"
    )
}

fun Color.umamify(factor: Float = 1f): Color {
    val oklch = this.toOklch()

    return OklchColor(oklch.l, oklch.c * factor, oklch.h, oklch.alpha).toColor()
}

fun Color.lumify(factor: Float = 1f): Color {
    val oklch = this.toOklch()

    return OklchColor((oklch.l * factor).coerceIn(0f, 1f), oklch.c, oklch.h, oklch.alpha).toColor()
}
