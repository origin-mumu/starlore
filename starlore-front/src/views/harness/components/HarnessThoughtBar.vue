<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ChevronDown, ChevronRight, Loader2, Brain } from '@lucide/vue'
import HarnessToolActionRow from './HarnessToolActionRow.vue'
import type { HarnessStepDetail, HarnessToolCall } from '../types'

const props = defineProps<{
  durationMs?: number
  reasoningContent?: string
  toolCalls?: HarnessToolCall[]
  stepDetails?: HarnessStepDetail[]
  isRunning?: boolean
}>()

// 思考总栏：生成中默认展开，生成完成之后自动折叠起来
const isExpanded = ref(Boolean(props.isRunning))

watch(
  () => props.isRunning,
  (running, oldRunning) => {
    if (oldRunning && !running) {
      // 生成结束时自动折叠
      isExpanded.value = false
    } else if (running) {
      isExpanded.value = true
    }
  },
  { immediate: true }
)

// 各分段小思考的展开/折叠状态，默认全部折叠
const expandedThoughts = ref<Record<string, boolean>>({})

function toggleThought(key: string) {
  expandedThoughts.value[key] = !expandedThoughts.value[key]
}

function isThoughtExpanded(key: string, idx?: number) {
  if (expandedThoughts.value[key] !== undefined) {
    return expandedThoughts.value[key]
  }
  // 正在生成中且为当前最新活跃步骤时：默认展开推导内容，让用户看到文字实时流淌
  if (props.isRunning) {
    if (idx !== undefined && idx === validSteps.value.length - 1) {
      return true
    }
    if (key === 'flat_reasoning') {
      return true
    }
  }
  return false
}

function getPreviewText(text: string, maxLen = 24): string {
  if (!text) return ''
  const clean = text.replace(/[\r\n\t]+/g, ' ').trim()
  if (clean.length <= maxLen) return clean
  return `${clean.slice(0, maxLen)}...`
}

const durationText = computed(() => {
  if (!props.durationMs || props.durationMs <= 0) {
    return props.isRunning ? '正在思考与生成中...' : '已深度思考 (1s)'
  }
  const totalSec = Math.floor(props.durationMs / 1000)
  if (totalSec < 60) {
    return props.isRunning ? `正在生成 (${totalSec}s)...` : `已深度思考 (${totalSec}s)`
  }
  const mins = Math.floor(totalSec / 60)
  const secs = totalSec % 60
  return props.isRunning ? `正在生成 (${mins}m ${secs}s)...` : `已深度思考 (${mins}m ${secs}s)`
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
    <!-- 极简用时折叠顶栏（生成中展开，生成完成自动折叠） -->
    <button
      type="button"
      class="duration-btn"
      @click="isExpanded = !isExpanded"
    >
      <Loader2 v-if="isRunning" class="icon-xs spin" />
      <Brain v-else class="icon-xs brain-icon" />
      <span>{{ durationText }}</span>
      <ChevronDown v-if="isExpanded" class="icon-xs chevron" />
      <ChevronRight v-else class="icon-xs chevron" />
    </button>

    <!-- 展开后的细分链条：小思考(折叠前几个字) -> 工具调用(折叠) -> 再次思考(折叠) -->
    <div v-if="isExpanded" class="thought-expanded">
      <!-- 结构化步骤列表 -->
      <div v-if="validSteps.length > 0" class="step-details-container">
        <div
          v-for="(st, idx) in validSteps"
          :key="st.step || idx"
          class="step-block"
        >
          <!-- 该步骤的深度思维链（折叠前几个字，点击展开） -->
          <div v-if="st.reasoning && st.reasoning.trim().length > 0" class="sub-thought-item">
            <div
              class="sub-thought-header"
              @click="toggleThought(`step_r_${idx}`)"
            >
              <span class="sub-thought-tag">
                {{ idx === 0 ? '思考' : '再次思考' }}
              </span>
              <span class="sub-thought-preview">
                {{ getPreviewText(st.reasoning) }}
              </span>
              <ChevronDown v-if="isThoughtExpanded(`step_r_${idx}`, idx)" class="icon-tiny chevron" />
              <ChevronRight v-else class="icon-tiny chevron" />
            </div>

            <!-- 展开后的完整推导正文 -->
            <div v-if="isThoughtExpanded(`step_r_${idx}`, idx)" class="sub-thought-body">
              {{ st.reasoning }}
            </div>
          </div>

          <!-- 该步骤中间规划草稿（若有） -->
          <div v-if="st.scratchpad && st.scratchpad.trim().length > 0" class="sub-thought-item">
            <div
              class="sub-thought-header"
              @click="toggleThought(`step_s_${idx}`)"
            >
              <span class="sub-thought-tag tag-scratch">
                规划草稿
              </span>
              <span class="sub-thought-preview">
                {{ getPreviewText(st.scratchpad) }}
              </span>
              <ChevronDown v-if="isThoughtExpanded(`step_s_${idx}`, idx)" class="icon-tiny chevron" />
              <ChevronRight v-else class="icon-tiny chevron" />
            </div>
            <div v-if="isThoughtExpanded(`step_s_${idx}`, idx)" class="sub-thought-body scratch-body">
              {{ st.scratchpad }}
            </div>
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

      <!-- 兜底呈现（单个长思考链分段折叠） -->
      <template v-else>
        <div v-if="reasoningContent" class="sub-thought-item">
          <div
            class="sub-thought-header"
            @click="toggleThought('flat_reasoning')"
          >
            <span class="sub-thought-tag">思考</span>
            <span class="sub-thought-preview">
              {{ getPreviewText(reasoningContent) }}
            </span>
            <ChevronDown v-if="isThoughtExpanded('flat_reasoning')" class="icon-tiny chevron" />
            <ChevronRight v-else class="icon-tiny chevron" />
          </div>
          <div v-if="isThoughtExpanded('flat_reasoning')" class="sub-thought-body">
            {{ reasoningContent }}
          </div>
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
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-muted, #71717a);
  background: var(--surface, rgba(0, 0, 0, 0.03));
  border: 1px solid rgba(0, 0, 0, 0.08);
  padding: 4px 11px;
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

.brain-icon {
  color: #8b5cf6;
}

.thought-expanded {
  margin-top: 6px;
  margin-bottom: 8px;
  padding: 10px 14px;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 12px;
}

[data-theme="dark"] .thought-expanded {
  background: rgba(255, 255, 255, 0.03);
  border-color: rgba(255, 255, 255, 0.06);
}

.step-details-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 细分子思考单项 */
.sub-thought-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sub-thought-header {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 3px 6px;
  border-radius: 6px;
  transition: background 0.15s ease;
  max-width: fit-content;
}

.sub-thought-header:hover {
  background: rgba(0, 0, 0, 0.04);
}

[data-theme="dark"] .sub-thought-header:hover {
  background: rgba(255, 255, 255, 0.06);
}

.sub-thought-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(139, 92, 246, 0.1);
  color: #7c3aed;
}

.tag-scratch {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}

[data-theme="dark"] .sub-thought-tag {
  background: rgba(139, 92, 246, 0.2);
  color: #a78bfa;
}

.sub-thought-preview {
  font-size: 12.5px;
  color: var(--ink-muted, #71717a);
}

[data-theme="dark"] .sub-thought-preview {
  color: #a1a1aa;
}

.sub-thought-body {
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--ink-muted, #71717a);
  white-space: pre-wrap;
  word-break: break-word;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.02);
  border-left: 2.5px solid #8b5cf6;
  border-radius: 0 8px 8px 0;
  margin-top: 2px;
  margin-left: 4px;
}

.scratch-body {
  border-left-color: #3b82f6;
  font-style: italic;
}

[data-theme="dark"] .sub-thought-body {
  color: #a1a1aa;
  background: rgba(255, 255, 255, 0.02);
}

.step-tools {
  margin-top: 2px;
  margin-bottom: 2px;
}

.icon-xs {
  width: 13px;
  height: 13px;
}

.icon-tiny {
  width: 11px;
  height: 11px;
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
