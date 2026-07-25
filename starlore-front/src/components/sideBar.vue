<script lang="ts" setup>
import { ref, onMounted, computed } from 'vue'
import { getBlogStatsService, getPublicBlogStatsService } from '@/api/article'
import { useUserStore } from '@/stores/user'
import defaultAvatar from '@/assets/avatar.jpg'

const userStore = useUserStore()

const data = ref()
interface Categories {
  id: number
  name: string
  article_count: number
}
const categories = ref<Categories[]>()
const totalArticles = ref(0)
const totalCategories = ref(0)

const avatarSrc = computed(() => {
  return userStore.user?.avatar || defaultAvatar
})
const displayName = computed(() => {
  if (!userStore.isLoggedIn) return '访客'
  return userStore.user?.nickname || userStore.user?.username || '访客'
})

onMounted(async () => {
  try {
    const res = userStore.isLoggedIn
      ? await getBlogStatsService()
      : await getPublicBlogStatsService()
    // 后端返回 { data: BlogStatsData } 嵌套结构，解一层
    const stats = res.data?.data ?? res.data
    data.value = stats
    categories.value = stats?.popularCategories ?? []
    totalArticles.value = stats?.totalArticles ?? 0
    totalCategories.value = stats?.totalCategories ?? 0
  } catch (err) {
    console.error('获取侧边栏数据失败:', err)
  }
})
</script>
<template>
  <aside class="sidebar">
    <div class="widget-card profile-card fade-in-up" style="animation-delay: 0.35s">
      <img class="avatar" :src="avatarSrc" alt="头像" />
      <h2 class="profile-name">{{ displayName }}</h2>
      <p v-if="userStore.user?.bio" class="profile-quote">{{ userStore.user.bio }}</p>
      <div class="stats">
        <div class="stat-item">
          <span class="stat-num">{{ totalArticles }}</span>
          <span class="stat-label">星记</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ totalCategories }}</span>
          <span class="stat-label">星域</span>
        </div>
      </div>
    </div>

    <div class="widget-card fade-in-up" style="animation-delay: 0.4s">
      <h4 class="widget-title">星域</h4>
      <div v-if="categories && categories.length > 0" class="tags-cloud">
        <router-link
          class="tag-pill"
          v-for="category in categories"
          :key="category.name"
          :to="`/articles?category=${encodeURIComponent(category.name)}`"
        >
          {{ category.name }}
        </router-link>
      </div>
      <div v-else class="empty-tags">
        <p class="empty-tags-text">还没有星域</p>
        <router-link to="/categories" class="write-link">添加星域 →</router-link>
      </div>
    </div>
  </aside>
</template>
<style scoped>
.sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: 80px;
}

.widget-card {
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  transition: all var(--transition);
}

.widget-card:hover {
  box-shadow: var(--shadow-card-hover);
}

.profile-card {
  text-align: center;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  margin: 0 auto 12px;
  border: 2px solid var(--border);
}

.profile-name {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 4px;
}

.profile-quote {
  font-size: 0.85rem;
  color: var(--ink-muted);
  line-height: 1.6;
  margin-bottom: 16px;
  font-style: italic;
}

.stats {
  display: flex;
  justify-content: center;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--accent);
}

.stat-label {
  font-size: 0.78rem;
  color: var(--ink-muted);
  margin-top: 2px;
}

.widget-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--ink-soft);
  letter-spacing: 0.04em;
  margin-bottom: 14px;
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-pill {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.8);
  color: var(--ink-soft);
  padding: 5px 12px;
  border-radius: var(--radius-sm);
  font-size: 0.78rem;
  font-weight: 500;
  cursor: pointer;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: all var(--transition);
}

.tag-pill:hover {
  background: rgba(255, 255, 255, 0.95);
  color: var(--accent);
  border-color: var(--border-interactive);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.empty-tags {
  text-align: center;
  padding: 8px 0;
}
.empty-tags-text {
  margin: 0 0 8px;
  color: var(--ink-muted);
  font-size: 0.85rem;
}
.write-link {
  color: var(--accent);
  font-size: 0.85rem;
  font-weight: 500;
  text-decoration: none;
  transition: opacity var(--transition);
}
.write-link:hover {
  opacity: 0.8;
}

@media (max-width: 1024px) {
  .sidebar {
    width: 100%;
    position: static;
  }
}
</style>
