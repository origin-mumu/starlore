package com.starlore.app.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.*

@Serializable
data class UserInfo(
    val id: Int,
    val username: String,
    val nickname: String,
    val email: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val website: String? = null,
    val github: String? = null,
    val role: String
)

@Serializable
data class AuthResult(
    val token: String,
    val user: UserInfo
)

@Serializable
data class ApiResponse<T>(
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    val nickname: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    val email: String? = null,
    val location: String? = null,
    val website: String? = null,
    val github: String? = null
)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResult>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResult>

    @GET("auth/me")
    suspend fun getCurrentUser(): ApiResponse<UserInfo>

    @PUT("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<UserInfo>
}
