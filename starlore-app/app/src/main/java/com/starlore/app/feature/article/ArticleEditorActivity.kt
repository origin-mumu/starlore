package com.starlore.app.feature.article

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.GlasenseTheme
import com.starlore.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.starlore.glasense.theme.LocalGlasenseContentColor

class ArticleEditorActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ARTICLE_ID = "article_id"
        const val EXTRA_SAVED_ARTICLE_ID = "saved_article_id"

        fun createIntent(context: Context, articleId: Int? = null) =
            Intent(context, ArticleEditorActivity::class.java).apply {
                articleId?.let { putExtra(EXTRA_ARTICLE_ID, it) }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        val articleId = intent.getIntExtra(EXTRA_ARTICLE_ID, -1).takeIf { it >= 0 }

        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory()
                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalGlasenseContentColor provides AppColors.content
                ) {
                    ArticleEditorScreen(
                        articleId = articleId,
                        onBack = { finish() },
                        onSaved = { savedId ->
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(EXTRA_SAVED_ARTICLE_ID, savedId)
                            )
                            finish()
                        }
                    )
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}
