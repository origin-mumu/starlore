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
            <EmotionBall :size="172" shape="blob" emotion="02" :show-rings="false" />
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

<style scoped>
.knowledge-home {
  --soft-glass: rgba(255, 255, 255, 0.6);
  --soft-glass-strong: rgba(255, 255, 255, 0.72);
  --soft-glass-border: rgba(255, 255, 255, 0.88);
  --soft-glass-shadow:
    0 40px 50px -32px rgba(72, 53, 42, 0.14),
    inset 0 0 20px rgba(255, 255, 255, 0.25);
  width: min(1080px, calc(100% - 32px));
  margin: 0 auto;
  padding: 128px 0 72px;
  font-family: 'Inter', 'Noto Sans SC', system-ui, sans-serif;
  position: relative;
  isolation: isolate;
}

.knowledge-home::before {
  content: '';
  position: absolute;
  z-index: -2;
  inset: 82px -8vw auto;
  height: 560px;
  background-image: radial-gradient(circle, color-mix(in srgb, var(--ink-muted) 42%, transparent) 1px, transparent 1.2px);
  background-size: 24px 24px;
  opacity: 0.34;
  mask-image: linear-gradient(to bottom, black 5%, black 65%, transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, black 5%, black 65%, transparent 100%);
}

.knowledge-home::after {
  content: '';
  position: absolute;
  z-index: -1;
  top: 118px;
  left: 8%;
  width: 78%;
  height: 420px;
  background:
    radial-gradient(circle at 24% 48%, rgba(111, 169, 231, 0.17), transparent 38%),
    radial-gradient(circle at 76% 42%, rgba(151, 128, 218, 0.14), transparent 39%);
  filter: blur(42px);
  pointer-events: none;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.knowledge-hero {
  width: 100%;
  min-height: auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
  margin-bottom: 16px;
}

.knowledge-intro {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 32px;
  margin-bottom: 16px;
}

.knowledge-intro-copy {
  min-width: 0;
}

.knowledge-eyebrow,
.section-caption {
  margin: 0 0 8px;
  color: var(--ink-muted);
  font-family: 'Inter', 'Noto Sans SC', system-ui, sans-serif;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.knowledge-intro h1 {
  max-width: 680px;
  margin: 0 0 16px;
  color: var(--ink);
  font-family: 'Inter', 'Noto Sans SC', system-ui, sans-serif;
  font-size: clamp(2.65rem, 5.2vw, 4.8rem);
  font-weight: 850;
  line-height: 0.98;
  letter-spacing: -0.075em;
}

.knowledge-intro h1 span {
  display: block;
  margin-bottom: 8px;
  color: var(--ink-soft);
  font-size: 0.34em;
  font-weight: 650;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.knowledge-intro p:last-child {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.9rem;
}

.knowledge-create {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  flex-shrink: 0;
  border: 1px solid color-mix(in srgb, var(--ink) 15%, transparent);
  border-radius: var(--radius-full);
  background: var(--ink) !important;
  color: #FFFFFF !important;
  font-family: 'Inter', 'Noto Sans SC', system-ui, sans-serif;
  font-size: 0.9rem;
  font-weight: 650;
  text-decoration: none;
  box-shadow: var(--shadow-button);
  transition: transform 180ms var(--ease-out-quart), box-shadow 180ms var(--ease-out-quart);
}

.knowledge-create svg {
  color: #FFFFFF !important;
}

.knowledge-create:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-button-hover);
  color: #FFFFFF !important;
}

.knowledge-constellation {
  position: relative;
  width: clamp(300px, 26vw, 360px);
  grid-column: 2;
  justify-self: end;
  color: var(--ink);
  transform: translateY(12px);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.constellation-bg-svg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.knowledge-constellation svg {
  display: block;
  width: 100%;
  overflow: visible;
}

.constellation-ball-core {
  position: relative;
  width: 160px;
  height: 160px;
  margin: 40px 0 44px;
  z-index: 2;
  display: flex;
  justify-content: center;
  align-items: center;
}

.knowledge-constellation > span {
  display: block;
  margin-top: -12px;
  color: var(--ink-muted);
  font: 650 0.65rem/1 'Inter', system-ui, sans-serif;
  letter-spacing: 0.16em;
  text-align: center;
  z-index: 3;
}

.constellation-path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
  opacity: 0.22;
  vector-effect: non-scaling-stroke;
}

.constellation-path-faint {
  stroke-dasharray: 3 7;
  opacity: 0.16;
}

.orbit-one {
  stroke: color-mix(in srgb, var(--accent-sky) 58%, currentColor);
  opacity: 0.28;
}

.orbit-two {
  opacity: 0.17;
}

.constellation-halo {
  fill: rgba(116, 137, 215, 0.08);
  stroke: rgba(116, 137, 215, 0.15);
  stroke-width: 1;
}

.halo-outer {
  fill: rgba(116, 137, 215, 0.055);
  stroke-dasharray: 2 5;
}

.halo-inner {
  fill: rgba(145, 197, 211, 0.1);
  stroke: rgba(145, 197, 211, 0.2);
}

.constellation-node {
  fill: var(--canvas);
  stroke: color-mix(in srgb, var(--ink) 72%, transparent);
  stroke-width: 2;
  vector-effect: non-scaling-stroke;
}

.node-accent,
.node-sky,
.node-warm {
  stroke: var(--canvas);
  stroke-width: 3;
}

.node-accent {
  fill: color-mix(in srgb, var(--accent) 68%, var(--canvas));
}

.node-sky {
  fill: color-mix(in srgb, var(--accent-sky) 62%, var(--canvas));
}

.node-warm {
  fill: color-mix(in srgb, var(--warm) 62%, var(--canvas));
}

.node-muted {
  fill: var(--ink-muted);
  stroke: none;
}

.constellation-star-dot {
  fill: var(--ink-muted);
  opacity: 0.42;
}

.knowledge-search {
  position: relative;
  z-index: 1;
  width: min(640px, 100%);
  min-height: 60px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 10px 8px 20px;
  border: 1px solid var(--soft-glass-border);
  border-radius: var(--radius-full);
  background: var(--soft-glass-strong);
  color: var(--ink-muted);
  box-shadow:
    0 18px 32px -26px rgba(72, 53, 42, 0.2),
    inset 0 0 16px rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  transition: border-color 180ms var(--ease-out-quart), box-shadow 180ms var(--ease-out-quart);
}

.knowledge-search:focus-within {
  border-color: var(--accent);
  box-shadow: var(--shadow-card-hover);
}

.knowledge-search svg {
  color: var(--ink-muted);
  flex-shrink: 0;
}

.knowledge-search input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: var(--ink);
  font-family: inherit;
  font-size: 1rem;
}

.knowledge-search input::placeholder {
  color: var(--ink-muted);
}

.knowledge-search button {
  min-height: 44px;
  min-width: 72px;
  padding: 0 18px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--ink);
  color: var(--canvas);
  font-family: inherit;
  font-size: 0.9rem;
  font-weight: 650;
  cursor: pointer;
  box-shadow: var(--shadow-button);
  transition: transform 180ms var(--ease-out-quart), opacity 180ms ease;
}

.knowledge-search button:hover {
  transform: translateY(-1px);
  opacity: 0.92;
}

.knowledge-shortcuts {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 12px 22px;
  flex-wrap: wrap;
}

.knowledge-shortcuts a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-soft);
  font-size: 0.85rem;
  text-decoration: none;
  transition: color 140ms ease;
}

.knowledge-shortcuts a:hover {
  color: var(--accent);
}

.knowledge-shortcuts a span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--ink) 8%, transparent);
  font-size: 0.72rem;
  font-weight: 700;
}

.knowledge-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
  margin-top: 14px;
}

.knowledge-recent,
.knowledge-spaces {
  background: var(--soft-glass-strong);
  border: 1px solid var(--soft-glass-border);
  border-radius: 28px;
  box-shadow: var(--soft-glass-shadow);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.knowledge-recent {
  padding: 28px 24px 18px;
}

.knowledge-spaces {
  padding: 24px 20px;
}

.knowledge-section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.knowledge-section-heading h2 {
  font-size: 1.15rem;
  font-weight: 750;
  color: var(--ink);
  margin: 0 0 4px;
}

.knowledge-section-heading p {
  font-size: 0.78rem;
  color: var(--ink-muted);
  margin: 0;
}

.knowledge-section-heading a {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 0.8rem;
  color: var(--ink-muted);
  text-decoration: none;
  transition: color 140ms ease;
}

.knowledge-section-heading a:hover {
  color: var(--accent);
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.knowledge-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 18px;
  text-decoration: none;
  color: inherit;
  transition: background 140ms ease, transform 140ms var(--ease-out-quart);
}

.knowledge-row:hover {
  background: rgba(255, 255, 255, 0.75);
  transform: translateX(3px);
}

.knowledge-file-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: color-mix(in srgb, var(--accent-sky) 16%, var(--canvas));
  color: var(--accent-sky);
}

.knowledge-row-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.knowledge-row-main strong {
  font-size: 0.92rem;
  font-weight: 650;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.knowledge-row-main small {
  font-size: 0.76rem;
  color: var(--ink-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.knowledge-row-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.75rem;
  color: var(--ink-muted);
}

.knowledge-category {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--ink) 6%, transparent);
}

.row-arrow {
  color: var(--ink-muted);
  opacity: 0.6;
}

.knowledge-empty,
.spaces-empty {
  padding: 32px 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.knowledge-empty h3 {
  font-size: 1rem;
  margin: 0;
  color: var(--ink);
}

.knowledge-empty p,
.spaces-empty p {
  font-size: 0.82rem;
  color: var(--ink-muted);
  margin: 0;
}

.knowledge-empty a,
.spaces-empty a {
  margin-top: 8px;
  font-size: 0.82rem;
  color: var(--accent);
  text-decoration: none;
  font-weight: 600;
}

.knowledge-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.space-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.space-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 14px;
  text-decoration: none;
  color: var(--ink);
  font-size: 0.86rem;
  transition: background 140ms ease;
}

.space-row:hover {
  background: rgba(255, 255, 255, 0.75);
}

.space-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent-sky);
  margin-right: 10px;
}

.space-row small {
  color: var(--ink-muted);
  font-size: 0.76rem;
  font-weight: 600;
}

.rediscover-card {
  padding: 20px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(232, 93, 42, 0.08), rgba(130, 155, 209, 0.12));
  border: 1px solid var(--soft-glass-border);
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-decoration: none;
  color: inherit;
  box-shadow: var(--shadow-sm);
  transition: transform 180ms var(--ease-out-quart);
}

.rediscover-card:hover {
  transform: translateY(-2px);
}

.rediscover-label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--accent);
  text-transform: uppercase;
}

.rediscover-card strong {
  font-size: 0.92rem;
  color: var(--ink);
}

.rediscover-card p {
  font-size: 0.76rem;
  color: var(--ink-soft);
  margin: 0;
  line-height: 1.4;
}

.rediscover-link {
  margin-top: 6px;
  font-size: 0.76rem;
  color: var(--accent);
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.library-summary {
  display: flex;
  justify-content: space-around;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.4);
  font-size: 0.78rem;
  color: var(--ink-soft);
}

.library-summary strong {
  color: var(--ink);
}

/* ── Responsive ── */
@media (max-width: 1024px) {
  .knowledge-dashboard {
    grid-template-columns: minmax(0, 1fr) 240px;
    gap: 28px;
  }
}

@media (max-width: 600px) {
  .knowledge-home {
    width: min(100% - 28px, 1080px);
    padding: 96px 0 40px;
  }

  .knowledge-home::before {
    inset-inline: -14px;
    height: 440px;
    background-size: 20px 20px;
  }

  .knowledge-hero {
    min-height: auto;
    padding-top: 32px;
  }

  .knowledge-intro {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: 12px;
  }

  .knowledge-intro h1 {
    font-size: 2.35rem;
  }

  .knowledge-intro p:last-child {
    max-width: 28ch;
    font-size: 0.86rem;
  }

  .knowledge-create {
    width: 44px;
    padding: 0;
    justify-content: center;
    font-size: 0;
  }

  .knowledge-constellation {
    width: 92px;
    grid-column: 2;
    grid-row: 1;
    transform: none;
  }

  .knowledge-constellation > span {
    display: none;
  }

  .knowledge-search {
    width: 100%;
    min-height: 58px;
    padding-left: 16px;
  }

  .knowledge-search button {
    min-width: 58px;
    min-height: 42px;
  }

  .knowledge-shortcuts {
    width: 100%;
    flex-wrap: wrap;
    gap: 12px 18px;
  }

  .knowledge-shortcuts .ask-knowledge {
    flex: 1;
    justify-content: center;
  }

  .knowledge-shortcuts .knowledge-create {
    width: auto;
    flex: 1;
    justify-content: center;
    font-size: 0.8rem;
  }

  .knowledge-dashboard {
    grid-template-columns: 1fr;
    gap: 42px;
  }

  .knowledge-recent,
  .knowledge-spaces {
    border-radius: 22px;
  }

  .knowledge-recent {
    padding: 22px 18px 12px;
  }

  .knowledge-spaces {
    padding: 22px 18px;
  }

  .knowledge-row {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .knowledge-row-meta {
    display: none;
  }

  .knowledge-empty {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .knowledge-empty a {
    width: 100%;
    margin-left: 54px;
  }
}
</style>
