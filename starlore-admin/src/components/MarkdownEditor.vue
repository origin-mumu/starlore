<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { Bold, Code2, Eye, Heading2, Image, Italic, Link, List, Quote } from 'lucide-vue-next'
import { uploadImage } from '@/api/article'

const model = defineModel<string>({ required: true, default: '' })
const host = ref<HTMLElement>()
const imageInput = ref<HTMLInputElement>()
let editor: Vditor | null = null
let internalUpdate = false
const sourceMode = ref(false)
const imageUploading = ref(false)

function insertMarkdown(before: string, after = '', placeholder = '文字') {
  if (!editor) return
  const selection = editor.getSelection()
  if (selection) editor.deleteValue()
  editor.insertMD(`${before}${selection || placeholder}${after}`)
  editor.focus()
}

function insertLinePrefix(prefix: string, placeholder: string) {
  if (!editor) return
  const selection = editor.getSelection()
  if (selection) editor.deleteValue()
  const content = (selection || placeholder).split('\n').map(line => `${prefix}${line}`).join('\n')
  editor.insertMD(content)
  editor.focus()
}

async function handleImage(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !editor) return
  imageUploading.value = true
  try {
    const response: any = await uploadImage(file)
    const url = response.data?.url || response.url
    if (url) editor.insertMD(`![${file.name}](${url})`)
  } finally {
    imageUploading.value = false
    input.value = ''
  }
}

async function createEditor() {
  await nextTick()
  if (!host.value) return

  editor = new Vditor(host.value, {
    value: model.value,
    mode: sourceMode.value ? 'sv' : 'wysiwyg',
    lang: 'zh_CN',
    height: 520,
    minHeight: 520,
    cache: { enable: false },
    placeholder: '请输入文章正文内容（支持 Markdown 语法与所见即所得编辑）…',
    toolbar: [],
    toolbarConfig: { hide: true },
    counter: { enable: false },
    preview: {
      mode: 'editor',
      markdown: { sanitize: true, codeBlockPreview: true, mathBlockPreview: true },
      hljs: { enable: true, lineNumber: false, style: 'github' },
    },
    input: value => {
      internalUpdate = true
      model.value = value
      nextTick(() => { internalUpdate = false })
    },
  })
}

async function toggleMode() {
  const value = editor?.getValue() ?? model.value
  editor?.destroy()
  editor = null
  model.value = value
  sourceMode.value = !sourceMode.value
  await createEditor()
}

onMounted(async () => {
  await createEditor()
})

watch(model, value => {
  if (!editor || internalUpdate || editor.getValue() === value) return
  editor.setValue(value, true)
})

onBeforeUnmount(() => {
  editor?.destroy()
  editor = null
})
</script>

<template>
  <div class="markdown-shell">
    <div class="markdown-shell__bar">
      <div class="markdown-tools" role="toolbar" aria-label="正文格式工具栏">
        <button type="button" title="二级标题" aria-label="二级标题" @click="insertLinePrefix('## ', '标题')"><Heading2 /></button>
        <button type="button" title="加粗" aria-label="加粗" @click="insertMarkdown('**', '**', '加粗文字')"><Bold /></button>
        <button type="button" title="斜体" aria-label="斜体" @click="insertMarkdown('*', '*', '斜体文字')"><Italic /></button>
        <button type="button" title="引用" aria-label="引用" @click="insertLinePrefix('> ', '引用内容')"><Quote /></button>
        <button type="button" title="无序列表" aria-label="无序列表" @click="insertLinePrefix('- ', '列表项')"><List /></button>
        <button type="button" title="链接" aria-label="链接" @click="insertMarkdown('[', '](https://)', '链接文字')"><Link /></button>
        <button type="button" title="上传图片" aria-label="上传图片" :disabled="imageUploading" @click="imageInput?.click()"><Image /></button>
        <button type="button" title="行内代码" aria-label="行内代码" @click="insertMarkdown('`', '`', '代码')"><Code2 /></button>
        <input ref="imageInput" type="file" accept="image/*" hidden @change="handleImage" />
      </div>
      <button type="button" class="mode-toggle" @click="toggleMode">
        <Eye v-if="sourceMode" />
        <Code2 v-else />
        <span>{{ sourceMode ? '返回所见即所得' : '查看源码' }}</span>
      </button>
    </div>
    <div ref="host" class="starlore-markdown-editor" aria-label="文章正文 Markdown 编辑器"></div>
  </div>
</template>

<style>
.markdown-shell {
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--border-soft, #e6ecf5);
  border-radius: var(--radius-sm, 12px);
  background: var(--surface-solid, #ffffff);
  box-shadow: var(--shadow-sm);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.markdown-shell:focus-within {
  border-color: var(--brand-primary, #4f6ef7);
  box-shadow: 0 0 0 2px var(--brand-subtle, rgba(79, 110, 247, 0.12));
}
.markdown-shell__bar {
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--border-soft, #e6ecf5);
  background: var(--surface-solid, #ffffff);
  font-family: var(--font-family-base, system-ui, sans-serif);
}
.markdown-tools {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}
.markdown-tools::-webkit-scrollbar {
  display: none;
}
.markdown-tools button {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  color: var(--text-secondary, #4a5a75);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}
.markdown-tools button:hover {
  color: var(--brand-primary, #4f6ef7);
  background: var(--brand-subtle, rgba(79, 110, 247, 0.08));
}
.markdown-tools button:disabled {
  opacity: 0.4;
  cursor: wait;
}
.markdown-shell__bar .mode-toggle {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  border: 1px solid var(--border-soft, #e6ecf5);
  border-radius: 20px;
  color: var(--text-secondary, #4a5a75);
  background: var(--surface-solid, #ffffff);
  font: 500 13px/1 var(--font-family-base, system-ui, sans-serif);
  cursor: pointer;
  transition: all 0.2s ease;
}
.markdown-shell__bar .mode-toggle:hover {
  color: var(--brand-primary, #4f6ef7);
  border-color: var(--brand-primary, #4f6ef7);
  background: var(--brand-subtle, rgba(79, 110, 247, 0.08));
}
.markdown-shell__bar button:focus-visible {
  outline: 2px solid var(--brand-primary, #4f6ef7);
  outline-offset: 2px;
}
.markdown-shell__bar svg {
  width: 16px;
  height: 16px;
}
.starlore-markdown-editor.vditor {
  --panel-background-color: var(--surface-solid, #ffffff);
  --toolbar-background-color: var(--surface-solid, #ffffff);
  --toolbar-icon-color: var(--text-secondary, #4a5a75);
  --toolbar-icon-hover-color: var(--brand-primary, #4f6ef7);
  --toolbar-icon-hover-background-color: var(--brand-subtle, rgba(79, 110, 247, 0.08));
  --textarea-background-color: transparent;
  --textarea-text-color: var(--text-primary, #172033);
  --border-color: var(--border-soft, #e6ecf5);
  border: 0;
  border-radius: 0;
  overflow: hidden;
  background: var(--surface-solid, #ffffff);
  color: var(--text-primary, #172033);
  font-family: var(--font-family-base, system-ui, sans-serif);
}
.starlore-markdown-editor .vditor-toolbar,
.starlore-markdown-editor .vditor-toolbar--hide {
  display: none !important;
  height: 0 !important;
  min-height: 0 !important;
  padding: 0 !important;
  border: 0 !important;
}
.starlore-markdown-editor .vditor-content {
  height: 520px !important;
  min-height: 520px;
  max-height: 520px;
  overflow: hidden;
}
.starlore-markdown-editor .vditor-wysiwyg,
.starlore-markdown-editor .vditor-ir,
.starlore-markdown-editor .vditor-sv {
  height: 520px !important;
  min-height: 520px !important;
  max-height: 520px !important;
  overflow-y: auto !important;
  scrollbar-width: thin;
  scrollbar-color: var(--brand-subtle, rgba(79, 110, 247, 0.2)) transparent;
}
.starlore-markdown-editor .vditor-reset {
  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;
  overflow-y: auto !important;
  scrollbar-width: thin;
  scrollbar-color: var(--brand-subtle, rgba(79, 110, 247, 0.2)) transparent;
}
.starlore-markdown-editor .vditor-reset::-webkit-scrollbar,
.starlore-markdown-editor .vditor-wysiwyg::-webkit-scrollbar,
.starlore-markdown-editor .vditor-ir::-webkit-scrollbar,
.starlore-markdown-editor .vditor-sv::-webkit-scrollbar {
  width: 6px;
}
.starlore-markdown-editor .vditor-reset::-webkit-scrollbar-track,
.starlore-markdown-editor .vditor-wysiwyg::-webkit-scrollbar-track,
.starlore-markdown-editor .vditor-ir::-webkit-scrollbar-track,
.starlore-markdown-editor .vditor-sv::-webkit-scrollbar-track {
  background: transparent;
}
.starlore-markdown-editor .vditor-reset::-webkit-scrollbar-thumb,
.starlore-markdown-editor .vditor-wysiwyg::-webkit-scrollbar-thumb,
.starlore-markdown-editor .vditor-ir::-webkit-scrollbar-thumb,
.starlore-markdown-editor .vditor-sv::-webkit-scrollbar-thumb {
  background: var(--border-strong, #d5deeb);
  border-radius: 6px;
}
.starlore-markdown-editor .vditor-reset,
.starlore-markdown-editor .vditor-wysiwyg,
.starlore-markdown-editor .vditor-ir,
.starlore-markdown-editor .vditor-sv {
  color: var(--text-primary, #172033);
  font-family: var(--font-family-base, system-ui, sans-serif);
  font-size: 15px;
  line-height: 1.8;
  padding: 16px 20px;
  max-width: none;
  margin: 0;
}
.starlore-markdown-editor .vditor-reset > :first-child {
  margin-top: 0 !important;
}
.starlore-markdown-editor .vditor-reset > :last-child {
  margin-bottom: 0 !important;
}
.starlore-markdown-editor .vditor-wysiwyg,
.starlore-markdown-editor .vditor-ir {
  padding: 0 !important;
}
.starlore-markdown-editor .vditor-reset h1,
.starlore-markdown-editor .vditor-reset h2,
.starlore-markdown-editor .vditor-reset h3 {
  margin-top: 1em;
  margin-bottom: 0.45em;
  color: var(--text-primary, #172033);
}
.starlore-markdown-editor .vditor-reset p,
.starlore-markdown-editor .vditor-reset ul,
.starlore-markdown-editor .vditor-reset ol,
.starlore-markdown-editor .vditor-reset blockquote {
  margin-top: 0.45em;
  margin-bottom: 0.65em;
}
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h1::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h2::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h3::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h4::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h5::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h6::before {
  display: none !important;
  content: none !important;
}
.starlore-markdown-editor .vditor-wysiwyg h1,
.starlore-markdown-editor .vditor-wysiwyg h2,
.starlore-markdown-editor .vditor-wysiwyg h3,
.starlore-markdown-editor .vditor-wysiwyg h4,
.starlore-markdown-editor .vditor-wysiwyg h5,
.starlore-markdown-editor .vditor-wysiwyg h6 {
  margin-left: 0 !important;
}
.starlore-markdown-editor .vditor-sv {
  font-family: var(--font-family-mono, monospace);
  font-size: 14px;
}
.starlore-markdown-editor .vditor-panel,
.starlore-markdown-editor .vditor-hint {
  background: var(--surface-solid, #ffffff);
  color: var(--text-primary, #172033);
  border-color: var(--border-soft, #e6ecf5);
  box-shadow: var(--shadow-md);
}
</style>
