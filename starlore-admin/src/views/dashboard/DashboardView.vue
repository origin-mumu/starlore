<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DataLine, Document, FolderOpened, View, Plus } from '@element-plus/icons-vue'
import { getBlogStatsService } from '@/api/article'
import { getLoginLogsService } from '@/api/auth'
import { useECharts } from '@/composables/useECharts'
import type { BlogStatsVO, LoginLogItem } from '@/types'
import StatCard from '@/components/common/StatCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'

const router = useRouter()
const stats = ref<BlogStatsVO>()
const recentLogs = ref<LoginLogItem[]>([])
const chartRef = ref<HTMLElement>()
const { render } = useECharts(chartRef)

const hasCategories = computed(() => (stats.value?.popularCategories.length ?? 0) > 0)

const formatLogTime = (time: string): string => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const renderCategoryChart = (data: BlogStatsVO): void => {
  const palette = [
    '#B85C38',
    '#C4893A',
    '#8B6E4E',
    '#D4A070',
    '#A04E2E',
    '#C49A3A',
    '#7A5C3E',
    '#E8C4A0',
    '#9E8E7A',
  ]
  void render({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}篇 ({d}%)',
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      textStyle: { color: '#6B5D4D', fontFamily: 'inherit' },
    },
    series: [
      {
        name: '星记数量',
        type: 'pie',
        radius: ['44%', '72%'],
        center: ['62%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fffdf8', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 17, fontWeight: 'bold', fontFamily: 'inherit' },
        },
        labelLine: { show: false },
        data: data.popularCategories.map((category, index) => ({
          name: category.name,
          value: category.article_count || 0,
          itemStyle: { color: palette[index % palette.length] },
        })),
      },
    ],
  })
}

onMounted(async () => {
  try {
    stats.value = await getBlogStatsService()
    if (hasCategories.value && stats.value) renderCategoryChart(stats.value)
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }

  try {
    const logs = await getLoginLogsService({ page: 1, limit: 10 })
    recentLogs.value = logs.data ?? []
  } catch {
    /* 非管理员忽略 */
  }
})

const goArticles = (): void => {
  void router.push({ name: 'articles' })
}
const goCategories = (): void => {
  void router.push({ name: 'categories' })
}
</script>

<template>
  <div class="dashboard">
    <header class="page-header">
      <div>
        <h1 class="page-title">知识库首页</h1>
        <p class="page-subtitle">星域运营数据总览</p>
      </div>
    </header>

    <div class="dashboard__grid">
      <StatCard label="星记总数" :value="stats?.totalArticles ?? 0">
        <el-icon><Document /></el-icon>
      </StatCard>

      <StatCard label="星域数量" :value="stats?.totalCategories ?? 0">
        <el-icon><FolderOpened /></el-icon>
      </StatCard>

      <StatCard label="总阅读量" :value="stats?.totalViews ?? 0">
        <el-icon><View /></el-icon>
      </StatCard>
    </div>

    <div v-if="hasCategories" class="bento-card dashboard__chart-card">
      <h3 class="bento-card__title">
        <el-icon><DataLine /></el-icon>
        星记星域分布
      </h3>
      <div ref="chartRef" class="dashboard__chart"></div>
    </div>

    <div class="dashboard__row">
      <div class="bento-card dashboard__actions">
        <h3 class="bento-card__title">快速操作</h3>
        <div class="dashboard__action-buttons">
          <button class="btn btn--primary" type="button" @click="goArticles">
            <el-icon><Plus /></el-icon>
            新建星记
          </button>
          <button class="btn btn--secondary" type="button" @click="goCategories">
            <el-icon><FolderOpened /></el-icon>
            管理星域
          </button>
        </div>
      </div>

      <div class="bento-card dashboard__logs">
        <h3 class="bento-card__title">最近登录</h3>
        <EmptyState v-if="recentLogs.length === 0" text="暂无登录记录" />
        <ul v-else class="dashboard__log-list">
          <li v-for="log in recentLogs" :key="log.id" class="dashboard__log-item">
            <span class="dashboard__log-user">{{ log.username }}</span>
            <span class="dashboard__log-time">{{ formatLogTime(log.loginTime) }}</span>
          </li>
        </ul>
      </div>
    </div>

    <LoadingState v-if="!stats" text="正在加载运营数据..." />
  </div>
</template>

<style scoped>
.dashboard__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.dashboard__chart-card {
  margin-bottom: 18px;
}

.dashboard__chart {
  width: 100%;
  height: 360px;
  min-height: 360px;
}

.dashboard__row {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 18px;
}

.dashboard__actions {
  display: flex;
  flex-direction: column;
}

.dashboard__action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.dashboard__logs {
  display: flex;
  flex-direction: column;
}

.dashboard__log-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 12px;
}

.dashboard__log-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 14px;
  border-radius: var(--radius-sm);
  background: var(--surface-hover);
  transition: background var(--transition-fast);
}

.dashboard__log-item:hover {
  background: var(--brand-subtle);
}

.dashboard__log-user {
  font-weight: 500;
  font-size: 0.88rem;
  color: var(--text-primary);
}

.dashboard__log-time {
  font-size: 0.8rem;
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .dashboard__row {
    grid-template-columns: 1fr;
  }

  .dashboard__chart {
    height: 280px;
    min-height: 280px;
  }
}
</style>
