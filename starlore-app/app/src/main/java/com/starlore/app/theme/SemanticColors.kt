package com.starlore.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.starlore.glasense.theme.GlasenseTheme

/** Stable feature accents. Components should choose a semantic role instead of a raw hue. */
@Immutable
data class StarloreSemanticColors(
    val brand: Color,
    val ai: Color,
    val knowledge: Color,
    val creation: Color,
    val personal: Color,
    val success: Color,
    val warning: Color
)

private val LightSemanticColors = StarloreSemanticColors(
    brand = Color(0xFF4F6EF7),
    ai = Color(0xFF586AF5),
    knowledge = Color(0xFF735EF1),
    creation = Color(0xFF3478F6),
    personal = Color(0xFF64718A),
    success = Color(0xFF22A06B),
    warning = Color(0xFFF4B928)
)

private val DarkSemanticColors = StarloreSemanticColors(
    brand = Color(0xFF8DA2FF),
    ai = Color(0xFFA899FF),
    knowledge = Color(0xFFB29CFF),
    creation = Color(0xFF7DB4FF),
    personal = Color(0xFFAAB4C8),
    success = Color(0xFF61D69D),
    warning = Color(0xFFFFD45C)
)

val AppSemanticColors: StarloreSemanticColors
    @Composable
    get() {
        val palette = if (GlasenseTheme.darkTheme) DarkSemanticColors else LightSemanticColors
        // Brand follows the user's custom primary color; feature roles remain recognizable.
        return palette.copy(brand = AppColors.primary)
    }
