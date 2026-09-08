<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  Presentation,
  FileText,
  File,
  Download,
  Copy,
  Check,
  Sparkles,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'
import type { HarnessArtifact } from '../types'

const props = defineProps<{
  artifact: HarnessArtifact
}>()

const copied = ref(false)

const isPpt = computed(() => {
  const t = (props.artifact.file_type || '').toLowerCase()
  return t.includes('ppt')
})

const isDoc = computed(() => {
  const t = (props.artifact.file_type || '').toLowerCase()
  return t.includes('doc')
})

const isDownloading = ref(false)

async function handleDownload() {
  if (!props.artifact.download_url || isDownloading.value) return
  let filename = props.artifact.name || 'document'
  const ext = `.${props.artifact.file_type || 'docx'}`
  if (!filename.toLowerCase().endsWith(ext.toLowerCase())) {
    filename = `${filename}${ext}`
  }

  isDownloading.value = true
  try {
    const res = await fetch(props.artifact.download_url)
    if (!res.ok) throw new Error('下载请求异常')
    const blob = await res.blob()
    const blobUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(blobUrl)
    ElMessage.success(`开始下载：${filename}`)
  } catch (err) {
    // 跨域或异常时兜底降级触发
    const link = document.createElement('a')
    link.href = props.artifact.download_url
    link.download = filename
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } finally {
    isDownloading.value = false
  }
}

async function handleCopyLink() {
  if (!props.artifact.download_url) return
  try {
    await navigator.clipboard.writeText(props.artifact.download_url)
    copied.value = true
    ElMessage.success('已复制下载链接到剪贴板')
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}
</script>

<template>
  <div class="artifact-card" :class="{ 'is-ppt': isPpt, 'is-doc': isDoc }">
    <!-- 顶部极简标记与尺寸 -->
    <div class="artifact-badge-row">
      <span class="type-pill">
        <Sparkles class="icon-tiny" />
        <span>{{ isPpt ? 'PPTX 演示文稿' : isDoc ? 'DOCX 结构化报告' : '产出交付物' }}</span>
      </span>
      <span class="size-text">{{ artifact.size_str }}</span>
    </div>

    <!-- 主体：图标、名称与动作 -->
    <div class="artifact-content">
      <!-- 左侧文件图标 -->
      <div class="file-icon-box">
        <Presentation v-if="isPpt" class="icon-file" />
        <FileText v-else-if="isDoc" class="icon-file" />
        <File v-else class="icon-file" />
      </div>

      <!-- 中间文件名与描述 -->
      <div class="file-details">
        <h4 class="file-title" :title="artifact.name">
          {{ artifact.name }}
        </h4>
        <p class="file-sub">
          云端生产就绪 · 已存储于 MinIO
        </p>
      </div>

      <!-- 右侧操作按钮组 -->
      <div class="btn-group">
        <button
          type="button"
          class="copy-pill-btn"
          title="复制下载直链"
          @click="handleCopyLink"
        >
          <Check v-if="copied" class="icon-xs text-success" />
          <Copy v-else class="icon-xs" />
          <span>{{ copied ? '已复制' : '复制直链' }}</span>
        </button>

        <button
          type="button"
          class="download-pill-btn"
          title="立即下载文件"
          @click="handleDownload"
        >
          <Download class="icon-xs" />
          <span>下载交付件</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.artifact-card {
  margin: 16px 0;
  max-width: 580px;
  border-radius: 20px;
  background: var(--surface, rgba(255, 255, 255, 0.75));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border, rgba(0, 0, 0, 0.08));
  padding: 14px 18px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.04);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  user-select: text;
}

[data-theme="dark"] .artifact-card {
  background: var(--surface, rgba(24, 24, 28, 0.85));
  border-color: var(--border, rgba(255, 255, 255, 0.08));
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
}

.artifact-card:hover {
  border-color: var(--border-focus, rgba(222, 67, 49, 0.3));
  box-shadow: 0 10px 32px rgba(222, 67, 49, 0.08);
  transform: translateY(-1px);
}

.artifact-badge-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.type-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-full, 9999px);
  background: var(--accent-soft, rgba(222, 67, 49, 0.08));
  color: var(--accent, #DE4331);
}

.artifact-card.is-doc .type-pill {
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
}

[data-theme="dark"] .artifact-card.is-doc .type-pill {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.size-text {
  font-size: 11.5px;
  color: var(--ink-muted, #8A7A6A);
}

.artifact-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.file-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--accent-soft, rgba(222, 67, 49, 0.1));
  color: var(--accent, #DE4331);
  transition: transform 0.2s ease;
}

.artifact-card.is-doc .file-icon-box {
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
}

[data-theme="dark"] .file-icon-box {
  background: rgba(222, 67, 49, 0.2);
  color: #ff7865;
}

[data-theme="dark"] .artifact-card.is-doc .file-icon-box {
  background: rgba(59, 130, 246, 0.2);
  color: #93c5fd;
}

.artifact-card:hover .file-icon-box {
  transform: scale(1.05);
}

.icon-file {
  width: 22px;
  height: 22px;
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink, #1A1410);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}

[data-theme="dark"] .file-title {
  color: #f4f4f5;
}

.file-sub {
  font-size: 12px;
  color: var(--ink-muted, #8A7A6A);
  margin: 3px 0 0 0;
}

.btn-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.copy-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: var(--radius-full, 9999px);
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-soft, #5C4D3D);
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
  border: 1px solid var(--border, rgba(0, 0, 0, 0.08));
  cursor: pointer;
  transition: all 0.2s ease;
}

.copy-pill-btn:hover {
  background: rgba(0, 0, 0, 0.08);
  color: var(--ink, #1A1410);
  border-color: var(--border-interactive, rgba(0, 0, 0, 0.15));
}

[data-theme="dark"] .copy-pill-btn {
  color: #a1a1aa;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.08);
}

[data-theme="dark"] .copy-pill-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
}

.download-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 15px;
  border-radius: var(--radius-full, 9999px);
  font-size: 12.5px;
  font-weight: 600;
  color: #ffffff;
  background: var(--accent-gradient, linear-gradient(135deg, #DE4331, #FCC841));
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(222, 67, 49, 0.28);
}

.download-pill-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(222, 67, 49, 0.42);
  filter: brightness(1.04);
}

.icon-tiny {
  width: 12px;
  height: 12px;
}

.icon-xs {
  width: 14px;
  height: 14px;
}

.text-success {
  color: #10b981;
}
</style>
