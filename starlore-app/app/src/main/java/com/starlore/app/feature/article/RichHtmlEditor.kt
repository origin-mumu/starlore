package com.starlore.app.feature.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starlore.app.R
import com.starlore.app.theme.AppColors
import com.starlore.glasense.core.component.Icon
import com.starlore.glasense.core.component.Text

class MarkdownEditorController internal constructor() {
    internal var applyEdit: ((String, String, String) -> Unit)? = null
    fun wrap(before: String, after: String = before, placeholder: String = "文字") =
        applyEdit?.invoke(before, after, placeholder)
}

@Composable
fun rememberMarkdownEditorController() = remember { MarkdownEditorController() }

@Composable
fun MarkdownEditor(
    initialMarkdown: String,
    onMarkdownChange: (String) -> Unit,
    controller: MarkdownEditorController,
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf(TextFieldValue(initialMarkdown)) }

    LaunchedEffect(initialMarkdown) {
        if (initialMarkdown != value.text) value = TextFieldValue(initialMarkdown)
    }
    controller.applyEdit = { before, after, placeholder ->
        val selection = value.selection
        val selected = value.text.substring(selection.min, selection.max).ifEmpty { placeholder }
        val next = value.text.replaceRange(selection.min, selection.max, before + selected + after)
        value = TextFieldValue(next, TextRange(selection.min + before.length, selection.min + before.length + selected.length))
        onMarkdownChange(next)
    }

    Column(modifier.background(androidx.compose.ui.graphics.Color.Transparent)) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { value = it; onMarkdownChange(it.text) },
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = AppColors.content,
                fontSize = 16.sp,
                lineHeight = 28.sp,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(AppColors.primary),
            decorationBox = { input ->
                if (value.text.isEmpty()) Text("开始书写正文… 支持 Markdown 语法", color = AppColors.contentVariant, fontSize = 15.sp)
                input()
            }
        )
    }
}

@Composable
fun MarkdownToolbar(controller: MarkdownEditorController, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorTool("H₂", FontWeight.Bold) { controller.wrap("## ", "", "标题") }
        EditorTool("B", FontWeight.Bold) { controller.wrap("**") }
        EditorTool("I", FontWeight.Normal) { controller.wrap("*") }
        EditorTool("“", FontWeight.Medium) { controller.wrap("> ", "", "引用") }
        EditorIconTool(R.drawable.ic_list_bullet, "无序列表") { controller.wrap("- ", "", "列表项") }
        EditorIconTool(R.drawable.ic_link, "链接") { controller.wrap("[", "](https://)", "链接文字") }
        EditorIconTool(R.drawable.ic_photo, "图片") { controller.wrap("![", "](https://)", "图片描述") }
        EditorTool("</>", FontWeight.Medium) { controller.wrap("`", "`", "代码") }
    }
}

@Composable
private fun RowScope.EditorTool(label: String, weight: FontWeight = FontWeight.Normal, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.width(46.dp).height(44.dp).clip(RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = if (label == "</>") 13.sp else 17.sp, fontWeight = weight, color = AppColors.content)
    }
}

@Composable
private fun RowScope.EditorIconTool(iconRes: Int, description: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.width(46.dp).height(44.dp).clip(RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier.size(19.dp),
            tint = AppColors.content
        )
    }
}
