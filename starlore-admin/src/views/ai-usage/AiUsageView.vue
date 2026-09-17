<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getAiUsageService, getAiUsageSummaryService } from '@/api/ai-usage'
import { getUserListService } from '@/api/user'
import { useECharts } from '@/composables/useECharts'
import type {
  AiUsageItem,
  AiUsageQuery,
  AiUsageSummaryVO,
  UserItem,
} from '@/types'

const loading = ref(false)
const summary = ref<AiUsageSummaryVO | null>(null)
const logs = ref<AiUsageItem[]>([])
const users = ref<UserItem[]>([])

const total = ref(0)
const pages = ref(1)

const query = reactive<AiUsageQuery>({
  page: 1,
  limit: 15,
  userId: undefined,
  scene: '',
  status: '',
  dateFrom: '',
  dateTo: '',
})

const dateRange = ref<[string, string] | null>(null)

const trendRef = ref<HTMLElement>()
const sceneRef = ref<HTMLElement>()
const { render: renderTrend } = useECharts(trendRef)
const { render: renderScene } = useECharts(sceneRef)

const SCENE_LABELS: Record<string, string> = {
  harness: '云端智能体',
  chat: '伴读对话',
  thinking: '深度研读',
  agent: 'Agent 对话',
  diverge: '灵感发散',
  knowledge: '知识记忆',
}

const STATUS_META: Record<string, { label: string; type: 'success' | 'warning' | 'danger' }> = {
  success: { label: '成功', type: 'success' },
  quota_exhausted: { label: '额度拒答', type: 'warning' },
  error: { label: '失败', type: 'danger' },
}

function sceneLabel(scene: string): string {
  return SCENE_LABELS[scene] ?? scene
}

async function loadSummary(): Promise<void> {
  try {
    summary.value = await getAiUsageSummaryService()
    renderCharts()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '汇总加载失败')
  }
}

async function loadLogs(): Promise<void> {
  loading.value = true
  try {
    const res = await getAiUsageService({
      ...query,
      scene: query.scene || undefined,
      status: query.status || undefined,
      dateFrom: dateRange.value?.[0] || undefined,
      dateTo: dateRange.value?.[1] || undefined,
    })
    logs.value = res.data ?? []
    total.value = res.total ?? 0
    pages.value = res.pages ?? 1
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '明细加载失败')
  } finally {
    loading.value = false
  }
}

function renderCharts(): void {
  const data = summary.value
  if (!data) return
  if (trendRef.value) {
    void renderTrend({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 16, top: 20, bottom: 28 },
      xAxis: {
        type: 'category',
        data: data.trend.map((t) => t.date.slice(5)),
        axisLine: { lineStyle: { color: '#d5deeb' } },
        axisLabel: { fontSize: 11 },
      },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 } },
      series: [
        {
          name: '调用次数',
          type: 'bar',
          barMaxWidth: 22,
          itemStyle: { color: '#4f6ef7', borderRadius: [6, 6, 0, 0] },
          data: data.trend.map((t) => t.count),
        },
      ],
    })
  }
  if (sceneRef.value) {
    void renderScene({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', right: 4, top: 'center', textStyle: { fontSize: 11 } },
      series: [
        {
          type: 'pie',
          radius: ['40%', '68%'],
          center: ['38%', '50%'],
          itemStyle: { borderRadius: 5, borderColor: 'transparent', borderWidth: 2 },
          label: { show: false },
          data: data.sceneDistribution.map((s) => ({
            name: sceneLabel(s.scene),
            value: s.count,
          })),
        },
      ],
    })
  }
}

function handleSearch(): void {
  query.page = 1
  void loadLogs()
}

function handleReset(): void {
  query.userId = undefined
  query.scene = ''
  query.status = ''
  dateRange.value = null
  handleSearch()
}

function formatTime(value: string): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

function formatTokens(row: AiUsageItem): string {
  const t = row.tokensPrompt + row.tokensCompletion
  return t > 0 ? t.toLocaleString() : '-'
}

onMounted(() => {
  void loadSummary()
  void loadLogs()
  getUserListService()
    .then((list) => (users.value = list))
    .catch(() => undefined)
})
</script>

<template>
  <div class="ai-usage">
    <!-- 今日概况 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">今日调用总数</div>
        <div class="stat-value">{{ summary?.today.total ?? '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日活跃用户</div>
        <div class="stat-value">{{ summary?.today.activeUsers ?? '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日额度拒答</div>
        <div class="stat-value">{{ summary?.today.exhausted ?? '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日 Tokens</div>
        <div class="stat-value">{{ summary?.today.tokens?.toLocaleString() ?? '-' }}</div>
      </div>
    </div>

    <!-- 趋势 + 场景分布 -->
    <div class="chart-row">
      <div class="bento-card">
        <h2 class="card-title">近 14 天调用趋势</h2>
        <div ref="trendRef" class="chart-box" />
      </div>
      <div class="bento-card">
        <h2 class="card-title">场景分布（近 30 天）</h2>
        <div ref="sceneRef" class="chart-box" />
      </div>
    </div>

    <div class="bottom-row">
      <!-- 用户排行 -->
      <div class="bento-card">
        <h2 class="card-title">用户排行（近 30 天）</h2>
        <el-table :data="summary?.userRanking ?? []" size="small" empty-text="暂无数据">
          <el-table-column type="index" label="#" width="46" />
          <el-table-column prop="username" label="用户" min-width="100" />
          <el-table-column prop="role" label="角色" width="80">
            <template #default="{ row }">
              <span class="pill muted">{{ row.role }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="count" label="调用次数" width="90" align="right" />
        </el-table>
      </div>

      <!-- 模型分布 -->
      <div class="bento-card">
        <h2 class="card-title">模型分布（近 30 天）</h2>
        <el-table :data="summary?.modelDistribution ?? []" size="small" empty-text="暂无数据">
          <el-table-column prop="model" label="模型" min-width="140" />
          <el-table-column prop="count" label="调用次数" width="90" align="right" />
        </el-table>
      </div>
    </div>

    <!-- 明细表 -->
    <div class="table-wrap">
      <div class="filter-bar head-filter">
        <el-select v-model="query.userId" placeholder="全部用户" clearable filterable style="width: 150px">
          <el-option
            v-for="u in users"
            :key="u.id"
            :label="u.nickname || u.username"
            :value="u.id"
          />
        </el-select>
        <el-select v-model="query.scene" placeholder="全部场景" clearable style="width: 140px">
          <el-option v-for="(label, key) in SCENE_LABELS" :key="key" :label="label" :value="key" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="成功" value="success" />
          <el-option label="额度拒答" value="quota_exhausted" />
          <el-option label="失败" value="error" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 240px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="logs" empty-text="暂无调用记录">
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="用户" min-width="100" />
        <el-table-column prop="role" label="角色" width="80">
          <template #default="{ row }">
            <span class="pill muted">{{ row.role || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="场景" min-width="110">
          <template #default="{ row }">{{ sceneLabel(row.scene) }}</template>
        </el-table-column>
        <el-table-column prop="model" label="模型" min-width="130">
          <template #default="{ row }">{{ row.model || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_META[row.status]?.type ?? 'info'" size="small" effect="light">
              {{ STATUS_META[row.status]?.label ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">{{ row.durationMs > 0 ? `${(row.durationMs / 1000).toFixed(1)}s` : '-' }}</template>
        </el-table-column>
        <el-table-column label="Tokens" width="100" align="right">
          <template #default="{ row }">{{ formatTokens(row) }}</template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.limit"
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
.chart-row {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 16px;
  margin-bottom: 16px;
}

.bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.chart-box {
  width: 100%;
  height: 240px;
}

.head-filter {
  margin-bottom: 12px;
}

@media (max-width: 1024px) {
  .chart-row,
  .bottom-row {
    grid-template-columns: 1fr;
  }
}
</style>
