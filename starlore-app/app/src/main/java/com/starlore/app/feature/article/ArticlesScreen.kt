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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.theme.AppSpecs
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.isScrolledPast
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.data.api.ArticleSummary
import com.starlore.app.data.api.CategoryItem
import com.starlore.app.theme.AppColors
import com.starlore.glasense.core.component.Text
import com.starlore.glasense.core.component.VGap
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesScreen(
    onArticleClick: (Int) -> Unit,
    isOverlayOpen: Boolean = false,
    accountSessionKey: Int = 0,
    viewModel: ArticlesViewModel = koinViewModel(key = "articles-$accountSessionKey")
) {
    val articlesState by remember { viewModel.articlesState }
    val categoriesState by remember { viewModel.categoriesState }
    val selectedCategory by remember { viewModel.selectedCategory }

    val lazyListState = rememberLazyListState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isSmallTitleState by lazyListState.isScrolledPast(statusBarHeight + 24.dp)
    val isSmallTitleVisible = isSmallTitleState && !isOverlayOpen

    val backgroundColor = AppColors.pageBackground
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
            .background(backgroundColor)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
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

        // 5. Collapsing Header Small Title Bar
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = "星记空间",
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = backgroundColor
        ) {}
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppSpecs.cardShape)
            .background(AppColors.cardBackground.copy(alpha = 0.5f))
            .clickable { onClick() }
            .glasenseHighlight(AppSpecs.cardShape)
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
                // Views
                Text(
                    text = "👁 ${article.viewCount ?: 0}",
                    fontSize = 11.sp,
                    color = AppColors.contentVariant
                )
                
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
