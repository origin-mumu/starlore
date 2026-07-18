<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getArticleByIdService, getPublicArticleByIdService } from '@/api/article'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import { List, Hash } from '@lucide/vue'
import { sanitizeHtml } from '@/utils/sanitize'

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
let tocObserver: IntersectionObserver | null = null

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
    document.querySelectorAll('pre code').forEach(block => {
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
onMounted(async () => {
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
    highlightCode()
    buildToc()
  }
})

onBeforeUnmount(() => {
  tocObserver?.disconnect()
})

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="page-container">
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
                  <div v-html="sanitizeHtml(article?.content || '星记内容为空')"></div>
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

<style scoped>
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
  padding: 32px;
}

.loading-spinner {
  text-align: center;
  color: var(--ink-muted);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border);
  border-top: 4px solid var(--accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-spinner p {
  margin: 0;
  font-size: 17px;
}

.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
  padding: 32px;
  max-width: 440px;
  margin: 120px auto 0;
}

.error-content {
  text-align: center;
  max-width: 400px;
}

.error-content h2 {
  color: var(--ink);
  margin-bottom: 16px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.025em;
}

.error-content p {
  color: var(--ink-muted);
  margin-bottom: 32px;
  line-height: 1.7;
}

.back-btn {
  border-radius: var(--radius-full);
}

.article-hero {
  padding-top: 120px;
  padding-bottom: 48px;
  text-align: center;
}

.article-header {
  max-width: 760px;
  margin: 0 auto;
}

.meta-badge {
  display: inline-block;
  color: var(--accent);
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 16px;
  padding: 5px 14px;
  background: var(--badge-bg);
  border: 1px solid var(--badge-border);
  border-radius: var(--radius-full);
}

.article-title {
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.15;
  margin-bottom: 20px;
  color: var(--ink);
}

.article-meta {
  color: var(--ink-muted);
  font-size: 14px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.meta-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--border-interactive);
}

.edit-link {
  color: var(--accent);
  text-decoration: none;
  font-weight: 600;
  transition: opacity var(--transition);
}
.edit-link:hover {
  opacity: 0.8;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
  padding: 0 0 80px;
}

.main-content {
  min-width: 0;
}

.sidebar-area {
  position: sticky;
  top: 100px;
  height: fit-content;
}

/* ── 目录导航 ── */
.toc-card {
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
}

.toc-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  cursor: pointer;
  user-select: none;
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  border-bottom: 1px solid var(--border);
  transition: background 0.2s ease;
  letter-spacing: 0.02em;
}
.toc-header:hover {
  background: var(--surface-hover);
}
.toc-toggle {
  margin-left: auto;
  font-size: 12px;
  color: var(--ink-muted);
  font-weight: 500;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  transition: all 0.2s ease;
}
.toc-header:hover .toc-toggle {
  color: var(--accent);
  background: var(--accent-soft);
}

.toc-list {
  list-style: none;
  margin: 0;
  padding: 8px 8px;
  max-height: 55vh;
  overflow-y: auto;
  scrollbar-width: thin;
}

.toc-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  line-height: 1.5;
  color: var(--ink-muted);
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
  margin-bottom: 2px;
}
.toc-item:last-child {
  margin-bottom: 0;
}
.toc-item:hover {
  color: var(--ink);
  background: var(--surface-hover);
}
.toc-item.toc-active {
  color: var(--accent);
  background: var(--accent-soft);
  font-weight: 600;
}
.toc-item.toc-h2 {
  padding-left: 24px;
  font-size: 12.5px;
}
.toc-item.toc-h3 {
  padding-left: 36px;
  font-size: 12px;
}

.toc-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--border);
  flex-shrink: 0;
  transition: all 0.2s ease;
}
.toc-item:hover .toc-dot {
  background: var(--ink-soft);
}
.toc-active .toc-dot {
  background: var(--accent);
  transform: scale(1.3);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.toc-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-card-enter {
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 40px 48px;
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  opacity: 0;
  transform: translateY(14px);
  animation: detailCardIn 560ms ease forwards;
  animation-delay: 80ms;
}

@keyframes detailCardIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.typography {
  color: var(--ink);
  line-height: 1.7;
  font-size: 16px;
}

.typography :deep(h1) {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 2rem 0 1rem;
  color: var(--ink);
  letter-spacing: -0.025em;
}

.typography :deep(h2) {
  font-size: 1.4rem;
  font-weight: 700;
  margin: 1.8rem 0 0.8rem;
  color: var(--ink);
  letter-spacing: -0.02em;
}

.typography :deep(h3) {
  font-size: 1.15rem;
  font-weight: 600;
  margin: 1.4rem 0 0.6rem;
  color: var(--ink);
}

.typography :deep(p) {
  margin-bottom: 0.75rem;
}

.typography :deep(blockquote) {
  background: var(--tag-bg);
  border-left: 3px solid var(--accent);
  padding: 16px 24px;
  margin: 24px 0;
  color: var(--ink-soft);
  border-radius: var(--radius-sm);
}

.typography :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 20px 0;
  font-size: 0.9rem;
}
.typography :deep(th) {
  background: var(--tag-bg);
  color: var(--ink);
  font-weight: 600;
  padding: 10px 14px;
  border: 1px solid var(--border);
  text-align: center;
}
.typography :deep(td) {
  padding: 10px 14px;
  border: 1px solid var(--border);
  color: var(--ink-soft);
}
.typography :deep(tr:nth-child(even)) {
  background: var(--surface);
}
.typography :deep(tr:hover) {
  background: var(--surface-hover);
}

.typography :deep(hr) {
  border: none;
  border-top: 1px solid var(--border);
  margin: 32px 0;
}

.typography :deep(pre) {
  position: relative;
  background: #292d35;
  margin: 24px 0;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
}

.typography :deep(code),
.typography :deep(.line-numbers-wrapper) {
  font-family: 'Fira Code', Consolas, Monaco, 'Courier New', monospace;
  font-size: 14px;
  line-height: 22px;
  padding-top: 16px;
  padding-bottom: 16px;
}

.typography :deep(code) {
  flex: 1;
  overflow-x: auto;
  color: #abb2bf;
}

.typography :deep(.line-numbers-wrapper) {
  width: 40px;
  text-align: center;
  color: #5c6370;
  background: rgba(0, 0, 0, 0.2);
  border-right: 1px solid #3e4451;
  user-select: none;
  display: flex;
  flex-direction: column;
}

.typography :deep(.line-number) {
  height: 22px;
}

.typography :deep(.copy-code-btn) {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 4px 8px;
  font-size: 12px;
  color: #abb2bf;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;
}

.typography :deep(pre:hover .copy-code-btn) {
  opacity: 1;
}

.typography :deep(.copy-code-btn:hover) {
  background: var(--accent);
  color: white;
}

.typography :deep(.copy-code-btn.copied) {
  background: #2e7d32;
  color: white;
}

.back-action {
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid var(--border);
}

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .sidebar-area {
    position: static;
  }

  .article-title {
    font-size: 1.75rem;
  }

  .detail-card-enter {
    padding: 24px 20px;
  }
}
</style>
