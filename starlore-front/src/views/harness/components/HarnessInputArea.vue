<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import {
  ArrowUp,
  Square,
  Cpu,
  Building2,
  ChevronDown,
  Check,
  ShieldCheck,
} from '@lucide/vue'
import type { HarnessModelItem } from '../types'

const props = defineProps<{
  models: HarnessModelItem[]
  currentModelId: string
  isRunning: boolean
}>()

const emit = defineEmits<{
  send: [text: string]
  stop: []
  updateModel: [modelId: string]
}>()

const inputText = ref('')
const isVendorOpen = ref(false)
const isModelOpen = ref(false)
const vendorWrapRef = ref<HTMLElement | null>(null)
const modelWrapRef = ref<HTMLElement | null>(null)

// 提取所有唯一厂商列表
const vendors = computed(() => {
  const list: string[] = []
  for (const m of props.models) {
    const v = m.vendor || '通用厂商'
    if (!list.includes(v)) {
      list.push(v)
    }
  }
  return list
})

const selectedVendor = ref('')

// 同步厂商与选中模型
watch(
  [() => props.currentModelId, () => props.models],
  ([curId, allModels]) => {
    if (curId && allModels.length > 0) {
      const found = allModels.find((m) => m.id === curId)
      if (found?.vendor) {
        selectedVendor.value = found.vendor
        return
      }
    }
    if (!selectedVendor.value && vendors.value.length > 0) {
      selectedVendor.value = vendors.value[0]
    }
  },
  { immediate: true }
)

// 当前厂商下的模型：严格截取前五个
const currentVendorModels = computed(() => {
  if (!selectedVendor.value) return []
  const list = props.models.filter(
    (m) => (m.vendor || '通用厂商') === selectedVendor.value
  )
  return list.slice(0, 5)
})

// 当前模型显示名称
const currentModelDisplayName = computed(() => {
  const found = props.models.find((m) => m.id === props.currentModelId)
  if (found) {
    return found.name || found.id
  }
  return props.currentModelId || '选择模型'
})

function toggleVendorMenu() {
  isVendorOpen.value = !isVendorOpen.value
  isModelOpen.value = false
}

function toggleModelMenu() {
  isModelOpen.value = !isModelOpen.value
  isVendorOpen.value = false
}

function selectVendor(v: string) {
  selectedVendor.value = v
  isVendorOpen.value = false
  // 切换厂商后，自动默认选中该厂商前 5 个模型中的首个
  const available = props.models
    .filter((m) => (m.vendor || '通用厂商') === v)
    .slice(0, 5)
  if (available.length > 0) {
    emit('updateModel', available[0].id)
  }
}

function selectModel(mId: string) {
  emit('updateModel', mId)
  isModelOpen.value = false
}

function handleGlobalClick(e: MouseEvent) {
  const target = e.target as Node
  if (vendorWrapRef.value && !vendorWrapRef.value.contains(target)) {
    isVendorOpen.value = false
  }
  if (modelWrapRef.value && !modelWrapRef.value.contains(target)) {
    isModelOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleGlobalClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
})

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleSend() {
  if (props.isRunning) return
  const text = inputText.value.trim()
  if (!text) return
  emit('send', text)
  inputText.value = ''
}

function setInput(text: string) {
  inputText.value = text
}

defineExpose({
  setInput,
})
</script>

<template>
  <div class="input-area-wrapper">
    <div class="input-card">
      <!-- 输入文本域 -->
      <textarea
        v-model="inputText"
        rows="2"
        placeholder="随心输入你的需求，如：检索知识库微服务治理并生成 6 页 PPT 或规范报告..."
        class="input-textarea"
        @keydown="handleKeyDown"
      />

      <!-- 底部控制工具栏 -->
      <div class="input-footer">
        <!-- 左侧：厂商优先联动模型选择器 -->
        <div class="footer-left">
          <!-- 1. 厂商选择胶囊与自定义上拉弹窗 -->
          <div ref="vendorWrapRef" class="custom-select-wrap">
            <button
              type="button"
              class="selector-pill"
              :class="{ active: isVendorOpen }"
              title="切换 AI 厂商"
              @click.stop="toggleVendorMenu"
            >
              <Building2 class="icon-xs pill-icon" />
              <span class="pill-label">{{ selectedVendor || '厂商' }}</span>
              <ChevronDown class="icon-xs arrow-icon" :class="{ 'is-open': isVendorOpen }" />
            </button>

            <!-- 厂商上拉弹窗 -->
            <Transition name="dropdown-pop">
              <div v-if="isVendorOpen" class="custom-popover vendor-popover">
                <div class="popover-header">
                  <span>AI 厂商 (Provider)</span>
                </div>
                <div class="popover-list">
                  <div
                    v-for="v in vendors"
                    :key="v"
                    class="popover-item"
                    :class="{ selected: selectedVendor === v }"
                    @click="selectVendor(v)"
                  >
                    <span class="item-name">{{ v }}</span>
                    <Check v-if="selectedVendor === v" class="icon-xs check-mark" />
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <!-- 2. 模型选择胶囊 (截取前5个) 与自定义上拉弹窗 -->
          <div ref="modelWrapRef" class="custom-select-wrap">
            <button
              type="button"
              class="selector-pill"
              :class="{ active: isModelOpen }"
              :disabled="currentVendorModels.length === 0"
              title="切换模型 (前 5 个)"
              @click.stop="toggleModelMenu"
            >
              <Cpu class="icon-xs pill-icon" />
              <span class="pill-label model-label">{{ currentModelDisplayName }}</span>
              <ChevronDown class="icon-xs arrow-icon" :class="{ 'is-open': isModelOpen }" />
            </button>

            <!-- 模型上拉弹窗 -->
            <Transition name="dropdown-pop">
              <div v-if="isModelOpen" class="custom-popover model-popover">
                <div class="popover-header">
                  <span>{{ selectedVendor }} · 可用模型</span>
                  <span class="popover-badge">Top 5</span>
                </div>
                <div class="popover-list">
                  <div
                    v-for="m in currentVendorModels"
                    :key="m.id"
                    class="popover-item model-item"
                    :class="{ selected: currentModelId === m.id }"
                    @click="selectModel(m.id)"
                  >
                    <div class="model-info-col">
                      <span class="item-name">{{ m.name }}</span>
                      <span class="item-sub-id">{{ m.id }}</span>
                    </div>
                    <Check v-if="currentModelId === m.id" class="icon-xs check-mark" />
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <!-- 完全访问状态胶囊 -->
          <span class="access-pill">
            <ShieldCheck class="icon-tiny" />
            <span>完全访问</span>
          </span>
        </div>

        <!-- 右侧：发送/停止按钮 -->
        <div class="footer-right">
          <!-- 运行中：停止按钮 -->
          <button
            v-if="isRunning"
            type="button"
            class="stop-btn"
            title="停止生成"
            @click="emit('stop')"
          >
            <Square class="icon-xs fill-white" />
          </button>

          <!-- 就绪态：发送按钮 -->
          <button
            v-else
            type="button"
            :disabled="!inputText.trim()"
            class="send-btn"
            :class="{ active: inputText.trim() }"
            title="发送指令 (Enter)"
            @click="handleSend"
          >
            <ArrowUp class="icon-sm" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.input-area-wrapper {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  z-index: 40;
  padding: 0 20px;
  display: flex;
  justify-content: center;
  pointer-events: none;
}

.input-card {
  width: 100%;
  max-width: 780px;
  pointer-events: auto;
  border-radius: 20px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #ffffff;
  padding: 12px 16px;
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.08);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.input-card:focus-within {
  border-color: var(--border-focus, rgba(222, 67, 49, 0.4));
  box-shadow: 0 12px 40px rgba(222, 67, 49, 0.12);
}

[data-theme="dark"] .input-card {
  border-color: rgba(255, 255, 255, 0.1);
  background: #1c1c22;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

[data-theme="dark"] .input-card:focus-within {
  border-color: var(--accent, #DE4331);
}

.input-textarea {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  font-family: inherit;
  color: var(--ink, #1A1410);
  max-height: 140px;
  display: block;
}

.input-textarea::placeholder {
  color: var(--ink-muted, #8A7A6A);
}

[data-theme="dark"] .input-textarea {
  color: #f4f4f5;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  user-select: none;
}

[data-theme="dark"] .input-footer {
  border-top-color: rgba(255, 255, 255, 0.06);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ── 极简胶囊选择器 ── */
.custom-select-wrap {
  position: relative;
}

.selector-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
  border-radius: var(--radius-full, 9999px);
  font-size: 12px;
  font-weight: 550;
  color: var(--ink-soft, #5C4D3D);
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
  border: 1px solid rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.2s ease;
}

.selector-pill:hover,
.selector-pill.active {
  color: var(--ink, #1A1410);
  border-color: rgba(222, 67, 49, 0.3);
  background: var(--surface, #ffffff);
}

[data-theme="dark"] .selector-pill {
  color: #a1a1aa;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.08);
}

[data-theme="dark"] .selector-pill:hover,
[data-theme="dark"] .selector-pill.active {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.2);
}

.pill-icon {
  color: var(--accent, #DE4331);
}

.pill-label {
  max-width: 100px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-label {
  max-width: 140px;
}

.arrow-icon {
  color: var(--ink-muted, #8A7A6A);
  transition: transform 0.2s ease;
}

.arrow-icon.is-open {
  transform: rotate(180deg);
  color: var(--accent, #DE4331);
}

/* ── 自定义上拉浮窗：不透明高质感 ── */
.custom-popover {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 0;
  background: #ffffff !important;
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
  border-radius: 16px;
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.16), 0 4px 12px rgba(0, 0, 0, 0.06);
  padding: 6px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

[data-theme="dark"] .custom-popover {
  background: #1c1c22 !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  box-shadow: 0 16px 44px rgba(0, 0, 0, 0.6);
}

.vendor-popover {
  min-width: 180px;
}

.model-popover {
  min-width: 260px;
  max-width: 320px;
}

.popover-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--ink-muted, #8A7A6A);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  margin-bottom: 4px;
}

.popover-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--radius-full, 9999px);
  background: var(--accent-soft, rgba(222, 67, 49, 0.08));
  color: var(--accent, #DE4331);
}

.popover-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 240px;
  overflow-y: auto;
}

.popover-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 10px;
  font-size: 12.5px;
  color: var(--ink, #1A1410);
  cursor: pointer;
  transition: all 0.15s ease;
}

.popover-item:hover {
  background: var(--accent-soft, rgba(222, 67, 49, 0.08));
  color: var(--accent, #DE4331);
}

.popover-item.selected {
  font-weight: 600;
  background: var(--accent-soft, rgba(222, 67, 49, 0.1));
  color: var(--accent, #DE4331);
}

[data-theme="dark"] .popover-item {
  color: #e4e4e7;
}

[data-theme="dark"] .popover-item:hover,
[data-theme="dark"] .popover-item.selected {
  background: rgba(222, 67, 49, 0.18);
  color: #ff7865;
}

.model-info-col {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.item-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-sub-id {
  font-size: 10.5px;
  color: var(--ink-muted, #8A7A6A);
  margin-top: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.check-mark {
  color: var(--accent, #DE4331);
  flex-shrink: 0;
}

/* 下拉菜单淡入弹出动效 */
.dropdown-pop-enter-active,
.dropdown-pop-leave-active {
  transition: all 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}

.dropdown-pop-enter-from,
.dropdown-pop-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.97);
}

/* 权限胶囊 */
.access-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: var(--radius-full, 9999px);
  background: rgba(245, 158, 11, 0.1);
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.2);
}

[data-theme="dark"] .access-pill {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border-color: rgba(245, 158, 11, 0.3);
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.send-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #e4e4e7;
  color: #a1a1aa;
  cursor: not-allowed;
  transition: all 0.2s ease;
}

.send-btn.active {
  background: var(--accent-gradient, linear-gradient(135deg, #DE4331, #FCC841));
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(222, 67, 49, 0.25);
}

.send-btn.active:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 14px rgba(222, 67, 49, 0.4);
}

[data-theme="dark"] .send-btn {
  background: rgba(255, 255, 255, 0.08);
  color: #71717a;
}

[data-theme="dark"] .send-btn.active {
  background: var(--accent-gradient, linear-gradient(135deg, #DE4331, #FCC841));
  color: #ffffff;
}

.stop-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #ef4444;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
}

.stop-btn:hover {
  background: #dc2626;
}

.icon-sm {
  width: 16px;
  height: 16px;
}

.icon-xs {
  width: 14px;
  height: 14px;
}

.icon-tiny {
  width: 12px;
  height: 12px;
}

.fill-white {
  fill: currentColor;
}
</style>
