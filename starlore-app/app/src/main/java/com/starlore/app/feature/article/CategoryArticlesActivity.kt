package com.starlore.app.feature.article

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

class CategoryArticlesActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_CATEGORY_NAME = "category_name"

        fun createIntent(context: Context, categoryName: String) =
            Intent(context, CategoryArticlesActivity::class.java)
                .putExtra(EXTRA_CATEGORY_NAME, categoryName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME)
        if (categoryName.isNullOrBlank()) {
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
                    CategoryArticlesScreen(
                        categoryName = categoryName,
                        onBack = { finish() },
                        onArticleClick = {
                            startActivity(ArticleDetailActivity.createIntent(this@CategoryArticlesActivity, it))
                        }
                    )
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}
