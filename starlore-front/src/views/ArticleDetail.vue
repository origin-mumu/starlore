<script lang="ts" setup>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getArticleByIdService, getPublicArticleByIdService } from '@/api/article'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import { List, Hash, Sparkles, Send, Trash2, Bot, User } from '@lucide/vue'
import { getAuthToken } from '@/utils/authToken'
import { renderArticleContent } from '@/utils/articleContent'

const userStore = useUserStore()

interface Article {
  id: number
  title: string
  description: string
  content: string
  category: string
  tags: string[]
  cover_image: string
  view_count: number
  status: 'published' | 'draft'
  createdAt: string
  updatedAt: string
}

/** 目录项 */
interface TocItem {
  id: string
  text: string
  level: number // 1=h1, 2=h2, 3=h3
}

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
}

const route = useRoute()
const article = ref<Article>()
const isLoading = ref(true)
const error = ref<string | null>(null)
const tocItems = ref<TocItem[]>([])
const activeTocId = ref<string | null>(null)
const tocOpen = ref(true)
const readingProgress = ref(0)
let tocObserver: IntersectionObserver | null = null
let progressFrame = 0
let enhancementTimer = 0
let highlightIdleCallback = 0

// ─── AI 伴读对话状态 ───
const aiMessages = ref<ChatMessage[]>([
  {
    id: 'welcome',
    role: 'assistant',
    content: '你好！我是这篇星记的 AI 伴读助手。你可以随时向我提问关于文章内容的任何问题，或者点击下方快捷键让我为你总结要点。',
    timestamp: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
  },
])
const inputMessage = ref('')
const isAiStreaming = ref(false)
const aiChatScrollRef = ref<HTMLElement | null>(null)
let aiAbortController: AbortController | null = null

const quickPrompts = [
  '总结这篇星记的核心要点',
  '解释文中的关键概念',
  '针对文章出 3 道复习思考题',
]

const cleanArticleContent = computed(() => {
  let raw = article.value?.content || '星记内容为空'
  if (article.value?.title) {
    const escaped = article.value.title.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const titleRegex = new RegExp(`^#\\s*${escaped}\\s*\\n+`, 'i')
    raw = raw.replace(titleRegex, '')
  }
  return raw
})

const renderedArticleContent = computed(() =>
  renderArticleContent(cleanArticleContent.value),
)

const updateReadingProgress = () => {
  progressFrame = 0
  const content = document.querySelector<HTMLElement>('.typography')
  if (!content) {
    readingProgress.value = 0
    return
  }
  const contentTop = content.getBoundingClientRect().top + window.scrollY
  const readableDistance = Math.max(1, content.offsetHeight - window.innerHeight * 0.68)
  readingProgress.value = Math.min(
    1,
    Math.max(0, (window.scrollY - contentTop + 96) / readableDistance),
  )
}

const scheduleReadingProgress = () => {
  if (!progressFrame) progressFrame = requestAnimationFrame(updateReadingProgress)
}

/** 从渲染后的 HTML 中提取标题并注入 ID，生成目录 */
function buildToc() {
  nextTick(() => {
    const container = document.querySelector('.typography')
    if (!container) return
    const headings = container.querySelectorAll('h1, h2, h3')
    const items: TocItem[] = []

    headings.forEach((h, i) => {
      const id = 'heading-' + i + '-' + (h.textContent || '').trim().replace(/\s+/g, '-').slice(0, 30)
      h.id = id
      items.push({
        id,
        text: (h.textContent || '').trim(),
        level: parseInt(h.tagName[1]), // 1, 2, 3
      })
    })

    tocItems.value = items

    // IntersectionObserver 跟踪当前阅读位置
    tocObserver?.disconnect()
    tocObserver = new IntersectionObserver(
      entries => {
        for (const e of entries) {
          if (e.isIntersecting) {
            activeTocId.value = e.target.id
          }
        }
      },
      { rootMargin: '-80px 0px -60% 0px' },
    )
    headings.forEach(h => tocObserver!.observe(h))
  })
}

/** 点击目录项，平滑滚动到对应标题 */
function scrollToHeading(id: string) {
  activeTocId.value = id
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const highlightCode = () => {
  nextTick(() => {
    document.querySelectorAll('.typography pre code').forEach(block => {
      hljs.highlightElement(block as HTMLElement)

      const pre = block.parentElement
      if (!pre || pre.querySelector('.line-numbers-wrapper')) return

      const codeText = (block as HTMLElement).innerText.replace(/\n$/, '')
      const lines = codeText.split('\n').length

      const lineNumbersWrapper = document.createElement('div')
      lineNumbersWrapper.className = 'line-numbers-wrapper'
      let numbering = ''
      for (let i = 1; i <= lines; i++) {
        numbering += `<span class="line-number">${i}</span>`
      }
      lineNumbersWrapper.innerHTML = numbering
      pre.insertBefore(lineNumbersWrapper, block)

      const copyBtn = document.createElement('button')
      copyBtn.className = 'copy-code-btn'
      copyBtn.textContent = '复制'
      copyBtn.onclick = () => {
        navigator.clipboard.writeText(codeText).then(() => {
          copyBtn.textContent = '已复制'
          copyBtn.classList.add('copied')
          setTimeout(() => {
            copyBtn.textContent = '复制'
            copyBtn.classList.remove('copied')
          }, 2000)
        })
      }
      pre.appendChild(copyBtn)
    })
  })
}

const scheduleArticleEnhancements = () => {
  nextTick(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    enhancementTimer = window.setTimeout(() => {
      buildToc()
      nextTick(scheduleReadingProgress)

      const runHighlight = () => highlightCode()
      if ('requestIdleCallback' in window) {
        highlightIdleCallback = window.requestIdleCallback(runHighlight, { timeout: 800 })
      } else {
        runHighlight()
      }
    }, reducedMotion ? 0 : 260)
  })
}

// ─── AI 伴读交互逻辑 ───
function handleQuickPrompt(promptText: string) {
  inputMessage.value = promptText
  sendAiMessage()
}

function scrollAiToBottom() {
  nextTick(() => {
    if (aiChatScrollRef.value) {
      aiChatScrollRef.value.scrollTop = aiChatScrollRef.value.scrollHeight
    }
  })
}

async function sendAiMessage() {
  const query = inputMessage.value.trim()
  if (!query || isAiStreaming.value) return

  const userMsgId = 'user-' + Date.now()
  aiMessages.value.push({
    id: userMsgId,
    role: 'user',
    content: query,
    timestamp: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
  })

  inputMessage.value = ''
  scrollAiToBottom()

  const assistantMsgId = 'ai-' + Date.now()
  aiMessages.value.push({
    id: assistantMsgId,
    role: 'assistant',
    content: '',
    timestamp: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
  })

  isAiStreaming.value = true

  const articleContext = article.value
    ? `当前阅读文章标题：《${article.value.title}》\n分类：${article.value.category || '未分类'}\n文章摘要及核心内容：\n${article.value.description || ''}\n${article.value.content.slice(0, 2000)}`
    : ''

  const apiMessages = [
    {
      role: 'system',
      content: `你是一个智能伴读助手，用户正在阅读文章。请依据以下文章内容并结合通用知识，回答用户关于该文章的问题。保持专业、亲和、清晰条理，回答尽量精炼并使用 Markdown 格式。\n\n【文章上下文】\n${articleContext}`,
    },
    ...aiMessages.value
      .filter(m => m.id !== 'welcome' && m.id !== assistantMsgId)
      .map(m => ({ role: m.role, content: m.content })),
  ]

  try {
    aiAbortController = new AbortController()
    const token = getAuthToken()
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const response = await fetch('/api/ai/sse', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        messages: apiMessages,
      }),
      signal: aiAbortController.signal,
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    const assistantIdx = aiMessages.value.findIndex(m => m.id === assistantMsgId)

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
          if (data.content && assistantIdx !== -1) {
            aiMessages.value[assistantIdx].content += data.content
            scrollAiToBottom()
          }
        } catch {
          // ignore parse errors
        }
      }
    }
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      const assistantIdx = aiMessages.value.findIndex(m => m.id === assistantMsgId)
      if (assistantIdx !== -1 && !aiMessages.value[assistantIdx].content) {
        aiMessages.value[assistantIdx].content = '抱歉，回答生成遇到了一点问题，请稍后重试。'
      }
    }
  } finally {
    isAiStreaming.value = false
    aiAbortController = null
    scrollAiToBottom()
  }
}

function clearAiMessages() {
  if (aiAbortController) {
    aiAbortController.abort()
    aiAbortController = null
  }
  isAiStreaming.value = false
  aiMessages.value = [
    {
      id: 'welcome-' + Date.now(),
      role: 'assistant',
      content: `对话已重置。你可以随时向我提问关于《${article.value?.title || '本篇星记'}》的任何内容！`,
      timestamp: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
    },
  ]
}

onMounted(async () => {
  window.addEventListener('scroll', scheduleReadingProgress, { passive: true })
  window.addEventListener('resize', scheduleReadingProgress, { passive: true })
  const id = Number(route.params.id)

  if (isNaN(id)) {
    error.value = '星记ID格式错误'
    isLoading.value = false
    return
  }

  try {
    isLoading.value = true
    error.value = null

    const res = userStore.isLoggedIn
      ? ((await getArticleByIdService(id)) as any)
      : ((await getPublicArticleByIdService(id)) as any)

    if (res.data) {
      article.value = res.data
      document.title = `${res.data.title} - Starlore`
    } else {
      error.value = '星记不存在'
    }
  } catch (err) {
    console.error('获取文章失败:', err)
    error.value = '获取星记失败，请稍后重试'
  } finally {
    isLoading.value = false
    scheduleArticleEnhancements()
  }
})

onBeforeUnmount(() => {
  if (aiAbortController) {
    aiAbortController.abort()
  }
  tocObserver?.disconnect()
  window.removeEventListener('scroll', scheduleReadingProgress)
  window.removeEventListener('resize', scheduleReadingProgress)
  if (progressFrame) cancelAnimationFrame(progressFrame)
  if (enhancementTimer) clearTimeout(enhancementTimer)
  if (highlightIdleCallback && 'cancelIdleCallback' in window) {
    window.cancelIdleCallback(highlightIdleCallback)
  }
})

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="page-container">
    <div
      v-if="article && !isLoading"
      class="reading-progress"
      :style="{ transform: `scaleX(${readingProgress})` }"
      role="progressbar"
      aria-label="文章阅读进度"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-valuenow="Math.round(readingProgress * 100)"
    ></div>

    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner">
        <div class="spinner"></div>
        <p>星记加载中...</p>
      </div>
    </div>

    <div v-else-if="error" class="error-container ink-glass-card">
      <div class="error-content">
        <h2>加载失败</h2>
        <p>{{ error }}</p>
        <button @click="$router.back()" class="btn-primary back-btn">返回上一页</button>
      </div>
    </div>

    <div v-else class="article-detail-page">
      <section class="section-parchment">
        <div class="detail-container">
          <div class="detail-3col-layout">
            <!-- ─── 左侧：文章目录 (TOC) ─── -->
            <aside class="sidebar-toc" :class="{ 'is-collapsed': !tocOpen }">
              <nav class="toc-card ink-glass-card" :class="{ 'is-collapsed': !tocOpen }">
                <div class="toc-header" @click="tocOpen = !tocOpen">
                  <div class="toc-header-title">
                    <List :size="16" class="toc-icon" />
                    <span>文章目录</span>
                  </div>
                  <div class="toc-header-right">
                    <span class="toc-toggle">{{ tocOpen ? '收起' : '展开' }}</span>
                  </div>
                </div>
                <div v-show="tocOpen" class="toc-body">
                  <ul v-if="tocItems.length > 0" class="toc-list">
                    <li
                      v-for="item in tocItems"
                      :key="item.id"
                      class="toc-item"
                      :class="{
                        'toc-active': activeTocId === item.id,
                        'toc-h2': item.level === 2,
                        'toc-h3': item.level === 3,
                      }"
                      @click="scrollToHeading(item.id)"
                    >
                      <Hash v-if="item.level === 1" :size="12" class="toc-dot" />
                      <span v-else class="toc-dot"></span>
                      <span class="toc-text">{{ item.text }}</span>
                    </li>
                  </ul>
                  <div v-else class="toc-empty">
                    <span>暂无章节标题</span>
                  </div>
                </div>
              </nav>
            </aside>

            <!-- ─── 中间：文章正文区 ─── -->
            <main class="main-content">
              <article class="article-body-card ink-glass-card">
                <header class="article-header">
                  <span class="meta-badge">{{ article?.category || '未分类' }}</span>
                  <h1 class="article-title">{{ article?.title || '无标题' }}</h1>
                  <div class="article-meta">
                    <span>{{ formatDate(article?.createdAt || '未知日期') }}</span>
                    <span class="meta-dot"></span>
                    <span>{{ article?.view_count || 0 }} 阅读</span>
                    <template v-if="userStore.isLoggedIn">
                      <span class="meta-dot"></span>
                      <router-link :to="`/articles/edit/${article?.id}`" class="edit-link">编辑星记</router-link>
                    </template>
                  </div>
                </header>

                <div class="typography">
                  <div v-html="renderedArticleContent"></div>
                </div>

                <footer class="article-footer-actions">
                  <button @click="$router.back()" class="btn-primary">返回星记列表</button>
                </footer>
              </article>
            </main>

            <!-- ─── 右侧：AI 伴读问答窗口 ─── -->
            <aside class="sidebar-ai">
              <div class="ai-companion-card ink-glass-card">
                <div class="ai-card-header">
                  <div class="ai-header-left">
                    <div class="ai-avatar-icon">
                      <Sparkles :size="15" />
                    </div>
                    <div class="ai-header-meta">
                      <div class="ai-header-title">AI 伴读助手</div>
                      <div class="ai-header-status">
                        <span class="status-indicator"></span>
                        <span>随时为你解答</span>
                      </div>
                    </div>
                  </div>
                  <button
                    class="ai-clear-btn"
                    type="button"
                    title="清空对话"
                    @click="clearAiMessages"
                  >
                    <Trash2 :size="14" />
                  </button>
                </div>

                <!-- 消息滚动列表 -->
                <div ref="aiChatScrollRef" class="ai-messages-scroll">
                  <div
                    v-for="msg in aiMessages"
                    :key="msg.id"
                    class="ai-msg-row"
                    :class="`is-${msg.role}`"
                  >
                    <div class="ai-msg-avatar">
                      <Bot v-if="msg.role === 'assistant'" :size="14" />
                      <User v-else :size="14" />
                    </div>
                    <div class="ai-msg-bubble">
                      <div
                        v-if="msg.role === 'assistant'"
                        class="ai-bubble-content markdown-body"
                      >
                        <div v-if="!msg.content && isAiStreaming" class="ai-thinking-state">
                          <div class="ai-thinking-dots">
                            <span class="ai-thinking-dot"></span>
                            <span class="ai-thinking-dot"></span>
                            <span class="ai-thinking-dot"></span>
                          </div>
                        </div>
                        <div v-else v-html="renderArticleContent(msg.content)"></div>
                      </div>
                      <div v-else class="ai-bubble-content">{{ msg.content }}</div>
                      <span class="ai-msg-time">{{ msg.timestamp }}</span>
                    </div>
                  </div>
                </div>

                <!-- 快捷提问预设词 -->
                <div class="ai-quick-prompts">
                  <button
                    v-for="prompt in quickPrompts"
                    :key="prompt"
                    type="button"
                    class="quick-prompt-pill"
                    :disabled="isAiStreaming"
                    @click="handleQuickPrompt(prompt)"
                  >
                    {{ prompt }}
                  </button>
                </div>

                <!-- 输入区 -->
                <div class="ai-input-box">
                  <textarea
                    v-model="inputMessage"
                    placeholder="向 AI 提问文章内容... (Enter 发送)"
                    rows="1"
                    :disabled="isAiStreaming"
                    @keydown.enter.exact.prevent="sendAiMessage"
                  ></textarea>
                  <button
                    type="button"
                    class="ai-send-btn"
                    :disabled="!inputMessage.trim() || isAiStreaming"
                    @click="sendAiMessage"
                    aria-label="发送消息"
                  >
                    <Send :size="14" />
                  </button>
                </div>
              </div>
            </aside>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style src="./ArticleDetail.css"></style>
