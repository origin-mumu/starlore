<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getAuthToken } from '@/utils/authToken'
import ImmersiveMode from '@/components/ImmersiveMode.vue'
const userStore = useUserStore()
import {
  appendChatPair,
  buildMultiAgentSseUrl,
  createAiSession,
  deleteAiSession,
  evaluateRag,
  getAgentConfig,
  getAiModels,
  getAiQuota,
  getCharacterCards,
  getSessionMessages,
  listAiSessions,
  updateAgentConfig,
  updateAiSession,
  type AiSessionRow,
  type CharacterCard,
  type RagEvaluationResult,
} from '@/api/ai'
import { getGuestQuota } from '@/api/guest-ai'

const router = useRouter()

function goHome() {
  router.push({ path: '/' })
}

type ChatMsg = {
  role: 'user' | 'assistant'
  content: string
  reasoningContent?: string
  imageUrl?: string
  agentTrace?: AgentTrace
}

type AgentTrace = {
  planSummary: string
  subtasks: { id: number; desc: string; status: 'pending' | 'running' | 'done' }[]
  reviewDecision: string
  reviewFeedback: string
  retryCount: number
  metrics: { tokensIn: number; tokensOut: number; latencyMs: number } | null
  ragContexts?: { articleId: number; title: string }[]
  ragRetrievalMode?: 'vector' | 'keyword'
  ragEvaluation?: RagEvaluationResult
  ragEvaluationStatus?: 'pending' | 'complete' | 'failed'
  ragEvaluationError?: string
}

const characterCards = ref<CharacterCard[]>([])
const sessions = ref<AiSessionRow[]>([])
const currentSessionId = ref<number | null>(null)
const selectedCharacterKey = ref('default')
const messages = ref<ChatMsg[]>([])
const inputText = ref('')
const isSending = ref(false)
const connectionError = ref('')
const activeTab = ref<'chat' | 'sessions'>('chat')
const fileInputRef = ref<HTMLInputElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)
const chatScrollRef = ref<HTMLElement | null>(null)
const charPickerOpen = ref(false)
const toolStatus = ref<string | null>(null)

/* ─── Agent 检索调参 Drawer ─── */
const agentConfigDrawerOpen = ref(false)
const agentConfigLoading = ref(false)
const agentConfigForm = reactive({
  modelName: 'deepseek-chat',
  similarityThreshold: 0.6,
  topK: 5,
  temperature: 0.7,
  enableRerank: 1
})

const availableModels = ref<{ id: string; name: string; configured?: boolean }[]>([
  { id: 'deepseek-v4-flash', name: 'DeepSeek V4 Flash' },
  { id: 'mimo', name: '小米 MiMo' }
])

async function openAgentConfigDrawer() {
  agentConfigDrawerOpen.value = true
  agentConfigLoading.value = true
  try {
    const modelRes = await getAiModels()
    if (modelRes && modelRes.models && modelRes.models.length) {
      availableModels.value = modelRes.models.filter(m => !m.id.includes('embedding'))
    }
  } catch {}
  try {
    const res = await getAgentConfig()
    if (res.success && res.data) {
      agentConfigForm.modelName = res.data.modelName || 'glm-4-flash'
      agentConfigForm.similarityThreshold = res.data.similarityThreshold ?? 0.6
      agentConfigForm.topK = res.data.topK ?? 5
      agentConfigForm.temperature = res.data.temperature ?? 0.7
      agentConfigForm.enableRerank = res.data.enableRerank ?? 1
    }
  } catch {}
  agentConfigLoading.value = false
}

async function saveAgentConfig() {
  try {
    await updateAgentConfig(agentConfigForm)
    agentConfigDrawerOpen.value = false
    ElMessage.success('Agent 检索参数保存成功！')
  } catch (e: any) {
    ElMessage.error(e.message || '保存配置失败')
  }
}


/* ─── 多 Agent 追踪折叠状态 ─── */
const traceCollapsed = ref<Record<number, boolean>>({})
const reasoningCollapsed = ref<Record<number, boolean>>({})
const hasReceivedContent = ref(false)

/* ─── 沉浸模式 ─── */
const immersiveActive = ref(true)

async function runAutomaticRagEvaluation(trace: AgentTrace, question: string, answer: string) {
  if (!trace.ragContexts?.length || !question.trim() || !answer.trim()) return
  trace.ragEvaluationStatus = 'pending'
  trace.ragEvaluationError = ''
  try {
    trace.ragEvaluation = await evaluateRag({
      question,
      answer,
      articleIds: trace.ragContexts.map(item => item.articleId),
    })
    trace.ragEvaluationStatus = 'complete'
  } catch (error: any) {
    trace.ragEvaluationStatus = 'failed'
    trace.ragEvaluationError =
      error?.response?.data?.message || error?.message || '自动质量评估失败'
  }
}

function exitImmersive() {
  router.push({ path: '/' })
}

function handleImmersiveImageUpload(base64: string) {
  pendingImage.value = base64
  pendingImagePreview.value = base64
}

function handleImmersiveTxtUpload(text: string) {
  inputText.value = (inputText.value ? `${inputText.value}\n\n` : '') + text
}

async function onImmersiveSend(userContent: string, assistantContent: string, agentTrace?: string) {
  const sid = currentSessionId.value
  if (sid == null) return
  try {
    await appendChatPair(sid, userContent, assistantContent, agentTrace)
    await refreshSessions()
  } catch {
    /* ignore persistence errors */
  }
}

/** 流式用 auto 紧跟光标；平时用 smooth */
function scrollChatToBottom(behavior: ScrollBehavior = 'auto', force = false) {
  nextTick(() => {
    requestAnimationFrame(() => {
      const el = chatScrollRef.value
      if (!el || activeTab.value !== 'chat') return
      const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight
      if (force || distanceFromBottom < 400) {
        el.scrollTo({ top: el.scrollHeight, behavior })
      }
    })
  })
}

watch(
  messages,
  () => {
    scrollChatToBottom('smooth')
  },
  { deep: true, immediate: true }
)

watch(toolStatus, () => {
  scrollChatToBottom('smooth')
})

/* ─── 图片上传 ─── */
const pendingImage = ref<string | null>(null) // base64
const pendingImagePreview = ref<string | null>(null) // preview URL
const lastImageDescription = ref('') // MiMo 识别结果
const imageRecognitionContent = ref('') // 流式识别内容
const imageRecognitionCollapsed = ref(false) // 识别结果是否折叠

/* ─── 语音输入 ─── */
const isListening = ref(false)
const speechSupported = ref(false)

/* ─── 每日对话上限（走后端鉴权） ─── */
const dailyRemaining = ref(10)
const dailyLimit = ref(10)
const isAdminUser = ref(false)
const dailyExceeded = computed(() => !isAdminUser.value && dailyRemaining.value <= 0)

async function refreshQuota() {
  try {
    if (userStore.isLoggedIn) {
      const res: any = await getAiQuota()
      const q = res.data
      dailyLimit.value = q.dailyLimit
      dailyRemaining.value = q.remaining
      isAdminUser.value = q.isAdmin
    } else {
      const res = await getGuestQuota()
      dailyLimit.value = res.limit
      dailyRemaining.value = res.remaining
      isAdminUser.value = false
    }
  } catch {
    // 离线或未登录时默认为 10
  }
}

function toggleReasoning(i: number) {
  reasoningCollapsed.value[i] = !reasoningCollapsed.value[i]
}

/* ── 多 Agent 节点中文映射 ── */
const agentNodeLabelMap: Record<string, string> = {
  planner: 'Planner 规划中：分析用户意图，拆解子任务...',
  executor: 'Executor 执行中：调用工具完成子任务...',
  reviewer: 'Reviewer 审查中：检查执行结果的完整性和准确性...',
  synthesizer: 'Synthesizer 合成中：整合结果生成最终回答...',
}

const toolLabelMap: Record<string, string> = {
  searchArticles: '正在搜索星记...',
  getArticleDetail: '正在获取星记详情...',
  getCategories: '正在获取星域列表...',
  getBlogStats: '正在获取知识库统计...',
  getRecentArticles: '正在获取最新星记...',
  writeArticle: '正在创建星记...',
  updateArticle: '正在更新星记...',
  deleteArticle: '正在删除星记...',
  getAllTags: '正在获取光痕列表...',
  getArticlesByCategory: '正在获取星域星记...',
  createCategory: '正在创建星域...',
}

let abortController: AbortController | null = null

const systemPrompt = computed(() => {
  const c = characterCards.value.find(x => x.key === selectedCharacterKey.value)
  return c?.systemPrompt ?? ''
})

function cancelStream() {
  abortController?.abort()
  abortController = null
}

function buildApiMessages(): { role: string; content: string }[] {
  const out: { role: string; content: string }[] = []
  const sys = systemPrompt.value.trim()
  if (sys) out.push({ role: 'system', content: sys })
  for (const m of messages.value) {
    if (m.role === 'assistant' && !m.content.trim()) continue
    // 带图片的用户消息：把图片描述拼入文字
    if (m.role === 'user' && m.imageUrl && lastImageDescription.value) {
      const text = `[用户上传了一张图片，图片内容：${lastImageDescription.value}]\n\n用户问题：${m.content}`
      out.push({ role: 'user', content: text })
      lastImageDescription.value = '' // 用完清空
    } else {
      out.push({ role: m.role, content: m.content })
    }
  }
  return out
}

async function ensureSession() {
  if (currentSessionId.value != null) return currentSessionId.value
  const { session } = await createAiSession({
    characterKey: selectedCharacterKey.value,
    modelId: 'deepseek-chat',
  })
  currentSessionId.value = session.id
  await refreshSessions()
  return session.id
}

async function refreshSessions() {
  try {
    const res = await listAiSessions()
    sessions.value = res.sessions || []
  } catch {
    sessions.value = []
  }
}

async function loadSession(id: number) {
  if (!userStore.isLoggedIn) return
  const res = await getSessionMessages(id)
  currentSessionId.value = id
  selectedCharacterKey.value = res.session.characterKey || 'default'
  messages.value = res.messages
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .map(m => {
      const msg: ChatMsg = { role: m.role as 'user' | 'assistant', content: m.content }
      if (m.agentTrace) {
        try { msg.agentTrace = JSON.parse(m.agentTrace) } catch { /* ignore */ }
      }
      return msg
    })
  activeTab.value = 'chat'
}

async function newSession() {
  if (!userStore.isLoggedIn) {
    alert('访客模式下无法新建云端会话，请登录开启您的专属 AI 空间！')
    return
  }
  const { session } = await createAiSession({
    characterKey: selectedCharacterKey.value,
    modelId: 'deepseek-chat',
  })
  currentSessionId.value = session.id
  messages.value = []
  await refreshSessions()
  activeTab.value = 'chat'
}

const deleteSessionId = ref<number | null>(null)
const showDeleteSession = ref(false)

function removeSession(id: number, e: Event) {
  e.stopPropagation()
  deleteSessionId.value = id
  showDeleteSession.value = true
}

async function confirmDeleteSession(emitId?: number) {
  const id = emitId ?? deleteSessionId.value
  if (id == null) return
  if (!userStore.isLoggedIn) {
    alert('访客模式下无法删除本地体验会话，请登录开启您的会话管理！')
    return
  }
  showDeleteSession.value = false
  await deleteAiSession(id)
  if (currentSessionId.value === id) {
    currentSessionId.value = null
    messages.value = []
  }
  await refreshSessions()
  if (!sessions.value.length) {
    await newSession()
  } else if (currentSessionId.value === null) {
    await loadSession(sessions.value[0].id)
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  const hasImage = !!pendingImage.value
  if ((!text && !hasImage) || isSending.value) return

  // 每日上限检查
  if (dailyExceeded.value) {
    connectionError.value = `今日 AI 对话次数已用尽（${dailyLimit.value}/${dailyLimit.value}），明天再来吧～`
    return
  }

  connectionError.value = ''
  let sid: number
  try {
    sid = await ensureSession()
  } catch {
    connectionError.value = '无法创建会话，请检查数据库与网络'
    return
  }

  const imageBase64 = pendingImage.value
  const userText = text || '请分析这张图片'

  // 先显示用户消息，不要等图片识别
  messages.value.push({ role: 'user', content: userText, imageUrl: imageBase64 || undefined })
  inputText.value = ''
  clearPendingImage()
  scrollChatToBottom('smooth', true)
  isSending.value = true
  hasReceivedContent.value = false
  imageRecognitionContent.value = ''
  imageRecognitionCollapsed.value = false
  messages.value.push({ role: 'assistant', content: '', reasoningContent: '' })
  const assistantIndex = messages.value.length - 1
  document.documentElement.classList.add('echobot-streaming')
  // 开始新对话前重置对话状态

  // 如果有图片，用 MiMo 流式识别（此时用户已看到自己的消息）
  let imageDescription = ''
  if (hasImage && imageBase64) {
    try {
      toolStatus.value = '正在识别图片...'
      imageRecognitionContent.value = ''
      imageRecognitionCollapsed.value = false
      const token = getAuthToken()
      const analyzeRes = await fetch('/api/ai/analyze-image/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ image: imageBase64, question: userText }),
      })

      if (!analyzeRes.ok) {
        throw new Error(`HTTP ${analyzeRes.status}`)
      }

      const reader = analyzeRes.body!.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed || !trimmed.startsWith('data:')) continue
          try {
            const data = JSON.parse(trimmed.slice(5).trim())
            if (data.error) {
              console.warn('图片识别失败：' + data.error)
              break
            }
            if (data.content) {
              imageRecognitionContent.value += data.content
              imageDescription += data.content
            }
            if (data.done) {
              lastImageDescription.value = imageDescription
            }
          } catch {
            /* ignore parse errors */
          }
        }
      }
    } catch (e) {
      console.warn('图片识别请求失败', e)
    }
    toolStatus.value = null
  }

  const history = buildApiMessages()

  try {
    cancelStream()
    abortController = new AbortController()
    toolStatus.value = null

    const token = getAuthToken()
    const authHeaders: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }

    // Multi-Agent 模式：Planner → Executor → Reviewer
    messages.value[assistantIndex].agentTrace = {
      planSummary: '',
      subtasks: [],
      reviewDecision: '',
      reviewFeedback: '',
      retryCount: 0,
      metrics: null,
    }
    toolStatus.value = '正在分析任务...'
    const response = await fetch(buildMultiAgentSseUrl('deepseek-v4-flash'), {
      method: 'POST',
      headers: authHeaders,
      body: JSON.stringify(history),
      signal: abortController.signal,
    })
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed || !trimmed.startsWith('data:')) continue
        try {
          const data = JSON.parse(trimmed.slice(5).trim())
          if (data.error) {
            messages.value[assistantIndex].content = `错误：${data.error}`
            reader.cancel()
            break
          }
          if (data.reasoning_content) {
            messages.value[assistantIndex].reasoningContent += data.reasoning_content
            toolStatus.value = null
          }
          if (data.content) {
            messages.value[assistantIndex].content += data.content
            hasReceivedContent.value = true
            toolStatus.value = null
          }
          if (data.tool_start) {
            toolStatus.value = toolLabelMap[data.tool_start] || `正在执行 ${data.tool_start}...`
          }
          if (data.type === 'rag_context') {
            const trace = messages.value[assistantIndex].agentTrace
            if (trace && Array.isArray(data.articles)) {
              const merged = [...(trace.ragContexts || []), ...data.articles]
              trace.ragContexts = Array.from(
                new Map(merged.map(item => [item.articleId, item])).values(),
              )
              trace.ragRetrievalMode = data.retrieval_mode === 'keyword' ? 'keyword' : 'vector'
            }
          }
          // ── 多 Agent 事件处理（写入消息的 agentTrace）──
          if (data.type === 'plan_start') {
            toolStatus.value = 'Planner 正在分析您的请求，拆解为可执行的子任务...'
          }
          if (data.type === 'plan') {
            const trace = messages.value[assistantIndex].agentTrace
            if (trace) {
              trace.planSummary = data.summary || ''
              if (Array.isArray(data.subtasks)) {
                trace.subtasks = data.subtasks.map((st: any) => ({
                  id: st.id,
                  desc: st.description || st.toolHint || '',
                  status: 'pending' as const,
                }))
              } else {
                const count = data.subtasks || 0
                trace.subtasks = Array.from({ length: count }, (_, i) => ({
                  id: i + 1,
                  desc: '',
                  status: 'pending' as const,
                }))
              }
            }
            toolStatus.value = `规划完成 → 共拆解为 ${trace?.subtasks.length || 0} 个子任务，开始执行...`
          }
          if (data.type === 'subtask_start') {
            const node = data.node || ''
            toolStatus.value = agentNodeLabelMap[node] || `正在处理：${node}...`
          }
          if (data.type === 'subtask_running') {
            const trace = messages.value[assistantIndex].agentTrace
            const subtaskId = data.subtask_id
            if (trace && trace.subtasks.length > 0) {
              const st = trace.subtasks.find((s: any) => s.id === subtaskId)
              if (st) {
                st.status = 'running'
              }
              toolStatus.value = `Executor 正在执行子任务 ${subtaskId}/${trace.subtasks.length}：${st?.desc || ''}`
            }
          }
          if (data.type === 'subtask_result') {
            const trace = messages.value[assistantIndex].agentTrace
            const subtaskId = data.subtask_id
            if (trace && trace.subtasks.length > 0) {
              const st = trace.subtasks.find((s: any) => s.id === subtaskId)
              if (st) {
                st.status = 'done'
              }
              const doneCount = trace.subtasks.filter((s: any) => s.status === 'done').length
              const total = trace.subtasks.length
              if (doneCount < total) {
                toolStatus.value = `子任务 ${doneCount}/${total} 已完成，继续执行下一个...`
              } else {
                toolStatus.value = `全部 ${total} 个子任务执行完毕，进入审查阶段...`
              }
            }
          }
          if (data.type === 'review') {
            const trace = messages.value[assistantIndex].agentTrace
            if (trace) {
              trace.reviewDecision = data.decision || ''
              trace.reviewFeedback = data.feedback || ''
              trace.retryCount = data.retry_count || 0
            }
            if (data.decision === 'PASS') {
              toolStatus.value = 'Reviewer 审查通过，正在生成最终回答...'
            } else if (data.decision === 'REVISE') {
              toolStatus.value = `Reviewer 发现问题，Executor 正在修正 (第 ${data.retry_count} 次重试)...`
            } else {
              toolStatus.value = 'Reviewer 审查未通过，生成最终回答...'
            }
          }
          if (data.type === 'metrics') {
            const trace = messages.value[assistantIndex].agentTrace
            if (trace) {
              trace.metrics = {
                tokensIn: data.total_tokens_in || 0,
                tokensOut: data.total_tokens_out || 0,
                latencyMs: data.total_latency_ms || 0,
              }
            }
          }
          if (data.type === 'done') {
            // 自动折叠追踪（新消息展开，旧消息折叠）
            traceCollapsed.value[assistantIndex] = false
          }
        } catch {
          /* ignore parse errors */
        }
      }
    }

    await refreshQuota()

    const userContent = messages.value[messages.value.length - 2]?.content ?? text
    const assistantContent = messages.value[assistantIndex].content
    const trace = messages.value[assistantIndex].agentTrace
    if (trace && assistantContent && !assistantContent.startsWith('错误：')) {
      toolStatus.value = trace.ragContexts?.length ? '正在自动评估 RAG 回答质量...' : null
      await runAutomaticRagEvaluation(trace, userContent, assistantContent)
    }
    const agentTraceStr = trace && (trace.planSummary || trace.subtasks.length > 0 || trace.reviewDecision) ? JSON.stringify(trace) : undefined
    if (assistantContent && !assistantContent.startsWith('错误：')) {
      await appendChatPair(sid, userContent, assistantContent, agentTraceStr)
      await refreshSessions()
    }
  } catch (e: any) {
    if (e.name === 'AbortError') {
      // User cancelled
    } else {
      connectionError.value = (e as Error).message || '发送失败'
      messages.value[assistantIndex].content = connectionError.value
    }
  } finally {
    isSending.value = false
    abortController = null
    toolStatus.value = null
    document.documentElement.classList.remove('echobot-streaming')
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function triggerTxtUpload() {
  fileInputRef.value?.click()
}

async function onTxtFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const text = await file.text()
    if (text) {
      const t = text.trim()
      if (t.length > 120000) {
        connectionError.value = '文本过长（请小于约 120KB）'
        return
      }
      inputText.value = (inputText.value ? `${inputText.value}\n\n` : '') + t
    }
  } catch {
    connectionError.value = '读取 TXT 失败'
  }
}

/* ─── 图片上传 ─── */
function triggerImageUpload() {
  imageInputRef.value?.click()
}

function onImageUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    connectionError.value = '请选择图片文件'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    connectionError.value = '图片过大（最大 10MB）'
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    const base64 = reader.result as string
    pendingImage.value = base64
    pendingImagePreview.value = base64
  }
  reader.readAsDataURL(file)
}

function clearPendingImage() {
  pendingImage.value = null
  pendingImagePreview.value = null
  imageRecognitionContent.value = ''
}

/* ─── 语音输入（MediaRecorder + 静音自动停止 + 后端 MiMo ASR 转录）─── */
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let silenceTimer: ReturnType<typeof setTimeout> | null = null
let audioContext: AudioContext | null = null
let analyserNode: AnalyserNode | null = null
let recordingMimeType = 'audio/webm'

// 静音检测参数 — 使用时域 RMS 更准确
const SILENCE_THRESHOLD = 0.008 // RMS 阈值，低于此值视为静音
const SILENCE_TIMEOUT_MS = 1400 // 连续静音多少毫秒后自动停止

async function initMediaRecorder() {
  if (!navigator.mediaDevices?.getUserMedia) {
    speechSupported.value = false
    return
  }
  speechSupported.value = true
}

async function toggleVoiceInput() {
  if (isListening.value) {
    stopRecording()
  } else {
    await startRecording()
  }
}

async function startRecording() {
  audioChunks = []
  connectionError.value = ''
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })

    // 初始化 AudioContext 做静音检测
    audioContext = new AudioContext()
    const source = audioContext.createMediaStreamSource(stream)
    analyserNode = audioContext.createAnalyser()
    analyserNode.fftSize = 256
    analyserNode.smoothingTimeConstant = 0.3
    source.connect(analyserNode)

    // 选择最佳 MIME 类型
    recordingMimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus'
      : MediaRecorder.isTypeSupported('audio/webm')
        ? 'audio/webm'
        : 'audio/ogg;codecs=opus'

    mediaRecorder = new MediaRecorder(stream, { mimeType: recordingMimeType })
    audioChunks = []

    mediaRecorder.ondataavailable = (e: BlobEvent) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }

    mediaRecorder.onstop = async () => {
      // 释放资源
      stream.getTracks().forEach(t => t.stop())
      if (audioContext) {
        audioContext.close()
        audioContext = null
      }
      if (silenceTimer) {
        clearTimeout(silenceTimer)
        silenceTimer = null
      }
      analyserNode = null

      if (audioChunks.length === 0) {
        isListening.value = false
        return
      }
      const audioBlob = new Blob(audioChunks, { type: recordingMimeType })
      await transcribeAudio(audioBlob)
    }

    mediaRecorder.onerror = () => {
      connectionError.value = '录音失败，请检查麦克风'
      isListening.value = false
    }

    mediaRecorder.start(100) // 每 100ms 收集一个数据块，用于静音检测
    isListening.value = true
    console.log('[语音输入] 开始录音，静音 ' + SILENCE_TIMEOUT_MS / 1000 + 's 后自动停止')

    // 启动静音检测
    checkSilence()
  } catch (e: any) {
    console.error('[语音输入] 启动失败:', e)
    if (e.name === 'NotAllowedError' || e.name === 'PermissionDeniedError') {
      connectionError.value = '麦克风权限被拒绝，请在浏览器设置中允许'
    } else {
      connectionError.value = '无法访问麦克风，请检查设备'
    }
    isListening.value = false
  }
}

/** 循环检测音量（时域 RMS），连续静音超时则自动停止 */
function checkSilence() {
  if (!analyserNode || !isListening.value || !mediaRecorder || mediaRecorder.state !== 'recording')
    return

  // 用时域数据算 RMS，比频域数据准确得多
  const dataArray = new Uint8Array(analyserNode.fftSize)
  analyserNode.getByteTimeDomainData(dataArray)

  // RMS: 计算采样值相对中心线(128)的均方根
  let sumSquares = 0
  for (let i = 0; i < dataArray.length; i++) {
    const normalized = (dataArray[i] - 128) / 128 // 归一化到 -1 ~ 1
    sumSquares += normalized * normalized
  }
  const rms = Math.sqrt(sumSquares / dataArray.length)

  if (rms < SILENCE_THRESHOLD) {
    if (!silenceTimer) {
      silenceTimer = setTimeout(() => {
        console.log('[语音输入] RMS=' + rms.toFixed(4) + '，检测到静音，自动停止')
        stopRecording()
      }, SILENCE_TIMEOUT_MS)
    }
  } else {
    if (silenceTimer) {
      clearTimeout(silenceTimer)
      silenceTimer = null
    }
  }

  // 每 100ms 检查一次，更快响应
  setTimeout(checkSilence, 100)
}

function stopRecording() {
  if (silenceTimer) {
    clearTimeout(silenceTimer)
    silenceTimer = null
  }
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
    console.log('[语音输入] 停止录音，正在转录...')
  }
  connectionError.value = ''
  isListening.value = false
}

async function transcribeAudio(audioBlob: Blob) {
  try {
    // MiMo ASR 只支持 mp3 / wav，需要把浏览器录的 webm 转成 wav
    const wavBlob = await convertToWav(audioBlob)
    const arrayBuffer = await wavBlob.arrayBuffer()
    const base64 = arrayBufferToBase64(arrayBuffer)
    const dataUri = `data:audio/wav;base64,${base64}`

    const token = getAuthToken()
    const res = await fetch('/api/ai/transcribe/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ audio: dataUri }),
    })

    if (!res.ok) {
      connectionError.value = `转录请求失败 (${res.status})`
      return
    }

    // 读取 SSE 流，逐字显示
    const reader = res.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    inputText.value = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed || !trimmed.startsWith('data:')) continue
        try {
          const data = JSON.parse(trimmed.slice(5).trim())
          if (data.error) {
            connectionError.value = data.error
            break
          }
          if (data.text) {
            inputText.value = data.text
          }
          if (data.done) {
            console.log('[语音输入] 转录完成:', data.text)
          }
        } catch {
          // ignore
        }
      }
    }
  } catch (e: any) {
    console.error('[语音输入] 转录请求失败:', e)
    connectionError.value = '语音转录网络异常'
  }
}

/** WebM/任意音频 Blob → WAV Blob（PCM 16-bit, mono） */
async function convertToWav(audioBlob: Blob): Promise<Blob> {
  const audioCtx = new AudioContext()
  try {
    const arrayBuffer = await audioBlob.arrayBuffer()
    const audioBuffer = await audioCtx.decodeAudioData(arrayBuffer)

    // 取单声道 PCM
    const channelData = audioBuffer.getChannelData(0)
    const sampleRate = audioBuffer.sampleRate
    const numChannels = 1
    const bitsPerSample = 16
    const byteRate = (sampleRate * numChannels * bitsPerSample) / 8
    const blockAlign = (numChannels * bitsPerSample) / 8
    const dataLength = channelData.length * blockAlign
    const buffer = new ArrayBuffer(44 + dataLength)
    const view = new DataView(buffer)

    // WAV header
    writeString(view, 0, 'RIFF')
    view.setUint32(4, 36 + dataLength, true)
    writeString(view, 8, 'WAVE')
    writeString(view, 12, 'fmt ')
    view.setUint32(16, 16, true)
    view.setUint16(20, 1, true) // PCM
    view.setUint16(22, numChannels, true)
    view.setUint32(24, sampleRate, true)
    view.setUint32(28, byteRate, true)
    view.setUint16(32, blockAlign, true)
    view.setUint16(34, bitsPerSample, true)
    writeString(view, 36, 'data')
    view.setUint32(40, dataLength, true)

    // PCM data
    let offset = 44
    for (let i = 0; i < channelData.length; i++) {
      const sample = Math.max(-1, Math.min(1, channelData[i]))
      const intSample = sample < 0 ? sample * 0x8000 : sample * 0x7fff
      view.setInt16(offset, intSample, true)
      offset += 2
    }

    return new Blob([buffer], { type: 'audio/wav' })
  } finally {
    audioCtx.close()
  }
}

function writeString(view: DataView, offset: number, str: string) {
  for (let i = 0; i < str.length; i++) {
    view.setUint8(offset + i, str.charCodeAt(i))
  }
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary)
}

watch(
  messages,
  () => {
    scrollChatToBottom(isSending.value ? 'auto' : 'smooth')
  },
  { deep: true }
)

watch(imageRecognitionContent, () => {
  scrollChatToBottom('auto')
})

watch(activeTab, t => {
  if (t === 'chat') scrollChatToBottom('smooth', true)
})

watch(isSending, s => {
  if (s) charPickerOpen.value = false
})

watch(selectedCharacterKey, async () => {
  if (currentSessionId.value == null) return
  try {
    await updateAiSession(currentSessionId.value, {
      modelId: 'deepseek-chat',
      characterKey: selectedCharacterKey.value,
    })
    await refreshSessions()
  } catch {
    /* ignore */
  }
})

const closeCharacterPicker = () => {
  charPickerOpen.value = false
}

onMounted(async () => {
  document.addEventListener('click', closeCharacterPicker)
  document.documentElement.classList.add('echobot-route')

  if (!userStore.isLoggedIn) {
    characterCards.value = [
      { key: 'default', name: '星轮助手', description: '你的智慧宇宙领航员，精通文学、科幻与本站知识。', systemPrompt: '' },
      { key: 'philosopher', name: '庄子', description: '物我两忘，逍遥游于字里行间的古代哲学家。', systemPrompt: '你现在扮演哲学家庄子，说话充满道家智慧和哲理，善用寓言。' },
      { key: 'explorer', name: '银河探索者', description: '热衷于探索未知星域的科幻领航员。', systemPrompt: '你是一名银河探索者，说话带有机甲、星河、探索的科幻色彩。' }
    ]
    sessions.value = [
      { id: -1, title: '访客体验会话', characterKey: 'default', modelId: 'mock', createdAt: '', updatedAt: '' }
    ]
    currentSessionId.value = -1
    messages.value = [
      {
        role: 'assistant',
        content: '你好！我是 Starlore 智能助理。目前系统已自动进入**访客体验模式**。\n\n> 💡 **提示**：访客模式下您可以**真实调用 AI 助手**进行对话，每日可享受 **20 次免费调用**（与创意发散共享）。\n\n您不仅可以正常对话，还能**自由切换角色卡**，AI 会自动为您检索公开文章进行智能回答。',
        reasoningContent: '检测到当前用户未登录，已初始化真实访客会话体验。'
      }
    ]
    await refreshQuota()
    return
  }

  try {
    const { cards } = await getCharacterCards()
    characterCards.value = cards || []
  } catch {
    /* ignore */
  }

  await refreshQuota()
  await refreshSessions()
  initMediaRecorder()
  if (sessions.value.length > 0) {
    await loadSession(sessions.value[0].id)
  } else {
    await newSession()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeCharacterPicker)
  document.documentElement.classList.remove('echobot-route')
  document.documentElement.classList.remove('echobot-streaming')
  cancelStream()
})
</script>

<template>
  <!-- 沉浸模式 -->
  <ImmersiveMode
    v-if="immersiveActive"
    :messages="messages"
    :system-prompt="systemPrompt"
    :character-cards="characterCards"
    :selected-character-key="selectedCharacterKey"
    :is-sending="isSending"
    :daily-remaining="dailyRemaining"
    :daily-exceeded="dailyExceeded"
    :sessions="sessions"
    :current-session-id="currentSessionId"
    @close="exitImmersive"
    @update:selected-character-key="selectedCharacterKey = $event"
    @image-upload="handleImmersiveImageUpload"
    @txt-upload="handleImmersiveTxtUpload"
    @load-session="loadSession"
    @new-session="newSession"
    @delete-session="confirmDeleteSession"
    @send="onImmersiveSend"
    @refresh-quota="refreshQuota"
  />
</template>

<style scoped src="./EchobotView.css"></style>

<!-- 非 scoped 样式：覆盖 v-html 渲染的 Markdown 内容 -->
<style>
.bubble .chat-table {
  border-collapse: collapse;
  margin: 0.4rem 0;
  width: 100%;
  font-size: 0.82rem;
  border: 1px solid #d0d0d0;
}
.bubble .chat-table th,
.bubble .chat-table td {
  border: 1px solid #d0d0d0;
  padding: 0.35rem 0.6rem;
  text-align: left;
}
.bubble .chat-table th {
  background: var(--accent-soft);
  font-weight: 700;
}
.bubble .chat-table tr:nth-child(even) {
  background: rgba(0, 0, 0, 0.015);
}
.bubble .answer-text h1,
.bubble .answer-text h2,
.bubble .answer-text h3,
.bubble .answer-text h4 {
  margin: 0.35rem 0 0.15rem;
  font-weight: 700;
  line-height: 1.3;
}
.bubble .answer-text p {
  margin: 0.1rem 0;
}
.bubble .answer-text p:first-child {
  margin-top: 0;
}
.bubble .answer-text p:last-child {
  margin-bottom: 0;
}
.bubble .answer-text p:empty {
  display: none;
}
.bubble .answer-text ul,
.bubble .answer-text ol {
  padding-left: 1.2rem;
  margin: 0.15rem 0;
}
.bubble .answer-text li {
  margin: 0.05rem 0;
}
.bubble .answer-text pre {
  margin: 0.45rem 0;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 0.8rem;
  line-height: 1.45;
  background: #282c34;
  padding: 0.6rem 0.8rem;
}
.bubble .answer-text pre code {
  background: transparent;
  padding: 0;
  border-radius: 0;
  font-size: inherit;
  color: #abb2bf;
}
.bubble .answer-text code {
  background: rgba(0, 0, 0, 0.06);
  padding: 0.1rem 0.3rem;
  border-radius: 3px;
  font-size: 0.85em;
  font-family: 'Fira Code', 'Consolas', monospace;
}
.bubble .answer-text hr {
  margin: 0.3rem 0;
  border: none;
  border-top: 1px solid var(--border);
}
.bubble .answer-text blockquote {
  margin: 0.4rem 0;
  padding: 0.25rem 0.6rem;
  border-left: 3px solid var(--accent);
  color: var(--ink-muted);
  background: rgba(0, 0, 0, 0.02);
  border-radius: 0 6px 6px 0;
}
/* 打字加载动画 */
.typing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0.5rem 0.8rem;
}
.typing-indicator .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: oklch(0.55 0.15 35 / 0.4);
  animation: typing-bounce 1.4s infinite ease-in-out;
}
.typing-indicator .dot:nth-child(1) {
  animation-delay: 0s;
}
.typing-indicator .dot:nth-child(2) {
  animation-delay: 0.2s;
}
.typing-indicator .dot:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes typing-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

/* ── 深色主题下的 Guest Overlay ── */
[data-theme="dark"] .guest-overlay {
  background: rgba(15, 17, 23, 0.78);
}

[data-theme="dark"] .guest-overlay-content {
  background: rgba(26, 28, 38, 0.9);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    0 2px 8px rgba(0, 0, 0, 0.2);
}
</style>

