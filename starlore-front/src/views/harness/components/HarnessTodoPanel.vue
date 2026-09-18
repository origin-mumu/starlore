<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ChevronDown, ChevronUp, ListChecks, Loader2 } from '@lucide/vue'
import type { HarnessTodoItem } from '../types'

const props = defineProps<{
  todos: HarnessTodoItem[]
  isRunning?: boolean
}>()

// 执行中默认展开跟随；结束后自动收起为一行摘要，可手动展开回看
const isExpanded = ref(props.isRunning === true)

watch(
  () => props.isRunning,
  (running, prev) => {
    if (prev && !running) {
      isExpanded.value = false
    }
  }
)

const doneCount = computed(() => props.todos.filter((t) => t.status === 'completed').length)
const activeCount = computed(() => props.todos.filter((t) => t.status === 'in_progress').length)
const pendingCount = computed(
  () => props.todos.length - doneCount.value - activeCount.value
)

// 头部进度统计：零值段省略（对齐 deepseek-harness TodoPanel 的 progressLabel 行为）
const progressText = computed(() => {
  const parts: string[] = []
  if (doneCount.value > 0) parts.push(`${doneCount.value} 已完成`)
  if (activeCount.value > 0) parts.push(`${activeCount.value} 进行中`)
  if (pendingCount.value > 0) parts.push(`${pendingCount.value} 待处理`)
  return parts.join(' · ')
})
</script>

<template>
  <div v-if="todos.length > 0" class="todo-panel">
    <button type="button" class="todo-header" :aria-expanded="isExpanded" @click="isExpanded = !isExpanded">
      <ListChecks class="icon-sm header-icon" />
      <span class="header-title">任务清单</span>
      <span class="header-progress">{{ progressText }}</span>
      <ChevronUp v-if="isExpanded" class="icon-xs chevron" />
      <ChevronDown v-else class="icon-xs chevron" />
    </button>

    <ul v-if="isExpanded" class="todo-list">
      <li
        v-for="(item, idx) in todos"
        :key="idx"
        class="todo-item"
        :class="`is-${item.status}`"
      >
        <!-- 三态图标：进行中细环旋转 / 已完成实勾圆环 / 待处理虚线圈 -->
        <Loader2 v-if="item.status === 'in_progress'" class="icon-xs status-icon spin text-accent" />
        <svg
          v-else-if="item.status === 'completed'"
          class="status-icon is-done"
          viewBox="0 0 14 14"
          fill="none"
          aria-hidden="true"
        >
          <circle cx="7" cy="7" r="6.4" stroke="currentColor" stroke-width="1.2" />
          <path
            d="M4.6 7.1 6.3 8.8 9.5 5.4"
            stroke="currentColor"
            stroke-width="1.3"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <svg v-else class="status-icon" viewBox="0 0 14 14" fill="none" aria-hidden="true">
          <circle cx="7" cy="7" r="6.4" stroke="currentColor" stroke-width="1.2" stroke-dasharray="2.4 2.4" />
        </svg>

        <span class="item-content">{{ item.content }}</span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.todo-panel {
  pointer-events: auto;
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #ffffff;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.07);
  overflow: hidden;
}

[data-theme="dark"] .todo-panel {
  border-color: rgba(255, 255, 255, 0.1);
  background: #1c1c22;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
}

/* 头部：极简一行，纯文字 + 进度统计（Codex 式风格，与用时折叠栏同气质） */
.todo-header {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  user-select: none;
  text-align: left;
}

.header-icon {
  width: 15px;
  height: 15px;
  color: var(--ink-soft, #5C4D3D);
  flex-shrink: 0;
}

.header-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink, #1A1410);
  white-space: nowrap;
}

.header-progress {
  font-size: 12px;
  color: var(--ink-muted, #8A7A6A);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chevron {
  margin-left: auto;
  color: var(--ink-muted, #8A7A6A);
  flex-shrink: 0;
}

[data-theme="dark"] .header-icon {
  color: #a1a1aa;
}

[data-theme="dark"] .header-title {
  color: #f4f4f5;
}

[data-theme="dark"] .header-progress,
[data-theme="dark"] .chevron {
  color: #71717a;
}

/* 清单行：三态图标 + 内容 */
.todo-list {
  list-style: none;
  margin: 0;
  padding: 2px 14px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 220px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.todo-item {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  font-size: 12.5px;
  line-height: 1.55;
  color: var(--ink, #1A1410);
}

[data-theme="dark"] .todo-item {
  color: #e4e4e7;
}

.status-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  margin-top: 2px;
}

.status-icon.is-done {
  color: #059669;
}

[data-theme="dark"] .status-icon.is-done {
  color: #34d399;
}

.todo-item:not(.is-in_progress):not(.is-completed) .status-icon {
  color: var(--ink-muted, #8A7A6A);
}

[data-theme="dark"] .todo-item:not(.is-in_progress):not(.is-completed) .status-icon {
  color: #71717a;
}

.text-accent {
  color: var(--accent, #DE4331);
}

.item-content {
  min-width: 0;
  word-break: break-word;
}

.todo-item.is-completed .item-content {
  color: var(--ink-muted, #8A7A6A);
  text-decoration: line-through;
  text-decoration-color: rgba(0, 0, 0, 0.25);
}

[data-theme="dark"] .todo-item.is-completed .item-content {
  color: #71717a;
  text-decoration-color: rgba(255, 255, 255, 0.25);
}

.spin {
  animation: todo-spin 1s linear infinite;
}

@keyframes todo-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.icon-sm {
  width: 15px;
  height: 15px;
}

.icon-xs {
  width: 14px;
  height: 14px;
}
</style>
