package com.starlore.app.feature.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starlore.app.data.api.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ChatViewModel(private val aiApi: AiApi) : ViewModel() {

    var activeSessionId = mutableStateOf<Int?>(null)
    val messages = mutableStateListOf<AiMessageRow>()
    val sessions = mutableStateListOf<AiSessionRow>()
    val characterCards = mutableStateListOf<CharacterCard>()

    var isSending = mutableStateOf(false)
    var isLoadingSession = mutableStateOf(false)
    var inputText = mutableStateOf("")
    var selectedCharacterKey = mutableStateOf("default")
    var quota = mutableStateOf<AiQuota?>(null)
    var errorMessage = mutableStateOf<String?>(null)

    init {
        initSession()
    }

    private suspend fun getOrCreateSessionId(): Int? {
        val currentId = activeSessionId.value
        if (currentId != null) return currentId

        try {
            val sessionsResponse = aiApi.listAiSessions()
            val id = if (sessionsResponse.sessions.isNotEmpty()) {
                sessionsResponse.sessions.first().id
            } else {
                aiApi.createAiSession(CreateSessionRequest(title = "移动端对话")).session.id
            }
            activeSessionId.value = id
            return id
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to initialize active session", e)
            return null
        }
    }

    private fun initSession() {
        viewModelScope.launch {
            try {
                characterCards.addAll(aiApi.getCharacterCards().cards)
                refreshQuota()
                refreshSessionsInternal()
                val id = sessions.firstOrNull()?.id ?: createSessionInternal()
                if (id != null) selectSessionInternal(id)
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "无法加载会话"
            }
        }
    }

    fun refreshSessions() {
        viewModelScope.launch {
            try {
                refreshSessionsInternal()
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "无法刷新会话"
            }
        }
    }

    private suspend fun refreshSessionsInternal() {
        sessions.clear()
        sessions.addAll(aiApi.listAiSessions().sessions)
    }

    private suspend fun createSessionInternal(): Int? {
        val session = aiApi.createAiSession(
            CreateSessionRequest(
                characterKey = selectedCharacterKey.value,
                modelId = "deepseek-v4-flash"
            )
        ).session
        refreshSessionsInternal()
        return session.id
    }

    fun newSession() {
        if (isSending.value) return
        viewModelScope.launch {
            try {
                val id = createSessionInternal() ?: return@launch
                selectSessionInternal(id)
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "无法新建会话"
            }
        }
    }

    fun selectSession(sessionId: Int) {
        if (isSending.value || activeSessionId.value == sessionId) return
        viewModelScope.launch { selectSessionInternal(sessionId) }
    }

    private suspend fun selectSessionInternal(sessionId: Int) {
        isLoadingSession.value = true
        try {
            val response = aiApi.getSessionMessages(sessionId)
            activeSessionId.value = sessionId
            selectedCharacterKey.value = response.session.characterKey
            messages.clear()
            messages.addAll(
                response.messages
                    .filter { it.role == "user" || it.role == "assistant" }
                    .map { it.copy(agentTrace = formatStoredTrace(it.agentTrace)) }
            )
        } catch (e: Exception) {
            errorMessage.value = e.message ?: "无法打开会话"
        } finally {
            isLoadingSession.value = false
        }
    }

    fun deleteSession(sessionId: Int) {
        if (isSending.value) return
        viewModelScope.launch {
            try {
                aiApi.deleteAiSession(sessionId)
                refreshSessionsInternal()
                if (activeSessionId.value == sessionId) {
                    val nextId = sessions.firstOrNull()?.id ?: createSessionInternal()
                    if (nextId != null) selectSessionInternal(nextId) else messages.clear()
                }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "无法删除会话"
            }
        }
    }

    fun selectCharacter(key: String) {
        if (isSending.value || selectedCharacterKey.value == key) return
        selectedCharacterKey.value = key
        activeSessionId.value?.let { sessionId ->
            viewModelScope.launch {
                try {
                    aiApi.updateAiSession(sessionId, UpdateSessionRequest(characterKey = key))
                    refreshSessionsInternal()
                } catch (e: Exception) {
                    errorMessage.value = e.message ?: "无法切换助手"
                }
            }
        }
    }

    private suspend fun refreshQuota() {
        try {
            quota.value = aiApi.getAiQuota().data
        } catch (_: Exception) {
            quota.value = null
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun sendMessage() {
        val text = inputText.value.trim()
        if (text.isEmpty() || isSending.value) return

        inputText.value = ""
        isSending.value = true

        val userMessage = AiMessageRow(
            id = -1,
            role = "user",
            content = text,
            createdAt = ""
        )
        messages.add(userMessage)

        val assistantMessageIndex = messages.size
        val assistantPlaceholder = AiMessageRow(
            id = -2,
            role = "assistant",
            content = "",
            createdAt = ""
        )
        messages.add(assistantPlaceholder)

        viewModelScope.launch {
            val sessionId = getOrCreateSessionId()
            if (sessionId == null) {
                messages[assistantMessageIndex] = assistantPlaceholder.copy(
                    content = "无法初始化对话会话，请确认网络连接或重新登录。"
                )
                isSending.value = false
                return@launch
            }
            try {
                // Compile past history messages payload
                val payload = messages.dropLast(2).map {
                    AiChatRequestMessage(role = it.role, content = it.content)
                } + AiChatRequestMessage(role = "user", content = text)

                val responseBody = aiApi.multiAgentSse(
                    model = "deepseek-v4-flash",
                    messages = payload
                )

                var assistantContent = ""
                val traceBuilder = StringBuilder()
                var planSummary = ""
                var subtaskCount = 0
                val subtaskDescriptions = mutableMapOf<Int, String>()
                val completedSubtasks = mutableSetOf<Int>()
                var reviewDecision = ""
                var reviewFeedback = ""
                var retryCount = 0

                ApiClient.parseSseFlow(responseBody)
                    .onCompletion {
                        isSending.value = false
                    }
                    .collect { sseEvent ->
                        val dataJson = try {
                            ApiClient.json.parseToJsonElement(sseEvent.data) as? JsonObject
                        } catch (_: Exception) {
                            null
                        }

                        val eventType = dataJson?.get("type")?.jsonPrimitive?.content ?: sseEvent.event

                        when (eventType) {
                            "plan_start" -> {
                                traceBuilder.append("📍 规划中...\n\n")
                                updateAssistantMessage(assistantMessageIndex, assistantContent, traceBuilder.toString())
                            }
                            "plan" -> {
                                traceBuilder.append("📍 Planner 规划计划:\n")
                                val summary = dataJson?.get("summary")?.jsonPrimitive?.content ?: ""
                                planSummary = summary
                                val subtasks = dataJson?.get("subtasks")
                                val subtaskArray = subtasks as? JsonArray
                                if (subtaskArray != null) {
                                    subtaskCount = subtaskArray.size
                                    subtaskArray.forEachIndexed { index, element ->
                                        val item = element as? JsonObject ?: return@forEachIndexed
                                        val id = item["id"]?.jsonPrimitive?.intOrNull ?: index + 1
                                        val description = item["description"]?.jsonPrimitive?.content.orEmpty()
                                        subtaskDescriptions[id] = description
                                    }
                                } else {
                                    subtaskCount = subtasks?.jsonPrimitive?.intOrNull ?: 0
                                }
                                if (summary.isNotEmpty()) {
                                    traceBuilder.append(summary).append("\n\n")
                                } else {
                                    traceBuilder.append(sseEvent.data).append("\n\n")
                                }
                                updateAssistantMessage(assistantMessageIndex, assistantContent, traceBuilder.toString())
                            }
                            "subtask_start" -> {
                                val node = dataJson?.get("node")?.jsonPrimitive?.content ?: ""
                                traceBuilder.append("⚡ Executor 执行子任务: $node\n")
                                updateAssistantMessage(assistantMessageIndex, assistantContent, traceBuilder.toString())
                            }
                            "subtask_result" -> {
                                (dataJson?.get("subtask_id") ?: dataJson?.get("id"))
                                    ?.jsonPrimitive?.intOrNull?.let(completedSubtasks::add)
                                val result = dataJson?.get("result")?.jsonPrimitive?.content ?: ""
                                traceBuilder.append("✅ 子任务完成反馈: ")
                                traceBuilder.append(result.take(150)).append("...\n\n")
                                updateAssistantMessage(assistantMessageIndex, assistantContent, traceBuilder.toString())
                            }
                            "review" -> {
                                val decision = dataJson?.get("decision")?.jsonPrimitive?.content ?: ""
                                val feedback = dataJson?.get("feedback")?.jsonPrimitive?.content ?: ""
                                reviewDecision = decision
                                reviewFeedback = feedback
                                retryCount = dataJson?.get("retry_count")?.jsonPrimitive?.intOrNull ?: 0
                                traceBuilder.append("🔍 Reviewer 审查决策: $decision ($feedback)\n\n")
                                updateAssistantMessage(assistantMessageIndex, assistantContent, traceBuilder.toString())
                            }
                            "content" -> {
                                val textPart = dataJson?.get("content")?.jsonPrimitive?.content ?: ""
                                assistantContent += textPart
                                updateAssistantMessage(assistantMessageIndex, assistantContent, traceBuilder.toString())
                            }
                            "error" -> {
                                val errorMsg = dataJson?.get("error")?.jsonPrimitive?.content ?: ""
                                assistantContent = "对话发生错误: $errorMsg"
                                updateAssistantMessage(assistantMessageIndex, assistantContent, traceBuilder.toString())
                                isSending.value = false
                            }
                            "done" -> {
                                aiApi.appendChatPair(
                                    sessionId = sessionId,
                                    body = AppendChatRequest(
                                        userContent = text,
                                        assistantContent = assistantContent,
                                        agentTrace = buildStoredTrace(
                                            planSummary = planSummary,
                                            subtaskCount = subtaskCount,
                                            subtaskDescriptions = subtaskDescriptions,
                                            completedSubtasks = completedSubtasks,
                                            reviewDecision = reviewDecision,
                                            reviewFeedback = reviewFeedback,
                                            retryCount = retryCount
                                        )
                                    )
                                )
                                refreshSessionsInternal()
                                refreshQuota()
                                isSending.value = false
                            }
                        }
                    }
            } catch (e: Exception) {
                isSending.value = true
                val recovered = recoverSavedAnswer(sessionId)
                if (!recovered) {
                    val hasUsableStreamedAnswer = assistantMessageIndex < messages.size &&
                        messages[assistantMessageIndex].content.isNotBlank()
                    if (!hasUsableStreamedAnswer) {
                        if (assistantMessageIndex < messages.size) messages.removeAt(assistantMessageIndex)
                        errorMessage.value = "连接中断，回答可能仍在生成，请稍后重试"
                    } else {
                        // The answer reached the UI before HTTP/2 closed the stream. Keep it;
                        // transport completion is not a user-visible failure.
                        errorMessage.value = null
                    }
                }
                isSending.value = false
            }
        }
    }

    /**
     * HTTP/2 may reset an SSE stream after the server has already persisted the answer.
     * Re-read the session before presenting a failure so a completed answer is never
     * replaced by a transport error.
     */
    private suspend fun recoverSavedAnswer(sessionId: Int): Boolean {
        repeat(3) { attempt ->
            delay(700L + attempt * 800L)
            try {
                val response = aiApi.getSessionMessages(sessionId)
                val restored = response.messages
                    .filter { it.role == "user" || it.role == "assistant" }
                    .map { it.copy(agentTrace = formatStoredTrace(it.agentTrace)) }
                if (restored.lastOrNull()?.role == "assistant" && restored.last().content.isNotBlank()) {
                    activeSessionId.value = sessionId
                    messages.clear()
                    messages.addAll(restored)
                    refreshSessionsInternal()
                    refreshQuota()
                    errorMessage.value = null
                    return true
                }
            } catch (_: Exception) {
                // Retry briefly: persistence can complete just after the stream closes.
            }
        }
        return false
    }

    private fun updateAssistantMessage(index: Int, content: String, trace: String) {
        if (index < messages.size) {
            messages[index] = messages[index].copy(
                content = content,
                agentTrace = trace.takeIf { it.isNotEmpty() }
            )
        }
    }

    private fun buildStoredTrace(
        planSummary: String,
        subtaskCount: Int,
        subtaskDescriptions: Map<Int, String>,
        completedSubtasks: Set<Int>,
        reviewDecision: String,
        reviewFeedback: String,
        retryCount: Int
    ): String = buildJsonObject {
        put("planSummary", planSummary)
        put("subtasks", buildJsonArray {
            repeat(subtaskCount) { index ->
                val id = index + 1
                add(buildJsonObject {
                    put("id", id)
                    put("desc", subtaskDescriptions[id].orEmpty())
                    put("status", if (id in completedSubtasks) "done" else "pending")
                })
            }
        })
        put("reviewDecision", reviewDecision)
        put("reviewFeedback", reviewFeedback)
        put("retryCount", retryCount)
    }.toString()

    private fun formatStoredTrace(trace: String?): String? {
        if (trace.isNullOrBlank() || !trace.trimStart().startsWith("{")) return trace
        val json = try {
            ApiClient.json.parseToJsonElement(trace) as? JsonObject
        } catch (_: Exception) {
            null
        } ?: return trace

        val summary = json["planSummary"]?.jsonPrimitive?.content.orEmpty()
        val decision = json["reviewDecision"]?.jsonPrimitive?.content.orEmpty()
        val feedback = json["reviewFeedback"]?.jsonPrimitive?.content.orEmpty()
        return buildString {
            if (summary.isNotEmpty()) append("📋 Planner 规划计划:\n").append(summary).append("\n\n")
            if (decision.isNotEmpty()) {
                append("🔎 Reviewer 审查决策: ").append(decision)
                if (feedback.isNotEmpty()) append(" (").append(feedback).append(')')
            }
        }.takeIf { it.isNotEmpty() }
    }
}
