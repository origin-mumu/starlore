package com.starlore.app.feature.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.shapes.Capsule
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.starlore.app.R
import com.starlore.app.data.api.CategoryItem
import com.starlore.app.data.api.CreateArticleRequest
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.AppPageColor
import com.starlore.app.theme.appPageBackground
import com.starlore.app.theme.AppButtonColors
import com.starlore.app.theme.AppSpecs
import com.starlore.app.ui.components.glasense.GlasenseButtonAlt
import com.starlore.app.ui.components.glasense.GlasenseButtonColors
import com.starlore.app.ui.components.glasense.GlasenseSwitch
import com.starlore.app.ui.components.glasense.GlasenseSwipeable
import com.starlore.app.ui.components.glasense.SwipeableActionButton
import com.starlore.app.ui.components.glasense.rememberSwipeableListState
import com.starlore.app.ui.components.glasense.GlasenseNavigationButton
import com.starlore.app.ui.components.glasense.GlasenseBackButton
import com.starlore.app.ui.components.glasense.GlasenseDialog
import com.starlore.app.ui.components.glasense.GlasenseDynamicSmallTitle
import com.starlore.app.ui.components.glasense.isScrolledPast
import com.starlore.app.ui.components.glasense.DialogItemData
import com.starlore.app.ui.components.glasense.DialogState
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.ui.components.appScrollBreathingRoom
import com.starlore.app.ui.components.packed.ConfigTextField
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import org.koin.androidx.compose.koinViewModel
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ArticleEditorScreen(
    articleId: Int?,
    onBack: () -> Unit,
    onSaved: (Int) -> Unit,
    viewModel: ArticleManageViewModel = koinViewModel()
) {
    val state by remember { viewModel.editorState }
    val categories by remember { viewModel.categories }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var isPublic by rememberSaveable { mutableStateOf(true) }
    var initialized by remember { mutableStateOf(false) }
    var showPublishSettings by remember { mutableStateOf(false) }
    val editorController = rememberMarkdownEditorController()

    LaunchedEffect(articleId) { initialized = false; viewModel.loadEditor(articleId) }
    LaunchedEffect(state) {
        when (val current = state) {
            is EditorUiState.Ready -> if (!initialized) {
                current.article?.let {
                    title = it.title; description = it.description.orEmpty(); content = it.content
                    category = it.category.orEmpty(); tags = it.tags.joinToString(", "); isPublic = it.isPublic != false
                }
                initialized = true
            }
            is EditorUiState.Saved -> onSaved(current.article.id)
            else -> Unit
        }
    }

    val saving = state is EditorUiState.Saving
    val canPublish = title.isNotBlank() && content.isNotBlank() && !saving
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val publish = {
        viewModel.saveArticle(articleId, CreateArticleRequest(
            title = title.trim(), content = content, description = description.trim().ifBlank { null },
            category = category.ifBlank { null },
            tags = tags.split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }, isPublic = isPublic
        ))
    }

    Box(Modifier.fillMaxSize().appPageBackground().imePadding()) {
        if (state is EditorUiState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.primary) }
        } else {
            Column(
                Modifier.fillMaxSize().padding(top = statusBarHeight + 68.dp, bottom = 76.dp)
            ) {
                ConfigTextField(
                    value = title, onValueChange = { title = it }, singleLine = true,
                    backgroundColor = Color.Transparent, decorateText = "请输入标题",
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp)
                )
                ThinDivider()
                EditorMetaBar(
                    category = category,
                    isPublic = isPublic,
                    tagCount = tags.split(',', '，').count { it.isNotBlank() },
                    onClick = { showPublishSettings = true }
                )
                ThinDivider()
                if (initialized) MarkdownEditor(
                    initialMarkdown = content,
                    onMarkdownChange = { content = it },
                    controller = editorController,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }

        WritingTopBar(
            title = if (articleId == null) "写星记" else "编辑星记",
            canPublish = canPublish, saving = saving, onClose = onBack,
            onPublish = publish, modifier = Modifier.align(Alignment.TopCenter)
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = AppColors.primary.copy(alpha = .10f),
                    spotColor = AppColors.primary.copy(alpha = .14f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(AppColors.cardBackground.copy(alpha = .96f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MarkdownToolbar(editorController, Modifier.fillMaxWidth())
        }
        (state as? EditorUiState.Error)?.let { error ->
            Text(error.message, color = AppColors.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarHeight + 70.dp))
        }
    }

    if (showPublishSettings) PublishSettingsSheet(
        categories = categories, description = description, onDescriptionChange = { description = it },
        category = category, onCategoryChange = { category = it }, tags = tags, onTagsChange = { tags = it },
        isPublic = isPublic, onPublicChange = { isPublic = it },
        onDismiss = { showPublishSettings = false }
    )
}

@Composable
private fun EditorMetaBar(category: String, isPublic: Boolean, tagCount: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.cardBackground.copy(alpha = .72f))
            .clickable(onClick = onClick)
            .glasenseHighlight(RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(16.dp), AppColors.primary)
            Text(category.ifBlank { "未分类" }, fontSize = 12.sp, color = AppColors.contentVariant)
        }
        Text(if (isPublic) "公开" else "私密", fontSize = 12.sp, color = AppColors.contentVariant)
        if (tagCount > 0) Text("$tagCount 个标签", fontSize = 12.sp, color = AppColors.contentVariant)
        Spacer(Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_sliders), "发布设置", Modifier.size(16.dp), AppColors.contentVariant)
        Icon(painterResource(R.drawable.ic_chevron_forward_compact), null, Modifier.size(15.dp), AppColors.contentVariant)
    }
}

@Composable
private fun WritingTopBar(title: String, canPublish: Boolean, saving: Boolean, onClose: () -> Unit, onPublish: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().statusBarsPadding().height(68.dp)) {
        Box(Modifier.align(Alignment.CenterStart).padding(start = 12.dp)) {
            GlasenseNavigationButton(
                modifier = Modifier.size(48.dp),
                isActive = false,
                onClick = onClose
            ) {
                Icon(painterResource(R.drawable.ic_cross), "关闭", Modifier.size(20.dp), AppColors.primary)
            }
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .clip(Capsule())
                .background(AppColors.cardBackground.copy(alpha = .72f))
                .glasenseHighlight(Capsule())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).width(72.dp).height(44.dp)
                .clip(Capsule())
                .background(if (canPublish) AppColors.primary else AppColors.elevatedCardBackground)
                .clickable(enabled = canPublish) { onPublish() }
                .glasenseHighlight(Capsule()),
            contentAlignment = Alignment.Center
        ) {
            Text(if (saving) "保存中" else "发布", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (canPublish) AppColors.onPrimary else AppColors.contentVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishSettingsSheet(
    categories: List<CategoryItem>, description: String, onDescriptionChange: (String) -> Unit,
    category: String, onCategoryChange: (String) -> Unit, tags: String, onTagsChange: (String) -> Unit,
    isPublic: Boolean, onPublicChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var categoriesExpanded by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.elevatedPageBackground, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState())
                .imePadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("发布设置", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            ConfigTextField(
                title = "摘要", value = description, onValueChange = onDescriptionChange,
                minLines = 2, maxLines = 3, backgroundColor = AppColors.elevatedCardBackground,
                decorateText = "简要介绍这篇星记", modifier = Modifier.height(104.dp)
            )
            Column {
                PropertyRow(R.drawable.ic_folder, "星域", category.ifBlank { "未分类" }) { categoriesExpanded = !categoriesExpanded }
                if (categoriesExpanded) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryOption("未分类", category.isBlank()) { onCategoryChange(""); categoriesExpanded = false }
                        categories.forEach { item ->
                            CategoryOption(item.name, category == item.name) { onCategoryChange(item.name); categoriesExpanded = false }
                        }
                    }
                }
            }
            ConfigTextField(
                title = "标签", value = tags, onValueChange = onTagsChange, singleLine = true,
                backgroundColor = AppColors.elevatedCardBackground, decorateText = "多个标签用逗号分隔",
                modifier = Modifier.height(76.dp)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("公开发布", fontWeight = FontWeight.Bold); Text(if (isPublic) "所有人都可以阅读" else "仅自己可见", fontSize = 12.sp, color = AppColors.contentVariant) }
                GlasenseSwitch(
                    backgroundColor = AppColors.elevatedPageBackground,
                    checked = isPublic,
                    colors = AppColors,
                    onCheckedChange = onPublicChange
                )
            }
            Text("设置会自动保留，完成后点击右上角发布", fontSize = 11.sp, color = AppColors.contentVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
    }
}

@Composable
private fun CategoryOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) AppColors.primary else AppColors.elevatedCardBackground)
            .clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) AppColors.onPrimary else AppColors.content)
    }
}

@Composable
fun CategoryManageScreen(
    onBack: () -> Unit,
    onOpenCategory: (String) -> Unit,
    viewModel: ArticleManageViewModel = koinViewModel()
) {
    val categories by remember { viewModel.categories }
    val loading by remember { viewModel.categoriesLoading }
    val error by remember { viewModel.categoryError }
    var editing by remember { mutableStateOf<CategoryItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CategoryItem?>(null) }
    val swipeableState = rememberSwipeableListState()
    val listState = rememberLazyListState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isSmallTitleVisible by listState.isScrolledPast(statusBarHeight + 24.dp)
    val categoryPageBackground = AppPageColor
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = categoryPageBackground,
            size = Size(size.width * 3, size.height * 3),
            topLeft = Offset(-size.width, -size.height)
        )
        drawContent()
    }
    val actions = persistentListOf(
        SwipeableActionButton(index = 1, color = AppColors.primary, icon = painterResource(R.drawable.ic_square_and_pencil), iconColor = AppColors.onPrimary, contentDescription = "编辑"),
        SwipeableActionButton(index = 0, color = AppColors.error, icon = painterResource(R.drawable.ic_trash), iconColor = AppColors.onError, contentDescription = "删除", isDestructive = true, triggerOnDeepSwipe = true)
    )
    LaunchedEffect(Unit) { viewModel.loadCategories() }

    Box(Modifier.fillMaxSize().appPageBackground()) {
        EditorOrbs()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusBarHeight + 76.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "category-page-heading") {
                Column(Modifier.padding(start = 4.dp, bottom = 18.dp)) {
                    Text("分类管理", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${categories.size} 个星域", color = AppColors.contentVariant, fontSize = 12.sp)
                }
            }
            if (loading && categories.isEmpty()) item { Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.primary) } }
            if (!loading && categories.isEmpty()) item { EmptyCategories { creating = true } }
            items(categories, key = { it.id }) { category ->
                GlasenseSwipeable(
                    key = category.id,
                    modifier = Modifier.fillMaxWidth(),
                    listState = swipeableState,
                    actions = actions,
                    onAction = { index -> if (index == 0) deleting = category else editing = category }
                ) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AppColors.cardBackground.copy(alpha = .58f))
                            .glasenseHighlight(RoundedCornerShape(20.dp)).clickable { onOpenCategory(category.name) }.padding(horizontal = 17.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(21.dp), AppColors.primary)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(category.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(category.description ?: "暂无描述", color = AppColors.contentVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${category.articleCount ?: 0} 篇", color = AppColors.contentVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(10.dp))
                        Icon(painterResource(R.drawable.ic_chevron_forward_compact), "左滑管理", Modifier.size(16.dp), AppColors.contentVariant.copy(alpha = .55f))
                    }
                }
            }
            error?.let { item { Text(it, color = AppColors.error, fontSize = 13.sp) } }
            // Keep a small scroll range even when the category list fits on screen,
            // so a vertical drag still has natural overscroll and spring-back feedback.
            appScrollBreathingRoom(key = "category-scroll-breathing-room")
        }
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = "分类管理",
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = categoryPageBackground
        ) {}
        GlasenseBackButton(
            onClick = onBack,
            backdrop = backdrop,
            modifier = Modifier.padding(top = statusBarHeight, start = 12.dp).size(48.dp).align(Alignment.TopStart)
        )
        GlasenseNavigationButton(
            modifier = Modifier.padding(top = statusBarHeight, end = 12.dp).size(48.dp).align(Alignment.TopEnd),
            isActive = false,
            onClick = { creating = true },
            backdrop = backdrop,
            liquidGlass = true
        ) {
            Icon(painterResource(R.drawable.ic_add), "新建分类", Modifier.size(22.dp), AppColors.primary)
        }
    }

    if (creating || editing != null) CategoryEditorDialog(editing, { creating = false; editing = null }) { name, description ->
        viewModel.saveCategory(editing?.id, name, description) { creating = false; editing = null }
    }
    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = AppColors.elevatedPageBackground,
            title = { Text("删除“${category.name}”？", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = { Text("分类中的文章会保留，并移动到未分类。此操作无法撤销。", color = AppColors.contentVariant) },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消", color = AppColors.content) }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category.id) { deleting = null }
                }) { Text("删除", color = AppColors.error, fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
private fun CategoryTopBar(title: String, subtitle: String, onBack: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().statusBarsPadding().height(72.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        GlasenseBackButton(onClick = onBack, modifier = Modifier.size(48.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = AppColors.contentVariant, fontSize = 11.sp)
        }
        GlasenseNavigationButton(
            modifier = Modifier.size(48.dp),
            isActive = false,
            onClick = onAdd
        ) {
            Icon(painterResource(R.drawable.ic_add), "新建分类", Modifier.size(22.dp), AppColors.primary)
        }
    }
}

@Composable
fun CategoryArticlesScreen(
    categoryName: String,
    onBack: () -> Unit,
    onArticleClick: (Int) -> Unit,
    viewModel: ArticlesViewModel = koinViewModel(key = "category-articles-$categoryName")
) {
    val articlesState by remember { viewModel.articlesState }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(categoryName) { viewModel.selectCategory(categoryName) }

    Box(Modifier.fillMaxSize().appPageBackground()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusBarHeight + 88.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("$categoryName 的星记", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            }
            when (val state = articlesState) {
                is ArticlesUiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.primary)
                    }
                }
                is ArticlesUiState.Success -> {
                    if (state.articles.isEmpty()) item {
                        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            Text("这个分类还没有星记", color = AppColors.contentVariant, fontSize = 14.sp)
                        }
                    } else items(state.articles, key = { it.id }) { article ->
                        ArticleFeedCard(article = article) { onArticleClick(article.id) }
                    }
                }
                is ArticlesUiState.Error -> item {
                    Text(state.message, color = AppColors.error, fontSize = 13.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(68.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlasenseBackButton(onClick = onBack, modifier = Modifier.size(48.dp))
            Text(categoryName, Modifier.padding(start = 12.dp), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable private fun EditorOrbs() {
    Box(Modifier.size(220.dp).offset(x = 250.dp, y = (-60).dp).blur(80.dp).background(Brush.radialGradient(listOf(AppColors.primary.copy(alpha = .16f), Color.Transparent))))
    Box(Modifier.size(240.dp).offset(x = (-140).dp, y = 500.dp).blur(100.dp).background(Brush.radialGradient(listOf(AppColors.activeTrack.copy(alpha = .10f), Color.Transparent))))
}

@Composable private fun EditorTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().statusBarsPadding().height(68.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        GhostIconButton(R.drawable.ic_forward_nav, "返回", AppColors.primary, onBack)
        Text(title, Modifier.padding(start = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable private fun EditorSection(title: String, icon: Int, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, Modifier.size(16.dp), AppColors.primary)
            Text(title, Modifier.padding(start = 7.dp), color = AppColors.contentVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxWidth().clip(AppSpecs.cardShape).background(color).glasenseHighlight(AppSpecs.cardShape), content = content)
    }
}

@Composable private fun PropertyRow(icon: Int, label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.size(19.dp), AppColors.primary)
        Text(label, Modifier.padding(start = 12.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(value, color = AppColors.contentVariant, fontSize = 13.sp)
        Icon(painterResource(R.drawable.ic_chevron_forward_compact), null, Modifier.padding(start = 8.dp).size(16.dp), AppColors.contentVariant)
    }
}

@Composable private fun SaveBar(saving: Boolean, enabled: Boolean, isNew: Boolean, modifier: Modifier, onSave: () -> Unit) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
        GlasenseButtonAlt(
            modifier = Modifier.fillMaxWidth().height(54.dp), enabled = enabled, onClick = onSave,
            colors = GlasenseButtonColors(
                AppColors.primary, AppColors.onPrimary,
                AppColors.cardBackground.copy(alpha = .85f), AppColors.contentVariant.copy(alpha = .55f)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (saving) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = AppColors.onPrimary)
                else Icon(painterResource(if (isNew) R.drawable.ic_add else R.drawable.ic_checkmark), null, Modifier.size(19.dp), AppColors.onPrimary)
                Text(if (saving) "正在保存" else if (isNew) "发布星记" else "保存修改", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (enabled) AppColors.onPrimary else AppColors.contentVariant.copy(alpha = .55f))
            }
        }
    }
}

@Composable private fun CategoryEditorDialog(item: CategoryItem?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember(item) { mutableStateOf(item?.name.orEmpty()) }
    var description by remember(item) { mutableStateOf(item?.description.orEmpty()) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(AppColors.elevatedPageBackground)
                    .glasenseHighlight(RoundedCornerShape(30.dp))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (item == null) "新建星域" else "编辑星域",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "为同一主题的星记建立一个清晰归处",
                            color = AppColors.contentVariant,
                            fontSize = 12.sp
                        )
                    }
                    GhostIconButton(R.drawable.ic_xmark_bold, "关闭", tint = AppColors.contentVariant, onClick = onDismiss)
                }
                ConfigTextField(
                    title = "名称",
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    backgroundColor = AppColors.elevatedCardBackground,
                    decorateText = "例如：灵感随笔",
                    modifier = Modifier.height(72.dp)
                )
                ConfigTextField(
                    title = "描述",
                    value = description,
                    onValueChange = { description = it },
                    minLines = 2,
                    maxLines = 3,
                    backgroundColor = AppColors.elevatedCardBackground,
                    decorateText = "这个星域主要收纳什么内容？",
                    modifier = Modifier.height(96.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlasenseButtonAlt(
                        modifier = Modifier.weight(1f).height(50.dp),
                        enabled = true,
                        onClick = onDismiss,
                        colors = AppButtonColors.secondary()
                    ) {
                        Text("取消", fontWeight = FontWeight.SemiBold)
                    }
                    GlasenseButtonAlt(
                        modifier = Modifier.weight(1.4f).height(50.dp),
                        enabled = name.isNotBlank(),
                        onClick = { onSave(name.trim(), description.trim()) },
                        colors = AppButtonColors.primary()
                    ) {
                        Text(
                            if (item == null) "创建星域" else "保存修改",
                            fontWeight = FontWeight.Bold,
                            color = if (name.isNotBlank()) AppColors.onPrimary else AppColors.contentVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun ThinDivider() { Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(AppColors.scrimLight)) }

@Composable private fun GhostIconButton(icon: Int, description: String, tint: Color = AppColors.primary, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(painterResource(icon), description, Modifier.size(20.dp), tint) }
}

@Composable private fun FilledIconButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(Modifier.size(46.dp).clip(CircleShape).background(AppColors.primary).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(painterResource(icon), description, Modifier.size(21.dp), AppColors.onPrimary) }
}

@Composable private fun EmptyCategories(onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(AppColors.primary.copy(alpha = .10f)), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_folder_plus), null, Modifier.size(28.dp), AppColors.primary) }
        Text("还没有星域", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("创建分类，让每篇星记都有归处", color = AppColors.contentVariant, fontSize = 12.sp)
        TextButton(onClick = onClick) { androidx.compose.material3.Text("创建第一个星域") }
    }
}
