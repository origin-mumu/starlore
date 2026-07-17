package com.starlore.app.feature.article

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.starlore.app.theme.AppColors
import com.starlore.glasense.core.component.Text
import org.json.JSONObject

private class EditorBridge(private val onChange: (String) -> Unit) {
    @JavascriptInterface fun onContentChanged(html: String) = onChange(html)
}

class RichHtmlEditorController internal constructor() {
    internal var webView: WebView? = null
    fun command(command: String, value: String? = null) { webView.exec(command, value) }
}

@Composable fun rememberRichHtmlEditorController() = remember { RichHtmlEditorController() }

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichHtmlEditor(initialHtml: String, onHtmlChange: (String) -> Unit, controller: RichHtmlEditorController, modifier: Modifier = Modifier) {
    val contentColor = AppColors.content.toArgb()
    val mutedColor = AppColors.contentVariant.toArgb()
    val background = AppColors.cardBackground.toArgb()
    val primary = AppColors.primary.toArgb()

    Box(modifier) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        controller.webView = this
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        settings.defaultFontSize = 16
                        isVerticalScrollBarEnabled = false
                        overScrollMode = WebView.OVER_SCROLL_NEVER
                        addJavascriptInterface(EditorBridge(onHtmlChange), "StarloreEditor")
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                view.evaluateJavascript("window.setEditorHtml(${JSONObject.quote(initialHtml)});", null)
                            }
                        }
                        loadDataWithBaseURL(null, editorDocument(contentColor, mutedColor, background, primary), "text/html", "UTF-8", null)
                    }
                },
                update = { controller.webView = it }
            )
    }
}

@Composable
fun RichHtmlToolbar(controller: RichHtmlEditorController, modifier: Modifier = Modifier) {
    Row(
        modifier.height(52.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        EditorIconTool("B", "加粗", FontWeight.Bold) { controller.command("bold") }
        EditorIconTool("H", "标题", FontWeight.Medium) { controller.command("formatBlock", "h2") }
        EditorIconTool("❝", "引用") { controller.command("formatBlock", "blockquote") }
        EditorIconTool("≡", "无序列表") { controller.command("insertUnorderedList") }
        EditorIconTool("↶", "撤销", fontSize = 24) { controller.command("undo") }
        EditorIconTool("↷", "重做", fontSize = 24) { controller.command("redo") }
    }
}

private fun WebView?.exec(command: String, value: String? = null) {
    val jsValue = value?.let(JSONObject::quote) ?: "null"
    this?.evaluateJavascript("window.editorCommand(${JSONObject.quote(command)}, $jsValue);", null)
}

@Composable
private fun RowScope.EditorIconTool(
    label: String,
    description: String,
    weight: FontWeight = FontWeight.Normal,
    fontSize: Int = 19,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = fontSize.sp, fontWeight = weight, color = AppColors.content)
    }
}

private fun editorDocument(content: Int, muted: Int, background: Int, primary: Int): String {
    fun cssColor(argb: Int) = "#%06X".format(0xFFFFFF and argb)
    return """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
        <style>
          *{box-sizing:border-box} html,body{margin:0;background:transparent;color:${cssColor(content)}}
          body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Noto Sans SC',sans-serif;font-size:16px;line-height:1.75;padding:14px 16px 90px;min-height:500px}
          #editor{outline:none;min-height:390px;word-break:break-word}
          #editor:empty:before{content:'开始书写正文…';color:${cssColor(muted)};pointer-events:none}
          h1,h2,h3,h4{line-height:1.35;margin:1.35em 0 .55em;font-weight:750} h2{font-size:1.45em} h3{font-size:1.22em} h4{font-size:1.08em}
          p{margin:.65em 0} ul,ol{padding-left:1.5em} li{margin:.35em 0}
          blockquote{margin:1em 0;padding:10px 14px;background:${cssColor(background)};border-radius:12px;color:${cssColor(muted)}}
          pre{overflow:auto;padding:13px;border-radius:12px;background:${cssColor(background)};font-family:monospace;font-size:13px;line-height:1.55}
          code{font-family:monospace;color:${cssColor(primary)}}
          table{width:100%;border-collapse:collapse;display:block;overflow:auto;margin:1em 0} td,th{border:1px solid ${cssColor(muted)};padding:7px 9px;text-align:left} th{background:${cssColor(background)}}
          a{color:${cssColor(primary)}} img{max-width:100%;height:auto;border-radius:12px}
        </style></head><body><div id="editor" contenteditable="true"></div>
        <script>
          const editor=document.getElementById('editor');
          window.setEditorHtml=function(html){editor.innerHTML=html||'';};
          window.editorCommand=function(command,value){editor.focus();document.execCommand(command,false,value);notify();};
          let timer; function notify(){clearTimeout(timer);timer=setTimeout(()=>StarloreEditor.onContentChanged(editor.innerHTML),120);}
          editor.addEventListener('input',notify); editor.addEventListener('blur',notify);
        </script></body></html>
    """.trimIndent()
}
