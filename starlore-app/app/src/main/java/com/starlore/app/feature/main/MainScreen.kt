package com.starlore.app.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.R
import com.starlore.app.feature.article.ArticleDetailScreen
import com.starlore.app.feature.article.ArticlesScreen
import com.starlore.app.feature.article.ArticleEditorScreen
import com.starlore.app.feature.article.CategoryManageScreen
import com.starlore.app.feature.article.CategoryArticlesScreen
import com.starlore.app.feature.auth.LoginScreen
import com.starlore.app.feature.chat.EchobotScreen
import com.starlore.app.feature.diverge.DivergeScreen
import com.starlore.app.feature.profile.ProfileScreen
import com.starlore.app.feature.settings.util.SettingsManager
import com.starlore.app.feature.settings.update.UpdateBottomSheet
import com.starlore.app.feature.settings.update.UpdateCheckResult
import com.starlore.app.feature.settings.update.UpdateChecker
import com.starlore.app.feature.settings.update.UpdateInfo
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.LocalGlasenseSettings
import com.starlore.app.ui.components.glasense.GlasenseNavigationButton
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.ui.components.glasense.material.MaterialRecipes
import com.starlore.app.ui.components.glasense.material.rememberMaterialRenderEffectOrNull
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
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text

sealed class Screen(val route: String, val title: String, val iconRes: Int) {
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
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Articles.route) }
    var selectedArticleId by remember { mutableStateOf<Int?>(null) }
    var editingArticleId by remember { mutableStateOf<Int?>(null) }
    var isCreatingArticle by remember { mutableStateOf(false) }
    var isManagingCategories by remember { mutableStateOf(false) }
    var openedCategoryName by remember { mutableStateOf<String?>(null) }
    var contentVersion by remember { mutableIntStateOf(0) }
    val hasOverlay = selectedArticleId != null || editingArticleId != null || isCreatingArticle || isManagingCategories || openedCategoryName != null

    BackHandler(enabled = hasOverlay) {
        when {
            editingArticleId != null -> editingArticleId = null
            isCreatingArticle -> isCreatingArticle = false
            openedCategoryName != null -> { openedCategoryName = null; isManagingCategories = true }
            isManagingCategories -> isManagingCategories = false
            else -> selectedArticleId = null
        }
    }

    val items = listOf(
        Screen.Articles,
        Screen.Chat,
        Screen.Diverge,
        Screen.Profile
    )

    val cardBg = AppColors.cardBackground
    val primaryColor = AppColors.primary
    val onPrimaryColor = AppColors.onPrimary
    val contentVariantColor = AppColors.contentVariant

    val surfaceColor = AppColors.background
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = surfaceColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        // Active Content View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            when (currentRoute) {
                Screen.Articles.route -> ArticlesScreen(
                    onArticleClick = { selectedArticleId = it },
                    onCreateArticle = { isCreatingArticle = true },
                    onManageCategories = { isManagingCategories = true },
                    isOverlayOpen = hasOverlay,
                    accountSessionKey = authSessionVersion + contentVersion
                )
                Screen.Chat.route -> EchobotScreen(accountSessionKey = authSessionVersion)
                Screen.Diverge.route -> DivergeScreen()
                Screen.Profile.route -> ProfileScreen(onLogout = onLogout)
            }
        }

        // Overlay Article Details if selected
        selectedArticleId?.let { articleId ->
            ArticleDetailScreen(
                articleId = articleId,
                backdrop = backdrop,
                onBack = { selectedArticleId = null },
                onEdit = {
                    selectedArticleId = null
                    editingArticleId = it
                }
            )
        }

        if (isCreatingArticle || editingArticleId != null) {
            ArticleEditorScreen(
                articleId = editingArticleId,
                onBack = { isCreatingArticle = false; editingArticleId = null },
                onSaved = {
                    isCreatingArticle = false
                    editingArticleId = null
                    contentVersion++
                    selectedArticleId = it
                }
            )
        }

        if (isManagingCategories) {
            CategoryManageScreen(onBack = {
                isManagingCategories = false
                contentVersion++
            }, onOpenCategory = { categoryName ->
                isManagingCategories = false
                openedCategoryName = categoryName
            })
        }

        openedCategoryName?.let { categoryName ->
            CategoryArticlesScreen(
                categoryName = categoryName,
                onBack = {
                    openedCategoryName = null
                    isManagingCategories = true
                },
                onArticleClick = { selectedArticleId = it }
            )
        }

        if (!hasOverlay) {
            // Unified Floating bottom navigation bar
            val liquidGlass = LocalGlasenseSettings.current.liquidGlass
            val materialEffect = rememberMaterialRenderEffectOrNull(MaterialRecipes.thin())
            val bottomNavBackdrop = materialEffect?.let { renderEffect ->
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    shadow = {
                        Shadow(
                            radius = 24.dp,
                            color = Color.Black.copy(alpha = 0.08f),
                            offset = DpOffset(0.dp, 8.dp)
                        )
                    },
                    innerShadow = null,
                    highlight = {
                        Highlight.Default.copy(
                            style = HighlightStyle.Default(
                                angle = 90f
                            )
                        )
                    },
                    effects = {
                        padding = 8f.dp.toPx() * 2
                        if (!liquidGlass) {
                            effect(renderEffect)
                        }
                        blur(if (liquidGlass) 8f.dp.toPx() else 16f.dp.toPx(), TileMode.Clamp)
                        if (liquidGlass) {
                            lens(16f.dp.toPx(), 64f.dp.toPx())
                        }
                    },
                    onDrawSurface = {
                        drawRect(cardBg.copy(alpha = if (liquidGlass) 0.15f else 0.25f))
                    }
                )
            } ?: Modifier
                .clip(Capsule())
                .background(cardBg.copy(alpha = 0.8f))
                .glasenseHighlight(Capsule())


        BoxWithConstraints(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .then(bottomNavBackdrop)
                .align(Alignment.BottomCenter)
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / items.size
            val selectedIndex = items.indexOfFirst { it.route == currentRoute }

            val indicatorWidth = 80.dp
            val indicatorHeight = 48.dp

            val targetOffset = (tabWidth * selectedIndex) + (tabWidth - indicatorWidth) / 2
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f
                ),
                label = "indicator_offset"
            )

            val liquidGlass = LocalGlasenseSettings.current.liquidGlass

            // 1. Sliding active indicator in the background layer
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .align(Alignment.CenterStart)
            ) {
                GlasenseNavigationButton(
                    modifier = Modifier
                        .size(width = indicatorWidth, height = indicatorHeight),
                    isActive = true,
                    onClick = {},
                    backdrop = backdrop,
                    liquidGlass = liquidGlass
                ) {}
            }

            // 2. Foreground navigation items Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val isActive = currentRoute == screen.route

                    val iconTint by animateColorAsState(
                        targetValue = if (isActive) onPrimaryColor else contentVariantColor,
                        animationSpec = spring(stiffness = 300f),
                        label = "icon_tint"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (currentRoute != screen.route) {
                                    currentRoute = screen.route
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = screen.iconRes),
                            contentDescription = screen.title,
                            modifier = Modifier.size(24.dp),
                            tint = iconTint
                        )
                    }
                }
            }
        }
        }
    }
}
