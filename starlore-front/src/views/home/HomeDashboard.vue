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

const openKnowledgeSearch = () => {
  const query = searchQuery.value.trim()
  router.push(query ? { path: '/articles', query: { search: query } } : '/articles')
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
    <section class="knowledge-hero">
      <div class="knowledge-intro">
        <div class="knowledge-intro-copy">
          <p class="knowledge-eyebrow">我的知识库</p>
          <h1>
            <span>{{ displayName }}，</span>
            知识正在形成星系
          </h1>
          <p>搜索、续写，或从一条旧知识重新出发。</p>
        </div>
        <div class="knowledge-constellation knowledge-constellation--interactive" aria-label="知识星系">
          <div class="constellation-bg-svg" aria-hidden="true">
            <svg viewBox="0 0 260 250" role="presentation">
              <ellipse class="constellation-path orbit-one" cx="130" cy="124" rx="98" ry="43" transform="rotate(-18 130 124)" />
              <ellipse class="constellation-path orbit-two" cx="130" cy="124" rx="84" ry="62" transform="rotate(38 130 124)" />
              <path class="constellation-path constellation-path-faint orbit-trail" d="M53 80 C91 31 178 32 216 82 C242 116 229 174 183 205" />

              <circle class="constellation-halo halo-outer" cx="130" cy="124" r="48" />
              <circle class="constellation-halo halo-inner" cx="130" cy="124" r="32" />

              <circle class="constellation-node node-accent" cx="73" cy="61" r="7" />
              <circle class="constellation-node node-sky" cx="211" cy="84" r="6" />
              <circle class="constellation-node node-warm" cx="214" cy="159" r="5" />
              <circle class="constellation-node node-small" cx="49" cy="155" r="4" />
              <circle class="constellation-node node-small" cx="104" cy="196" r="4" />
              <circle class="constellation-node node-muted" cx="184" cy="207" r="3" />
              <circle class="constellation-star-dot" cx="36" cy="91" r="2" />
              <circle class="constellation-star-dot" cx="224" cy="53" r="2.5" />
              <circle class="constellation-star-dot" cx="231" cy="188" r="1.8" />
            </svg>
          </div>
          <div class="constellation-ball-core">
            <EmotionBall :size="220" :show-rings="false" />
          </div>
          <span>KNOWLEDGE MAP</span>

        </div>
      </div>

      <form class="knowledge-search" role="search" @submit.prevent="openKnowledgeSearch">
        <Search :size="21" aria-hidden="true" />
        <label class="sr-only" for="knowledge-search-input">搜索知识库</label>
        <input
          id="knowledge-search-input"
          v-model="searchQuery"
          type="search"
          placeholder="搜索标题、正文与分类"
          autocomplete="off"
        />
        <button type="submit">搜索</button>
      </form>

      <nav class="knowledge-shortcuts" aria-label="快捷操作">
        <router-link to="/articles/edit" class="knowledge-create">
          <Plus :size="17" />
          新建知识
        </router-link>
        <router-link to="/echobot" class="ask-knowledge">
          <MessageCircle :size="17" />
          询问知识库
        </router-link>
        <router-link to="/articles">
          浏览全部知识
          <span>{{ totalArticles }}</span>
        </router-link>
        <router-link to="/vr">
          查看知识图谱
          <ChevronRight :size="15" />
        </router-link>
      </nav>
    </section>

    <div class="knowledge-dashboard">
      <section class="knowledge-recent" aria-labelledby="recent-heading">
        <div class="knowledge-section-heading">
          <div>
            <h2 id="recent-heading">最近访问</h2>
            <p>继续阅读或整理最近接触的内容</p>
          </div>
          <router-link to="/articles">全部知识 <ChevronRight :size="16" /></router-link>
        </div>

        <div v-if="recentArticles.length" class="knowledge-list">
          <router-link
            v-for="article in recentArticles"
            :key="article.id"
            :to="`/articles/${article.id}`"
            class="knowledge-row"
          >
            <span class="knowledge-file-icon"><FileText :size="18" /></span>
            <span class="knowledge-row-main">
              <strong>{{ article.title || '未命名知识' }}</strong>
              <small>{{ article.description || '打开继续阅读与整理' }}</small>
            </span>
            <span class="knowledge-row-meta">
              <span v-if="article.category" class="knowledge-category">{{ article.category }}</span>
              <time>{{ formatKnowledgeDate(article.createdAt) }}</time>
            </span>
            <ChevronRight :size="17" class="row-arrow" />
          </router-link>
        </div>

        <div v-else class="knowledge-empty">
          <span class="knowledge-file-icon"><FileText :size="20" /></span>
          <div>
            <h3>知识库还是空的</h3>
            <p>先记录一个想法，以后就能在这里快速找到它。</p>
          </div>
          <router-link to="/articles/edit">创建第一条知识</router-link>
        </div>
      </section>

      <aside class="knowledge-sidebar">
        <section class="knowledge-spaces" aria-labelledby="spaces-heading">
          <div class="knowledge-section-heading spaces-heading">
            <div>
              <h2 id="spaces-heading">知识空间</h2>
              <p>{{ totalCategories }} 个空间</p>
            </div>
            <router-link to="/categories" aria-label="管理知识空间">
              <ChevronRight :size="17" />
            </router-link>
          </div>

          <div v-if="categories.length" class="space-list">
            <router-link
              v-for="category in categories.slice(0, 6)"
              :key="category.name"
              :to="{ path: '/articles', query: { category: category.name } }"
              class="space-row"
            >
              <span class="space-dot" aria-hidden="true"></span>
              <span>{{ category.name }}</span>
              <small>{{ category.article_count ?? 0 }}</small>
            </router-link>
          </div>
          <div v-else class="spaces-empty">
            <p>用空间组织同一主题下的知识。</p>
            <router-link to="/categories">创建知识空间</router-link>
          </div>
        </section>

        <router-link
          v-if="recentArticles.length"
          :to="`/articles/${recentArticles[recentArticles.length - 1].id}`"
          class="rediscover-card"
        >
          <span class="rediscover-label">随机重读</span>
          <strong>{{ recentArticles[recentArticles.length - 1].title }}</strong>
          <p>重新看看一条旧知识，也许会有新的发现。</p>
          <span class="rediscover-link">打开知识 <ChevronRight :size="15" /></span>
        </router-link>

        <div class="library-summary">
          <span><strong>{{ totalArticles }}</strong> 条知识</span>
          <span><strong>{{ totalCategories }}</strong> 个空间</span>
        </div>
      </aside>
    </div>
  </main>
</template>

<style scoped src="./HomeDashboard.css"></style>
