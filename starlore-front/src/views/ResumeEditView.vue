<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getResume,
  updateResume,
  uploadImage,
  exportResumePdf,
  type ResumeData,
} from '@/api/resume'
import RichEditor from '@/components/RichEditor.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { useUserStore } from '@/stores/user'
import { sanitizeHtml } from '@/utils/sanitize'
import { Sparkles } from '@lucide/vue'
import HarnessChatPanel from '@/views/harness/components/HarnessChatPanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notification = reactive({ show: false, title: '', message: '' })
function notify(title: string, message: string) {
  notification.title = title
  notification.message = message
  notification.show = true
}

const hasAccess = computed(() => {
  const r = userStore.role
  return r === 'member' || r === 'admin'
})
const resumeId = ref(Number(route.params.id) || 0)
const loading = ref(true)
const saving = ref(false)

// ============= Resume Data ==============
const form = reactive({
  title: '全栈开发实习生 · 张三',
  template: 'classic',
  name: '张三',
  jobTitle: '全栈开发实习生',
  phone: '138-0000-0000',
  email: 'zhangsan@example.com',
  photoUrl: '',
})

interface ResumeContent {
  education: { school: string; major: string; degree: string; period: string; detail: string }[]
  experience: { company: string; position: string; period: string; detail: string }[]
  projects: { name: string; role: string; period: string; detail: string }[]
  skills: string
  spacing: { moduleGap: number; lineHeight: number; fontSize: number }
}

const defaultSpacing = { moduleGap: 25, lineHeight: 6, fontSize: 14 }

const content = reactive<ResumeContent>({
  education: [],
  experience: [],
  projects: [],
  skills: '',
  spacing: { ...defaultSpacing },
})

const photoPreview = ref('')

// Initial preview height before the content measurer has completed.
const A4_PX_H = 1123

// 测量用容器ref
const contentMeasurer = ref<HTMLElement>()
const totalContentHeight = ref(0)

const pageCount = computed(() => 1)
const previewPageHeight = computed(() => totalContentHeight.value || A4_PX_H)
const currentPage = ref(1)

// 测量内容总高度
const measureContent = () => {
  if (!contentMeasurer.value) return
  totalContentHeight.value = contentMeasurer.value.scrollHeight
}

let contentResizeObserver: ResizeObserver | undefined
const observeContentHeight = () => {
  if (!contentMeasurer.value) return
  contentResizeObserver?.disconnect()
  contentResizeObserver = new ResizeObserver(measureContent)
  contentResizeObserver.observe(contentMeasurer.value)
}

onBeforeUnmount(() => contentResizeObserver?.disconnect())

onMounted(async () => {
  if (!hasAccess.value) {
    loading.value = false
    return
  }
  if (!resumeId.value) {
    loading.value = false
    await nextTick()
    measureContent()
    observeContentHeight()
    return
  }
  try {
    const res: any = await getResume(resumeId.value)
    const data = res.data as ResumeData
    if (data) {
      form.title = data.title || ''
      form.template = data.template || 'classic'
      form.name = data.name || ''
      form.jobTitle = data.jobTitle || ''
      form.phone = data.phone || ''
      form.email = data.email || ''
      form.photoUrl = data.photoUrl || ''
      photoPreview.value = data.photoUrl || ''
      if (data.content) {
        try {
          const parsed = JSON.parse(data.content)
          // 仅复制 ResumeContent 的有效字段，防止旧数据污染
          const validFields = ['education', 'experience', 'projects', 'skills', 'spacing']
          const clean: Record<string, any> = {}
          for (const field of validFields) {
            if (field in parsed) clean[field] = parsed[field]
          }
          Object.assign(content, clean)
          // 兼容旧数据：skills 从 string[] 迁移为富文本 string
          if (Array.isArray(content.skills)) {
            content.skills = (content.skills as unknown as string[])
              .map((s: string) => `<p>${s}</p>`)
              .join('')
          }
          // 兼容旧数据没有 spacing 字段
          if (!content.spacing) content.spacing = { ...defaultSpacing }
        } catch {
          /* ignore */
        }
      }
    }
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
  await nextTick()
  measureContent()
  observeContentHeight()
})

const handleSave = async () => {
  saving.value = true
  try {
    const saveContent = { ...content, spacing: { ...content.spacing } }
    await updateResume({
      id: resumeId.value,
      title: form.title,
      template: form.template,
      name: form.name,
      jobTitle: form.jobTitle,
      phone: form.phone,
      email: form.email,
      photoUrl: form.photoUrl,
      content: JSON.stringify(saveContent),
    })
    notify('保存成功', '简历已保存')
  } catch {
    /* ignore */
  } finally {
    saving.value = false
  }
}

const exporting = ref(false)

const handleExport = async () => {
  if (!resumeId.value) {
    notify('导出失败', '请先保存简历')
    return
  }
  exporting.value = true
  try {
    // 先保存当前预览的排版设置
    const saveContent = { ...content, spacing: { ...content.spacing } }
    await updateResume({
      id: resumeId.value,
      title: form.title,
      template: form.template,
      name: form.name,
      jobTitle: form.jobTitle,
      phone: form.phone,
      email: form.email,
      photoUrl: form.photoUrl,
      content: JSON.stringify(saveContent),
    })
    // 再导出
    const blob = await exportResumePdf(resumeId.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${form.title || '简历'}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('PDF 导出失败:', e)
    notify('导出失败', e instanceof Error ? e.message : '导出 PDF 失败，请重试')
  } finally {
    exporting.value = false
  }
}

const MAX_FILE_SIZE = 2 * 1024 * 1024 // 2MB
const COMPRESS_TARGET = 500 * 1024 // 500KB
const MAX_DIMENSION = 800 // 最长边 800px

const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => {
      const img = new Image()
      img.onload = () => {
        // 先计算缩放后的尺寸
        let w = img.width
        let h = img.height
        if (w > MAX_DIMENSION || h > MAX_DIMENSION) {
          const scale = MAX_DIMENSION / Math.max(w, h)
          w = Math.round(w * scale)
          h = Math.round(h * scale)
        }

        const canvas = document.createElement('canvas')
        canvas.width = w
        canvas.height = h
        const ctx = canvas.getContext('2d')!
        ctx.drawImage(img, 0, 0, w, h)

        // 从 0.9 开始逐步降低质量
        let quality = 0.9
        const tryCompress = () => {
          canvas.toBlob(
            b => {
              if (!b) {
                reject(new Error('压缩失败'))
                return
              }
              if (b.size > COMPRESS_TARGET && quality > 0.3) {
                quality -= 0.1
                tryCompress()
              } else {
                resolve(new File([b], file.name, { type: 'image/jpeg' }))
              }
            },
            'image/jpeg',
            quality
          )
        }
        tryCompress()
      }
      img.onerror = () => reject(new Error('图片加载失败'))
      img.src = e.target?.result as string
    }
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsDataURL(file)
  })
}

const handlePhotoUpload = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 大文件提示
  if (file.size > MAX_FILE_SIZE) {
    notify(
      '图片过大',
      `当前图片 ${(file.size / 1024 / 1024).toFixed(1)}MB，将自动压缩至 500KB 以内`
    )
  }

  try {
    // 压缩图片
    const compressed = await compressImage(file)
    const res: any = await uploadImage(compressed)
    const url = res.data?.url
    if (url) {
      form.photoUrl = url
      photoPreview.value = url
    }
  } catch (err: any) {
    notify('上传失败', '上传失败: ' + (err?.message || '请检查后端服务是否启动'))
  }
}

// Add / Remove helpers
const addItem = (field: 'education' | 'experience' | 'projects') => {
  const empty: any = { school: '', major: '', degree: '', period: '', detail: '' }
  if (field === 'experience')
    Object.assign(empty, { company: '', position: '', period: '', detail: '' })
  if (field === 'projects') Object.assign(empty, { name: '', role: '', period: '', detail: '' })
  content[field].push(empty)
}

const removeItem = (field: 'education' | 'experience' | 'projects', idx: number) => {
  content[field].splice(idx, 1)
}

// Preview computed
const previewData = computed(() => ({
  ...form,
  photoUrl: photoPreview.value,
  ...content,
}))

// 分页滚动
const onPageScroll = (e: Event) => {
  const target = e.target as HTMLElement
  const scrollTop = target.scrollTop
  currentPage.value = Math.min(pageCount.value, Math.floor(scrollTop / A4_PX_H) + 1)
}

const scrollToPage = (dir: -1 | 1) => {
  const container = document.querySelector('.pages-container') as HTMLElement
  if (!container) return
  const next = currentPage.value + dir
  if (next < 1 || next > pageCount.value) return
  container.scrollTo({ top: (next - 1) * A4_PX_H, behavior: 'smooth' })
  currentPage.value = next
}

// 编辑模块导航
const activeModule = ref('spacing')
const MODULES = [
  { key: 'spacing', label: '排版设置' },
  { key: 'basic', label: '基本信息' },
  { key: 'education', label: '教育背景' },
  { key: 'experience', label: '实习经历' },
  { key: 'projects', label: '项目经历' },
  { key: 'skills', label: '专业技能' },
]

// 间距样式计算
const spacingStyle = computed(() => ({
  '--module-gap': `${content.spacing.moduleGap}px`,
  '--line-gap': `${content.spacing.lineHeight}px`,
  '--font-size': `${content.spacing.fontSize}px`,
}))

// ─── 视窗分屏滑轨状态 ───
// 'ai-closed' (常规预览+编辑)
// 'preview-focus' (主视角是预览+编辑，AI在右侧露边)
// 'ai-focus' (主视角是编辑+AI，预览在左侧露边)
const viewStage = ref<'preview-focus' | 'ai-focus' | 'ai-closed'>('ai-closed')

const toggleAiStage = () => {
  if (viewStage.value === 'ai-closed') {
    viewStage.value = 'ai-focus'
  } else {
    viewStage.value = 'ai-closed'
  }
}

// 组装注入 AI 卡片的简历上下文
const resumeContext = computed(() => {
  const parts: string[] = []
  parts.push(`【基本信息】\n姓名：${form.name || '未填'} | 求职意向：${form.jobTitle || '未填'}\n联系方式：${form.phone || ''} | ${form.email || ''}`)
  if (content.skills) {
    parts.push(`【专业技能】\n${content.skills}`)
  }
  if (content.experience?.length) {
    const expText = content.experience
      .map((e, idx) => `${idx + 1}. 公司：${e.company} | 岗位：${e.position} | 时间：${e.period}\n工作详情：\n${e.detail}`)
      .join('\n\n')
    parts.push(`【实习/工作经历】\n${expText}`)
  }
  if (content.projects?.length) {
    const prjText = content.projects
      .map((p, idx) => `${idx + 1}. 项目名：${p.name} | 角色：${p.role} | 时间：${p.period}\n项目详情：\n${p.detail}`)
      .join('\n\n')
    parts.push(`【项目经历】\n${prjText}`)
  }
  if (content.education?.length) {
    const eduText = content.education
      .map((ed) => `学校：${ed.school} | 专业：${ed.major} | 学历：${ed.degree} | 时间：${ed.period}\n详情：${ed.detail}`)
      .join('\n')
    parts.push(`【教育背景】\n${eduText}`)
  }
  return parts.join('\n\n')
})

const resumeQuickPrompts = [
  '请用 STAR 法则帮我重写项目亮点并量化成果',
  '针对该求职意向诊断简历的 3 个薄弱点与改进建议',
  '帮我提炼一份匹配该求职意向的硬核专业技能清单',
  '帮我润色实习经历中的专业术语与业务深度',
]
</script>

<template>
  <div class="page-container">
    <!-- Access denied -->
    <div v-if="!hasAccess" class="access-denied">
      <div class="access-denied-content">
        <div class="access-denied-icon">&#128274;</div>
        <h2>仅会员开放</h2>
        <p>简历功能仅对会员和管理员开放</p>
        <router-link to="/" class="btn-primary">返回首页</router-link>
      </div>
    </div>

    <template v-else>
      <section class="page-header">
        <div class="container header-bar">
          <div class="header-left">
            <button class="btn-back" @click="router.push('/resume')">&larr; 返回</button>
            <h1>{{ resumeId ? '编辑简历' : '新建简历' }}</h1>
          </div>
          <div class="header-right">
            <!-- ✨ AI 智能润色按钮 -->
            <button
              type="button"
              class="btn-outline btn-ai-toggle"
              :class="{ 'is-active': viewStage !== 'ai-closed' }"
              @click="toggleAiStage"
            >
              <Sparkles :size="15" />
              <span>{{ viewStage === 'ai-closed' ? 'AI 润色助手' : '收起 AI' }}</span>
            </button>
            <button class="btn-outline btn-export" :disabled="exporting" @click="handleExport">
              {{ exporting ? '导出中...' : '导出 PDF' }}
            </button>
            <button class="btn-outline btn-export" :disabled="saving" @click="handleSave">
              {{ saving ? '保存中...' : '保存简历' }}
            </button>
          </div>
        </div>
      </section>

      <div v-if="loading" class="loading-box">加载中...</div>

      <section v-else class="editor-section">
        <div class="resume-stage-viewport" :class="`stage--${viewStage}`">
          <div class="resume-panels-track">
            <!-- 1. LEFT: 分页预览卡片 -->
            <div
              class="track-panel preview-panel"
              :class="{ 'is-peeking': viewStage === 'ai-focus' }"
              @click="viewStage === 'ai-focus' && (viewStage = 'preview-focus')"
            >
              <!-- 留出边缘时的点击切回提示 -->
              <div v-if="viewStage === 'ai-focus'" class="peek-tab-overlay" title="点击切回预览">
                <span class="peek-tab-pill">📄 点击切换至预览</span>
              </div>

              <div class="preview-header">
                简历预览
                <span v-if="pageCount > 1" class="page-label"
                  >{{ currentPage }} / {{ pageCount }}</span
                >
              </div>

            <!-- 分页滚动容器 -->
            <div class="pages-container" @scroll="onPageScroll">
              <div
                v-for="(_, idx) in pageCount"
                :key="idx"
                class="page-frame"
                :style="{ height: `${previewPageHeight}px` }"
              >
                <div
                  class="resume-page"
                  :style="spacingStyle"
                >
                  <!-- 头像右上角 -->
                  <div
                    class="avatar-box clickable-section"
                    :style="
                      previewData.photoUrl
                        ? {
                            backgroundImage: `url(${previewData.photoUrl})`,
                            backgroundSize: 'cover',
                            backgroundPosition: 'center',
                          }
                        : {}
                    "
                    @click="activeModule = 'basic'"
                  >
                    <div v-if="!previewData.photoUrl" class="avatar-placeholder">&#128100;</div>
                  </div>

                  <!-- 头部信息 -->
                  <div
                    class="rp-header clickable-section"
                    :class="{ 'has-avatar': !!previewData.photoUrl }"
                    @click="activeModule = 'basic'"
                  >
                    <h1 class="rp-name">{{ previewData.name || '姓名' }}</h1>
                    <div v-if="previewData.jobTitle?.trim()" class="rp-job-intent">
                      求职意向：{{ previewData.jobTitle }}
                    </div>
                    <div class="rp-contact">
                      {{ previewData.phone || '电话' }} | {{ previewData.email || '邮箱' }}
                    </div>
                  </div>

                  <!-- 教育背景 -->
                  <section
                    v-if="previewData.education.length"
                    class="rp-section clickable-section"
                    @click="activeModule = 'education'"
                  >
                    <div class="rp-section-title">教育背景</div>
                    <div v-for="(item, i) in previewData.education" :key="i" class="rp-edu-item">
                      <div class="rp-edu-header">
                        <span class="item-name"
                          ><strong
                            >{{ item.school || '(学校)' }} -- {{ item.major || '(专业)' }}</strong
                          >
                          <span class="text-secondary">({{ item.degree || '学历' }})</span></span
                        >
                        <span class="item-date text-secondary">{{ item.period }}</span>
                      </div>
                      <div class="rp-edu-content" v-html="sanitizeHtml(item.detail || '')"></div>
                    </div>
                  </section>

                  <!-- 实习/工作经历 -->
                  <section
                    v-if="previewData.experience.length"
                    class="rp-section clickable-section"
                    @click="activeModule = 'experience'"
                  >
                    <div class="rp-section-title">实习经历</div>
                    <div v-for="(item, i) in previewData.experience" :key="i" class="rp-exp-item">
                      <div class="rp-exp-header">
                        <span class="item-name"
                          ><strong
                            >{{ item.company || '(公司)' }} --
                            {{ item.position || '(职位)' }}</strong
                          ></span
                        >
                        <span class="item-date text-secondary">{{ item.period }}</span>
                      </div>
                      <div class="rp-exp-content" v-html="sanitizeHtml(item.detail)" v-if="item.detail"></div>
                    </div>
                  </section>

                  <!-- 项目经历 -->
                  <section
                    v-if="previewData.projects.length"
                    class="rp-section clickable-section"
                    @click="activeModule = 'projects'"
                  >
                    <div class="rp-section-title">项目经历</div>
                    <div v-for="(item, i) in previewData.projects" :key="i" class="rp-project-item">
                      <div class="rp-project-header">
                        <span class="item-name"
                          ><strong
                            >{{ item.name || '(项目名称)' }} -- {{ item.role || '(角色)' }}</strong
                          ></span
                        >
                        <span class="item-date text-secondary">{{ item.period }}</span>
                      </div>
                      <div class="rp-project-content" v-html="sanitizeHtml(item.detail)" v-if="item.detail"></div>
                    </div>
                  </section>

                  <!-- 专业技能 -->
                  <section
                    v-if="previewData.skills"
                    class="rp-section clickable-section"
                    @click="activeModule = 'skills'"
                  >
                    <div class="rp-section-title">专业技能</div>
                    <div class="rp-skills-content" v-html="sanitizeHtml(previewData.skills)"></div>
                  </section>
                </div>
              </div>
            </div>

            <!-- 翻页按钮 -->
            <div v-if="pageCount > 1" class="page-nav">
              <button class="page-arrow" :disabled="currentPage <= 1" @click="scrollToPage(-1)">
                &lsaquo;
              </button>
              <span class="page-dot">{{ currentPage }} / {{ pageCount }}</span>
              <button
                class="page-arrow"
                :disabled="currentPage >= pageCount"
                @click="scrollToPage(1)"
              >
                &rsaquo;
              </button>
            </div>
          </div>

          <!-- 2. CENTER: 编辑面板 -->
          <div class="track-panel edit-panel">
            <div class="edit-menu">
              <button
                v-for="m in MODULES"
                :key="m.key"
                :class="['edit-menu-item', { active: activeModule === m.key }]"
                @click="activeModule = m.key"
              >
                {{ m.label }}
              </button>
            </div>
            <div class="edit-content">
              <!-- 排版设置 -->
              <div v-if="activeModule === 'spacing'" class="edit-section spacing-section">
                <h3>排版设置</h3>
                <div class="spacing-field">
                  <label class="spacing-label">
                    <span>字体大小</span>
                    <div class="spacing-input-group">
                      <input
                        type="number"
                        min="12"
                        max="20"
                        v-model.number="content.spacing.fontSize"
                        class="spacing-number"
                      />
                      <span class="spacing-unit">px</span>
                    </div>
                  </label>
                  <input
                    type="range"
                    min="12"
                    max="20"
                    v-model.number="content.spacing.fontSize"
                    class="spacing-slider"
                  />
                </div>
                <div class="spacing-field">
                  <label class="spacing-label">
                    <span>模块间距</span>
                    <div class="spacing-input-group">
                      <input
                        type="number"
                        min="8"
                        max="40"
                        v-model.number="content.spacing.moduleGap"
                        class="spacing-number"
                      />
                      <span class="spacing-unit">px</span>
                    </div>
                  </label>
                  <input
                    type="range"
                    min="8"
                    max="40"
                    v-model.number="content.spacing.moduleGap"
                    class="spacing-slider"
                  />
                </div>
                <div class="spacing-field">
                  <label class="spacing-label">
                    <span>行间距</span>
                    <div class="spacing-input-group">
                      <input
                        type="number"
                        min="0"
                        max="20"
                        v-model.number="content.spacing.lineHeight"
                        class="spacing-number"
                      />
                      <span class="spacing-unit">px</span>
                    </div>
                  </label>
                  <input
                    type="range"
                    min="0"
                    max="20"
                    v-model.number="content.spacing.lineHeight"
                    class="spacing-slider"
                  />
                </div>
              </div>

              <!-- Basic Info -->
              <div v-if="activeModule === 'basic'" class="edit-section">
                <h3>基本信息</h3>
                <div class="form-row">
                  <div class="form-field">
                    <label>简历标题</label>
                    <input v-model="form.title" placeholder="我的简历" />
                  </div>
                </div>
                <div class="form-row row-2">
                  <div class="form-field">
                    <label>姓名 *</label>
                    <input v-model="form.name" placeholder="请输入姓名" />
                  </div>
                  <div class="form-field">
                    <label>求职意向</label>
                    <input v-model="form.jobTitle" placeholder="前端开发工程师" />
                  </div>
                </div>
                <div class="form-row row-2">
                  <div class="form-field">
                    <label>电话</label>
                    <input v-model="form.phone" placeholder="手机号" />
                  </div>
                  <div class="form-field">
                    <label>邮箱</label>
                    <input v-model="form.email" placeholder="email@example.com" />
                  </div>
                </div>
                <div class="form-field">
                  <label>照片</label>
                  <div class="photo-upload">
                    <img v-if="photoPreview" :src="photoPreview" class="photo-thumb" />
                    <button
                      class="btn-outline btn-sm"
                      @click="($refs.photoInput as HTMLInputElement).click()"
                    >
                      {{ photoPreview ? '更换照片' : '上传照片' }}
                    </button>
                    <input
                      ref="photoInput"
                      type="file"
                      accept="image/*"
                      hidden
                      @change="handlePhotoUpload"
                    />
                  </div>
                </div>
              </div>

              <!-- Education -->
              <div v-if="activeModule === 'education'" class="edit-section">
                <div class="section-header">
                  <h3>教育背景</h3>
                  <button class="btn-outline btn-sm" @click="addItem('education')">+ 添加</button>
                </div>
                <div v-for="(item, i) in content.education" :key="i" class="sub-card">
                  <div class="form-row row-2">
                    <div class="form-field"><label>学校</label><input v-model="item.school" /></div>
                    <div class="form-field"><label>专业</label><input v-model="item.major" /></div>
                  </div>
                  <div class="form-row row-2">
                    <div class="form-field">
                      <label>学历</label><input v-model="item.degree" placeholder="本科" />
                    </div>
                    <div class="form-field">
                      <label>时间</label
                      ><input v-model="item.period" placeholder="2023-09 ~ 2027-06" />
                    </div>
                  </div>
                  <div class="form-field">
                    <label>详细信息</label>
                    <RichEditor
                      v-model="item.detail"
                      :minHeight="400"
                      placeholder="输入详细内容..."
                    />
                  </div>
                  <button class="btn-remove" @click="removeItem('education', i)">删除</button>
                </div>
              </div>

              <!-- Experience -->
              <div v-if="activeModule === 'experience'" class="edit-section">
                <div class="section-header">
                  <h3>实习/工作经历</h3>
                  <button class="btn-outline btn-sm" @click="addItem('experience')">+ 添加</button>
                </div>
                <div v-for="(item, i) in content.experience" :key="i" class="sub-card">
                  <div class="form-row row-2">
                    <div class="form-field">
                      <label>公司</label><input v-model="item.company" />
                    </div>
                    <div class="form-field">
                      <label>职位</label><input v-model="item.position" />
                    </div>
                  </div>
                  <div class="form-row row-2">
                    <div class="form-field"><label>时间</label><input v-model="item.period" /></div>
                  </div>
                  <div class="form-field">
                    <label>工作内容</label>
                    <RichEditor
                      v-model="item.detail"
                      :minHeight="400"
                      placeholder="输入工作内容..."
                    />
                  </div>
                  <button class="btn-remove" @click="removeItem('experience', i)">删除</button>
                </div>
              </div>

              <!-- Projects -->
              <div v-if="activeModule === 'projects'" class="edit-section">
                <div class="section-header">
                  <h3>项目经历</h3>
                  <button class="btn-outline btn-sm" @click="addItem('projects')">+ 添加</button>
                </div>
                <div v-for="(item, i) in content.projects" :key="i" class="sub-card">
                  <div class="form-row row-2">
                    <div class="form-field">
                      <label>项目名称</label><input v-model="item.name" />
                    </div>
                    <div class="form-field"><label>角色</label><input v-model="item.role" /></div>
                  </div>
                  <div class="form-row row-2">
                    <div class="form-field"><label>时间</label><input v-model="item.period" /></div>
                  </div>
                  <div class="form-field">
                    <label>项目描述</label>
                    <RichEditor
                      v-model="item.detail"
                      :minHeight="400"
                      placeholder="输入项目描述..."
                    />
                  </div>
                  <button class="btn-remove" @click="removeItem('projects', i)">删除</button>
                </div>
              </div>

              <!-- Skills -->
              <div v-if="activeModule === 'skills'" class="edit-section">
                <h3>专业技能</h3>
                <div class="form-field">
                  <RichEditor
                    v-model="content.skills"
                    :minHeight="400"
                    placeholder="输入专业技能..."
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 3. RIGHT: AI 智能润色卡片 (复用通用 HarnessChatPanel) -->
          <div
            v-if="viewStage !== 'ai-closed'"
            class="track-panel ai-panel"
            :class="{ 'is-peeking': viewStage === 'preview-focus' }"
            @click="viewStage === 'preview-focus' && (viewStage = 'ai-focus')"
          >
            <!-- 留出边缘时的点击展开提示 -->
            <div v-if="viewStage === 'preview-focus'" class="peek-tab-overlay" title="点击切换至 AI 润色">
              <span class="peek-tab-pill">✨ 点击切换至 AI 润色</span>
            </div>

            <HarnessChatPanel
              :context="resumeContext"
              :context-title="form.title || form.name || '我的简历'"
              :quick-prompts="resumeQuickPrompts"
              :show-close="true"
              @close="viewStage = 'ai-closed'"
            />
          </div>
        </div>
      </div>
    </section>

      <!-- 隐藏的测量容器 -->
      <div ref="contentMeasurer" class="content-measurer" aria-hidden="true">
        <div class="resume-page measurer-inner" :style="spacingStyle">
          <div
            class="avatar-box"
            :style="
              photoPreview
                ? {
                    backgroundImage: `url(${photoPreview})`,
                    backgroundSize: 'cover',
                    backgroundPosition: 'center',
                  }
                : {}
            "
          >
            <div v-if="!photoPreview" class="avatar-placeholder">&#128100;</div>
          </div>
          <div class="rp-header" :class="{ 'has-avatar': !!photoPreview }">
            <h1 class="rp-name">{{ previewData.name || '姓名' }}</h1>
            <div v-if="previewData.jobTitle?.trim()" class="rp-job-intent">
              求职意向：{{ previewData.jobTitle }}
            </div>
            <div class="rp-contact">
              {{ previewData.phone || '电话' }} | {{ previewData.email || '邮箱' }}
            </div>
          </div>
          <section v-if="previewData.education.length" class="rp-section">
            <div class="rp-section-title">教育背景</div>
            <div v-for="(item, i) in previewData.education" :key="i" class="rp-edu-item">
              <div class="rp-edu-header">
                <span
                  >{{ item.school || '(学校)' }} -- {{ item.major || '(专业)' }} ({{
                    item.degree || '学历'
                  }})</span
                >
                <span>{{ item.period }}</span>
              </div>
              <div class="rp-edu-content" v-html="sanitizeHtml(item.detail || '')"></div>
            </div>
          </section>
          <section v-if="previewData.experience.length" class="rp-section">
            <div class="rp-section-title">实习经历</div>
            <div v-for="(item, i) in previewData.experience" :key="i" class="rp-exp-item">
              <div class="rp-exp-header">
                <span>{{ item.company || '(公司)' }} -- {{ item.position || '(职位)' }}</span>
                <span>{{ item.period }}</span>
              </div>
              <div class="rp-exp-content" v-html="sanitizeHtml(item.detail)" v-if="item.detail"></div>
            </div>
          </section>
          <section v-if="previewData.projects.length" class="rp-section">
            <div class="rp-section-title">项目经历</div>
            <div v-for="(item, i) in previewData.projects" :key="i" class="rp-project-item">
              <div class="rp-project-header">
                <span>{{ item.name || '(项目名称)' }} -- {{ item.role || '(角色)' }}</span>
                <span>{{ item.period }}</span>
              </div>
              <div class="rp-project-content" v-html="sanitizeHtml(item.detail)" v-if="item.detail"></div>
            </div>
          </section>
          <section v-if="previewData.skills" class="rp-section">
            <div class="rp-section-title">专业技能</div>
            <div class="rp-skills-content" v-html="sanitizeHtml(previewData.skills)"></div>
          </section>
        </div>
      </div>
    </template>
  </div>

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

<style scoped src="./ResumeEditView.css"></style>

<style>
.rp-exp-content ul,
.rp-project-content ul,
.rp-skills-content ul {
  padding-left: 18px;
  list-style-type: disc !important;
}
.rp-exp-content li,
.rp-project-content li,
.rp-skills-content li {
  display: list-item !important;
  margin-bottom: 3px;
}
.rp-edu-content strong,
.rp-edu-content b,
.rp-exp-content strong,
.rp-exp-content b,
.rp-project-content strong,
.rp-project-content b,
.rp-skills-content strong,
.rp-skills-content b {
  color: #333 !important;
  font-weight: 600 !important;
}
</style>
