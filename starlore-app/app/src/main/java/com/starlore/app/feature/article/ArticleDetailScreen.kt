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
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text
import org.koin.androidx.compose.koinViewModel
import com.starlore.glasense.theme.GlasenseTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.core.text.HtmlCompat
import android.webkit.WebView
import android.view.ViewGroup
import android.graphics.Color
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.starlore.app.ui.components.glasense.GlasenseMenu
import com.starlore.app.ui.components.glasense.MenuItemData
import com.starlore.app.ui.components.glasense.MenuState
import com.starlore.app.ui.components.glasense.glasenseHighlight
import androidx.compose.ui.draw.clip

@Composable
fun ArticleDetailScreen(
    articleId: Int,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    viewModel: ArticleDetailViewModel = koinViewModel()
) {
    val detailState by remember { viewModel.detailState }
    val deleteState by remember { viewModel.deleteState }
    val context = LocalContext.current
    var menuState by remember { mutableStateOf(MenuState()) }
    var moreButtonBounds by remember { mutableStateOf(Rect.Zero) }
    val editIcon = painterResource(R.drawable.ic_square_and_pencil)
    val deleteIcon = painterResource(R.drawable.ic_trash)
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
            .background(pageBgColor)
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


        // 6. Action Buttons Bar (Always Visible)
        val btnBgColor = AppColors.cardBackground

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(48.dp)
        ) {
            // Back button on the left
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(btnBgColor)
                    .glasenseHighlight(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_up),
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = -90f },
                    tint = AppColors.primary
                )
            }

            // More button on the right
            var moreButtonCoordinates: LayoutCoordinates? = null
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(48.dp)
                    .onGloballyPositioned { moreButtonCoordinates = it }
                    .clip(CircleShape)
                    .background(btnBgColor)
                    .glasenseHighlight(CircleShape)
                    .clickable {
                        moreButtonBounds = moreButtonCoordinates?.boundsInWindow() ?: Rect.Zero
                        menuState = MenuState(
                            isVisible = true,
                            anchorBounds = moreButtonBounds,
                            items = listOf(
                                MenuItemData(
                                    text = "编辑",
                                    icon = editIcon,
                                    onClick = {
                                        menuState = menuState.copy(isVisible = false)
                                        onEdit(articleId)
                                    }
                                ),
                                MenuItemData(
                                    text = "删除",
                                    icon = deleteIcon,
                                    iconColor = errorColor,
                                    isDestructive = true,
                                    onClick = {
                                        menuState = menuState.copy(isVisible = false)
                                        viewModel.deleteArticle(articleId) {
                                            onBack()
                                        }
                                    }
                                )
                            )
                        )
                    },
                contentAlignment = Alignment.Center
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
        onDismiss = { menuState = menuState.copy(isVisible = false) }
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
    val isDark = GlasenseTheme.darkTheme
    val textColor = if (isDark) "#E5E7EB" else "#333333"
    val tableHeaderBg = if (isDark) "#1F2937" else "#F3F4F6"
    val borderColor = if (isDark) "#374151" else "#E5E7EB"
    val codeBg = if (isDark) "#1F2937" else "#F9FAFB"
    val inlineCodeBg = if (isDark) "rgba(255,255,255,0.1)" else "rgba(0,0,0,0.05)"

    // Check if original content is HTML or Markdown.
    // Pure HTML articles always start with an HTML tag (e.g. <div or <p).
    // Markdown files might contain tags inside code blocks but start with markdown text.
    val isHtml = content.trim().startsWith("<")
    val htmlContent = if (isHtml) {
        content
    } else {
        markdownToHtml(content)
    }

    val fullHtml = """
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                font-size: 15px;
                line-height: 1.6;
                color: $textColor;
                background-color: transparent;
                margin: 0;
                padding: 0;
            }
            h1, h2, h3, h4, h5, h6 {
                font-weight: 700;
                margin-top: 24px;
                margin-bottom: 12px;
                color: $textColor;
            }
            h1 { font-size: 1.5em; border-bottom: 1px solid $borderColor; padding-bottom: 6px; }
            h2 { font-size: 1.3em; }
            h3 { font-size: 1.1em; }
            p { margin-top: 0; margin-bottom: 16px; }
            a { color: #2563EB; text-decoration: none; }
            a:hover { text-decoration: underline; }
            ul, ol { margin-top: 0; margin-bottom: 16px; padding-left: 20px; }
            li { margin-bottom: 6px; }
            
            /* Table Styling */
            table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 16px;
                margin-bottom: 16px;
                font-size: 14px;
            }
            table th {
                background-color: $tableHeaderBg;
                font-weight: 600;
                text-align: left;
            }
            table td, table th {
                border: 1px solid $borderColor;
                padding: 8px 12px;
                color: $textColor;
            }
            
            /* Code Block Styling */
            pre {
                background-color: $codeBg;
                border-radius: 8px;
                padding: 12px;
                overflow-x: auto;
                margin-top: 16px;
                margin-bottom: 16px;
                border: 1px solid $borderColor;
            }
            code {
                font-family: SFMono-Regular, Consolas, "Liberation Mono", Menlo, Courier, monospace;
                font-size: 13px;
                background-color: $inlineCodeBg;
                padding: 2px 4px;
                border-radius: 4px;
                color: $textColor;
            }
            pre code {
                background-color: transparent;
                padding: 0;
                border-radius: 0;
                display: block;
                white-space: pre;
            }
        </style>
        </head>
        <body>
            $htmlContent
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                settings.javaScriptEnabled = false
                setBackgroundColor(Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
        }
    )
}

fun markdownToHtml(markdown: String): String {
    val lines = markdown.split("\n")
    val html = StringBuilder()
    var inCodeBlock = false
    var codeContent = StringBuilder()
    var inTable = false
    var tableHeaders = listOf<String>()
    var tableRows = mutableListOf<List<String>>()
    var inList = false

    fun closeTable() {
        if (inTable) {
            html.append("<table>")
            if (tableHeaders.isNotEmpty()) {
                html.append("<thead><tr>")
                for (h in tableHeaders) {
                    html.append("<th>").append(parseInlineMarkdown(h)).append("</th>")
                }
                html.append("</tr></thead>")
            }
            html.append("<tbody>")
            for (row in tableRows) {
                html.append("<tr>")
                for (cell in row) {
                    html.append("<td>").append(parseInlineMarkdown(cell)).append("</td>")
                }
                html.append("</tr>")
            }
            html.append("</tbody></table>")
            inTable = false
            tableHeaders = emptyList()
            tableRows.clear()
        }
    }

    fun closeList() {
        if (inList) {
            html.append("</ul>")
            inList = false
        }
    }

    for (line in lines) {
        val trimmed = line.trim()

        // 1. Code Block state machine
        if (trimmed.startsWith("```")) {
            closeTable()
            closeList()
            if (inCodeBlock) {
                // End code block
                html.append("<pre><code>")
                html.append(escapeHtml(codeContent.toString().trimEnd()))
                html.append("</code></pre>")
                codeContent = StringBuilder()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            codeContent.append(line).append("\n")
            continue
        }

        // 2. Table state machine
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            closeList()
            val cells = trimmed.split("|").map { it.trim() }.drop(1).dropLast(1)
            // Check if separator row like |---|---|
            val isSeparator = cells.isNotEmpty() && cells.all { cell -> cell.all { it == '-' || it == ':' } }
            if (isSeparator) {
                continue
            }
            if (!inTable) {
                inTable = true
                tableHeaders = cells
            } else {
                tableRows.add(cells)
            }
            continue
        } else {
            closeTable()
        }

        // 3. List state machine
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            if (!inList) {
                inList = true
                html.append("<ul>")
            }
            html.append("<li>").append(parseInlineMarkdown(trimmed.substring(2))).append("</li>")
            continue
        } else {
            closeList()
        }

        if (trimmed.isEmpty()) {
            html.append("<p></p>")
            continue
        }

        // 4. Headers and plain text
        when {
            trimmed.startsWith("# ") -> {
                html.append("<h1>").append(parseInlineMarkdown(trimmed.substring(2))).append("</h1>")
            }
            trimmed.startsWith("## ") -> {
                html.append("<h2>").append(parseInlineMarkdown(trimmed.substring(3))).append("</h2>")
            }
            trimmed.startsWith("### ") -> {
                html.append("<h3>").append(parseInlineMarkdown(trimmed.substring(4))).append("</h3>")
            }
            trimmed.startsWith("#### ") -> {
                html.append("<h4>").append(parseInlineMarkdown(trimmed.substring(5))).append("</h4>")
            }
            else -> {
                html.append("<p>").append(parseInlineMarkdown(line)).append("</p>")
            }
        }
    }

    closeTable()
    closeList()
    return html.toString()
}

fun escapeHtml(text: String): String {
    return text.replace("&", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;")
               .replace("\"", "&quot;")
               .replace("'", "&#039;")
}

fun parseInlineMarkdown(text: String): String {
    var result = text
    result = escapeHtml(result)
    
    // Bold: **text**
    val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
    result = boldRegex.replace(result) { "<strong>${it.groupValues[1]}</strong>" }
    
    // Italic: *text*
    val italicRegex = "\\*(.*?)\\*".toRegex()
    result = italicRegex.replace(result) { "<em>${it.groupValues[1]}</em>" }
    
    // Inline code: `code`
    val codeRegex = "`(.*?)`".toRegex()
    result = codeRegex.replace(result) { "<code>${it.groupValues[1]}</code>" }
    
    // Images: ![alt](url)
    val imgRegex = "!\\[(.*?)\\]\\((.*?)\\)".toRegex()
    result = imgRegex.replace(result) { "<img src=\"${it.groupValues[2]}\" alt=\"${it.groupValues[1]}\" style=\"max-width:100%;\" />" }
    
    // Links: [text](url)
    val linkRegex = "\\[(.*?)\\]\\((.*?)\\)".toRegex()
    result = linkRegex.replace(result) { "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>" }
    
    return result
}
