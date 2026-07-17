package com.starlore.app.data.api

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.http.*

@Serializable
data class AiModelInfo(
    val id: String,
    val name: String,
    val configured: Boolean
)

@Serializable
data class CharacterCard(
    val key: String,
    val name: String,
    val description: String,
    val systemPrompt: String
)

@Serializable
data class AiSessionRow(
    val id: Int,
    val title: String,
    val characterKey: String,
    val modelId: String,
    // Some Java backend responses embed a compact session object without timestamps.
    // Defaults keep those valid responses backward-compatible with the app model.
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class AiMessageRow(
    val id: Int,
    val role: String, // "user" | "assistant" | "system"
    val content: String,
    val agentTrace: String? = null,
    val createdAt: String = ""
)

@Serializable
data class AiChatRequestMessage(
    val role: String,
    val content: String
)

@Serializable
data class AppendChatRequest(
    val userContent: String,
    val assistantContent: String,
    val agentTrace: String? = null
)

@Serializable
data class CreateSessionRequest(
    val title: String? = null,
    val characterKey: String? = null,
    val modelId: String? = null
)

@Serializable
data class UpdateSessionRequest(
    val title: String? = null,
    val characterKey: String? = null,
    val modelId: String? = null
)

@Serializable
data class AiQuota(
    val dailyLimit: Int,
    val used: Int,
    val remaining: Int,
    val isAdmin: Boolean
)

@Serializable
data class AiQuotaResponse(
    val data: AiQuota
)

@Serializable
data class SessionMessagesResponse(
    val success: Boolean,
    val session: AiSessionRow,
    val messages: List<AiMessageRow>
)

@Serializable
data class SessionListResponse(
    val success: Boolean,
    val sessions: List<AiSessionRow>
)

@Serializable
data class SessionCreateResponse(
    val success: Boolean,
    val session: AiSessionRow
)

@Serializable
data class CharacterCardsResponse(
    val success: Boolean,
    val cards: List<CharacterCard>
)

@Serializable
data class AiModelsResponse(
    val success: Boolean? = true,
    val models: List<AiModelInfo>
)

@Serializable
data class CommonSuccessResponse(
    val success: Boolean
)

interface AiApi {
    @GET("ai/models")
    suspend fun getAiModels(): AiModelsResponse

    @GET("ai/character-cards")
    suspend fun getCharacterCards(): CharacterCardsResponse

    @GET("ai/sessions")
    suspend fun listAiSessions(): SessionListResponse

    @POST("ai/sessions")
    suspend fun createAiSession(@Body body: CreateSessionRequest): SessionCreateResponse

    @PATCH("ai/sessions/{id}")
    suspend fun updateAiSession(@Path("id") id: Int, @Body body: UpdateSessionRequest): CommonSuccessResponse

    @DELETE("ai/sessions/{id}")
    suspend fun deleteAiSession(@Path("id") id: Int): CommonSuccessResponse

    @GET("ai/sessions/{id}/messages")
    suspend fun getSessionMessages(@Path("id") id: Int): SessionMessagesResponse

    @POST("ai/sessions/{sessionId}/append")
    suspend fun appendChatPair(
        @Path("sessionId") sessionId: Int,
        @Body body: AppendChatRequest
    ): CommonSuccessResponse

    @GET("ai/quota")
    suspend fun getAiQuota(): AiQuotaResponse

    @Streaming
    @POST("ai/multi-agent-sse")
    suspend fun multiAgentSse(
        @Query("model") model: String,
        @Body messages: List<AiChatRequestMessage>
    ): ResponseBody

    @POST("ai/diverge")
    suspend fun diverge(
        @Body body: Map<String, String>
    ): DivergeResponse
}

@Serializable
data class DivergePair(
    val zh: String,
    val en: String
)

@Serializable
data class DivergeResponse(
    val pairs: List<DivergePair>
)
