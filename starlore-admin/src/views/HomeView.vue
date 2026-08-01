<script setup lang="ts">
import Nav from '@/components/nav.vue'
import { getBlogStatsService } from '@/api/article'
import { onMounted, ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

interface BlogStats {
  totalArticles: number
  totalCategories: number
  totalViews: number
  popularArticles: any[]
  popularCategories: any[]
}

interface LoginLog {
  id: number
  userId: number
  username: string
  loginTime: string
}

const router = useRouter()
const blogStats = ref<BlogStats>()
const loginLogs = ref<LoginLog[]>([])
const chartRef = ref<HTMLElement>()
let chartInstance: any = null

const initChart = () => {
  if (!chartRef.value || !blogStats.value?.popularCategories) return

  import('echarts').then((echarts) => {
    chartInstance = echarts.init(chartRef.value!)

    if (!blogStats.value?.popularCategories.length) return
    const chartData = blogStats.value.popularCategories.map((category: any, index: number) => ({
      name: category.name,
      value: category.article_count || 0,
      itemStyle: {
        color: getCategoryColor(index),
      },
    }))

    const option = {
      title: {
        text: '星记星域分布',
        left: 'center',
        textStyle: {
          color: '#2A2118',
          fontSize: 16,
          fontWeight: 'bold',
          fontFamily: "'LXGW WenKai', 'Source Serif 4', serif",
        },
      },
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c}篇 ({d}%)',
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'middle',
        textStyle: {
          color: '#6B5D4D',
          fontFamily: "'LXGW WenKai', serif",
        },
      },
      series: [
        {
          name: '星记数量',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['60%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#FDFBF5',
            borderWidth: 2,
          },
          label: {
            show: false,
            position: 'center',
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 18,
              fontWeight: 'bold',
            },
          },
          labelLine: {
            show: false,
          },
          data: chartData,
        },
      ],
    }

    chartInstance.setOption(option)

    const resizeChart = () => {
      chartInstance?.resize()
    }
    window.addEventListener('resize', resizeChart)
  })
}

const getCategoryColor = (index: number) => {
  const colors = [
    '#B85C38', // burnt sienna (accent)
    '#C4893A', // amber (warm)
    '#8B6E4E', // warm brown
    '#D4A070', // tan
    '#A04E2E', // deep sienna
    '#C49A3A', // golden
    '#7A5C3E', // cocoa
    '#E8C4A0', // peach
    '#9E8E7A', // muted taupe
  ]
  return colors[index % colors.length]
}

onMounted(() => {
  getBlogStatsService().then((res) => {
    blogStats.value = res.data
    if (blogStats.value?.popularCategories && blogStats.value.popularCategories.length > 0) {
      setTimeout(initChart, 100)
    }
  })

  request.get('/admin/login-logs').then((res: any) => {
    loginLogs.value = (res.data || []).slice(0, 10)
  }).catch(() => { /* 非管理员忽略 */ })
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

const formatLogTime = (t: string) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  })
}

const handleCreateArticle = () => {
  router.push({ name: 'articles' })
}
const handleManageCategories = () => {
  router.push({ name: 'categories' })
}
</script>

<template>
  <div class="home-layout">
    <div class="sidebar-container">
      <Nav></Nav>
    </div>
    <div class="content-container">
      <div class="content-box">
        <div class="title">知识库首页</div>
        <div class="dashboard-content">
          <!-- 统计卡片 -->
          <div class="stats-cards">
            <div class="stat-card">
              <div class="stat-icon">📊</div>
              <div class="stat-info">
                <div class="stat-number">{{ blogStats?.totalArticles || 0 }}</div>
                <div class="stat-label">星记总数</div>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon">📂</div>
              <div class="stat-info">
                <div class="stat-number">{{ blogStats?.totalCategories || 0 }}</div>
                <div class="stat-label">星域数量</div>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon">👀</div>
              <div class="stat-info">
                <div class="stat-number">{{ blogStats?.totalViews || 0 }}</div>
                <div class="stat-label">总阅读量</div>
              </div>
            </div>
          </div>

          <!-- 图表区域 -->
          <div
            class="chart-section"
            v-if="blogStats?.popularCategories && blogStats.popularCategories.length > 0"
          >
            <div class="chart-card">
              <h3 class="chart-title">📈 星记星域分布</h3>
              <div ref="chartRef" class="chart-container"></div>
            </div>
          </div>

          <div class="quick-actions">
            <h3>快速操作</h3>
            <div class="action-buttons">
              <button class="action-btn primary" @click="handleCreateArticle">新建星记</button>
              <button class="action-btn secondary" @click="handleManageCategories">管理星域</button>
            </div>
          </div>

          <!-- 登录日志 -->
          <div class="log-section">
            <h3>最近登录</h3>
            <div v-if="loginLogs.length === 0" class="log-empty">暂无登录记录</div>
            <div v-else class="log-list">
              <div v-for="log in loginLogs" :key="log.id" class="log-item">
                <span class="log-user">{{ log.username }}</span>
                <span class="log-time">{{ formatLogTime(log.loginTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.title {
  font-size: 1.8rem;
  font-weight: 700;
  margin-bottom: var(--space-6);
  color: var(--ink);
  letter-spacing: -0.02em;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-5);
}

.stat-card {
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  transition: all var(--transition);
  box-shadow: var(--shadow-sm);
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--border-interactive);
}

.stat-icon {
  font-size: 2rem;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 1.8rem;
  font-weight: 700;
  margin-bottom: 2px;
  color: var(--ink);
  font-family: 'Source Serif 4', serif;
}

.stat-label {
  font-size: 0.85rem;
  color: var(--ink-soft);
}

.quick-actions {
  background: var(--canvas-deep);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  border: 1px solid var(--border);
}

.quick-actions h3 {
  font-size: 1.2rem;
  margin-bottom: var(--space-5);
  color: var(--ink);
  font-weight: 600;
}

.log-section {
  background: var(--canvas-deep);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  border: 1px solid var(--border);
}

.log-section h3 {
  font-size: 1.2rem;
  margin-bottom: var(--space-4);
  color: var(--ink);
  font-weight: 600;
}

.log-empty {
  color: var(--ink-muted);
  font-size: 0.9rem;
  text-align: center;
  padding: var(--space-5);
}

.log-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.log-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  background: var(--surface);
  transition: background var(--transition);
}

.log-item:hover {
  background: var(--accent-soft);
}

.log-user {
  font-weight: 500;
  color: var(--ink);
  font-size: 0.9rem;
}

.log-time {
  color: var(--ink-muted);
  font-size: 0.82rem;
}

.action-buttons {
  display: flex;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.action-btn {
  padding: 10px 22px;
  border-radius: var(--radius-md);
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
  border: 1px solid transparent;
}

.action-btn.primary {
  background: var(--accent);
  color: #FDFBF5;
  box-shadow: var(--shadow-button);
}

.action-btn.primary:hover {
  background: var(--accent-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-button-hover);
}

.action-btn.secondary {
  background: transparent;
  color: var(--ink-soft);
  border-color: var(--border-interactive);
}

.action-btn.secondary:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

/* 图表区域 */
.chart-section {
  margin: 0;
}

.chart-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
}

.chart-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: var(--space-5);
  text-align: center;
}

.chart-container {
  width: 100%;
  height: 400px;
  min-height: 400px;
}

@media (max-width: 768px) {
  .chart-card {
    padding: var(--space-5);
  }

  .chart-container {
    height: 300px;
    min-height: 300px;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>
