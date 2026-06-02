<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import ConfirmModal from '@/components/ConfirmModal.vue'
import GeoNexusGlobe from '@/components/GeoNexusGlobe.vue'

const userStore = useUserStore()
import {
  appendChatPair,
  buildAgentSseUrl,
  createAiSession,
  deleteAiSession,
  getAiQuota,
  getCharacterCards,
  getSessionMessages,
  listAiSessions,
  updateAiSession,
  type AiSessionRow,
  type CharacterCard,
} from '@/api/ai'

const router = useRouter()

function goHome() {
  router.push({ path: '/' })
}

type ChatMsg = { role: 'user' | 'assistant'; content: string; reasoningContent?: string }

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
const chatScrollRef = ref<HTMLElement | null>(null)
const charPickerOpen = ref(false)
const agentMode = ref(true)
const toolStatus = ref<string | null>(null)
const reasoningCollapsed = ref<Record<number, boolean>>({})
const hasReceivedContent = ref(false)

/* ─── 每日对话上限（走后端鉴权） ─── */
const dailyRemaining = ref(10)
const dailyLimit = ref(10)
const isAdminUser = ref(false)
const dailyExceeded = computed(() => !isAdminUser.value && dailyRemaining.value <= 0)

/** 粒子状态：0=平常 1=思考 2=回答 */
const particleState = computed(() => {
  if (!isSending.value) return 0
  if (hasReceivedContent.value) return 2
  return 1
})

async function refreshQuota() {
  try {
    const res: any = await getAiQuota()
    const q = res.data
    dailyLimit.value = q.dailyLimit
    dailyRemaining.value = q.remaining
    isAdminUser.value = q.isAdmin
  } catch {
    // 离线或未登录时默认为 10
  }
}

function toggleReasoning(i: number) {
  reasoningCollapsed.value[i] = !reasoningCollapsed.value[i]
}

const toolLabelMap: Record<string, string> = {
  searchArticles: '正在搜索星迹...',
  getArticleDetail: '正在获取星迹详情...',
  getCategories: '正在获取星域列表...',
  getBlogStats: '正在获取博客统计...',
  getRecentArticles: '正在获取最新星迹...',
  writeArticle: '正在创建星迹...',
  updateArticle: '正在更新星迹...',
  deleteArticle: '正在删除星迹...',
  getAllTags: '正在获取光痕列表...',
  getArticlesByCategory: '正在获取星域星迹...',
  createCategory: '正在创建星域...',
}

let abortController: AbortController | null = null

marked.use({
  breaks: false,
  gfm: true,
})

/* 自定义代码块渲染器，带 highlight.js 高亮 */
const renderer = new marked.Renderer()
renderer.code = ({ text, lang }: { text: string; lang?: string }) => {
  const langAttr = lang ? ` class="language-${lang}"` : ''
  try {
    const highlighted = lang
      ? hljs.highlight(text, { language: lang }).value
      : hljs.highlightAuto(text).value
    return `<pre><code${langAttr}>${highlighted}</code></pre>`
  } catch {
    return `<pre><code${langAttr}>${hljs.highlightAuto(text).value}</code></pre>`
  }
}
marked.use({ renderer })

function formatMessage(content: string): string {
  if (!content) return ''
  try {
    // 清理多余空行：把连续空行合并为1个换行
    let cleaned = content
      .trim()
      .replace(/^\n+/, '')
      .replace(/\n+$/, '')
      .replace(/\n{2,}/g, '\n')
      // 转义单个 ~ 符号，避免被误解为删除线语法（保留 ~~ 用于真正的删除线）
      .replace(/(?<!~)~(?!~)/g, '\\~')
    const html = marked.parse(cleaned) as string
    return html
      .replace(/<table>/g, '<table class="chat-table">')
      .replace(/<img /g, '<img class="chat-img" ')
      .replace(/<p>\s*<\/p>/g, '')
      .replace(/(<br\s*\/?>){2,}/g, '<br>')
      .replace(/<p>(\s*<br\s*\/?>\s*)*<\/p>/g, '')
      .trim()
  } catch {
    return content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>')
  }
}

function toggleAgentMode() {
  if (isSending.value) return
  agentMode.value = !agentMode.value
}

function toggleCharPicker() {
  charPickerOpen.value = !charPickerOpen.value
}

function pickCharacter(key: string) {
  selectedCharacterKey.value = key
  charPickerOpen.value = false
}

/** 流式用 auto 紧跟光标；平时用 smooth */
function scrollChatToBottom(behavior: ScrollBehavior = 'auto') {
  nextTick(() => {
    requestAnimationFrame(() => {
      const el = chatScrollRef.value
      if (!el || activeTab.value !== 'chat') return
      el.scrollTo({ top: el.scrollHeight, behavior })
    })
  })
}

const systemPrompt = computed(() => {
  const c = characterCards.value.find(x => x.key === selectedCharacterKey.value)
  return c?.systemPrompt ?? ''
})

const sessionTitle = computed(() => {
  const s = sessions.value.find(x => x.id === currentSessionId.value)
  return s?.title ?? '未选择会话'
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
    out.push({ role: m.role, content: m.content })
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
  const res = await getSessionMessages(id)
  currentSessionId.value = id
  selectedCharacterKey.value = res.session.characterKey || 'default'
  messages.value = res.messages
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .map(m => ({ role: m.role as 'user' | 'assistant', content: m.content }))
  activeTab.value = 'chat'
}

async function newSession() {
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

async function confirmDeleteSession() {
  const id = deleteSessionId.value
  if (id == null) return
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
  if (!text || isSending.value) return

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

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollChatToBottom('smooth')
  isSending.value = true
  hasReceivedContent.value = false
  messages.value.push({ role: 'assistant', content: '', reasoningContent: '' })
  const assistantIndex = messages.value.length - 1
  document.documentElement.classList.add('echobot-streaming')

  const history = buildApiMessages()

  try {
    cancelStream()
    abortController = new AbortController()
    toolStatus.value = null

    const token = localStorage.getItem('ro_blog_token')
    const authHeaders: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }

    let response: Response
    if (agentMode.value) {
      response = await fetch(buildAgentSseUrl('deepseek-v4-flash'), {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify(history),
        signal: abortController.signal,
      })
    } else {
      response = await fetch(
        `/api/ai/sse?model=deepseek-v4-flash&messages=${encodeURIComponent(JSON.stringify(history))}`,
        {
          headers: authHeaders,
          signal: abortController.signal,
        }
      )
    }
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
        } catch {
          /* ignore parse errors */
        }
      }
    }

    await refreshQuota()

    const userContent = messages.value[messages.value.length - 2]?.content ?? text
    const assistantContent = messages.value[assistantIndex].content
    if (assistantContent && !assistantContent.startsWith('错误：')) {
      await appendChatPair(sid, userContent, assistantContent)
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

watch(
  messages,
  () => {
    scrollChatToBottom(isSending.value ? 'auto' : 'smooth')
  },
  { deep: true }
)

watch(activeTab, t => {
  if (t === 'chat') scrollChatToBottom('smooth')
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

onMounted(async () => {
  document.addEventListener('click', () => {
    charPickerOpen.value = false
  })
  document.documentElement.classList.add('echobot-route')

  if (!userStore.isLoggedIn) return

  try {
    const { cards } = await getCharacterCards()
    characterCards.value = cards || []
  } catch {
    /* ignore */
  }

  await refreshQuota()
  await refreshSessions()
  if (sessions.value.length > 0) {
    await loadSession(sessions.value[0].id)
  } else {
    await newSession()
  }
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('echobot-route')
  document.documentElement.classList.remove('echobot-streaming')
  cancelStream()
})
</script>

<template>
  <div class="echobot-page page-container">
    <!-- Guest overlay -->
    <div v-if="!userStore.isLoggedIn" class="guest-overlay">
      <div class="guest-overlay-content">
        <h2>AI 助手</h2>
        <p>登录后即可使用 AI 对话功能</p>
        <router-link to="/login" class="btn-primary">立即登录</router-link>
      </div>
    </div>
    <div class="echobot-shell">
      <section class="echobot-left echobot-card" aria-label="粒子效果">
        <header class="left-topbar">
          <button type="button" class="back-btn" title="返回首页" @click="goHome">← 返回</button>
          <div class="brand">
            <span class="brand-title">RO ECHOBOT</span>
            <span class="badge" :class="{ on: !isSending }">{{
              isSending ? '生成中' : '就绪'
            }}</span>
          </div>
          <span class="sess-label">会话：{{ sessionTitle }}</span>
          <!-- <div class="left-actions">
            <button type="button" class="ghost-btn" @click="stopGeneration" :disabled="!isSending">
              停止生成
            </button>
          </div> -->
        </header>
        <div class="left-canvas" aria-hidden="true">
          <GeoNexusGlobe :state="particleState" />
        </div>
      </section>

      <aside class="echobot-right echobot-card">
        <nav class="right-tabs">
          <button
            type="button"
            :class="{ active: activeTab === 'chat' }"
            @click="activeTab = 'chat'"
          >
            对话
          </button>
          <button
            type="button"
            :class="{ active: activeTab === 'sessions' }"
            @click="activeTab = 'sessions'"
          >
            会话列表
          </button>
          <button type="button" class="new-chat" @click="newSession">＋ 新会话</button>
        </nav>

        <div v-show="activeTab === 'chat'" class="panel-chat">
          <div class="panel-toolbar">
            <div class="toolbar-well">
              <div class="fld fld-grow">
                <span class="lbl">角色卡</span>
                <div
                  class="ui-select ui-select-wide"
                  :class="{ open: charPickerOpen, disabled: isSending }"
                >
                  <button
                    type="button"
                    class="ui-select-trigger"
                    :disabled="isSending"
                    @click.stop="toggleCharPicker"
                  >
                    <span class="ui-select-value">{{
                      characterCards.find(x => x.key === selectedCharacterKey)?.name || '—'
                    }}</span>
                    <svg
                      class="ui-select-chev"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                    >
                      <path fill="currentColor" d="M7 10l5 5 5-5H7z" />
                    </svg>
                  </button>
                  <div v-show="charPickerOpen" class="ui-select-panel" role="listbox">
                    <button
                      v-for="c in characterCards"
                      :key="c.key"
                      type="button"
                      class="ui-select-opt"
                      :class="{ active: c.key === selectedCharacterKey }"
                      role="option"
                      @click.stop="pickCharacter(c.key)"
                    >
                      <span class="ui-select-opt-main">{{ c.name }}</span>
                      <span class="ui-select-opt-sub">{{ c.description }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="agent-toggle">
              <button
                type="button"
                class="agent-btn"
                :class="{ active: agentMode }"
                :disabled="isSending"
                @click="toggleAgentMode()"
              >
                {{ agentMode ? '🤖 Agent ON' : '💬 普通模式' }}
              </button>
            </div>
          </div>

          <div ref="chatScrollRef" class="chat-scroll">
            <p v-if="!messages.length && !toolStatus" class="chat-empty">
              输入消息开始对话，支持上传 TXT。开启 Agent 模式可查询博客数据。
            </p>
            <div v-if="toolStatus" class="tool-status">
              <span class="tool-spinner"></span>
              <span>{{ toolStatus }}</span>
            </div>
            <div
              v-for="(msg, i) in messages"
              :key="i"
              class="row"
              :class="msg.role === 'user' ? 'is-user' : 'is-ai'"
              v-show="msg.role === 'user' || msg.content || msg.reasoningContent"
            >
              <div class="bubble">
                <div v-if="msg.role === 'assistant' && msg.reasoningContent" class="think-block">
                  <div class="think-header" @click="toggleReasoning(i)">
                    <span class="think-icon">🤔</span>
                    <span class="think-label">思考过程</span>
                    <span class="think-toggle">{{ reasoningCollapsed[i] ? '展开' : '收起' }}</span>
                  </div>
                  <div
                    v-show="!reasoningCollapsed[i]"
                    class="think-body"
                    v-html="formatMessage(msg.reasoningContent)"
                  ></div>
                </div>
                <div
                  v-if="msg.content"
                  class="answer-text"
                  v-html="formatMessage(msg.content)"
                ></div>
              </div>
            </div>
            <!-- 加载动画：AI 正在思考（等待响应且无工具执行时显示） -->
            <div v-if="isSending && !toolStatus && !hasReceivedContent" class="row is-ai">
              <div class="bubble typing-indicator">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>

          <div class="input-block">
            <textarea
              v-model="inputText"
              class="area"
              rows="4"
              placeholder="例如：今天帮我安排一下工作重点。"
              :disabled="isSending"
              @keydown="onKeydown"
            />
            <div class="input-bar">
              <span class="tip"
                >Shift + Enter 换行 ·
                {{ isAdminUser ? '管理员无限制' : `今日剩余 ${dailyRemaining} 次` }}</span
              >
              <div class="input-bar-right">
                <input
                  ref="fileInputRef"
                  type="file"
                  accept=".txt,text/plain"
                  class="hidden-file"
                  @change="onTxtFile"
                />
                <button type="button" class="icon-btn" title="上传 TXT" @click="triggerTxtUpload">
                  📄
                </button>
                <button
                  type="button"
                  class="send"
                  :disabled="isSending || !inputText.trim() || dailyExceeded"
                  @click="sendMessage"
                >
                  {{ isSending ? '生成中…' : dailyExceeded ? '已达上限' : '发送' }}
                </button>
              </div>
            </div>
            <p v-if="connectionError" class="err">{{ connectionError }}</p>
          </div>
        </div>

        <div v-show="activeTab === 'sessions'" class="panel-sessions">
          <ul class="sess-list">
            <li
              v-for="s in sessions"
              :key="s.id"
              class="sess-item"
              :class="{ current: s.id === currentSessionId }"
              @click="loadSession(s.id)"
            >
              <div class="sess-title">{{ s.title }}</div>
              <div class="sess-meta">{{ s.modelId }} · {{ s.characterKey }}</div>
              <button type="button" class="del" @click="removeSession(s.id, $event)">删</button>
            </li>
          </ul>
          <p v-if="!sessions.length" class="empty">暂无会话，点「新会话」开始</p>
        </div>
      </aside>
    </div>
  </div>

  <ConfirmModal
    :show="showDeleteSession"
    title="确认删除"
    message="确定删除该会话？"
    confirm-text="删除"
    @confirm="confirmDeleteSession"
    @cancel="showDeleteSession = false"
  />
</template>

<style scoped>
/* ── Page Container ── */
.echobot-page {
  flex: 1;
  min-height: 0;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  padding: 0.65rem 0.85rem 0.55rem;
  background: transparent;
  overflow: hidden;
  position: relative;
}

/* ── Guest Overlay ── */
.guest-overlay {
  position: absolute;
  inset: 0;
  z-index: 100;
  background: var(--canvas);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
}
.guest-overlay-content {
  text-align: center;
  padding: 40px;
}
.guest-overlay-icon {
  font-size: 3rem;
  margin-bottom: 16px;
}
.guest-overlay-content h2 {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 8px;
}
.guest-overlay-content p {
  color: var(--ink-muted);
  margin: 0 0 24px;
  font-size: 0.95rem;
}

.echobot-page.page-container {
  overflow-x: hidden;
  overflow-y: hidden;
}

.echobot-shell {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 3fr;
  gap: 0.85rem;
  align-items: stretch;
}

/* ── Glass Cards ── */
.echobot-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--border);
  background: var(--surface);

  box-shadow: var(--shadow-card);
  min-height: 0;
}

.echobot-left {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.left-topbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 1rem;
  padding: 0.65rem 1rem;
  background: var(--surface);
  border-bottom: 1px solid var(--border);

  z-index: 5;
}

.back-btn {
  flex-shrink: 0;
  padding: 0.32rem 0.75rem;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-interactive);
  background: var(--surface);
  color: var(--ink);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
}

.back-btn:hover {
  background: var(--surface-hover);
  border-color: var(--border-interactive);
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.brand-title {
  font-weight: 800;
  letter-spacing: 0.12em;
  font-size: 0.82rem;
  color: var(--ink);
}

.badge {
  font-size: 0.72rem;
  padding: 0.12rem 0.45rem;
  border-radius: var(--radius-full);
  background: var(--tag-bg);
  color: var(--ink-muted);
}

.badge.on {
  background: rgba(109, 200, 130, 0.12);
  color: #2e7d32;
}

.sess-label {
  font-size: 0.78rem;
  color: var(--ink-muted);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.left-actions {
  margin-left: auto;
}

.ghost-btn {
  padding: 0.28rem 0.65rem;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-interactive);
  background: var(--surface);
  color: var(--accent);
  font-size: 0.78rem;
  cursor: pointer;
  transition: all var(--transition);
}

.ghost-btn:hover:not(:disabled) {
  background: var(--surface-hover);
  border-color: var(--border-focus);
}

.ghost-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.left-canvas {
  flex: 1;
  min-height: 0;
  background-color: var(--canvas-deep);
}

.echobot-right {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: var(--surface);
}

/* ── Right Tabs ── */
.right-tabs {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 0.6rem;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}

.right-tabs button {
  padding: 0.35rem 0.75rem;
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--ink-muted);
  font-size: 0.85rem;
  cursor: pointer;
  transition: all var(--transition);
}

.right-tabs button.active {
  background: var(--accent);
  color: #ffffff;
  font-weight: 700;
  box-shadow: 0 4px 16px oklch(0.55 0.15 35 / 0.22);
}

.right-tabs button:hover:not(.active) {
  background: var(--tag-hover);
  color: var(--ink);
}

.new-chat {
  margin-left: auto;
}

/* ── Chat Panel ── */
.panel-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel-toolbar {
  flex-shrink: 0;
  padding: 0.55rem 0.65rem 0.65rem;
  border-bottom: 1px solid var(--border);
}

.toolbar-well {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.55rem 0.75rem;
  padding: 0.65rem 0.75rem;
  border-radius: var(--radius-md);
  background: oklch(0.55 0.15 35 / 0.04);
  border: 1px solid var(--border);
}

.fld {
  display: flex;
  flex-direction: column;
  gap: 0.28rem;
  min-width: 0;
}

.fld-grow {
  flex: 1 1 200px;
}

.lbl {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink-muted);
  text-transform: none;
}

/* ── Custom Select ── */
.ui-select {
  position: relative;
  width: 100%;
  max-width: 200px;
}

.ui-select-wide {
  max-width: none;
}

.ui-select.disabled .ui-select-trigger {
  opacity: 0.55;
  cursor: not-allowed;
}

.ui-select-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.42rem 0.65rem 0.42rem 0.85rem;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink);
  font-size: 0.82rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--transition);
}

/* Thinking chain: collapsible gray block */
.think-block {
  margin-bottom: 0.75rem;
  border: 1px solid rgba(200, 200, 220, 0.4);
  border-radius: var(--radius-sm);
  background: rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.think-header {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.55rem;
  cursor: pointer;
  user-select: none;
  font-size: 0.78rem;
  color: var(--ink-muted);
  border-bottom: 1px solid rgba(200, 200, 220, 0.2);
  transition: background 0.15s;
}

.think-header:hover {
  background: rgba(0, 0, 0, 0.03);
}

.think-icon {
  font-size: 0.85rem;
}

.think-label {
  font-weight: 600;
  flex: 1;
}

.think-toggle {
  font-size: 0.72rem;
  color: var(--accent);
  opacity: 0.7;
}

.think-body {
  padding: 0.45rem 0.55rem;
  font-size: 0.82rem;
  line-height: 1.6;
  color: var(--ink-muted);
  border-top: 1px solid transparent;
}

.think-body p {
  margin: 0.25rem 0;
}

/* ensure no extra margin when answer follows */
.think-block + .answer-text {
  margin-top: 0;
}

/* ── Markdown rendered content ── */
.bubble :deep(h1),
.bubble :deep(h2),
.bubble :deep(h3),
.bubble :deep(h4) {
  margin: 0.35rem 0 0.15rem;
  font-weight: 700;
  line-height: 1.3;
}
.bubble :deep(h1) {
  font-size: 1.15rem;
}
.bubble :deep(h2) {
  font-size: 1.05rem;
}
.bubble :deep(h3) {
  font-size: 0.95rem;
}
.bubble :deep(p) {
  margin: 0.1rem 0;
}
.bubble :deep(p:first-child) {
  margin-top: 0;
}
.bubble :deep(p:last-child) {
  margin-bottom: 0;
}
.bubble :deep(p:empty) {
  display: none;
}
.bubble :deep(br:only-child) {
  display: none;
}
.answer-text {
  overflow: hidden;
  line-height: 1.5;
}
.bubble :deep(ul),
.bubble :deep(ol) {
  padding-left: 1.2rem;
  margin: 0.15rem 0;
}
.bubble :deep(li) {
  margin: 0.05rem 0;
}
.bubble :deep(hr) {
  margin: 0.3rem 0;
  border: none;
  border-top: 1px solid var(--border);
}
.bubble :deep(blockquote) {
  margin: 0.4rem 0;
  padding: 0.25rem 0.6rem;
  border-left: 3px solid var(--accent);
  color: var(--ink-muted);
  background: rgba(0, 0, 0, 0.02);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}
.bubble :deep(pre) {
  margin: 0.45rem 0;
  border-radius: var(--radius-sm);
  overflow-x: auto;
  font-size: 0.8rem;
  line-height: 1.45;
}
.bubble :deep(pre code) {
  background: transparent;
  padding: 0;
  font-family: 'Fira Code', 'JetBrains Mono', 'Consolas', monospace;
}
.bubble :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 0.1rem 0.3rem;
  border-radius: 3px;
  font-size: 0.82rem;
  font-family: 'Fira Code', 'Consolas', monospace;
}
.chat-table {
  border-collapse: collapse;
  margin: 0.4rem 0;
  width: 100%;
  font-size: 0.82rem;
  border: 1px solid #d0d0d0;
}
.chat-table th,
.chat-table td {
  border: 1px solid #d0d0d0;
  padding: 0.35rem 0.6rem;
  text-align: left;
}
.chat-table th {
  background: oklch(0.55 0.15 35 / 0.06);
  font-weight: 700;
}
.chat-table tr:nth-child(even) {
  background: rgba(0, 0, 0, 0.015);
}
.chat-img {
  max-width: 100%;
  border-radius: var(--radius-sm);
  margin: 0.3rem 0;
}

.ui-select-trigger:hover:not(:disabled) {
  border-color: var(--border-focus);
}

.ui-select.open .ui-select-trigger {
  border-color: var(--border-focus);
  box-shadow: 0 0 0 3px oklch(0.55 0.15 35 / 0.1);
}

.ui-select-chev {
  flex-shrink: 0;
  color: var(--ink-muted);
  transition: transform 0.2s ease;
}

.ui-select.open .ui-select-chev {
  transform: rotate(180deg);
}

.ui-select-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}

.ui-select-panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 6px);
  z-index: 40;
  padding: 6px;
  border-radius: var(--radius-md);
  background: var(--surface);
  border: 1px solid var(--border);

  box-shadow: var(--shadow-card-hover);
  max-height: 240px;
  overflow-y: auto;
}

.ui-select-opt {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.12rem;
  width: 100%;
  padding: 0.5rem 0.72rem;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--ink);
  font-size: 0.8rem;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.12s ease;
}

.ui-select-opt:hover {
  background: var(--tag-hover);
}

.ui-select-opt.active {
  background: var(--tag-hover);
}

.ui-select-opt-main {
  font-weight: 700;
  line-height: 1.3;
}

.ui-select-opt-sub {
  font-size: 0.72rem;
  font-weight: 500;
  color: var(--ink-muted);
  line-height: 1.35;
}

.warn {
  flex-shrink: 0;
  margin: 0 0.75rem;
  font-size: 0.72rem;
  color: #b4532d;
}

/* ── Chat Messages ── */
.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.chat-empty {
  margin: 0;
  padding: 1rem 0.5rem;
  text-align: center;
  font-size: 0.86rem;
  color: var(--ink-muted);
  line-height: 1.6;
}

.row {
  display: flex;
}

.row.is-user {
  justify-content: flex-end;
}

.row.is-ai {
  justify-content: flex-start;
}

.bubble {
  max-width: 92%;
  padding: 0.5rem 0.7rem;
  border-radius: var(--radius-md);
  font-size: 0.88rem;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

/* User bubble: gradient purple */
.is-user .bubble {
  background: var(--accent);
  color: #ffffff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 16px oklch(0.55 0.15 35 / 0.18);
}

/* AI bubble: glass background */
.is-ai .bubble {
  background: var(--surface);
  color: var(--ink);
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
}

/* ── Thinking Chain (collapsible) ── */
.think-block {
  margin-bottom: 0.65rem;
  border: 1px solid rgba(180, 180, 200, 0.35);
  border-radius: var(--radius-sm);
  background: rgba(0, 0, 0, 0.025);
  overflow: hidden;
}

.think-header {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.5rem;
  cursor: pointer;
  user-select: none;
  font-size: 0.76rem;
  color: var(--ink-muted);
  border-bottom: 1px solid rgba(180, 180, 200, 0.18);
  transition: background 0.15s;
}

.think-header:hover {
  background: rgba(0, 0, 0, 0.035);
}

.think-icon {
  font-size: 0.82rem;
}

.think-label {
  font-weight: 600;
  flex: 1;
}

.think-toggle {
  font-size: 0.7rem;
  color: var(--accent);
  opacity: 0.65;
}

.think-body {
  padding: 0.4rem 0.5rem;
  font-size: 0.8rem;
  line-height: 1.55;
  color: #888;
  font-style: italic;
}

.think-body p {
  margin: 0.2rem 0;
}

/* Thinking chain: collapsible gray block */
.think-block {
  margin-bottom: 0.75rem;
  border: 1px solid rgba(200, 200, 220, 0.4);
  border-radius: var(--radius-sm);
  background: rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.think-header {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.55rem;
  cursor: pointer;
  user-select: none;
  font-size: 0.78rem;
  color: var(--ink-muted);
  border-bottom: 1px solid rgba(200, 200, 220, 0.2);
  transition: background 0.15s;
}

.think-header:hover {
  background: rgba(0, 0, 0, 0.03);
}

.think-icon {
  font-size: 0.85rem;
}

.think-label {
  font-weight: 600;
  flex: 1;
}

.think-toggle {
  font-size: 0.72rem;
  color: var(--accent);
  opacity: 0.7;
}

.think-body {
  padding: 0.45rem 0.55rem;
  font-size: 0.82rem;
  line-height: 1.6;
  color: var(--ink-muted);
  border-top: 1px solid transparent;
}

/* ── Input Area ── */
.input-block {
  flex-shrink: 0;
  padding: 0.6rem 0.75rem 0.75rem;
  border-top: 1px solid var(--border);
  background: var(--surface);
}

.area {
  width: 100%;
  box-sizing: border-box;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  padding: 0.55rem 0.65rem;
  font-size: 0.88rem;
  font-family: inherit;
  resize: none;
  min-height: 76px;
  max-height: 140px;
  background: var(--surface);
  color: var(--ink);

  transition: all var(--transition);
}

.area:focus {
  outline: none;
  border-color: var(--border-focus);
  box-shadow: 0 0 0 3px oklch(0.55 0.15 35 / 0.12);
  background: var(--surface-hover);
}

.input-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.45rem;
  gap: 0.5rem;
}

.tip {
  font-size: 0.72rem;
  color: var(--ink-muted);
}

.input-bar-right {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.hidden-file {
  display: none;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  font-size: 1rem;
  transition: all var(--transition);
}

.icon-btn:hover {
  background: var(--surface-hover);
  border-color: var(--border-interactive);
}

/* Send button: gradient purple pill */
.send {
  padding: 0.45rem 1.5rem;
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  font-weight: 700;
  font-size: 0.88rem;
  color: #ffffff;
  background: var(--accent);
  box-shadow: var(--shadow-button);
  transition: all var(--transition);
}

.send:hover:not(:disabled) {
  box-shadow: var(--shadow-button-hover);
  transform: translateY(-1px);
}

.send:active:not(:disabled) {
  transform: translateY(0);
}

.send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.err {
  margin: 0.35rem 0 0;
  font-size: 0.78rem;
  color: #c44a4a;
}

/* ── Agent Toggle ── */
.agent-toggle {
  display: flex;
  align-items: center;
}

.agent-btn {
  padding: 0.32rem 0.75rem;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-interactive);
  background: var(--surface);
  color: var(--ink-muted);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
}

.agent-btn:hover:not(:disabled) {
  background: var(--surface-hover);
  border-color: var(--border-interactive);
}

.agent-btn.active {
  background: var(--accent);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 4px 16px oklch(0.55 0.15 35 / 0.22);
}

.agent-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ── Tool Status ── */
.tool-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-md);
  background: oklch(0.55 0.15 35 / 0.06);
  border: 1px solid oklch(0.55 0.15 35 / 0.15);
  font-size: 0.82rem;
  color: var(--accent);
  font-weight: 500;
}

.tool-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid oklch(0.55 0.15 35 / 0.2);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── Sessions Panel ── */
.panel-sessions {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0.5rem;
}

.sess-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.sess-item {
  position: relative;
  padding: 0.65rem 2rem 0.65rem 0.65rem;
  margin-bottom: 0.35rem;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  transition: all var(--transition);
}

.sess-item:hover {
  background: var(--surface-hover);
  border-color: var(--border-interactive);
  box-shadow: 0 4px 16px oklch(0.55 0.15 35 / 0.06);
}

.sess-item.current {
  border-color: var(--accent);
  background: oklch(0.55 0.15 35 / 0.06);
  box-shadow: 0 4px 20px oklch(0.55 0.15 35 / 0.1);
}

.sess-title {
  font-weight: 600;
  font-size: 0.88rem;
  color: var(--ink);
}

.sess-meta {
  font-size: 0.72rem;
  color: var(--ink-muted);
  margin-top: 0.2rem;
}

.del {
  position: absolute;
  right: 0.4rem;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: oklch(0.55 0.15 35 / 0.06);
  color: var(--ink-muted);
  border-radius: 8px;
  padding: 0.2rem 0.4rem;
  cursor: pointer;
  font-size: 0.72rem;
  transition: all var(--transition);
}

.del:hover {
  background: rgba(231, 76, 60, 0.1);
  color: #c44a4a;
}

.empty {
  text-align: center;
  color: var(--ink-muted);
  font-size: 0.85rem;
  padding: 2rem;
}

/* ── Responsive ── */
@media (max-width: 900px) {
  .echobot-shell {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(160px, 26vh) minmax(0, 1fr);
  }

  .echobot-page {
    padding: 0.45rem 0.5rem 0.45rem;
  }
}
</style>

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
  background: oklch(0.55 0.15 35 / 0.06);
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
</style>
