<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import Nav from '@/components/nav.vue'
import router from '@/router'
import {
  getAllArticlesService,
  deleteArticleService,
  toggleArticleStatusService,
  articleCategoryListService,
} from '@/api/article'
import { getUserListService } from '@/api/user'

interface Article {
  id: number
  userId: number
  authorName: string
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

interface Pagination {
  current: number
  total: number
  pages: number
}

const articles = ref<Article[]>([])
const searchKeyword = ref('')
const selectedCategory = ref('all')
const selectedStatus = ref('all')
const selectedAuthor = ref<number | 'all'>('all')
const loading = ref(false)
const categories = ref<string[]>(['all'])
const users = ref<{ id: number; nickname: string }[]>([])
const pagination = ref<Pagination>({
  current: 1,
  total: 0,
  pages: 0,
})

const fetchArticles = async () => {
  try {
    loading.value = true
    const params: any = {
      page: pagination.value.current,
      limit: 10,
    }

    if (searchKeyword.value) {
      params.search = searchKeyword.value
    }
    if (selectedCategory.value !== 'all') {
      params.category = selectedCategory.value
    }
    if (selectedAuthor.value !== 'all') {
      params.authorUserId = selectedAuthor.value
    }

    const response = (await getAllArticlesService(params)) as any

    articles.value = response.data
    pagination.value = response.pagination
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const response = await articleCategoryListService()
    const categoryNames = response.data.map((cat: any) => cat.name || cat.category)
    categories.value = ['all', ...categoryNames]
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

const fetchUsers = async () => {
  try {
    const res: any = await getUserListService()
    users.value = (res.data || []).map((u: any) => ({ id: u.id, nickname: u.nickname || u.username }))
  } catch {
    // 非管理员忽略
  }
}

onMounted(() => {
  fetchUsers()
  fetchCategories()
  fetchArticles()
})

watch([searchKeyword, selectedCategory, selectedStatus, selectedAuthor], () => {
  pagination.value.current = 1
  fetchArticles()
})

const prevPage = () => {
  if (pagination.value.current > 1) {
    pagination.value.current--
    fetchArticles()
  }
}

const nextPage = () => {
  if (pagination.value.current < pagination.value.pages) {
    pagination.value.current++
    fetchArticles()
  }
}

const goToPage = (page: number) => {
  if (page >= 1 && page <= pagination.value.pages) {
    pagination.value.current = page
    fetchArticles()
  }
}

const deleteArticle = async (id: number) => {
  if (confirm('确定要删除这篇星记吗？')) {
    try {
      await deleteArticleService(id)
      fetchArticles()
    } catch (error) {
      console.error('删除文章失败:', error)
      alert('删除失败，请重试')
    }
  }
}

const toggleStatus = async (article: Article) => {
  try {
    const newStatus = article.status === 'published' ? 'draft' : 'published'
    await toggleArticleStatusService(article.id, newStatus)
    article.status = newStatus
  } catch (error) {
    console.error('切换状态失败:', error)
    alert('状态切换失败，请重试')
  }
}

const addNewArticle = () => {
  router.push({ name: 'AddArticle' })
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

const editArticle = (id: number) => {
  router.push({ name: 'EditArticle', params: { id } })
}
</script>

<template>
  <div class="home-layout">
    <div class="sidebar-container">
      <Nav></Nav>
    </div>
    <div class="content-container">
      <div class="content-box">
        <div class="page-header">
          <h1 class="page-title">星记管理</h1>
          <button class="add-btn" @click="addNewArticle">
            <span class="btn-icon">➕</span>
            新建星记
          </button>
        </div>

        <!-- 搜索和筛选区域 -->
        <div class="filter-section">
          <div class="search-box">
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索星记标题或描述..."
              class="search-input"
            />
            <span class="search-icon">🔍</span>
          </div>

          <div class="filter-controls">
            <select v-model="selectedCategory" class="filter-select">
              <option value="all">全部星域</option>
              <option
                v-for="category in categories.filter((c) => c !== 'all')"
                :key="category"
                :value="category"
              >
                {{ category }}
              </option>
            </select>

            <select v-model="selectedStatus" class="filter-select">
              <option value="all">全部状态</option>
              <option value="published">已发布</option>
              <option value="draft">草稿</option>
            </select>

            <select v-model="selectedAuthor" class="filter-select">
              <option value="all">全部观星者</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.nickname }}</option>
            </select>
          </div>
        </div>

        <!-- 文章列表 -->
        <div class="articles-table">
          <div v-if="loading" class="loading">加载中...</div>

          <div v-else-if="articles.length === 0" class="empty-state">
            <div class="empty-icon">📝</div>
            <p>没有找到符合条件的星记</p>
          </div>

          <div v-else class="table-container">
            <div class="table-header">
              <div class="header-cell title-cell">星记标题</div>
              <div class="header-cell">作者</div>
              <div class="header-cell">星域</div>
              <div class="header-cell">光痕</div>
              <div class="header-cell">状态</div>
              <div class="header-cell">创建时间</div>
              <div class="header-cell">浏览量</div>
              <div class="header-cell">操作</div>
            </div>

            <div class="table-body">
              <div v-for="article in articles" :key="article.id" class="table-row">
                <div class="table-cell title-cell">
                  <span class="article-title">{{ article.title }}</span>
                </div>
                <div class="table-cell">
                  <span class="author-name">{{ article.authorName || '-' }}</span>
                </div>
                <div class="table-cell">
                  <span class="category-tag">{{ article.category }}</span>
                </div>
                <div class="table-cell">
                  <span v-if="article.tags && article.tags.length" class="tags-display">
                    <span v-for="tag in article.tags.slice(0, 3)" :key="tag" class="tag-chip">{{ tag }}</span>
                    <span v-if="article.tags.length > 3" class="tag-more">+{{ article.tags.length - 3 }}</span>
                  </span>
                  <span v-else class="no-tags">-</span>
                </div>
                <div class="table-cell">
                  <span :class="['status-badge', article.status]" @click="toggleStatus(article)">
                    {{ article.status == 'published' ? '已发布' : '草稿' }}
                  </span>
                </div>
                <div class="table-cell">{{ formatDate(article.createdAt) }}</div>
                <div class="table-cell">{{ article.view_count }}</div>
                <div class="table-cell">
                  <div class="action-buttons">
                    <button class="action-btn edit-btn" @click="editArticle(article.id)">
                      编辑
                    </button>
                    <button class="action-btn delete-btn" @click="deleteArticle(article.id)">
                      删除
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="pagination.pages > 1" class="pagination">
          <button class="page-btn" :disabled="pagination.current <= 1" @click="prevPage">上一页</button>
          <span class="page-info">第 {{ pagination.current }} / {{ pagination.pages }} 页</span>
          <button class="page-btn" :disabled="pagination.current >= pagination.pages" @click="nextPage">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--border);
}

.page-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
  letter-spacing: -0.02em;
}

.add-btn {
  background: var(--accent);
  color: #FDFBF5;
  border: none;
  padding: 10px 22px;
  border-radius: var(--radius-md);
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: var(--shadow-button);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.add-btn:hover {
  background: var(--accent-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-button-hover);
}

.filter-section {
  display: flex;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
  flex-wrap: wrap;
  align-items: center;
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 250px;
}

.search-input {
  width: 100%;
  padding: 10px 16px 10px 40px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 0.95rem;
  font-family: inherit;
  transition: border-color var(--transition), box-shadow var(--transition);
  background: var(--surface);
  color: var(--ink);
}

.search-input:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px oklch(0.55 0.15 35 / 0.1);
}

.search-input::placeholder {
  color: var(--ink-muted);
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ink-muted);
}

.filter-controls {
  display: flex;
  gap: var(--space-3);
}

.filter-select {
  padding: 10px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-family: inherit;
  background: var(--surface);
  color: var(--ink);
  cursor: pointer;
  transition: border-color var(--transition);
}

.filter-select:focus {
  outline: none;
  border-color: var(--accent);
}

.articles-table {
  margin-bottom: var(--space-6);
}

.loading,
.empty-state {
  text-align: center;
  padding: var(--space-8) var(--space-5);
  color: var(--ink-muted);
  font-size: 1rem;
}

.empty-icon {
  font-size: 2.5rem;
  margin-bottom: var(--space-4);
}

.table-container {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 2fr 0.8fr 0.8fr 1.2fr 0.8fr 1fr 0.8fr 1.5fr;
  background: var(--canvas-deep);
  border-bottom: 1px solid var(--border);
  font-weight: 600;
  color: var(--ink);
  font-size: 0.88rem;
}

.header-cell {
  padding: 14px 16px;
  text-align: left;
}

.table-body {
  background: var(--surface);
}

.table-row {
  display: grid;
  grid-template-columns: 2fr 0.8fr 0.8fr 1.2fr 0.8fr 1fr 0.8fr 1.5fr;
  border-bottom: 1px solid var(--border);
  transition: background-color var(--transition);
}

.table-row:hover {
  background-color: var(--canvas-deep);
}

.table-row:last-child {
  border-bottom: none;
}

.table-cell {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  font-size: 0.9rem;
  color: var(--ink-soft);
}

.title-cell {
  font-weight: 500;
}

.article-title {
  color: var(--ink);
  font-weight: 500;
}

.author-name {
  color: var(--ink-soft);
  font-size: 0.88rem;
}

.tags-display {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-chip {
  background: var(--tag-bg);
  color: var(--accent);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.tag-more {
  color: var(--ink-muted);
  font-size: 0.72rem;
}

.no-tags {
  color: var(--ink-muted);
  font-size: 0.85rem;
}

.category-tag {
  background: var(--badge-bg);
  color: var(--warm);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 500;
}

.status-badge {
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition);
}

.status-badge.published {
  background: var(--status-published-bg);
  color: var(--status-published-text);
}

.status-badge.draft {
  background: var(--status-draft-bg);
  color: var(--status-draft-text);
}

.status-badge:hover {
  opacity: 0.8;
}

.action-buttons {
  display: flex;
  gap: var(--space-2);
}

.action-btn {
  padding: 6px 14px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition);
}

.edit-btn {
  background: var(--accent-soft);
  color: var(--accent);
}

.edit-btn:hover {
  background: var(--accent);
  color: #FDFBF5;
}

.delete-btn {
  background: var(--status-error-bg);
  color: var(--status-error-text);
}

.delete-btn:hover {
  background: var(--status-error-text);
  color: #FDFBF5;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-6);
}

.page-btn {
  padding: 8px 18px;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition);
  font-family: inherit;
  color: var(--ink-soft);
  font-size: 0.88rem;
}

.page-btn:not(:disabled):hover {
  border-color: var(--accent);
  color: var(--accent);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: var(--ink-muted);
  font-size: 0.88rem;
}

@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    min-width: auto;
  }

  .table-header,
  .table-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .header-cell,
  .table-cell {
    padding: 8px 12px;
  }

  .action-buttons {
    justify-content: center;
  }
}
</style>
