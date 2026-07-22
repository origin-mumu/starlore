<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { Bold, Code2, Eye, Heading2, Image, Italic, Link, List, Quote } from '@lucide/vue'
import { uploadImage } from '@/api/article'

const model = defineModel<string>({ required: true })
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
    height: 560,
    minHeight: 560,
    cache: { enable: false },
    placeholder: '开始书写正文…',
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
        {{ sourceMode ? '返回编辑' : '查看源码' }}
      </button>
    </div>
    <div ref="host" class="starlore-markdown-editor" aria-label="文章正文 Markdown 编辑器"></div>
  </div>
</template>

<style>
.markdown-shell {
  overflow: hidden;
  border: 1px solid var(--border-interactive);
  border-radius: 16px;
  background: color-mix(in oklch, var(--surface) 72%, transparent);
  backdrop-filter: blur(20px) saturate(1.15);
  -webkit-backdrop-filter: blur(20px) saturate(1.15);
  box-shadow: inset 0 1px 0 color-mix(in oklch, var(--surface) 75%, transparent);
}
.markdown-shell__bar {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 9px 12px 9px 20px;
  border-bottom: 1px solid var(--border);
  font-family: Inter, 'Noto Sans SC', system-ui, sans-serif;
}
.markdown-tools { display: flex; align-items: center; gap: 2px; min-width: 0; overflow-x: auto; scrollbar-width: none; }
.markdown-tools::-webkit-scrollbar { display: none; }
.markdown-tools button {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 9px;
  color: var(--ink-soft);
  background: transparent;
  cursor: pointer;
}
.markdown-tools button:hover { color: var(--accent); background: var(--accent-soft); }
.markdown-tools button:disabled { opacity: .4; cursor: wait; }
.markdown-shell__bar .mode-toggle {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  color: var(--ink-soft);
  background: var(--surface);
  font: 600 12px/1 Inter, 'Noto Sans SC', system-ui, sans-serif;
  cursor: pointer;
  transition: color .2s var(--ease-out-quart), border-color .2s var(--ease-out-quart), background .2s var(--ease-out-quart);
}
.markdown-shell__bar .mode-toggle:hover { color: var(--accent); border-color: var(--border-focus); background: var(--accent-soft); }
.markdown-shell__bar button:focus-visible { outline: 3px solid color-mix(in oklch, var(--accent) 18%, transparent); outline-offset: 2px; }
.markdown-shell__bar svg { width: 15px; height: 15px; }
.starlore-markdown-editor.vditor {
  --panel-background-color: var(--surface);
  --toolbar-background-color: color-mix(in oklch, var(--surface) 90%, transparent);
  --toolbar-icon-color: var(--ink-soft);
  --toolbar-icon-hover-color: var(--accent);
  --toolbar-icon-hover-background-color: var(--accent-soft);
  --textarea-background-color: transparent;
  --textarea-text-color: var(--ink);
  --border-color: var(--border-interactive);
  border: 0;
  border-radius: 0;
  overflow: hidden;
  background: color-mix(in oklch, var(--surface) 60%, transparent);
  backdrop-filter: blur(16px) saturate(1.1);
  -webkit-backdrop-filter: blur(16px) saturate(1.1);
  color: var(--ink);
  font-family: 'LXGW WenKai', 'Source Serif 4', Georgia, 'Noto Serif SC', serif;
  transition: border-color .2s var(--ease-out-quart), box-shadow .2s var(--ease-out-quart);
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
  height: 560px !important;
  min-height: 560px;
  max-height: 560px;
  overflow: hidden;
}
.starlore-markdown-editor .vditor-wysiwyg,
.starlore-markdown-editor .vditor-ir,
.starlore-markdown-editor .vditor-sv {
  height: 560px !important;
  min-height: 560px !important;
  max-height: 560px !important;
  overflow-y: auto !important;
  scrollbar-width: thin;
  scrollbar-color: color-mix(in oklch, var(--accent) 42%, transparent) transparent;
}
.starlore-markdown-editor .vditor-reset {
  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;
  overflow-y: auto !important;
  scrollbar-width: thin;
  scrollbar-color: color-mix(in oklch, var(--accent) 42%, transparent) transparent;
}
.starlore-markdown-editor .vditor-reset::-webkit-scrollbar { width: 7px; }
.starlore-markdown-editor .vditor-reset::-webkit-scrollbar-track { background: transparent; }
.starlore-markdown-editor .vditor-reset::-webkit-scrollbar-button { display: none; width: 0; height: 0; }
.starlore-markdown-editor .vditor-reset::-webkit-scrollbar-thumb {
  background: color-mix(in oklch, var(--accent) 34%, var(--border));
  border: 2px solid transparent;
  border-radius: var(--radius-full);
  background-clip: padding-box;
}
.starlore-markdown-editor .vditor-wysiwyg::-webkit-scrollbar,
.starlore-markdown-editor .vditor-ir::-webkit-scrollbar,
.starlore-markdown-editor .vditor-sv::-webkit-scrollbar { width: 7px; }
.starlore-markdown-editor .vditor-wysiwyg::-webkit-scrollbar-track,
.starlore-markdown-editor .vditor-ir::-webkit-scrollbar-track,
.starlore-markdown-editor .vditor-sv::-webkit-scrollbar-track { background: transparent; }
.starlore-markdown-editor .vditor-wysiwyg::-webkit-scrollbar-button,
.starlore-markdown-editor .vditor-ir::-webkit-scrollbar-button,
.starlore-markdown-editor .vditor-sv::-webkit-scrollbar-button { display: none; width: 0; height: 0; }
.starlore-markdown-editor .vditor-wysiwyg::-webkit-scrollbar-thumb,
.starlore-markdown-editor .vditor-ir::-webkit-scrollbar-thumb,
.starlore-markdown-editor .vditor-sv::-webkit-scrollbar-thumb {
  background: color-mix(in oklch, var(--accent) 34%, var(--border));
  border: 2px solid transparent;
  border-radius: var(--radius-full);
  background-clip: padding-box;
}
.starlore-markdown-editor .vditor-wysiwyg::-webkit-scrollbar-thumb:hover,
.starlore-markdown-editor .vditor-ir::-webkit-scrollbar-thumb:hover,
.starlore-markdown-editor .vditor-sv::-webkit-scrollbar-thumb:hover {
  background: color-mix(in oklch, var(--accent) 58%, var(--border));
  border: 2px solid transparent;
  background-clip: padding-box;
}
.markdown-shell:focus-within { border-color: color-mix(in oklch, var(--accent) 45%, var(--border)); box-shadow: 0 0 0 2px color-mix(in oklch, var(--accent) 9%, transparent); }
.starlore-markdown-editor .vditor-reset,
.starlore-markdown-editor .vditor-wysiwyg,
.starlore-markdown-editor .vditor-ir,
.starlore-markdown-editor .vditor-sv {
  color: var(--ink);
  font-family: 'LXGW WenKai', 'Source Serif 4', Georgia, 'Noto Serif SC', serif;
  font-size: 16px;
  line-height: 1.85;
  padding: 18px 22px;
  max-width: none;
  margin: 0;
}
.starlore-markdown-editor .vditor-reset > :first-child { margin-top: 0 !important; }
.starlore-markdown-editor .vditor-reset > :last-child { margin-bottom: 0 !important; }
.starlore-markdown-editor .vditor-wysiwyg,
.starlore-markdown-editor .vditor-ir { padding: 0 !important; }
.starlore-markdown-editor .vditor-reset h1,
.starlore-markdown-editor .vditor-reset h2,
.starlore-markdown-editor .vditor-reset h3 { margin-top: 1em; margin-bottom: .45em; }
.starlore-markdown-editor .vditor-reset p,
.starlore-markdown-editor .vditor-reset ul,
.starlore-markdown-editor .vditor-reset ol,
.starlore-markdown-editor .vditor-reset blockquote { margin-top: .45em; margin-bottom: .65em; }
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h1::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h2::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h3::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h4::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h5::before,
.starlore-markdown-editor .vditor-wysiwyg > .vditor-reset > h6::before { display: none !important; content: none !important; }
.starlore-markdown-editor .vditor-wysiwyg h1,
.starlore-markdown-editor .vditor-wysiwyg h2,
.starlore-markdown-editor .vditor-wysiwyg h3,
.starlore-markdown-editor .vditor-wysiwyg h4,
.starlore-markdown-editor .vditor-wysiwyg h5,
.starlore-markdown-editor .vditor-wysiwyg h6 { margin-left: 0 !important; }
.starlore-markdown-editor .vditor-sv {
  font-family: 'Fira Code', Consolas, monospace;
  font-size: 14px;
}
.starlore-markdown-editor .vditor-counter {
  color: var(--ink-muted);
  background: color-mix(in oklch, var(--surface) 92%, transparent);
  border-top: 1px solid var(--border);
}
.starlore-markdown-editor .vditor-panel,
.starlore-markdown-editor .vditor-hint {
  background: var(--surface);
  color: var(--ink);
  border-color: var(--border);
  box-shadow: var(--shadow-card-hover);
}
@media (max-width: 600px) {
  .markdown-shell__bar { gap: 8px; padding-left: 8px; }
  .markdown-tools button { width: 34px; height: 34px; }
  .markdown-shell__bar .mode-toggle { flex: 0 0 auto; padding: 0 10px; }
  .starlore-markdown-editor.vditor { height: 440px !important; min-height: 440px !important; }
  .starlore-markdown-editor.vditor,
  .starlore-markdown-editor .vditor-content,
  .starlore-markdown-editor .vditor-wysiwyg,
  .starlore-markdown-editor .vditor-ir,
  .starlore-markdown-editor .vditor-sv {
    height: 440px !important;
    min-height: 440px !important;
    max-height: 440px !important;
  }
  .starlore-markdown-editor .vditor-reset,
  .starlore-markdown-editor .vditor-wysiwyg,
  .starlore-markdown-editor .vditor-ir,
  .starlore-markdown-editor .vditor-sv { padding: 14px 16px; }
}
</style>
