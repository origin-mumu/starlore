<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Document } from '@element-plus/icons-vue'
import {
  getAllArticlesService,
  deleteArticleService,
  toggleArticleStatusService,
  articleCategoryListService,
} from '@/api/article'
import { getUserListService } from '@/api/user'
import type { ArticleItem, ArticleStatus } from '@/types'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'

const router = useRouter()

const articles = ref<ArticleItem[]>([])
const loading = ref(false)
const categories = ref<string[]>([])
const users = ref<{ id: number; nickname: string }[]>([])

const filters = reactive({
  keyword: '',
  category: 'all',
  status: 'all' as ArticleStatus | 'all',
  author: 'all' as number | 'all',
})

const pagination = reactive({ current: 1, pages: 0, total: 0 })

const fetchArticles = async (): Promise<void> => {
  loading.value = true
  try {
    const result = await getAllArticlesService({
      page: pagination.current,
      limit: 10,
      search: filters.keyword || undefined,
      category: filters.category !== 'all' ? filters.category : undefined,
      status: filters.status,
      authorUserId: filters.author !== 'all' ? filters.author : undefined,
    })
    articles.value = result.data
    pagination.pages = result.pagination?.pages ?? 0
    pagination.total = result.pagination?.total ?? 0
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchCategories = async (): Promise<void> => {
  try {
    const list = await articleCategoryListService()
    categories.value = list.map((item) => item.name || String((item as { category?: string }).category ?? ''))
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

const fetchUsers = async (): Promise<void> => {
  try {
    const list = await getUserListService()
    users.value = list.map((u) => ({ id: u.id, nickname: u.nickname || u.username }))
  } catch {
    /* 非管理员忽略 */
  }
}

watch(
  () => ({ ...filters }),
  () => {
    pagination.current = 1
    void fetchArticles()
  },
)

onMounted(() => {
  void fetchUsers()
  void fetchCategories()
  void fetchArticles()
})

const goToPage = (page: number): void => {
  if (page < 1 || page > pagination.pages) return
  pagination.current = page
  void fetchArticles()
}

const toggleStatus = async (article: ArticleItem): Promise<void> => {
  const next: ArticleStatus = article.status === 'published' ? 'draft' : 'published'
  try {
    await toggleArticleStatusService(article.id, next)
    article.status = next
  } catch {
    ElMessage.error('状态切换失败，请重试')
  }
}

const deleteArticle = async (id: number): Promise<void> => {
  if (!confirm('确定要删除这篇星记吗？')) return
  try {
    await deleteArticleService(id)
    ElMessage.success('删除成功')
    await fetchArticles()
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}

const formatDate = (dateString: string): string => new Date(dateString).toLocaleDateString('zh-CN')
</script>

<template>
  <div class="article-list">
    <header class="page-header">
      <div>
        <h1 class="page-title">星记管理</h1>
        <p class="page-subtitle">共 {{ pagination.total }} 篇星记</p>
      </div>
      <button class="btn btn--primary" type="button" @click="router.push({ name: 'AddArticle' })">
        <el-icon><Plus /></el-icon>
        新建星记
      </button>
    </header>

    <div class="filter-bar">
      <div class="search-field">
        <el-icon class="search-field__icon" :size="15"><Search /></el-icon>
        <input
          v-model="filters.keyword"
          class="form-input"
          type="text"
          placeholder="搜索星记标题或描述..."
        />
      </div>
      <select v-model="filters.category" class="form-select">
        <option value="all">全部星域</option>
        <option v-for="category in categories" :key="category" :value="category">
          {{ category }}
        </option>
      </select>
      <select v-model="filters.status" class="form-select">
        <option value="all">全部状态</option>
        <option value="published">已发布</option>
        <option value="draft">草稿</option>
      </select>
      <select v-model="filters.author" class="form-select">
        <option value="all">全部观星者</option>
        <option v-for="user in users" :key="user.id" :value="user.id">{{ user.nickname }}</option>
      </select>
    </div>

    <div class="article-list__table" style="--table-cols: 2fr 0.8fr 0.8fr 1.2fr 0.8fr 1fr 0.7fr 1.4fr">
      <LoadingState v-if="loading" />

      <EmptyState v-else-if="articles.length === 0" text="没有找到符合条件的星记">
        <template #icon>
          <el-icon><Document /></el-icon>
        </template>
      </EmptyState>

      <div v-else class="data-table">
        <div class="data-table__header">
          <div>星记标题</div>
          <div>作者</div>
          <div>星域</div>
          <div>光痕</div>
          <div>状态</div>
          <div>创建时间</div>
          <div>浏览量</div>
          <div>操作</div>
        </div>
        <div v-for="article in articles" :key="article.id" class="data-table__row">
          <div>
            <span class="cell-primary" :title="article.title">{{ article.title }}</span>
          </div>
          <div>{{ article.authorName || '-' }}</div>
          <div><span class="pill pill--neutral">{{ article.category }}</span></div>
          <div>
            <span v-if="article.tags && article.tags.length" class="article-list__tags">
              <span v-for="tag in article.tags.slice(0, 3)" :key="tag" class="tag-chip">{{ tag }}</span>
              <span v-if="article.tags.length > 3" class="tag-more">+{{ article.tags.length - 3 }}</span>
            </span>
            <span v-else>-</span>
          </div>
          <div>
            <span
              class="pill pill--clickable"
              :class="article.status === 'published' ? 'pill--success' : 'pill--warning'"
              :title="'点击切换为' + (article.status === 'published' ? '草稿' : '已发布')"
              @click="toggleStatus(article)"
            >
              {{ article.status === 'published' ? '已发布' : '草稿' }}
            </span>
          </div>
          <div>{{ formatDate(article.createdAt) }}</div>
          <div>{{ article.view_count }}</div>
          <div>
            <div class="article-list__actions">
              <button
                class="btn btn--secondary btn--sm"
                type="button"
                @click="router.push({ name: 'EditArticle', params: { id: article.id } })"
              >
                编辑
              </button>
              <button class="btn btn--danger btn--sm" type="button" @click="deleteArticle(article.id)">
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="pagination.pages > 1" class="pagination">
      <button class="btn btn--secondary btn--sm" :disabled="pagination.current <= 1" @click="goToPage(pagination.current - 1)">
        上一页
      </button>
      <span class="pagination__info">第 {{ pagination.current }} / {{ pagination.pages }} 页</span>
      <button class="btn btn--secondary btn--sm" :disabled="pagination.current >= pagination.pages" @click="goToPage(pagination.current + 1)">
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped>
.article-list__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.article-list__actions {
  display: flex;
  gap: 8px;
}
</style>
