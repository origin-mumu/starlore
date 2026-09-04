<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ConfirmModal from '@/components/ConfirmModal.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { articleContentToMarkdown } from '@/utils/articleContent'
import {
  getArticleByIdService,
  createArticleService,
  updateArticleService,
  getCategoriesService,
  uploadImage,
  type CreateArticleData,
} from '@/api/article'
import { ArrowLeft, Save, FileText, Globe, Lock, LayoutGrid, Image, Tag, Plus, X, Settings, ChevronDown } from '@lucide/vue'

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
  status: 'published',
  is_public: false,
})

const categories = ref<{ name: string }[]>([])
const tagInput = ref('')

const visibilityOpen = ref(false)
const categoryOpen = ref(false)

const closeAllDropdowns = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.custom-select')) {
    visibilityOpen.value = false
    categoryOpen.value = false
  }
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
        content: articleContentToMarkdown(data.content || ''),
        description: data.description || '',
        category: data.category || '',
        tags: data.tags || [],
        coverImage: data.coverImage || data.cover_image || '',
        status: data.status === 'draft' ? 'published' : (data.status || 'published'),
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
              
              <div class="form-group editor-editor-group">
                <MarkdownEditor v-model="form.content" />
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
                  <span>文章摘要</span>
                </label>
                <textarea
                  v-model="form.description"
                  class="input-desc"
                  rows="3"
                  placeholder="为这篇星记写一段简短摘要..."
                ></textarea>
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

<style scoped src="./ArticleEditView.css"></style>
