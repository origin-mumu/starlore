<script setup lang="ts">
import {
  ClipboardList,
  CheckCircle,
  RotateCcw,
  XCircle,
  Bot,
} from '@lucide/vue'
import type { RagEvaluationResult } from '@/api/ai'

export type AgentTrace = {
  planSummary?: string
  subtasks: { id: number; desc: string; status: 'pending' | 'running' | 'done' }[]
  reviewDecision?: string
  reviewFeedback?: string
  retryCount?: number
  metrics?: {
    tokensIn: number
    tokensOut: number
    latencyMs: number
  } | null
  ragContexts?: { articleId: number; title: string }[]
  ragRetrievalMode?: 'vector' | 'keyword'
  ragEvaluation?: RagEvaluationResult | null
  ragEvaluationStatus?: 'pending' | 'complete' | 'failed'
  ragEvaluationError?: string
}

interface Props {
  agentTrace: AgentTrace
  hasContent?: boolean
  toolLabelMap?: Record<string, string>
  agentNodeLabelMap?: Record<string, string>
}

const props = withDefaults(defineProps<Props>() , {
  hasContent: false,
  toolLabelMap: () => ({}),
  agentNodeLabelMap: () => ({}),
})
</script>

<template>
  <div
    v-if="
      agentTrace &&
      (agentTrace.planSummary ||
        agentTrace.subtasks.length ||
        agentTrace.reviewDecision)
    "
    class="imm-trace-stepper"
  >
    <!-- 1. Planner Node -->
    <div
      v-if="agentTrace.planSummary"
      class="imm-step-node"
      :class="{
        'is-running': !agentTrace.subtasks.length && !agentTrace.reviewDecision && !hasContent,
        'is-done': agentTrace.subtasks.length > 0 || agentTrace.reviewDecision || hasContent,
      }"
    >
      <div
        class="imm-step-line"
        v-if="agentTrace.subtasks.length > 0 || agentTrace.reviewDecision || hasContent"
      ></div>
      <div class="imm-step-icon-container">
        <span
          v-if="agentTrace.subtasks.length > 0 || agentTrace.reviewDecision || hasContent"
          class="imm-step-dot done"
        >✓</span>
        <span v-else class="imm-step-dot running"></span>
      </div>
      <div class="imm-step-content">
        <div class="imm-step-title">任务规划 (Planner)</div>
        <div class="imm-step-desc">{{ agentTrace.planSummary }}</div>
      </div>
    </div>

    <!-- 2. Subtask Nodes (when subtasks exist) -->
    <div
      v-for="st in agentTrace.subtasks"
      :key="st.id"
      class="imm-step-node"
      :class="{
        'is-pending': st.status === 'pending',
        'is-running': st.status === 'running',
        'is-done': st.status === 'done',
      }"
    >
      <div class="imm-step-line"></div>
      <div class="imm-step-icon-container">
        <span v-if="st.status === 'done'" class="imm-step-dot done">✓</span>
        <span v-else-if="st.status === 'running'" class="imm-step-dot running"></span>
        <span v-else class="imm-step-dot pending"></span>
      </div>
      <div class="imm-step-content">
        <div class="imm-step-title">子任务 {{ st.id }}</div>
        <div class="imm-step-desc">
          {{
            toolLabelMap[st.desc] ||
            agentNodeLabelMap[st.desc] ||
            st.desc ||
            '等待获取执行内容...'
          }}
        </div>
      </div>
    </div>

    <!-- 2b. Direct Executor Node (when no subtasks exist and planned as direct answer) -->
    <div
      v-if="agentTrace && (!agentTrace.subtasks || agentTrace.subtasks.length === 0) && (agentTrace.reviewDecision || hasContent)"
      class="imm-step-node is-done"
    >
      <div class="imm-step-line"></div>
      <div class="imm-step-icon-container">
        <span class="imm-step-dot done">✓</span>
      </div>
      <div class="imm-step-content">
        <div class="imm-step-title">执行阶段 (Executor)</div>
        <div class="imm-step-desc">无需外部工具，直接分析并生成回答...</div>
      </div>
    </div>

    <!-- 3. Reviewer Node -->
    <div
      v-if="
        agentTrace.reviewDecision || (agentTrace.subtasks && agentTrace.subtasks.length > 0) || hasContent
      "
      class="imm-step-node"
      :class="{
        'is-pending': !agentTrace.reviewDecision,
        'is-done': agentTrace.reviewDecision === 'PASS',
        'is-warning': agentTrace.reviewDecision === 'REVISE',
        'is-error': agentTrace.reviewDecision === 'FAIL',
      }"
    >
      <div class="imm-step-line" v-if="agentTrace.metrics"></div>
      <div class="imm-step-icon-container">
        <CheckCircle
          v-if="agentTrace.reviewDecision === 'PASS'"
          :size="11"
          class="imm-step-icon"
        />
        <RotateCcw
          v-else-if="agentTrace.reviewDecision === 'REVISE'"
          :size="11"
          class="imm-step-icon"
        />
        <XCircle
          v-else-if="agentTrace.reviewDecision === 'FAIL'"
          :size="11"
          class="imm-step-icon"
        />
        <Bot v-else :size="11" class="imm-step-icon" />
      </div>
      <div class="imm-step-content">
        <div class="imm-step-title">结果审核 (Reviewer)</div>
        <div class="imm-step-desc">
          <span v-if="agentTrace.reviewDecision">
            决策:
            <strong :class="agentTrace.reviewDecision.toLowerCase()">{{
              agentTrace.reviewDecision
            }}</strong>
            <span v-if="agentTrace.reviewFeedback">
              ({{ agentTrace.reviewFeedback }})</span
            >
          </span>
          <span v-else>正在评估执行结果的质量和完整性...</span>
        </div>
      </div>
    </div>

    <!-- 4. Metrics Node -->
    <div v-if="agentTrace.metrics" class="imm-step-node is-metrics">
      <div class="imm-step-icon-container">
        <span class="imm-step-dot metrics"></span>
      </div>
      <div class="imm-step-content">
        <div class="imm-step-desc metrics-data">
          Token 消耗: {{ agentTrace.metrics.tokensIn }}↓ /
          {{ agentTrace.metrics.tokensOut }}↑ · 耗时:
          {{ agentTrace.metrics.latencyMs }}ms
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.imm-trace-stepper {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  margin-bottom: 8px;
  max-width: 100%;
}

.imm-step-node {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  position: relative;
  padding-bottom: 12px;
}
.imm-step-node:last-child {
  padding-bottom: 0;
}

.imm-step-line {
  position: absolute;
  left: 9px;
  top: 18px;
  bottom: 0;
  width: 1px;
  background: var(--border);
}

.imm-step-icon-container {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.05));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  z-index: 1;
}

.imm-step-node.is-done .imm-step-icon-container {
  background: rgba(40, 200, 64, 0.15);
  color: #28c840;
}
.imm-step-node.is-running .imm-step-icon-container {
  background: var(--accent-soft);
  color: var(--accent);
}
.imm-step-node.is-warning .imm-step-icon-container {
  background: rgba(254, 188, 46, 0.15);
  color: #febc2e;
}
.imm-step-node.is-error .imm-step-icon-container {
  background: rgba(255, 95, 87, 0.15);
  color: #ff5f57;
}

.imm-step-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.imm-step-dot.done {
  font-size: 10px;
  font-weight: bold;
  color: #28c840;
  display: flex;
  align-items: center;
  justify-content: center;
}
.imm-step-dot.running {
  background: var(--accent);
  animation: pulse-dot 1.2s infinite;
}
.imm-step-dot.pending {
  background: var(--ink-muted);
  opacity: 0.4;
}
.imm-step-dot.metrics {
  background: var(--accent-sky, #7B9AFF);
}

@keyframes pulse-dot {
  0%, 100% { transform: scale(0.8); opacity: 0.5; }
  50% { transform: scale(1.3); opacity: 1; }
}

.imm-step-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.imm-step-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--ink);
}

.imm-step-desc {
  font-size: 11px;
  color: var(--ink-muted);
  line-height: 1.4;
  word-break: break-word;
}

.imm-step-desc strong.pass {
  color: #28c840;
}
.imm-step-desc strong.revise {
  color: #febc2e;
}
.imm-step-desc strong.fail {
  color: #ff5f57;
}

.metrics-data {
  font-family: 'Fira Code', monospace;
  font-size: 10px;
  opacity: 0.8;
}
</style>
