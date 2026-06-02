<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getAllArticlesService, getCategoriesService } from '@/api/article'
import { useVRScene } from './vr/useVRScene'
import VRInfoCard from './vr/VRInfoCard.vue'
import VRAssistant from './vr/VRAssistant.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const canvasRef = ref<HTMLDivElement>()
const showUI = ref(false)
const sidebarOpen = ref(false)

const {
  loading,
  currentTime,
  stats,
  hoveredPlanet,
  selectedCategory,
  currentViewMode,
  moreClicked,
  coreDblClicked,
  buildScene,
  startTimeUpdater,
  resetCamera,
  switchView,
  setArticlesByCategory,
} = useVRScene()

/* --- Category data --- */
const categories = ref<{ name: string; count: number; lastUpdated: string }[]>([])
const navItems = ref<string[]>(['全部'])
const activeNav = ref('全部')

/* --- Demo data for guests --- */
const demoCategories = [
  { name: '前端开发', count: 5, lastUpdated: '2026-05-20' },
  { name: '后端技术', count: 3, lastUpdated: '2026-05-18' },
  { name: '设计思考', count: 2, lastUpdated: '2026-05-15' },
]

/* --- Fetch data and build scene --- */
onMounted(async () => {
  startTimeUpdater()

  try {
    let result: { name: string; count: number; lastUpdated: string }[] = []

    if (!userStore.isLoggedIn) {
      result = demoCategories
    } else {
      /* Fetch categories and articles in parallel */
      const [catsRes, articlesRes] = await Promise.all([
        getCategoriesService() as any,
        getAllArticlesService({ page: 1, limit: 200 }) as any,
      ])

      const apiCategories: { id: number; name: string; article_count: number }[] = catsRes?.data || []
      const articles: any[] = articlesRes?.data || []

      /* Build article stats map: category name -> { count, lastUpdated } */
      const articleStats = new Map<string, { count: number; lastUpdated: string }>()
      for (const a of articles) {
        const cat = a.category || '未分类'
        if (!articleStats.has(cat)) articleStats.set(cat, { count: 0, lastUpdated: '' })
        const s = articleStats.get(cat)!
        s.count++
        const d = a.updatedAt || a.createdAt
        if (d && d > s.lastUpdated) s.lastUpdated = d.slice(0, 10)
      }

      /* Build planets from API categories (source of truth) */
      for (const cat of apiCategories) {
        const stats = articleStats.get(cat.name)
        result.push({
          name: cat.name,
          count: stats?.count ?? cat.article_count ?? 0,
          lastUpdated: stats?.lastUpdated || '-',
        })
      }
      /* Also include any article categories not in the category list */
      for (const [name, s] of articleStats) {
        if (!result.find(r => r.name === name)) {
          result.push({ name, count: s.count, lastUpdated: s.lastUpdated })
        }
      }

      /* Build articles grouped by category for label switching */
      const groupedArticles = new Map<string, { id: number; title: string }[]>()
      for (const a of articles) {
        const cat = a.category || '未分类'
        if (!groupedArticles.has(cat)) groupedArticles.set(cat, [])
        groupedArticles.get(cat)!.push({ id: a.id, title: a.title })
      }
      setArticlesByCategory(groupedArticles)
    }

    categories.value = result
    navItems.value = ['全部', ...result.map(r => r.name)]

    /* Build 3D scene */
    await nextTick()
    if (canvasRef.value) {
      await buildScene(canvasRef.value, result)
    }

    /* Fade in UI */
    setTimeout(() => {
      showUI.value = true
    }, 300)
  } catch (e) {
    console.error('VR scene init failed:', e)
    loading.value = false
  }
})

/* --- Navigation --- */
function onNavClick(name: string) {
  activeNav.value = name
  switchView(name)
  if (name === '全部') {
    resetCamera()
    selectedCategory.value = ''
  }
}

function goBack() {
  router.back()
}

/* --- Watch selected category from 3D click --- update sidebar + switch scene --- */
watch(selectedCategory, cat => {
  if (cat) {
    activeNav.value = cat
    switchView(cat)
  }
})

/* --- Core planet double-clicked --- switch to 全部 --- */
watch(coreDblClicked, val => {
  if (val) {
    onNavClick('全部')
    coreDblClicked.value = false
  }
})

/* --- "more" planet clicked --- navigate to relevant page --- */
watch(moreClicked, val => {
  if (val) {
    const mode = currentViewMode.value
    if (!mode || mode === '全部') {
      router.push('/categories')
    } else {
      router.push({ path: '/articles', query: { category: mode } })
    }
    /* Reset immediately to prevent re-trigger */
    moreClicked.value = false
  }
})
</script>

<template>
  <div class="vr-page">
    <!-- 3D Canvas -->
    <div ref="canvasRef" class="vr-canvas"></div>

    <!-- UI Overlay -->
    <Transition name="ui-fade">
      <div v-if="showUI" class="vr-overlay">
        <!-- ====== TOP BAR ====== -->
        <header class="vr-topbar">
          <button class="vr-btn vr-btn--back" @click="goBack">
            <svg
              viewBox="0 0 24 24"
              width="16"
              height="16"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
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

        <!-- ====== LEFT SIDEBAR NAV ====== -->
        <nav class="vr-sidebar" :class="{ 'vr-sidebar--open': sidebarOpen }">
          <button class="vr-sidebar__toggle" @click="sidebarOpen = !sidebarOpen">
            <svg
              viewBox="0 0 24 24"
              width="18"
              height="18"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
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
              <span
                class="vr-sidebar__dot"
                :class="{ 'vr-sidebar__dot--active': activeNav === item }"
              ></span>
              <span>{{ item }}</span>
            </button>
          </div>
        </nav>

        <!-- ====== BOTTOM CONSOLE ====== -->
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
          <div class="vr-console__hint">拖拽旋转 · 滚轮缩放 · 双击星球查看</div>
        </footer>

        <!-- ====== HOVER INFO CARD ====== -->
        <Transition name="card-slide">
          <div v-if="hoveredPlanet" class="vr-info-anchor">
            <VRInfoCard :planet="hoveredPlanet" />
          </div>
        </Transition>

        <!-- ====== ASSISTANT ====== -->
        <div class="vr-assistant-anchor">
          <VRAssistant />
        </div>
      </div>
    </Transition>

    <!-- ====== LOADING SCREEN ====== -->
    <Transition name="loading-fade">
      <div v-if="loading" class="vr-loading">
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
/* ============================================================ */
/*  BASE                                                         */
/* ============================================================ */
.vr-page {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #0a0812;
  z-index: 1;
}

.vr-canvas {
  width: 100%;
  height: 100%;
}

.vr-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 10;
}

/* ============================================================ */
/*  TOP BAR                                                      */
/* ============================================================ */
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

.vr-topbar__center {
  text-align: center;
}

.vr-topbar__title {
  font-size: 16px;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 3px;
  margin: 0;
  text-shadow: 0 0 20px oklch(0.55 0.15 35 / 0.4);
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

/* ============================================================ */
/*  BUTTONS                                                      */
/* ============================================================ */
.vr-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid oklch(0.55 0.15 35 / 0.2);
  border-radius: 10px;
  background: rgba(18, 14, 30, 0.6);
  color: #d0c8e0;
  font-size: 13px;
  cursor: pointer;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: all 0.25s ease;
  pointer-events: auto;
}

.vr-btn:hover {
  background: oklch(0.55 0.15 35 / 0.15);
  border-color: oklch(0.55 0.15 35 / 0.4);
  color: #fff;
  transform: translateY(-1px);
}

/* ============================================================ */
/*  LEFT SIDEBAR                                                 */
/* ============================================================ */
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
  border: 1px solid oklch(0.55 0.15 35 / 0.2);
  border-radius: 10px;
  background: rgba(18, 14, 30, 0.7);
  color: #d0c8e0;
  cursor: pointer;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
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
  border: 1px solid oklch(0.55 0.15 35 / 0.4);
  border-radius: 14px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.5),
    inset 0 0 30px oklch(0.55 0.15 35 / 0.05);
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
  background: oklch(0.55 0.15 35 / 0.1);
  color: #e2e8f0;
}

.vr-sidebar__item--active {
  background: oklch(0.55 0.15 35 / 0.3);
  color: #ffffff;
  text-shadow: 0 0 20px rgba(183, 178, 255, 0.8);
}

.vr-sidebar__dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: oklch(0.55 0.15 35 / 0.3);
  transition: all 0.2s ease;
}

.vr-sidebar__dot--active {
  background: var(--accent);
  box-shadow: 0 0 8px oklch(0.55 0.15 35 / 0.5);
}

/* ============================================================ */
/*  BOTTOM CONSOLE                                               */
/* ============================================================ */
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
  border: 1px solid oklch(0.55 0.15 35 / 0.3);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow:
    0 4px 15px rgba(0, 0, 0, 0.4),
    inset 0 0 20px oklch(0.55 0.15 35 / 0.05);
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
  background: oklch(0.55 0.15 35 / 0.15);
}

.vr-console__hint {
  font-size: 12px;
  color: #ffffff;
  letter-spacing: 0.5px;
  text-shadow: 0 0 10px rgba(183, 178, 255, 0.6);
}

/* ============================================================ */
/*  INFO CARD ANCHOR                                             */
/* ============================================================ */
.vr-info-anchor {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  pointer-events: none;
  z-index: 15;
}

/* ============================================================ */
/*  ASSISTANT ANCHOR                                             */
/* ============================================================ */
.vr-assistant-anchor {
  position: absolute;
  bottom: 70px;
  right: 24px;
  pointer-events: auto;
  z-index: 15;
}

/* ============================================================ */
/*  LOADING SCREEN                                               */
/* ============================================================ */
.vr-loading {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0a0812;
  z-index: 100;
}

.vr-loading__content {
  text-align: center;
}

.vr-loading__ring {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  position: relative;
  border: 2px solid oklch(0.55 0.15 35 / 0.1);
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
  to {
    transform: rotate(360deg);
  }
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
  background: oklch(0.55 0.15 35 / 0.1);
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
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(400%);
  }
}

/* ============================================================ */
/*  TRANSITIONS                                                  */
/* ============================================================ */
.ui-fade-enter-active {
  transition: opacity 0.8s ease 0.3s;
}
.ui-fade-leave-active {
  transition: opacity 0.3s ease;
}
.ui-fade-enter-from,
.ui-fade-leave-to {
  opacity: 0;
}

.card-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.card-slide-leave-active {
  transition: all 0.2s ease-in;
}
.card-slide-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(12px) scale(0.95);
}
.card-slide-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px) scale(0.97);
}

.loading-fade-leave-active {
  transition: opacity 0.6s ease;
}
.loading-fade-leave-to {
  opacity: 0;
}

/* ============================================================ */
/*  RESPONSIVE                                                   */
/* ============================================================ */
@media (max-width: 768px) {
  .vr-topbar {
    padding: 12px 14px;
  }

  .vr-topbar__title {
    font-size: 14px;
    letter-spacing: 2px;
  }

  .vr-topbar__subtitle {
    display: none;
  }

  .vr-topbar__right {
    gap: 10px;
  }

  .vr-topbar__time {
    font-size: 11px;
  }

  .vr-sidebar {
    left: 8px;
  }

  .vr-sidebar__toggle {
    display: flex;
  }

  .vr-sidebar__items {
    display: none;
  }

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

  .vr-console__stats {
    gap: 12px;
    padding: 6px 14px;
  }

  .vr-console__stat-value {
    font-size: 15px;
  }

  .vr-console__hint {
    font-size: 10.5px;
  }

  .vr-assistant-anchor {
    bottom: 100px;
    right: 12px;
  }

  .vr-info-anchor {
    bottom: 110px;
    left: 50%;
    width: calc(100% - 32px);
    max-width: 300px;
  }

  .vr-btn--back {
    padding: 6px 12px;
    font-size: 12px;
  }

  .vr-btn--back span {
    display: none;
  }
}
</style>
