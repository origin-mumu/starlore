<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Tickets } from '@element-plus/icons-vue'
import { getLoginLogsService } from '@/api/auth'
import { getUserListService } from '@/api/user'
import type { LoginLogItem } from '@/types'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'

const logs = ref<LoginLogItem[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pages: 0, total: 0 })

const filters = reactive({
  username: '',
  dateFrom: '',
  dateTo: '',
})

const users = ref<{ id: number; nickname: string; username: string }[]>([])

const fetchLogs = async (): Promise<void> => {
  loading.value = true
  try {
    const result = await getLoginLogsService({
      page: pagination.current,
      limit: 20,
      username: filters.username || undefined,
      dateFrom: filters.dateFrom || undefined,
      dateTo: filters.dateTo || undefined,
    })
    logs.value = result.data ?? []
    pagination.total = result.total ?? 0
    pagination.pages = result.pages ?? 0
  } catch {
    /* 非管理员忽略 */
  } finally {
    loading.value = false
  }
}

const fetchUsers = async (): Promise<void> => {
  try {
    const list = await getUserListService()
    users.value = list.map((u) => ({
      id: u.id,
      nickname: u.nickname || u.username,
      username: u.username,
    }))
  } catch {
    /* 非管理员忽略 */
  }
}

const onFilterChange = (): void => {
  pagination.current = 1
  void fetchLogs()
}

const resetFilter = (): void => {
  filters.username = ''
  filters.dateFrom = ''
  filters.dateTo = ''
  onFilterChange()
}

const goToPage = (page: number): void => {
  if (page < 1 || page > pagination.pages) return
  pagination.current = page
  void fetchLogs()
}

const formatTime = (time: string): string =>
  time ? new Date(time).toLocaleString('zh-CN') : '-'

const formatLocation = (log: LoginLogItem): string => {
  if (!log.country) return '-'
  const province = log.province && log.province !== log.country ? log.province : ''
  const city = log.city && log.city !== log.province ? log.city : ''
  return [province, city].filter(Boolean).join(' ') || log.country
}

onMounted(() => {
  void fetchUsers()
  void fetchLogs()
})
</script>

<template>
  <div class="login-log">
    <header class="page-header">
      <div>
        <h1 class="page-title">登录日志</h1>
        <p class="page-subtitle">共 {{ pagination.total }} 条记录</p>
      </div>
    </header>

    <div class="filter-bar">
      <div class="form-field login-log__filter">
        <label>观星者</label>
        <select v-model="filters.username" class="form-select" @change="onFilterChange">
          <option value="">全部观星者</option>
          <option v-for="user in users" :key="user.id" :value="user.username">
            {{ user.nickname }}
          </option>
        </select>
      </div>
      <div class="form-field login-log__filter">
        <label>起始日期</label>
        <input v-model="filters.dateFrom" class="form-input" type="date" @change="onFilterChange" />
      </div>
      <div class="form-field login-log__filter">
        <label>结束日期</label>
        <input v-model="filters.dateTo" class="form-input" type="date" @change="onFilterChange" />
      </div>
      <button class="btn btn--secondary" type="button" @click="resetFilter">重置</button>
    </div>

    <LoadingState v-if="loading" />

    <EmptyState v-else-if="logs.length === 0" text="暂无登录记录">
      <template #icon>
        <el-icon><Tickets /></el-icon>
      </template>
    </EmptyState>

    <div v-else class="data-table" style="--table-cols: 0.6fr 1.1fr 1.1fr 1.4fr 1.8fr">
      <div class="data-table__header">
        <div>ID</div>
        <div>用户名</div>
        <div>IP 地址</div>
        <div>位置</div>
        <div>登录时间</div>
      </div>
      <div v-for="log in logs" :key="log.id" class="data-table__row">
        <div>{{ log.id }}</div>
        <div><span class="cell-primary">{{ log.username }}</span></div>
        <div>{{ log.ip || '-' }}</div>
        <div>{{ formatLocation(log) }}</div>
        <div>{{ formatTime(log.loginTime) }}</div>
      </div>
    </div>

    <div v-if="pagination.pages > 1" class="pagination">
      <button
        class="btn btn--secondary btn--sm"
        :disabled="pagination.current <= 1"
        @click="goToPage(pagination.current - 1)"
      >
        上一页
      </button>
      <span class="pagination__info">
        第 {{ pagination.current }} / {{ pagination.pages }} 页（共 {{ pagination.total }} 条）
      </span>
      <button
        class="btn btn--secondary btn--sm"
        :disabled="pagination.current >= pagination.pages"
        @click="goToPage(pagination.current + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped>
.login-log__filter {
  min-width: 160px;
}
</style>
