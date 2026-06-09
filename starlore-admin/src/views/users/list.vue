<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Nav from '@/components/nav.vue'
import {
  getUserListService,
  updateUserService,
  deleteUserService,
  reindexService,
  type UserItem,
} from '@/api/user'

const users = ref<UserItem[]>([])
const loading = ref(false)

const editDialogVisible = ref(false)
const editingUser = ref<Partial<UserItem> & { id: number }>({ id: 0 })
const editForm = ref({
  nickname: '',
  email: '',
  bio: '',
  location: '',
  website: '',
  github: '',
  role: 'user',
  aiDailyLimit: 10,
})

const fetchUsers = async () => {
  loading.value = true
  try {
    const res: any = await getUserListService()
    users.value = res.data || []
  } catch (err) {
    console.error('获取用户列表失败', err)
  } finally {
    loading.value = false
  }
}

const openEditDialog = (user: UserItem) => {
  editingUser.value = { id: user.id }
  editForm.value = {
    nickname: user.nickname || '',
    email: user.email || '',
    bio: user.bio || '',
    location: user.location || '',
    website: user.website || '',
    github: user.github || '',
    role: (user as any).role || 'user',
    aiDailyLimit: (user as any).aiDailyLimit ?? 10,
  }
  editDialogVisible.value = true
}

const submitEdit = async () => {
  try {
    await updateUserService(editingUser.value.id, editForm.value)
    editDialogVisible.value = false
    await fetchUsers()
  } catch (err) {
    console.error('更新用户失败', err)
  }
}

const confirmDelete = async (id: number, username: string) => {
  if (!confirm(`确定删除观星者「${username}」吗？`)) return
  try {
    await deleteUserService(id)
    await fetchUsers()
  } catch (err) {
    console.error('删除用户失败', err)
  }
}

const formatDate = (d: string) => {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}

onMounted(fetchUsers)

const reindexingId = ref<number | null>(null)
const handleReindex = async (userId: number) => {
  if (reindexingId.value !== null) return
  reindexingId.value = userId
  try {
    const res: any = await reindexService(userId)
    alert(res.message || '索引重建成功')
  } catch (err: any) {
    alert('重建失败: ' + (err?.message || '未知错误'))
  } finally {
    reindexingId.value = null
  }
}
</script>

<template>
  <div class="home-layout">
    <div class="sidebar-container">
      <Nav />
    </div>
    <div class="content-container">
      <div class="content-box">
        <div class="page-header">
          <h1 class="page-title">观星者管理</h1>
        </div>

        <div v-if="loading" class="loading">加载中...</div>

        <div v-else-if="users.length === 0" class="empty-state">
          <p>暂无观星者数据</p>
        </div>

        <div v-else class="table-container">
          <div class="table-header">
            <div class="header-cell">ID</div>
            <div class="header-cell">用户名</div>
            <div class="header-cell">昵称</div>
            <div class="header-cell">角色</div>
            <div class="header-cell">每日上限</div>
            <div class="header-cell">邮箱</div>
            <div class="header-cell">注册时间</div>
            <div class="header-cell">操作</div>
          </div>
          <div class="table-body">
            <div v-for="user in users" :key="user.id" class="table-row">
              <div class="table-cell">{{ user.id }}</div>
              <div class="table-cell">{{ user.username }}</div>
              <div class="table-cell">{{ user.nickname || '-' }}</div>
              <div class="table-cell">
                <span :class="['role-badge', (user as any).role]">{{ (user as any).role || 'user' }}</span>
              </div>
              <div class="table-cell">{{ (user as any).aiDailyLimit ?? 10 }}</div>
              <div class="table-cell">{{ user.email || '-' }}</div>
              <div class="table-cell">{{ formatDate(user.createdAt) }}</div>
              <div class="table-cell">
                <div class="action-buttons">
                  <button class="action-btn edit-btn" @click="openEditDialog(user)">编辑</button>
                  <button class="action-btn reindex-btn" :disabled="reindexingId !== null" @click="handleReindex(user.id)">
                    {{ reindexingId === user.id ? '索引中...' : '索引' }}
                  </button>
                  <button class="action-btn delete-btn" @click="confirmDelete(user.id, user.username)">删除</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑用户弹窗 -->
    <div v-if="editDialogVisible" class="dialog-overlay" @click.self="editDialogVisible = false">
      <div class="dialog-panel">
        <h3>编辑观星者</h3>
        <div class="dialog-form">
          <label>昵称</label>
          <input v-model="editForm.nickname" type="text" />
          <label>邮箱</label>
          <input v-model="editForm.email" type="email" />
          <label>个人简介</label>
          <textarea v-model="editForm.bio" rows="3"></textarea>
          <label>所在地</label>
          <input v-model="editForm.location" type="text" />
          <label>网站</label>
          <input v-model="editForm.website" type="text" />
          <label>GitHub</label>
          <input v-model="editForm.github" type="text" />
          <label>角色</label>
          <select v-model="editForm.role" class="dialog-select">
            <option value="user">普通观星者</option>
            <option value="member">会员</option>
            <option value="admin">管理员</option>
          </select>
          <label>每日 AI 上限（-1 为不限）</label>
          <input v-model.number="editForm.aiDailyLimit" type="number" min="-1" />
        </div>
        <div class="dialog-actions">
          <button class="action-btn edit-btn" @click="submitEdit">保存</button>
          <button class="action-btn cancel-btn" @click="editDialogVisible = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--border);
}

.page-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
  letter-spacing: -0.02em;
}

.reindex-btn {
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 0.85rem;
}
.reindex-btn:hover:not(:disabled) {
  background: var(--accent);
  color: #FDFBF5;
}
.reindex-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading,
.empty-state {
  text-align: center;
  padding: var(--space-8) var(--space-5);
  color: var(--ink-muted);
  font-size: 1rem;
}

.table-container {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 0.5fr 1fr 1fr 0.8fr 0.8fr 1.2fr 1fr 2fr;
  background: var(--canvas-deep);
  border-bottom: 1px solid var(--border);
  font-weight: 600;
  color: var(--ink);
  font-size: 0.88rem;
}

.header-cell {
  padding: 14px 16px;
  text-align: left;
}

.table-body {
  background: var(--surface);
}

.table-row {
  display: grid;
  grid-template-columns: 0.5fr 1fr 1fr 0.8fr 0.8fr 1.2fr 1fr 2fr;
  border-bottom: 1px solid var(--border);
  transition: background-color var(--transition);
}

.table-row:hover {
  background-color: var(--canvas-deep);
}

.table-row:last-child {
  border-bottom: none;
}

.table-cell {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  font-size: 0.9rem;
  color: var(--ink-soft);
}

.action-buttons {
  display: flex;
  gap: var(--space-2);
}

.action-btn {
  padding: 6px 14px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition);
}

.edit-btn {
  background: var(--accent-soft);
  color: var(--accent);
}

.edit-btn:hover {
  background: var(--accent);
  color: #FDFBF5;
}

.delete-btn {
  background: var(--status-error-bg);
  color: var(--status-error-text);
}

.delete-btn:hover {
  background: var(--status-error-text);
  color: #FDFBF5;
}

.cancel-btn {
  background: var(--canvas-deep);
  color: var(--ink-soft);
  border: 1px solid var(--border);
}

.cancel-btn:hover {
  background: var(--border);
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: oklch(0.2 0.01 50 / 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  backdrop-filter: blur(4px);
}

.dialog-panel {
  background: var(--surface);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  width: 480px;
  max-width: 90vw;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-card-hover);
}

.dialog-panel h3 {
  margin: 0 0 var(--space-5);
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--ink);
}

.dialog-form label {
  display: block;
  margin-bottom: 4px;
  margin-top: var(--space-4);
  font-weight: 500;
  color: var(--ink-soft);
  font-size: 0.88rem;
}

.dialog-form label:first-child {
  margin-top: 0;
}

.role-badge {
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 0.78rem;
  font-weight: 600;
}

.role-badge.admin {
  background: var(--accent-soft);
  color: var(--accent);
}

.role-badge.member {
  background: var(--warm-soft);
  color: var(--warm);
}

.role-badge.user {
  background: var(--status-published-bg);
  color: var(--status-published-text);
}

.dialog-form input,
.dialog-form textarea,
.dialog-form select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-family: inherit;
  box-sizing: border-box;
  background: var(--surface);
  color: var(--ink);
  transition: border-color var(--transition), box-shadow var(--transition);
}

.dialog-form input:focus,
.dialog-form textarea:focus,
.dialog-form select:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px oklch(0.55 0.15 35 / 0.1);
}

.dialog-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  margin-top: var(--space-6);
}

@media (max-width: 768px) {
  .table-header,
  .table-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .header-cell,
  .table-cell {
    padding: 8px 12px;
  }
}
</style>
