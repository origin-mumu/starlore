package com.starlore.app.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.R
import com.starlore.app.theme.AppButtonColors
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.appPageBackground
import com.starlore.app.theme.harmonize
import com.starlore.app.ui.components.glasense.GlasenseButtonToolBar
import com.starlore.app.ui.components.glasense.GlasenseBackButton
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.GlasensePageHeader
import com.starlore.app.ui.components.glasense.isScrolledPast
import com.starlore.app.ui.components.packed.AboutEntryItem
import com.starlore.app.ui.components.packed.ConfigContainer
import com.starlore.app.ui.components.packed.ConfigEntryItem
import com.starlore.app.ui.components.packed.PageContent
import com.starlore.glasense.component.paddingItem
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.VGap
import com.starlore.glasense.theme.tokens.Blue500
import com.starlore.glasense.theme.tokens.Pink400
import com.starlore.glasense.theme.tokens.Purple500
import com.starlore.glasense.theme.tokens.Slate500

@Composable
fun SettingsScreen() {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val hierarchicalSurfaceColor = AppColors.cardBackground

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val context = LocalContext.current
    val activity = LocalActivity.current

    val backgroundColor = AppColors.pageBackground
    val backdrop = rememberLayerBackdrop {
        drawContent()
    }
    Box(modifier = Modifier.appPageBackground()) {
        PageContent(
            state = lazyListState,
            modifier = Modifier
                .layerBackdrop(backdrop),
            tabPadding = true
        ) {
            item {
                GlasensePageHeader(
                    title = stringResource(R.string.settings)
                )
            }
            item {
                ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ConfigEntryItem(
                            brush = Brush.sweepGradient(
                                colorStops = arrayOf(
                                    0f to harmonize(Pink400),
                                    0.33f to harmonize(Purple500),
                                    0.66f to harmonize(Blue500),
                                    1f to harmonize(Pink400)
                                )
                            ),
                            icon = painterResource(R.drawable.ic_twotone_sparkles),
                            title = stringResource(R.string.ai),
                            enableGlow = true,
                            onClick = {
                                context.startActivity(
                                    SettingsActivity.createIntent(context, SettingsDestination.AI)
                                )
                            }
                        )
                    }
                }
                VGap()
            }
            item {
                ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ConfigEntryItem(
                            color = harmonize(Blue500),
                            icon = painterResource(R.drawable.ic_twotone_image),
                            title = stringResource(R.string.appearance),
                            onClick = {
                                context.startActivity(
                                    SettingsActivity.createIntent(
                                        context,
                                        SettingsDestination.APPEARANCE
                                    )
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ConfigEntryItem(
                            color = harmonize(Slate500),
                            icon = painterResource(R.drawable.ic_twotone_gear),
                            title = stringResource(R.string.general),
                            onClick = {
                                context.startActivity(
                                    SettingsActivity.createIntent(
                                        context,
                                        SettingsDestination.GENERAL
                                    )
                                )
                            }
                        )
                    }
                }
                VGap()
            }
            item {
                ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                    AboutEntryItem(
                        icon = painterResource(R.drawable.app_icon_default),
                        onClick = {
                            context.startActivity(
                                SettingsActivity.createIntent(context, SettingsDestination.ABOUT)
                            )
                        }
                    )
                }
            }
            paddingItem(lazyListState)
        }
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.settings),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop
        ) {
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp)
        ) {
            GlasenseBackButton(
                onClick = {
                    activity?.finish()
                },
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(48.dp)
            )
        }
    }
}
