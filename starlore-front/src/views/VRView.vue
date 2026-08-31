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
                <span>更新时间：{{ selectedArticle.updatedAt?.slice(0, 10) || selectedArticle.createdAt?.slice(0, 10) || '-' }}</span>
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

<style scoped src="./VRView.css"></style>
