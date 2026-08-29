<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import VoiceWaveStage from './immersive/VoiceWaveStage.vue'
import CharacterCardPicker from './immersive/CharacterCardPicker.vue'
import AgentTraceStepper, { type AgentTrace } from './immersive/AgentTraceStepper.vue'
import AgentConfigDrawer from './immersive/AgentConfigDrawer.vue'
import RagFeedbackModal from './immersive/RagFeedbackModal.vue'
import {
  buildMultiAgentSseUrl,
  evaluateRag,
  getMcpTools,
  reindexKnowledgeBase,
  getAiModels,
  getAgentConfig,
  updateAgentConfig,
  submitMessageFeedback,
  type CharacterCard,
  type McpToolInfo,
  type RagEvaluationResult,
} from '@/api/ai'
import { guestChat } from '@/api/guest-ai'
import { useUserStore } from '@/stores/user'
import { sanitizeHtml } from '@/utils/sanitize'
import { getAuthToken } from '@/utils/authToken'
import {
  ThumbsUp,
  ThumbsDown,
  BookOpen,
  Sliders,
  Image,
  FileText,
  Send,
  Trash2,
  X,
  Paperclip,
  Activity,
  RefreshCw,
  Server,
} from '@lucide/vue'

const userStore = useUserStore()

type ChatMsg = {
  id?: number
  role: 'user' | 'assistant'
  content: string
  reasoningContent?: string
  imageUrl?: string
  agentTrace?: AgentTrace
  attachmentName?: string
  userFeedback?: 'LIKE' | 'DISLIKE' | null
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

/* ─── 子组件 Ref 与 交互状态 ─── */
const voiceStageRef = ref<InstanceType<typeof VoiceWaveStage> | null>(null)
const currentEmotion = ref('02')

function setEmotion(emoId: string) {
  currentEmotion.value = emoId
}

/* ─── 状态 ─── */
type Mode = 'idle' | 'listening' | 'thinking' | 'speaking'
const currentMode = ref<Mode>('idle')
const statusText = ref('SYSTEM READY')
const showHint = ref(true)
const isLocalSending = ref(false)
const interimText = ref('')

/* ─── 用户反馈点赞点踩 ─── */
const feedbackModalOpen = ref(false)
const feedbackTargetMessageId = ref<number | null>(null)

async function handleLike(msg: any) {
  msg.userFeedback = msg.userFeedback === 'LIKE' ? null : 'LIKE'
  const targetId = msg.id || 9999
  if (msg.userFeedback === 'LIKE') {
    setEmotion('19')
  }
  try {
    await submitMessageFeedback(targetId, {
      sessionId: props.currentSessionId || 0,
      rating: 'LIKE'
    })
  } catch {}
}

function openDislikeModal(msg: any) {
  msg.userFeedback = 'DISLIKE'
  feedbackTargetMessageId.value = msg.id || 9999
  feedbackModalOpen.value = true
  setEmotion('12')
}

async function handleFeedbackSubmit(data: { type: string; comment: string }) {
  const targetId = feedbackTargetMessageId.value
  try {
    await submitMessageFeedback(targetId || 9999, {
      sessionId: props.currentSessionId || 0,
      rating: 'DISLIKE',
      feedbackType: data.type,
      comment: data.comment
    })
  } catch {}
  feedbackModalOpen.value = false
}

/* ─── 文字输入 ─── */
const inputText = ref('')
const textareaRef = ref<HTMLTextAreaElement | null>(null)

watch(inputText, () => {
  nextTick(() => {
    const el = textareaRef.value
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${el.scrollHeight}px`
  })
})

/* ─── 会话列表 ─── */
const activeTab = ref<'chat' | 'sessions' | 'capabilities' | 'config'>('chat')
const showDeleteConfirm = ref<number | null>(null)

/* ─── Agent 检索调参 ─── */
const agentConfigSavedHint = ref('')
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

async function openAgentConfig() {
  activeTab.value = 'config'
  agentConfigSavedHint.value = ''
  try {
    const modelRes = await getAiModels()
    if (modelRes && modelRes.models && modelRes.models.length) {
      availableModels.value = modelRes.models.filter(m => !m.id.includes('embedding'))
    }
  } catch {}
  try {
    const res = await getAgentConfig()
    if (res.success && res.data) {
      const dbModel = res.data.modelName
      agentConfigForm.modelName = (dbModel && availableModels.value.some(m => m.id === dbModel))
        ? dbModel
        : 'deepseek-v4-flash'
      agentConfigForm.similarityThreshold = res.data.similarityThreshold ?? 0.6
      agentConfigForm.topK = res.data.topK ?? 5
      agentConfigForm.temperature = res.data.temperature ?? 0.7
      agentConfigForm.enableRerank = res.data.enableRerank ?? 1
    }
  } catch {}
}

async function saveAgentConfig() {
  agentConfigSavedHint.value = ''
  try {
    await updateAgentConfig(agentConfigForm)
    agentConfigSavedHint.value = '✓ 参数保存成功！'
  } catch (e: any) {
    agentConfigSavedHint.value = '保存失败: ' + (e.message || '未知错误')
  }
}

/* ─── Agent 能力检查 ─── */
const mcpTools = ref<McpToolInfo[]>([])
const mcpServers = ref<string[]>([])
const mcpLoading = ref(false)
const mcpError = ref('')
const reindexLoading = ref(false)
const reindexMessage = ref('')

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
  '写一篇技术文章大纲',
  '推荐几个学习方向',
  '帮我查一下后端相关文章',
  '介绍一下 Starlore 项目',
  '给新文章起个标题',
]

/* ─── 图片/文件 附件 ─── */
const imageInputRef = ref<HTMLInputElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const isParsingFile = ref(false)
const pendingImage = ref<string | null>(null)
const pendingImagePreview = ref<string | null>(null)
const pendingAttachment = ref<{ name: string; text: string } | null>(null)

function removeAttachment() {
  pendingAttachment.value = null
}
function removeImage() {
  pendingImage.value = null
  pendingImagePreview.value = null
}

const isRecording = ref(false)
const chatScrollRef = ref<HTMLElement | null>(null)

/* ─── 语音识别（MediaRecorder + MiMo ASR + 静音自动停止）─── */
const speechSupported = ref(false)
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let silenceTimer: ReturnType<typeof setTimeout> | null = null
let audioCtx: AudioContext | null = null
let analyser: AnalyserNode | null = null
let micStream: MediaStream | null = null

function initSpeech() {
  speechSupported.value = !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)
}

function toggleVoice() {
  if (isRecording.value) {
    stopRecording(true)
  } else {
    startRecording()
  }
}

async function startRecording() {
  if (isLocalSending.value || props.isSending) return
  try {
    micStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
    analyser = audioCtx.createAnalyser()
    analyser.fftSize = 256
    const source = audioCtx.createMediaStreamSource(micStream)
    source.connect(analyser)

    mediaRecorder = new MediaRecorder(micStream)
    audioChunks = []
    mediaRecorder.ondataavailable = e => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    mediaRecorder.onstop = () => {
      if (audioChunks.length > 0) {
        const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })
        sendAudioToAsr(audioBlob)
      }
    }
    mediaRecorder.start(200)
    isRecording.value = true
    currentMode.value = 'listening'
    statusText.value = 'Listening...'
    interimText.value = ''
    showHint.value = false
    setEmotion('16')
    detectSilence()
  } catch (err) {
    console.error('录音权限获取失败:', err)
    statusText.value = 'Mic Error'
    setTimeout(() => {
      statusText.value = 'System Ready'
      currentMode.value = 'idle'
    }, 2000)
  }
}

function detectSilence() {
  if (!analyser || !isRecording.value) return
  const data = new Uint8Array(analyser.frequencyBinCount)
  const check = () => {
    if (!isRecording.value || !analyser) return
    analyser.getByteFrequencyData(data)
    const sum = data.reduce((a, b) => a + b, 0)
    const avg = sum / data.length
    if (avg < 10) {
      if (!silenceTimer) {
        silenceTimer = setTimeout(() => {
          if (isRecording.value) stopRecording(true)
        }, 2200)
      }
    } else {
      if (silenceTimer) {
        clearTimeout(silenceTimer)
        silenceTimer = null
      }
    }
    requestAnimationFrame(check)
  }
  requestAnimationFrame(check)
}

function stopRecording(send: boolean) {
  if (!isRecording.value) return
  isRecording.value = false
  if (silenceTimer) {
    clearTimeout(silenceTimer)
    silenceTimer = null
  }
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    if (!send) audioChunks = []
    mediaRecorder.stop()
  }
  if (micStream) {
    micStream.getTracks().forEach(t => t.stop())
    micStream = null
  }
  if (!send) {
    currentMode.value = 'idle'
    statusText.value = 'System Ready'
    showHint.value = true
    setEmotion('02')
  } else {
    currentMode.value = 'thinking'
    statusText.value = 'Transcribing...'
  }
}

async function sendAudioToAsr(blob: Blob) {
  try {
    const formData = new FormData()
    formData.append('audio', blob, 'audio.webm')
    const token = getAuthToken()
    const res = await fetch('/api/ai/asr', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: formData,
    })
    const json = await res.json()
    if (json.text && json.text.trim()) {
      interimText.value = json.text
      handleSend(json.text)
    } else {
      statusText.value = 'No Speech Detected'
      currentMode.value = 'idle'
      setEmotion('08')
      setTimeout(() => {
        statusText.value = 'System Ready'
        showHint.value = true
        setEmotion('02')
      }, 2000)
    }
  } catch (err) {
    console.error('ASR 请求失败:', err)
    statusText.value = 'ASR Error'
    currentMode.value = 'idle'
    setTimeout(() => {
      statusText.value = 'System Ready'
      showHint.value = true
    }, 2000)
  }
}

/* ─── SSE 流式响应处理 ─── */
let abortCtrl: AbortController | null = null
const toolStatus = ref('')

const toolLabelMap: Record<string, string> = {
  get_articles: '正在读取知识库文章列表...',
  get_article: '正在获取文章详细内容...',
  search_articles: '正在通过关键词搜索知识库...',
  semantic_search: '正在执行向量语义检索与相关度匹配...',
  get_categories: '正在加载分类列表...',
  web_search: '正在联网检索最新资料...',
  generate_creative_mindmap: '正在生成思维发散导图...',
}

const agentNodeLabelMap: Record<string, string> = {
  planner: '任务规划中：分析意图并拆解子任务...',
  executor: '执行阶段：并行调用外部工具与数据检索...',
  reviewer: '审查阶段：核验输出质量与事实一致性...',
}

async function handleSend(userText: string) {
  const content = userText.trim()
  if (!content && !pendingAttachment.value && !pendingImage.value) return
  if (isLocalSending.value || props.isSending) return

  let finalContent = content
  if (pendingAttachment.value) {
    finalContent = `[附件: ${pendingAttachment.value.name}]\n${pendingAttachment.value.text}\n\n${content}`
  }

  const userMsg: ChatMsg = {
    role: 'user',
    content: finalContent,
    imageUrl: pendingImagePreview.value || undefined,
    attachmentName: pendingAttachment.value?.name || undefined,
  }
  props.messages.push(userMsg)

  const currentAttachName = pendingAttachment.value?.name
  const currentImgPreview = pendingImagePreview.value
  const currentImgBase64 = pendingImage.value
  inputText.value = ''
  pendingAttachment.value = null
  pendingImage.value = null
  pendingImagePreview.value = null
  if (textareaRef.value) textareaRef.value.style.height = 'auto'

  isLocalSending.value = true
  currentMode.value = 'thinking'
  statusText.value = 'Thinking...'
  showHint.value = false
  toolStatus.value = 'Planner 正在分析您的请求，拆解为可执行的子任务...'
  const assistantMsg: ChatMsg = {
    role: 'assistant',
    content: '',
    reasoningContent: '',
    agentTrace: {
      planSummary: '正在分析您的请求，拆解为可执行的子任务...',
      subtasks: [],
      reviewDecision: '',
      reviewFeedback: '',
      retryCount: 0,
      metrics: null,
    },
  }
  props.messages.push(assistantMsg)

  const isGuest = !userStore.token
  if (isGuest) {
    try {
      const history = props.messages.slice(0, -1).map(m => ({ role: m.role, content: m.content }))
      const res = await guestChat(finalContent, history, props.selectedCharacterKey)
      assistantMsg.content = res.content
      isLocalSending.value = false
      currentMode.value = 'speaking'
      statusText.value = 'Speaking...'
      setEmotion('19')
      emit('send', finalContent, res.content)
      emit('refreshQuota')
      speakText(res.content)
    } catch (err: any) {
      assistantMsg.content = err?.message || '请求失败，请稍后重试'
      isLocalSending.value = false
      currentMode.value = 'idle'
      statusText.value = 'System Ready'
      showHint.value = true
      setEmotion('04')
    }
    return
  }

  abortCtrl = new AbortController()
  try {
    const history = props.messages
      .slice(0, -2)
      .filter(m => m.content && m.content.trim())
      .map(m => ({ role: m.role, content: m.content }))

    const requestBody = {
      messages: [...history, { role: 'user', content: finalContent }],
      characterKey: props.selectedCharacterKey,
      model: agentConfigForm.modelName,
      imageUrl: currentImgBase64 || undefined,
      sessionId: props.currentSessionId || 0,
    }

    const token = getAuthToken()
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    }
    if (token) headers['Authorization'] = `Bearer ${token}`

    const response = await fetch(`/api/ai/multi-agent-sse?model=${encodeURIComponent(agentConfigForm.modelName)}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(requestBody),
      signal: abortCtrl.signal,
    })

    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const reader = response.body?.getReader()
    if (!reader) throw new Error('No readable stream')

    const decoder = new TextDecoder()
    let buffer = ''
    let fullText = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const raw = line.slice(5).trim()
        if (!raw || raw === '[DONE]') continue

        try {
          const parsed = JSON.parse(raw)
          if (parsed.type === 'plan_start') {
            toolStatus.value = 'Planner 正在分析您的请求，拆解为可执行的子任务...'
            setEmotion('14')
            scrollChat()
          } else if (parsed.type === 'plan') {
            assistantMsg.agentTrace = {
              planSummary: parsed.summary || '任务拆解完成',
              subtasks: (parsed.subtasks || []).map((st: any, idx: number) => {
                const text = typeof st === 'object' ? (st.description || st.toolHint || JSON.stringify(st)) : String(st)
                return {
                  id: (typeof st === 'object' && st.id) ? st.id : idx + 1,
                  desc: text,
                  status: 'pending',
                }
              }),
              reviewDecision: '',
              reviewFeedback: '',
              retryCount: 0,
              metrics: null,
            }
            toolStatus.value = `任务拆解完成: 共 ${assistantMsg.agentTrace.subtasks.length} 个子任务，开始执行...`
            setEmotion('06')
            scrollChat()
          } else if (parsed.type === 'subtask_running') {
            if (assistantMsg.agentTrace) {
              const st = assistantMsg.agentTrace.subtasks.find(s => s.id === parsed.subtask_id)
              if (st) st.status = 'running'
              assistantMsg.agentTrace = {
                ...assistantMsg.agentTrace,
                subtasks: [...assistantMsg.agentTrace.subtasks],
              }
              toolStatus.value = `正在执行子任务 ${parsed.subtask_id}: ${st?.desc || ''}`
            }
            setEmotion('14')
            scrollChat()
          } else if (parsed.type === 'subtask_result') {
            if (assistantMsg.agentTrace) {
              const st = assistantMsg.agentTrace.subtasks.find(s => s.id === parsed.subtask_id)
              if (st) st.status = 'done'
              const doneCount = assistantMsg.agentTrace.subtasks.filter(s => s.status === 'done').length
              const total = assistantMsg.agentTrace.subtasks.length
              assistantMsg.agentTrace = {
                ...assistantMsg.agentTrace,
                subtasks: [...assistantMsg.agentTrace.subtasks],
              }
              toolStatus.value = doneCount < total
                ? `子任务 ${doneCount}/${total} 完成，继续执行...`
                : `全部子任务执行完毕，进入审查阶段...`
            }
            scrollChat()
          } else if (parsed.type === 'tool_start') {
            const label = toolLabelMap[parsed.tool] || `正在调用 ${parsed.tool}...`
            toolStatus.value = label
            setEmotion('14')
            scrollChat()
          } else if (parsed.type === 'tool_end') {
            toolStatus.value = ''
            scrollChat()
          } else if (parsed.type === 'review') {
            if (assistantMsg.agentTrace) {
              assistantMsg.agentTrace = {
                ...assistantMsg.agentTrace,
                reviewDecision: parsed.decision || '',
                reviewFeedback: parsed.feedback || '',
              }
            }
            if (parsed.decision === 'FAIL') {
              toolStatus.value = `审核未通过: ${parsed.feedback || ''}，正在重新规划...`
              setEmotion('08')
            } else if (parsed.decision === 'REVISE') {
              toolStatus.value = `正在修正补充: ${parsed.feedback || ''}`
              setEmotion('14')
            } else {
              toolStatus.value = '审查通过，正在生成最终回答...'
              setEmotion('06')
            }
            scrollChat()
          } else if (parsed.type === 'retry') {
            if (assistantMsg.agentTrace) {
              assistantMsg.agentTrace = {
                ...assistantMsg.agentTrace,
                retryCount: parsed.count || 0,
              }
            }
            toolStatus.value = `正在进行第 ${parsed.count} 次重试...`
            setEmotion('08')
            scrollChat()
          } else if (parsed.type === 'metrics') {
            if (assistantMsg.agentTrace) {
              assistantMsg.agentTrace = {
                ...assistantMsg.agentTrace,
                metrics: {
                  tokensIn: parsed.total_tokens_in || parsed.tokensIn || 0,
                  tokensOut: parsed.total_tokens_out || parsed.tokensOut || 0,
                  latencyMs: parsed.total_latency_ms || parsed.latencyMs || 0,
                },
              }
            }
            scrollChat()
          } else if (parsed.type === 'rag_context' || parsed.type === 'rag_contexts') {
            if (assistantMsg.agentTrace) {
              assistantMsg.agentTrace = {
                ...assistantMsg.agentTrace,
                ragContexts: parsed.articles || parsed.contexts || [],
                ragRetrievalMode: parsed.retrieval_mode || parsed.mode || 'vector',
              }
            }
            scrollChat()
          } else if (parsed.type === 'content' || parsed.type === 'token' || parsed.content) {
            const chunk = parsed.content || ''
            fullText += chunk
            assistantMsg.content = fullText
            toolStatus.value = ''
            scrollChat()
          } else if (parsed.type === 'reasoning' || parsed.reasoning_content) {
            const chunk = parsed.content || parsed.reasoning_content || ''
            assistantMsg.reasoningContent = (assistantMsg.reasoningContent || '') + chunk
            scrollChat()
          } else if (parsed.type === 'error' || parsed.error) {
            assistantMsg.content = `[错误] ${parsed.message || parsed.error || '生成失败'}`
            setEmotion('04')
            scrollChat()
          }
        } catch {
          if (raw) {
            fullText += raw
            assistantMsg.content = fullText
            scrollChat()
          }
        }
      }
    }

    toolStatus.value = ''
    isLocalSending.value = false
    currentMode.value = 'speaking'
    statusText.value = 'Speaking...'
    setEmotion('19')
    emit('send', finalContent, fullText, assistantMsg.agentTrace ? JSON.stringify(assistantMsg.agentTrace) : undefined)
    emit('refreshQuota')
    if (assistantMsg.agentTrace && assistantMsg.agentTrace.ragContexts?.length) {
      void runAutomaticRagEvaluation(assistantMsg.agentTrace, finalContent, fullText)
    }
    speakText(fullText)
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      console.error('SSE 流式错误:', err)
      assistantMsg.content = assistantMsg.content || '请求异常中断，请重试'
      setEmotion('04')
    }
    toolStatus.value = ''
    isLocalSending.value = false
    currentMode.value = 'idle'
    statusText.value = 'System Ready'
    showHint.value = true
  }
}

function speakText(text: string) {
  if (!text || !('speechSynthesis' in window)) {
    finishSpeaking()
    return
  }
  window.speechSynthesis.cancel()
  const clean = text.replace(/[#*`_~\[\]()]/g, '').slice(0, 300)
  const utter = new SpeechSynthesisUtterance(clean)
  utter.lang = 'zh-CN'
  utter.rate = 1.05
  utter.onend = finishSpeaking
  utter.onerror = finishSpeaking
  window.speechSynthesis.speak(utter)
}

function finishSpeaking() {
  currentMode.value = 'idle'
  statusText.value = 'System Ready'
  showHint.value = true
  setEmotion('02')
}

function scrollChat() {
  nextTick(() => {
    if (chatScrollRef.value) {
      chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
    }
  })
}

function handleTextSend() {
  handleSend(inputText.value)
}

function onInputKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleTextSend()
  }
}

function onFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  isParsingFile.value = true
  const reader = new FileReader()
  reader.onload = () => {
    pendingAttachment.value = { name: file.name, text: (reader.result as string) || '' }
    isParsingFile.value = false
  }
  reader.onerror = () => {
    isParsingFile.value = false
  }
  reader.readAsText(file)
  input.value = ''
}

function onImageSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const b64 = reader.result as string
    pendingImage.value = b64
    pendingImagePreview.value = b64
  }
  reader.readAsDataURL(file)
  input.value = ''
}

function pickCharacter(key: string) {
  emit('update:selectedCharacterKey', key)
  setEmotion('06')
}

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

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => {
  document.documentElement.classList.add('immersive-mode-active')
  window.addEventListener('keydown', handleKeydown)
  initSpeech()
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('immersive-mode-active')
  window.removeEventListener('keydown', handleKeydown)
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
  if (msg.role === 'user') return true
  if (msg.content && msg.content.trim()) return true
  if (msg.imageUrl || msg.attachmentName) return true
  if (
    msg.agentTrace &&
    (msg.agentTrace.planSummary ||
      (msg.agentTrace.subtasks && msg.agentTrace.subtasks.length > 0) ||
      msg.agentTrace.reviewDecision)
  ) {
    return true
  }
  return isLocalSending.value && msg === props.messages[props.messages.length - 1]
}
</script>

<template>
  <div class="immersive-overlay">
    <!-- 左侧吉祥物与语音交互舞台 (5:5 均分布局) -->
    <VoiceWaveStage
      ref="voiceStageRef"
      :current-emotion="currentEmotion"
      :current-mode="currentMode"
      :status-text="statusText"
      :interim-text="interimText"
      :show-hint="showHint"
      @toggle-voice="toggleVoice"
    />

    <!-- 右侧会话与控制面板 (5:5 均分布局) -->
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
        <button
          type="button"
          class="imm-tab"
          :class="{ active: activeTab === 'config' }"
          @click="openAgentConfig"
        >
          <Sliders :size="13" style="margin-right: 4px;" />
          调参
        </button>
        <button type="button" class="imm-tab imm-tab-action" @click="emit('newSession')">
          ＋ 新会话
        </button>
      </div>

      <!-- 对话面板 -->
      <div v-show="activeTab === 'chat'" class="imm-panel-chat">
        <!-- 角色卡选择器 -->
        <CharacterCardPicker
          :character-cards="characterCards"
          :selected-character-key="selectedCharacterKey"
          @select="pickCharacter"
        />

        <div ref="chatScrollRef" class="chat-messages">
          <div
            v-for="(msg, i) in messages"
            :key="i"
            class="msg"
            :class="msg.role"
          >
            <!-- Agent 追踪信息（流式步骤节点） -->
            <AgentTraceStepper
              v-if="msg.agentTrace && (msg.agentTrace.planSummary || msg.agentTrace.subtasks.length || msg.agentTrace.reviewDecision)"
              :agent-trace="msg.agentTrace"
              :has-content="Boolean(msg.content)"
              :tool-label-map="toolLabelMap"
              :agent-node-label-map="agentNodeLabelMap"
            />

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

            <!-- RAG 评估卡片 -->
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
              <div v-if="msg.agentTrace.ragEvaluation.contexts?.length" class="imm-rag-citation-list">
                <span class="imm-citation-label"><BookOpen :size="12" /> 知识库引用来源：</span>
                <div class="imm-citation-badges">
                  <div
                    v-for="(ctx, idx) in msg.agentTrace.ragEvaluation.contexts"
                    :key="idx"
                    class="imm-citation-badge"
                  >
                    <span class="imm-citation-num">[{{ idx + 1 }}]</span>
                    <span class="imm-citation-title">{{ ctx.title }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 点赞/点踩反馈工具条 -->
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
            v-for="qr in quickReplies"
            :key="qr"
            type="button"
            class="imm-qr-btn"
            :disabled="isSending || isLocalSending"
            @click="handleSend(qr)"
          >
            {{ qr }}
          </button>
        </div>

        <!-- 隐藏的 input -->
        <input
          ref="fileInputRef"
          type="file"
          accept=".txt,.md,.markdown,.docx,.pdf"
          style="display: none"
          @change="onFileSelect"
        />
        <input
          ref="imageInputRef"
          type="file"
          accept="image/*"
          style="display: none"
          @change="onImageSelect"
        />

        <!-- 输入区域 -->
        <div class="imm-input-box">
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

      <!-- Agent 调参面板 -->
      <AgentConfigDrawer
        v-show="activeTab === 'config'"
        :agent-config-form="agentConfigForm"
        :available-models="availableModels"
        :agent-config-saved-hint="agentConfigSavedHint"
        @save="saveAgentConfig"
      />

      <!-- MCP 能力面板 -->
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
  <RagFeedbackModal
    :open="feedbackModalOpen"
    @close="feedbackModalOpen = false"
    @submit="handleFeedbackSubmit"
  />
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

/* ── 右侧会话面板 (左右55开，顶部留足 76px 留白避让顶部导航栏) ── */
.chat-panel {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 50vw;
  max-width: 600px;
  min-width: 380px;
  padding: 76px 36px 24px 32px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  z-index: 20;
  pointer-events: none;
  background: linear-gradient(to right, transparent 0%, var(--canvas) 45%, var(--canvas) 100%);
  -webkit-mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    black 6%,
    black 94%,
    transparent 100%
  );
  mask-image: linear-gradient(to bottom, transparent 0%, black 6%, black 94%, transparent 100%);
}

@media (max-width: 900px) {
  .chat-panel {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    padding: 76px 16px 24px 16px;
    background: var(--canvas);
  }
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

.imm-rag-inline {
  width: min(85%, 390px);
  margin-top: 5px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: var(--shadow-sm);
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
.imm-rag-inline-heading span {
  color: var(--ink-muted);
  font-size: 10px;
}
.imm-rag-inline-total {
  color: var(--accent) !important;
  font-size: 17px !important;
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

.imm-rag-citation-list {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed var(--border);
}
.imm-citation-label {
  font-size: 11px;
  color: var(--ink-muted);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}
.imm-citation-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.imm-citation-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--surface-secondary, rgba(0, 0, 0, 0.04));
  border: 1px solid var(--border);
  font-size: 11px;
  color: var(--ink);
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

.imm-tool-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
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
  to { transform: rotate(360deg); }
}

.imm-msg-actions {
  display: flex;
  gap: 6px;
  margin-top: 4px;
}
.imm-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid transparent;
  background: transparent;
  font-size: 11px;
  color: var(--ink-muted);
  cursor: pointer;
  transition: all 0.15s;
}
.imm-action-btn:hover {
  background: var(--hover-bg);
  color: var(--ink);
}
.imm-action-btn.active {
  color: var(--accent);
  background: var(--accent-soft);
  border-color: var(--border-interactive);
}

.imm-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 8px;
  pointer-events: auto;
  flex-shrink: 0;
}
.imm-tab {
  padding: 6px 14px;
  border: 1px solid transparent;
  background: transparent;
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--ink-soft);
  cursor: pointer;
  transition: all 0.2s;
}
.imm-tab:hover {
  background: var(--hover-bg, rgba(0, 0, 0, 0.04));
  color: var(--ink);
}
.imm-tab.active {
  background: var(--ink);
  color: #FFFFFF !important;
  font-weight: 600;
  border-color: var(--ink);
}
.imm-tab-action {
  margin-left: auto;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink);
}
.imm-tab-action:hover {
  border-color: var(--ink);
}

.imm-panel-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.imm-quick-replies {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 10px 0;
  pointer-events: auto;
  user-select: none;
}
.imm-quick-replies::-webkit-scrollbar {
  height: 0;
}
.imm-qr-btn {
  padding: 6px 14px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}
.imm-qr-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.imm-input-box {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: auto;
  box-shadow: var(--shadow-sm);
}

.imm-textarea {
  width: 100%;
  box-sizing: border-box;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--ink);
  resize: none;
  font-family: inherit;
}

.imm-input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.imm-icon-btn {
  background: none;
  border: none;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 6px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}
.imm-icon-btn:hover {
  background: var(--hover-bg);
  color: var(--ink);
}

.imm-input-tip {
  margin-left: auto;
  font-size: 11px;
  color: var(--ink-muted);
}

.imm-send-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: var(--accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.imm-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.imm-attachment, .imm-image-preview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.04));
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  width: fit-content;
}
.imm-image-preview img {
  height: 36px;
  width: 36px;
  object-fit: cover;
  border-radius: 4px;
}
.imm-attachment-remove, .imm-image-remove {
  background: none;
  border: none;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
}

.imm-panel-sessions {
  flex: 1;
  overflow-y: auto;
  pointer-events: auto;
}
.imm-sess-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.imm-sess-item {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  transition: all 0.2s;
}
.imm-sess-item:hover {
  border-color: var(--border-interactive);
}
.imm-sess-item.current {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.imm-sess-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.imm-sess-meta {
  font-size: 11px;
  color: var(--ink-muted);
  margin-right: 12px;
}
.imm-sess-del {
  background: none;
  border: none;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 4px;
}
.imm-sess-del:hover {
  color: #ef4444;
}
.imm-sess-confirm {
  display: flex;
  gap: 6px;
}
.imm-sess-confirm button {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  cursor: pointer;
}
.imm-sess-confirm-yes {
  background: #ef4444;
  color: white;
  border: none;
}
.imm-sess-confirm-no {
  background: var(--surface);
  border: 1px solid var(--border);
  color: var(--ink-soft);
}
.imm-sess-empty {
  text-align: center;
  color: var(--ink-muted);
  font-size: 13px;
  margin-top: 40px;
}

.imm-panel-capabilities {
  flex: 1;
  overflow-y: auto;
  padding: 6px 4px 16px;
  max-width: 520px;
  width: 100%;
  margin: 0 auto;
  pointer-events: auto;
}
.imm-cap-section {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 18px 20px;
  margin-bottom: 16px;
  box-shadow: 0 4px 20px -8px rgba(0, 0, 0, 0.06);
}
.imm-cap-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}
.imm-cap-eyebrow {
  font-size: 11px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--accent);
  font-weight: 700;
  margin-bottom: 4px;
}
.imm-cap-heading h2 {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
}
.imm-cap-refresh {
  background: none;
  border: 1px solid var(--border);
  padding: 6px;
  border-radius: var(--radius-sm);
  color: var(--ink-muted);
  cursor: pointer;
}
.imm-cap-empty {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--ink-soft);
}
.imm-server-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}
.imm-server-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
}
.imm-server-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.imm-server-copy strong {
  font-size: 13px;
  color: var(--ink);
}
.imm-server-copy span {
  font-size: 11px;
  color: var(--ink-muted);
}
.imm-server-connected {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #28c840;
  font-weight: 500;
}
.imm-server-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #28c840;
}
.imm-index-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.imm-index-button:hover {
  border-color: var(--accent);
  color: var(--accent);
}
</style>
