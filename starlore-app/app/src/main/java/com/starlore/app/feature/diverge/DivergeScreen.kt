package com.starlore.app.feature.diverge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.theme.AppSpecs
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.R
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.AppPageColor
import com.starlore.app.theme.appPageBackground
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivergeScreen(
    viewModel: DivergeViewModel = koinViewModel()
) {
    val uiState by remember { viewModel.uiState }
    val coreWord by remember { viewModel.coreWord }
    val pairsList = viewModel.pairsList

    val scrollState = rememberScrollState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isSmallTitleVisible = scrollState.value > 100

    val backgroundColor = AppPageColor
    val panelShape = RoundedCornerShape(24.dp)
    val nodeShape = RoundedCornerShape(20.dp)
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .appPageBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(statusBarHeight + 12.dp))

            // Title
            Text(
                text = "创意发散",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.primary
            )

            Text(
                text = "围绕核心主题向外无限发散你的创意星海...",
                fontSize = 13.sp,
                color = AppColors.contentVariant
            )

            // Keyword prompt input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(panelShape)
                    .background(AppColors.cardBackground.copy(alpha = 0.5f))
                    .glasenseHighlight(panelShape)
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.wordInput.value,
                        onValueChange = { viewModel.wordInput.value = it },
                        placeholder = { androidx.compose.material3.Text("输入核心概念 (如: 宇宙)", color = AppColors.contentVariant) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primary,
                            unfocusedBorderColor = AppColors.scrimMedium,
                            cursorColor = AppColors.primary
                        ),
                        singleLine = true,
                        enabled = uiState !is DivergeUiState.Loading
                    )

                    IconButton(
                        onClick = { viewModel.startDivergence() },
                        enabled = uiState !is DivergeUiState.Loading && viewModel.wordInput.value.trim().isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (viewModel.wordInput.value.trim().isNotEmpty() && uiState !is DivergeUiState.Loading) AppColors.primary else AppColors.scrimNormal)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_wand_and_rays),
                            contentDescription = "Brainstorm",
                            tint = if (viewModel.wordInput.value.trim().isNotEmpty() && uiState !is DivergeUiState.Loading) AppColors.onPrimary else AppColors.contentVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Result content section
            when (val state = uiState) {
                is DivergeUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppColors.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "正在构思围绕「${coreWord}」的星汉联想...",
                                fontSize = 13.sp,
                                color = AppColors.contentVariant
                            )
                        }
                    }
                }
                is DivergeUiState.Success -> {
                    AnimatedVisibility(
                        visible = pairsList.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Central Core Concept
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(AppColors.primary)
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🪐 $coreWord",
                                    color = AppColors.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Branching grid of 8 child nodes
                            pairsList.chunked(2).forEach { rowPairs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowPairs.forEach { pair ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(nodeShape)
                                                .background(AppColors.cardBackground.copy(alpha = 0.5f))
                                                .glasenseHighlight(nodeShape)
                                                .clickable {
                                                    viewModel.wordInput.value = pair.zh
                                                    viewModel.startDivergence()
                                                }
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = pair.zh,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = pair.en,
                                                    fontSize = 10.sp,
                                                    color = AppColors.contentVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "💡 提示: 点击任何联想节点，将以此节点为中心开启二次创意发散之旅！",
                                fontSize = 10.sp,
                                color = AppColors.contentVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
                is DivergeUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "创意受阻: ${state.message}",
                            color = AppColors.error,
                            fontSize = 14.sp
                        )
                    }
                }
                is DivergeUiState.Idle -> {
                    // Initial prompt card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(panelShape)
                            .background(AppColors.cardBackground.copy(alpha = 0.3f))
                            .glasenseHighlight(panelShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无发散，在上方输入词语以联想生成星罗网格...",
                            color = AppColors.contentVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Collapsing Header Small Title Bar
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = "创意发散",
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = backgroundColor
        ) {}
    }
}
