<script lang="ts" setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MoreHorizontal,
  Edit3,
  Trash2,
  Camera,
  FolderGit2,
  User,
  Shield,
  Plus,
  ExternalLink,
  Mail,
  MapPin,
  Globe,
  GitFork,
  Sparkles,
  Layers,
  Lock,
  LogOut,
  AlertCircle,
} from '@lucide/vue'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import { changePasswordService } from '@/api/auth'
import { uploadImage, getBlogStatsService } from '@/api/article'
import {
  listProjectsService,
  createProjectService,
  updateProjectService,
  deleteProjectService,
  type Project,
  type ProjectParams,
} from '@/api/project'
import defaultAvatar from '@/assets/avatar.jpg'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

// ── 当前激活 Tab ──
const activeTab = ref<'projects' | 'profile' | 'security'>('projects')

// ── 角色徽章 ──
const roleLabel = computed(() => {
  if (userStore.role === 'admin') return '系统管理员'
  if (userStore.role === 'member') return '知识创作者'
  return '星系探索者'
})

// ── 知识库与项目统计 ──
const statsData = ref<{ totalArticles?: number; totalCategories?: number } | null>(null)
const fetchStats = async () => {
  try {
    const res = await getBlogStatsService()
    statsData.value = res.data?.data ?? res.data
  } catch (err) {
    console.error('获取知识库统计失败:', err)
  }
}

// ── 登出逻辑 ──
const guestAllowedPaths = ['/', '/about', '/categories', '/vr']
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出当前账号并返回首页吗？', '退出登录确认', {
    confirmButtonText: '退出登录',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    userStore.logout()
    const path = route.path
    const isGuestAllowed = guestAllowedPaths.some((p) => path === p || path.startsWith(p + '/'))
    if (isGuestAllowed) {
      router.go(0)
    } else {
      router.push('/login')
    }
  }).catch(() => {
    // 用户取消
  })
}

// ── 头像上传 ──
const avatarUploading = ref(false)
const avatarInput = ref<HTMLInputElement>()

const triggerAvatarSelect = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  avatarUploading.value = true
  try {
    const res = await uploadImage(file)
    const url = res.data?.url
    if (url) {
      await userStore.updateProfile({ avatar: url })
      ElMessage.success('头像更新成功')
    }
  } catch (err: any) {
    ElMessage.error(err.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

// ── 个人资料弹窗 ──
const profileDialogVisible = ref(false)
const profileForm = reactive({
  nickname: '',
  email: '',
  bio: '',
  location: '',
  website: '',
  github: '',
})
const profileMsg = ref('')
const profileLoading = ref(false)

const openProfileDialog = () => {
  if (userStore.user) {
    profileForm.nickname = userStore.user.nickname || ''
    profileForm.email = userStore.user.email || ''
    profileForm.bio = userStore.user.bio || ''
    profileForm.location = userStore.user.location || ''
    profileForm.website = userStore.user.website || ''
    profileForm.github = userStore.user.github || ''
  }
  profileMsg.value = ''
  profileDialogVisible.value = true
}

const handleUpdateProfile = async () => {
  profileMsg.value = ''
  profileLoading.value = true
  try {
    await userStore.updateProfile({
      nickname: profileForm.nickname.trim(),
      email: profileForm.email.trim() || undefined,
      bio: profileForm.bio.trim() || undefined,
      location: profileForm.location.trim() || undefined,
      website: profileForm.website.trim() || undefined,
      github: profileForm.github.trim() || undefined,
    })
    ElMessage.success('个人资料已保存')
    profileDialogVisible.value = false
  } catch (e: any) {
    profileMsg.value = e.message || '更新失败，请重试'
  } finally {
    profileLoading.value = false
  }
}

// ── 密码修改 ──
const passwordDialogVisible = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const passwordMsg = ref('')
const passwordSuccess = ref(false)
const passwordLoading = ref(false)

const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordMsg.value = ''
  passwordSuccess.value = false
  passwordDialogVisible.value = true
}

const handleChangePassword = async () => {
  passwordMsg.value = ''
  passwordSuccess.value = false

  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    passwordMsg.value = '请填写原密码和新密码'
    return
  }
  if (passwordForm.newPassword.length < 6) {
    passwordMsg.value = '新密码长度至少6位'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordMsg.value = '两次输入的新密码不一致'
    return
  }

  passwordLoading.value = true
  try {
    await changePasswordService({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordSuccess.value = true
    passwordMsg.value = '密码修改成功'
    ElMessage.success('密码修改成功')
    setTimeout(() => {
      passwordDialogVisible.value = false
    }, 1200)
  } catch (e: any) {
    passwordMsg.value = e.message || '修改失败，请重试'
  } finally {
    passwordLoading.value = false
  }
}

// ── 项目管理 ──
const projects = ref<Project[]>([])
const projectsLoading = ref(false)

const fetchProjects = async () => {
  projectsLoading.value = true
  try {
    const res = await listProjectsService()
    projects.value = res.data || []
  } catch {
    projects.value = []
  } finally {
    projectsLoading.value = false
  }
}

const projectDialogVisible = ref(false)
const projectForm = reactive<ProjectParams>({ name: '' })
const projectEditingId = ref<number | null>(null)
const projectMsg = ref('')
const projectLoading = ref(false)

const openProjectDialog = (p?: Project) => {
  if (p) {
    projectEditingId.value = p.id
    projectForm.name = p.name
    projectForm.description = p.description || ''
    projectForm.url = p.url || ''
    projectForm.image = p.image || ''
    projectForm.sortOrder = p.sortOrder ?? 0
  } else {
    projectEditingId.value = null
    projectForm.name = ''
    projectForm.description = ''
    projectForm.url = ''
    projectForm.image = ''
    projectForm.sortOrder = 0
  }
  projectMsg.value = ''
  projectDialogVisible.value = true
}

const handleSaveProject = async () => {
  projectMsg.value = ''
  if (!projectForm.name.trim()) {
    projectMsg.value = '请输入项目名称'
    return
  }
  projectLoading.value = true
  try {
    if (projectEditingId.value) {
      await updateProjectService(projectEditingId.value, projectForm)
      ElMessage.success('项目已更新')
    } else {
      await createProjectService(projectForm)
      ElMessage.success('项目添加成功')
    }
    projectDialogVisible.value = false
    await fetchProjects()
  } catch (e: any) {
    projectMsg.value = e.message || '操作失败'
  } finally {
    projectLoading.value = false
  }
}

const confirmDeleteProject = (p: Project) => {
  ElMessageBox.confirm(`确定删除项目「${p.name}」？此操作不可恢复。`, '确认删除', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await deleteProjectService(p.id)
    ElMessage.success('项目已删除')
    await fetchProjects()
  }).catch(() => {
    // 用户取消
  })
}

const activeProjectMoreId = ref<number | null>(null)
const toggleProjectMore = (id: number) => {
  activeProjectMoreId.value = activeProjectMoreId.value === id ? null : id
}
const closeProjectMore = () => {
  activeProjectMoreId.value = null
}

onMounted(() => {
  fetchProjects()
  fetchStats()
  window.addEventListener('click', closeProjectMore)
})

onUnmounted(() => {
  window.removeEventListener('click', closeProjectMore)
})
</script>

<template>
  <div class="profile-page">
    <div class="profile-shell">
      <!-- ── 顶部一体化个人名片 Header ── -->
      <header class="profile-hero-card">
        <div class="hero-aurora-bg" aria-hidden="true"></div>

        <div class="hero-content">
          <!-- 头像区 -->
          <div class="avatar-box">
            <div class="avatar-wrap" @click="triggerAvatarSelect" title="点击上传更换头像">
              <img
                class="profile-avatar"
                :src="userStore.user?.avatar || defaultAvatar"
                alt="个人头像"
              />
              <div class="avatar-overlay">
                <span v-if="avatarUploading" class="avatar-spinner"></span>
                <Camera v-else :size="20" class="avatar-icon" />
              </div>
            </div>
            <input
              ref="avatarInput"
              type="file"
              accept="image/*"
              class="avatar-input-hidden"
              @change="handleAvatarChange"
            />
          </div>

          <!-- 用户核心信息 -->
          <div class="user-meta">
            <div class="user-title-row">
              <h1 class="user-display-name">
                {{ userStore.user?.nickname || userStore.user?.username || '探索者' }}
              </h1>
              <span class="user-role-badge" :class="userStore.role">
                <Sparkles :size="12" />
                {{ roleLabel }}
              </span>
            </div>

            <p class="user-username">@{{ userStore.user?.username }}</p>
            <p class="user-bio">
              {{ userStore.user?.bio || '在星尘与代码间构建知识宇宙。' }}
            </p>

            <!-- 社交与属性标签 -->
            <div class="meta-chips">
              <span v-if="userStore.user?.location" class="meta-chip">
                <MapPin :size="13" />
                {{ userStore.user.location }}
              </span>
              <span v-if="userStore.user?.email" class="meta-chip">
                <Mail :size="13" />
                {{ userStore.user.email }}
              </span>
              <a
                v-if="userStore.user?.website"
                :href="userStore.user.website"
                target="_blank"
                rel="noopener noreferrer"
                class="meta-chip meta-chip--link"
              >
                <Globe :size="13" />
                个人网站
                <ExternalLink :size="11" />
              </a>
              <a
                v-if="userStore.user?.github"
                :href="userStore.user.github"
                target="_blank"
                rel="noopener noreferrer"
                class="meta-chip meta-chip--link"
              >
                <GitFork :size="13" />
                GitHub
                <ExternalLink :size="11" />
              </a>
            </div>
          </div>

          <!-- 顶部右侧快捷数据与操作 -->
          <div class="hero-aside">
            <button class="btn-edit-profile" @click="openProfileDialog">
              <Edit3 :size="15" />
              <span>编辑资料</span>
            </button>

            <div class="hero-stats-row">
              <div class="h-stat-item">
                <span class="h-stat-num">{{ statsData?.totalArticles ?? 0 }}</span>
                <span class="h-stat-label">星记篇目</span>
              </div>
              <div class="h-stat-divider"></div>
              <div class="h-stat-item">
                <span class="h-stat-num">{{ statsData?.totalCategories ?? 0 }}</span>
                <span class="h-stat-label">知识星域</span>
              </div>
              <div class="h-stat-divider"></div>
              <div class="h-stat-item">
                <span class="h-stat-num">{{ projects.length }}</span>
                <span class="h-stat-label">展示项目</span>
              </div>
            </div>
          </div>
        </div>
      </header>

      <!-- ── Segmented Tabs 切换栏 ── -->
      <nav class="profile-tabs-bar" aria-label="个人中心选项卡">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'projects' }"
          @click="activeTab = 'projects'"
        >
          <FolderGit2 :size="16" />
          <span>作品与项目</span>
          <span class="tab-count">{{ projects.length }}</span>
        </button>

        <button
          class="tab-btn"
          :class="{ active: activeTab === 'profile' }"
          @click="activeTab = 'profile'"
        >
          <User :size="16" />
          <span>详细档案</span>
        </button>

        <button
          class="tab-btn"
          :class="{ active: activeTab === 'security' }"
          @click="activeTab = 'security'"
        >
          <Shield :size="16" />
          <span>账号安全与偏好</span>
        </button>
      </nav>

      <!-- ── Tab 1: 作品与项目 (Projects) ── -->
      <section v-show="activeTab === 'projects'" class="tab-panel fade-in-up">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">作品与实践项目</h2>
            <p class="panel-desc">展示你的独立开发、开源实践与研究作品</p>
          </div>
          <button class="btn-primary-action" @click="openProjectDialog()">
            <Plus :size="16" />
            <span>新增项目</span>
          </button>
        </div>

        <div v-if="projectsLoading" class="panel-loading">
          <div class="loading-spinner"></div>
          <span>正在同步项目列表...</span>
        </div>

        <div v-else-if="projects.length === 0" class="panel-empty">
          <FolderGit2 :size="44" class="empty-icon" />
          <h3>暂无公开展示项目</h3>
          <p>添加你的开源项目或技术作品，构建个人作品集星图。</p>
          <button class="btn-primary-action" @click="openProjectDialog()">
            <Plus :size="16" />
            <span>添加第一个项目</span>
          </button>
        </div>

        <div v-else class="projects-grid">
          <div v-for="p in projects" :key="p.id" class="project-bento-card">
            <div class="proj-top-bar">
              <div class="proj-badge">
                <Layers :size="14" />
                <span>Featured Project</span>
              </div>
              <div class="proj-more-wrap" @click.stop>
                <button
                  class="btn-icon-more"
                  :class="{ active: activeProjectMoreId === p.id }"
                  @click.stop="toggleProjectMore(p.id)"
                  title="更多操作"
                  aria-label="更多操作"
                >
                  <MoreHorizontal :size="16" />
                </button>
                <transition name="dropdown-fade">
                  <div v-if="activeProjectMoreId === p.id" class="proj-dropdown-menu">
                    <button class="menu-item" @click="closeProjectMore(); openProjectDialog(p)">
                      <Edit3 :size="14" />
                      <span>编辑信息</span>
                    </button>
                    <button class="menu-item danger" @click="closeProjectMore(); confirmDeleteProject(p)">
                      <Trash2 :size="14" />
                      <span>删除项目</span>
                    </button>
                  </div>
                </transition>
              </div>
            </div>

            <h3 class="proj-name">{{ p.name }}</h3>
            <p class="proj-desc">{{ p.description || '暂无详细介绍描述' }}</p>

            <div class="proj-footer">
              <a v-if="p.url" :href="p.url" target="_blank" rel="noopener noreferrer" class="proj-link">
                <span>浏览项目源码 / 体验</span>
                <ExternalLink :size="13" />
              </a>
              <span v-else class="proj-nolink">内部私有项目</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ── Tab 2: 详细档案 (Profile) ── -->
      <section v-show="activeTab === 'profile'" class="tab-panel fade-in-up">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">公开档案与基本信息</h2>
            <p class="panel-desc">维护你的公开名片信息，让访客与系统更好地识别你</p>
          </div>
        </div>

        <div class="profile-info-grid">
          <div class="info-card">
            <span class="info-card-label">登录用户名</span>
            <div class="info-card-value font-mono">{{ userStore.user?.username || '-' }}</div>
            <span class="info-card-tip">唯一系统识别标识（不可修改）</span>
          </div>

          <div class="info-card">
            <span class="info-card-label">显示昵称</span>
            <div class="info-card-value">{{ userStore.user?.nickname || '-' }}</div>
            <span class="info-card-tip">公开显示的作者称呼</span>
          </div>

          <div class="info-card">
            <span class="info-card-label">联系邮箱</span>
            <div class="info-card-value">{{ userStore.user?.email || '未绑定邮箱' }}</div>
            <span class="info-card-tip">接收重要通知与安全验证</span>
          </div>

          <div class="info-card">
            <span class="info-card-label">所在地 / 城市</span>
            <div class="info-card-value">{{ userStore.user?.location || '未设置' }}</div>
            <span class="info-card-tip">你的常驻地域</span>
          </div>

          <div class="info-card">
            <span class="info-card-label">个人主页 / 博客</span>
            <div class="info-card-value">
              <a v-if="userStore.user?.website" :href="userStore.user.website" target="_blank" rel="noopener noreferrer" class="val-link">
                {{ userStore.user.website }}
              </a>
              <span v-else>未提供</span>
            </div>
            <span class="info-card-tip">外部个人主页链接</span>
          </div>

          <div class="info-card">
            <span class="info-card-label">GitHub 主页</span>
            <div class="info-card-value">
              <a v-if="userStore.user?.github" :href="userStore.user.github" target="_blank" rel="noopener noreferrer" class="val-link">
                {{ userStore.user.github }}
              </a>
              <span v-else>未绑定</span>
            </div>
            <span class="info-card-tip">开源主页链接</span>
          </div>

          <div class="info-card info-card--full">
            <span class="info-card-label">个人简介 / 座右铭</span>
            <div class="info-card-value info-card-value--bio">
              {{ userStore.user?.bio || '暂无个人简介描述。' }}
            </div>
          </div>
        </div>
      </section>

      <!-- ── Tab 3: 账号安全与设置 (Security) ── -->
      <section v-show="activeTab === 'security'" class="tab-panel fade-in-up">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">账号安全与偏好管理</h2>
            <p class="panel-desc">保护你的账户凭据安全，管理会话与登录状态</p>
          </div>
        </div>

        <div class="security-cards-stack">
          <!-- 修改密码卡片 -->
          <div class="security-block">
            <div class="sec-icon-badge">
              <Lock :size="20" />
            </div>
            <div class="sec-content">
              <h3>登录密码</h3>
              <p>定期更新高强度的登录密码，建议使用包含字母、数字和符号的组合。</p>
            </div>
            <button class="btn-outline-action" @click="openPasswordDialog">
              修改密码
            </button>
          </div>

          <!-- 会话安全 -->
          <div class="security-block">
            <div class="sec-icon-badge">
              <Shield :size="20" />
            </div>
            <div class="sec-content">
              <h3>身份角色与权限</h3>
              <p>当前账号权限级别：<strong>{{ roleLabel }}</strong>。享有知识创建与智能体深度协作权限。</p>
            </div>
            <div class="role-status-badge" title="当前身份权限有效">
              <Sparkles :size="13" />
              <span>{{ roleLabel }}</span>
            </div>
          </div>

          <!-- 危险区：退出登录 -->
          <div class="security-block danger-block">
            <div class="sec-icon-badge danger">
              <AlertCircle :size="20" />
            </div>
            <div class="sec-content">
              <h3 class="danger-text">退出当前会话</h3>
              <p>退出后将清除本设备的本地认证凭据，需要重新登录方可访问知识库与工作台。</p>
            </div>
            <button class="btn-danger-action" @click="handleLogout">
              <LogOut :size="15" />
              <span>退出登录</span>
            </button>
          </div>
        </div>
      </section>
    </div>

    <!-- ── 资料编辑弹窗 ── -->
    <el-dialog
      v-model="profileDialogVisible"
      title="编辑资料"
      width="600px"
      class="account-dialog account-dialog--profile"
      align-center
      :append-to-body="true"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template #header>
        <div class="dialog-heading">
          <span class="dialog-kicker">PERSONAL INFORMATION</span>
          <h2>编辑个人档案</h2>
          <p>完善你的公开信息，让你的个人知识宇宙更具辨识度。</p>
        </div>
      </template>

      <form @submit.prevent="handleUpdateProfile" class="modern-dialog-form">
        <div class="form-grid-2">
          <div class="form-group">
            <label>登录用户名</label>
            <input
              type="text"
              :value="userStore.user?.username"
              disabled
              class="input-readonly"
            />
          </div>

          <div class="form-group">
            <label for="profile-nickname">显示昵称</label>
            <input
              id="profile-nickname"
              v-model="profileForm.nickname"
              type="text"
              placeholder="请输入你的昵称"
            />
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label for="profile-email">联系邮箱</label>
            <input
              id="profile-email"
              v-model="profileForm.email"
              type="email"
              placeholder="yourname@example.com"
            />
          </div>

          <div class="form-group">
            <label for="profile-location">所在地</label>
            <input
              id="profile-location"
              v-model="profileForm.location"
              type="text"
              placeholder="例如：北京、上海、硅谷"
            />
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label for="profile-website">个人网站</label>
            <input
              id="profile-website"
              v-model="profileForm.website"
              type="url"
              placeholder="https://yourwebsite.com"
            />
          </div>

          <div class="form-group">
            <label for="profile-github">GitHub 链接</label>
            <input
              id="profile-github"
              v-model="profileForm.github"
              type="url"
              placeholder="https://github.com/yourname"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="profile-bio">个人简介 / 座右铭</label>
          <textarea
            id="profile-bio"
            v-model="profileForm.bio"
            rows="3"
            placeholder="写一句介绍，或你的探索信条..."
          ></textarea>
        </div>

        <p v-if="profileMsg" class="form-error-msg">{{ profileMsg }}</p>
      </form>

      <template #footer>
        <button type="button" class="btn-cancel" @click="profileDialogVisible = false">取消</button>
        <button type="button" class="btn-confirm" :disabled="profileLoading" @click="handleUpdateProfile">
          <span v-if="profileLoading" class="btn-spinner"></span>
          {{ profileLoading ? '保存中...' : '保存更改' }}
        </button>
      </template>
    </el-dialog>

    <!-- ── 密码修改弹窗 ── -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="460px"
      class="account-dialog account-dialog--password"
      align-center
      :append-to-body="true"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template #header>
        <div class="dialog-heading">
          <span class="dialog-kicker">SECURITY & CREDENTIALS</span>
          <h2>更新登录密码</h2>
          <p>建议使用 8 位以上，包含大小写字母与数字的强密码。</p>
        </div>
      </template>

      <form @submit.prevent="handleChangePassword" class="modern-dialog-form">
        <div class="form-group">
          <label for="old-password">原密码</label>
          <input
            id="old-password"
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            autocomplete="current-password"
          />
        </div>

        <div class="form-group">
          <label for="new-password">新密码</label>
          <input
            id="new-password"
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="新密码（至少 6 位）"
            autocomplete="new-password"
          />
        </div>

        <div class="form-group">
          <label for="confirm-password">确认新密码</label>
          <input
            id="confirm-password"
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            autocomplete="new-password"
          />
        </div>

        <p v-if="passwordMsg" class="form-error-msg" :class="{ 'form-success-msg': passwordSuccess }">
          {{ passwordMsg }}
        </p>
      </form>

      <template #footer>
        <button type="button" class="btn-cancel" @click="passwordDialogVisible = false">取消</button>
        <button type="button" class="btn-confirm" :disabled="passwordLoading" @click="handleChangePassword">
          <span v-if="passwordLoading" class="btn-spinner"></span>
          {{ passwordLoading ? '更新中...' : '确认更新密码' }}
        </button>
      </template>
    </el-dialog>

    <!-- ── 项目编辑弹窗 ── -->
    <el-dialog
      v-model="projectDialogVisible"
      :title="projectEditingId ? '编辑项目' : '新增项目'"
      width="560px"
      class="account-dialog account-dialog--project"
      align-center
      :append-to-body="true"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template #header>
        <div class="dialog-heading">
          <span class="dialog-kicker">PROJECT MANAGEMENT</span>
          <h2>{{ projectEditingId ? '编辑项目展示' : '新增实践项目' }}</h2>
          <p>录入你的项目名称、定位与访问链接，打造更立体的个人履历。</p>
        </div>
      </template>

      <form @submit.prevent="handleSaveProject" class="modern-dialog-form">
        <div class="form-group">
          <label for="proj-name">项目名称 *</label>
          <input
            id="proj-name"
            v-model="projectForm.name"
            type="text"
            placeholder="例如：Starlore Knowledge Engine"
          />
        </div>

        <div class="form-group">
          <label for="proj-desc">项目定位与描述</label>
          <textarea
            id="proj-desc"
            v-model="projectForm.description"
            rows="3"
            placeholder="简要概括技术架构、解决的核心痛点与功能亮点..."
          ></textarea>
        </div>

        <div class="form-group">
          <label for="proj-url">项目链接 / 源码仓库</label>
          <input
            id="proj-url"
            v-model="projectForm.url"
            type="url"
            placeholder="https://github.com/yourname/repo"
          />
        </div>

        <p v-if="projectMsg" class="form-error-msg">{{ projectMsg }}</p>
      </form>

      <template #footer>
        <button type="button" class="btn-cancel" @click="projectDialogVisible = false">取消</button>
        <button type="button" class="btn-confirm" :disabled="projectLoading" @click="handleSaveProject">
          <span v-if="projectLoading" class="btn-spinner"></span>
          {{ projectLoading ? '保存中...' : '确认保存' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped src="./ProfileView.css"></style>
