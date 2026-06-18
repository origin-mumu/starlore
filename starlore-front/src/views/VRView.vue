<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAllArticlesService, getCategoriesService, getPublicArticlesService, getPublicCategoriesService } from '@/api/article'
import StarfieldCanvas from './vr/StarfieldCanvas.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const showUI = ref(false)
const sidebarOpen = ref(false)

const allArticles = ref<any[]>([])
const categories = ref<{ name: string; count: number; lastUpdated: string }[]>([])
const articlesByCategory = ref<Map<string, { id: number; title: string; desc: string }[]>>(new Map())
const navItems = ref<string[]>(['全部'])
const activeNav = ref('全部')
const selectedNode = ref<any>(null)

// Search & Filter state
const searchQuery = ref('')
const showCategories = ref(true)
const showArticles = ref(true)
const showLinks = ref(true)

// Side panel details drawer state
const selectedArticle = ref<any>(null)
const drawerOpen = ref(false)

// Node colors
const nodeColors = [
  '#ffcc00', '#00ccff', '#ff6699', '#66ff66', '#ff9933',
  '#cc99ff', '#00ffcc', '#ff6666', '#66ccff', '#ffcc66'
]

// Seeded random helper
function seededRandom(seed: number) {
  let x = Math.sin(seed * 127.1) * 43758.5453
  return x - Math.floor(x)
}

// Build knowledge nodes list
const knowledgeNodes = computed(() => {
  const nodes: any[] = []
  const cats = categories.value
  const articles = allArticles.value
  const cx = 0.5
  const cy = 0.5

  if (activeNav.value === '全部') {
    // 1. Center hub node
    if (showCategories.value) {
      nodes.push({
        id: 0,
        label: '知识星系',
        desc: `${articles.length} 篇文章 · ${cats.length} 个分类`,
        rx: cx,
        ry: cy,
        size: 15,
        color: '#ffffff',
        type: 'category' as const,
      })
    }

    // 2. Category nodes
    cats.forEach((cat, i) => {
      if (!showCategories.value) return
      const angle = (i / cats.length) * Math.PI * 2
      const rx = cx + Math.cos(angle) * 0.15
      const ry = cy + Math.sin(angle) * 0.15
      nodes.push({
        id: 100000 + i,
        label: cat.name,
        desc: `${cat.count} 篇文章 · 更新于 ${cat.lastUpdated}`,
        rx,
        ry,
        size: 10,
        color: nodeColors[i % nodeColors.length],
        type: 'category' as const,
      })
    })

    // 3. Article nodes
    if (showArticles.value) {
      articles.forEach((art, i) => {
        const catIndex = cats.findIndex(c => c.name === art.category)
        const catColor = catIndex >= 0 ? nodeColors[catIndex % nodeColors.length] : '#ffffff'
        const angle = (i / articles.length) * Math.PI * 2 + seededRandom(i * 17) * 0.2
        const rx = cx + Math.cos(angle) * 0.35
        const ry = cy + Math.sin(angle) * 0.35
        nodes.push({
          id: art.id,
          label: art.title,
          desc: art.description || art.title,
          rx,
          ry,
          size: 5,
          color: catColor,
          type: 'article' as const,
          link: `/articles/${art.id}`,
          tags: art.tags || [],
          createdAt: art.createdAt,
          updatedAt: art.updatedAt,
        })
      })
    }
  } else {
    // Specific category is active
    const catIndex = cats.findIndex(c => c.name === activeNav.value)
    const catColor = catIndex >= 0 ? nodeColors[catIndex % nodeColors.length] : '#ffffff'
    const filteredArticles = articles.filter(art => art.category === activeNav.value)

    // 1. Center Category Node
    nodes.push({
      id: 0,
      label: activeNav.value,
      desc: `${filteredArticles.length} 篇文章`,
      rx: cx,
      ry: cy,
      size: 12,
      color: catColor,
      type: 'category' as const,
    })

    // 2. Article Nodes
    if (showArticles.value) {
      filteredArticles.forEach((art, i) => {
        const angle = (i / filteredArticles.length) * Math.PI * 2
        const rx = cx + Math.cos(angle) * 0.28
        const ry = cy + Math.sin(angle) * 0.28
        nodes.push({
          id: art.id,
          label: art.title,
          desc: art.description || art.title,
          rx,
          ry,
          size: 5,
          color: catColor,
          type: 'article' as const,
          link: `/articles/${art.id}`,
          tags: art.tags || [],
          createdAt: art.createdAt,
          updatedAt: art.updatedAt,
        })
      })
    }
  }

  return nodes
})

// Build connections between nodes
const knowledgeLinks = computed(() => {
  const links: [number, number][] = []
  if (!showLinks.value) return links

  const cats = categories.value
  const articles = allArticles.value

  if (activeNav.value === '全部') {
    // 1. Center node connected to Category nodes
    if (showCategories.value) {
      cats.forEach((_, i) => {
        links.push([0, 100000 + i])
      })
    }

    // 2. Articles connected to their Categories
    if (showArticles.value && showCategories.value) {
      articles.forEach((art) => {
        const catIndex = cats.findIndex(c => c.name === art.category)
        if (catIndex >= 0) {
          links.push([100000 + catIndex, art.id])
        }
      })
    }

    // 3. Articles connected to each other if they share tags
    if (showArticles.value) {
      const len = articles.length
      for (let i = 0; i < len; i++) {
        const art1 = articles[i]
        const tags1 = art1.tags || []
        if (tags1.length === 0) continue

        for (let j = i + 1; j < len; j++) {
          const art2 = articles[j]
          const tags2 = art2.tags || []
          const shared = tags1.filter((t: string) => tags2.includes(t))
          if (shared.length >= 1) {
            links.push([art1.id, art2.id])
          }
        }
      }
    }
  } else {
    // Specific category active
    const filteredArticles = articles.filter(art => art.category === activeNav.value)
    
    // 1. Center Category connected to Articles
    if (showArticles.value) {
      filteredArticles.forEach((art) => {
        links.push([0, art.id])
      })
    }

    // 2. Articles connected to each other if they share tags
    if (showArticles.value) {
      const len = filteredArticles.length
      for (let i = 0; i < len; i++) {
        const art1 = filteredArticles[i]
        const tags1 = art1.tags || []
        if (tags1.length === 0) continue

        for (let j = i + 1; j < len; j++) {
          const art2 = filteredArticles[j]
          const tags2 = art2.tags || []
          const shared = tags1.filter((t: string) => tags2.includes(t))
          if (shared.length >= 1) {
            links.push([art1.id, art2.id])
          }
        }
      }
    }
  }

  return links
})

// Highlight nodes computed based on search query
const highlightedNodes = computed(() => {
  const highlights: number[] = []
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return highlights

  knowledgeNodes.value.forEach(node => {
    const labelMatch = node.label.toLowerCase().includes(q)
    const descMatch = node.desc.toLowerCase().includes(q)
    const tagsMatch = node.tags && node.tags.some((t: string) => t.toLowerCase().includes(q))
    if (labelMatch || descMatch || tagsMatch) {
      highlights.push(node.id)
    }
  })
  return highlights
})

// Stats computation
const stats = computed(() => ({
  planets: activeNav.value === '全部' ? categories.value.length : 1,
  articles: activeNav.value === '全部'
    ? categories.value.reduce((sum, c) => sum + c.count, 0)
    : (allArticles.value.filter(art => art.category === activeNav.value)).length,
}))

// Current time
const currentTime = ref('')
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  requestAnimationFrame(updateTime)
}

onMounted(async () => {
  updateTime()

  try {
    let result: { name: string; count: number; lastUpdated: string }[] = []

    const [catsRes, articlesRes] = await Promise.all([
      userStore.isLoggedIn
        ? (getCategoriesService() as any)
        : (getPublicCategoriesService() as any),
      userStore.isLoggedIn
        ? (getAllArticlesService({ page: 1, limit: 200 }) as any)
        : (getPublicArticlesService({ page: 1, limit: 200 }) as any),
    ])

    const apiCategories: { id: number; name: string; article_count: number }[] = catsRes?.data || []
    const articles: any[] = articlesRes?.data || []
    allArticles.value = articles

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
  drawerOpen.value = false
  selectedArticle.value = null
}

function goBack() {
  router.back()
}

function onNodeClick(node: any) {
  selectedNode.value = node
  if (node.type === 'article') {
    selectedArticle.value = allArticles.value.find(art => art.id === node.id)
    drawerOpen.value = true
  } else if (node.type === 'category' && activeNav.value === '全部' && node.id !== 0) {
    activeNav.value = node.label
  }
}

function getRelatedArticles(art: any) {
  if (!art || !art.tags) return []
  return allArticles.value
    .filter(a => a.id !== art.id && a.tags && a.tags.some((t: string) => art.tags.includes(t)))
    .slice(0, 4)
}

function selectRelArticle(rel: any) {
  selectedArticle.value = rel
}

function readFullArticle(id: number) {
  router.push(`/articles/${id}`)
}
</script>

<template>
  <div class="vr-page">
    <!-- Canvas -->
    <StarfieldCanvas
      :nodes="knowledgeNodes"
      :links="knowledgeLinks"
      :highlightedNodes="highlightedNodes"
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
              知识图谱
            </h1>
            <p class="vr-topbar__subtitle">STARLORE GRAPH</p>
          </div>

          <div class="vr-topbar__right">
            <div class="vr-search-box">
              <input
                v-model="searchQuery"
                class="vr-search-input"
                placeholder="搜索文档/标签..."
              />
              <span class="vr-search-icon">&#128269;</span>
            </div>
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
          <div class="vr-console__stats-group">
            <div class="vr-console__stats">
              <div class="vr-console__stat">
                <div>
                  <span class="vr-console__stat-value">{{ stats.planets }}</span>
                  <span class="vr-console__stat-label">分类</span>
                </div>
              </div>
              <div class="vr-console__divider"></div>
              <div class="vr-console__stat">
                <div>
                  <span class="vr-console__stat-value">{{ stats.articles }}</span>
                  <span class="vr-console__stat-label">星记</span>
                </div>
              </div>
            </div>

            <!-- Graph filter settings -->
            <div class="vr-console__filters">
              <label class="vr-filter-label">
                <input type="checkbox" v-model="showCategories" />
                <span>分类</span>
              </label>
              <label class="vr-filter-label">
                <input type="checkbox" v-model="showArticles" />
                <span>文章</span>
              </label>
              <label class="vr-filter-label">
                <input type="checkbox" v-model="showLinks" />
                <span>关系线</span>
              </label>
            </div>
          </div>

          <div class="vr-console__hint">
            {{ activeNav === '全部' ? '悬浮查看 · 拖拽节点 · 点击文章查看详情' : '悬浮查看 · 拖拽节点 · 点击文章查看详情' }}
          </div>
        </footer>

        <!-- SIDE DRAWER PANEL -->
        <Transition name="panel-fade">
          <div v-if="drawerOpen && selectedArticle" class="vr-drawer glass-card">
            <button class="vr-drawer__close" @click="drawerOpen = false">&times;</button>
            <div class="vr-drawer__content">
              <span class="vr-drawer__category">{{ selectedArticle.category || '未分类' }}</span>
              <h2 class="vr-drawer__title">{{ selectedArticle.title }}</h2>
              <div class="vr-drawer__meta">
                <span>📅 更新时间：{{ selectedArticle.updatedAt?.slice(0, 10) || selectedArticle.createdAt?.slice(0, 10) || '-' }}</span>
              </div>
              <p class="vr-drawer__desc">{{ selectedArticle.description || '暂无描述' }}</p>
              
              <div v-if="selectedArticle.tags && selectedArticle.tags.length" class="vr-drawer__tags">
                <span v-for="tag in selectedArticle.tags" :key="tag" class="vr-drawer__tag">
                  # {{ tag }}
                </span>
              </div>
              
              <div class="vr-drawer__relations">
                <h3>关联文档 (通过共有标签)</h3>
                <div class="vr-drawer__rel-list">
                  <div
                    v-for="rel in getRelatedArticles(selectedArticle)"
                    :key="rel.id"
                    class="vr-drawer__rel-item"
                    @click="selectRelArticle(rel)"
                  >
                    <span class="vr-drawer__rel-title">{{ rel.title }}</span>
                    <span class="vr-drawer__rel-cat">{{ rel.category }}</span>
                  </div>
                  <div v-if="!getRelatedArticles(selectedArticle).length" class="vr-drawer__rel-empty">
                    暂无关联节点
                  </div>
                </div>
              </div>
              
              <button class="vr-drawer__btn" @click="readFullArticle(selectedArticle.id)">
                阅读全文 &rarr;
              </button>
            </div>
          </div>
        </Transition>
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

.vr-console__stats-group {
  display: flex;
  align-items: center;
  gap: 16px;
  pointer-events: none;
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

/* FILTERS */
.vr-console__filters {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 18px;
  background: rgba(18, 14, 30, 0.85);
  border: 1px solid rgba(100, 200, 255, 0.3);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  pointer-events: auto;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4);
}
.vr-filter-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}
.vr-filter-label input {
  cursor: pointer;
  accent-color: var(--accent);
}

/* SEARCH BOX */
.vr-search-box {
  position: relative;
  display: flex;
  align-items: center;
  pointer-events: auto;
  margin-right: 15px;
}
.vr-search-input {
  width: 150px;
  padding: 6px 32px 6px 14px;
  background: rgba(18, 14, 30, 0.6);
  border: 1px solid rgba(100, 200, 255, 0.2);
  border-radius: 999px;
  color: #fff;
  font-size: 13px;
  outline: none;
  backdrop-filter: blur(8px);
  transition: all 0.25s ease;
}
.vr-search-input:focus {
  border-color: rgba(100, 200, 255, 0.6);
  width: 220px;
  box-shadow: 0 0 10px rgba(100, 200, 255, 0.2);
}
.vr-search-icon {
  position: absolute;
  right: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

/* SIDE DRAWER */
.vr-drawer {
  position: absolute;
  top: 80px;
  right: 20px;
  bottom: 80px;
  width: 360px;
  background: rgba(18, 14, 30, 0.92) !important;
  border: 1px solid rgba(100, 200, 255, 0.3) !important;
  border-radius: 16px !important;
  backdrop-filter: blur(20px);
  box-shadow: -10px 0 30px rgba(0, 0, 0, 0.5);
  z-index: 100;
  pointer-events: auto;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
}
.vr-drawer__close {
  position: absolute;
  top: 16px;
  right: 16px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 24px;
  cursor: pointer;
  outline: none;
  transition: color 0.2s;
}
.vr-drawer__close:hover {
  color: #fff;
}
.vr-drawer__content {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.vr-drawer__category {
  font-size: 11px;
  font-weight: 600;
  color: var(--accent);
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin-bottom: 8px;
}
.vr-drawer__title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  line-height: 1.4;
  margin: 0 0 8px 0;
}
.vr-drawer__meta {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 16px;
}
.vr-drawer__desc {
  font-size: 13.5px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
  margin-bottom: 20px;
}
.vr-drawer__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 20px;
}
.vr-drawer__tag {
  font-size: 11px;
  color: #00ccff;
  background: rgba(0, 200, 255, 0.08);
  padding: 3px 8px;
  border-radius: 99px;
  border: 1px solid rgba(0, 200, 255, 0.15);
}
.vr-drawer__relations {
  margin-top: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 16px;
  margin-bottom: 20px;
}
.vr-drawer__relations h3 {
  font-size: 12.5px;
  color: #fff;
  margin: 0 0 10px 0;
  font-weight: 600;
}
.vr-drawer__rel-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.vr-drawer__rel-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.vr-drawer__rel-item:hover {
  background: rgba(100, 200, 255, 0.08);
  border-color: rgba(100, 200, 255, 0.2);
}
.vr-drawer__rel-title {
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.85);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}
.vr-drawer__rel-cat {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
}
.vr-drawer__rel-empty {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
  text-align: center;
  padding: 12px;
}
.vr-drawer__btn {
  width: 100%;
  padding: 10px;
  background: var(--accent);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.vr-drawer__btn:hover {
  background: var(--accent-hover);
}

/* DRAWER PANEL TRANSITION */
.panel-fade-enter-active,
.panel-fade-leave-active {
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.panel-fade-enter-from,
.panel-fade-leave-to {
  transform: translateX(400px);
  opacity: 0;
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
  .vr-search-box { margin-right: 5px; }
  .vr-search-input { width: 100px; padding: 4px 24px 4px 10px; font-size: 11px; }
  .vr-search-input:focus { width: 140px; }
  .vr-search-icon { right: 8px; font-size: 10px; }
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
  .vr-console__stats-group {
    flex-direction: column;
    gap: 8px;
    width: 100%;
    align-items: center;
  }
  .vr-console__stats { gap: 12px; padding: 6px 14px; }
  .vr-console__filters { gap: 10px; padding: 6px 12px; }
  .vr-console__stat-value { font-size: 15px; }
  .vr-console__hint { font-size: 10.5px; }
  .vr-btn--back { padding: 6px 12px; font-size: 12px; }
  .vr-btn--back span { display: none; }
  .vr-drawer {
    left: 20px;
    width: auto;
    bottom: 120px;
    top: 80px;
  }
}
</style>
