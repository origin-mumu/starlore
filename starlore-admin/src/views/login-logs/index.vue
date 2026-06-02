<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import Nav from '@/components/nav.vue'
import request from '@/utils/request'
import { getUserListService } from '@/api/user'

interface LoginLog {
  id: number
  userId: number
  username: string
  ip: string
  country: string
  province: string
  city: string
  loginTime: string
}

const formatLocation = (log: LoginLog) => {
  if (log.country) {
    let s = log.province && log.province !== log.country ? log.province : ''
    if (log.city && log.city !== log.province) s += (s ? ' ' : '') + log.city
    return s || log.country
  }
  return '-'
}

const logs = ref<LoginLog[]>([])
const loading = ref(false)
const currentPage = ref(1)
const total = ref(0)
const totalPages = ref(0)
const pageSize = ref(20)

const filterUsername = ref('')
const filterDateFrom = ref('')
const filterDateTo = ref('')
const users = ref<{ id: number; nickname: string; username: string }[]>([])

const fetchLogs = async () => {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, limit: pageSize.value }
    if (filterUsername.value) params.username = filterUsername.value
    if (filterDateFrom.value) params.dateFrom = filterDateFrom.value
    if (filterDateTo.value) params.dateTo = filterDateTo.value

    const res: any = await request.get('/admin/login-logs', { params })
    logs.value = res.data || []
    total.value = res.total || 0
    totalPages.value = res.pages || 0
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

const fetchUsers = async () => {
  try {
    const res: any = await getUserListService()
    users.value = (res.data || []).map((u: any) => ({
      id: u.id,
      nickname: u.nickname || u.username,
      username: u.username,
    }))
  } catch { /* ignore */ }
}

const goToPage = (p: number) => {
  if (p < 1 || p > totalPages.value) return
  currentPage.value = p
  fetchLogs()
}

const formatTime = (t: string) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

const resetFilter = () => {
  filterUsername.value = ''
  filterDateFrom.value = ''
  filterDateTo.value = ''
  currentPage.value = 1
  fetchLogs()
}

onMounted(() => {
  fetchUsers()
  fetchLogs()
})
</script>

<template>
  <div class="home-layout">
    <div class="sidebar-container"><Nav /></div>
    <div class="content-container">
      <div class="content-box">
        <div class="page-header">
          <h1 class="page-title">登录日志</h1>
        </div>

        <!-- Filter bar -->
        <div class="filter-section">
          <div class="filter-group">
            <label>观星者</label>
            <select v-model="filterUsername" class="filter-select" @change="currentPage=1;fetchLogs()">
              <option value="">全部观星者</option>
              <option v-for="u in users" :key="u.id" :value="u.username">{{ u.nickname }}</option>
            </select>
          </div>
          <div class="filter-group">
            <label>起始日期</label>
            <input v-model="filterDateFrom" type="date" class="filter-input" @change="currentPage=1;fetchLogs()" />
          </div>
          <div class="filter-group">
            <label>结束日期</label>
            <input v-model="filterDateTo" type="date" class="filter-input" @change="currentPage=1;fetchLogs()" />
          </div>
          <button class="action-btn reset-btn" @click="resetFilter">重置</button>
        </div>

        <!-- Table -->
        <div class="table-container">
          <div v-if="loading" class="loading">加载中...</div>
          <div v-else-if="logs.length === 0" class="empty-state">暂无登录记录</div>
          <div v-else>
            <div class="table-header">
              <div class="header-cell">ID</div>
              <div class="header-cell">用户名</div>
              <div class="header-cell">IP 地址</div>
              <div class="header-cell">位置</div>
              <div class="header-cell">登录时间</div>
            </div>
            <div class="table-body">
              <div v-for="log in logs" :key="log.id" class="table-row">
                <div class="table-cell">{{ log.id }}</div>
                <div class="table-cell">{{ log.username }}</div>
                <div class="table-cell">{{ log.ip || '-' }}</div>
                <div class="table-cell">{{ formatLocation(log) }}</div>
                <div class="table-cell">{{ formatTime(log.loginTime) }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="pagination">
          <button class="page-btn" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">上一页</button>
          <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页（共 {{ total }} 条）</span>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
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
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border);
}

.page-title {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
  letter-spacing: -0.02em;
}

.filter-section {
  display: flex;
  gap: var(--space-4);
  align-items: flex-end;
  margin-bottom: var(--space-5);
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-group label {
  font-size: 0.78rem;
  color: var(--ink-muted);
  font-weight: 500;
}

.filter-select,
.filter-input {
  padding: 8px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-family: inherit;
  background: var(--surface);
  color: var(--ink);
  transition: border-color var(--transition);
}

.filter-select:focus,
.filter-input:focus {
  outline: none;
  border-color: var(--accent);
}

.reset-btn {
  padding: 8px 18px;
  background: var(--canvas-deep);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 0.85rem;
  font-family: inherit;
  color: var(--ink-soft);
  transition: all var(--transition);
}

.reset-btn:hover {
  background: var(--border);
  color: var(--ink);
}

.loading,
.empty-state {
  text-align: center;
  padding: var(--space-8) var(--space-5);
  color: var(--ink-muted);
}

.table-container {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 0.8fr 1.2fr 1.2fr 1.5fr 1.8fr;
  background: var(--canvas-deep);
  border-bottom: 1px solid var(--border);
  font-weight: 600;
  color: var(--ink);
  font-size: 0.88rem;
}

.header-cell {
  padding: 14px 16px;
}

.table-body {
  background: var(--surface);
}

.table-row {
  display: grid;
  grid-template-columns: 0.8fr 1.2fr 1.2fr 1.5fr 1.8fr;
  border-bottom: 1px solid var(--border);
  transition: background var(--transition);
}

.table-row:last-child {
  border-bottom: none;
}

.table-row:hover {
  background: var(--canvas-deep);
}

.table-cell {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  color: var(--ink-soft);
  font-size: 0.88rem;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-5);
}

.page-btn {
  padding: 8px 18px;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-family: inherit;
  color: var(--ink-soft);
  font-size: 0.88rem;
  transition: all var(--transition);
}

.page-btn:not(:disabled):hover {
  border-color: var(--accent);
  color: var(--accent);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: var(--ink-muted);
  font-size: 0.88rem;
}

@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }

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
