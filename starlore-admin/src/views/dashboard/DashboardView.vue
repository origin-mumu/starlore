<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getBlogStatsService } from '@/api/article'
import { getLoginLogsService } from '@/api/auth'
import { getAiUsageSummaryService } from '@/api/ai-usage'
import { useECharts } from '@/composables/useECharts'
import type { BlogStatsVO, LoginLogItem, AiUsageSummaryVO } from '@/types'

const router = useRouter()

const loading = ref(true)
const stats = ref<BlogStatsVO | null>(null)
const aiSummary = ref<AiUsageSummaryVO | null>(null)
const recentLogs = ref<LoginLogItem[]>([])

const pieRef = ref<HTMLElement>()
const trendRef = ref<HTMLElement>()
const { render: renderPie } = useECharts(pieRef)
const { render: renderTrend } = useECharts(trendRef)

const BRAND = '#4f6ef7'

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const [statsRes, aiRes, logsRes] = await Promise.all([
      getBlogStatsService().catch(() => null),
      getAiUsageSummaryService().catch(() => null),
      getLoginLogsService({ page: 1, limit: 8 }).catch(() => null),
    ])
    stats.value = statsRes
    aiSummary.value = aiRes
    recentLogs.value = logsRes?.data ?? []
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts(): void {
  const categories = stats.value?.popularCategories ?? []
  if (pieRef.value) {
    void renderPie({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { fontSize: 11 } },
      series: [
        {
          type: 'pie',
          radius: ['42%', '68%'],
          itemStyle: { borderRadius: 6, borderColor: 'transparent', borderWidth: 2 },
          label: { show: false },
          data: categories.map((c) => ({ name: c.name, value: c.article_count })),
        },
      ],
    })
  }

  const trend = aiSummary.value?.trend ?? []
  if (trendRef.value) {
    void renderTrend({
      tooltip: { trigger: 'axis' },
      grid: { left: 36, right: 16, top: 24, bottom: 28 },
      xAxis: {
        type: 'category',
        data: trend.map((t) => t.date.slice(5)),
        axisLine: { lineStyle: { color: '#d5deeb' } },
        axisLabel: { fontSize: 11 },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { fontSize: 11 },
        splitLine: { lineStyle: { color: 'rgba(213,222,235,0.45)' } },
      },
      series: [
        {
          name: 'AI 调用',
          type: 'line',
          smooth: true,
          symbolSize: 6,
          lineStyle: { width: 2.5, color: BRAND },
          itemStyle: { color: BRAND },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(79,110,247,0.22)' },
                { offset: 1, color: 'rgba(79,110,247,0.01)' },
              ],
            },
          },
          data: trend.map((t) => t.count),
        },
      ],
    })
  }
}

function formatTime(value: string): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <!-- 指标卡 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">星记总数</div>
        <div class="stat-value">{{ stats?.totalArticles ?? '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">星域分类</div>
        <div class="stat-value">{{ stats?.totalCategories ?? '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计浏览</div>
        <div class="stat-value">{{ stats?.totalViews ?? '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日 AI 调用</div>
        <div class="stat-value">{{ aiSummary?.today.total ?? '-' }}</div>
        <div class="stat-hint">
          活跃用户 {{ aiSummary?.today.activeUsers ?? 0 }} · 额度拒答 {{ aiSummary?.today.exhausted ?? 0 }}
        </div>
      </div>
    </div>

    <!-- 图表行 -->
    <div class="chart-row">
      <div class="bento-card">
        <h2 class="card-title">近 14 天 AI 调用趋势</h2>
        <div ref="trendRef" class="chart-box" />
      </div>
      <div class="bento-card">
        <h2 class="card-title">星域文章分布</h2>
        <div ref="pieRef" class="chart-box" />
      </div>
    </div>

    <!-- 最近登录 -->
    <div class="bento-card">
      <h2 class="card-title">
        最近登录
        <el-button link type="primary" @click="router.push('/admin/login-logs')">查看全部</el-button>
      </h2>
      <el-table :data="recentLogs" empty-text="暂无登录记录">
        <el-table-column prop="username" label="用户" min-width="120" />
        <el-table-column label="IP 归属" min-width="180">
          <template #default="{ row }">
            {{ [row.country, row.province, row.city].filter(Boolean).join(' ') || row.ip || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.loginTime) }}</template>
        </el-table-column>
      </el-table>
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

.chart-box {
  width: 100%;
  height: 260px;
}

@media (max-width: 1024px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>
