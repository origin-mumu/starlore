import { onBeforeUnmount, ref, shallowRef } from 'vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'

interface UploadImageResponse {
  data?: { url?: string }
}

type InsertImageFn = (url: string, alt?: string, href?: string) => void

/** 富文本编辑器生命周期托管（创建 / 销毁 / 图片上传回填） */
export function useArticleEditor() {
  const editorRef = shallowRef<IDomEditor>()
  const editorHtml = ref('')
  const editorMode = ref<'default' | 'simple'>('default')

  const toolbarConfig: Partial<IToolbarConfig> = {}

  const editorConfig: Partial<IEditorConfig> = {
    placeholder: '请输入内容...',
    MENU_CONF: {
      uploadImage: {
        server: '/api/upload/image',
        fieldName: 'file',
        customInsert(res: unknown, insertFn: InsertImageFn) {
          const url = (res as UploadImageResponse | null)?.data?.url
          if (url) insertFn(url)
        },
      },
    },
  }

  const handleCreated = (editor: IDomEditor): void => {
    editorRef.value = editor
  }

  onBeforeUnmount(() => {
    editorRef.value?.destroy()
    editorRef.value = undefined
  })

  return { editorRef, editorHtml, editorMode, toolbarConfig, editorConfig, handleCreated }
}
