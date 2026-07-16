package com.starlore.app.feature.guide

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.starlore.app.MainActivity
import com.starlore.app.feature.settings.util.SettingsManager
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.GlasenseTheme
import com.starlore.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.starlore.glasense.theme.LocalGlasenseContentColor

class GuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory()

                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalGlasenseContentColor provides AppColors.content,
                ) {
                    GuideScreen(onFinish = {
                        SettingsManager.isFirstRun = false

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)

                        finish()
                    })
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}
