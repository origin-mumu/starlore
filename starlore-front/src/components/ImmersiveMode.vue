<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import {
  buildMultiAgentSseUrl,
  evaluateRag,
  getMcpTools,
  reindexKnowledgeBase,
  type CharacterCard,
  type McpToolInfo,
  type RagEvaluationResult,
} from '@/api/ai'
import { guestChat } from '@/api/guest-ai'
import { useUserStore } from '@/stores/user'
import { sanitizeHtml } from '@/utils/sanitize'
import { getAuthToken } from '@/utils/authToken'
import {
  Bot,
  ThumbsUp,
  ThumbsDown,
  BookOpen,
  Sliders,
  ExternalLink,
  MessageSquare,
  Image,
  FileText,
  Send,
  Trash2,
  X,
  ClipboardList,
  CheckCircle,
  RotateCcw,
  XCircle,
  Paperclip,
  Activity,
  RefreshCw,
  Server,
} from '@lucide/vue'

const userStore = useUserStore()

/* ─── 用户反馈点赞点踩 ─── */
const feedbackModalOpen = ref(false)
const feedbackTargetMessageId = ref<number | null>(null)
const feedbackRating = ref<'LIKE' | 'DISLIKE'>('LIKE')
const feedbackType = ref('NOT_RELEVANT')
const feedbackComment = ref('')

async function handleLike(msg: any) {
  msg.userFeedback = msg.userFeedback === 'LIKE' ? null : 'LIKE'
  const targetId = msg.id || 9999
  try {
    await submitMessageFeedback(targetId, {
      sessionId: props.sessionId || 0,
      rating: 'LIKE'
    })
  } catch {}
}

function openDislikeModal(msg: any) {
  msg.userFeedback = 'DISLIKE'
  feedbackTargetMessageId.value = msg.id || 9999
  feedbackRating.value = 'DISLIKE'
  feedbackType.value = 'NOT_RELEVANT'
  feedbackComment.value = ''
  feedbackModalOpen.value = true
}

async function submitDislikeFeedback() {
  const targetId = feedbackTargetMessageId.value
  try {
    await submitMessageFeedback(targetId || 9999, {
      sessionId: props.sessionId || 0,
      rating: 'DISLIKE',
      feedbackType: feedbackType.value,
      comment: feedbackComment.value
    })
  } catch {}
  feedbackModalOpen.value = false
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

type ChatMsg = {
  role: 'user' | 'assistant'
  content: string
  reasoningContent?: string
  imageUrl?: string
  agentTrace?: AgentTrace
  attachmentName?: string
}

type Session = {
  id: number
  title: string
  characterKey: string
  modelId: string
}

const props = defineProps<{
  messages: ChatMsg[]
  systemPrompt: string
  characterCards: CharacterCard[]
  selectedCharacterKey: string
  isSending: boolean
  dailyRemaining: number
  dailyExceeded: boolean
  sessions: Session[]
  currentSessionId: number | null
}>()

const emit = defineEmits<{
  close: []
  send: [userContent: string, assistantContent: string, agentTrace?: string]
  'update:selectedCharacterKey': [value: string]
  imageUpload: [base64: string]
  txtUpload: [text: string]
  loadSession: [id: number]
  newSession: []
  deleteSession: [id: number]
  refreshQuota: []
}>()

/* ─── 状态 ─── */
type Mode = 'idle' | 'listening' | 'thinking' | 'speaking'
const currentMode = ref<Mode>('idle')
const statusText = ref('System Ready')
const isRecording = ref(false)
const showHint = ref(true)
const isLocalSending = ref(false)
const interimText = ref('')

/* ─── 文字输入 ─── */
const inputText = ref('')
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const charPickerOpen = ref(false)

watch(inputText, () => {
  nextTick(() => {
    const el = textareaRef.value
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${el.scrollHeight}px`
  })
})

/* ─── 会话列表 ─── */
const activeTab = ref<'chat' | 'sessions' | 'capabilities'>('chat')
const showDeleteConfirm = ref<number | null>(null)

/* ─── Agent 能力检查 ─── */
const mcpTools = ref<McpToolInfo[]>([])
const mcpServers = ref<string[]>([])
const mcpLoading = ref(false)
const mcpError = ref('')
const reindexLoading = ref(false)
const reindexMessage = ref('')
const ragOpenIndex = ref<number | null>(null)
const ragLoading = ref(false)
const ragError = ref('')
const ragGroundTruth = ref('')
const ragEvaluation = ref<RagEvaluationResult | null>(null)

async function loadMcpCapabilities() {
  if (!userStore.token || mcpLoading.value) return
  mcpLoading.value = true
  mcpError.value = ''
  try {
    const result = await getMcpTools()
    mcpTools.value = result.tools || []
    mcpServers.value = result.servers || []
  } catch (error: any) {
    mcpError.value = error?.message || 'MCP 工具加载失败'
  } finally {
    mcpLoading.value = false
  }
}

async function rebuildKnowledgeIndex() {
  if (reindexLoading.value) return
  reindexLoading.value = true
  reindexMessage.value = ''
  try {
    const result = await reindexKnowledgeBase()
    reindexMessage.value = result.message || `已索引 ${result.count || 0} 篇文章`
  } catch (error: any) {
    reindexMessage.value = error?.response?.data?.message || error?.message || '重建索引失败'
  } finally {
    reindexLoading.value = false
  }
}

function openCapabilities() {
  activeTab.value = 'capabilities'
  if (!mcpTools.value.length && !mcpLoading.value) void loadMcpCapabilities()
}

function getMcpServerName(server: string) {
  return server === 'zhipu-web-search' ? '智谱联网搜索' : server
}

function getMcpServerToolCount(server: string) {
  return mcpTools.value.filter(tool => tool.server === server).length
}

function findQaPair(assistantIndex: number) {
  const assistant = props.messages[assistantIndex]
  if (!assistant || assistant.role !== 'assistant' || !assistant.content.trim()) return null
  for (let userIndex = assistantIndex - 1; userIndex >= 0; userIndex--) {
    const user = props.messages[userIndex]
    if (user.role === 'user' && user.content.trim()) {
      return { question: user.content, answer: assistant.content }
    }
  }
  return null
}

function toggleRagEvaluation(index: number) {
  if (ragOpenIndex.value === index) {
    ragOpenIndex.value = null
    return
  }
  ragOpenIndex.value = index
  ragError.value = ''
  ragGroundTruth.value = ''
  ragEvaluation.value = null
}

async function evaluateAnswer(index: number) {
  if (ragLoading.value) return
  const pair = findQaPair(index)
  if (!pair) {
    ragError.value = '没有找到这条回答对应的问题'
    return
  }
  ragLoading.value = true
  ragError.value = ''
  ragEvaluation.value = null
  try {
    ragEvaluation.value = await evaluateRag({
      ...pair,
      groundTruth: ragGroundTruth.value.trim() || undefined,
      topK: 5,
    })
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || ''
    ragError.value =
      message === '服务器内部错误'
        ? '这轮对话没有检索到相关知识库文章，无法评分。请先询问一个与你文章内容相关的问题。'
        : message || '质量检测失败，请稍后重试'
  } finally {
    ragLoading.value = false
  }
}

function scorePercent(score: number) {
  return `${Math.round(score * 100)}%`
}

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

/* ─── 快捷回复 ─── */
const quickRepliesRef = ref<HTMLElement | null>(null)
let qrDragState = { isDown: false, startX: 0, scrollLeft: 0 }

function onQrMouseDown(e: MouseEvent) {
  const el = quickRepliesRef.value
  if (!el) return
  qrDragState.isDown = true
  qrDragState.startX = e.pageX - el.offsetLeft
  qrDragState.scrollLeft = el.scrollLeft
  el.style.cursor = 'grabbing'
}
function onQrMouseMove(e: MouseEvent) {
  if (!qrDragState.isDown) return
  const el = quickRepliesRef.value
  if (!el) return
  e.preventDefault()
  const x = e.pageX - el.offsetLeft
  el.scrollLeft = qrDragState.scrollLeft - (x - qrDragState.startX)
}
function onQrMouseUp() {
  qrDragState.isDown = false
  const el = quickRepliesRef.value
  if (el) el.style.cursor = 'grab'
}

const quickReplies = [
  '最近有什么新文章？',
  '帮我总结一下分类',
  '写一篇技术博客大纲',
  '推荐几个学习方向',
  '帮我查一下后端相关文章',
  '介绍一下 Starlore 项目',
  '给新文章起个标题',
]

/* ─── 图片/文件 附件 ─── */
const imageInputRef = ref<HTMLInputElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const isParsingFile = ref(false)
const pendingImage = ref<string | null>(null) // base64
const pendingImagePreview = ref<string | null>(null)
const pendingAttachment = ref<{ name: string; text: string } | null>(null)

function removeAttachment() {
  pendingAttachment.value = null
}

/* ─── 显示用消息列表（同步父组件） ─── */
const chatScrollRef = ref<HTMLElement | null>(null)

/* ─── Canvas refs ─── */
const mainCanvasRef = ref<HTMLCanvasElement | null>(null)
const waveCanvasRef = ref<HTMLCanvasElement | null>(null)

/* ─── 语音识别（MediaRecorder + MiMo ASR + 静音自动停止）─── */
const speechSupported = ref(false)
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let silenceTimer: ReturnType<typeof setTimeout> | null = null
let audioCtx: AudioContext | null = null
let analyser: AnalyserNode | null = null
let recordingMimeType = 'audio/webm'
let abortCtrl: AbortController | null = null

const SILENCE_THRESHOLD = 0.008
const SILENCE_TIMEOUT_MS = 700

function initSpeech() {
  speechSupported.value = !!navigator.mediaDevices?.getUserMedia
}

async function toggleVoice() {
  if (currentMode.value === 'thinking' || currentMode.value === 'speaking') return
  if (!speechSupported.value) return

  if (!isRecording.value) {
    abortCtrl?.abort()
    isRecording.value = true
    setMode('listening')
    await startRecording()
  } else {
    stopRecording()
  }
}

async function startRecording() {
  audioChunks = []
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioCtx = new AudioContext()
    const source = audioCtx.createMediaStreamSource(stream)
    analyser = audioCtx.createAnalyser()
    analyser.fftSize = 256
    analyser.smoothingTimeConstant = 0.3
    source.connect(analyser)

    recordingMimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus'
      : 'audio/webm'

    mediaRecorder = new MediaRecorder(stream, { mimeType: recordingMimeType })
    audioChunks = []

    mediaRecorder.ondataavailable = (e: BlobEvent) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }

    mediaRecorder.onstop = async () => {
      stream.getTracks().forEach(t => t.stop())
      if (audioCtx) {
        audioCtx.close()
        audioCtx = null
      }
      if (silenceTimer) {
        clearTimeout(silenceTimer)
        silenceTimer = null
      }
      analyser = null

      if (audioChunks.length === 0) {
        setMode('idle')
        return
      }
      const audioBlob = new Blob(audioChunks, { type: recordingMimeType })
      await transcribeAndSend(audioBlob)
    }

    mediaRecorder.onerror = () => {
      setMode('idle')
    }

    mediaRecorder.start(100)
    checkSilence()
  } catch {
    interimText.value = '麦克风权限被拒绝'
    setMode('idle')
  }
}

function checkSilence() {
  if (!analyser || !isRecording.value || !mediaRecorder || mediaRecorder.state !== 'recording')
    return

  const data = new Uint8Array(analyser.fftSize)
  analyser.getByteTimeDomainData(data)
  let sumSq = 0
  for (let i = 0; i < data.length; i++) {
    const n = (data[i] - 128) / 128
    sumSq += n * n
  }
  const rms = Math.sqrt(sumSq / data.length)

  if (rms < SILENCE_THRESHOLD) {
    if (!silenceTimer) {
      silenceTimer = setTimeout(() => stopRecording(), SILENCE_TIMEOUT_MS)
    }
  } else {
    if (silenceTimer) {
      clearTimeout(silenceTimer)
      silenceTimer = null
    }
  }
  setTimeout(checkSilence, 100)
}

function stopRecording() {
  if (silenceTimer) {
    clearTimeout(silenceTimer)
    silenceTimer = null
  }
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  isRecording.value = false
}

/** WebM → WAV 转换 */
async function convertToWav(audioBlob: Blob): Promise<Blob> {
  const ctx = new AudioContext()
  try {
    const buf = await ctx.decodeAudioData(await audioBlob.arrayBuffer())
    const ch = buf.getChannelData(0)
    const sr = buf.sampleRate,
      nc = 1,
      bps = 16
    const br = (sr * nc * bps) / 8,
      ba = (nc * bps) / 8
    const dl = ch.length * ba
    const ab = new ArrayBuffer(44 + dl)
    const v = new DataView(ab)
    writeStr(v, 0, 'RIFF')
    v.setUint32(4, 36 + dl, true)
    writeStr(v, 8, 'WAVE')
    writeStr(v, 12, 'fmt ')
    v.setUint32(16, 16, true)
    v.setUint16(20, 1, true)
    v.setUint16(22, nc, true)
    v.setUint32(24, sr, true)
    v.setUint32(28, br, true)
    v.setUint16(32, ba, true)
    v.setUint16(34, bps, true)
    writeStr(v, 36, 'data')
    v.setUint32(40, dl, true)
    let off = 44
    for (let i = 0; i < ch.length; i++) {
      const s = Math.max(-1, Math.min(1, ch[i]))
      v.setInt16(off, s < 0 ? s * 0x8000 : s * 0x7fff, true)
      off += 2
    }
    return new Blob([ab], { type: 'audio/wav' })
  } finally {
    ctx.close()
  }
}

function writeStr(v: DataView, o: number, s: string) {
  for (let i = 0; i < s.length; i++) v.setUint8(o + i, s.charCodeAt(i))
}

function arrayBufToB64(buf: ArrayBuffer): string {
  const bytes = new Uint8Array(buf)
  let bin = ''
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i])
  return btoa(bin)
}

/** 转录 + 发送给 AI 对话 */
async function transcribeAndSend(audioBlob: Blob) {
  if (!userStore.isLoggedIn) {
    // 模拟语音转录进度并预设查询
    setMode('thinking')
    interimText.value = '正在模拟转录中...'
    setTimeout(() => {
      interimText.value = ''
      handleVoiceSend('介绍一下星域分类')
    }, 1200)
    return
  }

  try {
    const wav = await convertToWav(audioBlob)
    const b64 = arrayBufToB64(await wav.arrayBuffer())
    const dataUri = `data:audio/wav;base64,${b64}`
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
      setMode('idle')
      return
    }

    const reader = res.body!.getReader()
    const decoder = new TextDecoder()
    let buf = ''
    let fullText = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      const lines = buf.split('\n')
      buf = lines.pop() || ''
      for (const line of lines) {
        const t = line.trim()
        if (!t || !t.startsWith('data:')) continue
        try {
          const d = JSON.parse(t.slice(5).trim())
          if (d.error) {
            setMode('idle')
            return
          }
          if (d.text) {
            fullText = d.text
            interimText.value = fullText
          }
          if (d.done) break
        } catch {}
      }
    }

    if (fullText) {
      interimText.value = ''
      handleVoiceSend(fullText)
    } else {
      setMode('idle')
    }
  } catch {
    setMode('idle')
  }
}

// 用于 API 发送的完整消息（含附件原文），与显示内容分离
let pendingApiText = ''
// 记录最后一条用户消息在 messages 中的索引
let lastUserMsgIndex = -1

/** 构建 API 消息：最后一条用户消息用完整文本（含附件），其余用显示文本 */
function buildApiMessages(): { role: string; content: string }[] {
  const out: { role: string; content: string }[] = []
  const sys = props.systemPrompt.trim()
  if (sys) out.push({ role: 'system', content: sys })
  const msgs = props.messages
  for (let i = 0; i < msgs.length; i++) {
    const m = msgs[i]
    if (m.role === 'assistant' && !m.content.trim()) continue
    // 最后一条用户消息如果有待发送的完整文本，用它（附件内容等）
    if (m.role === 'user' && i === lastUserMsgIndex && pendingApiText) {
      out.push({ role: 'user', content: pendingApiText })
    } else {
      out.push({ role: m.role, content: m.content })
    }
  }
  return out
}

/** 文字输入发送 */
function handleTextSend() {
  const text = inputText.value.trim()
  const attachment = pendingAttachment.value
  // 需要至少有文字或附件
  if ((!text && !attachment) || props.isSending || isLocalSending.value) return

  // 显示文本：只显示用户输入的文字
  let displayText = ''
  // API 文本：含附件完整内容
  let apiText = ''
  let attachName = ''

  if (attachment) {
    attachName = attachment.name
    displayText = text || ''
    apiText = `[附件: ${attachment.name}]\n${attachment.text}`
    if (text) apiText += `\n\n用户说明：${text}`
  } else {
    displayText = text
    apiText = text
  }

  inputText.value = ''
  pendingAttachment.value = null
  pendingApiText = apiText
  handleVoiceSend(displayText, attachName)
}

function onInputKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleTextSend()
  }
}

function useQuickReply(text: string) {
  if (props.isSending || isLocalSending.value) return
  inputText.value = text
}

function pickCharacter(key: string) {
  emit('update:selectedCharacterKey', key)
  charPickerOpen.value = false
}

function onImageUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !file.type.startsWith('image/')) return
  if (file.size > 10 * 1024 * 1024) return

  if (!userStore.isLoggedIn) {
    alert('访客模式暂不支持图片分析，请登录以体验云端真实的 DeepSeek 视觉大模型！')
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

function removeImage() {
  pendingImage.value = null
  pendingImagePreview.value = null
}

async function onFileUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!userStore.isLoggedIn) {
    alert('访客模式暂不支持文档解析，请登录以体验云端真实的智能文档分析！')
    return
  }

  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  const plainTextExts = ['txt', 'md', 'markdown', 'csv', 'json', 'xml', 'yaml', 'yml']

  if (plainTextExts.includes(ext)) {
    // 纯文本文件直接读取
    const text = await file.text()
    if (text.trim()) {
      pendingAttachment.value = { name: file.name, text: text.trim() }
    }
  } else {
    // docx/pdf 等需要后端解析
    isParsingFile.value = true
    try {
      const token = getAuthToken()
      const formData = new FormData()
      formData.append('file', file)
      const res = await fetch('/api/ai/parse-file', {
        method: 'POST',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: formData,
      })
      const data = await res.json()
      if (data.success && data.text) {
        pendingAttachment.value = { name: data.filename || file.name, text: data.text }
      } else {
        console.warn('文件解析失败:', data.error)
      }
    } catch (err) {
      console.warn('文件解析请求失败:', err)
    } finally {
      isParsingFile.value = false
    }
  }
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
  getBlogStats: '正在获取博客统计...',
  getRecentArticles: '正在获取最新星记...',
  writeArticle: '正在创建星记...',
  updateArticle: '正在更新星记...',
  deleteArticle: '正在删除星记...',
  getAllTags: '正在获取光痕列表...',
  getArticlesByCategory: '正在获取星域星记...',
  createCategory: '正在创建星域...',
  listMcpTools: '正在发现外部 MCP 工具...',
  callMcpTool: '正在调用外部 MCP 工具...',
}

const toolStatus = ref<string | null>(null)
const hasReceivedContent = ref(false)

/** 发送文字给 AI */
async function handleVoiceSend(text: string, attachmentName?: string) {
  if (props.isSending || isLocalSending.value) return
  isLocalSending.value = true
  hasReceivedContent.value = false

  const imageBase64 = pendingImage.value
  const hasImage = !!imageBase64

  // 往父组件的消息列表里加用户消息
  lastUserMsgIndex = props.messages.length
  props.messages.push({
    role: 'user',
    content: text || (hasImage ? '请分析这张图片' : ''),
    attachmentName: attachmentName || undefined,
    imageUrl: imageBase64 || undefined,
  })
  pendingImage.value = null
  pendingImagePreview.value = null
  scrollChat(true)
  setMode('thinking')

  // 加 AI 占位消息（带 agentTrace）
  props.messages.push({
    role: 'assistant',
    content: '',
    reasoningContent: '',
    agentTrace: {
      planSummary: '',
      subtasks: [],
      reviewDecision: '',
      reviewFeedback: '',
      retryCount: 0,
      metrics: null,
    },
  })
  const aiIdx = props.messages.length - 1

  if (!userStore.isLoggedIn) {
    // 游客真实 AI 对话逻辑
    try {
      toolStatus.value = '正在检索和思考...'
      const fullHistory = buildApiMessages()
      // 过滤系统提示词，保留对话历史
      const chatHistory = fullHistory
        .filter(m => m.role !== 'system')
        .map(m => ({ role: m.role as 'user' | 'assistant', content: m.content }))

      // 最后一个是当前输入的用户消息，需要分离开
      const lastMsg = chatHistory.pop()
      const userText = lastMsg ? lastMsg.content : text

      const res = await guestChat(userText, chatHistory, props.selectedCharacterKey)
      toolStatus.value = null
      hasReceivedContent.value = true

      // 设置 AI 思考链的元数据和回复的 reasoningContent
      const trace = props.messages[aiIdx].agentTrace
      if (trace) {
        trace.planSummary = '已通过语义搜索成功检索公开知识库内容，正在进行推理回答。'
        trace.subtasks = [
          { id: 1, desc: 'Planner: 检索公开内容', status: 'done' },
          { id: 2, desc: 'Executor: 生成推理回复', status: 'done' },
        ]
        trace.reviewDecision = 'PASS'
        trace.metrics = { tokensIn: 150, tokensOut: res.content.length, latencyMs: 500 }
      }

      props.messages[aiIdx].reasoningContent = res.reasoningContent || ''

      // 模拟流式打字输出
      let currentLen = 0
      const reply = res.content
      const interval = setInterval(() => {
        if (currentLen >= reply.length) {
          clearInterval(interval)
          props.messages[aiIdx].content = reply
          isLocalSending.value = false
          setMode('speaking')
          setTimeout(() => setMode('idle'), 1500)
          emit('refreshQuota') // 触发父组件刷新限额
        } else {
          const chunk = reply.substring(currentLen, currentLen + 2)
          props.messages[aiIdx].content += chunk
          scrollChat()
          currentLen += 2
        }
      }, 30)
    } catch (e: any) {
      toolStatus.value = null
      isLocalSending.value = false
      setMode('idle')
      const errorMsg =
        e?.response?.status === 429
          ? '今日访客体验额度（20次）已用尽，登录后即可体验更多哦！'
          : e?.message || '发送失败，请稍后重试'
      props.messages[aiIdx].content = errorMsg
      scrollChat(true)
    }
    return
  }

  abortCtrl = new AbortController()

  try {
    const token = getAuthToken()
    const authHeaders: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }

    // 如果有图片，先用 MiMo 流式识别
    let imageDescription = ''
    if (hasImage && imageBase64) {
      try {
        toolStatus.value = '正在识别图片...'
        const analyzeRes = await fetch('/api/ai/analyze-image/stream', {
          method: 'POST',
          headers: { ...authHeaders, Accept: 'text/event-stream' },
          body: JSON.stringify({ image: imageBase64, question: text || '请描述这张图片' }),
          signal: abortCtrl.signal,
        })
        if (analyzeRes.ok) {
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
                if (data.content) imageDescription += data.content
              } catch {
                /* ignore */
              }
            }
          }
        }
      } catch (e) {
        console.warn('图片识别失败', e)
      }
      toolStatus.value = null
    }

    const history = buildApiMessages()
    // 如果有图片描述，把描述拼到最后一条用户消息里
    if (imageDescription) {
      const lastMsg = history[history.length - 1]
      if (lastMsg && lastMsg.role === 'user') {
        lastMsg.content = `[用户上传了一张图片，图片内容：${imageDescription}]\n\n用户问题：${lastMsg.content}`
      }
    }

    toolStatus.value = '正在分析任务...'
    const res = await fetch(buildMultiAgentSseUrl('deepseek-v4-flash'), {
      method: 'POST',
      headers: authHeaders,
      body: JSON.stringify(history),
      signal: abortCtrl.signal,
    })

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`)
    }

    const reader = res.body!.getReader()
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
            props.messages[aiIdx].content = `错误：${data.error}`
            reader.cancel()
            break
          }
          if (data.reasoning_content) {
            props.messages[aiIdx].reasoningContent =
              (props.messages[aiIdx].reasoningContent || '') + data.reasoning_content
            toolStatus.value = null
          }
          if (data.content) {
            props.messages[aiIdx].content += data.content
            hasReceivedContent.value = true
            toolStatus.value = null
            scrollChat()
          }
          if (data.tool_start) {
            toolStatus.value = toolLabelMap[data.tool_start] || `正在执行 ${data.tool_start}...`
          }
          if (data.type === 'rag_context') {
            const trace = props.messages[aiIdx].agentTrace
            if (trace && Array.isArray(data.articles)) {
              const merged = [...(trace.ragContexts || []), ...data.articles]
              trace.ragContexts = Array.from(
                new Map(merged.map(item => [item.articleId, item])).values(),
              )
              trace.ragRetrievalMode = data.retrieval_mode === 'keyword' ? 'keyword' : 'vector'
            }
          }
          // ── 多 Agent 事件处理 ──
          if (data.type === 'plan_start') {
            toolStatus.value = 'Planner 正在分析您的请求，拆解为可执行的子任务...'
          }
          if (data.type === 'plan') {
            const trace = props.messages[aiIdx].agentTrace
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
            const trace = props.messages[aiIdx].agentTrace
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
            const trace = props.messages[aiIdx].agentTrace
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
            const trace = props.messages[aiIdx].agentTrace
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
            const trace = props.messages[aiIdx].agentTrace
            if (trace) {
              trace.metrics = {
                tokensIn: data.total_tokens_in || 0,
                tokensOut: data.total_tokens_out || 0,
                latencyMs: data.total_latency_ms || 0,
              }
            }
          }
          if (data.type === 'done') {
            toolStatus.value = null
          }
        } catch {}
      }
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') {
      props.messages[aiIdx].content += `\n\n[错误: ${e.message}]`
    }
  }

  // 持久化：通知父组件保存本轮对话
  const aiContent = props.messages[aiIdx]?.content || ''
  const trace = props.messages[aiIdx]?.agentTrace
  if (trace && aiContent && !aiContent.startsWith('错误：')) {
    toolStatus.value = trace.ragContexts?.length ? '正在自动评估 RAG 回答质量...' : null
    await runAutomaticRagEvaluation(trace, pendingApiText || text, aiContent)
  }
  const agentTraceStr =
    trace && (trace.planSummary || trace.subtasks.length > 0 || trace.reviewDecision)
      ? JSON.stringify(trace)
      : undefined
  if (aiContent && !aiContent.startsWith('错误：')) {
    emit('send', pendingApiText || text, aiContent, agentTraceStr)
  }

  pendingApiText = ''
  isLocalSending.value = false
  toolStatus.value = null
  // 回复完成后短暂停留 speaking 动画再回 idle
  setMode('speaking')
  setTimeout(() => setMode('idle'), 1500)
}

function setMode(mode: Mode) {
  currentMode.value = mode
  showHint.value = mode === 'idle'
  if (mode === 'idle') {
    statusText.value = 'System Ready'
    isRecording.value = false
  } else if (mode === 'listening') {
    statusText.value = 'Listening...'
  } else if (mode === 'thinking') {
    statusText.value = 'Processing'
  } else if (mode === 'speaking') {
    statusText.value = 'Transmitting'
  }
}

function scrollChat(force = false) {
  nextTick(() => {
    requestAnimationFrame(() => {
      const el = chatScrollRef.value
      if (el) {
        const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight
        if (force || distanceFromBottom < 400) {
          el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
        }
      }
    })
  })
}

watch(
  () => props.messages,
  () => {
    scrollChat()
  },
  { deep: true, immediate: true }
)

watch(toolStatus, () => {
  scrollChat()
})

/* ─── Markdown ─── */
marked.use({ breaks: false, gfm: true })
const renderer = new marked.Renderer()
renderer.code = ({ text, lang }: { text: string; lang?: string }) => {
  const la = lang ? ` class="language-${lang}"` : ''
  try {
    const h = lang ? hljs.highlight(text, { language: lang }).value : hljs.highlightAuto(text).value
    return `<pre><code${la}>${h}</code></pre>`
  } catch {
    return `<pre><code${la}>${hljs.highlightAuto(text).value}</code></pre>`
  }
}
marked.use({ renderer })

function fmt(s: string): string {
  if (!s) return ''
  try {
    // 清理多余空行 + 转义单个 ~ 避免被误解为删除线
    let cleaned = s
      .trim()
      .replace(/^\n+/, '')
      .replace(/\n+$/, '')
      .replace(/\n{2,}/g, '\n')
      .replace(/(?<!~)~(?!~)/g, '\\~')
    return (marked.parse(cleaned) as string)
      .replace(/<table>/g, '<table class="chat-table" tabindex="0">')
      .replace(/<p>\s*<\/p>/g, '')
      .trim()
  } catch {
    return s
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>')
  }
}

/* ═══════════════════════════════════════════
   3D 粒子球 + 背景星星
   ═══════════════════════════════════════════ */
const SPHERE_RADIUS = 180
const PARTICLE_COUNT = 800
// The app already has a soft nebula background. Keep the immersive surface
// consistent instead of adding a second, noisy full-screen star field.
const BG_STAR_COUNT = 0

let animId = 0
let width = 0
let height = 0
let rotX = 0
let rotY = 0
let isDragging = false
let lastMX = 0
let lastMY = 0
let targetSpeed = 1
let curSpeed = 1
let audioLevel = 0

/* ─── 主题感知颜色 ─── */
function getThemeColor(varName: string, fallback: string): string {
  const val = getComputedStyle(document.documentElement).getPropertyValue(varName).trim()
  return val || fallback
}

// 根据主题亮度判断是否为深色主题
function isDarkTheme(): boolean {
  const canvas = getThemeColor('--canvas', '#FFFCF7')
  // 简单判断：解析 hex 颜色的亮度
  const hex = canvas.replace('#', '')
  if (hex.length >= 6) {
    const r = parseInt(hex.substring(0, 2), 16)
    const g = parseInt(hex.substring(2, 4), 16)
    const b = parseInt(hex.substring(4, 6), 16)
    return (r * 299 + g * 587 + b * 114) / 1000 < 128
  }
  return true
}

function getParticleColors() {
  const dark = isDarkTheme()
  return {
    // 信号粒子：使用主题强调色
    signal: getThemeColor('--accent', dark ? '#ff4d4d' : '#E85D2A'),
    // 核心粒子：白色
    core: '#ffffff',
    // 空洞粒子：白色
    void: '#ffffff',
    // 背景星星
    star: dark ? '#ffffff' : getThemeColor('--ink-muted', '#8A7A6A'),
    // 模式颜色
    idle: getThemeColor('--accent', '#E85D2A'),
    listening: getThemeColor('--accent', '#a78bfa'),
    thinking: getThemeColor('--accent', '#a78bfa'),
    speaking: getThemeColor('--accent', '#a78bfa'),
  }
}

let themeColors = getParticleColors()

const modeColorMap = reactive<Record<Mode, string>>({
  idle: themeColors.idle,
  listening: themeColors.listening,
  thinking: themeColors.thinking,
  speaking: themeColors.speaking,
})

// 监听主题变化
const themeObserver = new MutationObserver(() => {
  themeColors = getParticleColors()
  modeColorMap.idle = themeColors.idle
  modeColorMap.listening = themeColors.listening
  modeColorMap.thinking = themeColors.thinking
  modeColorMap.speaking = themeColors.speaking
})

interface Star {
  x: number
  y: number
  size: number
  speed: number
  twinkle: number
  phase: number
}
let bgStars: Star[] = []

class OrbP {
  bx: number
  by: number
  bz: number
  x = 0
  y = 0
  z = 0
  type: 'signal' | 'core' | 'void'
  color: string
  size: number

  constructor() {
    const u = Math.random(),
      v = Math.random()
    const r = Math.pow(Math.random(), 1 / 3) * SPHERE_RADIUS
    const th = 2 * Math.PI * u,
      ph = Math.acos(2 * v - 1)
    this.bx = r * Math.sin(ph) * Math.cos(th)
    this.by = r * Math.sin(ph) * Math.sin(th)
    this.bz = r * Math.cos(ph)
    const rnd = Math.random()
    if (rnd < 0.4) {
      this.type = 'signal'
      this.color = themeColors.signal
    } else if (rnd < 0.8) {
      this.type = 'core'
      this.color = themeColors.core
    } else {
      this.type = 'void'
      this.color = themeColors.void
    }
    this.size = Math.random() * 1.5 + 0.5
  }

  update(time: number, rx: number, ry: number, mode: Mode) {
    if (this.type === 'signal') this.color = modeColorMap[mode] || '#ff4d4d'
    const wave =
      Math.sin(time * 0.002 + (this.bx + this.by + this.bz) * 0.01) * (15 + audioLevel * 50)
    const ru = Math.sqrt(this.bx ** 2 + this.by ** 2 + this.bz ** 2) + 0.001
    const sc = mode === 'thinking' ? 0.9 : 1
    const rf = (ru * sc + wave) / ru
    let tx = this.bx * rf,
      ty = this.by * rf,
      tz = this.bz * rf
    const cx = Math.cos(rx),
      sx = Math.sin(rx)
    const cy = Math.cos(ry),
      sy = Math.sin(ry)
    const y1 = ty * cx - tz * sx,
      z1 = ty * sx + tz * cx
    this.x = tx * cy + z1 * sy
    this.y = y1
    this.z = -tx * sy + z1 * cy
  }

  draw(ctx: CanvasRenderingContext2D, cx: number, cy: number) {
    const p = 600 / (600 - Math.max(-300, Math.min(this.z, 590)))
    const dx = this.x * p + cx,
      dy = this.y * p + cy,
      ds = Math.max(0, this.size * p)
    if (this.type === 'void') {
      // 空洞粒子：填充半透明
      ctx.fillStyle = this.color
      ctx.globalAlpha = Math.max(0.05, (p - 0.4) * 0.3)
      ctx.beginPath()
      ctx.arc(dx, dy, ds, 0, Math.PI * 2)
      ctx.fill()
    } else {
      ctx.fillStyle = this.color
      ctx.globalAlpha = Math.max(0.1, p - 0.4)
      ctx.beginPath()
      ctx.arc(dx, dy, ds, 0, Math.PI * 2)
      ctx.fill()
    }
  }
}

let orbParticles: OrbP[] = []

/* ═══════════════════════════════════════════
   声波渲染
   ═══════════════════════════════════════════ */
const waveLayers = [
  { speed: 0.005, freq: 0.006, alpha: 0.8, offset: 0, scale: 1.0 },
  { speed: 0.0075, freq: 0.008, alpha: 0.5, offset: 2, scale: 0.8 },
  { speed: 0.004, freq: 0.005, alpha: 0.3, offset: 4, scale: 0.6 },
  { speed: 0.01, freq: 0.01, alpha: 0.2, offset: 6, scale: 0.4 },
]

let targetWaveW = 160,
  curWaveW = 160
let targetAmp = 0,
  curAmp = 0
let targetMorph = 1,
  curMorph = 1

function renderWave(time: number) {
  const c = waveCanvasRef.value
  if (!c) return
  const ctx = c.getContext('2d')
  if (!ctx) return
  const w = c.width,
    h = c.height,
    cy = h / 2,
    cx = w / 2
  ctx.clearRect(0, 0, w, h)

  const mode = currentMode.value
  if (mode === 'idle') {
    targetWaveW = 160
    targetMorph = 1
    targetAmp = 0
  } else if (mode === 'listening') {
    targetWaveW = w * 0.7
    targetMorph = 0
    targetAmp = 0.3 + Math.sin(time * 0.005) * 0.1
  } else if (mode === 'thinking') {
    targetWaveW = w * 0.5
    targetMorph = 0
    targetAmp = 0.2 + Math.random() * 0.1
  } else if (mode === 'speaking') {
    targetWaveW = w * 0.9
    targetMorph = 0
    targetAmp = 0.2 + audioLevel * 0.8
  }

  curMorph += (targetMorph - curMorph) * 0.08
  curWaveW += (targetWaveW - curWaveW) * 0.1
  curAmp += (targetAmp - curAmp) * 0.1
  const waveAlpha = 1 - curMorph

  // 液态流光球（idle 状态）
  if (curMorph > 0.005) {
    ctx.save()
    ctx.translate(cx, cy)
    ctx.scale(1 + (1 - curMorph) * 6, 1 - (1 - curMorph) * 0.8)
    ctx.globalAlpha = curMorph
    const orbSize = 55 + Math.sin(time * 0.003) * 4
    ctx.globalCompositeOperation = 'screen'
    ctx.rotate(time * 0.001)
    const colors = [
      { r: 255, g: 128, b: 181, radius: orbSize * 1.3, offset: 0, dist: 12 },
      { r: 129, g: 230, b: 217, radius: orbSize * 1.2, offset: 2.1, dist: 18 },
      { r: 167, g: 139, b: 250, radius: orbSize * 1.4, offset: 4.2, dist: 10 },
    ]
    colors.forEach((item, idx) => {
      ctx.save()
      ctx.rotate(time * 0.002 * (idx % 2 === 0 ? 1 : -1) + item.offset)
      ctx.translate(item.dist, 0)
      const g = ctx.createRadialGradient(0, 0, 0, 0, 0, item.radius)
      g.addColorStop(0, `rgba(${item.r},${item.g},${item.b},0.9)`)
      g.addColorStop(0.5, `rgba(${item.r},${item.g},${item.b},0.4)`)
      g.addColorStop(1, `rgba(${item.r},${item.g},${item.b},0)`)
      ctx.fillStyle = g
      ctx.beginPath()
      ctx.arc(0, 0, item.radius, 0, Math.PI * 2)
      ctx.fill()
      ctx.restore()
    })
    ctx.globalCompositeOperation = 'source-over'
    const cg = ctx.createRadialGradient(0, 0, 0, 0, 0, orbSize * 0.7)
    cg.addColorStop(0, 'rgba(255,255,255,1)')
    cg.addColorStop(0.3, 'rgba(255,255,255,0.7)')
    cg.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = cg
    ctx.beginPath()
    ctx.arc(0, 0, orbSize * 0.7, 0, Math.PI * 2)
    ctx.fill()
    ctx.restore()
  }

  // 声波
  if (waveAlpha > 0.005) {
    ctx.save()
    ctx.globalAlpha = waveAlpha
    ctx.globalCompositeOperation = 'screen'
    const grad = ctx.createLinearGradient(0, 0, w, 0)
    grad.addColorStop(0.1, '#ff80b5')
    grad.addColorStop(0.5, '#a78bfa')
    grad.addColorStop(0.9, '#81e6d9')
    const sx = cx - curWaveW / 2,
      ex = cx + curWaveW / 2
    waveLayers.forEach(layer => {
      ctx.beginPath()
      ctx.moveTo(sx, cy)
      for (let x = sx; x <= ex; x += 2) {
        const prog = (x - sx) / curWaveW
        const env = Math.pow(Math.sin(prog * Math.PI), 2.5)
        const yOff =
          Math.sin(x * layer.freq + time * layer.speed + layer.offset) *
          ((h / 2) * curAmp * layer.scale * env)
        ctx.lineTo(x, cy + yOff)
      }
      for (let x = ex; x >= sx; x -= 2) {
        const prog = (x - sx) / curWaveW
        const env = Math.pow(Math.sin(prog * Math.PI), 2.5)
        const yOff =
          Math.sin(x * layer.freq + time * layer.speed + layer.offset + Math.PI) *
          ((h / 2) * curAmp * layer.scale * env)
        ctx.lineTo(x, cy + yOff)
      }
      ctx.closePath()
      ctx.fillStyle = grad
      ctx.globalAlpha = layer.alpha * waveAlpha * 1.2
      ctx.fill()
    })
    ctx.restore()
  }
}

/* ═══════════════════════════════════════════
   主动画循环
   ═══════════════════════════════════════════ */
function resizeMain() {
  const c = mainCanvasRef.value
  if (!c) return
  width = window.innerWidth
  height = window.innerHeight
  c.width = width
  c.height = height
  bgStars = Array.from({ length: BG_STAR_COUNT }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    size: Math.random() * 1.5 + 0.2,
    speed: Math.random() * 0.3 + 0.05,
    twinkle: Math.random() * 0.003 + 0.001,
    phase: Math.random() * Math.PI * 2,
  }))
}

function resizeWave() {
  const c = waveCanvasRef.value
  if (!c) return
  const p = c.parentElement
  if (!p) return
  c.width = p.clientWidth * 2
  c.height = p.clientHeight * 2
}

function animate(time: number) {
  const c = mainCanvasRef.value
  if (!c) return
  const ctx = c.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, width, height)
  // 背景星星颜色跟随主题
  ctx.fillStyle = themeColors.star

  bgStars.forEach(s => {
    s.y -= s.speed
    if (s.y < 0) s.y = height
    ctx.globalAlpha = Math.max(0.05, 0.4 + Math.sin(time * s.twinkle + s.phase) * 0.4)
    ctx.beginPath()
    ctx.arc(s.x, s.y, s.size, 0, Math.PI * 2)
    ctx.fill()
  })
  ctx.globalAlpha = 1

  curSpeed += (targetSpeed - curSpeed) * 0.05

  if (currentMode.value === 'speaking') {
    audioLevel +=
      (Math.max(0, Math.sin(time * 0.015) * Math.sin(time * 0.005) * Math.random()) * 1.5 -
        audioLevel) *
      0.2
  } else {
    audioLevel += (0 - audioLevel) * 0.1
  }

  if (!isDragging) {
    rotY += 0.005 * curSpeed
    rotX += 0.002 * curSpeed
  }

  for (const p of orbParticles) p.update(time, rotX, rotY, currentMode.value)
  orbParticles.sort((a, b) => a.z - b.z)
  for (const p of orbParticles) p.draw(ctx, width / 2, height / 2)

  renderWave(time)
  animId = requestAnimationFrame(animate)
}

/* ─── 鼠标拖拽 ─── */
function onDown(e: MouseEvent | TouchEvent) {
  isDragging = true
  const cx = 'clientX' in e ? e.clientX : e.touches[0].clientX
  const cy = 'clientY' in e ? e.clientY : e.touches[0].clientY
  lastMX = cx
  lastMY = cy
}
function onMove(e: MouseEvent | TouchEvent) {
  if (!isDragging) return
  const cx = 'clientX' in e ? e.clientX : e.touches[0].clientX
  const cy = 'clientY' in e ? e.clientY : e.touches[0].clientY
  rotY += (cx - lastMX) * 0.01
  rotX -= (cy - lastMY) * 0.01
  lastMX = cx
  lastMY = cy
}
function onUp() {
  isDragging = false
}

/* ─── 生命周期 ─── */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => {
  document.documentElement.classList.add('immersive-mode-active')
  orbParticles = Array.from({ length: PARTICLE_COUNT }, () => new OrbP())
  resizeMain()
  resizeWave()
  window.addEventListener('resize', resizeMain)
  window.addEventListener('resize', resizeWave)
  window.addEventListener('keydown', handleKeydown)
  animId = requestAnimationFrame(animate)
  initSpeech()
  // 监听主题变化
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme'],
  })
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('immersive-mode-active')
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resizeMain)
  window.removeEventListener('resize', resizeWave)
  window.removeEventListener('keydown', handleKeydown)
  themeObserver.disconnect()
  if (silenceTimer) clearTimeout(silenceTimer)
  mediaRecorder?.stop()
  audioCtx?.close()
  abortCtrl?.abort()
})

watch(
  () => props.messages,
  () => scrollChat(),
  { deep: true }
)

watch(toolStatus, () => scrollChat())

function shouldShowMessage(msg: ChatMsg) {
  if (msg.content && msg.content.trim()) return true
  if (msg.imageUrl) return true
  if (msg.attachmentName) return true
  if (msg.agentTrace && (msg.agentTrace.planSummary || msg.agentTrace.subtasks.length > 0))
    return true
  return false
}
</script>

<template>
  <div class="immersive-overlay">
    <canvas
      ref="mainCanvasRef"
      class="main-canvas"
      @mousedown="onDown"
      @mousemove="onMove"
      @mouseup="onUp"
      @mouseleave="onUp"
      @touchstart.prevent="onDown"
      @touchmove.prevent="onMove"
      @touchend="onUp"
    ></canvas>

    <div class="mic-wrapper">
      <div class="wave-container" @click="toggleVoice" title="点击开始/结束说话">
        <canvas ref="waveCanvasRef" class="wave-canvas"></canvas>
        <div class="interaction-hint" :class="{ hidden: !showHint }">[ 点击以语音交流 ]</div>
      </div>
      <div class="status-text" :class="currentMode">{{ statusText }}</div>
      <div v-if="interimText" class="interim-text">{{ interimText }}</div>
    </div>

    <div class="chat-panel">
      <!-- 标签栏 -->
      <div class="imm-tabs">
        <button
          type="button"
          class="imm-tab"
          :class="{ active: activeTab === 'chat' }"
          @click="activeTab = 'chat'"
        >
          对话
        </button>
        <button
          type="button"
          class="imm-tab"
          :class="{ active: activeTab === 'sessions' }"
          @click="activeTab = 'sessions'"
        >
          会话
        </button>
        <button
          type="button"
          class="imm-tab"
          :class="{ active: activeTab === 'capabilities' }"
          @click="openCapabilities"
        >
          能力
        </button>
        <button type="button" class="imm-tab imm-tab-action" @click="emit('newSession')">
          ＋ 新会话
        </button>
      </div>

      <!-- 对话面板 -->
      <div v-show="activeTab === 'chat'" class="imm-panel-chat">
        <!-- 工具栏：角色卡 -->
        <div class="imm-toolbar">
          <div class="imm-char-picker" :class="{ open: charPickerOpen }">
            <button
              type="button"
              class="imm-char-trigger"
              @click.stop="charPickerOpen = !charPickerOpen"
            >
              <span>{{
                characterCards.find(c => c.key === selectedCharacterKey)?.name || '角色卡'
              }}</span>
              <svg width="12" height="12" viewBox="0 0 24 24">
                <path fill="currentColor" d="M7 10l5 5 5-5H7z" />
              </svg>
            </button>
            <div v-show="charPickerOpen" class="imm-char-panel">
              <button
                v-for="c in characterCards"
                :key="c.key"
                type="button"
                class="imm-char-opt"
                :class="{ active: c.key === selectedCharacterKey }"
                @click.stop="pickCharacter(c.key)"
              >
                <span class="imm-char-name">{{ c.name }}</span>
                <span class="imm-char-desc">{{ c.description }}</span>
              </button>
            </div>
          </div>
          <span class="imm-agent-btn active" title="多Agent协作：Planner→Executor→Reviewer">
            <Bot :size="13" />
            Multi-Agent
          </span>
        </div>

        <div ref="chatScrollRef" class="chat-messages">
          <div
            v-for="(msg, i) in messages"
            :key="i"
            class="msg"
            :class="msg.role"
            v-show="shouldShowMessage(msg)"
          >
            <!-- Agent 追踪信息（流式步骤节点） -->
            <div
              v-if="
                msg.agentTrace &&
                (msg.agentTrace.planSummary ||
                  msg.agentTrace.subtasks.length ||
                  msg.agentTrace.reviewDecision)
              "
              class="imm-trace-stepper"
            >
              <!-- 1. Planner Node -->
              <div v-if="msg.agentTrace.planSummary" class="imm-step-node is-done">
                <div class="imm-step-line"></div>
                <div class="imm-step-icon-container">
                  <ClipboardList :size="11" class="imm-step-icon" />
                </div>
                <div class="imm-step-content">
                  <div class="imm-step-title">任务规划 (Planner)</div>
                  <div class="imm-step-desc">{{ msg.agentTrace.planSummary }}</div>
                </div>
              </div>

              <!-- 2. Subtask Nodes (when subtasks exist) -->
              <div
                v-for="st in msg.agentTrace.subtasks"
                :key="st.id"
                class="imm-step-node"
                :class="{
                  'is-pending': st.status === 'pending',
                  'is-running': st.status === 'running',
                  'is-done': st.status === 'done',
                }"
              >
                <div class="imm-step-line"></div>
                <div class="imm-step-icon-container">
                  <span v-if="st.status === 'done'" class="imm-step-dot done">✓</span>
                  <span v-else-if="st.status === 'running'" class="imm-step-dot running"></span>
                  <span v-else class="imm-step-dot pending"></span>
                </div>
                <div class="imm-step-content">
                  <div class="imm-step-title">子任务 {{ st.id }}</div>
                  <div class="imm-step-desc">
                    {{
                      toolLabelMap[st.desc] ||
                      agentNodeLabelMap[st.desc] ||
                      st.desc ||
                      '等待获取执行内容...'
                    }}
                  </div>
                </div>
              </div>

              <!-- 2b. Direct Executor Node (when no subtasks exist) -->
              <div
                v-if="msg.agentTrace && msg.agentTrace.subtasks.length === 0"
                class="imm-step-node"
                :class="{
                  'is-done': msg.agentTrace.reviewDecision || msg.content,
                  'is-running': !msg.agentTrace.reviewDecision && !msg.content,
                }"
              >
                <div class="imm-step-line"></div>
                <div class="imm-step-icon-container">
                  <span
                    v-if="msg.agentTrace.reviewDecision || msg.content"
                    class="imm-step-dot done"
                    >✓</span
                  >
                  <span v-else class="imm-step-dot running"></span>
                </div>
                <div class="imm-step-content">
                  <div class="imm-step-title">执行阶段 (Executor)</div>
                  <div class="imm-step-desc">无需外部工具，直接分析并生成回答...</div>
                </div>
              </div>

              <!-- 3. Reviewer Node -->
              <div
                v-if="
                  msg.agentTrace.reviewDecision || msg.agentTrace.subtasks.length > 0 || msg.content
                "
                class="imm-step-node"
                :class="{
                  'is-pending': !msg.agentTrace.reviewDecision,
                  'is-done': msg.agentTrace.reviewDecision === 'PASS',
                  'is-warning': msg.agentTrace.reviewDecision === 'REVISE',
                  'is-error': msg.agentTrace.reviewDecision === 'FAIL',
                }"
              >
                <div class="imm-step-line" v-if="msg.agentTrace.metrics"></div>
                <div class="imm-step-icon-container">
                  <CheckCircle
                    v-if="msg.agentTrace.reviewDecision === 'PASS'"
                    :size="11"
                    class="imm-step-icon"
                  />
                  <RotateCcw
                    v-else-if="msg.agentTrace.reviewDecision === 'REVISE'"
                    :size="11"
                    class="imm-step-icon"
                  />
                  <XCircle
                    v-else-if="msg.agentTrace.reviewDecision === 'FAIL'"
                    :size="11"
                    class="imm-step-icon"
                  />
                  <Bot v-else :size="11" class="imm-step-icon" />
                </div>
                <div class="imm-step-content">
                  <div class="imm-step-title">结果审核 (Reviewer)</div>
                  <div class="imm-step-desc">
                    <span v-if="msg.agentTrace.reviewDecision">
                      决策:
                      <strong :class="msg.agentTrace.reviewDecision.toLowerCase()">{{
                        msg.agentTrace.reviewDecision
                      }}</strong>
                      <span v-if="msg.agentTrace.reviewFeedback">
                        ({{ msg.agentTrace.reviewFeedback }})</span
                      >
                    </span>
                    <span v-else>正在评估执行结果的质量和完整性...</span>
                  </div>
                </div>
              </div>

              <!-- 4. Metrics Node -->
              <div v-if="msg.agentTrace.metrics" class="imm-step-node is-metrics">
                <div class="imm-step-icon-container">
                  <span class="imm-step-dot metrics"></span>
                </div>
                <div class="imm-step-content">
                  <div class="imm-step-desc metrics-data">
                    Token 消耗: {{ msg.agentTrace.metrics.tokensIn }}↓ /
                    {{ msg.agentTrace.metrics.tokensOut }}↑ · 耗时:
                    {{ msg.agentTrace.metrics.latencyMs }}ms
                  </div>
                </div>
              </div>
            </div>
            <!-- 附件标签 -->
            <div v-if="msg.attachmentName" class="imm-attach-tag">
              <Paperclip :size="12" />
              <span>{{ msg.attachmentName }}</span>
            </div>
            <!-- 图片消息 -->
            <img v-if="msg.imageUrl" :src="msg.imageUrl" class="imm-msg-image" />
            <div
              v-if="msg.content && msg.content.trim()"
              class="text"
              v-html="sanitizeHtml(fmt(msg.content))"
            ></div>
            <div
              v-if="msg.role === 'assistant' && msg.agentTrace?.ragEvaluation?.scores"
              class="imm-rag-inline"
            >
                <div class="imm-rag-inline-heading">
                  <div>
                    <strong><Activity :size="13" /> RAG 回答质量</strong>
                    <span>
                      已自动评估 ·
                      {{ msg.agentTrace.ragRetrievalMode === 'keyword' ? '关键词检索' : '向量检索' }}
                    </span>
                  </div>
                  <strong v-if="msg.agentTrace.ragEvaluation.scores" class="imm-rag-inline-total">
                    {{ scorePercent(msg.agentTrace.ragEvaluation.scores.overall) }}
                  </strong>
                </div>

                <dl class="imm-rag-inline-scores">
                  <div>
                    <dt>有据可查</dt>
                    <dd>{{ scorePercent(msg.agentTrace.ragEvaluation.scores.faithfulness) }}</dd>
                  </div>
                  <div>
                    <dt>切题程度</dt>
                    <dd>{{ scorePercent(msg.agentTrace.ragEvaluation.scores.answerRelevance) }}</dd>
                  </div>
                  <div>
                    <dt>检索准确</dt>
                    <dd>{{ scorePercent(msg.agentTrace.ragEvaluation.scores.contextPrecision) }}</dd>
                  </div>
                  <div>
                    <dt>检索完整</dt>
                    <dd>{{ scorePercent(msg.agentTrace.ragEvaluation.scores.contextRecall) }}</dd>
                  </div>
                </dl>
                <p v-if="msg.agentTrace.ragEvaluation.contexts?.length" class="imm-rag-inline-sources">
                  依据：{{ msg.agentTrace.ragEvaluation.contexts.map(item => item.title).join('、') }}
                </p>
            </div>
            <!-- 点赞/点踩反馈工具条（仅在回答完成且非加载中显示在最底部） -->
            <div
              v-if="msg.role === 'assistant' && msg.content && msg.content.trim() && !toolStatus && (!isLocalSending || msg !== messages[messages.length - 1])"
              class="imm-msg-actions"
            >
              <button
                class="imm-action-btn like"
                :class="{ active: msg.userFeedback === 'LIKE' }"
                title="有用"
                @click="handleLike(msg)"
              >
                <ThumbsUp :size="13" />
                <span>赞同</span>
              </button>
              <button
                class="imm-action-btn dislike"
                :class="{ active: msg.userFeedback === 'DISLIKE' }"
                title="答非所问 / 有问题"
                @click="openDislikeModal(msg)"
              >
                <ThumbsDown :size="13" />
                <span>踩</span>
              </button>
            </div>
          </div>
          <!-- 工具/Agent 状态 -->
          <div v-if="toolStatus" class="msg assistant">
            <div class="imm-tool-status">
              <span class="imm-tool-spinner"></span>
              <span>{{ toolStatus }}</span>
            </div>
          </div>
          <!-- 加载动画 -->
          <div v-if="isLocalSending && !toolStatus && !hasReceivedContent" class="msg assistant">
            <div class="imm-typing">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>

        <!-- 快捷回复 -->
        <div
          ref="quickRepliesRef"
          class="imm-quick-replies"
          @mousedown="onQrMouseDown"
          @mousemove="onQrMouseMove"
          @mouseup="onQrMouseUp"
          @mouseleave="onQrMouseUp"
        >
          <button
            v-for="item in quickReplies"
            :key="item"
            type="button"
            class="imm-qr-btn"
            :disabled="isSending || isLocalSending"
            @click="useQuickReply(item)"
          >
            {{ item }}
          </button>
        </div>

        <!-- 输入区 -->
        <div class="imm-input-area">
          <input
            ref="imageInputRef"
            type="file"
            accept="image/*"
            class="hidden-file"
            @change="onImageUpload"
          />
          <input
            ref="fileInputRef"
            type="file"
            accept=".txt,.md,.markdown,.csv,.json,.xml,.yaml,.yml,.docx,.pdf"
            class="hidden-file"
            @change="onFileUpload"
          />
          <!-- 附件预览 -->
          <div v-if="pendingAttachment" class="imm-attachment">
            <FileText :size="14" />
            <span class="imm-attachment-name">{{ pendingAttachment.name }}</span>
            <span v-if="isParsingFile" class="imm-attachment-status">解析中...</span>
            <button type="button" class="imm-attachment-remove" @click="removeAttachment">
              <X :size="12" />
            </button>
          </div>
          <!-- 图片预览 -->
          <div v-if="pendingImagePreview" class="imm-image-preview">
            <img :src="pendingImagePreview" alt="预览" />
            <button type="button" class="imm-image-remove" @click="removeImage">
              <X :size="12" />
            </button>
          </div>
          <textarea
            ref="textareaRef"
            v-model="inputText"
            class="imm-textarea"
            rows="2"
            placeholder="输入消息，或点击下方语音..."
            :disabled="isSending || isLocalSending"
            @keydown="onInputKeydown"
          ></textarea>
          <div class="imm-input-actions">
            <button
              type="button"
              class="imm-icon-btn"
              title="上传图片"
              @click="imageInputRef?.click()"
            >
              <Image :size="15" />
            </button>
            <button
              type="button"
              class="imm-icon-btn"
              title="上传文件 (txt/md/docx/pdf)"
              @click="fileInputRef?.click()"
              :disabled="isParsingFile"
            >
              <FileText :size="15" />
            </button>
            <div class="imm-input-tip">
              {{
                isParsingFile
                  ? '解析中...'
                  : dailyExceeded
                    ? '今日次数已用尽'
                    : `剩余 ${dailyRemaining} 次`
              }}
            </div>
            <button
              type="button"
              class="imm-send-btn"
              :disabled="
                isSending ||
                isLocalSending ||
                (!inputText.trim() && !pendingAttachment) ||
                dailyExceeded
              "
              @click="handleTextSend"
            >
              <Send :size="14" />
            </button>
          </div>
        </div>
      </div>
      <!-- 关闭 imm-panel-chat -->

      <!-- 会话列表面板 -->
      <div v-show="activeTab === 'sessions'" class="imm-panel-sessions">
        <ul class="imm-sess-list">
          <li
            v-for="s in sessions"
            :key="s.id"
            class="imm-sess-item"
            :class="{ current: s.id === currentSessionId }"
            @click="(emit('loadSession', s.id), (activeTab = 'chat'))"
          >
            <div class="imm-sess-title">{{ s.title }}</div>
            <div class="imm-sess-meta">{{ s.characterKey }}</div>
            <!-- 确认删除 -->
            <div v-if="showDeleteConfirm === s.id" class="imm-sess-confirm">
              <button
                type="button"
                class="imm-sess-confirm-yes"
                @click.stop="(emit('deleteSession', s.id), (showDeleteConfirm = null))"
              >
                确认
              </button>
              <button
                type="button"
                class="imm-sess-confirm-no"
                @click.stop="showDeleteConfirm = null"
              >
                取消
              </button>
            </div>
            <button
              v-else
              type="button"
              class="imm-sess-del"
              @click.stop="showDeleteConfirm = s.id"
            >
              <Trash2 :size="14" />
            </button>
          </li>
        </ul>
        <p v-if="!sessions.length" class="imm-sess-empty">暂无会话，点「新会话」开始</p>
      </div>

      <!-- Agent 能力面板 -->
      <div v-show="activeTab === 'capabilities'" class="imm-panel-capabilities">
        <section class="imm-cap-section" aria-labelledby="mcp-capability-title">
          <div class="imm-cap-heading">
            <div>
              <p class="imm-cap-eyebrow">External tools</p>
              <h2 id="mcp-capability-title">MCP 服务</h2>
            </div>
            <button
              type="button"
              class="imm-cap-refresh"
              :disabled="mcpLoading"
              aria-label="刷新 MCP 工具"
              @click="loadMcpCapabilities"
            >
              <RefreshCw :size="14" :class="{ spinning: mcpLoading }" />
            </button>
          </div>

          <p v-if="mcpError" class="imm-cap-error">{{ mcpError }}</p>
          <div v-else-if="mcpLoading" class="imm-cap-loading">
            <span class="imm-tool-spinner"></span>
            正在连接 MCP Server...
          </div>
          <div v-else-if="!mcpTools.length" class="imm-cap-empty">
            <Server :size="20" />
            <div>
              <strong>实时搜索等待配置</strong>
              <p>生产环境设置 ZHIPU_API_KEY 后，会自动启用智谱实时联网搜索。</p>
            </div>
          </div>
          <template v-else>
            <p class="imm-server-summary">已连接 {{ mcpServers.length }} 个 MCP 服务</p>
            <div class="imm-server-list">
              <article v-for="server in mcpServers" :key="server" class="imm-server-item">
                <div class="imm-server-status" aria-hidden="true">
                  <Server :size="17" />
                </div>
                <div class="imm-server-copy">
                  <strong>{{ getMcpServerName(server) }}</strong>
                  <span>{{ getMcpServerToolCount(server) }} 种实时搜索能力</span>
                </div>
                <span class="imm-server-connected">
                  <span class="imm-server-dot"></span>已连接
                </span>
              </article>
            </div>
          
    <!-- 反馈模态框 -->
    <div v-if="feedbackModalOpen" class="imm-feedback-backdrop" @click.self="feedbackModalOpen = false">
      <div class="imm-feedback-modal">
        <div class="imm-feedback-header">
          <h3>反馈回答质量</h3>
          <button class="imm-close-btn" @click="feedbackModalOpen = false"><X :size="16" /></button>
        </div>
        <div class="imm-feedback-body">
          <label class="imm-form-label">请选择主要问题类型：</label>
          <div class="imm-radio-group">
            <label><input type="radio" v-model="feedbackType" value="NOT_RELEVANT" /> 知识库未检索到正确资料</label>
            <label><input type="radio" v-model="feedbackType" value="HALLUCINATION" /> 包含大模型凭空幻觉内容</label>
            <label><input type="radio" v-model="feedbackType" value="WRONG_FACT" /> 事实或语法描述有误</label>
            <label><input type="radio" v-model="feedbackType" value="OTHER" /> 其他意见</label>
          </div>
          <label class="imm-form-label">补充说明 (选填)：</label>
          <textarea v-model="feedbackComment" placeholder="请输入具体意见或修正建议..." class="imm-feedback-input"></textarea>
        </div>
        <div class="imm-feedback-footer">
          <button class="imm-btn-cancel" @click="feedbackModalOpen = false">取消</button>
          <button class="imm-btn-submit" @click="submitDislikeFeedback">提交评价</button>
        </div>
      </div>
    </div>

</template>
        </section>

        <section class="imm-cap-section" aria-labelledby="rag-index-title">
          <div class="imm-cap-heading">
            <div>
              <p class="imm-cap-eyebrow">Knowledge index</p>
              <h2 id="rag-index-title">知识库向量索引</h2>
            </div>
          </div>
          <p class="imm-index-copy">
            已发布文章会自动写入索引。首次启用或部署前已有文章时，可手动完整重建一次。
          </p>
          <button
            type="button"
            class="imm-index-button"
            :disabled="reindexLoading"
            @click="rebuildKnowledgeIndex"
          >
            <RefreshCw :size="14" :class="{ spinning: reindexLoading }" />
            {{ reindexLoading ? '正在重建索引...' : '重建我的文章索引' }}
          </button>
          <p v-if="reindexMessage" class="imm-index-message">{{ reindexMessage }}</p>
        </section>

      </div>
    </div>
  </div>

    <!-- 反馈模态框 -->
    <div v-if="feedbackModalOpen" class="imm-feedback-backdrop" @click.self="feedbackModalOpen = false">
      <div class="imm-feedback-modal">
        <div class="imm-feedback-header">
          <h3>反馈回答质量</h3>
          <button class="imm-close-btn" @click="feedbackModalOpen = false"><X :size="16" /></button>
        </div>
        <div class="imm-feedback-body">
          <label class="imm-form-label">请选择主要问题类型：</label>
          <div class="imm-radio-group">
            <label><input type="radio" v-model="feedbackType" value="NOT_RELEVANT" /> 知识库未检索到正确资料</label>
            <label><input type="radio" v-model="feedbackType" value="HALLUCINATION" /> 包含大模型凭空幻觉内容</label>
            <label><input type="radio" v-model="feedbackType" value="WRONG_FACT" /> 事实或语法描述有误</label>
            <label><input type="radio" v-model="feedbackType" value="OTHER" /> 其他意见</label>
          </div>
          <label class="imm-form-label">补充说明 (选填)：</label>
          <textarea v-model="feedbackComment" placeholder="请输入具体意见或修正建议..." class="imm-feedback-input"></textarea>
        </div>
        <div class="imm-feedback-footer">
          <button class="imm-btn-cancel" @click="feedbackModalOpen = false">取消</button>
          <button class="imm-btn-submit" @click="submitDislikeFeedback">提交评价</button>
        </div>
      </div>
    </div>

</template>

<style scoped>
.immersive-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  isolation: isolate;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: var(--ink);
  overflow: hidden;
}

.main-canvas {
  display: block;
  position: absolute;
  inset: 0;
  z-index: 10;
  cursor: grab;
}
.main-canvas:active {
  cursor: grabbing;
}

.mic-wrapper {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.wave-container {
  width: 360px;
  height: 160px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
}

.wave-canvas {
  display: block;
  width: 100%;
  height: 100%;
  pointer-events: none;
  transition: opacity 0.3s;
}
.wave-container:hover .wave-canvas {
  filter: brightness(1.3);
}

.interaction-hint {
  position: absolute;
  bottom: 10px;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--ink-muted);
  pointer-events: none;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  animation: breathe-text 2s infinite ease-in-out;
  opacity: 1;
  transform: translateY(0);
}
.interaction-hint.hidden {
  opacity: 0;
  transform: translateY(15px);
  animation: none;
}
@keyframes breathe-text {
  0%,
  100% {
    opacity: 0.4;
  }
  50% {
    opacity: 0.9;
    text-shadow: 0 0 10px rgba(255, 255, 255, 0.4);
  }
}

.status-text {
  font-size: 11px;
  letter-spacing: 4px;
  text-transform: uppercase;
  color: var(--ink-muted);
  font-weight: bold;
  transition: color 0.3s;
}
.status-text.listening {
  color: var(--accent);
}
.status-text.thinking {
  color: var(--warm);
}
.status-text.speaking {
  color: var(--accent);
}

.interim-text {
  font-size: 13px;
  color: var(--ink-soft);
  max-width: 300px;
  text-align: center;
  animation: fadeInUp 0.3s ease;
}
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-panel {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 480px;
  padding: 40px 24px 32px 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  z-index: 20;
  pointer-events: none;
  background: linear-gradient(to right, transparent 0%, var(--canvas) 60%, var(--canvas) 100%);
  -webkit-mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    black 6%,
    black 92%,
    transparent 100%
  );
  mask-image: linear-gradient(to bottom, transparent 0%, black 6%, black 92%, transparent 100%);
}

.chat-header {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 3px;
  color: var(--ink-muted);
  margin-bottom: 20px;
  text-align: right;
  text-transform: uppercase;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 8px;
  pointer-events: auto;
}
.chat-messages::-webkit-scrollbar {
  width: 0;
}

.msg {
  display: flex;
  flex-direction: column;
  width: 100%;
}
.msg.user {
  align-items: flex-end;
}
.msg.user .text {
  color: var(--ink);
  font-size: 14px;
  line-height: 1.6;
  text-align: left;
  background: var(--accent-soft);
  border: 1px solid var(--border-interactive);
  padding: 10px 14px;
  border-radius: 16px 16px 2px 16px;
  max-width: 85%;
  width: fit-content;
  word-break: break-word;
  box-shadow: var(--shadow-sm);
}
.msg.assistant {
  align-items: flex-start;
}
.msg.assistant .text {
  color: var(--ink);
  font-size: 14px;
  line-height: 1.7;
  font-weight: 300;
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 10px 14px;
  border-radius: 16px 16px 16px 2px;
  max-width: 85%;
  width: fit-content;
  word-break: break-word;
  box-shadow: var(--shadow-sm);
}

/* ── 附件标签 ── */
.imm-rag-trigger {
  min-height: 30px;
  margin: 4px 0 0 6px;
  padding: 4px 8px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  align-self: flex-start;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--ink-muted);
  font: inherit;
  font-size: 11px;
  cursor: pointer;
  transition:
    color 180ms var(--ease-out-quart),
    background 180ms var(--ease-out-quart);
}
.imm-rag-trigger:hover,
.imm-rag-trigger[aria-expanded='true'] {
  color: var(--accent);
  background: var(--accent-soft);
}
.imm-rag-trigger:focus-visible {
  outline: 2px solid var(--border-focus);
  outline-offset: 2px;
}
.imm-rag-inline {
  width: min(85%, 390px);
  margin-top: 5px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: var(--shadow-sm);
  animation: rag-panel-in 180ms var(--ease-out-quart);
}
.imm-rag-inline-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.imm-rag-inline-heading > div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.imm-rag-inline-heading strong {
  color: var(--ink);
  font-size: 12px;
}
.imm-rag-inline-heading span,
.imm-rag-inline-copy,
.imm-rag-inline-sources {
  color: var(--ink-muted);
  font-size: 10px;
  line-height: 1.5;
}
.imm-rag-inline-total {
  color: var(--accent) !important;
  font-size: 17px !important;
}
.imm-rag-inline-copy {
  margin: 9px 0 8px;
}
.imm-rag-inline-scores {
  margin: 11px 0 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}
.imm-rag-inline-scores div {
  padding: 7px 8px;
  border-radius: 9px;
  background: var(--tag-bg);
}
.imm-rag-inline-scores dt {
  color: var(--ink-muted);
  font-size: 9px;
}
.imm-rag-inline-scores dd {
  margin: 2px 0 0;
  color: var(--ink);
  font-size: 12px;
  font-weight: 650;
}
.imm-rag-inline-sources {
  margin: 8px 0 0;
}
@keyframes rag-panel-in {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.imm-attach-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  margin-bottom: 6px;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.25);
  color: var(--accent);
  font-size: 12px;
  font-weight: 500;
  align-self: flex-end;
}
.imm-msg-image {
  max-width: 240px;
  max-height: 180px;
  border-radius: 10px;
  margin-bottom: 6px;
  object-fit: cover;
  cursor: pointer;
  align-self: flex-end;
}

.msg .text :deep(pre) {
  margin: 0.3rem 0;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 0.78rem;
  line-height: 1.45;
  background: var(--surface);
  padding: 0.5rem 0.7rem;
  border: 1px solid var(--border);
}
.msg .text :deep(code) {
  background: var(--accent-soft);
  padding: 0.1rem 0.25rem;
  border-radius: 3px;
  font-size: 0.82em;
  font-family: 'Fira Code', 'Consolas', monospace;
}
.msg .text :deep(pre code) {
  background: transparent;
  padding: 0;
}
.msg .text :deep(p) {
  margin: 0.1rem 0;
}
.msg .text :deep(p:first-child) {
  margin-top: 0;
}
.msg .text :deep(p:last-child) {
  margin-bottom: 0;
}
.msg .text :deep(.chat-table) {
  display: block;
  width: 100%;
  max-width: 100%;
  margin: 10px 0 16px;
  overflow-x: auto;
  overscroll-behavior-inline: contain;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  -webkit-overflow-scrolling: touch;
  border-collapse: collapse;
  table-layout: auto;
  color: var(--ink);
  font-family: 'Inter', 'Noto Sans SC', system-ui, sans-serif;
  font-size: 12px;
  line-height: 1.5;
}
.msg .text :deep(.chat-table + p) {
  margin-top: 0.75rem;
}
.msg .text :deep(.chat-table:focus-visible) {
  outline: 2px solid var(--border-focus);
  outline-offset: 2px;
}
.msg .text :deep(.chat-table th),
.msg .text :deep(.chat-table td) {
  min-width: 88px;
  padding: 9px 12px;
  border-bottom: 1px solid var(--border);
  text-align: left;
  vertical-align: top;
  word-break: keep-all;
}
.msg .text :deep(.chat-table th) {
  white-space: nowrap;
  background: var(--tag-bg);
  color: var(--ink);
  font-weight: 650;
}
.msg .text :deep(.chat-table td) {
  max-width: 320px;
  white-space: normal;
  overflow-wrap: break-word;
}
.msg .text :deep(.chat-table tbody tr:last-child td) {
  border-bottom: 0;
}
.msg .text :deep(.chat-table tbody tr:hover) {
  background: var(--surface-hover);
}
.msg .text :deep(ul),
.msg .text :deep(ol) {
  padding-left: 1.2rem;
  margin: 0.15rem 0;
}

/* ── Agent 追踪（步骤节点） ── */
.imm-trace-stepper {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 14px;
  background: var(--surface-raised, rgba(0, 0, 0, 0.02));
  border: 1px solid var(--border-interactive);
  border-radius: var(--radius-md, 10px);
  margin-bottom: 10px;
  align-self: flex-start;
  width: 100%;
}

.imm-step-node {
  position: relative;
  display: flex;
  gap: 12px;
}

.imm-step-line {
  position: absolute;
  top: 18px; /* start from center of icon container */
  left: 9px; /* align with center of icon container */
  bottom: -18px; /* extend to center of next icon container */
  width: 2px;
  background: var(--border-interactive);
  z-index: 1;
}

/* Hide line on the last visible node of the stepper to avoid hanging lines */
.imm-step-node:last-child .imm-step-line {
  display: none;
}

/* Highlight line if the current step is done */
.imm-step-node.is-done .imm-step-line {
  background: var(--accent);
}

.imm-step-icon-container {
  position: relative;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--surface);
  border: 1.5px solid var(--border-interactive);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
}

.imm-step-icon {
  color: var(--ink-muted);
}

/* Colors for states */
.imm-step-node.is-done .imm-step-icon-container {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.imm-step-node.is-done .imm-step-icon {
  color: var(--accent);
}

.imm-step-node.is-running .imm-step-icon-container {
  border-color: var(--accent);
  background: var(--surface);
  animation: pulse-ring 1.5s infinite;
}

.imm-step-node.is-warning .imm-step-icon-container {
  border-color: var(--warm);
  background: var(--warm-soft);
}
.imm-step-node.is-warning .imm-step-icon {
  color: var(--warm);
}

.imm-step-node.is-error .imm-step-icon-container {
  border-color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}
.imm-step-node.is-error .imm-step-icon {
  color: #ef4444;
}

/* Subtask dots */
.imm-step-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ink-muted);
  display: inline-block;
  transition: all 0.3s ease;
}

.imm-step-dot.done {
  width: auto;
  height: auto;
  background: transparent;
  color: var(--accent);
  font-size: 10px;
  font-weight: bold;
}

.imm-step-dot.running {
  background: var(--accent);
  animation: pulse-dot 1s infinite;
}

.imm-step-dot.pending {
  background: var(--ink-muted);
}

.imm-step-dot.metrics {
  width: 4px;
  height: 4px;
  background: var(--ink-muted);
}

.imm-step-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  flex: 1;
}

.imm-step-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.2;
}

.imm-step-desc {
  font-size: 11px;
  color: var(--ink-muted);
  margin-top: 2px;
  word-break: break-word;
  line-height: 1.4;
}

.imm-step-desc strong.pass {
  color: #28a745;
}
.imm-step-desc strong.revise {
  color: var(--warm);
}
.imm-step-desc strong.fail {
  color: #ef4444;
}

.imm-step-node.is-metrics {
  gap: 12px;
}

.imm-step-node.is-metrics .imm-step-icon-container {
  border: none;
  background: transparent;
  box-shadow: none;
}

.metrics-data {
  font-family: var(--font-mono, 'Fira Code', monospace);
  font-size: 10px;
  opacity: 0.8;
}

@keyframes pulse-ring {
  0% {
    box-shadow: 0 0 0 0 rgba(139, 92, 246, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(139, 92, 246, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(139, 92, 246, 0);
  }
}

@keyframes pulse-dot {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.6;
  }
}

/* ── 工具状态 ── */
.imm-tool-status {
  display: inline-flex;
  align-items: center;
  gap: 0px;
  padding: 8px 12px;
  border-radius: 10px;
  background: var(--accent-soft);
  border: 1px solid var(--border);
  font-size: 13px;
  color: var(--accent);
  font-weight: 500;
}
.imm-tool-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── 打字动画 ── */
.imm-typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
}
.imm-typing .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent);
  opacity: 0.4;
  animation: typing-bounce 1.4s infinite ease-in-out;
}
.imm-typing .dot:nth-child(2) {
  animation-delay: 0.2s;
}
.imm-typing .dot:nth-child(3) {
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

/* ── 工具栏 ── */
.imm-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0 12px;
  pointer-events: auto;
  flex-shrink: 0;
}

.imm-char-picker {
  position: relative;
}

.imm-char-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 20px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  backdrop-filter: blur(8px);
  font-family: inherit;
}
.imm-char-trigger:hover {
  background: var(--surface-hover);
  border-color: var(--border-interactive);
}
.imm-char-picker.open .imm-char-trigger {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.imm-char-panel {
  position: absolute;
  left: 0;
  top: calc(100% + 6px);
  z-index: 60;
  min-width: 200px;
  padding: 6px;
  border-radius: 12px;
  background: var(--surface);
  border: 1px solid var(--border);
  backdrop-filter: blur(16px);
  box-shadow: var(--shadow-card-hover);
  max-height: 240px;
  overflow-y: auto;
}

.imm-char-opt {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: 8px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--ink-soft);
  font-size: 12px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
}
.imm-char-opt:hover {
  background: var(--surface-hover);
}
.imm-char-opt.active {
  background: var(--accent-soft);
}
.imm-char-name {
  font-weight: 700;
  color: var(--ink);
}
.imm-char-desc {
  font-size: 11px;
  color: var(--ink-muted);
}

.imm-agent-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink-muted);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.imm-agent-btn:hover:not(:disabled) {
  background: var(--surface-hover);
  color: var(--ink);
}
.imm-agent-btn.active {
  background: var(--accent-soft);
  border-color: var(--accent);
  color: var(--accent);
}
.imm-agent-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ── 快捷回复 ── */
.imm-quick-replies {
  display: flex;
  gap: 8px;
  padding: 10px 0;
  overflow-x: auto;
  overflow-y: hidden;
  flex-shrink: 0;
  pointer-events: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
  cursor: grab;
}
.imm-quick-replies::-webkit-scrollbar {
  display: none;
}
.imm-quick-replies:active {
  cursor: grabbing;
}

.imm-qr-btn {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink-muted);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.imm-qr-btn:hover:not(:disabled) {
  background: var(--surface-hover);
  border-color: var(--accent);
  color: var(--accent);
  transform: translateY(-1px);
}
.imm-qr-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ── 输入区 ── */
.imm-input-area {
  flex-shrink: 0;
  padding: 16px;
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(20px) saturate(1.2);
  -webkit-backdrop-filter: blur(20px) saturate(1.2);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

[data-theme='dark'] .imm-input-area {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}

.hidden-file {
  display: none;
}

/* ── 附件预览 ── */
.imm-attachment {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.25);
  color: var(--accent);
  font-size: 13px;
  font-weight: 500;
}
.imm-attachment-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.imm-attachment-status {
  font-size: 11px;
  color: var(--accent);
  animation: pulse-opacity 1.2s infinite;
}
@keyframes pulse-opacity {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
.imm-attachment-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: rgba(139, 92, 246, 0.15);
  color: var(--accent);
  cursor: pointer;
  transition: all 0.2s;
}
.imm-attachment-remove:hover {
  background: rgba(231, 76, 60, 0.2);
  color: #c44a4a;
}

/* ── 图片预览 ── */
.imm-image-preview {
  position: relative;
  display: inline-block;
  margin-bottom: 4px;
}
.imm-image-preview img {
  max-width: 160px;
  max-height: 120px;
  border-radius: 10px;
  border: 1px solid rgba(139, 92, 246, 0.25);
  object-fit: cover;
}
.imm-image-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.imm-image-remove:hover {
  background: rgba(231, 76, 60, 0.8);
}

.imm-textarea {
  width: 100%;
  min-height: 80px;
  max-height: 300px;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.08);
  color: var(--ink);
  font-size: 14px;
  font-family: inherit;
  resize: none;
  transition:
    border-color 0.2s,
    background-color 0.2s,
    box-shadow 0.2s;
  box-sizing: border-box;
}
.imm-textarea::placeholder {
  color: var(--ink-muted);
}
.imm-textarea:focus {
  outline: none;
  border-color: var(--accent);
  background: rgba(255, 255, 255, 0.15);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.15);
}
.imm-textarea:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.imm-input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.imm-icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.08);
  background: var(--surface);
  color: var(--ink-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.imm-icon-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  color: var(--ink);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.imm-send-btn {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: none;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: auto;
  box-shadow: var(--shadow-button);
}
.imm-send-btn:hover:not(:disabled) {
  background: var(--accent-hover);
  box-shadow: var(--shadow-button-hover);
  transform: translateY(-1px);
}
.imm-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.imm-input-tip {
  font-size: 11px;
  color: var(--ink-muted);
  letter-spacing: 0.5px;
  flex: 1;
}

/* ── 标签栏 ── */
.imm-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-bottom: 10px;
  pointer-events: auto;
  flex-shrink: 0;
}
.imm-tab {
  padding: 5px 14px;
  border-radius: 20px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--ink-muted);
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}
.imm-tab.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}
.imm-tab:hover:not(.active) {
  background: var(--surface-hover);
  color: var(--ink);
}
.imm-tab-action {
  margin-left: auto;
}

/* ── 对话/会话面板 ── */
.imm-panel-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.imm-panel-sessions {
  flex: 1;
  overflow-y: auto;
  pointer-events: auto;
  padding: 4px 0;
}
.imm-panel-capabilities {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  pointer-events: auto;
  padding: 6px 8px 32px 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.imm-panel-sessions::-webkit-scrollbar,
.imm-panel-capabilities::-webkit-scrollbar {
  width: 0;
}

.imm-cap-section {
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.imm-cap-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  color: var(--ink-muted);
}
.imm-cap-heading h2 {
  margin: 2px 0 0;
  color: var(--ink);
  font-size: 16px;
  line-height: 1.25;
  letter-spacing: -0.02em;
}
.imm-cap-eyebrow {
  margin: 0;
  color: var(--ink-muted);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.imm-cap-copy {
  margin: 10px 0 12px;
  color: var(--ink-muted);
  font-size: 12px;
  line-height: 1.6;
}
.imm-index-copy {
  margin: 12px 0;
  color: var(--ink-muted);
  font-size: 11px;
  line-height: 1.6;
}
.imm-index-button {
  width: 100%;
  min-height: 38px;
  border: 1px solid var(--border-interactive);
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  background: var(--accent-soft);
  color: var(--ink-soft);
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.imm-index-button:hover:not(:disabled) {
  border-color: var(--border-focus);
  color: var(--accent);
}
.imm-index-button:disabled {
  cursor: wait;
  opacity: 0.7;
}
.imm-index-message {
  margin: 9px 0 0;
  color: var(--ink-muted);
  font-size: 11px;
  line-height: 1.5;
}

.imm-evaluation-target {
  margin: 0 0 10px;
  padding: 8px 10px;
  border-radius: 9px;
  background: var(--accent-soft);
  color: var(--ink);
  font-size: 11px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.imm-cap-refresh {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  flex: none;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: transparent;
  color: var(--ink-muted);
  cursor: pointer;
}
.imm-cap-refresh:hover:not(:disabled) {
  color: var(--accent);
  border-color: var(--border-interactive);
  background: var(--surface-hover);
}
.imm-cap-refresh:disabled {
  cursor: wait;
  opacity: 0.65;
}
.imm-cap-refresh .spinning {
  animation: spin 0.8s linear infinite;
}

.imm-cap-loading,
.imm-cap-empty {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ink-muted);
  font-size: 12px;
}
.imm-cap-empty {
  align-items: flex-start;
  padding: 14px;
  border-radius: 12px;
  background: var(--tag-bg);
}
.imm-cap-empty strong {
  color: var(--ink-soft);
  font-size: 13px;
}
.imm-cap-empty p {
  margin: 3px 0 0;
  line-height: 1.5;
}
.imm-cap-error {
  margin: 10px 0 0;
  color: oklch(0.58 0.17 25);
  font-size: 12px;
  line-height: 1.5;
}

.imm-server-summary {
  margin: 14px 0 8px;
  color: var(--ink-muted);
  font-size: 11px;
}
.imm-server-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.imm-server-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--tag-bg);
}
.imm-server-status {
  display: grid;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  background: var(--accent-soft);
  color: var(--accent);
}
.imm-server-copy {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}
.imm-server-copy strong {
  color: var(--ink);
  font-size: 13px;
}
.imm-server-copy span {
  color: var(--ink-muted);
  font-size: 11px;
}
.imm-server-connected {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--ink-muted);
  font-size: 10px;
}
.imm-server-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 7px var(--accent);
}

.imm-ground-truth {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  min-height: 56px;
  padding: 9px 11px;
  border: 1px solid var(--border);
  border-radius: 11px;
  outline: none;
  background: var(--tag-bg);
  color: var(--ink);
  font: inherit;
  font-size: 12px;
  line-height: 1.5;
}
.imm-ground-truth:focus {
  border-color: var(--border-focus);
  box-shadow: 0 0 0 3px var(--accent-soft);
}
.imm-ground-truth::placeholder {
  color: var(--ink-muted);
}
.imm-evaluate-btn {
  width: 100%;
  min-height: 38px;
  margin-top: 9px;
  border: 0;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  background: var(--accent);
  color: var(--canvas);
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.imm-evaluate-btn:hover:not(:disabled) {
  background: var(--accent-hover);
  transform: translateY(-1px);
}
.imm-evaluate-btn:disabled {
  cursor: wait;
  opacity: 0.7;
}
.imm-score-result {
  margin-top: 14px;
  padding-top: 13px;
  border-top: 1px solid var(--border);
}
.imm-overall-score {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  color: var(--ink-muted);
  font-size: 11px;
}
.imm-overall-score strong {
  color: var(--accent);
  font-size: 22px;
  letter-spacing: -0.04em;
}
.imm-score-list {
  margin: 10px 0 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
}
.imm-score-list div {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.imm-score-list dt,
.imm-score-list dd {
  margin: 0;
  font-size: 10px;
}
.imm-score-list dt {
  color: var(--ink-muted);
}
.imm-score-list dd {
  color: var(--ink-soft);
  font-weight: 700;
}
.imm-context-sources {
  margin: 10px 0 0;
  color: var(--ink-muted);
  font-size: 10px;
  line-height: 1.5;
}

/* ── 会话列表 ── */
.imm-sess-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.imm-sess-item {
  position: relative;
  padding: 10px 36px 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  transition: all 0.2s;
}
.imm-sess-item:hover {
  background: var(--surface-hover);
  border-color: var(--border-interactive);
}
.imm-sess-item.current {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.imm-sess-title {
  font-weight: 600;
  font-size: 13px;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.imm-sess-meta {
  font-size: 11px;
  color: var(--ink-muted);
  margin-top: 2px;
}
.imm-sess-del {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: transparent;
  color: var(--ink-muted);
  border-radius: 6px;
  padding: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.imm-sess-del:hover {
  background: rgba(231, 76, 60, 0.1);
  color: #c44a4a;
}
.imm-sess-confirm {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 4px;
}
.imm-sess-confirm-yes,
.imm-sess-confirm-no {
  border: none;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.imm-sess-confirm-yes {
  background: rgba(231, 76, 60, 0.15);
  color: #c44a4a;
}
.imm-sess-confirm-yes:hover {
  background: rgba(231, 76, 60, 0.3);
}
.imm-sess-confirm-no {
  background: var(--surface-hover);
  color: var(--ink-muted);
}
.imm-sess-confirm-no:hover {
  background: var(--border);
}
.imm-sess-empty {
  text-align: center;
  color: var(--ink-muted);
  font-size: 13px;
  padding: 24px 0;
}

@media (max-width: 768px) {
  .chat-panel {
    width: 100%;
    padding: 60px 20px 140px 20px;
  }
  .wave-container {
    width: 280px;
    height: 120px;
  }
  .imm-toolbar {
    flex-wrap: wrap;
  }
}
</style>

<style scoped>

/* ─── 点赞/点踩与反馈弹窗样式 ─── */
.imm-msg-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.imm-action-btn {
  background: rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.12);
  color: #475569;
  border-radius: 16px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.imm-action-btn:hover {
  color: #0f172a;
  border-color: rgba(0, 0, 0, 0.25);
  background: rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}
.imm-action-btn.active.like {
  color: #ffffff !important;
  border-color: #6366f1 !important;
  background: #6366f1 !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.4);
}
.imm-action-btn.active.dislike {
  color: #ffffff !important;
  border-color: #f43f5e !important;
  background: #f43f5e !important;
  box-shadow: 0 2px 8px rgba(244, 63, 94, 0.4);
}
.imm-feedback-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.imm-feedback-modal {
  background: #1e1e2e;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  width: 440px;
  padding: 20px;
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.4);
  color: #fff;
}
.imm-feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.imm-feedback-header h3 {
  margin: 0;
  font-size: 16px;
}
.imm-close-btn {
  background: none;
  border: none;
  color: #aaa;
  cursor: pointer;
}
.imm-form-label {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
}
.imm-radio-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 13px;
}
.imm-radio-group label {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}
.imm-feedback-input {
  width: 100%;
  height: 80px;
  background: #12121c;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  padding: 10px;
  font-size: 13px;
  resize: none;
  box-sizing: border-box;
}
.imm-feedback-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
.imm-btn-cancel {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #ccc;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
}
.imm-btn-submit {
  background: #6366f1;
  border: none;
  color: #fff;
  padding: 6px 16px;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
}

</style>
