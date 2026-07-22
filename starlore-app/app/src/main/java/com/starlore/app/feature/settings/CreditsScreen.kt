package com.starlore.app.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.starlore.app.R
import com.starlore.app.theme.AppButtonColors
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.appPageBackground
import com.starlore.app.theme.AppSpecs
import com.starlore.app.theme.harmonize
import com.starlore.app.ui.components.glasense.GlasenseButton
import com.starlore.app.ui.components.glasense.GlasenseBackButton
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.isScrolledPast
import com.starlore.app.ui.components.packed.ConfigInfoHeader
import com.starlore.app.ui.components.packed.TopBarSpacer
import com.starlore.glasense.component.ListRowAccessory
import com.starlore.glasense.component.ListStack
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import com.starlore.glasense.core.component.VGap
import com.starlore.glasense.theme.GlasenseTheme
import com.starlore.glasense.theme.tokens.Slate500

@Composable
fun CreditsScreen() {
    val activity = LocalActivity.current
    val uriHandler = LocalUriHandler.current

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val libraries by produceLibraries(R.raw.aboutlibraries)

    val backgroundColor = AppColors.pageBackground
    val backdrop = rememberLayerBackdrop {
        drawContent()
    }

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .appPageBackground()
    ) {
        ListStack(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            TopBarSpacer()
            item {
                ConfigInfoHeader(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = harmonize(Slate500),
                    backgroundColor = AppColors.cardBackground,
                    icon = painterResource(R.drawable.ic_twotone_info),
                    title = stringResource(R.string.credits),
                    info = stringResource(R.string.credits_info)
                )
            }
            libraries?.let { libs ->
                Section(key = "libraries", topSpacing = 24.dp) {
                    libs.libraries.forEach { library ->
                        Row(
                            key = library.uniqueId,
                            onClick = {
                                val url = library.website ?: library.scm?.url
                                url?.let { uriHandler.openUri(it) }
                            }, accessory = ListRowAccessory.Chevron
                        ) {
                            LibraryItem(library = library)
                        }
                    }
                }
            }
            item { VGap() }
        }
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.credits),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop
        ) {

        }
        GlasenseBackButton(
            onClick = { activity?.finish() },
            backdrop = backdrop,
            modifier = Modifier
                .padding(top = statusBarHeight, start = 12.dp)
                .size(48.dp)
                .align(Alignment.TopStart)
        )
    }
}

@Composable
fun LibraryItem(library: Library) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = library.name,
                style = GlasenseTheme.type.body,
                color = AppColors.content,
                modifier = Modifier.weight(1f)
            )
            library.artifactVersion?.let {
                Text(
                    text = it,
                    style = GlasenseTheme.type.body,
                    color = AppColors.contentVariant.copy(.3f)
                )
            }
        }
        val developers = library.developers.joinToString(", ") { it.name ?: "" }
        if (developers.isNotEmpty()) {
            Text(
                text = developers,
                style = GlasenseTheme.type.body,
                color = AppColors.contentVariant
            )
        }
    }
}
