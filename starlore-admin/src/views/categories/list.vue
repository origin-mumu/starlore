<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import Nav from '@/components/nav.vue'
import { createCategoryService, updateCategoryService, deleteCategoryService } from '@/api/article'
import { articleCategoryListService } from '@/api/article'
import { getUserListService } from '@/api/user'
const dialogVisible = ref(false)

interface Category {
  id: number
  userId: number
  name: string
  description: string
  article_count: number
  createdAt: string
  updatedAt: string
}

const categories = ref<Category[]>([])
const filteredCategories = ref<Category[]>([])
const searchKeyword = ref('')
const selectedAuthor = ref<number | 'all'>('all')
const users = ref<{ id: number; nickname: string }[]>([])
const categoryName = ref('')
const title = ref('添加星域')
const loading = ref(false)
const currentCategoryId = ref()

const fetchCategories = async () => {
  loading.value = true
  try {
    const response = await articleCategoryListService()
    const raw = (response.data || []).map((cat: any) => ({
      id: cat.id,
      userId: cat.userId,
      name: cat.name || cat.category,
      description: cat.description || '',
      article_count: cat.article_count ?? 0,
      createdAt: cat.createdAt,
      updatedAt: cat.updatedAt,
    }))
    categories.value = raw
    applyFilter()
  } catch (error) {
    console.error('获取分类列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchUsers = async () => {
  try {
    const res: any = await getUserListService()
    users.value = (res.data || []).map((u: any) => ({ id: u.id, nickname: u.nickname || u.username }))
  } catch { /* ignore */ }
}

const applyFilter = () => {
  let list = [...categories.value]
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(c => c.name.toLowerCase().includes(kw))
  }
  if (selectedAuthor.value !== 'all') {
    list = list.filter(c => c.userId === selectedAuthor.value)
  }
  filteredCategories.value = list
}

watch([searchKeyword, selectedAuthor], applyFilter)

onMounted(() => {
  fetchUsers()
  fetchCategories()
})

const addNewCategory = () => {
  dialogVisible.value = true
  categoryName.value = ''
  title.value = '添加星域'
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return isNaN(date.getTime()) ? '-' : date.toLocaleDateString('zh-CN')
}

const editCategory = (category: Category) => {
  currentCategoryId.value = category.id
  dialogVisible.value = true
  categoryName.value = category.name
  title.value = '编辑星域'
}

const saveCategory = async () => {
  try {
    loading.value = true
    if (title.value === '添加星域') {
      await createCategoryService({ name: categoryName.value })
    } else {
      await updateCategoryService(currentCategoryId.value, { name: categoryName.value })
    }
    loading.value = false
    dialogVisible.value = false
    fetchCategories()
  } catch (error) {
    loading.value = false
    console.error('保存分类失败:', error)
  }
}

const deleteCategory = async (id: number) => {
  if (confirm('确定要删除星域吗？')) {
    try {
      await deleteCategoryService(id)
      fetchCategories()
    } catch (error) {
      console.error('删除分类失败:', error)
      alert('删除失败，请重试')
    }
  }
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
          <h1 class="page-title">星域管理</h1>
          <button class="add-btn" @click="addNewCategory">
            <span class="btn-icon">➕</span>
            新建星域
          </button>
        </div>

        <!-- 搜索/筛选 -->
        <div class="filter-section">
          <div class="search-box">
            <input v-model="searchKeyword" type="text" placeholder="搜索星域名称..." class="search-input" />
            <span class="search-icon">🔍</span>
          </div>
          <div class="filter-controls">
            <select v-model="selectedAuthor" class="filter-select">
              <option value="all">全部观星者</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.nickname }}</option>
            </select>
          </div>
        </div>

        <!-- 分类列表 -->
        <div class="articles-table">
          <div v-if="loading" class="loading">加载中...</div>

          <div v-else-if="filteredCategories.length === 0" class="empty-state">
            <div class="empty-icon">📂</div>
            <p>没有找到符合条件的星域</p>
          </div>

          <div v-else class="table-container">
            <div class="table-header">
              <div class="header-cell">星域名称</div>
              <div class="header-cell">星记数</div>
              <div class="header-cell">创建时间</div>
              <div class="header-cell">操作</div>
            </div>

            <div class="table-body">
              <div v-for="category in filteredCategories" :key="category.id" class="table-row">
                <div class="table-cell">
                  <span class="category-name">{{ category.name }}</span>
                </div>
                <div class="table-cell">{{ category.article_count }}</div>
                <div class="table-cell">{{ formatDate(category.createdAt) }}</div>
                <div class="table-cell">
                  <div class="action-buttons">
                    <button class="action-btn edit-btn" @click="editCategory(category)">编辑</button>
                    <button class="action-btn delete-btn" @click="deleteCategory(category.id)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <el-dialog v-model="dialogVisible" :title="title" width="500">
          <el-input v-model="categoryName" placeholder="请输入星域名称" clearable></el-input>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="dialogVisible = false">取消</el-button>
              <el-button type="primary" @click="((dialogVisible = false), saveCategory())">
                确认
              </el-button>
            </div>
          </template>
        </el-dialog>
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
  grid-template-columns: 2fr 0.8fr 1fr 1.5fr;
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
  grid-template-columns: 2fr 0.8fr 1fr 1.5fr;
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

.category-name {
  font-weight: 500;
  color: var(--ink);
}

.category-tag {
  background: var(--badge-bg);
  color: var(--warm);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 500;
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
