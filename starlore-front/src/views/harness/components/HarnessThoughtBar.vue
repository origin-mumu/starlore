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

// 思考与执行链折叠状态：默认展开，与正文融为一体，用户可随时点击顶栏收起/展开
const isExpanded = ref(true)

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
    <!-- 极简用时顶栏 (Codex 原生风格：纯文字 + 微箭头，无卡片外框) -->
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

    <!-- 展开后的流式思考与工具链路 (Codex 极简风格：无外边框、无背景卡片、无紫色大标签，和正文无缝融合) -->
    <div v-if="isExpanded" class="codex-thought-chain">
      <!-- 结构化步骤列表 -->
      <div v-if="validSteps.length > 0" class="steps-flow">
        <div
          v-for="(st, idx) in validSteps"
          :key="st.step || idx"
          class="step-flow-item"
        >
          <!-- 思考与推导正文：原生 Markdown 排版，和正文风格完全统一 -->
          <div
            v-if="st.reasoning && st.reasoning.trim().length > 0"
            class="thought-markdown"
            v-html="renderMarkdown(st.reasoning)"
          />

          <!-- 中间草稿与规划文本：同样原生纯文本排版自然呈现 -->
          <div
            v-if="st.scratchpad && st.scratchpad.trim().length > 0"
            class="thought-markdown"
            v-html="renderMarkdown(st.scratchpad)"
          />

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
          class="thought-markdown"
          v-html="renderMarkdown(reasoningContent)"
        />
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

.codex-thought-chain {
  margin: 4px 0 8px 0;
  max-height: 280px;
  overflow-y: auto;
  scrollbar-width: thin;
  padding-right: 4px;
}

.codex-thought-chain::-webkit-scrollbar {
  width: 4px;
}

.codex-thought-chain::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

[data-theme="dark"] .codex-thought-chain::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
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

/* 思考与草稿 Markdown 正文：与主回答文本字体、行高、间距完全一致，纯粹自然 */
.thought-markdown {
  font-size: 14px;
  line-height: 1.7;
  color: var(--ink, #1a1410);
  word-break: break-word;
}

[data-theme="dark"] .thought-markdown {
  color: #e4e4e7;
}

.thought-markdown :deep(p) {
  margin: 0 0 8px 0;
}

.thought-markdown :deep(p:last-child) {
  margin-bottom: 4px;
}

.thought-markdown :deep(code) {
  font-family: var(--font-mono, monospace);
  font-size: 12.5px;
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.05);
  color: var(--ink, #1a1410);
}

[data-theme="dark"] .thought-markdown :deep(code) {
  background: rgba(255, 255, 255, 0.08);
  color: #f4f4f5;
}

.thought-markdown :deep(ul),
.thought-markdown :deep(ol) {
  margin: 4px 0 8px 20px;
  padding: 0;
}

.thought-markdown :deep(li) {
  margin-bottom: 4px;
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
