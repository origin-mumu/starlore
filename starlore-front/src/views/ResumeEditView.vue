<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
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

// Keep this identical to starlore-pdf/templates/classic.html.
const A4_PX_H = 1123
// Client and server Chromium can differ slightly in CJK font metrics. A short
// tail at the boundary is layout noise rather than a meaningful second page.
const PAGE_TAIL_TOLERANCE = 96

// 测量用容器ref
const contentMeasurer = ref<HTMLElement>()
const totalContentHeight = ref(0)

const pageCount = computed(() => {
  const pages = Math.max(1, Math.ceil(totalContentHeight.value / A4_PX_H))
  // 最后一页溢出不足50px时忽略（仅为残留margin/padding），避免出现几乎空白的页面
  if (pages > 1) {
    const lastPageContent = totalContentHeight.value - (pages - 1) * A4_PX_H
    if (lastPageContent <= PAGE_TAIL_TOLERANCE) return pages - 1
  }
  return pages
})
const currentPage = ref(1)

// 测量内容总高度
const measureContent = () => {
  if (!contentMeasurer.value) return
  totalContentHeight.value = contentMeasurer.value.scrollHeight
}

onMounted(async () => {
  if (!hasAccess.value) {
    loading.value = false
    return
  }
  if (!resumeId.value) {
    loading.value = false
    await nextTick()
    measureContent()
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
        <div class="editor-layout">
          <!-- LEFT: 分页预览 -->
          <div class="preview-panel">
            <div class="preview-header">
              简历预览
              <span v-if="pageCount > 1" class="page-label"
                >{{ currentPage }} / {{ pageCount }}</span
              >
            </div>

            <!-- 分页滚动容器 -->
            <div class="pages-container" @scroll="onPageScroll">
              <div v-for="(_, idx) in pageCount" :key="idx" class="page-frame">
                <div
                  class="resume-page"
                  :style="{ ...spacingStyle, transform: `translateY(-${idx * A4_PX_H}px)` }"
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
                  <div class="rp-header clickable-section" @click="activeModule = 'basic'">
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

          <!-- RIGHT: 编辑面板 -->
          <div class="edit-panel">
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
          <div class="rp-header">
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

<style scoped>
/* Access Denied */
.access-denied {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 40px 20px;
}
.access-denied-content {
  text-align: center;
  max-width: 400px;
}
.access-denied-icon {
  font-size: 3rem;
  margin-bottom: 16px;
}
.access-denied-content h2 {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 8px;
}
.access-denied-content p {
  color: var(--ink-muted);
  margin: 0 0 24px;
  font-size: 0.95rem;
}

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
  grid-template-columns: 2.2fr 2fr;
  gap: 16px;
  min-height: calc(100vh - 180px);
  /*background: var(--canvas);*/
  padding: 16px;
}

/* ===== Preview Panel ===== */
.preview-panel {
  overflow: hidden;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  /* height: calc(100vh - 100px); */
  height: 100vh;
}
.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--ink-muted);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.page-label {
  font-size: 0.82rem;
  color: var(--accent, #6d63ff);
  font-weight: 600;
}

/* 分页滚动容器 */
.pages-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: auto;
  padding: 10px;
}

/* 每页 frame */
.page-frame {
  width: 794px;
  height: 1123px;
  overflow: hidden;
  margin: 0 auto 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  background: #fff;
  position: relative;
  flex-shrink: 0;
}
.page-frame:last-child {
  margin-bottom: 0;
}

/* 简历页面内容 */
.resume-page {
  width: 794px;
  min-height: 1123px;
  padding: 28px 45px 28px 30px;
  font-family:
    Inter,
    -apple-system,
    BlinkMacSystemFont,
    Segoe UI,
    Roboto,
    'Noto Sans SC',
    'PingFang SC',
    'Microsoft YaHei',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  color: #333;
  font-weight: 500;
  position: relative;
  background: #fff;
}

/* === 颜色与字重映射 === */
.text-name {
  color: #111111;
  font-weight: 700;
}
.text-title {
  color: #222222;
  font-weight: 700;
}
.text-core {
  color: #333333;
  font-weight: 500;
}
.text-secondary {
  color: #555555;
  font-weight: 500;
}
.bold {
  font-weight: 700;
}

/* 测量容器：隐藏但保留布局，用于计算总高度 */
.content-measurer {
  position: fixed;
  left: -9999px;
  top: 0;
  width: 794px;
  opacity: 0;
  pointer-events: none;
  z-index: -1;
}
/* 去掉测量容器内的 min-height，避免人为撑高导致误判分页 */
.content-measurer .resume-page {
  min-height: auto;
}

/* 翻页导航 */
.page-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 10px;
  border-top: 1px solid var(--border);
  background: var(--glass-bg);
  backdrop-filter: blur(12px) saturate(1.2);
  -webkit-backdrop-filter: blur(12px) saturate(1.2);
  flex-shrink: 0;
}
.page-arrow {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--border);
  background: #fff;
  font-size: 1.1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink);
  transition: all 0.2s;
}
.page-arrow:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}
.page-arrow:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.page-dot {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--ink-muted);
}

/* Avatar - 右上角 */
.avatar-box {
  position: absolute;
  top: 28px;
  right: 45px;
  width: 80px;
  height: 110px;
  overflow: hidden;
  border-radius: 4px;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  color: #bbb;
  background: #f0ebe5;
}

/* Header */
.rp-header {
  text-align: center;
  margin-bottom: 16px;
}
.rp-name {
  font-size: 26px;
  font-weight: 700;
  color: #111;
  letter-spacing: 2px;
  margin: 0 0 8px;
}
.rp-job-intent {
  font-size: 11.5px;
  line-height: 1.5;
  margin-bottom: 0;
  color: #555;
  font-weight: 500;
}
.rp-contact {
  font-size: 11.5px;
  line-height: 1.5;
  color: #555;
  font-weight: 500;
}
.rp-contact .divider {
  margin: 0 8px;
  color: #ccc;
}

/* Section */
.rp-section {
  margin-bottom: var(--module-gap, 12px);
}

/* 可点击区域 */
.clickable-section {
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
  margin-left: -6px;
  margin-right: -6px;
  padding-left: 6px;
  padding-right: 6px;
}
.clickable-section:hover {
  background: rgba(109, 99, 255, 0.06);
}
.rp-section-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #222;
  border-bottom: 2px solid #222;
  padding-bottom: 2px;
  margin-bottom: 6px;
}

/* Education */
.rp-edu-item {
  margin-bottom: 0;
}
.rp-edu-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12.5px;
  font-weight: 500;
  margin-bottom: 3px;
}
.rp-edu-header .item-name {
  font-size: 12.5px;
}
.rp-edu-header .item-date {
  font-size: 11.5px;
}
.rp-edu-content {
  font-size: var(--font-size, 11.5px);
  line-height: calc(var(--font-size, 14px) + var(--line-gap, 6px));
  color: #777;
  font-weight: 500;
}
.rp-edu-content strong,
.rp-edu-content b {
  color: #333;
  font-weight: 600;
}
.rp-edu-content p {
  margin-bottom: 2px;
}

/* Experience */
.rp-exp-item {
  margin-bottom: 0;
}
.rp-exp-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12.5px;
  font-weight: 500;
  margin-bottom: 3px;
}
.rp-exp-header .item-name {
  font-size: 12.5px;
}
.rp-exp-header .item-date {
  font-size: 11.5px;
}
.rp-exp-content {
  font-size: var(--font-size, 11.5px);
  line-height: calc(var(--font-size, 14px) + var(--line-gap, 6px));
  color: #777;
  font-weight: 500;
  text-align: left;
}
.rp-exp-content strong,
.rp-exp-content b {
  color: #333;
  font-weight: 600;
}
.rp-exp-content p {
  margin-bottom: 2px;
}
.rp-exp-content ul {
  margin-top: 2px;
  padding-left: 16px;
  list-style-type: disc;
}
.rp-exp-content li {
  margin-bottom: 2px;
}
.rp-detail-block p {
  margin: 2px 0;
}
.rp-detail-block strong {
  font-weight: 700;
}
.rp-bullet-line {
  margin: 2px 0;
}

/* Project */
.rp-project-item {
  margin-bottom: 0;
}
.rp-project-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12.5px;
  font-weight: 500;
  margin-bottom: 3px;
}
.rp-project-header .item-name {
  font-size: 12.5px;
}
.rp-project-header .item-date {
  font-size: 11.5px;
}
.rp-project-content {
  font-size: var(--font-size, 11.5px);
  line-height: calc(var(--font-size, 14px) + var(--line-gap, 6px));
  color: #777;
  font-weight: 500;
  text-align: left;
}
.rp-project-content strong,
.rp-project-content b {
  color: #333;
  font-weight: 600;
}
.rp-project-content p {
  margin-bottom: 2px;
}
.rp-project-content ul {
  margin-top: 2px;
  padding-left: 16px;
  list-style-type: disc;
}
.rp-project-content li {
  margin-bottom: 2px;
}
.rp-bullet {
  margin: 2px 0;
  padding-left: 16px;
}

/* Skills */
.rp-skills-content {
  font-size: var(--font-size, 11.5px);
  line-height: calc(var(--font-size, 14px) + var(--line-gap, 6px));
  color: #777;
  font-weight: 500;
  text-align: left;
}
.rp-skills-content strong,
.rp-skills-content b {
  color: #333;
  font-weight: 600;
}
.rp-skills-content ul {
  padding-left: 16px;
  list-style-type: disc;
}
.rp-skills-content li {
  margin-bottom: 2px;
}
.rp-skills-content p {
  margin-bottom: 2px;
}
.rp-skills-content ul {
  margin-top: 2px;
  padding-left: 16px;
}
.rp-skills-content li {
  margin-bottom: 2px;
}
.rp-skills-content strong {
  font-weight: 700;
}

/* ===== Edit Panel ===== */
.edit-panel {
  display: flex;
  background: var(--glass-bg);
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  /* height: calc(100vh - 200px); */
  height: 100vh;
}
.edit-menu {
  width: 130px;
  flex-shrink: 0;
  padding: 16px 10px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.edit-menu-item {
  display: block;
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--canvas);
  text-align: left;
  font-size: 0.85rem;
  color: var(--ink-soft, #666);
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}
.edit-menu-item:hover {
  color: var(--ink);
  border-color: var(--accent, #6d63ff);
  background: rgba(109, 99, 255, 0.04);
}
.edit-menu-item.active {
  color: #fff;
  background: var(--accent, #6d63ff);
  border-color: var(--accent, #6d63ff);
  font-weight: 600;
}
.edit-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  max-width: 650px;
  min-height: 0;
}
.edit-section {
  margin-bottom: 28px;
}
.edit-section h3 {
  font-size: 0.95rem;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--ink);
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-header h3 {
  margin: 0;
}
.form-row {
  margin-bottom: 12px;
}
.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;
}
.form-field label {
  font-size: 0.78rem;
  font-weight: 500;
  color: var(--ink-muted);
}
.form-field input,
.form-field textarea {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.88rem;
  background: var(--canvas);
  color: var(--ink);
  outline: none;
  font-family: inherit;
  transition: border 0.2s;
}
.form-field input:focus,
.form-field textarea:focus {
  border-color: var(--accent, #6d63ff);
}
.sub-card {
  background: var(--canvas);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  position: relative;
}
.btn-remove {
  position: absolute;
  top: 8px;
  right: 8px;
  background: none;
  border: none;
  color: #e74c3c;
  font-size: 0.78rem;
  cursor: pointer;
  opacity: 0.5;
  transition: opacity 0.2s;
}
.btn-remove:hover {
  opacity: 1;
}
.photo-upload {
  display: flex;
  align-items: center;
  gap: 12px;
}
.photo-thumb {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
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
  margin-left: 8px;
}
.btn-outline:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.btn-sm {
  font-size: 0.8rem;
  padding: 5px 14px;
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
.btn-export {
  border-color: var(--accent);
  color: var(--accent);
}
.btn-export:hover {
  background: var(--accent);
  color: #fdfbf5 !important;
  border-color: var(--accent);
}

/* ===== Spacing Controls ===== */
.spacing-section {
  padding-top: 8px;
  border-top: 1px solid var(--border);
}
.spacing-field {
  margin-bottom: 16px;
}
.spacing-label {
  display: flex;
  justify-content: space-between;
  font-size: 0.82rem;
  color: var(--ink);
  margin-bottom: 6px;
}
.spacing-value {
  font-weight: 600;
  color: var(--accent, #6d63ff);
  font-size: 0.82rem;
}
.spacing-input-group {
  display: flex;
  align-items: center;
  gap: 4px;
}
.spacing-number {
  width: 52px;
  padding: 3px 6px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--accent, #6d63ff);
  text-align: center;
  background: var(--canvas);
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
  -moz-appearance: textfield;
  appearance: textfield;
}
.spacing-number::-webkit-inner-spin-button,
.spacing-number::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.spacing-number:focus {
  border-color: var(--accent, #6d63ff);
}
.spacing-unit {
  font-size: 0.78rem;
  color: var(--ink-muted);
}
.spacing-slider {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 4px;
  border-radius: 2px;
  background: var(--border);
  outline: none;
  cursor: pointer;
}
.spacing-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--accent, #6d63ff);
  cursor: pointer;
  border: 2px solid #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
}

@media (max-width: 900px) {
  .editor-layout {
    grid-template-columns: 1fr;
    padding: 12px;
    gap: 12px;
  }
  .page-frame {
    width: 100%;
    height: auto;
    min-height: 1138px;
  }
  .resume-page {
    width: 100%;
    height: auto;
  }
  .edit-menu {
    width: 100px;
    padding: 12px 8px;
  }
  .edit-content {
    max-width: 100%;
  }
}
</style>

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
