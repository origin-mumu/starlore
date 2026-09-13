<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  Presentation,
  FileText,
  File,
  Download,
  Loader2,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'
import type { HarnessArtifact } from '../types'

const props = defineProps<{
  artifact: HarnessArtifact
}>()

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

</script>

<template>
  <div class="artifact-card" :class="{ 'is-ppt': isPpt, 'is-doc': isDoc }">
    <!-- 左侧文件图标 -->
    <div class="file-icon-box">
      <Presentation v-if="isPpt" class="icon-file" />
      <FileText v-else-if="isDoc" class="icon-file" />
      <File v-else class="icon-file" />
    </div>

    <!-- 中间文件名与文件大小 -->
    <div class="file-details">
      <h4 class="file-title" :title="artifact.name">
        {{ artifact.name }}
      </h4>
      <span v-if="artifact.size_str" class="file-size-tag">
        {{ artifact.size_str }}
      </span>
    </div>

    <!-- 右侧单一操作：下载按钮 -->
    <button
      type="button"
      class="download-pill-btn"
      title="立即下载文件"
      :disabled="isDownloading"
      @click="handleDownload"
    >
      <Loader2 v-if="isDownloading" class="icon-xs spin" />
      <Download v-else class="icon-xs" />
      <span>{{ isDownloading ? '下载中' : '下载' }}</span>
    </button>
  </div>
</template>

<style scoped>
.artifact-card {
  margin: 12px 0 6px 0;
  max-width: 480px;
  border-radius: 16px;
  background: var(--surface, rgba(255, 255, 255, 0.75));
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--border, rgba(0, 0, 0, 0.08));
  padding: 10px 14px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 12px;
  user-select: text;
}

[data-theme="dark"] .artifact-card {
  background: var(--surface, rgba(24, 24, 28, 0.85));
  border-color: var(--border, rgba(255, 255, 255, 0.08));
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.3);
}

.artifact-card:hover {
  border-color: var(--border-focus, rgba(222, 67, 49, 0.3));
  box-shadow: 0 8px 24px rgba(222, 67, 49, 0.08);
  transform: translateY(-1px);
}

.file-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 12px;
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
  width: 20px;
  height: 20px;
}

.file-details {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.file-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink, #1A1410);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.35;
}

[data-theme="dark"] .file-title {
  color: #f4f4f5;
}

.file-size-tag {
  font-size: 11.5px;
  color: var(--ink-muted, #8A7A6A);
  font-weight: 500;
}

[data-theme="dark"] .file-size-tag {
  color: #a1a1aa;
}

.download-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: var(--radius-full, 9999px);
  font-size: 12.5px;
  font-weight: 600;
  color: #ffffff;
  background: var(--accent-gradient, linear-gradient(135deg, #DE4331, #FCC841));
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(222, 67, 49, 0.25);
  flex-shrink: 0;
}

.download-pill-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(222, 67, 49, 0.4);
  filter: brightness(1.04);
}

.download-pill-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.icon-xs {
  width: 14px;
  height: 14px;
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
