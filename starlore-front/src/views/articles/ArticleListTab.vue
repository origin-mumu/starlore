<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search,
  Database,
  Edit3,
  Trash2,
  MoreHorizontal,
} from '@lucide/vue'
import DummyCard from '@/components/dummyCard.vue'
import SideBar from '@/components/sideBar.vue'
import { useUserStore } from '@/stores/user'

export interface Article {
  id: number
  title: string
  description: string
  category: string
  tags: string[]
  cover_image: string
  view_count: number
  status: 'published' | 'draft'
  createdAt: string
  updatedAt: string
}

export interface Category {
  id: number
  name: string
  article_count: number
}

interface Props {
  articles: Article[]
  filteredArticles: Article[]
  categories: Category[]
  activeCategory: string
  articleSearch: string
  totalArticles: number
  totalPages: number
  currentPage: number
  isLoading: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:articleSearch', val: string): void
  (e: 'search'): void
  (e: 'filterCategory', cat: string): void
  (e: 'openChunkDrawer', payload: { title: string; articleId: number }): void
  (e: 'deleteArticle', id: number): void
  (e: 'pageChange', page: number): void
}>()

const router = useRouter()
const userStore = useUserStore()

const hasArticles = computed(() => props.filteredArticles.length > 0)

function onSearchInput(e: Event) {
  emit('update:articleSearch', (e.target as HTMLInputElement).value)
}

const activeMoreId = ref<number | null>(null)

function toggleMore(id: number) {
  activeMoreId.value = activeMoreId.value === id ? null : id
}

function closeMore() {
  activeMoreId.value = null
}

onMounted(() => {
  window.addEventListener('click', closeMore)
})

onUnmounted(() => {
  window.removeEventListener('click', closeMore)
})

function handleDelete(id: number, e: MouseEvent) {
  e.stopPropagation()
  emit('deleteArticle', id)
}
</script>

<template>
  <div class="content-layout">
    <main class="post-list">
      <!-- 分类 Filter -->
      <div class="list-toolbar">
        <form class="article-search" role="search" @submit.prevent="emit('search')">
          <Search :size="16" aria-hidden="true" />
          <input
            :value="articleSearch"
            type="search"
            placeholder="搜索知识"
            aria-label="搜索知识"
            @input="onSearchInput"
          />
          <button type="submit">搜索</button>
        </form>
        <div class="filter-bar" aria-label="按星域筛选">
          <button
            class="filter-pill"
            :class="{ active: activeCategory === '全部' }"
            :aria-pressed="activeCategory === '全部'"
            @click="emit('filterCategory', '全部')"
          >
            全部
          </button>
          <button
            v-for="cat in categories"
            :key="cat.id"
            class="filter-pill"
            :class="{ active: activeCategory === cat.name }"
            :aria-pressed="activeCategory === cat.name"
            @click="emit('filterCategory', cat.name)"
          >
            {{ cat.name }}
          </button>
        </div>
        <span class="article-count">{{ totalArticles }} 篇星记</span>
      </div>

      <!-- 文章列表 Card Grid -->
      <div class="article-items">
        <div v-if="isLoading" class="loading-state ink-glass-card">
          <span class="loading-spinner"></span>
          <p class="loading-text">正在加载星记...</p>
        </div>

        <div
          v-else-if="hasArticles"
          class="card-wrapper fade-in-up"
          v-for="(article, index) in filteredArticles"
          :key="`${activeCategory}-${currentPage}-${article.id}`"
          :style="{ animationDelay: `${index * 80}ms` }"
        >
          <div class="card">
            <DummyCard v-bind="article" />

            <!-- 知识库向量 Badge & 调优按钮 -->
            <div class="article-chunk-bar">
              <span class="chunk-badge">
                <Database :size="13" /> RAG 知识索引
              </span>
              <div class="article-actions">
                <button
                  class="btn-chunk-drawer"
                  @click.stop="emit('openChunkDrawer', { title: article.title, articleId: article.id })"
                >
                  查看切片
                </button>
                <div v-if="userStore.isLoggedIn" class="card-more-wrap">
                  <button
                    class="card-icon-btn"
                    :class="{ active: activeMoreId === article.id }"
                    @click.stop="toggleMore(article.id)"
                    title="更多操作"
                    aria-label="更多操作"
                  >
                    <MoreHorizontal :size="16" />
                  </button>
                  <transition name="dropdown-fade">
                    <div v-if="activeMoreId === article.id" class="card-dropdown">
                      <button
                        class="dropdown-item"
                        @click.stop="closeMore(); router.push(`/articles/edit/${article.id}`)"
                      >
                        <Edit3 :size="14" />
                        <span>编辑星记</span>
                      </button>
                      <button
                        class="dropdown-item danger"
                        @click="closeMore(); handleDelete(article.id, $event)"
                      >
                        <Trash2 :size="14" />
                        <span>删除星记</span>
                      </button>
                    </div>
                  </transition>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-state ink-glass-card">
          <p class="empty-title">{{ activeCategory === '全部' ? '还没有星记' : '当前星域暂无星记' }}</p>
          <p class="empty-desc">开始写一篇星记，与大家分享你的想法吧</p>
        </div>
      </div>

      <!-- 分页控制器 -->
      <div v-if="!isLoading && hasArticles && totalPages > 1" class="pagination">
        <button
          class="page-btn"
          :disabled="currentPage === 1"
          @click="emit('pageChange', currentPage - 1)"
          aria-label="上一页"
        >
          &lt;
        </button>
        <button
          v-for="p in totalPages"
          :key="p"
          class="page-btn"
          :class="{ active: currentPage === p }"
          @click="emit('pageChange', p)"
        >
          {{ p }}
        </button>
        <button
          class="page-btn"
          :disabled="currentPage === totalPages"
          @click="emit('pageChange', currentPage + 1)"
          aria-label="下一页"
        >
          &gt;
        </button>
      </div>
    </main>

    <SideBar />
  </div>
</template>

<style scoped>
.content-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 32px;
  align-items: start;
}

@media (max-width: 900px) {
  .content-layout {
    grid-template-columns: 1fr;
  }
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.list-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  justify-content: space-between;
}

.article-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  padding: 4px 6px 4px 14px;
  box-shadow: var(--shadow-sm);
  min-width: 240px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.article-search:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.article-search input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: var(--ink);
  flex: 1;
  min-width: 0;
}

.article-search button {
  flex-shrink: 0;
  white-space: nowrap;
  background: var(--ink);
  color: var(--canvas);
  border: none;
  border-radius: var(--radius-full);
  padding: 5px 14px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.article-search button:hover {
  opacity: 0.9;
}

.filter-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 4px 0;
}

.filter-pill {
  padding: 5px 14px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.filter-pill:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.filter-pill.active {
  background: var(--ink);
  color: var(--canvas);
  border-color: var(--ink);
  font-weight: 600;
}

.article-count {
  font-size: 12px;
  color: var(--ink-muted);
  font-family: 'Fira Code', monospace;
}

.article-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.card-wrapper {
  animation: fadeInUp 0.4s ease forwards;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.card :deep(.article-card) {
  flex: 1;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  transform: none;
}

.card :deep(.article-card:hover) {
  background: transparent;
  transform: none;
}

.card:hover {
  transform: translateY(-2px);
  border-color: var(--border-interactive);
  box-shadow: var(--shadow-md);
}

.article-chunk-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  border-top: 1px solid var(--border);
}

.chunk-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--accent);
  font-weight: 500;
}

.article-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-chunk-drawer {
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  color: var(--ink-soft);
  cursor: pointer;
}
.btn-chunk-drawer:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.card-icon-btn {
  background: none;
  border: none;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 3px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}
.card-icon-btn:hover,
.card-icon-btn.active {
  color: var(--ink);
  background: var(--hover-bg);
}
.card-icon-btn.danger:hover {
  color: #ef4444;
}

.card-more-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.card-dropdown {
  position: absolute;
  bottom: calc(100% + 6px);
  right: 0;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 8px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 4px;
  min-width: 104px;
  z-index: 50;
  display: flex;
  flex-direction: column;
  gap: 2px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 10px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm, 4px);
  color: var(--ink-soft);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.dropdown-item:hover {
  background: var(--hover-bg, rgba(0, 0, 0, 0.04));
  color: var(--ink);
}

.dropdown-item.danger {
  color: #ef4444;
}

.dropdown-item.danger:hover {
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink);
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.page-btn:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}
.page-btn.active {
  background: var(--ink);
  color: var(--canvas);
  border-color: var(--ink);
  font-weight: 600;
}
.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.loading-state, .empty-state {
  grid-column: 1 / -1;
  padding: 48px 24px;
  text-align: center;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

.loading-spinner {
  display: inline-block;
  width: 24px;
  height: 24px;
  border: 2px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}
@keyframes spin { to { transform: rotate(360deg); } }

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 4px;
}
.empty-desc {
  font-size: 13px;
  color: var(--ink-muted);
}
</style>
