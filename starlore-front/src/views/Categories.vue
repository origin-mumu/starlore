<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { getCategoriesService, getPublicCategoriesService, createCategoryService, deleteCategoryService } from '@/api/article'
import router from '@/router'
import SideBar from '@/components/sideBar.vue'
import { useUserStore } from '@/stores/user'
import ConfirmModal from '@/components/ConfirmModal.vue'

const userStore = useUserStore()
const notification = reactive({ show: false, title: '', message: '' })
function notify(title: string, message: string) {
  notification.title = title
  notification.message = message
  notification.show = true
}

const demoCategories: Category[] = [
  { id: -1, name: '前端开发', description: 'Vue、React、CSS 等', color: '#B85C38', article_count: 5 },
  { id: -2, name: '后端技术', description: 'Spring Boot、数据库等', color: '#4A8C5C', article_count: 3 },
  { id: -3, name: '设计思考', description: 'UI/UX、交互设计', color: '#3B7DD8', article_count: 2 },
]

interface Category {
  id: number
  name: string
  description?: string
  color?: string
  article_count: number
}

const categories = ref<Category[]>([])
const loading = ref(true)
const showAdd = ref(false)
const newName = ref('')
const adding = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res: any = userStore.isLoggedIn
      ? await getCategoriesService()
      : await getPublicCategoriesService()
    categories.value = res.data?.data || res.data || []
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}


onMounted(load)

const navigateToCategory = (categoryId: number, categoryName: string) => {
  router.push({ name: 'articles', query: { category: categoryName, categoryId: categoryId.toString() } })
}

const handleAdd = async () => {
  const name = newName.value.trim()
  if (!name) return
  adding.value = true
  try {
    await createCategoryService({ name })
    newName.value = ''
    showAdd.value = false
    await load()
  } catch { notify('创建失败', '创建星域失败，请重试') } finally {
    adding.value = false
  }
}

const deleteCatId = ref<number>(0)
const deleteCatName = ref('')
const showDeleteCat = ref(false)

const handleDelete = (id: number, name: string) => {
  deleteCatId.value = id
  deleteCatName.value = name
  showDeleteCat.value = true
}

const confirmDeleteCat = async () => {
  showDeleteCat.value = false
  try {
    await deleteCategoryService(deleteCatId.value)
    await load()
  } catch { notify('删除失败', '删除星域失败，请重试') }
}
</script>

<template>
  <div class="page-container">
    <section class="page-header">
      <div class="container">
        <h1>{{ userStore.isLoggedIn ? '星域管理' : '公开星域' }}</h1>
      </div>
    </section>

    <section class="section-parchment">
      <div class="container">
        <div class="content-layout">
          <main class="category-list">
            <div class="toolbar" v-if="userStore.isLoggedIn">
              <button class="btn-primary" @click="showAdd = !showAdd">+ 新增星域</button>
            </div>

            <div v-if="showAdd" class="add-card">
              <input v-model="newName" placeholder="输入星域名称" @keydown.enter="handleAdd" />
              <button class="btn-outline btn-sm" :disabled="adding" @click="handleAdd">{{ adding ? '创建中...' : '确认' }}</button>
              <button class="btn-outline btn-sm" @click="showAdd = false; newName = ''">取消</button>
            </div>

            <div v-if="loading" class="loading-box">加载中...</div>

            <div v-else class="categories-panel ink-glass-card">
              <div class="categories-grid">
                <div v-for="(cat, index) in categories" :key="cat.id" class="category-row fade-in-up"
                  :style="{ animationDelay: `${60 + index * 70}ms` }">
                  <div class="category-header" @click="navigateToCategory(cat.id, cat.name)">
                    <div class="cat-left">
                      <span class="cat-dot" :style="{ background: cat.color || 'var(--accent)' }"></span>
                      <h3 class="category-name">{{ cat.name }}</h3>
                      <span v-if="cat.description" class="cat-desc">{{ cat.description }}</span>
                    </div>
                    <div class="cat-right">
                      <span class="article-count">{{ cat.article_count }} 篇</span>
                      <button v-if="userStore.isLoggedIn" class="btn-del" @click.stop="handleDelete(cat.id, cat.name)" title="删除">&times;</button>
                    </div>
                  </div>
                </div>
                <div v-if="categories.length === 0 && !loading" class="empty-row">
                  {{ userStore.isLoggedIn ? '还没有星域，点击上方按钮添加' : '暂无公开星域' }}
                </div>
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
    :show="showDeleteCat"
    title="确认删除"
    :message="`确认删除星域「${deleteCatName}」？关联的星记将变为未分类`"
    confirm-text="删除"
    @confirm="confirmDeleteCat"
    @cancel="showDeleteCat = false"
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
.category-list { min-width: 0; }
.sidebar-area { position: sticky; top: 100px; }

.toolbar { margin-bottom: 20px; }

.add-card {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  align-items: center;
}
.add-card input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
  background: var(--canvas);
  color: var(--ink);
  font-family: inherit;
}

.categories-panel { padding: 8px; background: #FFFFFF; }
.categories-grid { display: grid; gap: 0; }

.category-row {
  cursor: pointer;
  border-bottom: 1px solid var(--border);
  transition: all var(--transition);
  border-radius: var(--radius-md);
}
.category-row:last-child { border-bottom: none; }
.category-row:hover { background: var(--surface-hover); }

.category-header {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.cat-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.cat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.category-name {
  font-size: 17px;
  font-weight: 600;
  margin: 0;
  letter-spacing: -0.01em;
  transition: color var(--transition);
  white-space: nowrap;
}
.category-row:hover .category-name { color: var(--accent); }
.cat-desc {
  font-size: 0.82rem;
  color: var(--ink-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cat-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.article-count {
  font-size: 14px;
  color: var(--ink-muted);
  white-space: nowrap;
}
.btn-del {
  background: none;
  border: none;
  font-size: 1.3rem;
  color: #e74c3c;
  cursor: pointer;
  opacity: 0;
  padding: 0 4px;
  transition: opacity 0.2s;
  line-height: 1;
}
.category-row:hover .btn-del { opacity: 0.5; }
.btn-del:hover { opacity: 1 !important; }

.loading-box { text-align: center; padding: 60px 0; color: var(--ink-muted); }
.empty-row { text-align: center; padding: 40px 0; color: var(--ink-muted); }

.btn-outline {
  padding: 6px 16px;
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  color: var(--ink);
  transition: all 0.2s;
  font-family: inherit;
}
.btn-outline:hover { border-color: var(--accent); color: var(--accent); }
.btn-sm { font-size: 0.8rem; padding: 5px 14px; }

@media (max-width: 900px) {
  .content-layout { grid-template-columns: 1fr; }
  .sidebar-area { position: static; }
}
</style>
