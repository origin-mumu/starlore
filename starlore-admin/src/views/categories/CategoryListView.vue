<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { Plus, Search, FolderOpened } from '@element-plus/icons-vue'
import {
  articleCategoryListService,
  createCategoryService,
  updateCategoryService,
  deleteCategoryService,
} from '@/api/article'
import { getUserListService } from '@/api/user'
import type { CategoryItem } from '@/types'
import AppModal from '@/components/common/AppModal.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'

const categories = ref<CategoryItem[]>([])
const filteredCategories = ref<CategoryItem[]>([])
const users = ref<{ id: number; nickname: string }[]>([])
const loading = ref(false)

const filters = reactive({
  keyword: '',
  author: 'all' as number | 'all',
})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentCategoryId = ref<number | null>(null)
const categoryName = ref('')
const categoryError = ref('')

const fetchCategories = async (): Promise<void> => {
  loading.value = true
  try {
    const raw = await articleCategoryListService()
    categories.value = raw.map((item) => ({
      ...item,
      name: item.name || String((item as { category?: string }).category ?? ''),
      description: item.description ?? '',
      article_count: item.article_count ?? 0,
    }))
    applyFilter()
  } catch (error) {
    console.error('获取分类列表失败:', error)
  } finally {
    loading.value = false
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

const applyFilter = (): void => {
  let list = [...categories.value]
  if (filters.keyword) {
    const keyword = filters.keyword.toLowerCase()
    list = list.filter((item) => item.name.toLowerCase().includes(keyword))
  }
  if (filters.author !== 'all') {
    list = list.filter((item) => item.userId === filters.author)
  }
  filteredCategories.value = list
}

watch(
  () => ({ ...filters }),
  applyFilter,
)

onMounted(() => {
  void fetchUsers()
  void fetchCategories()
})

const openCreateDialog = (): void => {
  dialogMode.value = 'create'
  currentCategoryId.value = null
  categoryName.value = ''
  categoryError.value = ''
  dialogVisible.value = true
}

const openEditDialog = (category: CategoryItem): void => {
  dialogMode.value = 'edit'
  currentCategoryId.value = category.id
  categoryName.value = category.name
  categoryError.value = ''
  dialogVisible.value = true
}

const saveCategory = async (): Promise<void> => {
  const name = categoryName.value.trim()
  if (!name) {
    categoryError.value = '请输入星域名称'
    return
  }
  try {
    if (dialogMode.value === 'create') {
      await createCategoryService({ name })
    } else if (currentCategoryId.value !== null) {
      await updateCategoryService(currentCategoryId.value, { name })
    }
    dialogVisible.value = false
    ElMessage.success('保存成功')
    await fetchCategories()
  } catch (error) {
    console.error('保存分类失败:', error)
    ElMessage.error('保存失败，请重试')
  }
}

const deleteCategory = async (id: number): Promise<void> => {
  if (!confirm('确定要删除星域吗？')) return
  try {
    await deleteCategoryService(id)
    ElMessage.success('删除成功')
    await fetchCategories()
  } catch (error) {
    console.error('删除分类失败:', error)
    ElMessage.error('删除失败，请重试')
  }
}

const formatDate = (dateString: string): string => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="category-list">
    <header class="page-header">
      <div>
        <h1 class="page-title">星域管理</h1>
        <p class="page-subtitle">知识库分类体系</p>
      </div>
      <button class="btn btn--primary" type="button" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建星域
      </button>
    </header>

    <div class="filter-bar">
      <div class="search-field">
        <el-icon class="search-field__icon" :size="15"><Search /></el-icon>
        <input v-model="filters.keyword" class="form-input" type="text" placeholder="搜索星域名称..." />
      </div>
      <select v-model="filters.author" class="form-select">
        <option value="all">全部观星者</option>
        <option v-for="user in users" :key="user.id" :value="user.id">{{ user.nickname }}</option>
      </select>
    </div>

    <LoadingState v-if="loading" />

    <EmptyState v-else-if="filteredCategories.length === 0" text="没有找到符合条件的星域">
      <template #icon>
        <el-icon><FolderOpened /></el-icon>
      </template>
    </EmptyState>

    <div v-else class="data-table" style="--table-cols: 2fr 0.7fr 1fr 1.3fr">
      <div class="data-table__header">
        <div>星域名称</div>
        <div>星记数</div>
        <div>创建时间</div>
        <div>操作</div>
      </div>
      <div v-for="category in filteredCategories" :key="category.id" class="data-table__row">
        <div><span class="cell-primary">{{ category.name }}</span></div>
        <div><span class="pill pill--brand">{{ category.article_count }}</span></div>
        <div>{{ formatDate(category.createdAt) }}</div>
        <div>
          <div class="category-list__actions">
            <button class="btn btn--secondary btn--sm" type="button" @click="openEditDialog(category)">
              编辑
            </button>
            <button class="btn btn--danger btn--sm" type="button" @click="deleteCategory(category.id)">
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <AppModal
      :title="dialogMode === 'create' ? '添加星域' : '编辑星域'"
      :width="440"
      @close="dialogVisible = false"
    >
      <div class="form-field">
        <label>星域名称</label>
        <input
          v-model="categoryName"
          class="form-input"
          :class="{ 'form-input--error': categoryError }"
          type="text"
          placeholder="请输入星域名称"
          @keyup.enter="saveCategory"
        />
        <Transition name="fade">
          <span v-if="categoryError" class="form-error">{{ categoryError }}</span>
        </Transition>
      </div>
      <template #footer>
        <button class="btn btn--secondary" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn--primary" type="button" @click="saveCategory">确认</button>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.category-list__actions {
  display: flex;
  gap: 8px;
}

.fade-enter-active {
  transition: all var(--transition-normal);
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(-2px);
}
</style>
