package com.starlore.app

import android.app.Application
import com.starlore.app.data.di.starloreAppModule
import com.starlore.app.data.todo.appModule
import com.tencent.mmkv.MMKV
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

import android.content.ComponentName
import android.content.pm.PackageManager

class StarloreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        startKoin {
            androidContext(this@StarloreApplication)
            modules(starloreAppModule, appModule)
        }
        ensureDefaultIconNotDisabled()
    }

    /**
     * Safety net: if a previous icon switch left the default alias disabled,
     * restore it to its manifest-declared (DEFAULT) state so the app remains launchable.
     */
    private fun ensureDefaultIconNotDisabled() {
        try {
            val defaultAlias = ComponentName(this, "com.starlore.app.AppIconDefault")
            val state = packageManager.getComponentEnabledSetting(defaultAlias)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                packageManager.setComponentEnabledSetting(
                    defaultAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (_: Exception) {
            // Ignore — best-effort recovery
        }
    }
}
