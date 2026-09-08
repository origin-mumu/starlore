<script setup lang="ts">
import { computed, ref } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { ChevronDown, ChevronUp } from '@lucide/vue'
import HarnessThoughtBar from './HarnessThoughtBar.vue'
import HarnessArtifactCard from './HarnessArtifactCard.vue'
import type { HarnessMessage } from '../types'

const props = defineProps<{
  message: HarnessMessage
  isRunning?: boolean
}>()

const isUserLongExpanded = ref(false)

const isUser = computed(() => props.message.role === 'user')

// 用户超长消息折叠判断 (> 400 字符)
const shouldTruncateUser = computed(() => {
  return isUser.value && props.message.content.length > 400
})

const displayUserContent = computed(() => {
  if (shouldTruncateUser.value && !isUserLongExpanded.value) {
    return props.message.content.slice(0, 380) + '...'
  }
  return props.message.content
})

// Markdown 渲染
const renderedContent = computed(() => {
  if (!props.message.content) return ''
  try {
    return DOMPurify.sanitize(marked.parse(props.message.content) as string)
  } catch {
    return props.message.content
  }
})
</script>

<template>
  <div class="message-wrapper">
    <!-- 用户消息：右侧浅灰圆角气泡 -->
    <div v-if="isUser" class="user-row">
      <div class="user-bubble">
        <div class="user-text">
          {{ displayUserContent }}
        </div>
        <!-- 截断时展开/收起按钮 -->
        <button
          v-if="shouldTruncateUser"
          type="button"
          class="user-more-btn"
          @click="isUserLongExpanded = !isUserLongExpanded"
        >
          <span>{{ isUserLongExpanded ? '收起' : '显示更多' }}</span>
          <ChevronUp v-if="isUserLongExpanded" class="icon-xs" />
          <ChevronDown v-else class="icon-xs" />
        </button>
      </div>
    </div>

    <!-- AI 消息：画布级极简，无背景卡片、无外边框 -->
    <div v-else class="ai-row">
      <!-- 思考过程与工具用时顶栏 (Codex 风格) -->
      <HarnessThoughtBar
        :duration-ms="message.duration_ms"
        :reasoning-content="message.reasoning_content"
        :tool-calls="message.tool_calls"
        :step-details="message.step_details"
        :is-running="isRunning"
      />

      <!-- 正文纯文字 Markdown 排版 -->
      <div
        v-if="message.content"
        class="ai-markdown"
        v-html="renderedContent"
      />

      <!-- 交付产物卡片 (仅当生成了 PPT/Doc 时附在最下方) -->
      <div v-if="message.artifacts && message.artifacts.length > 0" class="artifacts-container">
        <HarnessArtifactCard
          v-for="artifact in message.artifacts"
          :key="artifact.file_id"
          :artifact="artifact"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.message-wrapper {
  padding: 8px 16px;
  max-width: 820px;
  margin: 0 auto;
  width: 100%;
}

.user-row {
  display: flex;
  justify-content: flex-end;
}

.user-bubble {
  background: #f0f2f5;
  color: #18181b;
  border-radius: 18px;
  padding: 10px 16px;
  max-width: 580px;
  font-size: 14px;
  line-height: 1.6;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

[data-theme="dark"] .user-bubble {
  background: #27272a;
  color: #f4f4f5;
}

.user-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.user-more-btn {
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: #2563eb;
  cursor: pointer;
}

.user-more-btn:hover {
  text-decoration: underline;
}

.ai-row {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.ai-markdown {
  font-size: 14px;
  line-height: 1.7;
  color: #27272a;
  word-break: break-word;
}

[data-theme="dark"] .ai-markdown {
  color: #e4e4e7;
}

.ai-markdown :deep(h1),
.ai-markdown :deep(h2),
.ai-markdown :deep(h3) {
  font-weight: 600;
  margin: 1em 0 0.4em;
  color: #18181b;
}

[data-theme="dark"] .ai-markdown :deep(h1),
[data-theme="dark"] .ai-markdown :deep(h2),
[data-theme="dark"] .ai-markdown :deep(h3) {
  color: #f4f4f5;
}

.ai-markdown :deep(p) {
  margin: 0.5em 0;
}

.ai-markdown :deep(ul),
.ai-markdown :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.2em;
}

.ai-markdown :deep(li) {
  margin: 0.25em 0;
}

.ai-markdown :deep(code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12.5px;
}

[data-theme="dark"] .ai-markdown :deep(code) {
  background: rgba(255, 255, 255, 0.1);
}

/* ── 表格排版：避免生硬断行换行，支持横向自定义滚动条 ── */
.ai-markdown :deep(table) {
  display: block;
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border-collapse: separate;
  border-spacing: 0;
  margin: 16px 0;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 14px;
  background: var(--surface, rgba(255, 255, 255, 0.7));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  scrollbar-width: thin;
  scrollbar-color: var(--accent, #DE4331) rgba(0, 0, 0, 0.04);
}

[data-theme="dark"] .ai-markdown :deep(table) {
  background: var(--surface, rgba(24, 24, 28, 0.75));
  border-color: rgba(255, 255, 255, 0.08);
  scrollbar-color: var(--accent, #DE4331) rgba(255, 255, 255, 0.04);
}

/* 自定义横向滚动条 */
.ai-markdown :deep(table)::-webkit-scrollbar {
  height: 6px;
}

.ai-markdown :deep(table)::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 999px;
  margin: 0 8px;
}

[data-theme="dark"] .ai-markdown :deep(table)::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.ai-markdown :deep(table)::-webkit-scrollbar-thumb {
  background: var(--accent-gradient, linear-gradient(135deg, #DE4331, #FCC841));
  border-radius: 999px;
}

.ai-markdown :deep(table)::-webkit-scrollbar-thumb:hover {
  background: var(--accent, #DE4331);
}

/* 单元格与表头排版规范：禁止单字生硬换行 */
.ai-markdown :deep(th),
.ai-markdown :deep(td) {
  padding: 10px 18px;
  text-align: left;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  border-right: 1px solid rgba(0, 0, 0, 0.04);
  font-size: 13.5px;
  line-height: 1.6;
  white-space: nowrap; /* 核心：避免单字、短文本生硬断行折叠 */
  vertical-align: middle;
}

[data-theme="dark"] .ai-markdown :deep(th),
[data-theme="dark"] .ai-markdown :deep(td) {
  border-bottom-color: rgba(255, 255, 255, 0.06);
  border-right-color: rgba(255, 255, 255, 0.04);
}

.ai-markdown :deep(tr:last-child td) {
  border-bottom: none;
}

.ai-markdown :deep(th:last-child),
.ai-markdown :deep(td:last-child) {
  border-right: none;
}

.ai-markdown :deep(th) {
  font-weight: 650;
  color: var(--ink, #1A1410);
  background: rgba(0, 0, 0, 0.03);
  font-size: 13px;
  letter-spacing: 0.02em;
}

[data-theme="dark"] .ai-markdown :deep(th) {
  color: #f4f4f5;
  background: rgba(255, 255, 255, 0.04);
}

.ai-markdown :deep(tr:hover td) {
  background: var(--surface-hover, rgba(0, 0, 0, 0.02));
}

[data-theme="dark"] .ai-markdown :deep(tr:hover td) {
  background: rgba(255, 255, 255, 0.03);
}

.artifacts-container {
  margin-top: 8px;
}

.icon-xs {
  width: 12px;
  height: 12px;
}
</style>
