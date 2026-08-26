<script setup lang="ts">
import {
  FileText,
  UploadCloud,
  Plus,
  Search,
  Eye,
  Database,
  Trash2,
} from '@lucide/vue'
import type { KnowledgeDocumentRow } from '@/api/ai'

interface Props {
  filteredDocuments: KnowledgeDocumentRow[]
  docSearch: string
  docsLoading: boolean
  uploadingDoc: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:docSearch', val: string): void
  (e: 'uploadFile', file: File): void
  (e: 'openDocTextModal', doc: KnowledgeDocumentRow): void
  (e: 'openDocumentChunks', payload: { fileName: string; docId: number }): void
  (e: 'deleteDoc', docId: number): void
}>()

function onFileInput(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    emit('uploadFile', file)
  }
  input.value = ''
}

function onSearchInput(e: Event) {
  emit('update:docSearch', (e.target as HTMLInputElement).value)
}
</script>

<template>
  <div class="files-view-layout">
    <!-- 拖拽/点击上传 Banner Card -->
    <div class="upload-dropzone-card ink-glass-card">
      <div class="dropzone-inner">
        <UploadCloud :size="32" class="upload-icon" />
        <h3>上传 Word / PDF / TXT / MD 文件资源</h3>
        <p>系统将自动解析提取纯文本存入数据库，并建立 FAISS 向量切片供 AI 对话召回</p>
        <label class="btn-primary btn-upload-file">
          <Plus :size="14" /> {{ uploadingDoc ? '文件解析中...' : '选择文件上传' }}
          <input type="file" accept=".pdf,.docx,.txt,.md" style="display: none" @change="onFileInput" />
        </label>
      </div>
    </div>

    <!-- 文件列表 Table / Card List -->
    <div class="files-main-card ink-glass-card">
      <div class="files-card-header">
        <h3><FileText :size="18" style="vertical-align: -3px;" /> 已录入文件列表</h3>
        <div class="search-box">
          <Search :size="14" class="search-icon" />
          <input :value="docSearch" placeholder="搜索文件名或文本..." @input="onSearchInput" />
        </div>
      </div>

      <div v-if="docsLoading" class="loading-state">
        <p>正在读取文件列表...</p>
      </div>

      <div v-else-if="filteredDocuments.length" class="doc-table-list">
        <div v-for="doc in filteredDocuments" :key="doc.id" class="doc-row-item">
          <div class="doc-file-info">
            <span class="file-type-tag" :class="doc.fileType">{{ doc.fileType.toUpperCase() }}</span>
            <div class="file-name-meta">
              <h4>{{ doc.fileName }}</h4>
              <span class="file-meta-sub">{{ (doc.fileSize / 1024).toFixed(1) }} KB | {{ doc.createdAt }}</span>
            </div>
          </div>

          <div class="doc-status-badge">
            <span class="status-dot"></span>
            <span>{{ doc.chunkCount }} 个向量切片</span>
          </div>

          <div class="doc-row-actions">
            <button class="doc-action-btn" @click="emit('openDocTextModal', doc)">
              <Eye :size="13" /> 查看/编辑文本
            </button>
            <button class="doc-action-btn" @click="emit('openDocumentChunks', { fileName: doc.fileName, docId: doc.id })">
              <Database :size="13" /> 查看切片
            </button>
            <button class="doc-action-btn danger" @click="emit('deleteDoc', doc.id)">
              <Trash2 :size="13" /> 删除
            </button>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <p>暂无文件，拖拽或点击上方按钮上传 PDF / Word / TXT 文件</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.files-view-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.upload-dropzone-card {
  background: var(--surface);
  border: 2px dashed var(--border-interactive);
  border-radius: var(--radius-lg);
  padding: 36px 20px;
  text-align: center;
}

.dropzone-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  color: var(--accent);
  margin-bottom: 4px;
}

.upload-dropzone-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  margin: 0;
}

.upload-dropzone-card p {
  font-size: 13px;
  color: var(--ink-muted);
  max-width: 500px;
  margin: 0 0 8px;
}

.btn-primary {
  background: var(--ink);
  color: var(--canvas);
  border: none;
  border-radius: var(--radius-full);
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: transform 0.15s;
}
.btn-primary:hover {
  transform: translateY(-1px);
}

.files-main-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.files-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.files-card-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin: 0;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.03));
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  padding: 4px 12px;
}
.search-box input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 12px;
  color: var(--ink);
}
.search-icon {
  color: var(--ink-muted);
}

.doc-table-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doc-row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  gap: 16px;
}

.doc-file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.file-type-tag {
  font-size: 10px;
  font-weight: 700;
  font-family: 'Fira Code', monospace;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--surface);
  border: 1px solid var(--border);
}
.file-type-tag.pdf { color: #ef4444; border-color: rgba(239, 68, 68, 0.3); }
.file-type-tag.docx { color: #3b82f6; border-color: rgba(59, 130, 246, 0.3); }
.file-type-tag.md, .file-type-tag.txt { color: #10b981; border-color: rgba(16, 185, 129, 0.3); }

.file-name-meta h4 {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-meta-sub {
  font-size: 11px;
  color: var(--ink-muted);
}

.doc-status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-soft);
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
}

.doc-row-actions {
  display: flex;
  gap: 8px;
}

.doc-action-btn {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 4px 10px;
  font-size: 11px;
  color: var(--ink-soft);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s;
}
.doc-action-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.doc-action-btn.danger:hover {
  border-color: #ef4444;
  color: #ef4444;
}

.loading-state, .empty-state {
  padding: 32px 16px;
  text-align: center;
  color: var(--ink-muted);
  font-size: 13px;
}
</style>
