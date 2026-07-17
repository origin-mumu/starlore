package com.starlore.app.feature.settings

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

enum class SettingsDestination(val value: String) {
    SETTINGS("settings"),
    APPEARANCE("appearance"),
    AI("ai"),
    DATA_STORAGE("data_storage"),
    GENERAL("general"),
    ABOUT("about"),
    CREDITS("credits"),
    ACCOUNT("account");

    companion object {
        fun fromValue(value: String?): SettingsDestination {
            return entries.firstOrNull { it.value == value } ?: SETTINGS
        }
    }
}

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_SETTINGS_DESTINATION = "settings_destination"

        fun createIntent(context: Context, destination: SettingsDestination): Intent {
            return Intent(context, SettingsActivity::class.java)
                .putExtra(EXTRA_SETTINGS_DESTINATION, destination.value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val destination = SettingsDestination.fromValue(
            intent.getStringExtra(EXTRA_SETTINGS_DESTINATION)
        )

        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory()
                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalGlasenseContentColor provides AppColors.content
                ) {
                    when (destination) {
                        SettingsDestination.SETTINGS -> SettingsScreen()
                        SettingsDestination.APPEARANCE -> AppearanceScreen()
                        SettingsDestination.AI -> AIScreen()
                        SettingsDestination.DATA_STORAGE -> DataStorageScreen()
                        SettingsDestination.GENERAL -> GeneralScreen()
                        SettingsDestination.ABOUT -> AboutScreen()
                        SettingsDestination.CREDITS -> CreditsScreen()
                        SettingsDestination.ACCOUNT -> AccountScreen()
                    }
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}
