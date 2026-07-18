<script setup lang="ts">
import { ref, shallowRef, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAuthToken } from '@/utils/authToken'
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
import { ArrowLeft, Save, FileText, Globe, Lock, LayoutGrid, Image, Tag, Plus, X, Sparkles, Settings, ChevronDown } from '@lucide/vue'
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
  is_public: false,
})

const categories = ref<{ name: string }[]>([])
const tagInput = ref('')

const statusOpen = ref(false)
const visibilityOpen = ref(false)
const categoryOpen = ref(false)

const closeAllDropdowns = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.custom-select')) {
    statusOpen.value = false
    visibilityOpen.value = false
    categoryOpen.value = false
  }
}

// wangEditor
const editorRef = shallowRef()
const editorReady = ref(true)
const token = getAuthToken()

const editorConfig = {
  placeholder: '请输入星记内容...',
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
  window.addEventListener('click', closeAllDropdowns)
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
        is_public: data.is_public ?? data.isPublic ?? false,
      }
    }
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('click', closeAllDropdowns)
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
    notify('提示', '请输入星记标题')
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
  <div class="page-container editor-page">
    <!-- Decorative cosmic glowing orbs -->
    <div class="glow-orb orb-1" aria-hidden="true"></div>
    <div class="glow-orb orb-2" aria-hidden="true"></div>

    <section class="page-header">
      <div class="container header-bar">
        <div class="header-left">
          <button class="btn-back" @click="router.push('/articles')">
            <ArrowLeft class="icon-back" />
            <span>返回列表</span>
          </button>
          <div class="header-title-group">
            <span class="header-subtitle">STAR NOTE EDITOR</span>
            <h1 class="header-title">{{ articleId ? '编辑星记' : '创造星记' }}</h1>
          </div>
        </div>
        <div class="header-right">
          <button class="btn-primary btn-save" :disabled="saving" @click="handleSave">
            <span class="btn-content">
              <span v-if="saving" class="btn-spinner"></span>
              <Save v-else class="icon-save" />
              <span>{{ saving ? '保存中...' : '保存星记' }}</span>
            </span>
          </button>
        </div>
      </div>
    </section>

    <div v-if="loading" class="loading-box">
      <div class="loading-spinner-box">
        <span class="loading-spinner"></span>
        <span>正在读取星轨数据...</span>
      </div>
    </div>

    <section v-else class="section-editor">
      <div class="container">
        <div class="editor-layout">
          <!-- Left: Main editor card -->
          <div class="editor-main">
            <div class="main-card ink-glass-card">
              <div class="form-group">
                <label class="form-label">
                  <FileText class="field-icon" />
                  <span>星记标题 *</span>
                </label>
                <input v-model="form.title" class="input-title" placeholder="请输入星记标题，让思想在星空中闪耀..." />
              </div>
              
              <div class="form-group">
                <label class="form-label">
                  <FileText class="field-icon" />
                  <span>星记摘要</span>
                </label>
                <textarea
                  v-model="form.description"
                  class="input-desc"
                  rows="3"
                  placeholder="在此写下星记的简短摘要，便于在星轨中浏览与快速阅读..."
                ></textarea>
              </div>
              
              <div class="form-group editor-editor-group">
                <label class="form-label">
                  <Sparkles class="field-icon" />
                  <span>正文书写</span>
                </label>
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
          </div>

          <!-- Right: Sidebar settings card -->
          <div class="editor-sidebar">
            <div class="sidebar-card ink-glass-card">
              <div class="sidebar-header">
                <Settings class="icon-settings" />
                <h4>发布参数</h4>
              </div>
              
              <div class="field">
                <label class="field-label">
                  <FileText class="field-icon-sm" />
                  <span>发布状态</span>
                </label>
                <div class="custom-select">
                  <div class="custom-select-trigger" @click.stop="statusOpen = !statusOpen">
                    <span>{{ form.status === 'draft' ? '草稿 (Draft)' : '发布 (Publish)' }}</span>
                    <ChevronDown class="arrow-icon" :class="{ 'is-open': statusOpen }" />
                  </div>
                  <Transition name="dropdown-fade">
                    <div v-if="statusOpen" class="custom-select-options">
                      <div 
                        class="custom-select-option" 
                        :class="{ active: form.status === 'draft' }"
                        @click="form.status = 'draft'; statusOpen = false"
                      >
                        草稿 (Draft)
                      </div>
                      <div 
                        class="custom-select-option" 
                        :class="{ active: form.status === 'published' }"
                        @click="form.status = 'published'; statusOpen = false"
                      >
                        发布 (Publish)
                      </div>
                    </div>
                  </Transition>
                </div>
              </div>
              
              <div class="field">
                <label class="field-label">
                  <component :is="form.is_public ? Globe : Lock" class="field-icon-sm" />
                  <span>可见范围</span>
                </label>
                <div class="custom-select">
                  <div class="custom-select-trigger" @click.stop="visibilityOpen = !visibilityOpen">
                    <span>{{ form.is_public ? '公开 (游客可见)' : '私密 (仅自己可见)' }}</span>
                    <ChevronDown class="arrow-icon" :class="{ 'is-open': visibilityOpen }" />
                  </div>
                  <Transition name="dropdown-fade">
                    <div v-if="visibilityOpen" class="custom-select-options">
                      <div 
                        class="custom-select-option" 
                        :class="{ active: !form.is_public }"
                        @click="form.is_public = false; visibilityOpen = false"
                      >
                        私密 (仅自己可见)
                      </div>
                      <div 
                        class="custom-select-option" 
                        :class="{ active: form.is_public }"
                        @click="form.is_public = true; visibilityOpen = false"
                      >
                        公开 (游客可见)
                      </div>
                    </div>
                  </Transition>
                </div>
              </div>
              
              <div class="field">
                <label class="field-label">
                  <LayoutGrid class="field-icon-sm" />
                  <span>所属星域</span>
                </label>
                <div class="custom-select">
                  <div class="custom-select-trigger" @click.stop="categoryOpen = !categoryOpen">
                    <span>{{ form.category || '未分类 (Uncategorized)' }}</span>
                    <ChevronDown class="arrow-icon" :class="{ 'is-open': categoryOpen }" />
                  </div>
                  <Transition name="dropdown-fade">
                    <div v-if="categoryOpen" class="custom-select-options">
                      <div 
                        class="custom-select-option" 
                        :class="{ active: form.category === '' }"
                        @click="form.category = ''; categoryOpen = false"
                      >
                        未分类 (Uncategorized)
                      </div>
                      <div 
                        v-for="cat in categories" 
                        :key="cat.name" 
                        class="custom-select-option" 
                        :class="{ active: form.category === cat.name }"
                        @click="form.category = cat.name; categoryOpen = false"
                      >
                        {{ cat.name }}
                      </div>
                    </div>
                  </Transition>
                </div>
              </div>
              
              <div class="field">
                <label class="field-label">
                  <Image class="field-icon-sm" />
                  <span>封面图片</span>
                </label>
                <div 
                  class="cover-upload-zone"
                  :class="{ 'has-cover': form.coverImage, 'is-uploading': uploading }"
                  @click="($refs.coverInput as HTMLInputElement).click()"
                >
                  <div v-if="form.coverImage" class="cover-image-container">
                    <img
                      :src="form.coverImage"
                      class="cover-img-preview"
                      @error="form.coverImage = ''"
                    />
                    <div class="cover-overlay-actions" @click.stop>
                      <button
                        class="btn-overlay-action"
                        :disabled="uploading"
                        @click="($refs.coverInput as HTMLInputElement).click()"
                      >
                        更换
                      </button>
                      <button
                        class="btn-overlay-action btn-overlay-danger"
                        @click="form.coverImage = ''"
                      >
                        移除
                      </button>
                    </div>
                  </div>
                  
                  <div v-else class="cover-placeholder">
                    <Image class="placeholder-icon" />
                    <span class="placeholder-text">{{ uploading ? '正在上传图片...' : '上传封面图' }}</span>
                    <span class="placeholder-hint">点击或拖拽上传封面</span>
                  </div>
                  
                  <div v-if="uploading" class="upload-progress-overlay">
                    <span class="upload-spinner"></span>
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
                <label class="field-label">
                  <Tag class="field-icon-sm" />
                  <span>凝聚光痕</span>
                </label>
                <div class="tag-input-group">
                  <input
                    v-model="tagInput"
                    placeholder="输入标签并回车..."
                    class="input-tag"
                    @keydown.enter.prevent="addTag"
                  />
                  <button class="btn-add-tag" @click="addTag" title="添加标签">
                    <Plus class="icon-plus" />
                  </button>
                </div>
                <div class="tag-list" v-if="form.tags && form.tags.length > 0">
                  <span v-for="(tag, i) in form.tags" :key="i" class="tag-badge">
                    <span class="tag-text"># {{ tag }}</span>
                    <button class="tag-btn-remove" @click="removeTag(i)" title="移除">
                      <X class="icon-close" />
                    </button>
                  </span>
                </div>
                <span v-else class="tags-empty-hint">暂无关联光痕，回车快速添加</span>
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
.editor-page {
  position: relative;
  min-height: 100vh;
  padding-bottom: 80px;
}

/* Background Glowing Orbs */
.glow-orb {
  position: absolute;
  width: 450px;
  height: 450px;
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
  filter: blur(140px);
  opacity: 0.08;
  transition: all var(--transition-slow);
}
.orb-1 {
  background: var(--accent);
  top: 10%;
  right: 5%;
}
.orb-2 {
  background: var(--warm);
  bottom: 15%;
  left: -100px;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}
.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  padding: 8px 18px;
  cursor: pointer;
  font-size: 14px;
  color: var(--ink-soft);
  font-family: inherit;
  font-weight: 600;
  transition: all var(--transition);
  backdrop-filter: blur(8px);
}
.btn-back:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
  transform: translateX(-2px);
}
.icon-back {
  width: 16px;
  height: 16px;
}

.header-title-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.header-subtitle {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: var(--ink-muted);
  text-transform: uppercase;
}
.header-title {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--ink);
}

.btn-save {
  padding: 10px 24px;
  border-radius: var(--radius-full);
}
.btn-content {
  display: flex;
  align-items: center;
  gap: 8px;
}
.icon-save {
  width: 16px;
  height: 16px;
}
.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-box {
  text-align: center;
  padding: 120px 0;
  color: var(--ink-muted);
  font-size: 0.95rem;
  display: flex;
  justify-content: center;
  align-items: center;
}
.loading-spinner-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.editor-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 32px;
  align-items: flex-start;
  position: relative;
  z-index: 1;
}

.editor-main {
  min-width: 0;
}

.main-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px) saturate(1.2);
  -webkit-backdrop-filter: blur(20px) saturate(1.2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 36px;
  box-shadow: var(--shadow-card);
  transition: all var(--transition);
}
.main-card:hover {
  box-shadow: var(--shadow-card-hover);
  border-color: var(--border-interactive);
}

.form-group {
  margin-bottom: 28px;
}
.form-group:last-child {
  margin-bottom: 0;
}
.form-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--ink-soft);
  letter-spacing: 0.02em;
}
.field-icon {
  width: 16px;
  height: 16px;
  color: var(--accent);
  opacity: 0.85;
}

.input-title {
  width: 100%;
  padding: 16px 20px;
  font-size: 1.45rem;
  font-weight: 700;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--ink);
  outline: none;
  font-family: 'LXGW WenKai', 'Source Serif 4', 'Georgia', 'Noto Serif SC', serif;
  transition: all var(--transition);
}
.input-title:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
  background: var(--surface);
}

.input-desc {
  width: 100%;
  padding: 14px 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--ink);
  outline: none;
  font-family: inherit;
  resize: vertical;
  font-size: 0.92rem;
  line-height: 1.6;
  transition: all var(--transition);
}
.input-desc:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
  background: var(--surface);
}

/* wangEditor transparent styling */
.wangeditor-box {
  --w-e-textarea-bg-color: transparent;
  --w-e-toolbar-bg-color: transparent;
  --w-e-border-color: var(--border);
  --w-e-textarea-color: var(--ink);
  
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--glass-bg);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.03);
  transition: all var(--transition);
}
.wangeditor-box:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
  background: var(--surface);
}

.we-toolbar {
  border-bottom: 1px solid var(--border) !important;
  background: transparent !important;
  padding: 4px !important;
}
.we-editor {
  min-height: 680px;
}
.we-editor :deep(.w-e-text-container) {
  height: 680px !important;
  min-height: 680px !important;
  overflow-y: auto !important;
  background-color: transparent !important;
}
.wangeditor-box :deep(.w-e-toolbar button) {
  color: var(--ink-soft) !important;
}
.wangeditor-box :deep(.w-e-toolbar button:hover) {
  color: var(--ink) !important;
  background-color: var(--surface-hover) !important;
}
.wangeditor-box :deep(.w-e-active) {
  color: var(--accent) !important;
}
.wangeditor-box :deep(.w-e-bar) {
  background-color: transparent !important;
}
.wangeditor-box :deep(.w-e-select-list),
.wangeditor-box :deep(.w-e-drop-panel),
.wangeditor-box :deep(.w-e-menu-panel),
.wangeditor-box :deep(.w-e-panel-container),
.wangeditor-box :deep(.w-e-toolbar-select-list) {
  background: var(--surface) !important;
  background-color: var(--surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-card-hover) !important;
  z-index: 10000 !important;
}
.wangeditor-box :deep(.w-e-select-list *),
.wangeditor-box :deep(.w-e-drop-panel *),
.wangeditor-box :deep(.w-e-menu-panel *) {
  color: var(--ink) !important;
}
.wangeditor-box :deep(.w-e-select-list ul li:hover),
.wangeditor-box :deep(.w-e-select-list li:hover),
.wangeditor-box :deep(.w-e-menu-panel button:hover) {
  background-color: var(--surface-hover) !important;
  color: var(--accent) !important;
}
.wangeditor-box :deep(.w-e-panel-content-color ul li) {
  border: 1px solid var(--border) !important;
}
.wangeditor-box :deep(.w-e-text-placeholder) {
  font-family: inherit !important;
  color: var(--ink-muted) !important;
  top: 20px !important;
  left: 24px !important;
}
.we-editor :deep(.w-e-text-container [contenteditable="true"]) {
  padding: 20px 24px !important;
  font-family: 'LXGW WenKai', 'Source Serif 4', 'Georgia', 'Noto Serif SC', serif !important;
  font-size: 1.05rem !important;
  line-height: 1.85 !important;
}

/* Sidebar styling */
.editor-sidebar {
  position: sticky;
  top: 100px;
}
.sidebar-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px) saturate(1.2);
  -webkit-backdrop-filter: blur(20px) saturate(1.2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-card);
  transition: all var(--transition);
}
.sidebar-card:hover {
  box-shadow: var(--shadow-card-hover);
  border-color: var(--border-interactive);
}
.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 14px;
}
.icon-settings {
  width: 16px;
  height: 16px;
  color: var(--accent);
}
.sidebar-card h4 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ink);
}

.field {
  margin-bottom: 24px;
}
.field:last-child {
  margin-bottom: 0;
}
.field-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--ink-soft);
  margin-bottom: 8px;
}
.field-icon-sm {
  width: 14px;
  height: 14px;
  color: var(--ink-muted);
}

/* Premium Custom Select styles */
.custom-select {
  position: relative;
  width: 100%;
  user-select: none;
}
.custom-select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 11px 16px;
  background: var(--glass-bg);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  color: var(--ink);
  font-size: 0.85rem;
  font-weight: 500;
  transition: all var(--transition);
  cursor: pointer;
  box-sizing: border-box;
}
.custom-select-trigger:hover {
  border-color: var(--border-interactive);
  background: var(--surface-hover);
}
.custom-select-trigger:focus-within,
.custom-select:focus-within .custom-select-trigger {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
  background: var(--surface);
}
.arrow-icon {
  width: 14px;
  height: 14px;
  color: var(--ink-muted);
  transition: transform var(--transition), color var(--transition);
}
.arrow-icon.is-open {
  transform: rotate(180deg);
  color: var(--accent);
}

.custom-select-options {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card-hover);
  z-index: 100;
  max-height: 220px;
  overflow-y: auto;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.custom-select-option {
  padding: 9px 12px;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  color: var(--ink-soft);
  transition: all var(--transition);
  cursor: pointer;
}
.custom-select-option:hover {
  background: var(--accent-soft);
  color: var(--accent);
}
.custom-select-option.active {
  background: var(--accent);
  color: #ffffff;
  font-weight: 600;
}

/* Dropdown Animation */
.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: opacity 0.2s var(--ease-out-quart), transform 0.2s var(--ease-out-quart);
}
.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Premium Upload Cover image zone */
.cover-upload-zone {
  position: relative;
  width: 100%;
  min-height: 140px;
  background: var(--glass-bg);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 2px dashed var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all var(--transition);
}
.cover-upload-zone:hover {
  border-color: var(--accent);
  background: var(--surface-hover);
}
.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  text-align: center;
  color: var(--ink-muted);
  gap: 8px;
}
.placeholder-icon {
  width: 28px;
  height: 28px;
  stroke-width: 1.5;
  transition: transform var(--transition);
}
.cover-upload-zone:hover .placeholder-icon {
  transform: translateY(-2px) scale(1.1);
  color: var(--accent);
}
.placeholder-text {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--ink-soft);
}
.placeholder-hint {
  font-size: 0.72rem;
  color: var(--ink-muted);
}
.cover-image-container {
  width: 100%;
  height: 150px;
  position: relative;
}
.cover-img-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition);
}
.cover-upload-zone:hover .cover-img-preview {
  transform: scale(1.05);
}
.cover-overlay-actions {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  opacity: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  transition: opacity var(--transition);
}
.cover-image-container:hover .cover-overlay-actions {
  opacity: 1;
}
.btn-overlay-action {
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 0.78rem;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.9);
  color: #1a1a1a;
  cursor: pointer;
  border: none;
  transition: all var(--transition);
}
.btn-overlay-action:hover {
  background: #ffffff;
  transform: translateY(-1px);
}
.btn-overlay-danger {
  background: rgba(231, 76, 60, 0.9);
  color: #ffffff;
}
.btn-overlay-danger:hover {
  background: #e74c3c;
}
.upload-progress-overlay {
  position: absolute;
  inset: 0;
  background: rgba(var(--canvas-deep), 0.75);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* Premium Tag styling */
.tag-input-group {
  display: flex;
  position: relative;
  align-items: center;
}
.input-tag {
  flex: 1;
  padding: 10px 14px;
  padding-right: 46px !important;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 0.85rem;
  background: var(--glass-bg);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--ink);
  font-family: inherit;
  outline: none;
  transition: all var(--transition);
}
.input-tag:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
  background: var(--surface);
}
.btn-add-tag {
  position: absolute;
  right: 6px;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  color: var(--accent);
  cursor: pointer;
  transition: all var(--transition);
}
.btn-add-tag:hover {
  background: var(--accent);
  color: #ffffff;
  transform: scale(1.05);
}
.icon-plus {
  width: 16px;
  height: 16px;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.tag-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background: var(--tag-bg);
  border: 1px solid rgba(var(--accent-rgb, 232, 93, 42), 0.1);
  border-radius: var(--radius-full);
  font-size: 0.78rem;
  color: var(--accent);
  font-weight: 600;
  transition: all var(--transition);
  animation: tagAppear 0.3s var(--ease-out-quart);
}
@keyframes tagAppear {
  from {
    opacity: 0;
    transform: scale(0.85) translateY(4px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
.tag-badge:hover {
  background: var(--tag-hover);
  transform: translateY(-1px);
}
.tag-btn-remove {
  background: none;
  border: none;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: inherit;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity var(--transition);
}
.tag-btn-remove:hover {
  opacity: 1;
}
.icon-close {
  width: 12px;
  height: 12px;
}
.tags-empty-hint {
  display: block;
  margin-top: 10px;
  font-size: 0.75rem;
  color: var(--ink-muted);
  font-style: italic;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .editor-layout {
    grid-template-columns: 1fr;
    gap: 24px;
  }
  .editor-sidebar {
    position: static;
  }
}
</style>
