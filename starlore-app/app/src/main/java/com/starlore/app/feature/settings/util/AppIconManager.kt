package com.starlore.app.feature.settings.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import com.starlore.app.R

object AppIconManager {
    private const val ALIAS_DEFAULT = "com.starlore.app.AppIconDefault"
    private const val ALIAS_APRICOT = "com.starlore.app.AppIconApricot"
    private const val ALIAS_KIWI = "com.starlore.app.AppIconKiwi"
    private const val ALIAS_BLUEBERRY = "com.starlore.app.AppIconBlueberry"
    private const val ALIAS_PEACH = "com.starlore.app.AppIconPeach"


    enum class AppIcon(
        val alias: String,
        @StringRes val displayNameResId: Int,
        val mipmapResId: Int
    ) {
        DEFAULT(
            ALIAS_DEFAULT,
            R.string.app_icon_default,
            R.drawable.app_icon_default
        ),
        APRICOT(
            ALIAS_APRICOT,
            R.string.app_icon_apricot,
            R.drawable.app_icon_apricot
        ),
        BLUEBERRY(
            ALIAS_BLUEBERRY,
            R.string.app_icon_blueberry,
            R.drawable.app_icon_blueberry
        ),
        KIWI(
            ALIAS_KIWI,
            R.string.app_icon_kiwi,
            R.drawable.app_icon_kiwi
        ),
        PEACH(
            ALIAS_PEACH,
            R.string.app_icon_peach,
            R.drawable.app_icon_peach
        )
    }

    fun setIcon(context: Context, icon: AppIcon) {
        try {
            val pm = context.packageManager

            AppIcon.entries.forEach { entry ->
                val component = ComponentName(context, entry.alias)
                val state = when {
                    entry == icon -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    // Keep the default icon in its manifest-declared state as a fallback,
                    // so the app can always be launched even if something goes wrong.
                    entry == AppIcon.DEFAULT -> PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(
                    component,
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (_: Exception) {
            // Silently ignore to prevent crash; the icon simply won't change.
        }
    }

    fun getCurrentIcon(context: Context): AppIcon {
        return try {
            val pm = context.packageManager
            AppIcon.entries.firstOrNull { entry ->
                val component = ComponentName(context, entry.alias)
                val setting = pm.getComponentEnabledSetting(component)
                setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } ?: AppIcon.DEFAULT
        } catch (_: Exception) {
            AppIcon.DEFAULT
        }
    }
}