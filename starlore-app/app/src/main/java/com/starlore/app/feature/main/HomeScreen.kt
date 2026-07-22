package com.starlore.app.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberOverscrollEffect
import com.starlore.app.R
import com.starlore.app.data.api.ArticleSummary
import com.starlore.app.feature.article.ArticlesUiState
import com.starlore.app.feature.article.ArticlesViewModel
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.AppPageColor
import com.starlore.app.theme.appPageBackground
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import java.time.LocalTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    accountSessionKey: Int,
    refreshToken: Int,
    onAskAi: () -> Unit,
    onCreateArticle: () -> Unit,
    onOpenArticles: () -> Unit,
    onArticleClick: (Int) -> Unit,
    viewModel: ArticlesViewModel = koinViewModel(key = "articles-$accountSessionKey")
) {
    val articlesState by remember { viewModel.articlesState }
    var searchQuery by remember { mutableStateOf("") }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(refreshToken) {
        if (refreshToken > 0) viewModel.refresh()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().appPageBackground(),
        overscrollEffect = rememberOverscrollEffect(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = topInset + 18.dp,
            bottom = 108.dp
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = greeting(),
                    color = AppColors.contentVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "今天想探索什么？",
                    color = AppColors.content,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                placeholder = { Text("搜索你的星记", color = AppColors.contentVariant, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_magnifying_glass),
                        contentDescription = null,
                        tint = AppColors.contentVariant,
                        modifier = Modifier.size(19.dp)
                    )
                },
                shape = CircleShape,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.search(searchQuery.trim())
                    onOpenArticles()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppColors.cardBackground.copy(alpha = .72f),
                    unfocusedContainerColor = AppColors.cardBackground.copy(alpha = .52f),
                    focusedBorderColor = AppColors.primary,
                    unfocusedBorderColor = AppColors.scrimMedium,
                    cursorColor = AppColors.primary
                )
            )
        }

        item {
            AiEntry(onClick = onAskAi)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_square_and_pencil,
                    label = "写一篇星记",
                    hint = "记录正在发生的想法",
                    onClick = onCreateArticle
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_nav_document_filled,
                    label = "全部星记",
                    hint = "浏览与整理知识",
                    onClick = onOpenArticles
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("最近星记", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("继续上次的阅读", color = AppColors.contentVariant, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenArticles)
                        .background(AppColors.cardBackground.copy(alpha = .5f))
                        .padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("全部", color = AppColors.contentVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Icon(
                        painterResource(R.drawable.ic_chevron_forward_compact),
                        null,
                        Modifier.size(14.dp),
                        AppColors.contentVariant
                    )
                }
            }
        }

        when (val state = articlesState) {
            is ArticlesUiState.Success -> {
                val recent = state.articles.take(3)
                if (recent.isEmpty()) {
                    item { EmptyRecent(onCreateArticle) }
                } else {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(AppColors.cardBackground.copy(alpha = .58f))
                                .glasenseHighlight(RoundedCornerShape(24.dp))
                        ) {
                            recent.forEachIndexed { index, article ->
                                RecentArticle(article, onArticleClick)
                                if (index != recent.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 18.dp),
                                        color = AppColors.scrimNormal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is ArticlesUiState.Error -> item {
                Text("最近内容暂时未能加载", color = AppColors.contentVariant, fontSize = 13.sp)
            }
            ArticlesUiState.Loading -> item {
                Box(
                    Modifier.fillMaxWidth().height(128.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AppColors.cardBackground.copy(alpha = .38f))
                )
            }
        }
        item(key = "home-scroll-breathing-room") {
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun AiEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AppColors.cardBackground.copy(alpha = .58f))
            .glasenseHighlight(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(AppColors.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_sparkle_filled),
                contentDescription = null,
                tint = AppColors.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("问问 Starlore AI", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("梳理灵感、解读文章，或继续一次对话", color = AppColors.contentVariant, fontSize = 12.sp)
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_forward_compact),
            contentDescription = null,
            tint = AppColors.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: Int,
    label: String,
    hint: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(22.dp))
            .background(AppColors.cardBackground.copy(alpha = .54f))
            .glasenseHighlight(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(painterResource(icon), null, Modifier.size(22.dp), tint = AppColors.primary)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(hint, color = AppColors.contentVariant, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
private fun RecentArticle(article: ArticleSummary, onClick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(article.id) }.padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(article.category ?: "未分类", color = AppColors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(article.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            article.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = AppColors.contentVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(article.createdAt?.take(10)?.substring(5) ?: "", color = AppColors.contentVariant, fontSize = 11.sp)
    }
}

@Composable
private fun EmptyRecent(onCreateArticle: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(AppColors.cardBackground.copy(alpha = .5f)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("这里会出现最近的星记", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("先写下第一篇，让首页成为继续思考的起点", color = AppColors.contentVariant, fontSize = 12.sp)
        Text(
            "开始写作",
            color = AppColors.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onCreateArticle).padding(12.dp)
        )
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}
