<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getLoginLogsService } from '@/api/auth'
import type { LoginLogItem } from '@/types'

const loading = ref(false)
const logs = ref<LoginLogItem[]>([])
const total = ref(0)

const filters = reactive({
  page: 1,
  limit: 20,
  username: '',
  dateFrom: '',
  dateTo: '',
})

const dateRange = ref<[string, string] | null>(null)

async function loadLogs(): Promise<void> {
  loading.value = true
  try {
    const res = await getLoginLogsService({
      page: filters.page,
      limit: filters.limit,
      username: filters.username || undefined,
      dateFrom: dateRange.value?.[0] || undefined,
      dateTo: dateRange.value?.[1] || undefined,
    })
    logs.value = res.data ?? []
    total.value = res.total ?? 0
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载登录日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  filters.page = 1
  void loadLogs()
}

function handleReset(): void {
  filters.username = ''
  dateRange.value = null
  handleSearch()
}

function formatTime(value: string): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

function locationOf(row: LoginLogItem): string {
  return [row.country, row.province, row.city].filter(Boolean).join(' ') || '-'
}

onMounted(loadLogs)
</script>

<template>
  <div>
    <div class="table-wrap">
      <div class="filter-bar head-filter">
        <el-input
          v-model="filters.username"
          placeholder="用户名"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 250px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="logs" empty-text="暂无登录记录">
        <el-table-column prop="username" label="用户" min-width="110" />
        <el-table-column prop="ip" label="IP" min-width="130" />
        <el-table-column label="归属地" min-width="170">
          <template #default="{ row }">{{ locationOf(row) }}</template>
        </el-table-column>
        <el-table-column prop="userAgent" label="设备" min-width="220" show-overflow-tooltip />
        <el-table-column label="登录时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.loginTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="filters.page"
          :page-size="filters.limit"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="loadLogs"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.head-filter {
  margin-bottom: 12px;
}
</style>
