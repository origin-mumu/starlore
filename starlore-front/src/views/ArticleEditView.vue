<script setup lang="ts">
import { ref, shallowRef, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ConfirmModal from '@/components/ConfirmModal.vue'
import {
  getArticleByIdService,
  createArticleService,
  updateArticleService,
  getCategoriesService,
  uploadImage,
  type CreateArticleData,
} from '@/api/article'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()
const articleId = ref(Number(route.params.id) || 0)
const pendingNav = ref<string | null>(null)
const notification = reactive({ show: false, title: '', message: '' })
function notify(title: string, message: string) {
  notification.title = title
  notification.message = message
  notification.show = true
}
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)

const form = ref<CreateArticleData>({
  title: '',
  content: '',
  description: '',
  category: '',
  tags: [],
  coverImage: '',
  status: 'draft',
})

const categories = ref<{ name: string }[]>([])
const tagInput = ref('')

// wangEditor
const editorRef = shallowRef()
const editorReady = ref(true)
const TOKEN_KEY = 'ro_blog_token'
const token = typeof localStorage !== 'undefined' ? localStorage.getItem(TOKEN_KEY) : null

const editorConfig = {
  placeholder: '请输入星迹内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/upload/image',
      fieldName: 'file',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      customInsert(res: any, insertFn: any) {
        const url = res.data?.url
        if (url) insertFn(url)
      },
    },
  },
}
const toolbarConfig = {
  excludeKeys: ['fullScreen'],
}

const handleCreated = (editor: any) => {
  editorRef.value = editor
}

onMounted(async () => {
  try {
    const catRes: any = await getCategoriesService()
    categories.value = catRes.data?.data || catRes.data || []

    if (articleId.value) {
      const res: any = await getArticleByIdService(articleId.value)
      const data = res.data || res
      form.value = {
        title: data.title || '',
        content: data.content || '',
        description: data.description || '',
        category: data.category || '',
        tags: data.tags || [],
        coverImage: data.coverImage || data.cover_image || '',
        status: data.status || 'draft',
      }
    }
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  editorReady.value = false
  const editor = editorRef.value
  if (editor) editor.destroy()
})

const addTag = () => {
  const t = tagInput.value.trim()
  if (t && !form.value.tags!.includes(t)) {
    form.value.tags!.push(t)
  }
  tagInput.value = ''
}

const removeTag = (idx: number) => {
  form.value.tags!.splice(idx, 1)
}

const handleCoverUpload = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const res: any = await uploadImage(file)
    const url = res.data?.url || res.url
    if (url) form.value.coverImage = url
  } catch (err: any) {
    notify('上传失败', '封面上传失败: ' + (err?.message || ''))
  } finally {
    uploading.value = false
  }
}

const handleSave = async () => {
  if (!form.value.title.trim()) {
    notify('提示', '请输入星迹标题')
    return
  }
  // 保存前自动处理未添加的标签
  const pendingTag = tagInput.value.trim()
  if (pendingTag) {
    if (!form.value.tags) form.value.tags = []
    if (!form.value.tags.includes(pendingTag)) {
      form.value.tags.push(pendingTag)
    }
  }
  tagInput.value = ''
  saving.value = true
  try {
    if (articleId.value) {
      await updateArticleService(articleId.value, form.value)
      notify('更新成功', '文章已更新')
    } else {
      const res: any = await createArticleService(form.value)
      const newId = res.data?.data?.id
      if (newId) {
        pendingNav.value = `/articles/edit/${newId}`
        articleId.value = newId
      }
      notify('创建成功', '文章已成功创建')
    }
  } catch {
    notify('保存失败', '保存时发生错误，请重试')
  } finally {
    saving.value = false
  }
}

function handleNotifyConfirm() {
  notification.show = false
  if (pendingNav.value) {
    router.replace(pendingNav.value)
    pendingNav.value = null
  }
}
</script>

<template>
  <div class="page-container">
    <section class="page-header">
      <div class="container header-bar">
        <div class="header-left">
          <button class="btn-back" @click="router.push('/articles')">&larr; 返回</button>
          <h1>{{ articleId ? '编辑星迹' : '新增星迹' }}</h1>
        </div>
        <div class="header-right">
          <button class="btn-primary" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </section>

    <div v-if="loading" class="loading-box">加载中...</div>

    <section v-else class="section-parchment">
      <div class="container">
        <div class="editor-layout">
          <div class="editor-main">
            <div class="form-group">
              <label>星迹标题 *</label>
              <input v-model="form.title" class="input-title" placeholder="请输入星迹标题" />
            </div>
            <div class="form-group">
              <label>描述</label>
              <textarea
                v-model="form.description"
                class="input-desc"
                rows="3"
                placeholder="星迹摘要或描述"
              ></textarea>
            </div>
            <div class="form-group">
              <label>星迹内容</label>
              <div class="wangeditor-box">
                <Toolbar v-if="editorReady" :editor="editorRef" :defaultConfig="toolbarConfig" class="we-toolbar" />
                <Editor
                  v-if="editorReady"
                  :defaultConfig="editorConfig"
                  v-model="form.content"
                  mode="simple"
                  class="we-editor"
                  @onCreated="handleCreated"
                />
              </div>
            </div>
          </div>
          <div class="editor-sidebar">
            <div class="sidebar-card">
              <h4>发布设置</h4>
              <div class="field">
                <label>状态</label>
                <select v-model="form.status">
                  <option value="draft">草稿</option>
                  <option value="published">发布</option>
                </select>
              </div>
              <div class="field">
                <label>星域</label>
                <select v-model="form.category">
                  <option value="">未分类</option>
                  <option v-for="cat in categories" :key="cat.name" :value="cat.name">
                    {{ cat.name }}
                  </option>
                </select>
              </div>
              <div class="field">
                <label>封面图片</label>
                <div class="cover-upload">
                  <img
                    v-if="form.coverImage"
                    :src="form.coverImage"
                    class="cover-preview"
                    @error="form.coverImage = ''"
                  />
                  <div class="cover-actions">
                    <button
                      class="btn-outline btn-sm"
                      :disabled="uploading"
                      @click="($refs.coverInput as HTMLInputElement).click()"
                    >
                      {{ uploading ? '上传中...' : form.coverImage ? '更换图片' : '上传图片' }}
                    </button>
                    <button
                      v-if="form.coverImage"
                      class="btn-cover-remove"
                      @click="form.coverImage = ''"
                    >
                      移除
                    </button>
                  </div>
                  <input
                    ref="coverInput"
                    type="file"
                    accept="image/*"
                    hidden
                    @change="handleCoverUpload"
                  />
                </div>
              </div>
              <div class="field">
                <label>光痕</label>
                <div class="tag-input-row">
                  <input
                    v-model="tagInput"
                    placeholder="输入光痕"
                    @keydown.enter.prevent="addTag"
                  />
                  <button class="btn-outline btn-sm" @click="addTag">添加</button>
                </div>
                <div class="tag-list">
                  <span v-for="(tag, i) in form.tags" :key="i" class="tag-item">
                    {{ tag }}
                    <button class="tag-remove" @click="removeTag(i)">&times;</button>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>

  <ConfirmModal
    :show="notification.show"
    :title="notification.title"
    :message="notification.message"
    type="alert"
    confirm-text="确定"
    @confirm="handleNotifyConfirm"
    @cancel="notification.show = false"
  />
</template>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-left h1 {
  margin: 0;
  font-size: 1.2rem;
}
.btn-back {
  background: none;
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 6px 16px;
  cursor: pointer;
  font-size: 14px;
  color: var(--ink);
  font-family: inherit;
  transition: all 0.2s;
}
.btn-back:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.loading-box {
  text-align: center;
  padding: 80px 0;
  color: var(--ink-muted);
}

.editor-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
  align-items: flex-start;
}

.editor-main {
  min-width: 0;
}

.form-group {
  margin-bottom: 24px;
}
.form-group label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--ink);
}
.input-title {
  width: 100%;
  padding: 12px 16px;
  font-size: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  color: var(--ink);
  outline: none;
  font-family: inherit;
  transition: border 0.2s;
}
.input-title:focus {
  border-color: var(--accent);
}
.input-desc {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  color: var(--ink);
  outline: none;
  font-family: inherit;
  resize: vertical;
  font-size: 0.9rem;
  transition: border 0.2s;
}
.input-desc:focus {
  border-color: var(--accent);
}
/* wangEditor 容器 */
.wangeditor-box {
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface);
}
.we-toolbar {
  border-bottom: 1px solid var(--border) !important;
  background: var(--canvas) !important;
}
.we-editor {
  min-height: 480px;
}
.we-editor :deep(.w-e-text-container) {
  height: 480px !important;
  min-height: 480px !important;
  overflow-y: auto !important;
}

/* Sidebar */
.editor-sidebar {
  position: sticky;
  top: 100px;
}
.sidebar-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
}
.sidebar-card h4 {
  margin: 0 0 16px;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ink);
}
.field {
  margin-bottom: 16px;
}
.field label {
  display: block;
  font-size: 0.78rem;
  font-weight: 500;
  color: var(--ink-muted);
  margin-bottom: 6px;
}
.field select,
.field input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.85rem;
  background: var(--canvas);
  color: var(--ink);
  outline: none;
  font-family: inherit;
  transition: border 0.2s;
}
.field select:focus,
.field input:focus {
  border-color: var(--accent);
}

.tag-input-row {
  display: flex;
  gap: 6px;
}
.tag-input-row input {
  flex: 1;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: rgba(109, 99, 255, 0.08);
  border-radius: 999px;
  font-size: 0.78rem;
  color: var(--accent);
}
.tag-remove {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  opacity: 0.6;
}
.tag-remove:hover {
  opacity: 1;
}

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
.btn-outline:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.btn-sm {
  font-size: 0.8rem;
  padding: 5px 14px;
}

.btn-cover-remove {
  padding: 6px 16px;
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  color: #e74c3c;
  font-family: inherit;
  transition: all 0.2s;
}
.btn-cover-remove:hover {
  border-color: #e74c3c;
  background: rgba(231, 76, 60, 0.06);
}

@media (max-width: 900px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }
  .editor-sidebar {
    position: static;
  }
}
</style>
