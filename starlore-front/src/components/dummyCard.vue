<script setup lang="ts">
import router, { preloadArticleDetail } from '@/router'
import { Article } from '@/type/Article'
import { useUserStore } from '@/stores/user'
import { Lock, Unlock } from '@lucide/vue'

const props = defineProps<Article>()
const userStore = useUserStore()

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <div
    class="article-card starlore-spotlight"
    role="link"
    tabindex="0"
    @pointerenter="preloadArticleDetail"
    @focus="preloadArticleDetail"
    @click="router.push({ name: 'articleDetail', params: { id: props.id } })"
    @keydown.enter="router.push({ name: 'articleDetail', params: { id: props.id } })"
  >
    <!-- 封面图 -->
    <div v-if="props.cover_image" class="card-cover">
      <img
        :src="props.cover_image"
        :alt="props.title"
        class="cover-img"
        loading="lazy"
      />
    </div>
    <div class="card-body">
      <div class="tags-row">
        <!-- 可见性标识：仅登录后在管理界面展示 -->
        <span v-if="userStore.isLoggedIn" class="visibility-badge" :class="props.is_public ? 'public-badge' : 'private-badge'">
          <Unlock v-if="props.is_public" :size="12" />
          <Lock v-else :size="12" />
          {{ props.is_public ? '公开' : '私密' }}
        </span>
        <span v-for="tag in props.tags?.slice(0, 3)" :key="tag" class="card-tag">{{ tag }}</span>
      </div>
      <h3 class="card-title">{{ props.title }}</h3>
      <p class="card-desc">{{ props.description }}</p>
      <div class="card-meta">
        <span class="meta-date">{{ formatDate(props.createdAt) }}</span>
        <span class="meta-cat">{{ props.category }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.article-card {
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  transition: background var(--transition);
}

.article-card:hover {
  background: color-mix(in oklch, var(--glass-bg) 88%, var(--accent-soft));
  transform: translateY(-2px);
  border-color: var(--border-interactive);
  box-shadow: var(--shadow-card-hover);
}

/* ── 封面图 ── */
.card-cover {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: var(--canvas-deep);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s var(--ease-out-quart);
}

.article-card:hover .cover-img {
  transform: scale(1.05);
}

.card-body {
  padding: 22px 20px 18px;
  display: flex;
  flex-direction: column;
  min-height: 172px;
}

.tags-row {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.card-tag {
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  background: var(--tag-bg);
  border: 1px solid var(--badge-border);
  color: var(--ink-soft);
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: all var(--transition);
}

.card-title {
  font-family: var(--font-ui, Inter, "Noto Sans SC", system-ui, sans-serif);
  font-size: 1.12rem;
  color: var(--ink);
  letter-spacing: -0.02em;
  line-height: 1.4;
  margin-bottom: 10px;
  transition: color var(--transition);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-card:hover .card-title {
  color: var(--accent);
}

.card-desc {
  max-width: 70ch;
  font-size: 0.86rem;
  color: var(--ink-soft);
  line-height: 1.7;
  margin-bottom: 16px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 0;
  font-size: 0.78rem;
  color: var(--ink-muted);
}

.meta-cat {
  color: var(--accent);
  font-weight: 600;
}

@media (max-width: 768px) {
  .card-body {
    padding: 20px;
  }
}

/* ── 可见性徽章样式 ── */
.visibility-badge {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  gap: 3px;
  letter-spacing: 0.02em;
}

.public-badge {
  background: color-mix(in oklch, var(--accent-soft) 58%, transparent);
  color: var(--accent);
  border: 1px solid color-mix(in oklch, var(--accent) 16%, transparent);
}

.private-badge {
  background: color-mix(in oklch, var(--accent-soft) 82%, var(--surface));
  color: var(--accent);
  border: 1px solid color-mix(in oklch, var(--accent) 24%, transparent);
}
</style>
