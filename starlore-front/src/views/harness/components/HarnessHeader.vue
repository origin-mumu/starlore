<script setup lang="ts">
import { PanelLeft, Plus } from '@lucide/vue'

defineProps<{
  title: string
  isSidebarOpen: boolean
}>()

const emit = defineEmits<{
  toggleSidebar: []
  newSession: []
}>()
</script>

<template>
  <header class="harness-header">
    <!-- 左侧：折叠侧栏与会话标题 -->
    <div class="header-left">
      <button
        type="button"
        class="icon-btn"
        :title="isSidebarOpen ? '收起侧边栏' : '展开侧边栏'"
        @click="emit('toggleSidebar')"
      >
        <PanelLeft class="icon" />
      </button>

      <h1 class="header-title" :title="title || '新会话'">
        {{ title || '新会话' }}
      </h1>
    </div>

    <!-- 中间：为全局悬浮导航栏留出的清空区域，彻底避免元素碰撞重叠 -->
    <div class="header-center-spacer" aria-hidden="true"></div>

    <!-- 右侧：新建会话快捷按钮 -->
    <div class="header-right">
      <button
        type="button"
        class="new-chat-btn"
        title="开启新会话"
        @click="emit('newSession')"
      >
        <Plus class="icon-sm" />
        <span>新会话</span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.harness-header {
  height: 60px;
  border-bottom: 1px solid var(--border, rgba(0, 0, 0, 0.08));
  background: var(--surface, rgba(255, 255, 255, 0.65));
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
  user-select: none;
  z-index: 10;
}

[data-theme="dark"] .harness-header {
  border-bottom: 1px solid var(--border, rgba(255, 255, 255, 0.08));
  background: var(--surface, rgba(18, 18, 24, 0.75));
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  max-width: 320px;
}

.header-center-spacer {
  flex: 1;
  min-width: 520px;
  pointer-events: none;
}

.icon-btn {
  padding: 7px;
  border-radius: var(--radius-full, 9999px);
  color: var(--ink-soft, #71717a);
  background: var(--surface, transparent);
  border: 1px solid var(--border, rgba(0, 0, 0, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.icon-btn:hover {
  background: var(--surface-hover, rgba(0, 0, 0, 0.06));
  color: var(--ink, #18181b);
  border-color: var(--border-interactive, rgba(0, 0, 0, 0.15));
  transform: translateY(-1px);
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink, #18181b);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: -0.01em;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.new-chat-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-full, 9999px);
  font-size: 13px;
  font-weight: 500;
  color: var(--ink, #18181b);
  background: var(--surface, rgba(255, 255, 255, 0.7));
  border: 1px solid var(--border-interactive, rgba(0, 0, 0, 0.12));
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.new-chat-btn:hover {
  background: var(--surface-hover, rgba(255, 255, 255, 0.95));
  color: var(--accent, #DE4331);
  border-color: var(--accent, #DE4331);
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(222, 67, 49, 0.12);
}

.icon {
  width: 18px;
  height: 18px;
}

.icon-sm {
  width: 14px;
  height: 14px;
}
</style>
