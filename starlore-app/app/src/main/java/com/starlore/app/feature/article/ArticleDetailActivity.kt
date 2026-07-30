package com.starlore.app.feature.article

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.core.view.WindowCompat
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.GlasenseTheme
import com.starlore.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.starlore.glasense.theme.LocalGlasenseContentColor

class ArticleDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ARTICLE_ID = "article_id"
        const val EXTRA_CHANGED = "article_changed"
        const val EXTRA_AI_PROMPT = "ai_prompt"

        fun createIntent(context: Context, articleId: Int) =
            Intent(context, ArticleDetailActivity::class.java)
                .putExtra(EXTRA_ARTICLE_ID, articleId)
    }

    private var contentVersion by mutableIntStateOf(0)
    private val editorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            contentVersion++
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_CHANGED, true))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        val articleId = intent.getIntExtra(EXTRA_ARTICLE_ID, -1)
        if (articleId < 0) {
            finish()
            return
        }

        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory()
                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalGlasenseContentColor provides AppColors.content
                ) {
                    val surfaceColor = AppColors.background
                    val backdrop = rememberLayerBackdrop {
                        drawRect(
                            color = surfaceColor,
                            size = Size(size.width * 3, size.height * 3),
                            topLeft = Offset(-size.width, -size.height)
                        )
                        drawContent()
                    }
                    androidx.compose.runtime.key(contentVersion) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ArticleDetailScreen(
                                articleId = articleId,
                                backdrop = backdrop,
                                onBack = { finish() },
                                onEdit = {
                                    editorLauncher.launch(
                                        ArticleEditorActivity.createIntent(this@ArticleDetailActivity, it)
                                    )
                                },
                                onDeleted = {
                                    setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_CHANGED, true))
                                    finish()
                                },
                                onAskAi = { article ->
                                    val context = article.content
                                        .replace(Regex("<[^>]*>"), " ")
                                        .replace(Regex("\\s+"), " ")
                                        .trim()
                                        .take(8_000)
                                    val prompt = buildString {
                                        append("请基于下面这篇星记进行解读、总结，并给出可继续完善的建议。\n\n")
                                        append("标题：").append(article.title).append('\n')
                                        article.category?.let { append("分类：").append(it).append('\n') }
                                        if (article.tags.isNotEmpty()) append("标签：").append(article.tags.joinToString("、")).append('\n')
                                        append("\n正文：\n").append(context)
                                    }
                                    setResult(
                                        Activity.RESULT_FIRST_USER,
                                        Intent().putExtra(EXTRA_AI_PROMPT, prompt)
                                    )
                                    finish()
                                }
                            )
                        }
                    }
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}
