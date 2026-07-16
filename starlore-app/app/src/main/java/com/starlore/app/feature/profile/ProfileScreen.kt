package com.starlore.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.feature.settings.SettingsActivity
import com.starlore.app.feature.settings.SettingsDestination
import com.starlore.app.feature.settings.util.SettingsManager
import com.starlore.app.feature.settings.util.SettingsViewModel
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.AppSpecs
import com.starlore.app.theme.AppButtonColors
import com.starlore.app.theme.harmonize
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.ui.components.packed.ConfigContainer
import com.starlore.app.ui.components.packed.ConfigEntryItem
import com.starlore.app.ui.components.packed.AboutEntryItem
import com.starlore.glasense.core.component.Text
import com.starlore.glasense.theme.tokens.Blue500
import com.starlore.glasense.theme.tokens.Pink400
import com.starlore.glasense.theme.tokens.Purple500
import com.starlore.glasense.theme.tokens.Slate500
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val nickname = remember { SettingsManager.userNickname }
    val username = remember { SettingsManager.userUsername }
    val role = remember { SettingsManager.userRole }
    val avatar = remember { SettingsManager.userAvatar }

    val fullAvatarUrl = remember(avatar) {
        if (avatar.isEmpty()) {
            ""
        } else if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
            avatar
        } else {
            val baseUrl = SettingsManager.backendBaseUrl.trim()
            val rootUrl = if (baseUrl.contains("/api")) {
                baseUrl.substringBefore("/api")
            } else {
                baseUrl.removeSuffix("/")
            }
            if (avatar.startsWith("/")) {
                "$rootUrl$avatar"
            } else {
                "$rootUrl/$avatar"
            }
        }
    }

    val context = LocalContext.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isSmallTitleVisible = scrollState.value > 100

    val backgroundColor = AppColors.pageBackground
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    val hierarchicalSurfaceColor = AppColors.cardBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(statusBarHeight + 16.dp))

            // User Profile Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppSpecs.cardShape)
                    .background(AppColors.cardBackground.copy(alpha = 0.5f))
                    .glasenseHighlight(AppSpecs.cardShape)
                    .clickable {
                        context.startActivity(
                            SettingsActivity.createIntent(context, SettingsDestination.ACCOUNT)
                        )
                    }
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AppColors.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        var isError by remember { mutableStateOf(false) }
                        if (fullAvatarUrl.isNotEmpty() && !isError) {
                            AsyncImage(
                                model = fullAvatarUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onState = { state ->
                                    if (state is coil3.compose.AsyncImagePainter.State.Error) {
                                        android.util.Log.e("ProfileScreen", "Avatar load failed: $fullAvatarUrl", state.result.throwable)
                                        isError = true
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = nickname.firstOrNull()?.toString()?.uppercase() ?: "U",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.primary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = nickname.ifEmpty { "Starlore User" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .background(AppColors.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = role.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.primary
                                )
                            }
                        }
                        Text(
                            text = "@$username",
                            fontSize = 13.sp,
                            color = AppColors.contentVariant
                        )
                    }

                    Icon(
                        painter = painterResource(com.starlore.app.R.drawable.ic_chevron_forward_compact),
                        contentDescription = "账户管理",
                        tint = AppColors.contentVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // AI Section
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor.copy(alpha = 0.5f)) {
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
                        icon = painterResource(com.starlore.app.R.drawable.ic_twotone_sparkles),
                        title = "AI 智能助手",
                        enableGlow = true,
                        onClick = {
                            context.startActivity(
                                SettingsActivity.createIntent(context, SettingsDestination.AI)
                            )
                        }
                    )
                }
            }

            // Main Settings Group
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor.copy(alpha = 0.5f)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ConfigEntryItem(
                        color = harmonize(Blue500),
                        icon = painterResource(com.starlore.app.R.drawable.ic_twotone_image),
                        title = "外观",
                        onClick = {
                            context.startActivity(
                                SettingsActivity.createIntent(context, SettingsDestination.APPEARANCE)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ConfigEntryItem(
                        color = harmonize(Slate500),
                        icon = painterResource(com.starlore.app.R.drawable.ic_twotone_gear),
                        title = "通用设置",
                        onClick = {
                            context.startActivity(
                                SettingsActivity.createIntent(context, SettingsDestination.GENERAL)
                            )
                        }
                    )
                }
            }

            // About Section
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor.copy(alpha = 0.5f)) {
                AboutEntryItem(
                    icon = painterResource(com.starlore.app.R.drawable.app_icon_default),
                    onClick = {
                        context.startActivity(
                            SettingsActivity.createIntent(context, SettingsDestination.ABOUT)
                        )
                    }
                )
            }

            // Log out button (Premium Red Glass Capsule)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.error.copy(alpha = 0.12f))
                    .glasenseHighlight(RoundedCornerShape(24.dp))
                    .clickable {
                        SettingsManager.clearAccountSession()
                        onLogout()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "退出登录",
                    color = AppColors.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Collapsing Header Small Title Bar
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = "个人中心",
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = backgroundColor
        ) {}
    }
}
