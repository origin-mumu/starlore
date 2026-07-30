package com.starlore.app.feature.main

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.R
import com.starlore.app.feature.article.ArticleDetailActivity
import com.starlore.app.feature.article.ArticleEditorActivity
import com.starlore.app.feature.article.ArticlesScreen
import com.starlore.app.feature.article.CategoryManageActivity
import com.starlore.app.feature.auth.LoginScreen
import com.starlore.app.feature.chat.ChatActivity
import com.starlore.app.feature.diverge.DivergeScreen
import com.starlore.app.feature.profile.ProfileScreen
import com.starlore.app.feature.settings.util.SettingsManager
import com.starlore.app.feature.settings.update.UpdateBottomSheet
import com.starlore.app.feature.settings.update.UpdateCheckResult
import com.starlore.app.feature.settings.update.UpdateChecker
import com.starlore.app.feature.settings.update.UpdateInfo
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.AppPageColor
import com.starlore.app.theme.appPageBackground
import com.starlore.app.theme.LocalGlasenseSettings
import com.starlore.app.ui.components.glasense.GlasenseNavigationButton
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.ui.components.glasense.material.MaterialRecipes
import com.starlore.app.ui.components.glasense.material.rememberMaterialRenderEffectOrNull
import com.starlore.app.ui.components.liquid.liquidGlassCapsule
import com.starlore.app.ui.components.liquid.LiquidBottomTab
import com.starlore.app.ui.components.liquid.LiquidBottomTabs
import com.kyant.shapes.Capsule
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

sealed class Screen(val route: String, val title: String, val iconRes: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_star)
    object Articles : Screen("articles", "星记", R.drawable.ic_nav_document_filled)
    object Chat : Screen("chat", "AI助手", R.drawable.ic_nav_sparkle_filled)
    object Diverge : Screen("diverge", "发散", R.drawable.ic_nav_bulb_filled)
    object Profile : Screen("profile", "我", R.drawable.ic_nav_user_filled)
}

@Composable
fun MainScreen() {
    var isLoggedIn by remember { mutableStateOf(SettingsManager.authToken.isNotEmpty()) }
    val authSessionVersion by SettingsManager.authSessionVersionState
    var startupUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        if (!SettingsManager.isCheckUpdatesOnStartup) return@LaunchedEffect

        when (val result = UpdateChecker.check()) {
            is UpdateCheckResult.HasUpdate -> {
                SettingsManager.lastUpdateCheckAt = System.currentTimeMillis()
                startupUpdateInfo = result.updateInfo
            }
            UpdateCheckResult.NoUpdate -> {
                SettingsManager.lastUpdateCheckAt = System.currentTimeMillis()
            }
            UpdateCheckResult.NotConfigured,
            is UpdateCheckResult.Failed -> Unit
        }
    }

    if (!isLoggedIn) {
        LoginScreen(onAuthSuccess = { isLoggedIn = true })
    } else {
        MainShell(
            authSessionVersion = authSessionVersion,
            onLogout = { isLoggedIn = false }
        )
    }

    startupUpdateInfo?.let { info ->
        UpdateBottomSheet(
            updateInfo = info,
            onDismissed = {
                if (!info.isRequired) startupUpdateInfo = null
            }
        )
    }
}

@Composable
fun MainShell(authSessionVersion: Int, onLogout: () -> Unit) {
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Home.route) }
    var contentVersion by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val hasOverlay = false
    val detailLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) contentVersion++
        if (result.resultCode == Activity.RESULT_FIRST_USER) {
            result.data?.getStringExtra(ArticleDetailActivity.EXTRA_AI_PROMPT)?.let { prompt ->
                context.startActivity(ChatActivity.createIntent(context, prompt))
            }
        }
    }
    val editorLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            contentVersion++
            result.data?.getIntExtra(ArticleEditorActivity.EXTRA_SAVED_ARTICLE_ID, -1)
                ?.takeIf { it >= 0 }
                ?.let { detailLauncher.launch(ArticleDetailActivity.createIntent(context, it)) }
        }
    }
    val categoryManagerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) contentVersion++
    }

    val items = listOf(
        Screen.Home,
        Screen.Articles,
        Screen.Diverge,
        Screen.Profile
    )

    val cardBg = AppColors.cardBackground
    val primaryColor = AppColors.primary
    val contentVariantColor = AppColors.contentVariant

    val surfaceColor = AppPageColor
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = surfaceColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Each active destination paints the same full-screen page background.
            // Keep only a solid fallback here to avoid drawing all six gradients twice.
            .background(surfaceColor)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        // Active Content View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (useNavigationRail) 112.dp else 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .layerBackdrop(backdrop)
            ) {
                when (currentRoute) {
                    Screen.Home.route -> HomeScreen(
                        accountSessionKey = authSessionVersion,
                        refreshToken = contentVersion,
                        onAskAi = { context.startActivity(ChatActivity.createIntent(context)) },
                        onCreateArticle = { editorLauncher.launch(ArticleEditorActivity.createIntent(context)) },
                        onOpenArticles = { currentRoute = Screen.Articles.route },
                        onArticleClick = { detailLauncher.launch(ArticleDetailActivity.createIntent(context, it)) }
                    )
                    Screen.Articles.route -> ArticlesScreen(
                        onArticleClick = { detailLauncher.launch(ArticleDetailActivity.createIntent(context, it)) },
                        onCreateArticle = { editorLauncher.launch(ArticleEditorActivity.createIntent(context)) },
                        onManageCategories = {
                            categoryManagerLauncher.launch(CategoryManageActivity.createIntent(context))
                        },
                        isOverlayOpen = hasOverlay,
                        accountSessionKey = authSessionVersion,
                        refreshToken = contentVersion
                    )
                    Screen.Diverge.route -> DivergeScreen()
                    Screen.Profile.route -> ProfileScreen(onLogout = onLogout)
                }
            }
        }

        if (!hasOverlay) {
            if (useNavigationRail) {
                TabletNavigationRail(
                    items = items,
                    currentRoute = currentRoute,
                    onRouteSelected = { currentRoute = it },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                        .systemBarsPadding()
                )
                return@BoxWithConstraints
            }

            LiquidBottomTabs(
                selectedTabIndex = items.indexOfFirst { it.route == currentRoute },
                onTabSelected = { index -> currentRoute = items[index].route },
                backdrop = backdrop,
                tabsCount = items.size,
                accentColor = primaryColor,
                containerColor = cardBg.copy(alpha = .40f),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 12.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
            ) {
                items.forEach { screen ->
                    val isActive = currentRoute == screen.route
                    val iconTint by animateColorAsState(
                        targetValue = if (isActive) primaryColor else contentVariantColor,
                        animationSpec = spring(stiffness = 300f),
                        label = "icon_tint"
                    )
                    LiquidBottomTab(
                        onClick = {
                            if (currentRoute != screen.route) currentRoute = screen.route
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = screen.iconRes),
                            contentDescription = screen.title,
                            modifier = Modifier.size(22.dp),
                            tint = iconTint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletNavigationRail(
    items: List<Screen>,
    currentRoute: String,
    onRouteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(76.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(AppColors.cardBackground.copy(alpha = 0.82f))
            .glasenseHighlight(RoundedCornerShape(30.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { screen ->
            val selected = currentRoute == screen.route
            val tint by animateColorAsState(
                targetValue = if (selected) AppColors.onPrimary else AppColors.contentVariant,
                animationSpec = spring(stiffness = 300f),
                label = "rail_icon_tint"
            )
            val backgroundColor by animateColorAsState(
                targetValue = if (selected) AppColors.primary else Color.Transparent,
                animationSpec = spring(stiffness = 300f),
                label = "rail_item_background"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRouteSelected(screen.route) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(screen.iconRes),
                        contentDescription = screen.title,
                        modifier = Modifier.size(22.dp),
                        tint = tint
                    )
                }
                Text(
                    text = screen.title,
                    color = if (selected) AppColors.primary else AppColors.contentVariant,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
