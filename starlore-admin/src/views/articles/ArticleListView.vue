<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  deleteArticleService,
  getAllArticlesService,
  toggleArticleStatusService,
  articleCategoryListService,
} from '@/api/article'
import type { ArticleItem, ArticleStatus } from '@/types'

const router = useRouter()

const loading = ref(false)
const articles = ref<ArticleItem[]>([])
const categories = ref<string[]>([])
const total = ref(0)
const pages = ref(1)

const filters = reactive({
  page: 1,
  limit: 10,
  search: '',
  category: '',
  status: '' as ArticleStatus | '',
})

async function loadArticles(): Promise<void> {
  loading.value = true
  try {
    const res = await getAllArticlesService({
      ...filters,
      search: filters.search || undefined,
      category: filters.category || undefined,
      status: filters.status || undefined,
    })
    articles.value = res.data ?? []
    total.value = res.pagination?.total ?? 0
    pages.value = res.pagination?.pages ?? 1
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载文章失败')
  } finally {
    loading.value = false
  }
}

async function handleSearch(): Promise<void> {
  filters.page = 1
  await loadArticles()
}

async function toggleStatus(row: ArticleItem): Promise<void> {
  const next: ArticleStatus = row.status === 'published' ? 'draft' : 'published'
  try {
    await toggleArticleStatusService(row.id, next)
    row.status = next
    ElMessage.success(next === 'published' ? '已发布' : '已转为草稿')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  }
}

async function handleDelete(row: ArticleItem): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定删除《${row.title}》吗？此操作不可恢复。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteArticleService(row.id)
    ElMessage.success('删除成功')
    await loadArticles()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

onMounted(async () => {
  await loadArticles()
  const list = await articleCategoryListService().catch(() => [])
  categories.value = list.map((c) => c.name)
})
</script>

<template>
  <div>
    <div class="page-head">
      <div class="filter-bar">
        <el-input
          v-model="filters.search"
          placeholder="搜索标题关键词"
          clearable
          :prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filters.category" placeholder="全部分类" clearable style="width: 150px" @change="handleSearch">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px" @change="handleSearch">
          <el-option label="已发布" value="published" />
          <el-option label="草稿" value="draft" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="router.push('/admin/articles/add')">新建星记</el-button>
    </div>

    <div class="table-wrap">
      <el-table v-loading="loading" :data="articles" empty-text="暂无文章">
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column prop="authorName" label="作者" width="110" />
        <el-table-column prop="view_count" label="浏览" width="80" align="right" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'published'"
              active-text="发布"
              inline-prompt
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">
            {{ row.updatedAt ? new Date(row.updatedAt).toLocaleString('zh-CN', { hour12: false }) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/admin/articles/${row.id}`)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="filters.page"
          :page-size="filters.limit"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="loadArticles"
        />
      </div>
    </div>
  </div>
</template>
