<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAllArticlesService, getCategoriesService } from '@/api/article'
import StarfieldCanvas from './vr/StarfieldCanvas.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const showUI = ref(false)
const sidebarOpen = ref(false)

const categories = ref<{ name: string; count: number; lastUpdated: string }[]>([])
const articlesByCategory = ref<Map<string, { id: number; title: string; desc: string }[]>>(new Map())
const navItems = ref<string[]>(['全部'])
const activeNav = ref('全部')
const selectedNode = ref<any>(null)

// Node colors
const nodeColors = [
  '#ffcc00', '#00ccff', '#ff6699', '#66ff66', '#ff9933',
  '#cc99ff', '#00ffcc', '#ff6666', '#66ccff', '#ffcc66'
]

// Build knowledge nodes based on active nav
const knowledgeNodes = computed(() => {
  const nodes: any[] = []
  const cats = categories.value
  const centerX = 0.5
  const centerY = 0.45

  if (activeNav.value === '全部') {
    // Show category nodes in a circle
    const radius = 0.25
    cats.forEach((cat, i) => {
      const angle = (i / cats.length) * Math.PI * 2 - Math.PI / 2
      const rx = centerX + Math.cos(angle) * radius
      const ry = centerY + Math.sin(angle) * radius

      nodes.push({
        id: i + 1,
        label: cat.name,
        desc: `${cat.count} 篇文章 · 最近更新 ${cat.lastUpdated}`,
        rx,
        ry,
        size: Math.max(5, Math.min(8, cat.count / 2 + 3)),
        color: nodeColors[i % nodeColors.length],
        type: 'category' as const,
        link: `/articles?category=${encodeURIComponent(cat.name)}`,
      })
    })
  } else {
    // Show articles under selected category (max 7)
    const articles = articlesByCategory.value.get(activeNav.value) || []
    const catIndex = cats.findIndex(c => c.name === activeNav.value)
    const catColor = nodeColors[catIndex >= 0 ? catIndex % nodeColors.length : 0]
    const displayArticles = articles.slice(0, 7)

    // Place category node at center
    nodes.push({
      id: 0,
      label: activeNav.value,
      desc: `${articles.length} 篇文章`,
      rx: centerX,
      ry: centerY,
      size: 8,
      color: catColor,
      type: 'category' as const,
    })

    // Place article nodes around it
    const radius = 0.22
    displayArticles.forEach((article, i) => {
      const angle = (i / displayArticles.length) * Math.PI * 2 - Math.PI / 2
      const rx = centerX + Math.cos(angle) * radius
      const ry = centerY + Math.sin(angle) * radius

      nodes.push({
        id: article.id,
        label: article.title,
        desc: article.desc || '点击查看详情',
        rx,
        ry,
        size: 5,
        color: catColor,
        type: 'article' as const,
        link: `/articles/${article.id}`,
      })
    })
  }

  return nodes
})

// Links between nodes
const knowledgeLinks = computed(() => {
  const links: [number, number][] = []
  const nodes = knowledgeNodes.value

  if (activeNav.value === '全部') {
    // Connect adjacent categories
    for (let i = 0; i < nodes.length; i++) {
      const next = (i + 1) % nodes.length
      links.push([nodes[i].id, nodes[next].id])
    }
  } else {
    // Connect center category to each article
    const centerNode = nodes.find(n => n.id === 0)
    if (centerNode) {
      for (const node of nodes) {
        if (node.id !== 0) {
          links.push([0, node.id])
        }
      }
    }
  }

  return links
})

// Stats
const stats = computed(() => ({
  planets: activeNav.value === '全部' ? categories.value.length : 1,
  articles: activeNav.value === '全部'
    ? categories.value.reduce((sum, c) => sum + c.count, 0)
    : (articlesByCategory.value.get(activeNav.value) || []).length,
}))

// Current time
const currentTime = ref('')
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  requestAnimationFrame(updateTime)
}

// Demo data for guests
const demoCategories = [
  { name: '前端开发', count: 5, lastUpdated: '2026-05-20' },
  { name: '后端技术', count: 3, lastUpdated: '2026-05-18' },
  { name: '设计思考', count: 2, lastUpdated: '2026-05-15' },
  { name: 'AI 研究', count: 4, lastUpdated: '2026-05-22' },
  { name: '项目实践', count: 6, lastUpdated: '2026-05-25' },
]

const demoArticlesByCategory = new Map([
  ['前端开发', [
    { id: 101, title: 'Vue 3 组合式 API 实践', desc: '深入理解 Composition API 的设计理念' },
    { id: 102, title: 'CSS Grid 布局指南', desc: '掌握现代 CSS 布局技术' },
    { id: 103, title: 'TypeScript 高级类型', desc: '类型体操的艺术' },
    { id: 104, title: 'Vite 构建优化', desc: '提升前端构建性能' },
    { id: 105, title: '前端性能监控', desc: 'Web Vitals 实践' },
  ]],
  ['后端技术', [
    { id: 201, title: 'Spring Boot 微服务', desc: '构建可扩展的后端服务' },
    { id: 202, title: 'MySQL 索引优化', desc: '数据库性能调优' },
    { id: 203, title: 'Redis 缓存策略', desc: '分布式缓存设计' },
  ]],
  ['设计思考', [
    { id: 301, title: 'UI 设计原则', desc: '打造优秀的用户体验' },
    { id: 302, title: '色彩搭配指南', desc: '设计中的色彩心理学' },
  ]],
  ['AI 研究', [
    { id: 401, title: 'RAG 检索增强生成', desc: '结合检索与生成的 AI 架构' },
    { id: 402, title: 'Prompt Engineering', desc: '提示词工程最佳实践' },
    { id: 403, title: '向量数据库入门', desc: 'Embedding 与相似度搜索' },
    { id: 404, title: 'LLM 微调技术', desc: '大模型定制化训练' },
  ]],
  ['项目实践', [
    { id: 501, title: 'Starlore 项目总结', desc: '个人知识管理系统的构建' },
    { id: 502, title: 'AI Agent 开发笔记', desc: '智能代理系统设计' },
    { id: 503, title: 'Three.js 可视化', desc: '3D 知识图谱展示' },
    { id: 504, title: 'SSE 流式传输', desc: '实时 AI 对话实现' },
    { id: 505, title: 'JWT 认证方案', desc: '安全的身份验证' },
    { id: 506, title: 'Docker 部署实践', desc: '容器化部署流程' },
  ]],
])

onMounted(async () => {
  updateTime()

  try {
    let result: { name: string; count: number; lastUpdated: string }[] = []

    if (!userStore.isLoggedIn) {
      result = demoCategories
      articlesByCategory.value = demoArticlesByCategory
    } else {
      const [catsRes, articlesRes] = await Promise.all([
        getCategoriesService() as any,
        getAllArticlesService({ page: 1, limit: 200 }) as any,
      ])

      const apiCategories: { id: number; name: string; article_count: number }[] = catsRes?.data || []
      const articles: any[] = articlesRes?.data || []

      // Build article stats and group by category
      const articleStats = new Map<string, { count: number; lastUpdated: string }>()
      const grouped = new Map<string, { id: number; title: string; desc: string }[]>()

      for (const a of articles) {
        const cat = a.category || '未分类'
        if (!articleStats.has(cat)) articleStats.set(cat, { count: 0, lastUpdated: '' })
        const s = articleStats.get(cat)!
        s.count++
        const d = a.updatedAt || a.createdAt
        if (d && d > s.lastUpdated) s.lastUpdated = d.slice(0, 10)

        if (!grouped.has(cat)) grouped.set(cat, [])
        grouped.get(cat)!.push({
          id: a.id,
          title: a.title,
          desc: a.description || a.title,
        })
      }

      articlesByCategory.value = grouped

      for (const cat of apiCategories) {
        const s = articleStats.get(cat.name)
        result.push({
          name: cat.name,
          count: s?.count ?? cat.article_count ?? 0,
          lastUpdated: s?.lastUpdated || '-',
        })
      }
      for (const [name, s] of articleStats) {
        if (!result.find(r => r.name === name)) {
          result.push({ name, count: s.count, lastUpdated: s.lastUpdated })
        }
      }
    }

    categories.value = result
    navItems.value = ['全部', ...result.map(r => r.name)]

    setTimeout(() => {
      showUI.value = true
    }, 300)
  } catch (e) {
    console.error('VR scene init failed:', e)
  }
})

function onNavClick(name: string) {
  activeNav.value = name
}

function goBack() {
  router.back()
}

function onNodeClick(node: any) {
  selectedNode.value = node
  if (node.type === 'category' && activeNav.value === '全部') {
    activeNav.value = node.label
  }
}
</script>

<template>
  <div class="vr-page">
    <!-- Canvas -->
    <StarfieldCanvas
      :nodes="knowledgeNodes"
      :links="knowledgeLinks"
      @node-click="onNodeClick"
    />

    <!-- UI Overlay -->
    <Transition name="ui-fade">
      <div v-if="showUI" class="vr-overlay">
        <!-- TOP BAR -->
        <header class="vr-topbar">
          <button class="vr-btn vr-btn--back" @click="goBack">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            <span>返回</span>
          </button>

          <div class="vr-topbar__center">
            <h1 class="vr-topbar__title">
              <span class="vr-topbar__title-icon">&#9670;</span>
              知识星域
            </h1>
            <p class="vr-topbar__subtitle">STARLORE</p>
          </div>

          <div class="vr-topbar__right">
            <span class="vr-topbar__time">{{ currentTime }}</span>
          </div>
        </header>

        <!-- LEFT SIDEBAR -->
        <nav class="vr-sidebar" :class="{ 'vr-sidebar--open': sidebarOpen }">
          <button class="vr-sidebar__toggle" @click="sidebarOpen = !sidebarOpen">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12h18M3 6h18M3 18h18" />
            </svg>
          </button>
          <div class="vr-sidebar__items">
            <button
              v-for="item in navItems"
              :key="item"
              class="vr-sidebar__item"
              :class="{ 'vr-sidebar__item--active': activeNav === item }"
              @click="onNavClick(item)"
            >
              <span class="vr-sidebar__dot" :class="{ 'vr-sidebar__dot--active': activeNav === item }"></span>
              <span>{{ item }}</span>
            </button>
          </div>
        </nav>

        <!-- BOTTOM CONSOLE -->
        <footer class="vr-console">
          <div class="vr-console__stats">
            <div class="vr-console__stat">
              <div>
                <span class="vr-console__stat-value">{{ stats.planets }}</span>
                <span class="vr-console__stat-label">星域</span>
              </div>
            </div>
            <div class="vr-console__divider"></div>
            <div class="vr-console__stat">
              <div>
                <span class="vr-console__stat-value">{{ stats.articles }}</span>
                <span class="vr-console__stat-label">星迹</span>
              </div>
            </div>
          </div>
          <div class="vr-console__hint">
            {{ activeNav === '全部' ? '悬浮查看 · 点击分类进入' : '悬浮查看 · 点击文章查看详情' }}
          </div>
        </footer>
      </div>
    </Transition>

    <!-- LOADING SCREEN -->
    <Transition name="loading-fade">
      <div v-if="!showUI" class="vr-loading">
        <div class="vr-loading__content">
          <div class="vr-loading__ring">
            <div class="vr-loading__ring-inner"></div>
          </div>
          <p class="vr-loading__text">加载中...</p>
          <div class="vr-loading__bar">
            <div class="vr-loading__bar-fill"></div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.vr-page {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #0c0d13;
  z-index: 1;
}

.vr-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 10;
}

/* TOP BAR */
.vr-topbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  pointer-events: auto;
  background: linear-gradient(180deg, rgba(5, 8, 22, 0.85) 0%, transparent 100%);
}

.vr-topbar__center { text-align: center; }

.vr-topbar__title {
  font-size: 16px;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 3px;
  margin: 0;
  text-shadow: 0 0 20px rgba(100, 200, 255, 0.4);
}

.vr-topbar__title-icon {
  color: var(--accent);
  margin-right: 6px;
  font-size: 12px;
}

.vr-topbar__subtitle {
  font-size: 11px;
  color: #F0D9C4;
  letter-spacing: 4px;
  margin: 2px 0 0;
  text-shadow: 0 0 10px rgba(183, 178, 255, 0.6);
}

.vr-topbar__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.vr-topbar__time {
  font-size: 13px;
  font-family: 'Courier New', monospace;
  color: #ffffff;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(183, 178, 255, 0.5);
}

/* BUTTONS */
.vr-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid rgba(100, 200, 255, 0.2);
  border-radius: 10px;
  background: rgba(18, 14, 30, 0.6);
  color: #d0c8e0;
  font-size: 13px;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: all 0.25s ease;
  pointer-events: auto;
}

.vr-btn:hover {
  background: rgba(100, 200, 255, 0.15);
  border-color: rgba(100, 200, 255, 0.4);
  color: #fff;
  transform: translateY(-1px);
}

/* LEFT SIDEBAR */
.vr-sidebar {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: auto;
  z-index: 20;
}

.vr-sidebar__toggle {
  display: none;
  width: 36px;
  height: 36px;
  border: 1px solid rgba(100, 200, 255, 0.2);
  border-radius: 10px;
  background: rgba(18, 14, 30, 0.7);
  color: #d0c8e0;
  cursor: pointer;
  backdrop-filter: blur(10px);
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.vr-sidebar__items {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 8px;
  background: rgba(18, 14, 30, 0.9);
  border: 1px solid rgba(100, 200, 255, 0.4);
  border-radius: 14px;
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5), inset 0 0 30px rgba(100, 200, 255, 0.05);
}

.vr-sidebar__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  text-shadow: 0 0 12px rgba(183, 178, 255, 0.5);
}

.vr-sidebar__item:hover {
  background: rgba(100, 200, 255, 0.1);
  color: #e2e8f0;
}

.vr-sidebar__item--active {
  background: rgba(100, 200, 255, 0.3);
  color: #ffffff;
  text-shadow: 0 0 20px rgba(183, 178, 255, 0.8);
}

.vr-sidebar__dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(100, 200, 255, 0.3);
  transition: all 0.2s ease;
}

.vr-sidebar__dot--active {
  background: var(--accent);
  box-shadow: 0 0 8px rgba(100, 200, 255, 0.5);
}

/* BOTTOM CONSOLE */
.vr-console {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 14px 24px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  pointer-events: none;
  background: linear-gradient(0deg, rgba(5, 8, 22, 0.85) 0%, transparent 100%);
}

.vr-console__stats {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 18px;
  background: rgba(18, 14, 30, 0.85);
  border: 1px solid rgba(100, 200, 255, 0.3);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4), inset 0 0 20px rgba(100, 200, 255, 0.05);
}

.vr-console__stat {
  display: flex;
  align-items: center;
  gap: 8px;
}

.vr-console__stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--accent);
  font-family: 'Courier New', monospace;
}

.vr-console__stat-label {
  font-size: 10px;
  color: rgba(200, 190, 220, 0.4);
  margin-left: 4px;
  letter-spacing: 0.5px;
}

.vr-console__divider {
  width: 1px;
  height: 20px;
  background: rgba(100, 200, 255, 0.15);
}

.vr-console__hint {
  font-size: 12px;
  color: #ffffff;
  letter-spacing: 0.5px;
  text-shadow: 0 0 10px rgba(183, 178, 255, 0.6);
}

/* LOADING SCREEN */
.vr-loading {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0c0d13;
  z-index: 100;
}

.vr-loading__content { text-align: center; }

.vr-loading__ring {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  position: relative;
  border: 2px solid rgba(100, 200, 255, 0.1);
  border-radius: 50%;
  animation: ring-rotate 3s linear infinite;
}

.vr-loading__ring::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border: 2px solid transparent;
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: ring-rotate 1.5s linear infinite;
}

.vr-loading__ring-inner {
  position: absolute;
  top: 6px;
  left: 6px;
  right: 6px;
  bottom: 6px;
  border: 1px solid transparent;
  border-bottom-color: #F0D9C4;
  border-radius: 50%;
  animation: ring-rotate 2s linear infinite reverse;
}

@keyframes ring-rotate {
  to { transform: rotate(360deg); }
}

.vr-loading__text {
  font-size: 14px;
  color: rgba(200, 190, 220, 0.6);
  letter-spacing: 2px;
  margin: 0 0 16px;
}

.vr-loading__bar {
  width: 160px;
  height: 2px;
  background: rgba(100, 200, 255, 0.1);
  border-radius: 1px;
  margin: 0 auto;
  overflow: hidden;
}

.vr-loading__bar-fill {
  width: 40%;
  height: 100%;
  background: linear-gradient(90deg, var(--accent), #F0D9C4);
  border-radius: 1px;
  animation: bar-slide 1.5s ease-in-out infinite;
}

@keyframes bar-slide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(400%); }
}

/* TRANSITIONS */
.ui-fade-enter-active { transition: opacity 0.8s ease 0.3s; }
.ui-fade-leave-active { transition: opacity 0.3s ease; }
.ui-fade-enter-from, .ui-fade-leave-to { opacity: 0; }

.loading-fade-leave-active { transition: opacity 0.6s ease; }
.loading-fade-leave-to { opacity: 0; }

/* RESPONSIVE */
@media (max-width: 768px) {
  .vr-topbar { padding: 12px 14px; }
  .vr-topbar__title { font-size: 14px; letter-spacing: 2px; }
  .vr-topbar__subtitle { display: none; }
  .vr-topbar__right { gap: 10px; }
  .vr-topbar__time { font-size: 11px; }
  .vr-sidebar { left: 8px; }
  .vr-sidebar__toggle { display: flex; }
  .vr-sidebar__items { display: none; }
  .vr-sidebar--open .vr-sidebar__items {
    display: flex;
    position: absolute;
    top: 44px;
    left: 0;
  }
  .vr-console {
    padding: 10px 14px 14px;
    flex-direction: column;
    gap: 8px;
  }
  .vr-console__stats { gap: 12px; padding: 6px 14px; }
  .vr-console__stat-value { font-size: 15px; }
  .vr-console__hint { font-size: 10.5px; }
  .vr-btn--back { padding: 6px 12px; font-size: 12px; }
  .vr-btn--back span { display: none; }
}
</style>
