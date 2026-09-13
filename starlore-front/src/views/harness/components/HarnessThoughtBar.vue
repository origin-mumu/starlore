<script setup lang="ts">
import { computed, ref } from 'vue'
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
}>()

// 思考总栏折叠状态：默认展开，用户可随时点击“用时 21s”一键折叠全部
const isExpanded = ref(true)

// 每个小思考的独立折叠状态，默认展开（带高度限制与滚动条）
const expandedThoughts = ref<Record<string, boolean>>({})

function toggleThought(key: string) {
  expandedThoughts.value[key] = !isThoughtExpanded(key)
}

function isThoughtExpanded(key: string): boolean {
  if (expandedThoughts.value[key] !== undefined) {
    return expandedThoughts.value[key]
  }
  // 默认小思考展开，若长则受到高度限制并出现滚动条
  return true
}

function getPreviewText(text: string, maxLen = 30): string {
  if (!text) return ''
  const clean = text.replace(/[\r\n\t]+/g, ' ').trim()
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
  try {
    const rawHtml = marked.parse(content) as string
    return DOMPurify.sanitize(rawHtml)
  } catch {
    return content
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
          <!-- 小思考：可折叠、加高度限制、灰色字体与正文区分 -->
          <div
            v-if="st.reasoning && st.reasoning.trim().length > 0"
            class="sub-thought-item"
          >
            <div
              class="sub-thought-header"
              @click="toggleThought(`step_r_${idx}`)"
            >
              <span class="sub-thought-label">{{ idx === 0 ? '思考' : '再次思考' }}</span>
              <span class="sub-thought-preview">{{ getPreviewText(st.reasoning) }}</span>
              <ChevronDown v-if="isThoughtExpanded(`step_r_${idx}`)" class="icon-tiny chevron" />
              <ChevronRight v-else class="icon-tiny chevron" />
            </div>
            <!-- 小思考正文：带高度限制、微滚动条、灰色字体 -->
            <div
              v-if="isThoughtExpanded(`step_r_${idx}`)"
              class="sub-thought-body"
            >
              <div
                class="thought-markdown"
                v-html="renderMarkdown(st.reasoning)"
              />
            </div>
          </div>

          <!-- 中间规划草稿：同样支持折叠、高度限制、灰色字体 -->
          <div
            v-if="st.scratchpad && st.scratchpad.trim().length > 0"
            class="sub-thought-item"
          >
            <div
              class="sub-thought-header"
              @click="toggleThought(`step_s_${idx}`)"
            >
              <span class="sub-thought-label">规划草稿</span>
              <span class="sub-thought-preview">{{ getPreviewText(st.scratchpad) }}</span>
              <ChevronDown v-if="isThoughtExpanded(`step_s_${idx}`)" class="icon-tiny chevron" />
              <ChevronRight v-else class="icon-tiny chevron" />
            </div>
            <div
              v-if="isThoughtExpanded(`step_s_${idx}`)"
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

      <!-- 兜底呈现 (单段思考) -->
      <template v-else>
        <div
          v-if="reasoningContent"
          class="sub-thought-item"
        >
          <div
            class="sub-thought-header"
            @click="toggleThought('flat_reasoning')"
          >
            <span class="sub-thought-label">思考</span>
            <span class="sub-thought-preview">{{ getPreviewText(reasoningContent) }}</span>
            <ChevronDown v-if="isThoughtExpanded('flat_reasoning')" class="icon-tiny chevron" />
            <ChevronRight v-else class="icon-tiny chevron" />
          </div>
          <div
            v-if="isThoughtExpanded('flat_reasoning')"
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
  gap: 4px;
  font-size: 12.5px;
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
}

.sub-thought-header:hover {
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
}

[data-theme="dark"] .sub-thought-header:hover {
  background: rgba(255, 255, 255, 0.06);
}

.sub-thought-label {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--ink-muted, #71717a);
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.04);
}

[data-theme="dark"] .sub-thought-label {
  color: #a1a1aa;
  background: rgba(255, 255, 255, 0.07);
}

.sub-thought-preview {
  font-size: 12.5px;
  color: #8a7a6a;
  max-width: 480px;
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
  width: 12px;
  height: 12px;
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
