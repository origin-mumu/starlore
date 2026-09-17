<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  deleteUserService,
  getUserListService,
  reindexService,
  updateUserService,
} from '@/api/user'
import type { UserItem } from '@/types'

const loading = ref(false)
const users = ref<UserItem[]>([])
const keyword = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref<UserItem | null>(null)

const form = reactive({
  nickname: '',
  email: '',
  bio: '',
  location: '',
  website: '',
  github: '',
  role: 'user',
  aiDailyLimit: 10,
})

const ROLE_LABELS: Record<string, string> = {
  admin: '管理员',
  member: '会员',
  user: '普通用户',
}

const filteredUsers = (): UserItem[] => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return users.value
  return users.value.filter(
    (u) =>
      u.username.toLowerCase().includes(kw) ||
      (u.nickname ?? '').toLowerCase().includes(kw) ||
      (u.email ?? '').toLowerCase().includes(kw),
  )
}

function usageText(u: UserItem): string {
  return `${u.aiTodayCount ?? 0} / ${u.aiDailyLimit ?? 10}`
}

function usagePercent(u: UserItem): number {
  const limit = u.aiDailyLimit ?? 10
  if (limit <= 0) return 0
  return Math.min(100, Math.round(((u.aiTodayCount ?? 0) / limit) * 100))
}

function openEdit(u: UserItem): void {
  editing.value = u
  form.nickname = u.nickname ?? ''
  form.email = u.email ?? ''
  form.bio = u.bio ?? ''
  form.location = u.location ?? ''
  form.website = u.website ?? ''
  form.github = u.github ?? ''
  form.role = u.role
  form.aiDailyLimit = u.aiDailyLimit ?? 10
  dialogVisible.value = true
}

async function handleSave(): Promise<void> {
  if (!editing.value) return
  saving.value = true
  try {
    await updateUserService(editing.value.id, { ...form })
    ElMessage.success('用户信息已更新')
    dialogVisible.value = false
    await loadUsers()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '更新失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(u: UserItem): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定删除用户「${u.username}」吗？该操作不可恢复。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteUserService(u.id)
    ElMessage.success('已删除')
    await loadUsers()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function handleReindex(u: UserItem): Promise<void> {
  try {
    await reindexService(u.id)
    ElMessage.success(`已为「${u.username}」重建语义索引`)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '重建索引失败')
  }
}

async function loadUsers(): Promise<void> {
  loading.value = true
  try {
    users.value = await getUserListService()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载用户失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadUsers)
</script>

<template>
  <div>
    <div class="page-head">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名 / 昵称 / 邮箱"
        clearable
        :prefix-icon="Search"
        style="width: 260px"
      />
      <span class="total-hint">共 {{ users.length }} 位观星者</span>
    </div>

    <div class="table-wrap">
      <el-table v-loading="loading" :data="filteredUsers()" empty-text="暂无用户">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="110">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <span class="pill muted">{{ ROLE_LABELS[row.role] ?? row.role }}</span>
          </template>
        </el-table-column>
        <el-table-column label="今日 AI 用量" min-width="170">
          <template #default="{ row }">
            <div class="usage-cell">
              <el-progress
                :percentage="usagePercent(row)"
                :stroke-width="7"
                :show-text="false"
                class="usage-bar"
              />
              <span class="usage-text">{{ usageText(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="150">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleDateString('zh-CN') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link @click="handleReindex(row)">重建索引</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="`编辑用户：${editing?.username ?? ''}`" width="560px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio-button value="user">普通用户</el-radio-button>
            <el-radio-button value="member">会员</el-radio-button>
            <el-radio-button value="admin">管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="AI 日限额">
          <el-input-number v-model="form.aiDailyLimit" :min="0" :max="9999" />
          <span class="form-hint">0 表示按角色默认（member 99 / user 10）</span>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="网站">
          <el-input v-model="form.website" />
        </el-form-item>
        <el-form-item label="GitHub">
          <el-input v-model="form.github" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.bio" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.total-hint {
  font-size: 12.5px;
  color: var(--text-muted);
}

.usage-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.usage-bar {
  flex: 1;
  --el-progress-color: var(--brand-primary);
}

.usage-text {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}

.form-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
