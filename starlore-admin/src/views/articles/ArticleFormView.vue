<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { createArticleService, getArticleByIdService, updateArticleService, articleCategoryListService } from '@/api/article'
import { useArticleEditor } from '@/composables/useArticleEditor'
import type { ArticleStatus, CategoryItem } from '@/types'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()

const categories = ref<CategoryItem[]>([])
const saving = ref(false)
const errorMessage = ref('')

const articleId = computed(() => (route.params.id ? Number(route.params.id) : null))
const isEdit = computed(() => articleId.value !== null)
const pageTitle = computed(() => (isEdit.value ? '编辑星记' : '新增星记'))

const form = reactive({
  title: '',
  description: '',
  category: '',
  status: 'draft' as ArticleStatus,
})

const { editorRef, editorHtml, editorMode, toolbarConfig, editorConfig, handleCreated } =
  useArticleEditor()

const loadCategories = async (): Promise<void> => {
  try {
    categories.value = await articleCategoryListService()
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

const loadArticle = async (): Promise<void> => {
  if (articleId.value === null) return
  try {
    const article = await getArticleByIdService(articleId.value)
    form.title = article.title
    form.description = article.description ?? ''
    form.category = article.category
    form.status = article.status
    editorHtml.value = article.content
  } catch (error) {
    console.error('加载文章失败:', error)
    ElMessage.error('加载星记失败')
  }
}

const validate = (): boolean => {
  if (!form.title.trim()) {
    errorMessage.value = '请输入星记标题'
    return false
  }
  if (!form.category) {
    errorMessage.value = '请选择星域'
    return false
  }
  errorMessage.value = ''
  return true
}

const save = async (): Promise<void> => {
  if (!validate()) return
  saving.value = true
  try {
    const payload = {
      title: form.title,
      description: form.description,
      category: form.category,
      status: form.status,
      content: editorHtml.value,
    }
    if (isEdit.value && articleId.value !== null) {
      await updateArticleService(articleId.value, payload)
      ElMessage.success('星记编辑成功')
    } else {
      await createArticleService(payload)
      ElMessage.success('星记创建成功')
    }
    router.push({ name: 'articles' })
  } catch (error) {
    console.error('保存文章失败:', error)
    ElMessage.error(isEdit.value ? '编辑星记失败' : '创建星记失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadCategories()
  void loadArticle()
})
</script>

<template>
  <div class="article-form">
    <header class="page-header">
      <div class="article-form__toolbar">
        <button class="btn btn--secondary btn--sm" type="button" @click="router.push({ name: 'articles' })">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </button>
        <h1 class="page-title">{{ pageTitle }}</h1>
      </div>
      <button class="btn btn--primary" type="button" :disabled="saving" @click="save">
        <el-icon v-if="saving" class="is-loading"><Loading /></el-icon>
        <span>{{ saving ? '保存中...' : '保存' }}</span>
      </button>
    </header>

    <div class="bento-card article-form__body">
      <div class="article-form__grid">
        <div class="form-field">
          <label>标题</label>
          <input v-model="form.title" class="form-input" type="text" placeholder="请输入星记标题" />
        </div>
        <div class="form-field">
          <label>星域</label>
          <select v-model="form.category" class="form-select">
            <option value="" disabled>请选择星域</option>
            <option v-for="item in categories" :key="item.id" :value="item.name">
              {{ item.name }}
            </option>
          </select>
        </div>
        <div class="form-field">
          <label>状态</label>
          <select v-model="form.status" class="form-select">
            <option value="draft">草稿</option>
            <option value="published">已发布</option>
          </select>
        </div>
      </div>

      <div class="form-field">
        <label>描述</label>
        <input v-model="form.description" class="form-input" type="text" placeholder="请输入星记描述" />
      </div>

      <div class="form-field">
        <label>内容</label>
        <div class="article-form__editor">
          <Toolbar
            class="article-form__editor-toolbar"
            :editor="editorRef"
            :default-config="toolbarConfig"
            :mode="editorMode"
          />
          <Editor
            class="article-form__editor-area"
            v-model="editorHtml"
            :default-config="editorConfig"
            :mode="editorMode"
            @on-created="handleCreated"
          />
        </div>
      </div>

      <Transition name="error-fade">
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
      </Transition>
    </div>
  </div>
</template>

<style scoped>
.article-form__toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
}

.article-form__body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.article-form__grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 16px;
}

.article-form__editor {
  border: var(--border-subtle);
  border-radius: var(--radius-sm);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  background: #fff;
}

.article-form__editor-toolbar {
  border-bottom: var(--border-subtle);
  background: rgba(242, 235, 217, 0.45);
}

.article-form__editor-area {
  height: 420px;
  min-height: 320px;
  overflow-y: hidden;
}

.error-fade-enter-active {
  transition: all var(--transition-normal);
}

.error-fade-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 900px) {
  .article-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
