<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ChevronDown, ChevronRight, Loader2 } from '@lucide/vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import HarnessToolActionRow from './HarnessToolActionRow.vue'
import type { HarnessStepDetail, HarnessToolCall } from '../types'

const props = defineProps<{
  durationMs?: number
  reasoningContent?: string
  toolCalls?: HarnessToolCall[]
  stepDetails?: HarnessStepDetail[]
  isRunning?: boolean
  /** 正在流式输出思考文字的步骤号：仅该步骤的小思考自动展开 */
  activeStep?: number | null
  /** 正文是否已开始流式输出（全部思考完成的信号） */
  bodyStarted?: boolean
}>()

// 思考总栏折叠状态：生成中默认展开；全部思考完成（正文开始输出/整体结束）时自动折叠，用户可随时点开回看
const isExpanded = ref(props.isRunning === true)

watch(() => props.isRunning, (running, prev) => {
  if (prev && !running) {
    // 整体生成结束：自动折叠思考过程
    isExpanded.value = false
  }
})

watch(() => props.bodyStarted, (started, prev) => {
  if (started && !prev) {
    // 正文开始输出：全部思考完成，折叠整体思考栏，让正文成为视觉焦点
    isExpanded.value = false
  }
})

// 每个小思考的显式展开/折叠状态：用户手动操作优先于"活跃步骤自动展开"
const thoughtOverrides = ref<Record<string, boolean>>({})

// 某个小思考当前是否处于"正在思考"的活跃状态（思考文字正在流出）
function isStepThinking(st: HarnessStepDetail): boolean {
  return Boolean(props.isRunning) && props.activeStep != null && st.step === props.activeStep
}

// 兜底单段思考：生成中且正文尚未开始输出时视为正在思考
const isFlatThinking = computed(() => Boolean(props.isRunning) && !props.bodyStarted)

function isThoughtExpanded(key: string, isActive: boolean): boolean {
  const override = thoughtOverrides.value[key]
  if (override !== undefined) return override
  // 默认：正在思考的小思考展开，思考完毕即自动折叠（保留预览行可随时点开）
  return isActive
}

function toggleThought(key: string, isActive: boolean) {
  thoughtOverrides.value[key] = !isThoughtExpanded(key, isActive)
}

function cleanAiEmoji(text: string): string {
  if (!text) return ''
  return text.replace(/(?:\p{Extended_Pictographic}|\uFE0F|\u200D)+\s*/gu, '')
}

function getPreviewText(text: string, maxLen = 30): string {
  if (!text) return ''
  const clean = cleanAiEmoji(text).replace(/[\r\n\t]+/g, ' ').trim()
  if (clean.length <= maxLen) return clean
  return `${clean.slice(0, maxLen)}...`
}

const durationText = computed(() => {
  if (!props.durationMs || props.durationMs <= 0) {
    return props.isRunning ? '正在思考与处理中...' : '用时 1s'
  }
  const totalSec = Math.floor(props.durationMs / 1000)
  if (totalSec < 60) {
    return props.isRunning ? `正在处理 (${totalSec}s)...` : `用时 ${totalSec}s`
  }
  const mins = Math.floor(totalSec / 60)
  const secs = totalSec % 60
  return props.isRunning ? `正在处理 (${mins}m ${secs}s)...` : `用时 ${mins}m ${secs}s`
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

function renderMarkdown(content: string) {
  if (!content) return ''
  const clean = cleanAiEmoji(content)
  try {
    const rawHtml = marked.parse(clean) as string
    return DOMPurify.sanitize(rawHtml)
  } catch {
    return clean
  }
}
</script>

<template>
  <div v-if="hasContent || isRunning" class="thought-bar-wrapper">
    <!-- 极简用时顶栏 (Codex 原生风格：纯文字 + 微箭头，无卡片外框，点击一键展开/收起) -->
    <div class="timing-bar">
      <button
        type="button"
        class="codex-timing-btn"
        @click="isExpanded = !isExpanded"
      >
        <Loader2 v-if="isRunning" class="icon-tiny spin text-primary" />
        <span>{{ durationText }}</span>
        <ChevronDown v-if="isExpanded" class="icon-tiny chevron" />
        <ChevronRight v-else class="icon-tiny chevron" />
      </button>
    </div>

    <!-- 展开后的流式思考与工具链路 (整体无高度限制，自然向下延展) -->
    <div v-if="isExpanded" class="codex-thought-chain">
      <!-- 结构化步骤列表 -->
      <div v-if="validSteps.length > 0" class="steps-flow">
        <div
          v-for="(st, idx) in validSteps"
          :key="st.step || idx"
          class="step-flow-item"
        >
          <!-- 小思考：正在思考时自动展开，思考完毕自动折叠（保留预览行可点开回看） -->
          <div
            v-if="st.reasoning && st.reasoning.trim().length > 0"
            class="sub-thought-item"
          >
            <div
              class="sub-thought-header"
              @click="toggleThought(`step_r_${st.step}`, isStepThinking(st))"
            >
              <span class="sub-thought-label">{{ idx === 0 ? '思考' : '再次思考' }}</span>
              <span class="sub-thought-preview">{{ getPreviewText(st.reasoning) }}</span>
              <ChevronDown v-if="isThoughtExpanded(`step_r_${st.step}`, isStepThinking(st))" class="icon-tiny chevron" />
              <ChevronRight v-else class="icon-tiny chevron" />
            </div>
            <!-- 小思考正文：带高度限制、微滚动条、灰色字体 -->
            <div
              v-if="isThoughtExpanded(`step_r_${st.step}`, isStepThinking(st))"
              class="sub-thought-body"
            >
              <div
                class="thought-markdown"
                v-html="renderMarkdown(st.reasoning)"
              />
            </div>
          </div>

          <!-- 中间规划草稿：默认折叠为预览行，点击可展开（同样带高度限制与滚动条） -->
          <div
            v-if="st.scratchpad && st.scratchpad.trim().length > 0"
            class="sub-thought-item"
          >
            <div
              class="sub-thought-header"
              @click="toggleThought(`step_s_${st.step}`, false)"
            >
              <span class="sub-thought-label">规划草稿</span>
              <span class="sub-thought-preview">{{ getPreviewText(st.scratchpad) }}</span>
              <ChevronDown v-if="isThoughtExpanded(`step_s_${st.step}`, false)" class="icon-tiny chevron" />
              <ChevronRight v-else class="icon-tiny chevron" />
            </div>
            <div
              v-if="isThoughtExpanded(`step_s_${st.step}`, false)"
              class="sub-thought-body"
            >
              <div
                class="thought-markdown"
                v-html="renderMarkdown(st.scratchpad)"
              />
            </div>
          </div>

          <!-- 步骤工具动作：单行极简内嵌行 (如 ✎ 编辑了文件、🌐 检索了知识库) -->
          <div v-if="st.tool_calls && st.tool_calls.length > 0" class="step-tools">
            <HarnessToolActionRow
              v-for="tc in st.tool_calls"
              :key="tc.call_id"
              :tool-call="tc"
            />
          </div>
        </div>
      </div>

      <!-- 兜底呈现 (单段思考：生成中展开，正文输出/结束后自动折叠) -->
      <template v-else>
        <div
          v-if="reasoningContent"
          class="sub-thought-item"
        >
          <div
            class="sub-thought-header"
            @click="toggleThought('flat_reasoning', isFlatThinking)"
          >
            <span class="sub-thought-label">思考</span>
            <span class="sub-thought-preview">{{ getPreviewText(reasoningContent) }}</span>
            <ChevronDown v-if="isThoughtExpanded('flat_reasoning', isFlatThinking)" class="icon-tiny chevron" />
            <ChevronRight v-else class="icon-tiny chevron" />
          </div>
          <div
            v-if="isThoughtExpanded('flat_reasoning', isFlatThinking)"
            class="sub-thought-body"
          >
            <div
              class="thought-markdown"
              v-html="renderMarkdown(reasoningContent)"
            />
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
  margin-bottom: 8px;
  user-select: text;
}

.timing-bar {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

/* Codex 原生极简纯文本按钮 */
.codex-timing-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  line-height: 1.5;
  font-weight: 500;
  color: var(--ink-muted, #71717a);
  background: transparent;
  border: none;
  padding: 2px 4px 2px 0;
  cursor: pointer;
  transition: color 0.15s ease;
  user-select: none;
}

.codex-timing-btn:hover {
  color: var(--ink, #18181b);
}

[data-theme="dark"] .codex-timing-btn {
  color: #a1a1aa;
}

[data-theme="dark"] .codex-timing-btn:hover {
  color: #f4f4f5;
}

/* 整体思考链：不加高度限制，自然展开 */
.codex-thought-chain {
  margin: 4px 0 8px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.steps-flow {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-flow-item {
  display: flex;
  flex-direction: column;
}

/* 小思考单项：支持折叠展开 */
.sub-thought-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 4px;
}

.sub-thought-header {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 4px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  width: fit-content;
  transition: background 0.15s ease;
  font-size: 14px;
  line-height: 1.5;
}

.sub-thought-header:hover {
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
}

[data-theme="dark"] .sub-thought-header:hover {
  background: rgba(255, 255, 255, 0.06);
}

.sub-thought-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink-muted, #71717a);
  padding: 1px 7px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.04);
  line-height: 1.4;
}

[data-theme="dark"] .sub-thought-label {
  color: #a1a1aa;
  background: rgba(255, 255, 255, 0.07);
}

.sub-thought-preview {
  font-size: 14px;
  line-height: 1.5;
  color: #8a7a6a;
  max-width: 520px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

[data-theme="dark"] .sub-thought-preview {
  color: #9ca3af;
}

/* 小思考正文容器：高度限制 + 滚动条 */
.sub-thought-body {
  max-height: 180px;
  overflow-y: auto;
  scrollbar-width: thin;
  padding: 4px 6px 4px 2px;
  margin-top: 3px;
}

.sub-thought-body::-webkit-scrollbar {
  width: 4px;
}

.sub-thought-body::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

[data-theme="dark"] .sub-thought-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
}

/* 小思考文字：字号与正文保持完全一致(14px/1.7)，颜色灰色一点区分 */
.thought-markdown {
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
  word-break: break-word;
}

[data-theme="dark"] .thought-markdown {
  color: #94a3b8;
}

.thought-markdown :deep(p) {
  margin: 0 0 6px 0;
  color: inherit;
}

.thought-markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.thought-markdown :deep(code) {
  font-family: var(--font-mono, monospace);
  font-size: 12px;
  padding: 1px 4px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.04);
  color: #475569;
}

[data-theme="dark"] .thought-markdown :deep(code) {
  background: rgba(255, 255, 255, 0.06);
  color: #cbd5e1;
}

.thought-markdown :deep(ul),
.thought-markdown :deep(ol) {
  margin: 4px 0 6px 18px;
  padding: 0;
  color: inherit;
}

.thought-markdown :deep(li) {
  margin-bottom: 3px;
}

.step-tools {
  margin: 4px 0 8px 0;
}

.icon-tiny {
  width: 14px;
  height: 14px;
}

.text-primary {
  color: #3b82f6;
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
