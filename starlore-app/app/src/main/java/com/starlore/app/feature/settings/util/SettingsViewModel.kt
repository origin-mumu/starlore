package com.starlore.app.feature.settings.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.launch
import com.starlore.app.data.api.AuthApi
import com.starlore.app.data.api.UpdateProfileRequest

class SettingsViewModel : ViewModel() {

    val colorMode = SettingsManager.colorModeState
    val isCustomPrimaryColorEnabled = SettingsManager.isCustomPrimaryColorEnabledState
    val isUseDynamicColor = SettingsManager.isUseDynamicColorState
    val isLiteMode = SettingsManager.isLiteModeState
    val isLiquidGlass = SettingsManager.isLiquidGlassState
    val themePrimaryColor = SettingsManager.themePrimaryColorState
    val isDueTodayMarker = SettingsManager.isDueTodayMarkerState
    val isOverdueMarker = SettingsManager.isOverdueMarkerState
    val isCompletionSoundEnabled = SettingsManager.isCompletionSoundEnabledState
    val isEasterEggEnabled = SettingsManager.isEasterEggState
    val isSuperGraphicUltraModernGirlEnabled = SettingsManager.isSuperGraphicUltraModernGirlState
    val hasReturnedToTodayByTitle = SettingsManager.hasReturnedToTodayByTitleState
    val isExtractScreenQuickTileEnabled = SettingsManager.isExtractScreenQuickTileEnabledState
    val isAutoAddToSystemCalendar = SettingsManager.isAutoAddToSystemCalendarState
    val isCheckUpdatesOnStartup = SettingsManager.isCheckUpdatesOnStartupState
    val appIcon = SettingsManager.appIconState

    fun onCustomPrimaryColorChanged(isEnabled: Boolean) {
        SettingsManager.isCustomPrimaryColorEnabled = isEnabled
    }

    fun onUseDynamicColorChanged(isEnabled: Boolean) {
        SettingsManager.isUseDynamicColor = isEnabled
        if (isEnabled) {
            SettingsManager.isCustomPrimaryColorEnabled = false
        }
    }

    fun onLiteModeChanged(isEnabled: Boolean) {
        SettingsManager.isLiteMode = isEnabled
    }

    fun onLiquidGlassChanged(isEnabled: Boolean) {
        SettingsManager.isLiquidGlass = isEnabled
    }

    fun colorMode(mode: Int) {
        SettingsManager.colorMode = mode
    }

    fun onThemePrimaryColorChanged(colorArgb: Int) {
        SettingsManager.themePrimaryColor = colorArgb
    }

    fun onDueTodayMarkerChanged(isEnabled: Boolean) {
        SettingsManager.isDueTodayMarker = isEnabled
    }

    fun onOverdueMarkerChanged(isEnabled: Boolean) {
        SettingsManager.isOverdueMarker = isEnabled
    }

    fun onCompletionSoundChanged(isEnabled: Boolean) {
        SettingsManager.isCompletionSoundEnabled = isEnabled
    }

    fun unlockEasterEgg() {
        SettingsManager.isEasterEggEnabled = true
    }

    fun onSuperGraphicUltraModernGirlChanged(isEnabled: Boolean) {
        SettingsManager.isSuperGraphicUltraModernGirlEnabled = isEnabled
    }

    fun setHasReturnedToTodayByTitle(value: Boolean) {
        SettingsManager.hasReturnedToTodayByTitle = value
    }

    fun onExtractScreenQuickTileChanged(isEnabled: Boolean) {
        SettingsManager.isExtractScreenQuickTileEnabled = isEnabled
    }

    fun onAutoAddToSystemCalendarChanged(isEnabled: Boolean) {
        SettingsManager.isAutoAddToSystemCalendar = isEnabled
    }

    fun onCheckUpdatesOnStartupChanged(isEnabled: Boolean) {
        SettingsManager.isCheckUpdatesOnStartup = isEnabled
    }

    fun onAppIconChanged(context: Context, icon: AppIconManager.AppIcon) {
        SettingsManager.appIcon = icon
        AppIconManager.setIcon(context, icon)
    }

    fun fetchUserProfile(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val authApi = org.koin.core.context.GlobalContext.get().get<AuthApi>()
                val response = authApi.getCurrentUser()
                val user = response.data
                if (user != null) {
                    SettingsManager.userNickname = user.nickname
                    SettingsManager.userUsername = user.username
                    SettingsManager.userRole = user.role
                    SettingsManager.userAvatar = user.avatar ?: ""
                    SettingsManager.userBio = user.bio ?: ""
                    SettingsManager.userEmail = user.email ?: ""
                    SettingsManager.userLocation = user.location ?: ""
                    SettingsManager.userWebsite = user.website ?: ""
                    SettingsManager.userGithub = user.github ?: ""
                }
            } catch (e: Exception) {
                // Ignore fetch error in background
            } finally {
                onComplete()
            }
        }
    }

    fun updateProfile(
        nickname: String,
        bio: String,
        email: String,
        location: String,
        website: String,
        github: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authApi = org.koin.core.context.GlobalContext.get().get<AuthApi>()
                val response = authApi.updateProfile(
                    UpdateProfileRequest(
                        nickname = nickname,
                        bio = bio,
                        email = email,
                        location = location,
                        website = website,
                        github = github
                    )
                )
                val user = response.data
                if (user != null) {
                    SettingsManager.userNickname = user.nickname
                    SettingsManager.userUsername = user.username
                    SettingsManager.userRole = user.role
                    SettingsManager.userAvatar = user.avatar ?: ""
                    SettingsManager.userBio = user.bio ?: ""
                    SettingsManager.userEmail = user.email ?: ""
                    SettingsManager.userLocation = user.location ?: ""
                    SettingsManager.userWebsite = user.website ?: ""
                    SettingsManager.userGithub = user.github ?: ""
                    onSuccess()
                } else {
                    onFailure(response.message ?: "Failed to update profile")
                }
            } catch (e: Exception) {
                onFailure(e.message ?: "Network error")
            }
        }
    }
}
