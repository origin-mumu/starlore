package com.starlore.app.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.R
import com.starlore.app.feature.settings.util.AppLocaleManager
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.appPageBackground
import com.starlore.app.theme.AppSpecs
import com.starlore.app.theme.harmonize
import com.starlore.app.ui.components.glasense.GlasenseBackButton
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.GlasenseMenu
import com.starlore.app.ui.components.glasense.GlasenseMenuItem
import com.starlore.app.ui.components.glasense.MenuDivider
import com.starlore.app.ui.components.glasense.MenuState
import com.starlore.app.ui.components.glasense.SelectiveMenuItemData
import com.starlore.app.ui.components.glasense.isScrolledPast
import com.starlore.app.ui.components.packed.ConfigInfoHeader
import com.starlore.app.ui.components.packed.TopBarSpacer
import com.starlore.glasense.component.ListRowAccessory
import com.starlore.glasense.component.ListStack
import com.starlore.glasense.core.component.Text
import com.starlore.glasense.core.component.VGap
import com.starlore.glasense.theme.tokens.Slate500

@Composable
fun GeneralScreen() {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val lazyListState = rememberLazyListState()
    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val selectedLanguageTag = AppLocaleManager.getLanguageTag(context)
    val systemLanguageText = stringResource(R.string.system_language)
    val englishText = stringResource(R.string.english)
    val simplifiedChineseText = stringResource(R.string.simplified_chinese)
    val currentLanguageText = when (selectedLanguageTag) {
        AppLocaleManager.ENGLISH -> englishText
        AppLocaleManager.SIMPLIFIED_CHINESE -> simplifiedChineseText
        else -> systemLanguageText
    }

    var languageButtonBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var menuState by remember { mutableStateOf(MenuState()) }
    val languageMenuItems = remember(
        selectedLanguageTag,
        systemLanguageText,
        englishText,
        simplifiedChineseText,
        context
    ) {
        listOf(
            SelectiveMenuItemData(
                text = systemLanguageText,
                isSelected = { selectedLanguageTag == AppLocaleManager.SYSTEM },
                onClick = { AppLocaleManager.setLanguageTag(context, AppLocaleManager.SYSTEM) }
            ),
            MenuDivider,
            SelectiveMenuItemData(
                text = englishText,
                isSelected = { selectedLanguageTag == AppLocaleManager.ENGLISH },
                onClick = { AppLocaleManager.setLanguageTag(context, AppLocaleManager.ENGLISH) }
            ),
            SelectiveMenuItemData(
                text = simplifiedChineseText,
                isSelected = { selectedLanguageTag == AppLocaleManager.SIMPLIFIED_CHINESE },
                onClick = {
                    AppLocaleManager.setLanguageTag(context, AppLocaleManager.SIMPLIFIED_CHINESE)
                }
            )
        )
    }

    val backgroundColor = AppColors.pageBackground
    val backdrop = rememberLayerBackdrop {
        drawContent()
    }

    Box(Modifier.fillMaxSize().appPageBackground()) {
        ListStack(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            TopBarSpacer()
            item {
                ConfigInfoHeader(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = harmonize(Slate500),
                    backgroundColor = AppColors.cardBackground,
                    icon = painterResource(R.drawable.ic_twotone_gear),
                    title = stringResource(R.string.general),
                    info = stringResource(R.string.language)
                )
            }
            Section(topSpacing = 24.dp) {
                Row(
                    onClick = {
                        languageButtonBounds?.let { bounds ->
                            menuState = MenuState(
                                isVisible = true,
                                anchorBounds = bounds.boundsInWindow(),
                                items = languageMenuItems
                            )
                        }
                    },
                    trailing = {
                        Text(
                            text = currentLanguageText,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                languageButtonBounds = coordinates
                            }
                        )
                    },
                    accessory = ListRowAccessory.SelectIndicator
                ) {
                    Text(text = stringResource(R.string.language))
                }
            }
            item { VGap() }
        }

        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.general),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop
        ) {}
        GlasenseBackButton(
            onClick = { activity?.finish() },
            backdrop = backdrop,
            modifier = Modifier
                .padding(top = statusBarHeight, start = 12.dp)
                .size(48.dp)
                .align(Alignment.TopStart)
        )
        GlasenseMenu(
            menuState = menuState,
            backdrop = backdrop,
            onDismiss = { menuState = menuState.copy(isVisible = false) }
        )
    }
}
