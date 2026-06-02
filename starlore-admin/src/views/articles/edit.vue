<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  updateArticleService,
  getArticleByIdService,
  articleCategoryListService,
} from '@/api/article'
import router from '@/router'
import Nav from '@/components/nav.vue'
import '@wangeditor/editor/dist/css/style.css'

import { onBeforeUnmount, shallowRef } from 'vue'
// @ts-ignore
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor } from '@wangeditor/editor'

const route = useRoute()
const title = ref('')
const category = ref('')
const categories = ref<{ id: number; name: string }[]>([])

onMounted(() => {
  articleCategoryListService().then((res) => {
    categories.value = res.data
  })
})

const status = ref('')
const editorRef = shallowRef()
const editorReady = ref(true)
const valueHtml = ref('')
const mode = ref('default')

const toolbarConfig = {}
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/upload/image',
      fieldName: 'file',
      customInsert(res: any, insertFn: any) {
        const url = res.data?.url
        if (url) insertFn(url)
      },
    },
  },
}

onBeforeUnmount(() => {
  editorReady.value = false
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

const handleCreated = (editor: IDomEditor) => {
  editorRef.value = editor
}

const createArticle = async () => {
  try {
    const editorContent = valueHtml.value
    await updateArticleService(Number(route.params.id), {
      title: title.value,
      content: editorContent,
      category: category.value,
      status: status.value,
    })
    alert('星迹编辑成功')
    router.push({ name: 'articles' })
  } catch (error) {
    console.error('编辑文章失败:', error)
    alert('编辑星迹失败')
  }
}

const loadArticle = async () => {
  try {
    const response = await getArticleByIdService(Number(route.params.id))
    const article = response.data
    title.value = article.title
    valueHtml.value = article.content
    category.value = article.category
    status.value = article.status
  } catch (error) {
    console.error('加载文章失败:', error)
    alert('加载星迹失败')
  }
}

onMounted(() => {
  loadArticle()
})
</script>

<template>
  <div class="home-layout">
    <div class="sidebar-container">
      <Nav></Nav>
    </div>
    <div class="content-container">
      <div class="content-box">
        <div class="top">
          <div class="back" @click="router.push({ name: 'articles' })">⬅</div>
          <div class="page-title">编辑星迹</div>
          <div class="save" @click="createArticle">保存</div>
        </div>

        <div class="article-form">
          <label class="form-label">
            标题
            <input class="form-input" type="text" v-model="title" placeholder="请输入星迹标题" />
          </label>
          <label class="form-label">
            星域
            <select class="form-select" v-model="category">
              <option value="" disabled>请选择星域</option>
              <option v-for="item in categories" :key="item.id" :value="item.name">
                {{ item.name }}
              </option>
            </select>
          </label>

          <label class="form-label">
            状态
            <select class="form-select" v-model="status">
              <option value="draft">草稿</option>
              <option value="published">已发布</option>
            </select>
          </label>

          <div class="content-input">
            <div class="editor-wrapper">
              <Toolbar
                v-if="editorReady"
                class="editor-toolbar"
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                :mode="mode"
              />
              <Editor
                v-if="editorReady"
                class="editor-textarea"
                v-model="valueHtml"
                :defaultConfig="editorConfig"
                :mode="mode"
                @onCreated="handleCreated"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.top {
  display: flex;
  align-items: center;
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border);
}

.back {
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  background: var(--canvas-deep);
  margin-right: var(--space-3);
  transition: all var(--transition);
  border: 1px solid var(--border);
}

.back:hover {
  background: var(--accent-soft);
  border-color: var(--accent);
}

.page-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.02em;
}

.save {
  margin-left: auto;
  padding: 8px 20px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  background: var(--accent);
  color: #FDFBF5;
  font-weight: 600;
  font-size: 0.9rem;
  transition: all var(--transition);
  box-shadow: var(--shadow-button);
}

.save:hover {
  background: var(--accent-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-button-hover);
}

.article-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.form-label {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--ink);
}

.form-input,
.form-select {
  padding: 10px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  font-size: 0.95rem;
  font-family: inherit;
  background: var(--surface);
  color: var(--ink);
  transition: border-color var(--transition), box-shadow var(--transition);
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px oklch(0.55 0.15 35 / 0.1);
}

.form-input::placeholder {
  color: var(--ink-muted);
}

.editor-wrapper {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.editor-toolbar {
  border-bottom: 1px solid var(--border);
  background-color: var(--canvas-deep);
}

.editor-textarea {
  height: 400px;
  overflow-y: hidden;
  min-height: 300px;
}

@media (max-width: 768px) {
  .top {
    flex-wrap: wrap;
    gap: var(--space-3);
  }

  .save {
    margin-left: 0;
  }
}
</style>
