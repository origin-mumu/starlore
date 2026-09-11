<script setup lang="ts">
import { computed, ref } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import {
  ChevronDown,
  ChevronUp,
  Loader2,
  Sparkles,
  BookOpen,
  Presentation,
  FileText,
} from '@lucide/vue'
import HarnessThoughtBar from './HarnessThoughtBar.vue'
import HarnessArtifactCard from './HarnessArtifactCard.vue'
import type { HarnessMessage, HarnessToolCall } from '../types'

const props = defineProps<{
  message: HarnessMessage
  isRunning?: boolean
}>()

const isUserLongExpanded = ref(false)

const isUser = computed(() => props.message.role === 'user')

const messageImages = computed(() => {
  if (props.message.images && props.message.images.length > 0) {
    return props.message.images
  }
  if (
    props.message.artifacts &&
    typeof props.message.artifacts === 'object' &&
    Array.isArray((props.message.artifacts as any).images)
  ) {
    return (props.message.artifacts as any).images as string[]
  }
  return []
})

// 用户超长消息折叠判断 (> 400 字符)
const shouldTruncateUser = computed(() => {
  return isUser.value && (props.message.content?.length || 0) > 400
})

const displayUserContent = computed(() => {
  let content = props.message.content || ''
  // 过滤掉冗余的前缀如 【图片附件: ...】
  if (messageImages.value.length > 0 || content.startsWith('【图片附件:')) {
    content = content.replace(/^【图片附件:[^】]+】\s*/g, '').trim()
  }
  if (shouldTruncateUser.value && !isUserLongExpanded.value) {
    return content.slice(0, 380) + '...'
  }
  return content
})

// 当前处于执行中的具体工具调用（如生成文档、生成PPT、检索知识库等）
const activeRunningTool = computed<HarnessToolCall | null>(() => {
  if (!props.isRunning || !props.message.tool_calls) return null
  return props.message.tool_calls.find((t) => t.status === 'running') || null
})

function getToolIcon(toolName?: string) {
  if (!toolName) return Sparkles
  const t = toolName.toLowerCase()
  if (t.includes('knowledge') || t.includes('search')) return BookOpen
  if (t.includes('ppt') || t.includes('presentation')) return Presentation
  if (t.includes('doc') || t.includes('file')) return FileText
  return Sparkles
}

// Markdown 渲染
const renderedContent = computed(() => {
  if (!props.message.content) return ''
  try {
    const rawHtml = marked.parse(props.message.content) as string
    // 为 table 包裹 table-wrapper 容器，确保 100% 宽度充满卡片、消除右侧空白，并支持内容自然换行与横向滑动
    const wrappedHtml = rawHtml.replace(
      /<table\b([^>]*)>([\s\S]*?)<\/table>/gi,
      '<div class="table-wrapper"><table$1>$2</table></div>'
    )
    return DOMPurify.sanitize(wrappedHtml)
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
        <!-- 用户上传的真实图片展示 -->
        <div v-if="messageImages.length > 0" class="user-images-grid">
          <el-image
            v-for="(img, idx) in messageImages"
            :key="idx"
            :src="img"
            :preview-src-list="messageImages"
            :initial-index="idx"
            preview-teleported
            fit="cover"
            class="user-bubble-image"
          />
        </div>
        <div v-if="displayUserContent" class="user-text">
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

      <!-- 正文纯文字 Markdown 排版（流式输出时带轻量跳动光标） -->
      <div v-if="message.content" class="ai-markdown-wrap">
        <div
          class="ai-markdown"
          v-html="renderedContent"
        />
        <span v-if="isRunning" class="streaming-inline-cursor">▌</span>
      </div>

      <!-- 工具执行中提示卡片（仅当具体工具在执行时展示） -->
      <div v-if="isRunning && activeRunningTool" class="generating-status-container">
        <div class="active-tool-badge">
          <div class="active-tool-spin">
            <Loader2 class="icon-sm spin" />
          </div>
          <component :is="getToolIcon(activeRunningTool.tool)" class="icon-sm tool-type-icon" />
          <div class="active-tool-texts">
            <span class="active-tool-title">
              {{ activeRunningTool.label || activeRunningTool.tool }} 进行中...
            </span>
            <span class="active-tool-sub">
              {{ activeRunningTool.summary && activeRunningTool.summary !== '正在执行...' ? activeRunningTool.summary : 'AI 正在处理与排版产物，即将生成' }}
            </span>
          </div>
        </div>
      </div>

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

.user-images-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.user-bubble-image {
  max-width: 280px;
  max-height: 220px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease;
  display: block;
}

.user-bubble-image:hover {
  transform: scale(1.02);
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

/* ── 表格排版：自适应容器宽度与内容自然折行，同时保留横向平滑滚动 ── */
.ai-markdown :deep(.table-wrapper) {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
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

[data-theme="dark"] .ai-markdown :deep(.table-wrapper) {
  background: var(--surface, rgba(24, 24, 28, 0.75));
  border-color: rgba(255, 255, 255, 0.08);
  scrollbar-color: var(--accent, #DE4331) rgba(255, 255, 255, 0.04);
}

/* 自定义横向滚动条 */
.ai-markdown :deep(.table-wrapper)::-webkit-scrollbar {
  height: 6px;
}

.ai-markdown :deep(.table-wrapper)::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 999px;
  margin: 0 8px;
}

[data-theme="dark"] .ai-markdown :deep(.table-wrapper)::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.ai-markdown :deep(.table-wrapper)::-webkit-scrollbar-thumb {
  background: var(--accent-gradient, linear-gradient(135deg, #DE4331, #FCC841));
  border-radius: 999px;
}

.ai-markdown :deep(.table-wrapper)::-webkit-scrollbar-thumb:hover {
  background: var(--accent, #DE4331);
}

/* 核心：table 填满 100% 宽度，消除右侧空白 */
.ai-markdown :deep(table) {
  display: table;
  width: 100%;
  min-width: 100%;
  border-collapse: collapse;
  margin: 0;
  border: none;
  background: transparent;
}

/* 单元格与表头排版规范：支持长文本换行，消除右侧空白 */
.ai-markdown :deep(th),
.ai-markdown :deep(td) {
  padding: 10px 18px;
  text-align: left;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  border-right: 1px solid rgba(0, 0, 0, 0.04);
  font-size: 13.5px;
  line-height: 1.6;
  vertical-align: middle;
  white-space: normal; /* 关键：超出宽度自然换行，不再强制单行不折叠 */
  word-break: break-word;
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
  white-space: nowrap;
}

/* 第一列一般为短标题/关键词，保持适当最小宽度 */
.ai-markdown :deep(th:first-child),
.ai-markdown :deep(td:first-child) {
  min-width: 100px;
}

/* 内容列（第二列及以后）宽度更宽裕，内容较多时优雅换行 */
.ai-markdown :deep(th:not(:first-child)),
.ai-markdown :deep(td:not(:first-child)) {
  min-width: 160px;
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

/* ── 生成中的状态卡片与动效 ── */
.generating-status-container {
  margin-top: 12px;
}

.active-tool-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: var(--surface, rgba(255, 255, 255, 0.8));
  border: 1px solid rgba(222, 67, 49, 0.25);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(222, 67, 49, 0.08);
  backdrop-filter: blur(12px);
  animation: pulseBadge 2s ease-in-out infinite;
}

[data-theme="dark"] .active-tool-badge {
  background: rgba(30, 32, 40, 0.85);
  border-color: rgba(99, 133, 255, 0.35);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

@keyframes pulseBadge {
  0%, 100% {
    border-color: rgba(222, 67, 49, 0.25);
  }
  50% {
    border-color: rgba(222, 67, 49, 0.55);
  }
}

.active-tool-spin {
  display: flex;
  align-items: center;
  justify-content: center;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.tool-type-icon {
  color: var(--accent, #DE4331);
}

.active-tool-texts {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.active-tool-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink, #18181b);
}

[data-theme="dark"] .active-tool-title {
  color: #f4f4f5;
}

.active-tool-sub {
  font-size: 12px;
  color: var(--ink-muted, #71717a);
}

/* 流式输出轻量闪烁光标 */
.ai-markdown-wrap {
  position: relative;
  display: inline-block;
  width: 100%;
}

.streaming-inline-cursor {
  display: inline-block;
  color: var(--accent, #DE4331);
  font-weight: 700;
  margin-left: 2px;
  animation: cursorBlink 0.9s infinite ease-in-out;
}

@keyframes cursorBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.artifacts-container {
  margin-top: 8px;
}

.icon-xs {
  width: 12px;
  height: 12px;
}
</style>
