<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChevronDown, ChevronRight, Loader2 } from '@lucide/vue'
import HarnessToolActionRow from './HarnessToolActionRow.vue'
import type { HarnessStepDetail, HarnessToolCall } from '../types'

const props = defineProps<{
  durationMs?: number
  reasoningContent?: string
  toolCalls?: HarnessToolCall[]
  stepDetails?: HarnessStepDetail[]
  isRunning?: boolean
}>()

// 思考过程默认展开
const isExpanded = ref(true)

const durationText = computed(() => {
  if (!props.durationMs || props.durationMs <= 0) {
    return props.isRunning ? '正在思考与生成中...' : '用时 1s'
  }
  const totalSec = Math.floor(props.durationMs / 1000)
  if (totalSec < 60) {
    return props.isRunning ? `正在生成 (${totalSec}s)...` : `用时 ${totalSec}s`
  }
  const mins = Math.floor(totalSec / 60)
  const secs = totalSec % 60
  return props.isRunning ? `正在生成 (${mins}m ${secs}s)...` : `用时 ${mins}m ${secs}s`
})

const validSteps = computed(() => {
  if (!props.stepDetails || !Array.isArray(props.stepDetails)) return []
  return props.stepDetails.filter(
    (st) =>
      Boolean(st.reasoning && st.reasoning.trim().length > 0) ||
      Boolean(st.scratchpad && st.scratchpad.trim().length > 0) ||
      Boolean(st.tool_calls && st.tool_calls.length > 0)
  )
})

const hasContent = computed(() => {
  return (
    validSteps.value.length > 0 ||
    Boolean(props.reasoningContent && props.reasoningContent.trim().length > 0) ||
    Boolean(props.toolCalls && props.toolCalls.length > 0)
  )
})
</script>

<template>
  <div v-if="hasContent || isRunning" class="thought-bar-wrapper">
    <!-- Codex 极简用时折叠顶栏 -->
    <button
      type="button"
      class="duration-btn"
      @click="isExpanded = !isExpanded"
    >
      <Loader2 v-if="isRunning" class="icon-xs spin" />
      <span>{{ durationText }}</span>
      <ChevronDown v-if="isExpanded" class="icon-xs chevron" />
      <ChevronRight v-else class="icon-xs chevron" />
    </button>

    <!-- 展开后的思考过程与各步骤内嵌工具动作 -->
    <div v-if="isExpanded" class="thought-expanded">
      <!-- 方案 A：具有明确内容步骤时，按时序顺畅呈现（去除冗余的步骤标签） -->
      <div v-if="validSteps.length > 0" class="step-details-container">
        <div
          v-for="(st, idx) in validSteps"
          :key="st.step || idx"
          class="step-block"
        >
          <!-- 该步骤的深度思维链 -->
          <div v-if="st.reasoning" class="step-reasoning">
            {{ st.reasoning }}
          </div>

          <!-- 该步骤中间规划与阐述 -->
          <div v-if="st.scratchpad" class="step-scratchpad">
            {{ st.scratchpad }}
          </div>

          <!-- 该步骤内执行的工具动作：紧嵌于思考推导后方展示 -->
          <div v-if="st.tool_calls && st.tool_calls.length > 0" class="step-tools">
            <HarnessToolActionRow
              v-for="tc in st.tool_calls"
              :key="tc.call_id"
              :tool-call="tc"
            />
          </div>
        </div>
      </div>

      <!-- 方案 B：无结构化步骤时的平稳兼容兜底展现（保证历史消息完整展示） -->
      <template v-else>
        <div v-if="reasoningContent" class="step-reasoning">
          {{ reasoningContent }}
        </div>
        <div v-if="toolCalls && toolCalls.length > 0" class="step-tools">
          <HarnessToolActionRow
            v-for="tc in toolCalls"
            :key="tc.call_id"
            :tool-call="tc"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.thought-bar-wrapper {
  margin-bottom: 14px;
  user-select: text;
}

.duration-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-muted, #71717a);
  background: var(--surface, rgba(0, 0, 0, 0.03));
  border: 1px solid rgba(0, 0, 0, 0.08);
  padding: 4px 10px;
  border-radius: var(--radius-full, 9999px);
  transition: all 0.2s ease;
  cursor: pointer;
  margin-bottom: 6px;
}

.duration-btn:hover {
  color: var(--ink, #18181b);
  border-color: rgba(0, 0, 0, 0.16);
  background: var(--surface-hover, rgba(0, 0, 0, 0.06));
}

[data-theme="dark"] .duration-btn {
  color: #a1a1aa;
  border-color: rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .duration-btn:hover {
  color: #f4f4f5;
  background: rgba(255, 255, 255, 0.08);
}

.thought-expanded {
  margin-top: 6px;
  margin-bottom: 8px;
  padding-left: 4px;
  background: transparent;
}

.step-details-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.step-reasoning {
  font-size: 13px;
  line-height: 1.65;
  color: var(--ink-muted, #71717a);
  white-space: pre-wrap;
  word-break: break-word;
}

[data-theme="dark"] .step-reasoning {
  color: #a1a1aa;
}

.step-scratchpad {
  font-size: 13px;
  line-height: 1.6;
  color: var(--ink-soft, #52525b);
  font-style: italic;
  white-space: pre-wrap;
  word-break: break-word;
}

[data-theme="dark"] .step-scratchpad {
  color: #d4d4d8;
}

.step-tools {
  margin-top: 4px;
}

.icon-xs {
  width: 13px;
  height: 13px;
}

.chevron {
  opacity: 0.6;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
