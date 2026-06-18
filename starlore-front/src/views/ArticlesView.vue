<script lang="ts" setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { getAllArticlesService, getPublicArticlesService, getCategoriesService, getPublicCategoriesService, deleteArticleService } from '@/api/article'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import SideBar from '@/components/sideBar.vue'
import DummyCard from '@/components/dummyCard.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notification = reactive({ show: false, title: '', message: '' })
function notify(title: string, message: string) {
  notification.title = title
  notification.message = message
  notification.show = true
}
interface Article {
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
interface Category {
  id: number
  name: string
  article_count: number
}

const currentPage = ref(1)
const pageSize = ref(5)
const totalArticles = ref(0)
const totalPages = ref(0)

const articles = ref<Article[]>([])
const filteredArticles = ref<Article[]>([])
const isLoading = ref(false)
const hasArticles = computed(() => filteredArticles.value.length > 0)

const fetchArticles = async (page = currentPage.value, category?: string) => {
  isLoading.value = true
  try {
    const params: any = {
      page,
      limit: pageSize.value,
    }

    if (category && category !== '全部') {
      params.category = category
    }

    const res = userStore.isLoggedIn
      ? ((await getAllArticlesService(params)) as any)
      : ((await getPublicArticlesService(params)) as any)

    articles.value = res.data
    filteredArticles.value = articles.value
    totalArticles.value = res.pagination.total
    totalPages.value = res.pagination.pages
    currentPage.value = res.pagination.current
  } catch (error) {
    console.error('获取文章列表失败:', error)
    articles.value = []
    filteredArticles.value = []
    totalArticles.value = 0
    totalPages.value = 0
    currentPage.value = 1
  } finally {
    isLoading.value = false
  }
}

const activeCategory = ref('全部')
const categories = ref<Category[]>([])

onMounted(async () => {
  const initialCategory = (route.query.category as string) || '全部'
  activeCategory.value = initialCategory
  try {
    const fetchCats = userStore.isLoggedIn
      ? getCategoriesService()
      : getPublicCategoriesService()
    const [resCats] = await Promise.all([
      fetchCats,
      fetchArticles(1, initialCategory === '全部' ? undefined : initialCategory),
    ])
    const res: any = resCats
    categories.value = res.data?.data || res.data || []
  } catch (err) {
    console.error('加载星记或分类失败:', err)
  }
})

watch(
  () => route.query.category,
  newCategory => {
    if (newCategory && newCategory !== activeCategory.value) {
      activeCategory.value = newCategory as string
      currentPage.value = 1
      fetchArticles(1, newCategory as string)
    }
  }
)

const filterCategory = (cat: string) => {
  activeCategory.value = cat
  currentPage.value = 1
  fetchArticles(1, cat === '全部' ? undefined : cat)
}

const deleteArticleId = ref<number>(0)
const showDeleteArticle = ref(false)

const handleDelete = (id: number, e: Event) => {
  e.stopPropagation()
  deleteArticleId.value = id
  showDeleteArticle.value = true
}

const confirmDeleteArticle = async () => {
  showDeleteArticle.value = false
  try {
    await deleteArticleService(deleteArticleId.value)
    await fetchArticles()
  } catch { notify('删除失败', '删除星记失败，请重试') }
}

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    fetchArticles(page, activeCategory.value === '全部' ? undefined : activeCategory.value)
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    goToPage(currentPage.value - 1)
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    goToPage(currentPage.value + 1)
  }
}

const paginationButtons = computed(() => {
  const buttons = []
  const maxVisibleButtons = 5
  let startPage = Math.max(1, currentPage.value - Math.floor(maxVisibleButtons / 2))
  let endPage = Math.min(totalPages.value, startPage + maxVisibleButtons - 1)

  if (endPage - startPage + 1 < maxVisibleButtons) {
    startPage = Math.max(1, endPage - maxVisibleButtons + 1)
  }

  for (let i = startPage; i <= endPage; i++) {
    buttons.push(i)
  }

  return buttons
})
</script>

<template>
  <div class="page-container">
    <section class="page-header">
      <div class="container header-bar">
        <h1>{{ userStore.isLoggedIn ? '星记管理' : '公开星记 (访客只读)' }}</h1>
        <router-link v-if="userStore.isLoggedIn" to="/articles/edit" class="btn-primary btn-write">+ 写星记</router-link>
      </div>
    </section>

    <section class="section-parchment">
      <div class="container">
        <div class="content-layout">
          <main class="post-list">
            <div class="filter-bar">
              <button
                class="filter-pill"
                :class="{ active: activeCategory === '全部' }"
                @click="filterCategory('全部')"
              >
                全部
              </button>
              <button
                v-for="cat in categories"
                :key="cat.id"
                class="filter-pill"
                :class="{ active: activeCategory === cat.name }"
                @click="filterCategory(cat.name)"
              >
                {{ cat.name }}
              </button>
            </div>

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
                </div>
                <button
                  v-if="userStore.isLoggedIn"
                  class="btn-edit-card"
                  @click.stop="router.push(`/articles/edit/${article.id}`)"
                  title="编辑星记"
                >&#9998;</button>
                <button
                  v-if="userStore.isLoggedIn"
                  class="btn-delete-card"
                  @click="handleDelete(article.id, $event)"
                  title="删除星记"
                >&times;</button>
              </div>
              <div v-else class="empty-state ink-glass-card">
                <p class="empty-title">{{ activeCategory === '全部' ? '还没有星记' : '当前星域暂无星记' }}</p>
                <p class="empty-desc">{{ userStore.isLoggedIn ? '开始写一篇星记，与大家分享你的想法吧' : '暂无公开星记' }}</p>
                <div class="empty-actions">
                  <router-link v-if="userStore.isLoggedIn" to="/articles/edit" class="btn-primary empty-action">写星记</router-link>
                  <button v-if="activeCategory !== '全部'" class="btn-secondary empty-action" @click="filterCategory('全部')">查看全部</button>
                </div>
              </div>
            </div>

            <div v-if="!isLoading && hasArticles" class="pagination">
              <button class="page-btn" :class="{ disabled: currentPage === 1 }" @click="prevPage">
                &lt;
              </button>

              <button v-if="currentPage > 3" class="page-btn" @click="goToPage(1)">1</button>
              <span v-if="currentPage > 4" class="page-ellipsis">...</span>

              <button
                v-for="page in paginationButtons"
                :key="page"
                class="page-btn"
                :class="{ active: currentPage === page }"
                @click="goToPage(page)"
              >
                {{ page }}
              </button>

              <span v-if="currentPage < totalPages - 3" class="page-ellipsis">...</span>

              <button
                v-if="currentPage < totalPages - 2"
                class="page-btn"
                @click="goToPage(totalPages)"
              >
                {{ totalPages }}
              </button>

              <button
                class="page-btn"
                :class="{ disabled: currentPage === totalPages }"
                @click="nextPage"
              >
                &gt;
              </button>

              <div class="page-info">
                第 {{ currentPage }} 页，共 {{ totalPages }} 页（{{ totalArticles }} 篇星记）
              </div>
            </div>
          </main>

          <aside class="sidebar-area">
            <SideBar />
          </aside>
        </div>
      </div>
    </section>
  </div>

  <ConfirmModal
    :show="showDeleteArticle"
    title="确认删除"
    message="确定删除该星记？此操作不可撤销。"
    confirm-text="删除"
    @confirm="confirmDeleteArticle"
    @cancel="showDeleteArticle = false"
  />
  <ConfirmModal
    :show="notification.show"
    :title="notification.title"
    :message="notification.message"
    type="alert"
    confirm-text="确定"
    @confirm="notification.show = false"
    @cancel="notification.show = false"
  />
</template>

<style scoped>
.content-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
  align-items: flex-start;
}

.post-list {
  min-width: 0;
}

.sidebar-area {
  position: sticky;
  top: 100px;
}

.article-items {
  columns: 2;
  column-gap: 16px;
}

.article-items > :deep(*) {
  break-inside: avoid;
  margin-bottom: 16px;
}

.loading-state {
  column-span: all;
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 34px;
  height: 34px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  margin-top: 12px;
  color: var(--ink-muted);
  font-size: 15px;
}

.empty-state {
  column-span: all;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  padding: 32px 24px;
  text-align: center;
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

.empty-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}
.empty-action {
  padding: 10px 24px;
  font-size: 15px;
  text-decoration: none;
  display: inline-block;
}
.btn-secondary {
  padding: 10px 24px;
  font-size: 15px;
  border: 1px solid var(--border);
  background: var(--glass-bg);
  color: var(--ink);
  border-radius: var(--radius-full);
  cursor: pointer;
  font-weight: 500;
  transition: all var(--transition);
  font-family: inherit;
  backdrop-filter: blur(12px) saturate(1.2);
  -webkit-backdrop-filter: blur(12px) saturate(1.2);
}
.btn-secondary:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.card-wrapper {
  position: relative;
  min-width: 0;
}
.card-wrapper:hover .btn-delete-card {
  opacity: 1;
}
.card {
  min-width: 0;
}
.btn-edit-card,
.btn-delete-card {
  position: absolute;
  top: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 0.85rem;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
}
.card-wrapper:hover .btn-edit-card,
.card-wrapper:hover .btn-delete-card {
  opacity: 1;
}
.btn-edit-card {
  right: 42px;
}
.btn-edit-card:hover {
  background: #30cfff;
}
.btn-delete-card {
  right: 8px;
}
.btn-delete-card:hover {
  background: #e74c3c;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.filter-bar {
  display: flex;
  gap: 8px;
  padding: 0;
  margin-bottom: 32px;
  overflow-x: auto;
  background: transparent;
  border: none;
}

.filter-pill {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--ink-soft);
  cursor: pointer;
  font-weight: 500;
  transition: all var(--transition);
  white-space: nowrap;
  padding: 8px 18px;
  border-radius: var(--radius-full);
  background: var(--tag-bg);
  border: 1px solid transparent;
}

.filter-pill:hover {
  background: var(--tag-hover);
  border-color: var(--border-interactive);
}

.filter-pill.active {
  background: var(--accent);
  color: #ffffff;
  border-color: var(--accent);
  font-weight: 600;
  box-shadow: var(--shadow-button);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.page-btn {
  min-width: 38px;
  height: 38px;
  border: 1px solid var(--border);
  background: var(--glass-bg);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all var(--transition);
  color: var(--ink);
  font-size: 14px;
  padding: 0 8px;
  backdrop-filter: blur(12px) saturate(1.2);
  -webkit-backdrop-filter: blur(12px) saturate(1.2);
}

.page-btn:hover:not(.disabled) {
  border-color: var(--accent);
  color: var(--accent);
  box-shadow: 0 4px 16px oklch(0.55 0.15 35 / 0.1);
}

.page-btn.active {
  background: var(--accent);
  color: #ffffff;
  border-color: var(--accent);
  box-shadow: var(--shadow-button);
}

.page-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-ellipsis {
  padding: 0 8px;
  color: var(--ink-muted);
}

.page-info {
  margin-left: 12px;
  color: var(--ink-muted);
  font-size: 13px;
  white-space: nowrap;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.btn-write {
  padding: 8px 20px;
  border: none;
  border-radius: var(--radius-full);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  color: #FDFBF5;
  background: var(--accent);
  box-shadow: var(--shadow-button);
  transition: all var(--transition);
  text-decoration: none;
  font-family: inherit;
}
.btn-write:hover {
  background: var(--accent-hover);
  box-shadow: var(--shadow-button-hover);
  transform: translateY(-1px);
}

@media (max-width: 900px) {
  .content-layout {
    grid-template-columns: 1fr;
  }

  .sidebar-area {
    position: static;
  }

  .article-items {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .pagination {
    gap: 4px;
  }

  .page-btn {
    min-width: 32px;
    height: 32px;
    font-size: 13px;
  }

  .page-info {
    font-size: 12px;
  }
}
</style>
