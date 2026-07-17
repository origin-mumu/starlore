package com.starlore.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.starlore.app.feature.guide.GuideActivity
import com.starlore.app.feature.main.MainScreen
import com.starlore.app.feature.screenextract.ScreenExtractEvents
import com.starlore.app.feature.settings.util.SettingsManager
import com.starlore.app.theme.AppColors
import com.starlore.app.theme.GlasenseTheme
import com.starlore.app.util.NotificationPermissionCompat
import com.starlore.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.starlore.glasense.theme.LocalGlasenseContentColor

private const val REQUEST_POST_NOTIFICATIONS = 1001

/**
 * The main activity of the application.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This makes the app display behind the system bars.

        requestNotificationPermissionIfNeeded()
        handleScreenExtractErrorIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val isFirstRun = SettingsManager.isFirstRun

        if (isFirstRun) {
            startActivity(Intent(this, GuideActivity::class.java))
            finish()
            return
        }

        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory()

                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalGlasenseContentColor provides AppColors.content //provide content color
                ) {
                    MainScreen()
                }
            }
        }
        window.setBackgroundDrawable(null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleScreenExtractErrorIntent(intent)
    }

    private fun handleScreenExtractErrorIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ScreenExtractEvents.EXTRA_SHOW_ERROR_DIALOG, false) != true) {
            return
        }
        val message = intent.getStringExtra(ScreenExtractEvents.EXTRA_ERROR_MESSAGE)
            ?: getString(R.string.extract_screen_failed)
        ScreenExtractEvents.emitError(message)
        intent.removeExtra(ScreenExtractEvents.EXTRA_SHOW_ERROR_DIALOG)
        intent.removeExtra(ScreenExtractEvents.EXTRA_ERROR_MESSAGE)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (!NotificationPermissionCompat.shouldRequestPostNotificationsPermission()) {
            return
        }

        if (NotificationPermissionCompat.hasPostNotificationsPermission(this)) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(NotificationPermissionCompat.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS
        )
    }
}
