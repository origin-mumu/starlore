package com.starlore.app.feature.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.Backdrop
import com.starlore.app.theme.AppSpecs
import com.starlore.app.R
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.isScrolledPast
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.data.api.ArticleSummary
import com.starlore.app.data.api.CategoryItem
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.AppPageColor
import com.starlore.app.theme.appPageBackground
import com.starlore.app.ui.components.liquid.liquidGlass
import com.starlore.glasense.core.component.Text
import com.starlore.glasense.core.component.VGap
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesScreen(
    onArticleClick: (Int) -> Unit,
    onCreateArticle: () -> Unit,
    onManageCategories: () -> Unit,
    isOverlayOpen: Boolean = false,
    accountSessionKey: Int = 0,
    refreshToken: Int = 0,
    viewModel: ArticlesViewModel = koinViewModel(key = "articles-$accountSessionKey")
) {
    val articlesState by remember { viewModel.articlesState }
    val categoriesState by remember { viewModel.categoriesState }
    val selectedCategory by remember { viewModel.selectedCategory }
    val isRefreshing by remember { viewModel.isRefreshing }
    val pullToRefreshState = rememberPullToRefreshState()

    val lazyListState = rememberLazyListState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(refreshToken) {
        if (refreshToken > 0) viewModel.refresh()
    }
    val isSmallTitleState by lazyListState.isScrolledPast(statusBarHeight + 24.dp)
    val isSmallTitleVisible = isSmallTitleState && !isOverlayOpen

    val backgroundColor = AppPageColor
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                StarRefreshIndicator(
                    progress = pullToRefreshState.distanceFraction,
                    refreshing = isRefreshing,
                    topOffset = statusBarHeight + 178.dp
                )
            }
        ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
            contentPadding = PaddingValues(
                top = statusBarHeight + 12.dp,
                bottom = 100.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Large Page Title
            item {
                Text(
                    text = "星记空间",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 2. Search Bar
            item {
                OutlinedTextField(
                    value = viewModel.searchQuery.value,
                    onValueChange = { viewModel.search(it) },
                    placeholder = { androidx.compose.material3.Text("搜索文章...", color = AppColors.contentVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.cardBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = AppColors.cardBackground.copy(alpha = 0.3f),
                        focusedBorderColor = AppColors.primary,
                        unfocusedBorderColor = AppColors.scrimMedium,
                        cursorColor = AppColors.primary
                    ),
                    singleLine = true
                )
            }

            // 3. Categories selector
            item {
                when (val catState = categoriesState) {
                    is CategoriesUiState.Loading -> {
                        if (articlesState !is ArticlesUiState.Loading) {
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.primary)
                            }
                        }
                    }
                    is CategoriesUiState.Success -> {
                        CategorySelectorList(
                            categories = catState.categories,
                            selectedCategory = selectedCategory,
                            onCategorySelect = { viewModel.selectCategory(it) }
                        )
                    }
                    is CategoriesUiState.Error -> {}
                }
            }

            // 4. Articles Feed list
            when (val artState = articlesState) {
                is ArticlesUiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppColors.primary)
                        }
                    }
                }
                is ArticlesUiState.Success -> {
                    val articles = artState.articles
                    if (articles.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("空空如也，没有任何文章", color = AppColors.contentVariant, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(articles) { article ->
                            ArticleFeedCard(article = article, onClick = { onArticleClick(article.id) })
                        }
                    }
                }
                is ArticlesUiState.Error -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("加载失败: ${artState.message}", color = AppColors.error, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { viewModel.loadData() }) {
                                    androidx.compose.material3.Text("重试", color = AppColors.onPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // 5. Collapsing Header Small Title Bar
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = "星记空间",
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = backgroundColor
        ) {}

        Row(
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArticleActionButton(R.drawable.ic_folder, "管理分类", onManageCategories)
            ArticleActionButton(R.drawable.ic_add, "新建星记", onCreateArticle)
        }
    }
}

@Composable
private fun BoxScope.StarRefreshIndicator(
    progress: Float,
    refreshing: Boolean,
    topOffset: androidx.compose.ui.unit.Dp
) {
    val visible = refreshing || progress > 0f
    if (!visible) return
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        Modifier.align(Alignment.TopCenter)
            .padding(top = topOffset)
            .graphicsLayer {
                alpha = if (refreshing) 1f else clampedProgress
                val scale = 0.82f + 0.18f * clampedProgress
                scaleX = scale
                scaleY = scale
            }
            .size(42.dp)
            .clip(CircleShape)
            .background(AppColors.cardBackground.copy(alpha = .96f))
            .glasenseHighlight(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { if (refreshing) 0.35f else clampedProgress },
            modifier = Modifier.size(19.dp),
            color = AppColors.primary,
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun ArticleActionButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape)
            .background(AppColors.cardBackground.copy(alpha = .8f))
            .glasenseHighlight(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        com.starlore.glasense.core.component.Icon(
            painter = androidx.compose.ui.res.painterResource(icon), contentDescription = description,
            modifier = Modifier.size(20.dp), tint = AppColors.primary
        )
    }
}

@Composable
fun CategorySelectorList(
    categories: List<CategoryItem>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            CategoryChip(
                name = "全部",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelect(null) }
            )
        }
        items(categories) { category ->
            CategoryChip(
                name = category.name,
                isSelected = selectedCategory == category.name,
                onClick = { onCategorySelect(category.name) }
            )
        }
    }
}

@Composable
fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) AppColors.primary else AppColors.cardBackground.copy(alpha = 0.5f))
            .clickable { onClick() }
            .then(
                if (!isSelected) Modifier.glasenseHighlight(CircleShape) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) AppColors.onPrimary else AppColors.content,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ArticleFeedCard(
    article: ArticleSummary,
    backdrop: Backdrop? = null,
    onClick: () -> Unit
) {
    val cardMaterial = if (backdrop != null) {
        Modifier.liquidGlass(
            backdrop = backdrop,
            cornerRadius = AppSpecs.cardCorner,
            surfaceColor = AppColors.cardBackground.copy(alpha = .2f),
            blurRadius = 8.dp,
            lensRadius = 18.dp
        )
    } else {
        Modifier.background(AppColors.cardBackground.copy(alpha = 0.5f)).glasenseHighlight(AppSpecs.cardShape)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppSpecs.cardShape)
            .then(cardMaterial)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .background(AppColors.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = article.category ?: "默认星域",
                        fontSize = 11.sp,
                        color = AppColors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Date
                Text(
                    text = article.createdAt?.take(10) ?: "",
                    fontSize = 11.sp,
                    color = AppColors.contentVariant
                )
            }

            // Title
            Text(
                text = article.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Snippet
            article.description?.let { desc ->
                if (desc.isNotEmpty()) {
                    Text(
                        text = desc,
                        fontSize = 13.sp,
                        color = AppColors.contentVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tags
                article.tags.take(3).forEach { tag ->
                    Text(
                        text = "#$tag",
                        fontSize = 11.sp,
                        color = AppColors.primary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
