<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import {
  Database,
  ChevronDown,
  Search,
  RefreshCw,
  Eye,
} from '@lucide/vue'
import type { ArticleChunkItem } from '@/api/ai'

interface Props {
  dashboardChunks: ArticleChunkItem[]
  dashboardChunksLoading: boolean
  chunkCategories: string[]
  chunkArticles: { id: number; title: string; category?: string }[]
  chunkCategory: string
  chunkArticleId: number | null
  chunkGlobalSearch: string
  chunkTotal: number
  chunkCurrentPage: number
  chunkTotalPages: number
  reindexLoading: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:chunkCategory', cat: string): void
  (e: 'update:chunkArticleId', id: number | null): void
  (e: 'update:chunkGlobalSearch', q: string): void
  (e: 'selectCategory', cat: string): void
  (e: 'selectArticle', id: number | null): void
  (e: 'reindex'): void
  (e: 'toggleChunk', chunk: ArticleChunkItem): void
  (e: 'openDetail', chunk: ArticleChunkItem): void
  (e: 'pageChange', page: number): void
}>()

const chunkCategoryOpen = ref(false)
const chunkArticleOpen = ref(false)

const availableChunkArticles = computed(() => {
  if (!props.chunkCategory) return props.chunkArticles
  return props.chunkArticles.filter(art => art.category === props.chunkCategory)
})

const selectedArticleTitle = computed(() => {
  if (!props.chunkArticleId) return ''
  const item = props.chunkArticles.find(art => art.id === props.chunkArticleId)
  return item?.title || ''
})

const chunkPaginationPages = computed(() => {
  const visible = 7
  let start = Math.max(1, props.chunkCurrentPage - Math.floor(visible / 2))
  const end = Math.min(props.chunkTotalPages, start + visible - 1)
  if (end - start + 1 < visible) {
    start = Math.max(1, end - visible + 1)
  }
  const pages: number[] = []
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

function onSearchInput(e: Event) {
  emit('update:chunkGlobalSearch', (e.target as HTMLInputElement).value)
}

function handleOutsideClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.chunk-category-dropdown')) {
    chunkCategoryOpen.value = false
    chunkArticleOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('click', handleOutsideClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleOutsideClick)
})
</script>

<template>
  <div class="chunks-view-layout ink-glass-card">
    <div class="chunks-top-header">
      <div>
        <h3><Database :size="18" style="vertical-align: -3px;" /> 全站向量切片看板</h3>
        <p class="chunks-summary">
          共 {{ chunkTotal }} 个切片，第 {{ chunkCurrentPage }} / {{ chunkTotalPages }} 页
        </p>
      </div>
      <div class="top-actions">
        <!-- 分类筛选 (Category Select) -->
        <el-select
          :model-value="chunkCategory || ''"
          placeholder="全部星域"
          size="default"
          style="width: 140px"
          @update:model-value="(val: any) => emit('selectCategory', String(val ?? ''))"
        >
          <el-option label="全部星域" value="" />
          <el-option v-for="cat in chunkCategories" :key="cat" :label="cat" :value="cat" />
        </el-select>

        <!-- 文章筛选 (Article Select) -->
        <el-select
          :model-value="chunkArticleId ?? ''"
          placeholder="全部文章"
          size="default"
          style="width: 180px"
          @update:model-value="(val: any) => emit('selectArticle', val === '' || val === null ? null : Number(val))"
        >
          <el-option label="全部文章" :value="''" />
          <el-option
            v-for="art in availableChunkArticles"
            :key="art.id"
            :label="art.title"
            :value="art.id"
          />
        </el-select>

        <div class="search-box">
          <Search :size="14" class="search-icon" />
          <input :value="chunkGlobalSearch" placeholder="搜索标题或切片内容" @input="onSearchInput" />
        </div>
        <button class="btn-primary" :disabled="reindexLoading" @click="emit('reindex')">
          <RefreshCw :size="13" :class="{ spinning: reindexLoading }" /> {{ reindexLoading ? '正在重建向量库...' : '重建全站向量库' }}
        </button>
      </div>
    </div>

    <div v-if="dashboardChunksLoading" class="loading-state">
      <p class="loading-text">正在读取真实切片数据...</p>
    </div>

    <div v-else-if="dashboardChunks.length" class="chunk-cards-grid">
      <div
        v-for="chunk in dashboardChunks"
        :key="chunk.id"
        class="chunk-card clickable-chunk-card"
        @click="emit('openDetail', chunk)"
      >
        <div class="chunk-card-head">
          <span class="chunk-idx">Chunk #{{ Number(chunk.chunkIndex) + 1 }}</span>
          <span class="chunk-tokens">约 {{ chunk.tokenCount }} Tokens</span>
          <button
            class="btn-status-toggle"
            :class="{ disabled: chunk.isEnabled === 0 }"
            @click.stop="emit('toggleChunk', chunk)"
          >
            {{ chunk.isEnabled === 1 ? '已启用' : '已禁用' }}
          </button>
        </div>
        <p v-if="chunk.articleTitle" class="chunk-source">{{ chunk.articleTitle }}</p>
        <p class="chunk-text">{{ chunk.content }}</p>
        <div class="chunk-card-footer">
          <span class="view-detail-hint"><Eye :size="12" />查看切片</span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <p class="empty-title">{{ chunkGlobalSearch ? '没有匹配的切片' : '暂无知识切片' }}</p>
      <p class="empty-desc">
        {{ chunkGlobalSearch ? '换个关键词再试试。' : '创建含有正文的星记后，切片会在这里展示。' }}
      </p>
    </div>

    <div v-if="!dashboardChunksLoading && chunkTotalPages > 1" class="pagination chunk-pagination">
      <button
        class="page-btn"
        :disabled="chunkCurrentPage === 1"
        aria-label="上一页切片"
        @click="emit('pageChange', chunkCurrentPage - 1)"
      >
        &lt;
      </button>
      <button
        v-for="page in chunkPaginationPages"
        :key="page"
        class="page-btn"
        :class="{ active: chunkCurrentPage === page }"
        @click="emit('pageChange', page)"
      >
        {{ page }}
      </button>
      <button
        class="page-btn"
        :disabled="chunkCurrentPage === chunkTotalPages"
        aria-label="下一页切片"
        @click="emit('pageChange', chunkCurrentPage + 1)"
      >
        &gt;
      </button>
    </div>
  </div>
</template>

<style scoped>
.chunks-view-layout {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.chunks-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}

.chunks-top-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  margin: 0;
}

.chunks-summary {
  font-size: 12px;
  color: var(--ink-muted);
  margin: 4px 0 0;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.chunk-category-dropdown {
  position: relative;
}

.chunk-category-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: var(--surface);
  font-size: 12px;
  color: var(--ink);
  cursor: pointer;
  max-width: 160px;
}

.article-trigger-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.arrow-icon {
  transition: transform 0.2s;
}
.arrow-icon.is-open {
  transform: rotate(180deg);
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.025);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  padding: 0 14px;
  height: 38px;
  box-sizing: border-box;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.search-box:focus-within {
  background: #ffffff;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

[data-theme="dark"] .search-box {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .search-box:focus-within {
  background: rgba(255, 255, 255, 0.08);
}

.search-box .search-icon {
  color: var(--ink-muted);
  flex-shrink: 0;
}

.search-box input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: var(--ink);
  width: 170px;
  font-family: inherit;
}

.search-box input::placeholder {
  color: var(--ink-muted);
}

.btn-primary {
  background: var(--ink);
  color: var(--canvas);
  border: none;
  border-radius: 12px;
  padding: 0 18px;
  height: 38px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.btn-primary:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.chunk-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.chunk-card {
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.chunk-card:hover {
  border-color: var(--border-interactive);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.chunk-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chunk-idx {
  font-size: 11px;
  font-family: 'Fira Code', monospace;
  font-weight: 700;
  color: var(--accent);
}
.chunk-tokens {
  font-size: 10px;
  color: var(--ink-muted);
}

.btn-status-toggle {
  border: 1px solid rgba(40, 200, 64, 0.4);
  background: rgba(40, 200, 64, 0.1);
  color: #28c840;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  cursor: pointer;
}
.btn-status-toggle.disabled {
  border-color: var(--border);
  background: var(--surface);
  color: var(--ink-muted);
}

.chunk-source {
  font-size: 11px;
  font-weight: 600;
  color: var(--ink-soft);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chunk-text {
  font-size: 12px;
  color: var(--ink);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

.chunk-card-footer {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
}
.view-detail-hint {
  font-size: 11px;
  color: var(--accent);
  display: flex;
  align-items: center;
  gap: 3px;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
}
.page-btn {
  min-width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--surface);
  font-size: 13px;
  cursor: pointer;
}
.page-btn.active {
  background: var(--ink);
  color: var(--canvas);
  border-color: var(--ink);
}
.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.loading-state, .empty-state {
  padding: 48px 16px;
  text-align: center;
  color: var(--ink-muted);
}
</style>
