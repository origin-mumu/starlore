<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import {
  ArrowUp,
  Square,
  Cpu,
  ChevronDown,
  Check,
  Plus,
  Image,
  FileText,
  FileCode,
  AlignLeft,
  Files,
  X,
  Loader2,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'
import { parseFile } from '@/api/ai'
import type { HarnessModelItem } from '../types'

const props = defineProps<{
  models: HarnessModelItem[]
  currentModelId: string
  isRunning: boolean
}>()

const emit = defineEmits<{
  send: [text: string, images?: string[]]
  stop: []
  updateModel: [modelId: string]
}>()

const inputText = ref('')
const isModelOpen = ref(false)
const isUploadOpen = ref(false)
const modelWrapRef = ref<HTMLElement | null>(null)
const uploadWrapRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const currentAccept = ref('*')

export interface PendingAttachment {
  id: string
  file: File
  name: string
  size: number
  type: 'image' | 'pdf' | 'doc' | 'text' | 'other'
  previewUrl?: string
  extractedText?: string
  status: 'parsing' | 'ready' | 'error'
  error?: string
}

const pendingAttachments = ref<PendingAttachment[]>([])

const uploadOptions = [
  {
    type: 'image',
    name: '图片文件',
    badge: 'JPG, PNG, WebP',
    accept: 'image/png,image/jpeg,image/webp,image/gif',
    icon: Image,
    colorClass: 'item-color-image',
  },
  {
    type: 'pdf',
    name: 'PDF 文档',
    badge: '.pdf',
    accept: '.pdf,application/pdf',
    icon: FileText,
    colorClass: 'item-color-pdf',
  },
  {
    type: 'doc',
    name: 'Word 文档',
    badge: '.doc, .docx',
    accept: '.doc,.docx,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    icon: FileCode,
    colorClass: 'item-color-doc',
  },
  {
    type: 'text',
    name: '文本 / Markdown',
    badge: '.txt, .md',
    accept: '.txt,.md,.markdown,text/plain,text/markdown',
    icon: AlignLeft,
    colorClass: 'item-color-text',
  },
  {
    type: 'all',
    name: '全部支持格式',
    badge: '任意格式',
    accept: 'image/*,.pdf,.doc,.docx,.txt,.md',
    icon: Files,
    colorClass: 'item-color-all',
  },
]

// 当前模型显示名称
const currentModelDisplayName = computed(() => {
  const found = props.models.find((m) => m.id === props.currentModelId)
  if (found) {
    return found.name || found.id
  }
  return props.currentModelId || '选择模型'
})

function toggleModelMenu() {
  isModelOpen.value = !isModelOpen.value
  isUploadOpen.value = false
}

function toggleUploadMenu() {
  isUploadOpen.value = !isUploadOpen.value
  isModelOpen.value = false
}

function selectModel(mId: string) {
  emit('updateModel', mId)
  isModelOpen.value = false
}

function selectUploadType(opt: (typeof uploadOptions)[number]) {
  isUploadOpen.value = false
  currentAccept.value = opt.accept
  nextTick(() => {
    if (fileInputRef.value) {
      fileInputRef.value.accept = opt.accept
      fileInputRef.value.value = ''
      fileInputRef.value.click()
    }
  })
}

async function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    await addPendingFile(file)
  }
  input.value = ''
}

async function addPendingFile(file: File) {
  const name = file.name
  const ext = name.includes('.') ? name.slice(name.lastIndexOf('.')).toLowerCase() : ''
  let fileType: PendingAttachment['type'] = 'other'

  if (file.type.startsWith('image/') || ['.png', '.jpg', '.jpeg', '.webp', '.gif'].includes(ext)) {
    fileType = 'image'
  } else if (ext === '.pdf') {
    fileType = 'pdf'
  } else if (['.doc', '.docx'].includes(ext)) {
    fileType = 'doc'
  } else if (['.txt', '.md', '.markdown'].includes(ext)) {
    fileType = 'text'
  }

  const attachment: PendingAttachment = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    file,
    name,
    size: file.size,
    type: fileType,
    status: 'parsing',
  }
  pendingAttachments.value.push(attachment)
  const target = pendingAttachments.value[pendingAttachments.value.length - 1]

  if (fileType === 'image') {
    const reader = new FileReader()
    reader.onload = () => {
      target.previewUrl = reader.result as string
      target.status = 'ready'
    }
    reader.onerror = () => {
      target.status = 'error'
      target.error = '图片读取失败'
    }
    reader.readAsDataURL(file)
  } else if (fileType === 'text') {
    try {
      const text = await file.text()
      target.extractedText = text
      target.status = 'ready'
    } catch {
      target.status = 'error'
      target.error = '文本读取失败'
    }
  } else {
    try {
      const res = await parseFile(file)
      if (res && res.success && res.text) {
        target.extractedText = res.text
        target.status = 'ready'
      } else {
        target.status = 'ready'
        if (res && res.error) {
          ElMessage.warning(`文档提取提示: ${res.error}`)
        }
      }
    } catch (err: any) {
      console.warn('文档解析失败:', err)
      target.status = 'ready'
    }
  }
}

function removeAttachment(id: string) {
  pendingAttachments.value = pendingAttachments.value.filter((a) => a.id !== id)
}

function formatFileSize(bytes: number): string {
  if (!bytes || bytes <= 0) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function handleGlobalClick(e: MouseEvent) {
  const target = e.target as Node
  if (modelWrapRef.value && !modelWrapRef.value.contains(target)) {
    isModelOpen.value = false
  }
  if (uploadWrapRef.value && !uploadWrapRef.value.contains(target)) {
    isUploadOpen.value = false
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
  if (pendingAttachments.value.some((a) => a.status === 'parsing')) {
    ElMessage.info('附件正在解析中，请稍候...')
    return
  }

  const text = inputText.value.trim()
  if (!text && pendingAttachments.value.length === 0) return

  const imageList: string[] = []
  const docContexts: string[] = []

  for (const att of pendingAttachments.value) {
    if (att.type === 'image' && att.previewUrl) {
      imageList.push(att.previewUrl)
    } else if (att.extractedText) {
      const snippet =
        att.extractedText.length > 30000
          ? att.extractedText.slice(0, 30000) + '\n...(正文过长已截断)'
          : att.extractedText
      docContexts.push(`【附件文档: ${att.name}】\n${snippet}`)
    } else {
      docContexts.push(`【附件文档: ${att.name}】`)
    }
  }

  let fullMessage = text
  if (docContexts.length > 0) {
    const docHeader = docContexts.join('\n\n')
    fullMessage = fullMessage ? `${docHeader}\n\n${fullMessage}` : `${docHeader}\n\n请分析处理上传的文档内容。`
  }

  emit('send', fullMessage, imageList.length > 0 ? imageList : undefined)
  inputText.value = ''
  pendingAttachments.value = []
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
      <!-- 隐藏的文件选择 input -->
      <input
        ref="fileInputRef"
        type="file"
        :accept="currentAccept"
        style="display: none"
        multiple
        @change="onFileSelected"
      />

      <!-- 选中的附件预览横向列表 (ChatGPT 风格：小图横向排布) -->
      <div v-if="pendingAttachments.length > 0" class="attachments-preview-list">
        <div
          v-for="att in pendingAttachments"
          :key="att.id"
          class="attachment-card"
          :class="{
            'is-image': att.type === 'image',
            'is-doc': att.type !== 'image',
            'is-parsing': att.status === 'parsing',
            'is-error': att.status === 'error',
          }"
        >
          <!-- 图片小方卡片展示 (小图横向排布) -->
          <div v-if="att.type === 'image'" class="attachment-image-box">
            <img
              v-if="att.previewUrl"
              :src="att.previewUrl"
              class="attachment-image-thumb"
              :alt="att.name"
            />
            <div v-else class="attachment-image-placeholder">
              <Image class="icon-sm" />
            </div>
            <!-- 解析中半透明遮罩 -->
            <div v-if="att.status === 'parsing'" class="attachment-overlay">
              <Loader2 class="icon-xs spin-icon" />
            </div>
          </div>

          <!-- 文档卡片展示 -->
          <div v-else class="attachment-doc-box">
            <div class="doc-icon-wrap" :class="`doc-${att.type}`">
              <component
                :is="att.type === 'pdf' ? FileText : att.type === 'doc' ? FileCode : AlignLeft"
                class="icon-sm"
              />
            </div>
            <div class="doc-info">
              <span class="doc-name" :title="att.name">{{ att.name }}</span>
              <span class="doc-meta">
                {{ att.status === 'parsing' ? '解析中...' : formatFileSize(att.size) }}
              </span>
            </div>
            <div v-if="att.status === 'parsing'" class="doc-parsing-indicator">
              <Loader2 class="icon-tiny spin-icon" />
            </div>
          </div>

          <!-- 右上角悬浮黑色圆形关闭按钮 (ChatGPT 原生样式) -->
          <button
            type="button"
            class="attachment-close-badge"
            title="移除"
            @click.stop="removeAttachment(att.id)"
          >
            <X class="icon-tiny" />
          </button>
        </div>
      </div>

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
        <!-- 左侧：加号上传按钮 + 厂商优先联动模型选择器 -->
        <div class="footer-left">
          <!-- 加号上传按钮与自定义上拉弹窗 -->
          <div ref="uploadWrapRef" class="custom-select-wrap">
            <button
              type="button"
              class="upload-plus-btn"
              :class="{ active: isUploadOpen }"
              title="上传文件 (图片/PDF/Word/TXT/MD)"
              @click.stop="toggleUploadMenu"
            >
              <Plus class="icon-sm" />
            </button>

            <!-- 文件类型上拉弹窗 -->
            <Transition name="dropdown-pop">
              <div v-if="isUploadOpen" class="custom-popover upload-popover">
                <div class="upload-popover-header">
                  <span>上传附件</span>
                </div>
                <div class="upload-popover-list">
                  <div
                    v-for="opt in uploadOptions"
                    :key="opt.type"
                    class="upload-item"
                    :class="{ 'has-divider': opt.type === 'all' }"
                    @click="selectUploadType(opt)"
                  >
                    <div class="upload-item-icon-box" :class="opt.colorClass">
                      <component :is="opt.icon" class="icon-xs" />
                    </div>
                    <span class="upload-item-label">{{ opt.name }}</span>
                    <span class="upload-item-badge">{{ opt.badge }}</span>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <!-- 模型选择胶囊与自定义上拉弹窗 -->
          <div ref="modelWrapRef" class="custom-select-wrap">
            <button
              type="button"
              class="selector-pill"
              :class="{ active: isModelOpen }"
              :disabled="models.length === 0"
              title="切换模型"
              @click.stop="toggleModelMenu"
            >
              <span class="pill-label model-label">{{ currentModelDisplayName }}</span>
              <ChevronDown class="icon-xs arrow-icon" :class="{ 'is-open': isModelOpen }" />
            </button>

            <!-- 模型上拉弹窗 -->
            <Transition name="dropdown-pop">
              <div v-if="isModelOpen" class="custom-popover model-popover">
                <div class="popover-list">
                  <div
                    v-for="m in models"
                    :key="m.id"
                    class="popover-item model-item"
                    :class="{ selected: currentModelId === m.id }"
                    @click="selectModel(m.id)"
                  >
                    <span class="item-name">{{ m.name || m.id }}</span>
                    <Check v-if="currentModelId === m.id" class="icon-xs check-mark" />
                  </div>
                </div>
              </div>
            </Transition>
          </div>
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
            :disabled="!inputText.trim() && pendingAttachments.length === 0"
            class="send-btn"
            :class="{ active: inputText.trim() || pendingAttachments.length > 0 }"
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
  margin-top: 6px;
  padding-top: 4px;
  user-select: none;
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

.model-popover {
  min-width: 180px;
  max-width: 260px;
  padding: 6px;
}

.popover-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: none;
  overflow: visible;
}

.popover-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  border-radius: 8px;
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

.item-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.check-mark {
  color: var(--accent, #DE4331);
  flex-shrink: 0;
  margin-left: 8px;
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

/* 上传加号按钮 */
.upload-plus-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-full, 9999px);
  color: var(--ink-soft, #5C4D3D);
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
  border: 1px solid rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.upload-plus-btn:hover,
.upload-plus-btn.active {
  color: var(--accent, #DE4331);
  border-color: rgba(222, 67, 49, 0.35);
  background: var(--surface, #ffffff);
  box-shadow: 0 2px 8px rgba(222, 67, 49, 0.12);
  transform: scale(1.04);
}

[data-theme="dark"] .upload-plus-btn {
  color: #a1a1aa;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.08);
}

[data-theme="dark"] .upload-plus-btn:hover,
[data-theme="dark"] .upload-plus-btn.active {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.22);
}

/* ── 上传浮窗全新现代设计 ── */
.upload-popover {
  width: 228px;
  padding: 6px;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.04);
  bottom: calc(100% + 8px);
}

.upload-popover-header {
  padding: 3px 8px 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--ink-muted, #8A7A6A);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  margin-bottom: 3px;
  letter-spacing: 0.2px;
}

[data-theme="dark"] .upload-popover-header {
  border-bottom-color: rgba(255, 255, 255, 0.06);
  color: #a1a1aa;
}

.upload-popover-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.upload-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 8px;
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
}

.upload-item:hover {
  background: var(--surface-hover, rgba(0, 0, 0, 0.05));
}

[data-theme="dark"] .upload-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.upload-item.has-divider {
  margin-top: 3px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  padding-top: 6px;
}

[data-theme="dark"] .upload-item.has-divider {
  border-top-color: rgba(255, 255, 255, 0.06);
}

.upload-item-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 7px;
  flex-shrink: 0;
}

.upload-item-icon-box.item-color-image {
  background: rgba(249, 115, 22, 0.1);
  color: #ea580c;
}
.upload-item-icon-box.item-color-pdf {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}
.upload-item-icon-box.item-color-doc {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}
.upload-item-icon-box.item-color-text {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}
.upload-item-icon-box.item-color-all {
  background: rgba(107, 114, 128, 0.1);
  color: #4b5563;
}

[data-theme="dark"] .upload-item-icon-box.item-color-image {
  background: rgba(249, 115, 22, 0.2);
  color: #fb923c;
}
[data-theme="dark"] .upload-item-icon-box.item-color-pdf {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}
[data-theme="dark"] .upload-item-icon-box.item-color-doc {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
}
[data-theme="dark"] .upload-item-icon-box.item-color-text {
  background: rgba(16, 185, 129, 0.2);
  color: #34d399;
}
[data-theme="dark"] .upload-item-icon-box.item-color-all {
  background: rgba(156, 163, 175, 0.18);
  color: #9ca3af;
}

.upload-item-label {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink, #1A1410);
  white-space: nowrap;
}

[data-theme="dark"] .upload-item-label {
  color: #e4e4e7;
}

.upload-item-badge {
  font-size: 11px;
  color: var(--ink-muted, #8A7A6A);
  margin-left: auto;
  font-family: inherit;
}

[data-theme="dark"] .upload-item-badge {
  color: #71717a;
}

/* ── 附件横向排布区域 (ChatGPT 风格：小图横向排布) ── */
.attachments-preview-list {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow-x: auto;
  padding: 6px 4px 10px 4px;
  margin-bottom: 2px;
  scrollbar-width: thin;
}

.attachments-preview-list::-webkit-scrollbar {
  height: 4px;
}

.attachments-preview-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

[data-theme="dark"] .attachments-preview-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.18);
}

.attachment-card {
  position: relative;
  flex-shrink: 0;
  user-select: none;
}

/* 图片小方卡 (58px x 58px 经典小图排布) */
.attachment-card.is-image .attachment-image-box {
  width: 58px;
  height: 58px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.attachment-card.is-image:hover .attachment-image-box {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

[data-theme="dark"] .attachment-card.is-image .attachment-image-box {
  border-color: rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
}

.attachment-image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.attachment-image-placeholder {
  color: var(--ink-muted, #8A7A6A);
}

.attachment-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
}

/* 文档小胶囊卡 */
.attachment-card.is-doc .attachment-doc-box {
  height: 56px;
  padding: 0 12px;
  border-radius: 12px;
  background: var(--surface, #ffffff);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 200px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.attachment-card.is-doc:hover .attachment-doc-box {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

[data-theme="dark"] .attachment-card.is-doc .attachment-doc-box {
  background: #27272a;
  border-color: rgba(255, 255, 255, 0.1);
}

.doc-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.doc-icon-wrap.doc-pdf {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.doc-icon-wrap.doc-doc {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}

.doc-icon-wrap.doc-text {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.doc-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.doc-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--ink, #1A1410);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

[data-theme="dark"] .doc-name {
  color: #e4e4e7;
}

.doc-meta {
  font-size: 10.5px;
  color: var(--ink-muted, #8A7A6A);
  margin-top: 2px;
}

.doc-parsing-indicator {
  margin-left: auto;
  color: var(--accent, #DE4331);
}

/* 右上角悬浮黑色圆形关闭按钮 (如同 ChatGPT 原生样式) */
.attachment-close-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #18181b;
  color: #ffffff;
  border: 1.5px solid #ffffff;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
  transition: transform 0.15s ease, background 0.15s ease;
  z-index: 5;
}

.attachment-close-badge:hover {
  background: #ef4444;
  transform: scale(1.15);
}

[data-theme="dark"] .attachment-close-badge {
  background: #3f3f46;
  border-color: #18181b;
}

[data-theme="dark"] .attachment-close-badge:hover {
  background: #ef4444;
}

.spin-icon {
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
