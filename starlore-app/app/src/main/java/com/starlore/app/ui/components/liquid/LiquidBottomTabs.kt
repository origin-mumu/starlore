/*
 * Adapted from AndroidLiquidGlass-kmp's catalog LiquidBottomTabs component.
 * Copyright 2025 Kyant. Licensed under the Apache License, Version 2.0.
 * Modified for Starlore's Android navigation and theme system.
 */
package com.starlore.app.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private val LocalLiquidBottomTabScale = compositionLocalOf<() -> Float> { { 1f } }

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val tabsBackdrop = rememberLayerBackdrop()
    var currentIndex by remember { mutableIntStateOf(selectedTabIndex) }
    val position = remember { Animatable(selectedTabIndex.toFloat()) }
    val press = remember { Animatable(0f) }
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    val panelDrag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedTabIndex) {
        currentIndex = selectedTabIndex
        if (position.value != selectedTabIndex.toFloat()) {
            coroutineScope {
                launch { press.animateTo(1f, spring(1f, 1000f, .001f)) }
                launch { scaleX.animateTo(70f / 52f, spring(.6f, 250f, .001f)) }
                launch { scaleY.animateTo(70f / 52f, spring(.7f, 250f, .001f)) }
                launch {
                    position.animateTo(
                        selectedTabIndex.toFloat(),
                        spring(1f, 1000f, 0.001f)
                    )
                }
            }
            coroutineScope {
                launch { press.animateTo(0f, spring(1f, 1000f, .001f)) }
                launch { scaleX.animateTo(1f, spring(.6f, 250f, .001f)) }
                launch { scaleY.animateTo(1f, spring(.7f, 250f, .001f)) }
            }
        }
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val tabWidthPx = with(density) {
            (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount
        }
        val panelOffset = with(density) {
            val fraction = (panelDrag.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
            4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
        }

        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    },
                    layerBlock = {
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, press.value)
                        this.scaleX = scale
                        this.scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(60.dp)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides { lerp(1f, 1.2f, press.value) }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(
                                24.dp.toPx() * press.value,
                                24.dp.toPx() * press.value
                            )
                        },
                        highlight = { Highlight.Default.copy(alpha = press.value) },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .height(52.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX = if (isLtr) {
                        position.value * tabWidthPx + panelOffset
                    } else {
                        size.width - (position.value + 1f) * tabWidthPx + panelOffset
                    }
                }
                .pointerInput(tabsCount, tabWidthPx) {
                    detectDragGestures(
                        onDragStart = {
                            scope.launch {
                                launch { press.animateTo(1f, spring(1f, 1000f, 0.001f)) }
                                launch { scaleX.animateTo(70f / 52f, spring(.6f, 250f, .001f)) }
                                launch { scaleY.animateTo(70f / 52f, spring(.7f, 250f, .001f)) }
                            }
                        },
                        onDragEnd = {
                            val target = position.value.fastRoundToInt()
                                .fastCoerceIn(0, tabsCount - 1)
                            currentIndex = target
                            onTabSelected(target)
                            scope.launch {
                                launch { position.animateTo(target.toFloat(), spring(1f, 1000f, .001f)) }
                                launch { panelDrag.animateTo(0f, spring(1f, 300f, .5f)) }
                                launch { press.animateTo(0f, spring(1f, 1000f, .001f)) }
                                launch { scaleX.animateTo(1f, spring(.6f, 250f, .001f)) }
                                launch { scaleY.animateTo(1f, spring(.7f, 250f, .001f)) }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                launch { position.animateTo(currentIndex.toFloat()) }
                                launch { panelDrag.animateTo(0f) }
                                launch { press.animateTo(0f) }
                                launch { scaleX.animateTo(1f) }
                                launch { scaleY.animateTo(1f) }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            position.snapTo(
                                (position.value + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                                    .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                            )
                            panelDrag.snapTo(panelDrag.value + dragAmount.x)
                        }
                    }
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        lens(
                            10.dp.toPx() * press.value,
                            14.dp.toPx() * press.value,
                            chromaticAberration = true
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = press.value) },
                    shadow = { Shadow(alpha = press.value) },
                    innerShadow = {
                        InnerShadow(
                            radius = 8.dp * press.value,
                            alpha = press.value
                        )
                    },
                    layerBlock = {
                        this.scaleX = scaleX.value
                        this.scaleY = scaleY.value
                        val velocity = position.velocity / 10f
                        this.scaleX /= 1f - (velocity * .75f).fastCoerceIn(-.2f, .2f)
                        this.scaleY *= 1f - (velocity * .25f).fastCoerceIn(-.2f, .2f)
                    },
                    onDrawSurface = {
                        drawRect(Color.Black.copy(alpha = .1f), alpha = 1f - press.value)
                        drawRect(Color.Black.copy(alpha = .03f * press.value))
                    }
                )
                .height(52.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidBottomTabScale.current
    Column(
        modifier
            .clip(Capsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                scaleX = scale()
                scaleY = scale()
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
