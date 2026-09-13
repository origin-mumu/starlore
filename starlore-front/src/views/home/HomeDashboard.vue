<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmotionBall from '@/components/EmotionBall.vue'
import { getBlogStatsService } from '@/api/article'
import { Article } from '@/type/Article'
import { useUserStore } from '@/stores/user'
import {
  Search,
  Plus,
  MessageCircle,
  FileText,
  ChevronRight,
  Compass,
  Sparkles,
  Orbit,
  Clock,
  ArrowRight,
  Lightbulb,
  ArrowUpRight,
  Layers,
} from '@lucide/vue'

const userStore = useUserStore()
const router = useRouter()

const data = ref()
const articles = ref<Article[]>([])
const searchQuery = ref('')

interface KnowledgeCategory {
  id?: number
  name: string
  article_count?: number
}

const categories = computed<KnowledgeCategory[]>(() => data.value?.popularCategories ?? [])
const totalArticles = computed(() => data.value?.totalArticles ?? articles.value?.length ?? 0)
const totalCategories = computed(() => data.value?.totalCategories ?? categories.value.length)
const recentArticles = computed(() => (articles.value ?? []).slice(0, 6))
const displayName = computed(
  () => userStore.user?.nickname || userStore.user?.username || '探索者',
)

// 动态时段问候语
const timeGreeting = computed(() => {
  const hour = new Date().getHours()
  if (hour >= 5 && hour < 12) return '早安'
  if (hour >= 12 && hour < 18) return '午安'
  return '晚上好'
})

// 随机重读文章
const rediscoverArticle = computed(() => {
  if (!articles.value || articles.value.length === 0) return null
  return articles.value[articles.value.length - 1]
})

const formatKnowledgeDate = (value?: string) => {
  if (!value) return '最近更新'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '最近更新'
  const diff = Date.now() - date.getTime()
  const day = 24 * 60 * 60 * 1000
  if (diff < day && date.getDate() === new Date().getDate()) return '今天'
  if (diff < day * 2) return '昨天'
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// 知识库搜索
const openKnowledgeSearch = () => {
  const query = searchQuery.value.trim()
  router.push(query ? { path: '/articles', query: { search: query } } : '/articles')
}

// 向 AI 提问直通 Harness
const askAiHarness = (customQuery?: string) => {
  const q = (customQuery || searchQuery.value).trim()
  if (q) {
    router.push({ path: '/harness', query: { prompt: q } })
  } else {
    router.push('/harness')
  }
}

onMounted(async () => {
  try {
    const res = await getBlogStatsService()
    const stats = res.data?.data ?? res.data
    data.value = stats
    articles.value = stats?.popularArticles ?? []
  } catch (err) {
    console.error('获取知识库统计失败:', err)
    articles.value = []
  }
})
</script>

<template>
  <main class="knowledge-home">
    <!-- ── 顶部 Hero：知识工作台问候与 AI 伴侣灵动卡片 ── -->
    <section class="hero-workbench-section">
      <div class="hero-aurora-glow" aria-hidden="true"></div>

      <div class="hero-grid">
        <!-- 左侧：问候、标题、双模搜索与快捷动作 -->
        <div class="hero-intro-copy">
          <div class="greeting-kicker">
            <span class="pulse-dot"></span>
            <span class="kicker-text">{{ timeGreeting }}，{{ displayName }} · 知识星系正在演进</span>
          </div>

          <h1 class="hero-main-title">
            探索、续写，<br />
            让每一个灵感形成星图
          </h1>

          <p class="hero-subtext">
            融合 Multi-Agent 深度协作与 RAG 向量检索，构建属于你的智慧沉淀空间。
          </p>

          <!-- 全局双模搜索 Command Bar -->
          <form class="command-search-bar" role="search" @submit.prevent="openKnowledgeSearch">
            <Search :size="20" class="search-icon" aria-hidden="true" />
            <input
              id="knowledge-search-input"
              v-model="searchQuery"
              type="search"
              placeholder="搜索星记、星域、概念... 或输入问题向 AI 提问"
              autocomplete="off"
            />
            <div class="search-actions-group">
              <button type="submit" class="btn-search-normal" title="在知识库中搜索">
                <span>搜索星记</span>
              </button>
              <button
                type="button"
                class="btn-search-ai"
                @click="askAiHarness()"
                title="携当前内容前往 Harness 智能体解答"
              >
                <Sparkles :size="14" />
                <span>AI 提问</span>
              </button>
            </div>
          </form>

          <!-- 核心操作快捷入口 -->
          <div class="hero-shortcuts-row">
            <router-link to="/articles/edit" class="shortcut-pill shortcut-pill--primary">
              <Plus :size="16" />
              <span>新建知识星记</span>
            </router-link>

            <router-link to="/harness" class="shortcut-pill">
              <MessageCircle :size="15" />
              <span>智能体对话</span>
            </router-link>

            <router-link to="/vr" class="shortcut-pill">
              <Orbit :size="15" />
              <span>3D 知识星图</span>
            </router-link>

            <router-link to="/diverge" class="shortcut-pill">
              <Sparkles :size="15" />
              <span>灵感发散</span>
            </router-link>
          </div>
        </div>

        <!-- 右侧：AI 伴侣灵动卡片 -->
        <div class="hero-mascot-card">
          <div class="mascot-card-inner">
            <div class="mascot-sphere-wrap">
              <EmotionBall
                :size="200"
                shape="blob"
                emotion="02"
                :show-rings="true"
                :show-style-toggle="true"
                :interactive="true"
                :follow="true"
              />
            </div>

            <div class="mascot-meta">

              <div class="mascot-prompt-chips">
                <button
                  type="button"
                  class="prompt-chip"
                  @click="askAiHarness('请帮我总结知识库中近期的核心主题与逻辑关联')"
                >
                  <Sparkles :size="12" />
                  <span>总结近期沉淀</span>
                </button>
                <button
                  type="button"
                  class="prompt-chip"
                  @click="router.push('/diverge')"
                >
                  <Lightbulb :size="12" />
                  <span>发散新想法</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── 知识核心数据 Bento 看板 ── -->
    <section class="bento-stats-grid" aria-label="知识库核心数据">
      <router-link to="/articles" class="bento-stat-card">
        <div class="stat-card-icon-wrap">
          <FileText :size="22" />
        </div>
        <div class="stat-card-info">
          <span class="stat-card-num">{{ totalArticles }}</span>
          <span class="stat-card-title">沉淀星记</span>
          <span class="stat-card-sub">已记录的知识篇目</span>
        </div>
        <ArrowUpRight :size="16" class="stat-card-arrow" />
      </router-link>

      <router-link to="/categories" class="bento-stat-card">
        <div class="stat-card-icon-wrap stat-icon-sky">
          <Compass :size="22" />
        </div>
        <div class="stat-card-info">
          <span class="stat-card-num">{{ totalCategories }}</span>
          <span class="stat-card-title">知识星域</span>
          <span class="stat-card-sub">多维主题与空间归类</span>
        </div>
        <ArrowUpRight :size="16" class="stat-card-arrow" />
      </router-link>

      <router-link to="/harness" class="bento-stat-card">
        <div class="stat-card-icon-wrap stat-icon-purple">
          <Sparkles :size="22" />
        </div>
        <div class="stat-card-info">
          <span class="stat-card-num">Harness</span>
          <span class="stat-card-title">云端智能体</span>
          <span class="stat-card-sub">向量 RAG 与思维链协作</span>
        </div>
        <ArrowUpRight :size="16" class="stat-card-arrow" />
      </router-link>

      <router-link to="/vr" class="bento-stat-card">
        <div class="stat-card-icon-wrap stat-icon-warm">
          <Orbit :size="22" />
        </div>
        <div class="stat-card-info">
          <span class="stat-card-num">3D 星图</span>
          <span class="stat-card-title">空间关联网络</span>
          <span class="stat-card-sub">沉浸式三维知识星系</span>
        </div>
        <ArrowUpRight :size="16" class="stat-card-arrow" />
      </router-link>
    </section>

    <!-- ── 主工作区：双列知识流与侧翼空间 ── -->
    <div class="knowledge-workbench-grid">
      <!-- 左列：最近访问与整理 -->
      <section class="workbench-main-stream" aria-labelledby="recent-stream-heading">
        <div class="stream-section-header">
          <div class="header-left">
            <Clock :size="18" class="stream-header-icon" />
            <div>
              <h2 id="recent-stream-heading" class="stream-title">最近访问与整理</h2>
              <p class="stream-subtitle">继续阅读、迭代或沉淀最近接触的内容</p>
            </div>
          </div>
          <router-link to="/articles" class="stream-more-link">
            <span>全部星记 ({{ totalArticles }})</span>
            <ArrowRight :size="15" />
          </router-link>
        </div>

        <div v-if="recentArticles.length" class="knowledge-stream-list">
          <router-link
            v-for="article in recentArticles"
            :key="article.id"
            :to="`/articles/${article.id}`"
            class="knowledge-item-card"
          >
            <div class="item-icon-box">
              <FileText :size="18" />
            </div>

            <div class="item-body">
              <div class="item-title-row">
                <strong class="item-title">{{ article.title || '未命名知识' }}</strong>
                <span v-if="article.category" class="item-category-tag">
                  {{ article.category }}
                </span>
              </div>
              <p class="item-desc">
                {{ article.description || '点击打开继续深入阅读与结构化整理...' }}
              </p>
            </div>

            <div class="item-meta-right">
              <time class="item-date">{{ formatKnowledgeDate(article.createdAt) }}</time>
              <ChevronRight :size="17" class="item-arrow" />
            </div>
          </router-link>
        </div>

        <div v-else class="stream-empty-state">
          <FileText :size="40" class="empty-stream-icon" />
          <h3>知识星系尚是一片虚空</h3>
          <p>记录下第一个闪念或文章，知识将在宇宙中相遇并连接成网。</p>
          <router-link to="/articles/edit" class="btn-create-first">
            <Plus :size="16" />
            <span>创建第一篇知识</span>
          </router-link>
        </div>
      </section>

      <!-- 右列：知识空间、随机重读与发散导流 -->
      <aside class="workbench-sidebar">
        <!-- 知识空间矩阵 -->
        <div class="sidebar-block spaces-block">
          <div class="block-header">
            <div class="block-title-group">
              <Compass :size="17" class="block-icon" />
              <h3>知识星域</h3>
            </div>
            <router-link to="/categories" class="block-link" aria-label="管理星域">
              <span>管理星域</span>
              <ChevronRight :size="14" />
            </router-link>
          </div>

          <div v-if="categories.length" class="category-pills-grid">
            <router-link
              v-for="cat in categories.slice(0, 8)"
              :key="cat.name"
              :to="{ path: '/articles', query: { category: cat.name } }"
              class="cat-chip"
            >
              <span class="cat-chip-name">{{ cat.name }}</span>
              <span class="cat-chip-count">{{ cat.article_count ?? 0 }}</span>
            </router-link>
          </div>
          <div v-else class="block-empty-tip">
            <p>尚未建立知识星域分类</p>
            <router-link to="/categories" class="block-empty-action">创建星域 →</router-link>
          </div>
        </div>

        <!-- 灵感与随机重读 -->
        <router-link
          v-if="rediscoverArticle"
          :to="`/articles/${rediscoverArticle.id}`"
          class="sidebar-block rediscover-block"
        >
          <div class="rediscover-badge">
            <Sparkles :size="13" />
            <span>随机重读 · 发现遗忘</span>
          </div>
          <h4 class="rediscover-title">{{ rediscoverArticle.title }}</h4>
          <p class="rediscover-excerpt">
            {{ rediscoverArticle.description || '重新回顾一条旧日知识，在新的思考维度下常有惊喜发现。' }}
          </p>
          <div class="rediscover-action-row">
            <span>打开阅读</span>
            <ArrowRight :size="14" />
          </div>
        </router-link>

        <!-- 创意发散入口 -->
        <router-link to="/diverge" class="sidebar-block diverge-promo-block">
          <div class="diverge-promo-header">
            <div class="promo-icon-wrap">
              <Lightbulb :size="18" />
            </div>
            <div>
              <h4>思维发散引擎</h4>
              <p>从一个核心概念出发，AI 助你向外衍生思维导图</p>
            </div>
          </div>
          <div class="diverge-promo-link">
            <span>开启发散思维</span>
            <ArrowRight :size="14" />
          </div>
        </router-link>
      </aside>
    </div>
  </main>
</template>

<style scoped src="./HomeDashboard.css"></style>
