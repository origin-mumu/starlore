<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import { changePasswordService } from '@/api/auth'
import { uploadImage } from '@/api/article'
import {
  listProjectsService,
  createProjectService,
  updateProjectService,
  deleteProjectService,
  type Project,
  type ProjectParams,
} from '@/api/project'
import SideBar from '@/components/sideBar.vue'
import defaultAvatar from '@/assets/avatar.jpg'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const guestAllowedPaths = ['/', '/about', '/categories', '/vr']

const handleLogout = () => {
  userStore.logout()
  const path = route.path
  const isGuestAllowed = guestAllowedPaths.some(p => path === p || path.startsWith(p + '/'))
  if (isGuestAllowed) {
    router.go(0)
  } else {
    router.push('/login')
  }
}

// 头像上传
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

// 个人资料弹窗
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

// 密码弹窗
const passwordDialogVisible = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const passwordMsg = ref('')
const passwordSuccess = ref(false)
const passwordLoading = ref(false)

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
    profileDialogVisible.value = false
  } catch (e: any) {
    profileMsg.value = e.message || '更新失败，请重试'
  } finally {
    profileLoading.value = false
  }
}

const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordMsg.value = ''
  passwordSuccess.value = false
  passwordDialogVisible.value = true
}

// ─── 项目管理 ───────────────────────────────────────
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
    } else {
      await createProjectService(projectForm)
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
  ElMessageBox.confirm(`确定删除项目「${p.name}」？`, '确认删除', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await deleteProjectService(p.id)
    await fetchProjects()
  }).catch(() => {/* ignore */})
}

onMounted(() => {
  fetchProjects()
})

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
    setTimeout(() => {
      passwordDialogVisible.value = false
    }, 1200)
  } catch (e: any) {
    passwordMsg.value = e.message || '修改失败，请重试'
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <section class="page-header">
      <div class="container">
        <h1>个人中心</h1>
      </div>
    </section>

    <section class="section-parchment">
      <div class="container">
        <div class="content-layout">
          <main class="main-content">
            <!-- 个人资料 -->
            <div class="profile-card fade-in-up">
              <div class="section-header">
                <h2 class="section-heading">个人资料</h2>
                <button class="btn-outline" @click="openProfileDialog">编辑</button>
              </div>

              <div class="avatar-section">
                <div class="avatar-wrap" @click="triggerAvatarSelect">
                  <img
                    class="profile-avatar"
                    :src="userStore.user?.avatar || defaultAvatar"
                    alt="头像"
                  />
                  <div class="avatar-overlay">
                    <span v-if="avatarUploading" class="avatar-spinner"></span>
                    <span v-else class="avatar-hint">更换头像</span>
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

              <div class="info-list">
                <div class="info-item">
                  <span class="info-label">用户名</span>
                  <span class="info-value">{{ userStore.user?.username || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">昵称</span>
                  <span class="info-value">{{ userStore.user?.nickname || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">邮箱</span>
                  <span class="info-value">{{ userStore.user?.email || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">所在地</span>
                  <span class="info-value">{{ userStore.user?.location || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">个人网站</span>
                  <span class="info-value">
                    <a v-if="userStore.user?.website" :href="userStore.user.website" target="_blank" class="info-link">{{ userStore.user.website }}</a>
                    <span v-else>-</span>
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">GitHub</span>
                  <span class="info-value">
                    <a v-if="userStore.user?.github" :href="userStore.user.github" target="_blank" class="info-link">{{ userStore.user.github }}</a>
                    <span v-else>-</span>
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">个人简介</span>
                  <span class="info-value info-value--bio">{{ userStore.user?.bio || '-' }}</span>
                </div>
              </div>
            </div>

            <!-- 编辑资料弹窗 -->
            <el-dialog
              v-model="profileDialogVisible"
              title="编辑资料"
              width="640px"
              class="account-dialog account-dialog--profile"
              align-center
              :append-to-body="true"
              :close-on-click-modal="false"
              destroy-on-close
            >
              <template #header>
                <div class="dialog-heading">
                  <span class="dialog-kicker">个人资料</span>
                  <h2>编辑资料</h2>
                  <p>完善公开信息，让你的个人空间更容易被识别。</p>
                </div>
              </template>
              <form @submit.prevent="handleUpdateProfile" class="profile-form">
                <div class="form-field">
                  <label>用户名</label>
                  <input
                    type="text"
                    :value="userStore.user?.username"
                    disabled
                    class="input-disabled"
                  />
                </div>
                <div class="form-field">
                  <label for="profile-nickname">昵称</label>
                  <input
                    id="profile-nickname"
                    v-model="profileForm.nickname"
                    type="text"
                    placeholder="给自己起个昵称"
                  />
                </div>
                <div class="form-field">
                  <label for="profile-email">邮箱</label>
                  <input
                    id="profile-email"
                    v-model="profileForm.email"
                    type="email"
                    placeholder="your@email.com"
                  />
                </div>
                <div class="form-field">
                  <label for="profile-location">所在地</label>
                  <input
                    id="profile-location"
                    v-model="profileForm.location"
                    type="text"
                    placeholder="例如：上海"
                  />
                </div>
                <div class="form-field">
                  <label for="profile-website">个人网站</label>
                  <input
                    id="profile-website"
                    v-model="profileForm.website"
                    type="url"
                    placeholder="https://example.com"
                  />
                </div>
                <div class="form-field">
                  <label for="profile-github">GitHub</label>
                  <input
                    id="profile-github"
                    v-model="profileForm.github"
                    type="url"
                    placeholder="https://github.com/yourname"
                  />
                </div>
                <div class="form-field form-field--wide">
                  <label for="profile-bio">个人简介</label>
                  <textarea
                    id="profile-bio"
                    v-model="profileForm.bio"
                    rows="3"
                    placeholder="介绍一下自己吧..."
                  ></textarea>
                </div>
                <p v-if="profileMsg" class="form-msg">
                  {{ profileMsg }}
                </p>
              </form>
              <template #footer>
                <button type="button" class="btn-cancel" @click="profileDialogVisible = false">取消</button>
                <button type="button" class="btn-primary" :disabled="profileLoading" @click="handleUpdateProfile">
                  <span v-if="profileLoading" class="btn-spinner"></span>
                  {{ profileLoading ? '保存中...' : '保存' }}
                </button>
              </template>
            </el-dialog>

            <!-- 账户安全 -->
            <div class="profile-card fade-in-up" style="animation-delay: 120ms">
              <h2 class="section-heading">账户安全</h2>
              <div class="security-row">
                <div class="security-info">
                  <span class="security-label">密码</span>
                  <span class="security-desc">定期修改密码可以保护账户安全</span>
                </div>
                <button class="btn-outline" @click="openPasswordDialog">修改密码</button>
              </div>
            </div>

            <!-- 修改密码弹窗 -->
            <el-dialog
              v-model="passwordDialogVisible"
              title="修改密码"
              width="440px"
              class="account-dialog account-dialog--password"
              align-center
              :append-to-body="true"
              :close-on-click-modal="false"
              destroy-on-close
            >
              <template #header>
                <div class="dialog-heading">
                  <span class="dialog-kicker">账户安全</span>
                  <h2>修改密码</h2>
                  <p>建议使用至少 8 位且不与其他网站重复的密码。</p>
                </div>
              </template>
              <form @submit.prevent="handleChangePassword" class="password-form">
                <div class="form-field">
                  <label for="old-password">原密码</label>
                  <input
                    id="old-password"
                    v-model="passwordForm.oldPassword"
                    type="password"
                    placeholder="请输入原密码"
                    autocomplete="current-password"
                  />
                </div>
                <div class="form-field">
                  <label for="new-password">新密码</label>
                  <input
                    id="new-password"
                    v-model="passwordForm.newPassword"
                    type="password"
                    placeholder="至少6位新密码"
                    autocomplete="new-password"
                  />
                </div>
                <div class="form-field">
                  <label for="confirm-password">确认新密码</label>
                  <input
                    id="confirm-password"
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入新密码"
                    autocomplete="new-password"
                  />
                </div>
                <p
                  v-if="passwordMsg"
                  class="form-msg"
                  :class="{ success: passwordSuccess }"
                >
                  {{ passwordMsg }}
                </p>
              </form>
              <template #footer>
                <button type="button" class="btn-cancel" @click="passwordDialogVisible = false">取消</button>
                <button type="button" class="btn-primary" :disabled="passwordLoading" @click="handleChangePassword">
                  <span v-if="passwordLoading" class="btn-spinner"></span>
                  {{ passwordLoading ? '修改中...' : '确认修改' }}
                </button>
              </template>
            </el-dialog>

            <!-- 退出登录 -->
            <div class="profile-card profile-card--danger fade-in-up" style="animation-delay: 180ms">
              <div class="danger-row">
                <div class="danger-info">
                  <span class="danger-label">退出登录</span>
                  <span class="danger-desc">退出当前账号，返回首页</span>
                </div>
                <button class="btn-logout" @click="handleLogout">退出登录</button>
              </div>
            </div>

            <!-- 我的项目 -->
            <div class="profile-card fade-in-up" style="animation-delay: 240ms">
              <div class="section-header">
                <h2 class="section-heading">我的项目</h2>
                <button class="btn-outline" @click="openProjectDialog()">新增项目</button>
              </div>

              <div v-if="projectsLoading" class="projects-loading">加载中...</div>
              <div v-else-if="projects.length === 0" class="projects-empty">暂无项目，点击上方按钮添加</div>
              <div v-else class="projects-list">
                <div v-for="p in projects" :key="p.id" class="project-card">
                  <div class="project-info">
                    <h4 class="project-name">{{ p.name }}</h4>
                    <p v-if="p.description" class="project-desc">{{ p.description }}</p>
                    <a v-if="p.url" :href="p.url" target="_blank" class="project-link">{{ p.url }}</a>
                  </div>
                  <div class="project-actions">
                    <button class="project-btn" @click="openProjectDialog(p)" title="编辑">&#9998;</button>
                    <button class="project-btn project-btn--del" @click="confirmDeleteProject(p)" title="删除">&times;</button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 项目编辑弹窗 -->
            <el-dialog
              v-model="projectDialogVisible"
              :title="projectEditingId ? '编辑项目' : '新增项目'"
              width="540px"
              class="account-dialog account-dialog--project"
              align-center
              :append-to-body="true"
              :close-on-click-modal="false"
              destroy-on-close
            >
              <template #header>
                <div class="dialog-heading">
                  <span class="dialog-kicker">项目管理</span>
                  <h2>{{ projectEditingId ? '编辑项目' : '新增项目' }}</h2>
                  <p>添加或编辑你的个人展示项目，丰富个人作品集。</p>
                </div>
              </template>
              <form @submit.prevent="handleSaveProject" class="profile-form">
                <div class="form-field">
                  <label for="proj-name">项目名称 *</label>
                  <input id="proj-name" v-model="projectForm.name" type="text" placeholder="请输入项目名称" />
                </div>
                <div class="form-field">
                  <label for="proj-desc">项目描述</label>
                  <textarea id="proj-desc" v-model="projectForm.description" rows="3" placeholder="简要描述项目"></textarea>
                </div>
                <div class="form-field">
                  <label for="proj-url">项目链接</label>
                  <input id="proj-url" v-model="projectForm.url" type="url" placeholder="https://github.com/..." />
                </div>
                <div class="form-field">
                  <label for="proj-image">项目图片</label>
                  <input id="proj-image" v-model="projectForm.image" type="url" placeholder="图片URL（选填）" />
                </div>
                <p v-if="projectMsg" class="form-msg">{{ projectMsg }}</p>
              </form>
              <template #footer>
                <button type="button" class="btn-cancel" @click="projectDialogVisible = false">取消</button>
                <button type="button" class="btn-primary" :disabled="projectLoading" @click="handleSaveProject">
                  <span v-if="projectLoading" class="btn-spinner"></span>
                  {{ projectLoading ? '保存中...' : '保存' }}
                </button>
              </template>
            </el-dialog>
          </main>

          <aside class="sidebar-area">
            <SideBar />
          </aside>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped src="./ProfileView.css"></style>
