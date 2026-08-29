<script lang="ts" setup>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getArticleByIdService, getPublicArticleByIdService } from '@/api/article'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import { List, Hash } from '@lucide/vue'
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
  level: number  // 1=h1, 2=h2, 3=h3
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

const renderedArticleContent = computed(() =>
  renderArticleContent(article.value?.content || '星记内容为空'),
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
      { rootMargin: '-80px 0px -60% 0px' }
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

    <div v-else>
      <section class="article-hero page-header">
        <div class="container">
          <div class="article-header">
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
          </div>
        </div>
      </section>

      <section class="section-parchment">
        <div class="container">
          <div class="detail-layout">
            <main class="main-content">
              <div class="detail-card-enter">
                <div class="typography">
                  <div v-html="renderedArticleContent"></div>
                </div>
                <div class="back-action">
                  <button @click="$router.back()" class="btn-primary">返回星记列表</button>
                </div>
              </div>
            </main>

            <aside class="sidebar-area">
              <nav v-if="tocItems.length > 0" class="toc-card">
                <div class="toc-header" @click="tocOpen = !tocOpen">
                  <List :size="16" />
                  <span>目录</span>
                  <span class="toc-toggle">{{ tocOpen ? '收起' : '展开' }}</span>
                </div>
                <ul v-show="tocOpen" class="toc-list">
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
                    <span class="toc-dot" v-else></span>
                    <span class="toc-text">{{ item.text }}</span>
                  </li>
                </ul>
              </nav>
            </aside>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped src="./ArticleDetail.css"></style>
