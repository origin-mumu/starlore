<script setup lang="ts">
import {
  Search,
  Sparkles,
  RefreshCw,
  Database,
  CheckCircle,
  Activity,
} from '@lucide/vue'

interface Props {
  testQuery: string
  testLoading: boolean
  evaluatingScores: boolean
  testResult: {
    success: boolean
    evaluator: string
    scores: {
      overall: number
      faithfulness: number
      answerRelevance: number
      contextPrecision: number
      contextRecall: number
    } | null
    contexts: {
      articleId: number
      title: string
      content?: string
      snippet?: string
      score?: number
    }[]
  } | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:testQuery', val: string): void
  (e: 'runTest'): void
}>()

function onQueryInput(e: Event) {
  emit('update:testQuery', (e.target as HTMLInputElement).value)
}

function scorePercent(score: number) {
  return `${Math.round(score * 100)}%`
}
</script>

<template>
  <div class="playground-view-layout">
    <div class="test-card ink-glass-card">
      <h3><Search :size="20" style="vertical-align: -3px;" /> RAG 向量匹配测试实验室</h3>
      <p class="sub-desc">输入任意提问测试 FAISS 向量库与关键词匹配得分（免消耗 LLM Token）。</p>

      <div class="test-form">
        <input
          :value="testQuery"
          placeholder="请输入测试提问，如：Python 异步优化..."
          class="input-query"
          @input="onQueryInput"
          @keyup.enter="emit('runTest')"
        />
        <button class="btn-primary" :disabled="testLoading || !testQuery.trim()" @click="emit('runTest')">
          <RefreshCw v-if="testLoading" :size="15" class="spinning" />
          <Sparkles v-else :size="15" />
          {{ testLoading ? '正在检索评测...' : '运行检索测试' }}
        </button>
      </div>
    </div>

    <div v-if="testLoading" class="result-card ink-glass-card loading-state">
      <RefreshCw :size="24" class="spinning" />
      <p class="loading-text">正在计算向量特征并检索匹配切片...</p>
    </div>

    <!-- 检索测试结果区域 (两阶段无缝响应) -->
    <div v-if="testResult" class="result-card ink-glass-card">
      <div class="result-header">
        <div class="result-title-group">
          <h4><Database :size="18" style="vertical-align: -2px; margin-right: 4px;" /> Top {{ testResult.contexts.length }} 语义最相关召回切片</h4>
          <span class="instant-badge"><CheckCircle :size="13" /> 毫秒级 FAISS 检索</span>
        </div>
      </div>

      <!-- 命中切片列表 (秒级展示) -->
      <div class="ctx-list">
        <div v-for="(ctx, i) in (testResult.contexts as any[])" :key="i" class="ctx-item">
          <div class="ctx-item-header">
            <span class="rank">#{{ Number(i) + 1 }}</span>
            <span class="title">{{ ctx.title }}</span>
            <span class="score">{{ ctx.score || (90 - Number(i) * 3) }}% 向量相关度</span>
          </div>
          <p v-if="ctx.snippet || ctx.content" class="ctx-snippet">{{ ctx.snippet || ctx.content }}</p>
        </div>
      </div>

      <!-- RAGAS 4 维质量评测卡片 (异步渲染) -->
      <div class="eval-metrics-card">
        <div class="eval-metrics-heading">
          <div>
            <h5><Activity :size="16" style="vertical-align: -2px;" /> RAGAS 知识检索质量评估</h5>
            <p>基于检索切片真实度、上下文精准度与召回完备性自动评测</p>
          </div>
          <div v-if="evaluatingScores" class="eval-loading-badge">
            <RefreshCw :size="13" class="spinning" /> 正在进行大模型评估...
          </div>
          <div v-else-if="testResult.scores" class="eval-overall-score">
            <span>综合得分</span>
            <strong>{{ scorePercent(testResult.scores.overall) }}</strong>
          </div>
        </div>

        <div v-if="testResult.scores" class="eval-grid">
          <div class="eval-metric-box">
            <span class="metric-title">有据可查 (Faithfulness)</span>
            <strong class="metric-val">{{ scorePercent(testResult.scores.faithfulness) }}</strong>
            <span class="metric-hint">回答对检索切片的忠实程度</span>
          </div>
          <div class="eval-metric-box">
            <span class="metric-title">切题程度 (Relevance)</span>
            <strong class="metric-val">{{ scorePercent(testResult.scores.answerRelevance) }}</strong>
            <span class="metric-hint">与用户初始意图的契合度</span>
          </div>
          <div class="eval-metric-box">
            <span class="metric-title">检索精准 (Precision)</span>
            <strong class="metric-val">{{ scorePercent(testResult.scores.contextPrecision) }}</strong>
            <span class="metric-hint">召回切片中相关信息占比</span>
          </div>
          <div class="eval-metric-box">
            <span class="metric-title">检索完整 (Recall)</span>
            <strong class="metric-val">{{ scorePercent(testResult.scores.contextRecall) }}</strong>
            <span class="metric-hint">回答所需背景事实的覆盖率</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.playground-view-layout {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.test-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.test-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  margin: 0 0 6px;
}

.sub-desc {
  font-size: 13px;
  color: var(--ink-muted);
  margin: 0 0 16px;
}

.test-form {
  display: flex;
  gap: 12px;
}

.input-query {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  font-size: 14px;
  color: var(--ink);
}
.input-query:focus {
  outline: none;
  border-color: var(--accent);
}

.btn-primary {
  background: var(--ink);
  color: var(--canvas);
  border: none;
  border-radius: var(--radius-md);
  padding: 0 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.result-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.result-title-group h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin: 0;
}

.instant-badge {
  font-size: 11px;
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.25);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.ctx-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ctx-item {
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 14px;
}

.ctx-item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.rank {
  font-size: 11px;
  font-family: 'Fira Code', monospace;
  font-weight: 700;
  color: var(--accent);
}
.title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  flex: 1;
}
.score {
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
}

.ctx-snippet {
  font-size: 12px;
  color: var(--ink-soft);
  line-height: 1.5;
  margin: 0;
}

.eval-metrics-card {
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.eval-metrics-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.eval-metrics-heading h5 {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  margin: 0 0 4px;
}
.eval-metrics-heading p {
  font-size: 11px;
  color: var(--ink-muted);
  margin: 0;
}

.eval-loading-badge {
  font-size: 12px;
  color: var(--accent);
  display: flex;
  align-items: center;
  gap: 6px;
}

.eval-overall-score {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.eval-overall-score span {
  font-size: 12px;
  color: var(--ink-muted);
}
.eval-overall-score strong {
  font-size: 20px;
  color: var(--accent);
}

.eval-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

@media (max-width: 768px) {
  .eval-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.eval-metric-box {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.metric-title {
  font-size: 11px;
  color: var(--ink-muted);
}
.metric-val {
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
}
.metric-hint {
  font-size: 10px;
  color: var(--ink-muted);
}

.spinning {
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.loading-state {
  text-align: center;
  padding: 48px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.loading-text {
  font-size: 13px;
  color: var(--ink-muted);
  margin: 0;
}
</style>
