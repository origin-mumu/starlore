package com.starlore.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared motion and scrolling rules for full-page App destinations.
 *
 * Forward pages follow the same direction as Android Activity navigation:
 * enter from the right and return to the right. Short lazy lists receive
 * viewport-relative breathing room so the App overscroll factory can provide
 * the same elastic feedback as long lists.
 */
object AppPageMotion {
    val forwardEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
    )

    val forwardExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
    )
}

@Composable
fun AppForwardPage(
    visible: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = AppPageMotion.forwardEnter,
        exit = AppPageMotion.forwardExit
    ) {
        BackHandler(onBack = onBack)
        Box(Modifier.fillMaxSize()) {
            content()
        }
    }
}

fun LazyListScope.appScrollBreathingRoom(
    key: String = "app-scroll-breathing-room",
    minViewportFraction: Float = 0.72f
) {
    item(key = key) {
        Spacer(Modifier.fillParentMaxHeight(minViewportFraction))
    }
}
