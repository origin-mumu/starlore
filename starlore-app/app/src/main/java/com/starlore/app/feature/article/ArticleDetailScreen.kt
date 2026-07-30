package com.starlore.app.feature.article

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starlore.app.R
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.appPageBackground
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import org.koin.androidx.compose.koinViewModel
import com.starlore.glasense.theme.GlasenseTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.core.text.HtmlCompat
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.starlore.app.ui.components.glasense.GlasenseMenu
import com.starlore.app.ui.components.glasense.GlasenseBackButton
import com.starlore.app.ui.components.glasense.GlasenseNavigationButton
import com.starlore.app.ui.components.glasense.MenuItemData
import com.starlore.app.ui.components.glasense.MenuState
import com.starlore.app.ui.components.glasense.glasenseHighlight
import com.starlore.app.data.api.ArticleDetail
import androidx.compose.ui.draw.clip

@Composable
fun ArticleDetailScreen(
    articleId: Int,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onDeleted: () -> Unit,
    onAskAi: (ArticleDetail) -> Unit = {},
    viewModel: ArticleDetailViewModel = koinViewModel()
) {
    val detailState by remember { viewModel.detailState }
    val deleteState by remember { viewModel.deleteState }
    val context = LocalContext.current
    var menuState by remember { mutableStateOf(MenuState()) }
    var moreButtonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val editIcon = painterResource(R.drawable.ic_square_and_pencil)
    val deleteIcon = painterResource(R.drawable.ic_trash)
    val aiIcon = painterResource(R.drawable.ic_nav_sparkle_filled)
    val errorColor = AppColors.error

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    LaunchedEffect(deleteState) {
        if (deleteState is ArticleDeleteUiState.Error) {
            Toast.makeText(
                context,
                (deleteState as ArticleDeleteUiState.Error).message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val scrollState = rememberScrollState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarBgAlpha by animateFloatAsState(
        targetValue = if (scrollState.value > 0) 0.92f else 0f,
        animationSpec = tween(300)
    )

    val pageBgColor = AppColors.pageBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Record only the page content. Overlay controls sample this layer, so including
        // the controls themselves would create a recursive HWUI RenderNode graph.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .appPageBackground()
                .layerBackdrop(backdrop)
        ) {
            when (val state = detailState) {
            is ArticleDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.primary)
                }
            }
            is ArticleDetailUiState.Success -> {
                val article = state.article

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(statusBarHeight + 64.dp))
                        // Category Tag
                        Box(
                            modifier = Modifier
                                .background(AppColors.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = article.category ?: "默认星域",
                                fontSize = 12.sp,
                                color = AppColors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Title
                        Text(
                            text = article.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 32.sp
                        )

                        // Meta Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "阅读 ${article.viewCount ?: 0}",
                                fontSize = 12.sp,
                                color = AppColors.contentVariant
                            )
                            Text(
                                text = article.createdAt?.take(10) ?: "",
                                fontSize = 12.sp,
                                color = AppColors.contentVariant
                            )
                        }

                        Divider(color = AppColors.scrimMedium)

                        // Tags row
                        if (article.tags.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                article.tags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(AppColors.scrimLight, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            fontSize = 11.sp,
                                            color = AppColors.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Render full article content using WebView for proper table, code blocks, and rich style support
                        ArticleContentViewer(
                            content = article.content,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
            is ArticleDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败: ${state.message}", color = AppColors.error, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadArticle(articleId) }) {
                            androidx.compose.material3.Text("重试", color = AppColors.onPrimary)
                        }
                    }
                }
            }
            }
        }


        // 6. Action Buttons Bar (Always Visible)
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(48.dp)
        ) {
            GlasenseBackButton(
                onClick = onBack,
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(48.dp)
            )

            // More button on the right
            GlasenseNavigationButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(48.dp)
                    .onGloballyPositioned { moreButtonCoordinates = it },
                isActive = false,
                onClick = {
                        val anchor = moreButtonCoordinates?.boundsInWindow()
                            ?: return@GlasenseNavigationButton
                        menuState = MenuState(
                            isVisible = true,
                            anchorBounds = anchor,
                            items = listOf(
                                MenuItemData(
                                    text = "编辑",
                                    icon = editIcon,
                                    onClick = { onEdit(articleId) }
                                ),
                                MenuItemData(
                                    text = "AI 解读此篇",
                                    icon = aiIcon,
                                    onClick = {
                                        (detailState as? ArticleDetailUiState.Success)?.let { onAskAi(it.article) }
                                    }
                                ),
                                MenuItemData(
                                    text = "删除",
                                    icon = deleteIcon,
                                    iconColor = errorColor,
                                    isDestructive = true,
                                    onClick = { viewModel.deleteArticle(articleId) { onDeleted() } }
                                )
                            )
                        )
                    },
                backdrop = backdrop,
                liquidGlass = true
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ellipsis),
                    contentDescription = "More",
                    modifier = Modifier.size(20.dp),
                    tint = AppColors.primary
                )
            }
        }
    }

    GlasenseMenu(
        menuState = menuState,
        backdrop = backdrop,
        onDismiss = { menuState = menuState.copy(isVisible = false) },
        // 部分设备的 HWUI 在菜单实时玻璃背景与详情页渲染树叠加时会递归崩溃。
        // 仅关闭该菜单的运行时背景采样，保留 Glasense 的圆角、阴影和动效。
        forceSafeSurface = true
    )
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        var inCodeBlock = false
        var codeBlockContent = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            // Handle code block block wrapping
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // end code block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.scrimNormal)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = codeBlockContent.toString().trimEnd(),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AppColors.content
                        )
                    }
                    codeBlockContent = StringBuilder()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockContent.append(line).append("\n")
                continue
            }

            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                continue
            }

            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = markdownInline(trimmed.substring(2)),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.primary
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = markdownInline(trimmed.substring(3)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.primary
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = markdownInline(trimmed.substring(4)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.primary)
                        Text(text = markdownInline(trimmed.substring(2)), fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
                trimmed.matches(Regex("^(\\*{3,}|-{3,}|_{3,})$")) -> {
                    HorizontalDivider(
                        color = AppColors.scrimMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                trimmed.matches(Regex("^\\d+[.)]\\s+.+")) -> {
                    val marker = trimmed.substringBefore(' ').trimEnd('.', ')')
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("$marker.", fontWeight = FontWeight.Bold, color = AppColors.primary)
                        Text(
                            text = markdownInline(trimmed.substringAfter(' ')),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
                trimmed.startsWith("> ") -> {
                    Text(
                        text = markdownInline(trimmed.substring(2)),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = AppColors.contentVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.scrimLight)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                else -> {
                    Text(
                        text = markdownInline(line),
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun markdownInline(source: String): AnnotatedString {
    val codeBackground = AppColors.scrimNormal
    return buildAnnotatedString {
        var index = 0
        while (index < source.length) {
            val marker = when {
                source.startsWith("***", index) -> "***"
                source.startsWith("**", index) -> "**"
                source.startsWith("__", index) -> "__"
                source.startsWith("*", index) -> "*"
                source.startsWith("_", index) -> "_"
                source.startsWith("`", index) -> "`"
                else -> null
            }

            if (marker == null) {
                append(source[index])
                index++
                continue
            }

            val closing = source.indexOf(marker, index + marker.length)
            if (closing < 0) {
                append(marker)
                index += marker.length
                continue
            }

            val value = source.substring(index + marker.length, closing)
            val style = when (marker) {
                "***" -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                "**", "__" -> SpanStyle(fontWeight = FontWeight.Bold)
                "*", "_" -> SpanStyle(fontStyle = FontStyle.Italic)
                else -> SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
            }
            withStyle(style) { append(value) }
            index = closing + marker.length
        }
    }
}

@Composable
fun ArticleContentViewer(content: String, modifier: Modifier = Modifier) {
    val isHtml = content.trim().startsWith("<")
    if (isHtml) {
        val textColor = AppColors.content.toArgb()
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                TextView(context).apply {
                    setTextColor(textColor)
                    textSize = 15f
                    setLineSpacing(0f, 1.35f)
                    movementMethod = LinkMovementMethod.getInstance()
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { textView ->
                textView.text = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        )
    } else {
        MarkdownText(text = content, modifier = modifier.fillMaxWidth())
    }
}
