<script setup lang="ts">
import { ref, shallowRef, onBeforeUnmount, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
    minHeight?: number
  }>(),
  {
    modelValue: '',
    placeholder: '请输入内容...',
    minHeight: 200,
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editorRef = shallowRef<any>()
const editorReady = ref(false)

const TOKEN_KEY = 'ro_blog_token'
const token = typeof localStorage !== 'undefined' ? localStorage.getItem(TOKEN_KEY) : null

const editorConfig = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      server: '/api/upload/image',
      fieldName: 'file',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      customInsert(res: any, insertFn: any) {
        const url = res.data?.url
        if (url) insertFn(url)
      },
    },
  },
}

const toolbarConfig = {
  excludeKeys: ['fullScreen'],
}

const handleCreated = (editor: any) => {
  editorRef.value = editor
  editorReady.value = true
  if (props.modelValue) {
    editor.setHtml(props.modelValue)
  }
}

const handleChange = (editorOrHtml: any) => {
  // wangEditor @onChange 第一个参数可能是编辑器实例或 HTML 字符串
  const html = typeof editorOrHtml === 'string' ? editorOrHtml : (editorOrHtml?.getHtml?.() ?? '')
  emit('update:modelValue', html)
}

watch(
  () => props.modelValue,
  val => {
    if (typeof val !== 'string') return
    const editor = editorRef.value
    if (editor && val !== editor.getHtml()) {
      editor.setHtml(val)
    }
  }
)

onBeforeUnmount(() => {
  editorReady.value = false
  const editor = editorRef.value
  if (editor) editor.destroy()
})
</script>

<template>
  <div class="rich-editor-wrapper" :style="{ '--editor-height': minHeight + 'px' }">
    <Toolbar v-if="editorReady" :editor="editorRef" :defaultConfig="toolbarConfig" class="we-toolbar" />
    <Editor
      :defaultConfig="editorConfig"
      mode="simple"
      class="we-editor"
      @onCreated="handleCreated"
      @onChange="handleChange"
    />
  </div>
</template>

<style scoped>
.rich-editor-wrapper {
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--surface);
}
.we-toolbar {
  border-bottom: 1px solid var(--border) !important;
  background: var(--canvas) !important;
}
.we-editor {
  min-height: var(--editor-height, 200px);
}
.we-editor :deep(.w-e-text-container) {
  height: var(--editor-height, 200px) !important;
  min-height: var(--editor-height, 200px) !important;
  overflow-y: auto !important;
}
</style>
