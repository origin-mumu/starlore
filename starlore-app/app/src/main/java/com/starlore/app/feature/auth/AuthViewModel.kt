package com.starlore.app.feature.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starlore.app.data.api.AuthApi
import com.starlore.app.data.api.LoginRequest
import com.starlore.app.data.api.RegisterRequest
import com.starlore.app.feature.settings.util.SettingsManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val username: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(private val authApi: AuthApi) : ViewModel() {

    var uiState = mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    var username = mutableStateOf("")
    var password = mutableStateOf("")
    var email = mutableStateOf("")
    var nickname = mutableStateOf("")

    var isRegisterMode = mutableStateOf(false)

    private val _navigationFlow = MutableSharedFlow<Boolean>()
    val navigationFlow = _navigationFlow.asSharedFlow()

    fun submit() {
        if (isRegisterMode.value) {
            register()
        } else {
            login()
        }
    }

    private fun login() {
        val user = username.value.trim()
        val pwd = password.value.trim()

        if (user.isEmpty() || pwd.isEmpty()) {
            uiState.value = AuthUiState.Error("Username and password cannot be empty")
            return
        }

        uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = authApi.login(LoginRequest(user, pwd))
                val result = response.data
                if (result != null) {
                    SettingsManager.authToken = result.token
                    SettingsManager.userUsername = result.user.username
                    SettingsManager.userNickname = result.user.nickname
                    SettingsManager.userRole = result.user.role
                    SettingsManager.userAvatar = result.user.avatar ?: ""
                    
                    uiState.value = AuthUiState.Success(result.user.nickname)
                    _navigationFlow.emit(true)
                } else {
                    uiState.value = AuthUiState.Error(response.message ?: "Authentication failed")
                }
            } catch (e: Exception) {
                uiState.value = AuthUiState.Error(e.message ?: "Network error occurred")
            }
        }
    }

    private fun register() {
        val user = username.value.trim()
        val pwd = password.value.trim()
        val mail = email.value.trim()
        val nick = nickname.value.trim()

        if (user.isEmpty() || pwd.isEmpty()) {
            uiState.value = AuthUiState.Error("Username and password cannot be empty")
            return
        }

        uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = authApi.register(
                    RegisterRequest(
                        username = user,
                        password = pwd,
                        email = mail.takeIf { it.isNotEmpty() },
                        nickname = nick.takeIf { it.isNotEmpty() }
                    )
                )
                val result = response.data
                if (result != null) {
                    SettingsManager.authToken = result.token
                    SettingsManager.userUsername = result.user.username
                    SettingsManager.userNickname = result.user.nickname
                    SettingsManager.userRole = result.user.role
                    SettingsManager.userAvatar = result.user.avatar ?: ""

                    uiState.value = AuthUiState.Success(result.user.nickname)
                    _navigationFlow.emit(true)
                } else {
                    uiState.value = AuthUiState.Error(response.message ?: "Registration failed")
                }
            } catch (e: Exception) {
                uiState.value = AuthUiState.Error(e.message ?: "Network error occurred")
            }
        }
    }
}
