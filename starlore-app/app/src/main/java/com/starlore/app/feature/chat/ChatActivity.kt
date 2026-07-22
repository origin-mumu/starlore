package com.starlore.app.feature.chat

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

class ChatActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_INITIAL_PROMPT = "initial_prompt"

        fun createIntent(context: Context, initialPrompt: String? = null) =
            Intent(context, ChatActivity::class.java).apply {
                initialPrompt?.let { putExtra(EXTRA_INITIAL_PROMPT, it) }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory()
                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalGlasenseContentColor provides AppColors.content
                ) {
                    EchobotScreen(
                        initialPrompt = intent.getStringExtra(EXTRA_INITIAL_PROMPT),
                        promptToken = if (intent.hasExtra(EXTRA_INITIAL_PROMPT)) 1 else 0,
                        onBack = { finish() }
                    )
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}
