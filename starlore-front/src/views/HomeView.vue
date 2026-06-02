<script lang="ts" setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { getBlogStatsService } from '@/api/article'
import SideBar from '@/components/sideBar.vue'
import { Article } from '@/type/Article'
import DummyCard from '@/components/dummyCard.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const demoArticles: Article[] = [
  { id: -1, title: '欢迎来到 Starlore', summary: '这是一个演示星迹，登录后可以查看真实内容', category: '演示', createdAt: new Date().toISOString(), coverImage: '' } as any,
  { id: -2, title: '探索知识星域', summary: '在 VR 星图中浏览你的知识版图', category: '演示', createdAt: new Date().toISOString(), coverImage: '' } as any,
  { id: -3, title: 'AI 创意发散', summary: '用 AI 帮你拓展思维边界', category: '演示', createdAt: new Date().toISOString(), coverImage: '' } as any,
]

type HeroSlide = {
  id: number
  title: string
  image: string
}

const data = ref()
const articles = ref<Article[]>()

const activeIndex = ref(0)
let autoplayTimer: ReturnType<typeof setInterval> | null = null

const stopAutoplay = () => {
  if (autoplayTimer) {
    clearInterval(autoplayTimer)
    autoplayTimer = null
  }
}

const goToSlide = (index: number) => {
  activeIndex.value = index
  stopAutoplay()
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    articles.value = demoArticles
    return
  }
  try {
    const res = await getBlogStatsService()
    data.value = res.data
    articles.value = res.data?.popularArticles ?? []
  } catch (err) {
    console.error('获取博客统计失败:', err)
    articles.value = []
  }
  // startAutoplay()
})

onBeforeUnmount(() => stopAutoplay())
</script>

<template>
  <div class="page-container">
    <!-- Guest Banner -->
    <div v-if="!userStore.isLoggedIn" class="guest-banner fade-in-up">
      <span>你正在以访客模式浏览，</span>
      <router-link to="/login">登录</router-link>
      <span>后解锁完整功能</span>
    </div>

    <!-- Hero Section -->
    <section class="hero-section">
      <div class="container">
        <div class="hero-content fade-in-up">
          <span class="hero-kicker">PERSONAL STARLORE</span>
          <h1 class="hero-title">记录创造的<br />每一刻</h1>
          <p class="hero-desc">代码、设计、思考。在这里分享我的学习旅程和项目实践。</p>
          <div class="hero-actions">
            <router-link v-if="userStore.isLoggedIn" to="/articles" class="btn-primary">阅读星迹</router-link>
            <router-link v-else to="/login" class="btn-primary">登录探索</router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- Articles Section -->
    <section class="section-parchment">
      <div class="container">
        <div class="section-header fade-in-up" style="animation-delay: 0.25s">
          <h2 class="section-heading">最新星迹</h2>
          <router-link to="/articles" class="see-all">查看全部 <span>→</span></router-link>
        </div>
        <div class="content-layout">
          <main class="articles-grid">
            <div
              v-if="articles && articles.length > 0"
              v-for="(article, index) in articles"
              :key="article.id"
              class="fade-in-up"
              :style="{ animationDelay: `${0.3 + index * 0.08}s` }"
            >
              <DummyCard v-bind="article" />
            </div>
            <div v-else class="empty-state fade-in-up" style="animation-delay: 0.3s">
              <p class="empty-title">还没有星迹</p>
              <p class="empty-desc">开始写你的第一篇星迹吧</p>
              <router-link to="/articles/edit" class="btn-primary">写星迹</router-link>
            </div>
          </main>
          <SideBar />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.guest-banner {
  padding: 10px 20px;
  margin-top: 80px;
  text-align: center;
  font-size: 0.88rem;
  color: var(--ink-soft);
  background: var(--accent-soft);
  border-radius: var(--radius-lg);
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}
.guest-banner a {
  color: var(--accent);
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.hero-section {
  padding: 140px 0 64px;
}

.hero-content {
  max-width: 600px;
}

.hero-kicker {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent);
  margin-bottom: 20px;
}

.hero-title {
  font-size: clamp(2.4rem, 5vw, 3.6rem);
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.025em;
  color: var(--ink);
  margin-bottom: 20px;
}

.hero-desc {
  font-size: 1.1rem;
  font-weight: 400;
  line-height: 1.8;
  color: var(--ink-soft);
  margin-bottom: 36px;
  max-width: 480px;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  padding: 32px 24px;
  text-align: center;
  background: var(--surface-card, #fff);
  border-radius: var(--radius-xl, 16px);
  box-shadow: var(--shadow-card, none);
}
.empty-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: var(--ink);
}
.empty-desc {
  margin: 8px 0 16px;
  color: var(--ink-muted);
  font-size: 15px;
}
.hero-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

/* Carousel */
.carousel-section {
  padding: 0 0 56px;
}

.carousel-wrapper {
  max-width: 1080px;
  margin: 0 auto;
}

.carousel {
  position: relative;
  border-radius: var(--radius-xl);
  overflow: hidden;
  aspect-ratio: 21 / 9;
  box-shadow: var(--shadow-card);
}

.carousel-track {
  position: relative;
  width: 100%;
  height: 100%;
}

.carousel-slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 1s ease;
}

.carousel-slide.active {
  opacity: 1;
  z-index: 1;
}

.carousel-slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slide-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, oklch(0.15 0.02 50 / 0.6) 0%, transparent 50%);
  display: flex;
  align-items: flex-end;
  padding: 32px;
}

.slide-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #fdfbf5;
  letter-spacing: -0.02em;
}

.carousel-dots {
  position: absolute;
  bottom: 20px;
  right: 32px;
  z-index: 2;
  display: flex;
  gap: 8px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: oklch(1 0 0 / 0.4);
  border: none;
  padding: 0;
  cursor: pointer;
  transition: all 0.3s;
}

.dot.active {
  background: #fdfbf5;
  transform: scale(1.3);
}

/* Section Header */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.see-all {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--accent);
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all var(--transition);
}

.see-all:hover {
  gap: 8px;
}

.see-all span {
  transition: transform var(--transition);
}

.see-all:hover span {
  transform: translateX(3px);
}

/* Content Layout */
.content-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 32px;
  align-items: flex-start;
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

@media (max-width: 1024px) {
  .hero-content {
    max-width: 100%;
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .articles-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .hero-section {
    padding: 110px 0 40px;
  }

  .hero-title {
    font-size: 1.8rem;
  }

  .carousel {
    aspect-ratio: 16 / 9;
    border-radius: var(--radius-lg);
  }
}
</style>
