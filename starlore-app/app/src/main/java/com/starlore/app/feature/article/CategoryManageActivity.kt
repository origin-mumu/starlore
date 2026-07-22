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

class CategoryManageActivity : AppCompatActivity() {
    companion object {
        fun createIntent(context: Context) = Intent(context, CategoryManageActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setResult(Activity.RESULT_OK)

        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory()
                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalGlasenseContentColor provides AppColors.content
                ) {
                    CategoryManageScreen(
                        onBack = {
                            finish()
                        },
                        onOpenCategory = {
                            startActivity(CategoryArticlesActivity.createIntent(this@CategoryManageActivity, it))
                        }
                    )
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}
