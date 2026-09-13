<script setup lang="ts">
import { ref } from 'vue'
import {
  BookOpen,
  Presentation,
  FileText,
  FileEdit,
  Globe,
  Sparkles,
  ChevronRight,
  ChevronDown,
  Loader2,
  Check,
  AlertCircle,
} from '@lucide/vue'
import type { HarnessToolCall } from '../types'

const props = defineProps<{
  toolCall: HarnessToolCall
}>()

const isExpanded = ref(false)

function toggleExpand() {
  if (props.toolCall.citations && props.toolCall.citations.length > 0) {
    isExpanded.value = !isExpanded.value
  }
}

function getIcon(toolName: string) {
  const lower = toolName.toLowerCase()
  if (lower.includes('knowledge') || lower.includes('search')) return BookOpen
  if (lower.includes('ppt') || lower.includes('presentation')) return Presentation
  if (lower.includes('browser') || lower.includes('web') || lower.includes('url')) return Globe
  if (lower.includes('edit') || lower.includes('write')) return FileEdit
  if (lower.includes('doc') || lower.includes('file')) return FileText
  return Sparkles
}
</script>

<template>
  <div class="tool-row-container">
    <div
      class="tool-label-row"
      :class="{
        clickable: toolCall.citations && toolCall.citations.length > 0,
      }"
      @click="toggleExpand"
    >
      <!-- 状态图标 -->
      <Loader2
        v-if="toolCall.status === 'running'"
        class="icon-sm spin text-primary"
      />
      <component
        :is="getIcon(toolCall.tool)"
        v-else
        class="icon-sm muted-icon"
      />

      <!-- 动作文本 -->
      <span class="tool-name">
        {{ toolCall.label || toolCall.tool }}
      </span>

      <!-- 动作详情摘要 -->
      <span v-if="toolCall.summary" class="tool-summary">
        · {{ toolCall.summary }}
      </span>

      <!-- 引用展开箭头 -->
      <span
        v-if="toolCall.citations && toolCall.citations.length > 0"
        class="chevron-wrap"
      >
        <ChevronDown v-if="isExpanded" class="icon-xs" />
        <ChevronRight v-else class="icon-xs" />
      </span>

      <!-- 状态微标 -->
      <Check v-if="toolCall.status === 'success'" class="icon-xs text-success" />
      <AlertCircle v-else-if="toolCall.status === 'error'" class="icon-xs text-danger" />
    </div>

    <!-- 缩进展开的引用文章树形列表 (Codex 纯文字树形排版) -->
    <div
      v-if="isExpanded && toolCall.citations && toolCall.citations.length > 0"
      class="citations-tree"
    >
      <div
        v-for="(citation, idx) in toolCall.citations"
        :key="idx"
        class="citation-line"
      >
        <span class="tree-branch">
          {{ idx === toolCall.citations.length - 1 ? '└─' : '├─' }}
        </span>
        <span class="citation-text">{{ citation }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tool-row-container {
  margin: 4px 0;
  font-size: 14px;
  color: var(--ink-muted, #71717a);
  user-select: none;
}

[data-theme="dark"] .tool-row-container {
  color: #a1a1aa;
}

.tool-label-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 6px;
  border-radius: 6px;
  transition: all 0.15s ease;
  font-size: 14px;
  line-height: 1.5;
}

.tool-label-row.clickable {
  cursor: pointer;
}

.tool-label-row:hover {
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
  color: var(--ink, #18181b);
}

[data-theme="dark"] .tool-label-row:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #ffffff;
}

.tool-name {
  font-weight: 500;
  color: var(--ink-soft, #3f3f46);
  font-size: 14px;
}

[data-theme="dark"] .tool-name {
  color: #d4d4d8;
}

.tool-summary {
  color: var(--ink-muted, #8a7a6a);
  font-size: 14px;
}

.chevron-wrap {
  display: inline-flex;
  align-items: center;
  color: #a1a1aa;
}

.citations-tree {
  padding-left: 18px;
  margin-top: 2px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
  font-family: monospace;
}

.citation-line {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.3;
}

.tree-branch {
  color: #a1a1aa;
  flex-shrink: 0;
}

.citation-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.icon-sm {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}

.icon-xs {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}

.muted-icon {
  color: #a1a1aa;
}

.text-primary {
  color: #2563eb;
}

.text-success {
  color: #10b981;
}

.text-danger {
  color: #ef4444;
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
