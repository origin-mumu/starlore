// Package declaration for the settings screen
package com.starlore.app.feature.settings

// Import necessary libraries and components
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import com.starlore.app.R
import com.starlore.app.feature.settings.util.AppIconManager
import com.starlore.app.feature.settings.util.SettingsViewModel
import com.starlore.app.theme.AppButtonColors
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.appPageBackground
import com.starlore.app.theme.AppSpecs
import com.starlore.app.theme.harmonize
import com.starlore.app.ui.components.glasense.GlasenseButton
import com.starlore.app.ui.components.glasense.GlasenseBackButton
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.GlasenseModalTopBar
import com.starlore.app.ui.components.glasense.GlasensePopup
import com.starlore.app.ui.components.glasense.PopupDirection
import com.starlore.app.ui.components.glasense.PopupState
import com.starlore.app.ui.components.glasense.isScrolledPast
import com.starlore.app.ui.components.packed.ColorModeSelector
import com.starlore.app.ui.components.packed.ConfigInfoHeader
import com.starlore.app.ui.components.packed.TopBarSpacer
import com.starlore.app.util.supportsRuntimeShaderEffect
import com.starlore.glasense.component.ListStack
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import com.starlore.glasense.core.component.VGap
import com.starlore.glasense.core.interaction.DimIndication
import com.starlore.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.starlore.glasense.theme.tokens.Amber500
import com.starlore.glasense.theme.tokens.Blue500
import com.starlore.glasense.theme.tokens.Cyan500
import com.starlore.glasense.theme.tokens.Emerald500
import com.starlore.glasense.theme.tokens.Fuchsia500
import com.starlore.glasense.theme.tokens.Green500
import com.starlore.glasense.theme.tokens.Indigo500
import com.starlore.glasense.theme.tokens.Lime500
import com.starlore.glasense.theme.tokens.Orange500
import com.starlore.glasense.theme.tokens.Pink500
import com.starlore.glasense.theme.tokens.Purple500
import com.starlore.glasense.theme.tokens.Red500
import com.starlore.glasense.theme.tokens.Rose500
import com.starlore.glasense.theme.tokens.Sky500
import com.starlore.glasense.theme.tokens.Teal500
import com.starlore.glasense.theme.tokens.Violet500
import com.starlore.glasense.theme.tokens.Yellow500

@Composable
fun AppearanceScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current
    val context = LocalContext.current

    // Calculate the height of the status bar to adjust layout
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    // Calculate the scroll threshold in pixels for showing/hiding the small title

    // Get colors from the app's custom theme
    val backgroundColor = AppColors.pageBackground

    // Remember the state for the lazy list to control scrolling
    val lazyListState = rememberLazyListState()

    // Determine if the small title should be visible based on the scroll position
    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)


    // State variables for the various appearance settings, managed by the ViewModel
    var isLiteMode by settingsViewModel.isLiteMode
    var isLiquidGlass by settingsViewModel.isLiquidGlass
    val currentMode by settingsViewModel.colorMode
    val currentAppIcon by settingsViewModel.appIcon
    val systemInDarkTheme = isSystemInDarkTheme()
    val appIconEntries = AppIconManager.AppIcon.entries

    val overscrollFactory = rememberOffsetOverscrollFactory()

    val backdrop = rememberLayerBackdrop {
        drawContent()
    }

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrim = AppColors.scrimMedium
    val stroke = with(density) { 2.dp.toPx() }

    // Root container for the screen, filling the entire available space
    Box(
        modifier = Modifier
            .fillMaxSize()
            .appPageBackground()
    ) {
        // A vertically scrolling list that only composes and lays out the currently visible items
        ListStack(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            TopBarSpacer()
            // Header item for the Appearance section
            item {
                ConfigInfoHeader(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = harmonize(Blue500),
                    backgroundColor = AppColors.cardBackground,
                    icon = painterResource(R.drawable.ic_twotone_image),
                    title = stringResource(R.string.appearance),
                    info = stringResource(R.string.craft_your_unique_style_with_a_few_adorable_tweaks)
                )
            }
            item { VGap(24.dp) }
            // Item for selecting the color mode (light/dark/system)
            ColorModeSelector(
                onChange = { settingsViewModel.colorMode(it) },
                currentMode = currentMode,
                systemInDarkTheme = systemInDarkTheme
            )
            if (supportsRuntimeShaderEffect()) {
                Section(
                    header = { stringResource(R.string.design) },
                    footer = {
                        stringResource(
                            R.string.enabling_liquid_glass_can_significantly_impact_performance
                        )
                    }
                ) {
                    CustomSwitchRow(
                        glass = true,
                        checked = isLiquidGlass,
                        onCheckedChange = { settingsViewModel.onLiquidGlassChanged(it) }) {
                        Text(stringResource(R.string.liquid_glass))
                    }
                }
            }
            NoPaddingSection(header = { stringResource(R.string.app_icon) }) {
                Row {
                    CompositionLocalProvider(LocalOverscrollFactory provides overscrollFactory) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(
                                items = appIconEntries,
                                key = { index, icon -> "${index}_${icon.alias}" }) { _, icon ->
                                AppIconOption(
                                    icon = icon,
                                    selected = currentAppIcon == icon,
                                    onClick = {
                                        if (currentAppIcon != icon) {
                                            settingsViewModel.onAppIconChanged(context, icon)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item { VGap() }
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.appearance),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = AppColors.pageBackground
        ) {
            // This lambda is empty as the component handles its own content
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
private fun AppIconOption(
    icon: AppIconManager.AppIcon,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) AppColors.primary else Color.Black.copy(alpha = 0.05f),
                    RoundedRectangle(16.dp)
                )
                .size(64.dp)
                .clip(RoundedRectangle(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = DimIndication(),
                    onClick = onClick
                )
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = icon.mipmapResId,
                contentDescription = stringResource(icon.displayNameResId),
                modifier = Modifier
                    .size(64.dp)
            )

        }
        Text(
            stringResource(icon.displayNameResId),
            fontSize = 12.sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
            color = AppColors.contentVariant
        )
    }
}
