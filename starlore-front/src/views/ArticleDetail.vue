<script lang="ts" setup>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getArticleByIdService, getPublicArticleByIdService } from '@/api/article'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import { List, Hash, Sparkles } from '@lucide/vue'
import { renderArticleContent } from '@/utils/articleContent'
import HarnessChatPanel from '@/views/harness/components/HarnessChatPanel.vue'

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

// ─── AI 伴读侧栏状态 ───
const isAiActive = ref(false)

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

// 注入给复用 AI 卡片的文章上下文
const articleContext = computed(() => {
  if (!article.value) return ''
  return `【当前正在阅读的星记】\n标题：《${article.value.title}》\n分类：${article.value.category || '未分类'}\n摘要：${article.value.description || '无'}\n正文片段：\n${cleanArticleContent.value.slice(0, 3500)}`
})

const articleQuickPrompts = [
  '总结这篇星记的核心要点与逻辑框架',
  '提炼文中的核心技术概念并通俗解释',
  '根据文章内容出 3 道复习思考题',
]

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
          <div class="detail-3col-layout" :class="{ 'ai-active': isAiActive }">
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

            <!-- ─── 右侧：复用 HarnessChatPanel AI 伴读对话卡片 ─── -->
            <aside class="sidebar-ai">
              <HarnessChatPanel
                v-if="article"
                :context="articleContext"
                :context-title="article.title"
                :quick-prompts="articleQuickPrompts"
                :show-close="true"
                @close="isAiActive = false"
              />
            </aside>
          </div>
        </div>
      </section>

      <!-- ─── 右下角悬浮呼出 AI 伴读按钮 ─── -->
      <Transition name="fade-scale">
        <button
          v-if="!isAiActive && article"
          type="button"
          class="ai-trigger-fab"
          title="呼出 AI 伴读助手"
          @click="isAiActive = true"
        >
          <div class="fab-glow-ring"></div>
          <Sparkles :size="16" class="fab-icon" />
          <span class="fab-label">AI 伴读</span>
        </button>
      </Transition>
    </div>
  </div>
</template>

<style src="./ArticleDetail.css"></style>
