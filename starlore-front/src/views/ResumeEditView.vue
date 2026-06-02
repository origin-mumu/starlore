<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getResume, updateResume, uploadImage, type ResumeData } from '@/api/resume'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import RichEditor from '@/components/RichEditor.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { useUserStore } from '@/stores/user'

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
  title: '前端开发实习生 · 张三',
  template: 'classic',
  name: '张三',
  jobTitle: '前端开发实习生',
  phone: '138-0000-0000',
  email: 'zhangsan@example.com',
  photoUrl: '',
})

interface ResumeContent {
  education: { school: string; major: string; degree: string; period: string; detail: string }[]
  experience: { company: string; position: string; period: string; detail: string }[]
  projects: { name: string; role: string; period: string; detail: string }[]
  skills: string[]
  spacing: { moduleGap: number; lineHeight: number; fontSize: number }
}

const defaultSpacing = { moduleGap: 25, lineHeight: 1.6, fontSize: 14 }

/* ─── 模拟数据 ─── */
const mockData: ResumeContent = {
  education: [
    { school: '星星大学', major: '计算机科学与技术', degree: '本科', period: '2023.09 — 2027.06', detail: 'GPA 3.8/4.0 · 校级优秀学生干部 · 蓝桥杯省二等奖' },
  ],
  experience: [
    { company: '极光科技有限公司', position: '前端开发实习生', period: '2025.07 — 2025.10', detail: '参与公司后台管理系统开发，基于 Vue 3 + Element Plus 完成 6 个核心业务模块的前端实现，优化首屏加载速度 35%' },
    { company: '星云工作室', position: '前端组员', period: '2024.09 — 2025.06', detail: '负责社团官网重构，采用 Vite + TypeScript 构建，实现响应式布局与暗色模式切换，日均 PV 提升 50%' },
  ],
  projects: [
    { name: 'Starlore 全栈博客系统', role: '独立开发', period: '2025.03 — 至今', detail: 'Vue 3 + Spring Boot + MyBatis-Plus 全栈项目，集成 AI Agent（11 个 Function Calling 工具链）、SSE 流式对话、Three.js 3D 知识星域、6 套主题引擎，已部署上线' },
    { name: '算法刷题笔记平台', role: '前端开发', period: '2024.10 — 2024.12', detail: 'React + Next.js 开发的在线算法笔记平台，支持 Markdown 编辑、代码高亮、标签分类与全文检索' },
  ],
  skills: ['Vue 3 / React', 'TypeScript / JavaScript', 'Spring Boot / MyBatis', 'MySQL / Redis', 'Docker / Linux', 'Git / Vite / Webpack'],
  spacing: { ...defaultSpacing },
}

const content = reactive<ResumeContent>({
  education: [...mockData.education.map(e => ({ ...e }))],
  experience: [...mockData.experience.map(e => ({ ...e }))],
  projects: [...mockData.projects.map(p => ({ ...p }))],
  skills: [...mockData.skills],
  spacing: { ...defaultSpacing },
})

const photoPreview = ref('')

const A4_PX_H = 1122 // 297mm ≈ 1122px

// 测量用容器ref
const contentMeasurer = ref<HTMLElement>()
const totalContentHeight = ref(0)

const pageCount = computed(() => Math.max(1, Math.ceil(totalContentHeight.value / A4_PX_H)))
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
  exporting.value = true
  try {
    const el = document.querySelector('.resume-page') as HTMLElement
    if (!el) return

    const canvas = await html2canvas(el, {
      scale: 2,
      useCORS: true,
      logging: false,
      backgroundColor: '#ffffff',
      width: el.scrollWidth,
      height: el.scrollHeight,
    })

    const A4_W = 210
    const A4_H = 297
    const pdf = new jsPDF('p', 'mm', 'a4')
    const imgH = (canvas.height * A4_W) / canvas.width

    let remainingH = imgH
    let srcY = 0
    let page = 0
    while (remainingH > 0) {
      if (page > 0) pdf.addPage()
      const pageH = Math.min(A4_H, remainingH)
      const srcH = (pageH * canvas.width) / A4_W
      const tmpCanvas = document.createElement('canvas')
      tmpCanvas.width = canvas.width
      tmpCanvas.height = srcH
      const ctx = tmpCanvas.getContext('2d')!
      ctx.drawImage(canvas, 0, srcY, canvas.width, srcH, 0, 0, canvas.width, srcH)
      const pageImg = tmpCanvas.toDataURL('image/png')
      pdf.addImage(pageImg, 'PNG', 0, 0, A4_W, pageH)
      srcY += srcH
      remainingH -= pageH
      page++
    }

    pdf.save(`${form.name || '简历'}.pdf`)
  } catch (e) {
    notify('导出失败', '导出 PDF 失败，请重试')
  } finally {
    exporting.value = false
  }
}

const handlePhotoUpload = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const res: any = await uploadImage(file)
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

const addSkill = () => {
  content.skills.push('')
}
const removeSkill = (idx: number) => {
  content.skills.splice(idx, 1)
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
  '--line-height': content.spacing.lineHeight,
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
            <span v-if="pageCount > 1" class="page-label">{{ currentPage }} / {{ pageCount }}</span>
          </div>

          <!-- 分页滚动容器 -->
          <div class="pages-container" @scroll="onPageScroll">
            <div v-for="(_, idx) in pageCount" :key="idx" class="page-frame">
              <div
                class="resume-page"
                :style="{ ...spacingStyle, transform: `translateY(-${idx * A4_PX_H}px)` }"
              >
                <!-- 头像右上角 -->
                <div class="avatar-box">
                  <img v-if="previewData.photoUrl" :src="previewData.photoUrl" alt="photo" />
                  <div v-else class="avatar-placeholder">&#128100;</div>
                </div>

                <!-- 头部信息 -->
                <div class="rp-header">
                  <h1 class="rp-name">{{ previewData.name || '姓名' }}</h1>
                  <div class="rp-job-intent">
                    求职意向：{{ previewData.jobTitle || '求职意向' }}
                  </div>
                  <div class="rp-contact">
                    {{ previewData.phone || '电话' }} | {{ previewData.email || '邮箱' }}
                  </div>
                </div>

                <!-- 教育背景 -->
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
                    <div class="rp-edu-content" v-html="item.detail || ''"></div>
                  </div>
                </section>

                <!-- 实习/工作经历 -->
                <section v-if="previewData.experience.length" class="rp-section">
                  <div class="rp-section-title">实习经历</div>
                  <div v-for="(item, i) in previewData.experience" :key="i" class="rp-exp-item">
                    <div class="rp-exp-header">
                      <span>{{ item.company || '(公司)' }} -- {{ item.position || '(职位)' }}</span>
                      <span>{{ item.period }}</span>
                    </div>
                    <div class="rp-exp-content" v-html="item.detail" v-if="item.detail"></div>
                  </div>
                </section>

                <!-- 项目经历 -->
                <section v-if="previewData.projects.length" class="rp-section">
                  <div class="rp-section-title">项目经历</div>
                  <div v-for="(item, i) in previewData.projects" :key="i" class="rp-project-item">
                    <div class="rp-project-header">
                      <span>{{ item.name || '(项目名称)' }} -- {{ item.role || '(角色)' }}</span>
                      <span>{{ item.period }}</span>
                    </div>
                    <div class="rp-project-content" v-html="item.detail" v-if="item.detail"></div>
                  </div>
                </section>

                <!-- 专业技能 -->
                <section v-if="previewData.skills.length" class="rp-section">
                  <div class="rp-section-title">专业技能</div>
                  <ul class="rp-skills-list">
                    <li v-for="(s, i) in previewData.skills" :key="i">{{ s }}</li>
                  </ul>
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
            <div v-show="activeModule === 'spacing'" class="edit-section spacing-section">
              <h3>排版设置</h3>
              <div class="spacing-field">
                <label class="spacing-label">
                  <span>字体大小</span>
                  <span class="spacing-value">{{ content.spacing.fontSize }}px</span>
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
                  <span class="spacing-value">{{ content.spacing.moduleGap }}px</span>
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
                  <span class="spacing-value">{{ content.spacing.lineHeight.toFixed(1) }}</span>
                </label>
                <input
                  type="range"
                  min="1.0"
                  max="2.0"
                  step="0.1"
                  v-model.number="content.spacing.lineHeight"
                  class="spacing-slider"
                />
              </div>
            </div>

            <!-- Basic Info -->
            <div v-show="activeModule === 'basic'" class="edit-section">
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
            <div v-show="activeModule === 'education'" class="edit-section">
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
            <div v-show="activeModule === 'experience'" class="edit-section">
              <div class="section-header">
                <h3>实习/工作经历</h3>
                <button class="btn-outline btn-sm" @click="addItem('experience')">+ 添加</button>
              </div>
              <div v-for="(item, i) in content.experience" :key="i" class="sub-card">
                <div class="form-row row-2">
                  <div class="form-field"><label>公司</label><input v-model="item.company" /></div>
                  <div class="form-field"><label>职位</label><input v-model="item.position" /></div>
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
            <div v-show="activeModule === 'projects'" class="edit-section">
              <div class="section-header">
                <h3>项目经历</h3>
                <button class="btn-outline btn-sm" @click="addItem('projects')">+ 添加</button>
              </div>
              <div v-for="(item, i) in content.projects" :key="i" class="sub-card">
                <div class="form-row row-2">
                  <div class="form-field"><label>项目名称</label><input v-model="item.name" /></div>
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
            <div v-show="activeModule === 'skills'" class="edit-section">
              <div class="section-header">
                <h3>专业技能</h3>
                <button class="btn-outline btn-sm" @click="addSkill">+ 添加</button>
              </div>
              <div v-for="(_, i) in content.skills" :key="i" class="skill-row">
                <input v-model="content.skills[i]" placeholder="Vue.js" />
                <button class="btn-remove-sm" @click="removeSkill(i)">&times;</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 隐藏的测量容器 -->
    <div ref="contentMeasurer" class="content-measurer" aria-hidden="true">
      <div class="resume-page measurer-inner" :style="spacingStyle">
        <div class="avatar-box">
          <div v-if="photoPreview" class="avatar-placeholder"></div>
          <div v-else class="avatar-placeholder">&#128100;</div>
        </div>
        <div class="rp-header">
          <h1 class="rp-name">{{ previewData.name || '姓名' }}</h1>
          <div class="rp-job-intent">求职意向：{{ previewData.jobTitle || '求职意向' }}</div>
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
            <div class="rp-edu-content" v-html="item.detail || ''"></div>
          </div>
        </section>
        <section v-if="previewData.experience.length" class="rp-section">
          <div class="rp-section-title">实习经历</div>
          <div v-for="(item, i) in previewData.experience" :key="i" class="rp-exp-item">
            <div class="rp-exp-header">
              <span>{{ item.company || '(公司)' }} -- {{ item.position || '(职位)' }}</span>
              <span>{{ item.period }}</span>
            </div>
            <div class="rp-exp-content" v-html="item.detail" v-if="item.detail"></div>
          </div>
        </section>
        <section v-if="previewData.projects.length" class="rp-section">
          <div class="rp-section-title">项目经历</div>
          <div v-for="(item, i) in previewData.projects" :key="i" class="rp-project-item">
            <div class="rp-project-header">
              <span>{{ item.name || '(项目名称)' }} -- {{ item.role || '(角色)' }}</span>
              <span>{{ item.period }}</span>
            </div>
            <div class="rp-project-content" v-html="item.detail" v-if="item.detail"></div>
          </div>
        </section>
        <section v-if="previewData.skills.length" class="rp-section">
          <div class="rp-section-title">专业技能</div>
          <ul class="rp-skills-list">
            <li v-for="(s, i) in previewData.skills" :key="i">{{ s }}</li>
          </ul>
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
  grid-template-columns: 1fr 1fr;
  gap: 0;
  min-height: calc(100vh - 180px);
  background: var(--surface);
}

/* ===== Preview Panel ===== */
.preview-panel {
  border-right: 1px solid var(--border);
  overflow-y: hidden;
  background: #f5f3f0;
  display: flex;
  flex-direction: column;
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
  overflow-x: hidden;
  padding: 24px;
}

/* 每页 frame */
.page-frame {
  width: 210mm;
  height: 297mm;
  overflow: hidden;
  margin: 0 auto 24px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.1);
  background: #fff;
  position: relative;
  flex-shrink: 0;
}
.page-frame:last-child {
  margin-bottom: 0;
}

/* 简历页面内容 */
.resume-page {
  width: 210mm;
  padding: 30px;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  color: #222;
  position: relative;
  background: #fff;
}

/* 测量容器：隐藏但保留布局，用于计算总高度 */
.content-measurer {
  position: fixed;
  left: -9999px;
  top: 0;
  width: 210mm;
  opacity: 0;
  pointer-events: none;
  z-index: -1;
}

/* 翻页导航 */
.page-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 10px;
  border-top: 1px solid var(--border);
  background: var(--surface);
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
  top: 30px;
  right: 30px;
  width: 100px;
  height: 120px;
  background: #eee;
  border: 1px solid #ddd;
  overflow: hidden;
}
.avatar-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  margin-bottom: 30px;
}
.rp-name {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 10px;
}
.rp-job-intent {
  font-size: 16px;
  margin-bottom: 10px;
  color: #333;
}
.rp-contact {
  font-size: 14px;
  color: #555;
}

/* Section */
.rp-section {
  margin-bottom: var(--module-gap, 25px);
}
.rp-section-title {
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #333;
  padding-bottom: 5px;
  margin-bottom: 15px;
}

/* Education */
.rp-edu-item {
  margin-bottom: 15px;
}
.rp-edu-header {
  display: flex;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 8px;
}
.rp-edu-content {
  font-size: var(--font-size, 14px);
  line-height: var(--line-height, 1.6);
  color: #333;
}

/* Experience */
.rp-exp-item {
  margin-bottom: 15px;
}
.rp-exp-header {
  display: flex;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 8px;
}
.rp-exp-content {
  font-size: var(--font-size, 14px);
  line-height: var(--line-height, 1.6);
  color: #333;
}
.rp-detail-block p {
  margin: 4px 0;
}
.rp-detail-block strong {
  font-weight: 600;
}
.rp-bullet-line {
  margin: 2px 0;
}

/* Project */
.rp-project-item {
  margin-bottom: 15px;
}
.rp-project-header {
  display: flex;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 8px;
}
.rp-project-content {
  font-size: var(--font-size, 14px);
  line-height: var(--line-height, 1.8);
  color: #333;
}
.rp-bullet {
  margin: 2px 0;
  padding-left: 15px;
}

/* Skills */
.rp-skills-list {
  list-style-type: disc;
  margin-left: 20px;
  font-size: var(--font-size, 14px);
  line-height: var(--line-height, 1.8);
  color: #333;
}
.rp-skills-list li {
  margin-bottom: 2px;
}

/* ===== Edit Panel ===== */
.edit-panel {
  display: flex;
  background: var(--surface);
  overflow: hidden;
}
.edit-menu {
  width: 130px;
  flex-shrink: 0;
  border-right: 1px solid var(--border);
  padding: 12px 0;
  overflow-y: auto;
  background: var(--canvas);
}
.edit-menu-item {
  display: block;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: none;
  text-align: left;
  font-size: 0.85rem;
  color: var(--ink-soft, #666);
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
  border-left: 3px solid transparent;
}
.edit-menu-item:hover {
  color: var(--ink);
  background: rgba(109, 99, 255, 0.04);
}
.edit-menu-item.active {
  color: var(--accent, #6d63ff);
  background: rgba(109, 99, 255, 0.08);
  border-left-color: var(--accent, #6d63ff);
  font-weight: 600;
}
.edit-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  max-width: 650px;
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
.skill-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.skill-row input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.88rem;
  outline: none;
  font-family: inherit;
}
.btn-remove-sm {
  background: none;
  border: none;
  color: #e74c3c;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 4px;
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
  }
  .preview-panel {
    border-right: none;
    border-bottom: 1px solid var(--border);
  }
  .page-frame {
    width: 100%;
    height: auto;
    min-height: 297mm;
  }
  .resume-page {
    width: 100%;
  }
  .edit-menu {
    width: 100px;
  }
  .edit-content {
    max-width: 100%;
  }
}
</style>
