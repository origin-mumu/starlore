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
  getAiModels,
  getAgentConfig,
  updateAgentConfig,
  submitMessageFeedback,
  type CharacterCard,
  type RagEvaluationResult,
} from '@/api/ai'
import { guestChat } from '@/api/guest-ai'
import { useUserStore } from '@/stores/user'
import { useCompanionStore } from '@/stores/companion'
import { sanitizeHtml } from '@/utils/sanitize'
import { getAuthToken } from '@/utils/authToken'
import { useRoute, useRouter } from 'vue-router'
import {
  ThumbsUp,
  ThumbsDown,
  BookOpen,
  Sliders,
  Settings,
  Image,
  FileText,
  Send,
  Trash2,
  X,
  Paperclip,
  Activity,
  Sparkles,
} from '@lucide/vue'

const route = useRoute()
const router = useRouter()
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
const companionStore = useCompanionStore()
const voiceStageRef = ref<InstanceType<typeof VoiceWaveStage> | null>(null)
const currentEmotion = ref('02')
let emotionTimer: ReturnType<typeof setTimeout> | null = null

function setEmotion(emoId: string, durationMs?: number) {
  if (emotionTimer) {
    clearTimeout(emotionTimer)
    emotionTimer = null
  }
  currentEmotion.value = emoId
  if (durationMs && durationMs > 0) {
    emotionTimer = setTimeout(() => {
      currentEmotion.value = companionStore.expression || '02'
      emotionTimer = null
    }, durationMs)
  }
}

/* ─── 状态 ─── */
const isLocalSending = ref(false)

/* ─── 用户反馈点赞点踩 ─── */
const feedbackModalOpen = ref(false)
const feedbackTargetMessageId = ref<number | null>(null)

async function handleLike(msg: any) {
  msg.userFeedback = msg.userFeedback === 'LIKE' ? null : 'LIKE'
  const targetId = msg.id || 9999
  if (msg.userFeedback === 'LIKE') {
    // 触发单次欢快反馈并在 2 秒后自动恢复常态，避免无休止循环转圈
    setEmotion('happy', 2000)
    voiceStageRef.value?.getEmotionBall()?.spin(1)
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
  setEmotion('sad', 3000)
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
  setEmotion(companionStore.expression || '02')
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
const activeTab = ref<'chat' | 'sessions'>('chat')
const showDeleteConfirm = ref<number | null>(null)
const configModalOpen = ref(false)

/* ─── Agent 检索调参与形象设置 ─── */
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
  configModalOpen.value = true
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
    setTimeout(() => {
      configModalOpen.value = false
      agentConfigSavedHint.value = ''
    }, 1200)
  } catch (e: any) {
    agentConfigSavedHint.value = '保存失败: ' + (e.message || '未知错误')
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

const chatScrollRef = ref<HTMLElement | null>(null)

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
      setEmotion('05', 3000)
      emit('send', finalContent, res.content)
      emit('refreshQuota')
    } catch (err: any) {
      assistantMsg.content = err?.message || '请求失败，请稍后重试'
      isLocalSending.value = false
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
    setEmotion('05', 3000)
    emit('send', finalContent, fullText, assistantMsg.agentTrace ? JSON.stringify(assistantMsg.agentTrace) : undefined)
    emit('refreshQuota')
    if (assistantMsg.agentTrace && assistantMsg.agentTrace.ragContexts?.length) {
      void runAutomaticRagEvaluation(assistantMsg.agentTrace, finalContent, fullText)
    }
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      console.error('SSE 流式错误:', err)
      assistantMsg.content = assistantMsg.content || '请求异常中断，请重试'
      setEmotion('04')
    }
    toolStatus.value = ''
    isLocalSending.value = false
  }
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
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('immersive-mode-active')
  window.removeEventListener('keydown', handleKeydown)
  abortCtrl?.abort()
})

watch(
  () => props.messages,
  () => scrollChat(),
  { deep: true }
)
watch(toolStatus, () => scrollChat())

onMounted(() => {
  if (route.query.tab === 'config' || route.query.tab === 'settings') {
    openAgentConfig()
  }
})

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
    <!-- 左侧吉祥物舞台 (5:5 均分布局) -->
    <VoiceWaveStage
      ref="voiceStageRef"
      :current-emotion="currentEmotion"
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
          <span>对话</span>
        </button>
        <button
          type="button"
          class="imm-tab"
          :class="{ active: activeTab === 'sessions' }"
          @click="activeTab = 'sessions'"
        >
          <span>会话</span>
        </button>
        <button
          type="button"
          class="imm-tab"
          :class="{ active: configModalOpen }"
          @click="openAgentConfig"
        >
          <Settings :size="13" class="imm-tab-icon" />
          <span>设置</span>
        </button>
        <button
          type="button"
          class="imm-tab imm-tab-harness"
          title="进入 Harness 云端智能体"
          @click="router.push('/harness')"
        >
          <Sparkles :size="13" class="imm-tab-icon" />
          <span>Harness</span>
        </button>
        <button type="button" class="imm-tab imm-tab-action" @click="emit('newSession')">
          <span>＋ 新会话</span>
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
            placeholder="输入消息..."
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

      <!-- 会话历史列表 -->
      <div v-show="activeTab === 'sessions'" class="imm-panel-sessions">
        <ul v-if="sessions.length" class="imm-sess-list">
          <li
            v-for="s in sessions"
            :key="s.id"
            class="imm-sess-item"
            :class="{ active: s.id === currentSessionId }"
            @click="emit('loadSession', s.id); activeTab = 'chat'"
          >
            <span class="imm-sess-title">{{ s.title }}</span>
            <div v-if="showDeleteConfirm === s.id" class="imm-sess-confirm" @click.stop>
              <span>确认删除？</span>
              <button
                type="button"
                class="imm-sess-confirm-yes"
                @click.stop="emit('deleteSession', s.id); showDeleteConfirm = null"
              >
                删除
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
    </div>
  </div>

  <!-- 反馈模态框 -->
  <RagFeedbackModal
    :open="feedbackModalOpen"
    @close="feedbackModalOpen = false"
    @submit="handleFeedbackSubmit"
  />

  <!-- AI 形象与参数设置弹窗 (Modal) -->
  <AgentConfigDrawer
    :open="configModalOpen"
    :agent-config-form="agentConfigForm"
    :available-models="availableModels"
    :agent-config-saved-hint="agentConfigSavedHint"
    @close="configModalOpen = false"
    @save="saveAgentConfig"
  />
</template>

<style scoped src="./ImmersiveMode.css"></style>

