<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  Plus,
  Pin,
  Trash2,
  Edit2,
  Check,
  X,
  MessageSquare,
  PanelLeftClose,
  Settings,
} from '@lucide/vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import type { HarnessSession } from '../types'

const props = defineProps<{
  sessions: HarnessSession[]
  currentSessionId: number | null
  isOpen: boolean
}>()

const emit = defineEmits<{
  selectSession: [sessionId: number]
  newSession: []
  deleteSession: [sessionId: number]
  updateSession: [sessionId: number, payload: { title?: string; pinned?: boolean }]
  toggleSidebar: []
  openSettings: []
}>()

// 重命名状态
const editingSessionId = ref<number | null>(null)
const editingTitle = ref('')

function startRename(session: HarnessSession, e: Event) {
  e.stopPropagation()
  editingSessionId.value = session.id
  editingTitle.value = session.title
}

function saveRename(sessionId: number, e: Event) {
  e.stopPropagation()
  const trimmed = editingTitle.value.trim()
  if (trimmed) {
    emit('updateSession', sessionId, { title: trimmed })
  }
  editingSessionId.value = null
}

function cancelRename(e: Event) {
  e.stopPropagation()
  editingSessionId.value = null
}

// 置顶切换
function togglePin(session: HarnessSession, e: Event) {
  e.stopPropagation()
  emit('updateSession', session.id, { pinned: !session.pinned })
}

// 删除确认弹窗状态 (严格调用 ConfirmModal.vue)
const showDeleteConfirm = ref(false)
const sessionToDelete = ref<HarnessSession | null>(null)
const isDeleting = ref(false)

function askDelete(session: HarnessSession, e: Event) {
  e.stopPropagation()
  sessionToDelete.value = session
  showDeleteConfirm.value = true
}

function confirmDelete() {
  if (!sessionToDelete.value) return
  isDeleting.value = true
  emit('deleteSession', sessionToDelete.value.id)
  isDeleting.value = false
  showDeleteConfirm.value = false
  sessionToDelete.value = null
}

function cancelDelete() {
  showDeleteConfirm.value = false
  sessionToDelete.value = null
}

// 分组：置顶与其他
const pinnedSessions = computed(() => props.sessions.filter((s) => s.pinned))
const regularSessions = computed(() => props.sessions.filter((s) => !s.pinned))
</script>

<template>
  <aside class="harness-sidebar">
    <!-- 顶部：新建按钮与收起按钮 -->
    <div class="sidebar-top">
      <button
        type="button"
        class="create-session-btn"
        @click="emit('newSession')"
      >
        <Plus class="icon-sm" />
        <span>开启新会话</span>
      </button>

      <button
        type="button"
        class="collapse-btn"
        title="系统与 AI 助手设置"
        @click="emit('openSettings')"
      >
        <Settings class="icon-sm" />
      </button>

      <button
        type="button"
        class="collapse-btn"
        title="收起会话列表"
        @click="emit('toggleSidebar')"
      >
        <PanelLeftClose class="icon-sm" />
      </button>
    </div>

    <!-- 会话列表滚动区 -->
    <div class="sidebar-scroll">
      <!-- 置顶会话组 -->
      <div v-if="pinnedSessions.length > 0" class="session-group">
        <div class="group-title">
          <Pin class="icon-xs text-primary" />
          <span>置顶会话</span>
        </div>
        <div class="session-list">
          <div
            v-for="s in pinnedSessions"
            :key="s.id"
            class="session-item"
            :class="{ active: s.id === currentSessionId }"
            @click="emit('selectSession', s.id)"
          >
            <!-- 正在内联编辑标题 -->
            <div v-if="editingSessionId === s.id" class="rename-box" @click.stop>
              <input
                v-model="editingTitle"
                type="text"
                class="rename-input"
                @keydown.enter="saveRename(s.id, $event)"
              />
              <button class="action-btn text-success" @click="saveRename(s.id, $event)">
                <Check class="icon-xs" />
              </button>
              <button class="action-btn text-danger" @click="cancelRename($event)">
                <X class="icon-xs" />
              </button>
            </div>

            <!-- 常规展示 -->
            <template v-else>
              <div class="session-label">
                <MessageSquare class="icon-xs muted" />
                <span class="session-text">{{ s.title }}</span>
              </div>

              <!-- 悬浮操作图标组 -->
              <div class="hover-actions">
                <button
                  type="button"
                  class="action-btn"
                  title="取消置顶"
                  @click="togglePin(s, $event)"
                >
                  <Pin class="icon-xs pinned" />
                </button>
                <button
                  type="button"
                  class="action-btn"
                  title="重命名"
                  @click="startRename(s, $event)"
                >
                  <Edit2 class="icon-xs" />
                </button>
                <button
                  type="button"
                  class="action-btn text-danger-hover"
                  title="删除会话"
                  @click="askDelete(s, $event)"
                >
                  <Trash2 class="icon-xs" />
                </button>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 全部/近期历史会话组 -->
      <div class="session-group">
        <div class="group-title">
          <span>全部会话</span>
        </div>
        <div class="session-list">
          <div
            v-for="s in regularSessions"
            :key="s.id"
            class="session-item"
            :class="{ active: s.id === currentSessionId }"
            @click="emit('selectSession', s.id)"
          >
            <!-- 正在内联编辑标题 -->
            <div v-if="editingSessionId === s.id" class="rename-box" @click.stop>
              <input
                v-model="editingTitle"
                type="text"
                class="rename-input"
                @keydown.enter="saveRename(s.id, $event)"
              />
              <button class="action-btn text-success" @click="saveRename(s.id, $event)">
                <Check class="icon-xs" />
              </button>
              <button class="action-btn text-danger" @click="cancelRename($event)">
                <X class="icon-xs" />
              </button>
            </div>

            <!-- 常规展示 -->
            <template v-else>
              <div class="session-label">
                <MessageSquare class="icon-xs muted" />
                <span class="session-text">{{ s.title }}</span>
              </div>

              <!-- 悬浮操作图标组 -->
              <div class="hover-actions">
                <button
                  type="button"
                  class="action-btn"
                  title="置顶会话"
                  @click="togglePin(s, $event)"
                >
                  <Pin class="icon-xs" />
                </button>
                <button
                  type="button"
                  class="action-btn"
                  title="重命名"
                  @click="startRename(s, $event)"
                >
                  <Edit2 class="icon-xs" />
                </button>
                <button
                  type="button"
                  class="action-btn text-danger-hover"
                  title="删除会话"
                  @click="askDelete(s, $event)"
                >
                  <Trash2 class="icon-xs" />
                </button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部删除确认弹窗 (复用通用 ConfirmModal.vue) -->
    <ConfirmModal
      :show="showDeleteConfirm"
      title="删除会话"
      :message="`确定要删除会话 '${sessionToDelete?.title}' 吗？该操作将同时清理该会话产生的所有中间思考与交付记录。`"
      confirm-text="确认删除"
      cancel-text="取消"
      tone="danger"
      :busy="isDeleting"
      @confirm="confirmDelete"
      @cancel="cancelDelete"
    />
  </aside>
</template>

<style scoped>
.harness-sidebar {
  width: 270px;
  height: 100%;
  background: var(--surface, rgba(255, 255, 255, 0.75));
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 24px;
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  overflow: hidden;
  position: relative;
  z-index: 20;
}

[data-theme="dark"] .harness-sidebar {
  border-color: var(--border, rgba(255, 255, 255, 0.08));
  background: var(--surface, rgba(20, 20, 24, 0.85));
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.35);
}

.sidebar-top {
  padding: 14px 14px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--border, rgba(0, 0, 0, 0.06));
}

[data-theme="dark"] .sidebar-top {
  border-bottom: 1px solid var(--border, rgba(255, 255, 255, 0.06));
}

.create-session-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-full, 9999px);
  font-size: 13px;
  font-weight: 600;
  background: var(--accent-gradient, linear-gradient(135deg, #DE4331, #FCC841));
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(222, 67, 49, 0.22);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.create-session-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(222, 67, 49, 0.35);
}

[data-theme="dark"] .create-session-btn {
  background: var(--accent-gradient, linear-gradient(135deg, #DE4331, #FCC841));
  color: #ffffff;
}

.collapse-btn {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-full, 9999px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-muted, #8A7A6A);
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
  border: 1px solid var(--border, rgba(0, 0, 0, 0.08));
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.collapse-btn:hover {
  color: var(--ink, #1A1410);
  background: rgba(0, 0, 0, 0.08);
  border-color: var(--border-interactive, rgba(0, 0, 0, 0.15));
}

[data-theme="dark"] .collapse-btn {
  color: #a1a1aa;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.08);
}

[data-theme="dark"] .collapse-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
}

.sidebar-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 10px 8px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.15) transparent;
}

.sidebar-scroll::-webkit-scrollbar {
  width: 5px;
}

.sidebar-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 9999px;
}

.sidebar-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

[data-theme="dark"] .sidebar-scroll {
  scrollbar-color: rgba(255, 255, 255, 0.18) transparent;
}

[data-theme="dark"] .sidebar-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
}

.session-group {
  margin-bottom: 16px;
}

.group-title {
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 600;
  color: var(--ink-muted, #a1a1aa);
  display: flex;
  align-items: center;
  gap: 4px;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 4px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: var(--radius-md, 10px);
  font-size: 13px;
  color: var(--ink-soft, #52525b);
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

[data-theme="dark"] .session-item {
  color: var(--ink-muted, #a1a1aa);
}

.session-item:hover {
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
  color: var(--ink, #18181b);
}

[data-theme="dark"] .session-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #f4f4f5;
}

.session-item.active {
  background: var(--accent-soft, rgba(222, 67, 49, 0.08));
  color: var(--accent, #DE4331);
  font-weight: 600;
}

[data-theme="dark"] .session-item.active {
  background: var(--accent-soft, rgba(222, 67, 49, 0.15));
  color: var(--accent, #DE4331);
}

.session-label {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.session-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hover-actions {
  display: none;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.session-item:hover .hover-actions {
  display: flex;
}

.action-btn {
  padding: 3px;
  border-radius: 4px;
  color: #71717a;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #18181b;
}

[data-theme="dark"] .action-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.text-danger-hover:hover {
  color: #ef4444 !important;
}

.text-success {
  color: #10b981;
}

.text-danger {
  color: #ef4444;
}

.rename-box {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
}

.rename-input {
  flex: 1;
  background: #ffffff;
  border: 1px solid #d4d4d8;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 12px;
  outline: none;
}

[data-theme="dark"] .rename-input {
  background: #27272a;
  border-color: #3f3f46;
  color: #fff;
}

.icon-sm {
  width: 14px;
  height: 14px;
}

.icon-xs {
  width: 13px;
  height: 13px;
}

.muted {
  opacity: 0.65;
}

.pinned {
  color: #2563eb;
  fill: currentColor;
}
</style>
