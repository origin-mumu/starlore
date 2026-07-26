<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { listResumes, deleteResume, getResume, createResume, type ResumeData } from '@/api/resume'
import { useUserStore } from '@/stores/user'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { FileText } from '@lucide/vue'

const router = useRouter()
const userStore = useUserStore()
const resumes = ref<ResumeData[]>([])
const loading = ref(true)

const hasAccess = computed(() => {
  const r = userStore.role
  return r === 'member' || r === 'admin'
})

const loadResumes = async () => {
  if (!hasAccess.value) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const res: any = await listResumes()
    resumes.value = res.data || []
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
}

onMounted(loadResumes)

const TEMPLATES = [{ key: 'classic', name: '经典简历', desc: '标准排版，适合大多数场景' }]

const showTemplatePicker = ref(false)
const creating = ref(false)

const TEMPLATE_CONTENT: Record<
  string,
  { title: string; name: string; jobTitle: string; phone: string; email: string; content: any }
> = {
  classic: {
    title: '我的简历',
    name: '张三',
    jobTitle: '前端开发实习',
    phone: '138-8888-8888',
    email: 'zhangsan@example.com',
    content: {
      education: [
        {
          school: '星星大学',
          major: '软件工程',
          degree: '本科',
          period: '2023-09 ~ 2027-06',
          detail: '专业课程：数据结构与算法、操作系统、计算机网络、数据库、软件工程。',
        },
      ],
      experience: [
        {
          company: '星光科技有限公司',
          position: '前端开发实习',
          period: '2026-03 ~ 2026-05',
          detail:
            '<p><strong>项目职责：</strong></p><p>参与公司核心业务页面开发，使用 Vue 3 + TypeScript + Vite 构建高质量交互界面；完成接口对接、表单验证、组件复用与性能优化。</p>',
        },
      ],
      projects: [
        {
          name: '智能知识库系统',
          role: '全栈开发',
          period: '2025-12 ~ 至今',
          detail:
            '<p><strong>项目简介：</strong></p><p>构建 AI 驱动的知识库系统，支持知识创建、搜索、智能问答与可视化展示。</p><p><strong>技术栈：</strong>Vue 3、TypeScript、Vite、Pinia、Spring Boot、MyBatis、MySQL。</p>',
        },
      ],
      skills:
        '<ul><li>熟练使用 Vue 3、TypeScript、Vite 进行前端开发</li><li>掌握 HTML/CSS、响应式布局和组件化开发</li><li>了解后端 Spring Boot、MyBatis 和 RESTful API 设计</li></ul>',
      spacing: { moduleGap: 25, lineHeight: 1.6, fontSize: 14 },
    },
  },
}

const createWithTemplate = async (template: string) => {
  creating.value = true
  try {
    const tpl = TEMPLATE_CONTENT[template] || TEMPLATE_CONTENT.classic
    const res: any = await (
      await import('@/api/resume')
    ).createResume({
      title: tpl.title,
      template,
      name: tpl.name,
      jobTitle: tpl.jobTitle,
      phone: tpl.phone,
      email: tpl.email,
      content: JSON.stringify(tpl.content),
    })
    const id = res.data?.id
    if (id) {
      router.push(`/resume/edit/${id}`)
    }
  } catch {
    /* ignore */
  } finally {
    creating.value = false
    showTemplatePicker.value = false
  }
}

const showDeleteConfirm = ref(false)
const deleteTargetId = ref<number | null>(null)

const handleDelete = (id: number) => {
  deleteTargetId.value = id
  showDeleteConfirm.value = true
}

const confirmDelete = async () => {
  const id = deleteTargetId.value
  if (id == null) return
  showDeleteConfirm.value = false
  try {
    await deleteResume(id)
    await loadResumes()
  } catch {
    /* ignore */
  }
}

const copying = ref(false)
const handleCopy = async (item: ResumeData) => {
  copying.value = true
  try {
    const res: any = await getResume(item.id!)
    const source = res.data
    await createResume({
      title: (source.title || '未命名简历') + ' - 副本',
      template: source.template,
      name: source.name,
      jobTitle: source.jobTitle,
      phone: source.phone,
      email: source.email,
      content: source.content,
    })
    await loadResumes()
  } catch {
    /* ignore */
  } finally {
    copying.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <!-- Access denied for non-member roles -->
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
        <div class="container">
          <h1>简历管理</h1>
          <p class="page-desc">在线制作专业简历，支持多模板切换</p>
        </div>
      </section>

      <section class="section-parchment">
        <div class="container">
          <div class="toolbar">
            <button class="btn-primary" @click="showTemplatePicker = true">+ 新增简历</button>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="loading-box">加载中...</div>

          <!-- Empty -->
          <div v-else-if="resumes.length === 0" class="empty-box">
            <div class="empty-icon">
              <FileText :size="48" />
            </div>
            <p>还没有简历，点击上方按钮创建</p>
          </div>

          <!-- Resume List -->
          <div v-else class="resume-grid">
            <div v-for="item in resumes" :key="item.id" class="resume-card">
              <div class="card-header">
                <h3>{{ item.title || '未命名简历' }}</h3>
                <span class="template-badge">{{
                  item.template === 'classic'
                    ? '经典'
                    : item.template === 'modern'
                      ? '现代'
                      : '创意'
                }}</span>
              </div>
              <div class="card-body">
                <p v-if="item.name">姓名：{{ item.name }}</p>
                <p v-if="item.jobTitle">求职意向：{{ item.jobTitle }}</p>
                <p class="card-time">更新于 {{ item.updatedAt?.slice(0, 10) }}</p>
              </div>
              <div class="card-actions">
                <button class="btn-outline btn-sm" @click="router.push(`/resume/edit/${item.id}`)">
                  编辑
                </button>
                <button class="btn-outline btn-sm" @click="handleCopy(item)" :disabled="copying">
                  复制
                </button>
                <button class="btn-outline btn-sm btn-danger" @click="handleDelete(item.id!)">
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Template Picker Modal -->
      <Teleport to="body">
        <div
          v-if="showTemplatePicker"
          class="modal-overlay"
          @click.self="showTemplatePicker = false"
        >
          <div class="modal-card">
            <div class="modal-header">
              <h2>选择简历模板</h2>
              <button class="modal-close" @click="showTemplatePicker = false">&times;</button>
            </div>
            <div class="template-grid">
              <div
                v-for="tpl in TEMPLATES"
                :key="tpl.key"
                class="template-option"
                :class="{ 'template-option--active': false }"
                @click="createWithTemplate(tpl.key)"
              >
                <div class="template-preview">
                  <div class="template-placeholder">{{ tpl.name }}</div>
                </div>
                <div class="template-info">
                  <h4>{{ tpl.name }}</h4>
                  <p>{{ tpl.desc }}</p>
                </div>
              </div>
            </div>
            <div v-if="creating" class="modal-loading">创建中...</div>
          </div>
        </div>
      </Teleport>

      <ConfirmModal
        :show="showDeleteConfirm"
        title="确认删除"
        message="确认删除该简历？此操作不可撤销。"
        confirm-text="删除"
        @confirm="confirmDelete"
        @cancel="showDeleteConfirm = false"
      />
    </template>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 24px;
}

.loading-box,
.empty-box {
  text-align: center;
  padding: 80px 0;
  color: var(--ink-muted);
}
.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  color: var(--ink-muted);
  opacity: 0.5;
}
.resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.resume-card {
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  transition: all 0.25s;
}
.resume-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.card-header h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--ink);
  margin: 0;
}
.template-badge {
  font-size: 0.72rem;
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--accent-soft, rgba(109, 99, 255, 0.08));
  color: var(--accent, #6d63ff);
}
.card-body p {
  font-size: 0.88rem;
  color: var(--ink-soft);
  margin: 4px 0;
}
.card-time {
  font-size: 0.78rem !important;
  color: var(--ink-muted) !important;
  margin-top: 8px !important;
}
.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
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
  border-color: var(--accent, #6d63ff);
  color: var(--accent, #6d63ff);
}
.btn-danger:hover {
  border-color: #e74c3c !important;
  color: #e74c3c !important;
}
.btn-sm {
  font-size: 0.8rem;
  padding: 5px 14px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-card {
  background: var(--surface, #fff);
  border-radius: 20px;
  width: 90%;
  max-width: 640px;
  max-height: 80vh;
  overflow-y: auto;
  padding: 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.modal-header h2 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--ink);
}
.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--ink-muted);
}
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 16px;
}
.template-option {
  border: 2px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}
.template-option:hover {
  border-color: var(--accent, #6d63ff);
  transform: translateY(-2px);
}
.template-preview {
  height: 160px;
  background: linear-gradient(135deg, #f0edff, #faf6ee);
  display: flex;
  align-items: center;
  justify-content: center;
}
.template-placeholder {
  font-size: 0.9rem;
  color: var(--ink-muted);
  font-weight: 600;
}
.template-info {
  padding: 12px;
}
.template-info h4 {
  margin: 0 0 4px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--ink);
}
.template-info p {
  margin: 0;
  font-size: 0.78rem;
  color: var(--ink-muted);
}
.modal-loading {
  text-align: center;
  padding: 16px;
  color: var(--ink-muted);
}

.page-desc {
  font-size: 0.9rem;
  color: var(--ink-muted, #888);
  margin: 4px 0 0;
}

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
</style>
