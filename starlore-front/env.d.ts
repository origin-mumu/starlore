/// <reference types="vite/client" />

declare module '@wangeditor/editor-for-vue' {
  import { DefineComponent } from 'vue'
  export const Editor: DefineComponent<any, any, any>
  export const Toolbar: DefineComponent<any, any, any>
}

interface Window {
  /** Echobot 页为 true 时，Live2D SDK 忽略鼠标/触摸拖拽视线 */
  __LIVE2D_ECHOBOT_NO_DRAG?: boolean
  /** AI 流式回复时 0~1，每帧在 SDK update 末尾叠加嘴型（避免被 loadParameters 清掉） */
  __ECHOBOT_MOUTH_TARGET?: number
  live2d?: {
    init?: (args?: unknown) => void
    loadModel?: (path: string) => void
    randomExpression?: () => void
    getCanvasBlob?: () => Promise<Blob | null>
    setExpression?: (name: string) => void
    startTapBodyMotion?: () => void
    setMouthOpen?: (value01: number) => void
    resetWaifuDrag?: () => void
  }
}
