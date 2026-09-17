<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { User } from '@element-plus/icons-vue'
import {
  getUserListService,
  updateUserService,
  deleteUserService,
  reindexService,
} from '@/api/user'
import type { UserItem, UserPayload } from '@/types'
import AppModal from '@/components/common/AppModal.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'

const users = ref<UserItem[]>([])
const loading = ref(false)

const editDialogVisible = ref(false)
const editingUserId = ref<number>(0)
const editForm = reactive<UserPayload>({
  nickname: '',
  email: '',
  bio: '',
  location: '',
  website: '',
  github: '',
  role: 'user',
  aiDailyLimit: 10,
})

const reindexingId = ref<number | null>(null)

const fetchUsers = async (): Promise<void> => {
  loading.value = true
  try {
    users.value = (await getUserListService()) ?? []
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const openEditDialog = (user: UserItem): void => {
  editingUserId.value = user.id
  editForm.nickname = user.nickname || ''
  editForm.email = user.email || ''
  editForm.bio = user.bio || ''
  editForm.location = user.location || ''
  editForm.website = user.website || ''
  editForm.github = user.github || ''
  editForm.role = user.role || 'user'
  editForm.aiDailyLimit = user.aiDailyLimit ?? 10
  editDialogVisible.value = true
}

const submitEdit = async (): Promise<void> => {
  try {
    await updateUserService(editingUserId.value, { ...editForm })
    editDialogVisible.value = false
    ElMessage.success('保存成功')
    await fetchUsers()
  } catch (error) {
    console.error('更新用户失败:', error)
    ElMessage.error('更新失败，请重试')
  }
}

const confirmDelete = async (user: UserItem): Promise<void> => {
  if (!confirm(`确定删除观星者「${user.username}」吗？`)) return
  try {
    await deleteUserService(user.id)
    ElMessage.success('删除成功')
    await fetchUsers()
  } catch (error) {
    console.error('删除用户失败:', error)
    ElMessage.error('删除失败，请重试')
  }
}

const handleReindex = async (userId: number): Promise<void> => {
  if (reindexingId.value !== null) return
  reindexingId.value = userId
  try {
    const result = await reindexService(userId)
    ElMessage.success(result.message || '索引重建成功')
  } catch (error) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`重建失败: ${message}`)
  } finally {
    reindexingId.value = null
  }
}

const roleLabel = (role: string): string =>
  role === 'admin' ? '管理员' : role === 'member' ? '会员' : '普通观星者'

const rolePillClass = (role: string): string =>
  role === 'admin' ? 'pill--brand' : role === 'member' ? 'pill--warning' : 'pill--info'

const formatDate = (date: string): string =>
  date ? new Date(date).toLocaleDateString('zh-CN') : '-'

onMounted(() => {
  void fetchUsers()
})
</script>

<template>
  <div class="user-list">
    <header class="page-header">
      <div>
        <h1 class="page-title">观星者管理</h1>
        <p class="page-subtitle">共 {{ users.length }} 位观星者</p>
      </div>
    </header>

    <LoadingState v-if="loading" />

    <EmptyState v-else-if="users.length === 0" text="暂无观星者数据">
      <template #icon>
        <el-icon><User /></el-icon>
      </template>
    </EmptyState>

    <div
      v-else
      class="data-table"
      style="--table-cols: 0.5fr 1fr 1fr 0.9fr 0.8fr 1.3fr 1fr 1.8fr"
    >
      <div class="data-table__header">
        <div>ID</div>
        <div>用户名</div>
        <div>昵称</div>
        <div>角色</div>
        <div>每日上限</div>
        <div>邮箱</div>
        <div>注册时间</div>
        <div>操作</div>
      </div>
      <div v-for="user in users" :key="user.id" class="data-table__row">
        <div>{{ user.id }}</div>
        <div><span class="cell-primary">{{ user.username }}</span></div>
        <div>{{ user.nickname || '-' }}</div>
        <div>
          <span class="pill" :class="rolePillClass(user.role)">{{ roleLabel(user.role) }}</span>
        </div>
        <div>{{ user.aiDailyLimit ?? 10 }}</div>
        <div>{{ user.email || '-' }}</div>
        <div>{{ formatDate(user.createdAt) }}</div>
        <div>
          <div class="user-list__actions">
            <button class="btn btn--secondary btn--sm" type="button" @click="openEditDialog(user)">
              编辑
            </button>
            <button
              class="btn btn--secondary btn--sm"
              type="button"
              :disabled="reindexingId !== null"
              @click="handleReindex(user.id)"
            >
              {{ reindexingId === user.id ? '索引中...' : '索引' }}
            </button>
            <button class="btn btn--danger btn--sm" type="button" @click="confirmDelete(user)">
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <AppModal title="编辑观星者" :width="500" @close="editDialogVisible = false">
      <div class="user-list__form">
        <div class="form-field">
          <label>昵称</label>
          <input v-model="editForm.nickname" class="form-input" type="text" />
        </div>
        <div class="form-field">
          <label>邮箱</label>
          <input v-model="editForm.email" class="form-input" type="email" />
        </div>
        <div class="form-field">
          <label>个人简介</label>
          <textarea v-model="editForm.bio" class="form-textarea" rows="3"></textarea>
        </div>
        <div class="user-list__form-row">
          <div class="form-field">
            <label>所在地</label>
            <input v-model="editForm.location" class="form-input" type="text" />
          </div>
          <div class="form-field">
            <label>角色</label>
            <select v-model="editForm.role" class="form-select">
              <option value="user">普通观星者</option>
              <option value="member">会员</option>
              <option value="admin">管理员</option>
            </select>
          </div>
        </div>
        <div class="user-list__form-row">
          <div class="form-field">
            <label>网站</label>
            <input v-model="editForm.website" class="form-input" type="text" />
          </div>
          <div class="form-field">
            <label>GitHub</label>
            <input v-model="editForm.github" class="form-input" type="text" />
          </div>
        </div>
        <div class="form-field">
          <label>每日 AI 上限（-1 为不限）</label>
          <input v-model.number="editForm.aiDailyLimit" class="form-input" type="number" min="-1" />
        </div>
      </div>
      <template #footer>
        <button class="btn btn--secondary" type="button" @click="editDialogVisible = false">
          取消
        </button>
        <button class="btn btn--primary" type="button" @click="submitEdit">保存</button>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.user-list__actions {
  display: flex;
  gap: 8px;
}

.user-list__form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.user-list__form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
