<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  CheckCircle,
  RotateCcw,
  XCircle,
  Bot,
  ChevronRight,
  Sparkles,
  Layers,
  Cpu,
  Check,
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

const props = withDefaults(defineProps<Props>(), {
  hasContent: false,
  toolLabelMap: () => ({}),
  agentNodeLabelMap: () => ({}),
})

// 默认在生成中展开，生成完成后收起，用户也可随时手动点击展开/折叠
const isExpanded = ref(!props.hasContent)

// 当完成生成（hasContent 变为 true 且非空）时自动收起
watch(
  () => props.hasContent,
  (val) => {
    if (val) {
      isExpanded.value = false
    }
  }
)

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const isRunning = computed(() => {
  if (props.hasContent) return false
  const subtasks = props.agentTrace?.subtasks || []
  const hasPendingOrRunning = subtasks.some(
    (s) => s.status === 'running' || s.status === 'pending'
  )
  return hasPendingOrRunning || !props.agentTrace?.reviewDecision
})

const completedSubtasksCount = computed(() => {
  const subtasks = props.agentTrace?.subtasks || []
  return subtasks.filter((s) => s.status === 'done').length
})

const summaryText = computed(() => {
  const total = props.agentTrace?.subtasks?.length || 0
  if (isRunning.value) {
    if (total > 0) {
      return `思考中 · 正在执行步骤 (${completedSubtasksCount.value}/${total})`
    }
    return '思考中 · 正在规划任务...'
  }
  if (total > 0) {
    const latency = props.agentTrace?.metrics?.latencyMs
      ? ` · 耗时 ${(props.agentTrace.metrics.latencyMs / 1000).toFixed(1)}s`
      : ''
    return `已完成思考与步骤执行 (共 ${total} 步${latency})`
  }
  return '思考过程已完成'
})
</script>

<template>
  <div
    v-if="
      agentTrace &&
      (agentTrace.planSummary ||
        agentTrace.subtasks?.length ||
        agentTrace.reviewDecision)
    "
    class="ai-tree-wrapper"
  >
    <!-- 头部摘要栏 (点击展开/折叠) -->
    <div
      class="ai-tree-header"
      :class="{ 'is-running': isRunning, 'is-expanded': isExpanded }"
      @click="toggleExpand"
      role="button"
      tabindex="0"
      @keydown.enter.prevent="toggleExpand"
    >
      <div class="ai-tree-header-left">
        <div class="ai-tree-icon-badge" :class="{ running: isRunning }">
          <Sparkles v-if="isRunning" :size="13" class="spin-icon" />
          <Check v-else :size="13" />
        </div>
        <span class="ai-tree-title">{{ summaryText }}</span>
      </div>
      <div class="ai-tree-header-right">
        <ChevronRight
          :size="14"
          class="ai-tree-chevron"
          :class="{ 'rotate-90': isExpanded }"
        />
      </div>
    </div>

    <!-- 树状展开内容 -->
    <transition name="tree-expand">
      <div v-show="isExpanded" class="ai-tree-content">
        <div class="tree-root-line"></div>

        <!-- 1. 任务规划 (Planner) 节点 -->
        <div v-if="agentTrace.planSummary" class="tree-node">
          <div class="tree-branch"></div>
          <div class="tree-node-icon-wrap">
            <Layers :size="12" class="node-icon planner" />
          </div>
          <div class="tree-node-body">
            <div class="tree-node-header">
              <span class="node-tag planner">Planner</span>
              <span class="node-label">任务规划分析</span>
            </div>
            <div class="tree-node-desc">{{ agentTrace.planSummary }}</div>
          </div>
        </div>

        <!-- 2. 子任务树分支 (Subtasks Tree) -->
        <div
          v-for="st in agentTrace.subtasks"
          :key="st.id"
          class="tree-node"
          :class="st.status"
        >
          <div class="tree-branch"></div>
          <div class="tree-node-icon-wrap">
            <span v-if="st.status === 'done'" class="status-dot done">✓</span>
            <span v-else-if="st.status === 'running'" class="status-dot running"></span>
            <span v-else class="status-dot pending"></span>
          </div>
          <div class="tree-node-body">
            <div class="tree-node-header">
              <span class="node-tag subtask">步骤 {{ st.id }}</span>
              <span class="node-label">
                {{
                  toolLabelMap[st.desc] ||
                  agentNodeLabelMap[st.desc] ||
                  st.desc ||
                  '执行中...'
                }}
              </span>
            </div>
          </div>
        </div>

        <!-- 2b. 直接执行节点 (当无子任务时) -->
        <div
          v-if="
            (!agentTrace.subtasks || agentTrace.subtasks.length === 0) &&
            (agentTrace.reviewDecision || hasContent)
          "
          class="tree-node done"
        >
          <div class="tree-branch"></div>
          <div class="tree-node-icon-wrap">
            <span class="status-dot done">✓</span>
          </div>
          <div class="tree-node-body">
            <div class="tree-node-header">
              <span class="node-tag executor">Executor</span>
              <span class="node-label">直接分析并生成回答</span>
            </div>
          </div>
        </div>

        <!-- 3. 审查与核验 (Reviewer) 节点 -->
        <div
          v-if="
            agentTrace.reviewDecision ||
            (agentTrace.subtasks && agentTrace.subtasks.length > 0) ||
            hasContent
          "
          class="tree-node"
          :class="{
            done: agentTrace.reviewDecision === 'PASS',
            warning: agentTrace.reviewDecision === 'REVISE',
            error: agentTrace.reviewDecision === 'FAIL',
          }"
        >
          <div class="tree-branch"></div>
          <div class="tree-node-icon-wrap">
            <CheckCircle
              v-if="agentTrace.reviewDecision === 'PASS'"
              :size="12"
              class="node-icon pass"
            />
            <RotateCcw
              v-else-if="agentTrace.reviewDecision === 'REVISE'"
              :size="12"
              class="node-icon revise"
            />
            <XCircle
              v-else-if="agentTrace.reviewDecision === 'FAIL'"
              :size="12"
              class="node-icon fail"
            />
            <Bot v-else :size="12" class="node-icon" />
          </div>
          <div class="tree-node-body">
            <div class="tree-node-header">
              <span class="node-tag reviewer">Reviewer</span>
              <span class="node-label">
                {{
                  agentTrace.reviewDecision
                    ? `质量审查：${agentTrace.reviewDecision}`
                    : '正在评估结果质量...'
                }}
              </span>
            </div>
            <div v-if="agentTrace.reviewFeedback" class="tree-node-desc">
              反馈：{{ agentTrace.reviewFeedback }}
            </div>
          </div>
        </div>

        <!-- 4. Token 消耗与指标 (Metrics) 节点 -->
        <div v-if="agentTrace.metrics" class="tree-node metrics-node">
          <div class="tree-branch"></div>
          <div class="tree-node-icon-wrap">
            <Cpu :size="12" class="node-icon metrics" />
          </div>
          <div class="tree-node-body">
            <div class="tree-node-desc metrics-text">
              Tokens: {{ agentTrace.metrics.tokensIn }}↓ /
              {{ agentTrace.metrics.tokensOut }}↑ · 耗时:
              {{ agentTrace.metrics.latencyMs }}ms
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.ai-tree-wrapper {
  margin: 6px 0 10px 0;
  width: 100%;
  max-width: 100%;
  border-radius: 10px;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  border: 1px solid var(--border);
  overflow: hidden;
  font-family: inherit;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.ai-tree-wrapper:hover {
  border-color: var(--border-interactive, rgba(0, 0, 0, 0.15));
}

/* 顶部摘要行 */
.ai-tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
  gap: 8px;
  background: transparent;
  transition: background 0.15s ease;
}

.ai-tree-header:hover {
  background: rgba(0, 0, 0, 0.02);
}

.ai-tree-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.ai-tree-icon-badge {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(40, 200, 64, 0.12);
  color: #28c840;
  flex-shrink: 0;
}

.ai-tree-icon-badge.running {
  background: var(--accent-soft, rgba(59, 130, 246, 0.12));
  color: var(--accent, #3b82f6);
}

.spin-icon {
  animation: spin-pulse 1.6s infinite linear;
}

@keyframes spin-pulse {
  0% { transform: rotate(0deg) scale(0.9); }
  50% { transform: rotate(180deg) scale(1.1); }
  100% { transform: rotate(360deg) scale(0.9); }
}

.ai-tree-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-muted, #666);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-tree-header-right {
  display: flex;
  align-items: center;
}

.ai-tree-chevron {
  color: var(--ink-muted, #999);
  transition: transform 0.2s ease;
}

.rotate-90 {
  transform: rotate(90deg);
}

/* 树状展开内容区 */
.ai-tree-content {
  position: relative;
  padding: 6px 12px 12px 22px;
  border-top: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 树主干垂直线 */
.tree-root-line {
  position: absolute;
  left: 29px;
  top: 14px;
  bottom: 20px;
  width: 1px;
  background: var(--border, rgba(0, 0, 0, 0.1));
}

/* 树节点 */
.tree-node {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-left: 0;
  z-index: 1;
}

/* 节点水平分支连接线 */
.tree-branch {
  position: absolute;
  left: 7px;
  top: 9px;
  width: 10px;
  height: 1px;
  background: var(--border, rgba(0, 0, 0, 0.1));
}

.tree-node-icon-wrap {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--surface, #fff);
  border: 1px solid var(--border);
  z-index: 2;
  margin-top: 1px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-dot.done {
  width: auto;
  height: auto;
  font-size: 10px;
  font-weight: bold;
  color: #28c840;
}

.status-dot.running {
  background: var(--accent, #3b82f6);
  animation: pulse-dot 1.2s infinite;
}

.status-dot.pending {
  background: var(--ink-muted, #bbb);
  opacity: 0.5;
}

@keyframes pulse-dot {
  0%, 100% { transform: scale(0.8); opacity: 0.5; }
  50% { transform: scale(1.3); opacity: 1; }
}

.node-icon {
  color: var(--ink-muted);
}
.node-icon.planner {
  color: var(--accent, #3b82f6);
}
.node-icon.pass {
  color: #28c840;
}
.node-icon.revise {
  color: #febc2e;
}
.node-icon.fail {
  color: #ff5f57;
}
.node-icon.metrics {
  color: var(--ink-muted);
}

.tree-node-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.tree-node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.node-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.05));
  color: var(--ink-muted);
}

.node-tag.planner {
  background: rgba(59, 130, 246, 0.1);
  color: var(--accent, #3b82f6);
}

.node-tag.subtask {
  background: rgba(0, 0, 0, 0.04);
  color: var(--ink);
}

.node-tag.reviewer {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.node-tag.executor {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}

.node-label {
  font-size: 11px;
  color: var(--ink);
  font-weight: 500;
  line-height: 1.4;
  word-break: break-word;
}

.tree-node-desc {
  font-size: 11px;
  color: var(--ink-muted);
  line-height: 1.4;
  word-break: break-word;
  margin-top: 1px;
}

.metrics-text {
  font-family: 'Fira Code', Consolas, monospace;
  font-size: 10px;
  opacity: 0.85;
}

/* 展开收起动画 */
.tree-expand-enter-active,
.tree-expand-leave-active {
  transition: all 0.2s ease-out;
  overflow: hidden;
}

.tree-expand-enter-from,
.tree-expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.tree-expand-enter-to,
.tree-expand-leave-from {
  opacity: 1;
  max-height: 600px;
}
</style>
