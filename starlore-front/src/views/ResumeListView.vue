<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { listResumes, deleteResume, getResume, createResume, type ResumeData } from '@/api/resume'
import { useUserStore } from '@/stores/user'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { FileText, X } from '@lucide/vue'

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
                <p v-if="item.jobTitle?.trim()">求职意向：{{ item.jobTitle }}</p>
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
        <Transition name="modal-fade">
          <div
            v-if="showTemplatePicker"
            class="modal-overlay"
            @click.self="showTemplatePicker = false"
          >
            <div class="modal-card">
              <div class="modal-header">
                <h2>选择简历模板</h2>
                <button class="modal-close" @click="showTemplatePicker = false">
                  <X :size="16" />
                </button>
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
        </Transition>
      </Teleport>

      <ConfirmModal
        :show="showDeleteConfirm"
        title="确认删除"
        message="确认删除该简历？此操作不可撤销。"
        confirm-text="删除简历"
        tone="danger"
        @confirm="confirmDelete"
        @cancel="showDeleteConfirm = false"
      />
    </template>
  </div>
</template>

<style scoped src="./ResumeListView.css"></style>
