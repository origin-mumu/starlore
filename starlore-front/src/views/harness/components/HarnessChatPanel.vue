<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { PanelLeftOpen, Settings, X, Sparkles, Trash2 } from '@lucide/vue'
import HarnessMessageList from './HarnessMessageList.vue'
import HarnessInputArea from './HarnessInputArea.vue'
import HarnessTodoPanel from './HarnessTodoPanel.vue'
import {
  createHarnessSession,
  fetchHarnessMessages,
  fetchHarnessModels,
  fetchHarnessTodos,
  streamHarnessChat,
  updateHarnessSession,
} from '@/api/harness'
import type {
  HarnessMessage,
  HarnessModelItem,
  HarnessTodoItem,
  StreamEventPayload,
} from '../types'

const props = withDefaults(
  defineProps<{
    /** 当前会话 ID（可选，若未传入则自动根据 contextTitle 创建或初始化） */
    sessionId?: number | null
    /** 注入的外部上下文文本（如文章全文、简历结构等） */
    context?: string
    /** 上下文标题（如“星记：《深入浅出...》”或“简历：贾鑫科”） */
    contextTitle?: string
    /** 场景化快捷预设词列表 */
    quickPrompts?: string[]
    /** 是否展示右上角收起/关闭按钮 */
    showClose?: boolean
    /** 是否展示左上角的会话历史与设置按钮（用于全屏 Harness 页面） */
    showTopActions?: boolean
  }>(),
  {
    sessionId: null,
    context: '',
    contextTitle: '',
    quickPrompts: () => [],
    showClose: false,
    showTopActions: false,
  }
)

const emit = defineEmits<{
  (e: 'update:sessionId', id: number): void
  (e: 'update:isRunning', running: boolean): void
  (e: 'close'): void
  (e: 'openSidebar'): void
  (e: 'openSettings'): void
  (e: 'sessionCreated', id: number): void
}>()

// 模型与状态
const models = ref<HarnessModelItem[]>([])
const currentModelId = ref<string>('')
const modelsLoaded = ref(false)
const isRunning = ref(false)
const internalSessionId = ref<number | null>(props.sessionId)

watch(isRunning, (val) => {
  emit('update:isRunning', val)
})

// 消息与流式
const messages = ref<HarnessMessage[]>([])
const todoList = ref<HarnessTodoItem[]>([])
const streamingMessage = ref<HarnessMessage | null>(null)
const activeThinkingStep = ref<number | null>(null)
let abortController: AbortController | null = null

const inputAreaRef = ref<InstanceType<typeof HarnessInputArea> | null>(null)

// 监听外部 sessionId 变化
watch(
  () => props.sessionId,
  async (newId) => {
    if (newId !== internalSessionId.value) {
      internalSessionId.value = newId
      if (newId) {
        await loadSessionMessages(newId)
      } else {
        messages.value = []
        todoList.value = []
      }
    }
  }
)

// 加载指定会话的消息与代办
async function loadSessionMessages(sid: number) {
  try {
    messages.value = await fetchHarnessMessages(sid)
  } catch {
    messages.value = []
  }
  try {
    todoList.value = await fetchHarnessTodos(sid)
  } catch {
    todoList.value = []
  }
}

// 初始化
onMounted(async () => {
  try {
    const fetchedModels = await fetchHarnessModels()
    models.value = fetchedModels
    if (fetchedModels.length > 0) {
      currentModelId.value = fetchedModels[0].id
    }
    modelsLoaded.value = true

    if (internalSessionId.value) {
      await loadSessionMessages(internalSessionId.value)
    } else if (props.context) {
      // 伴生模式：自动创建一个专用会话
      await initContextSession()
    }
  } catch {
    modelsLoaded.value = true
  }
})

// 创建伴生上下文会话
async function initContextSession() {
  try {
    const title = props.contextTitle ? `伴读: ${props.contextTitle.slice(0, 16)}` : 'AI 伴生助手'
    const newSession = await createHarnessSession(title, currentModelId.value)
    internalSessionId.value = newSession.id
    emit('update:sessionId', newSession.id)
    emit('sessionCreated', newSession.id)
    messages.value = []
    todoList.value = []
  } catch (err: any) {
    console.error('初始化伴生会话失败:', err)
  }
}

// 清空当前对话
async function handleClearCurrentSession() {
  if (isRunning.value) return
  messages.value = []
  todoList.value = []
  if (props.context) {
    await initContextSession()
  }
  ElMessage.success('对话已重置')
}

// 发送消息核心逻辑
async function handleSend(userText: string, images?: string[]) {
  if (isRunning.value) return

  // 若尚未初始化 session，则现场创建
  if (!internalSessionId.value) {
    await initContextSession()
    if (!internalSessionId.value) {
      ElMessage.warning('未能创建会话，请稍候重试')
      return
    }
  }

  if (!currentModelId.value && models.value.length > 0) {
    currentModelId.value = models.value[0].id
  }

  // 1. 立即追加用户可见消息
  const userMsg: HarnessMessage = {
    role: 'user',
    content: userText,
    images: images && images.length > 0 ? [...images] : undefined,
  }
  messages.value.push(userMsg)

  // 2. 如果存在外部上下文（如文章或简历），且这是会话的第一轮或包含 context，将上下文拼入传输 payload
  let payloadMessage = userText
  if (props.context && messages.value.length <= 2) {
    payloadMessage = `【当前背景上下文（${props.contextTitle || '当前页面'}）】\n${props.context}\n\n【用户问题/指令】\n${userText}`
  }

  // 3. 初始化流式响应消息
  streamingMessage.value = {
    role: 'assistant',
    content: '',
    reasoning_content: '',
    tool_calls: [],
    artifacts: [],
    step_details: [],
    duration_ms: 0,
  }
  isRunning.value = true
  abortController = new AbortController()
  const startTime = Date.now()
  let currentStepNum = 1
  activeThinkingStep.value = null

  let targetContent = ''
  let typingTimer: number | null = null

  const appendSmoothContent = (delta: string) => {
    targetContent += delta
    if (!typingTimer) {
      typingTimer = window.setInterval(() => {
        if (!streamingMessage.value) {
          clearInterval(typingTimer!)
          typingTimer = null
          return
        }
        const current = streamingMessage.value.content || ''
        if (current.length < targetContent.length) {
          const diff = targetContent.length - current.length
          const step = diff > 80 ? 4 : diff > 30 ? 2 : 1
          streamingMessage.value.content = targetContent.slice(0, current.length + step)
        } else {
          clearInterval(typingTimer!)
          typingTimer = null
        }
      }, 20)
    }
  }

  const flushSmoothContent = () => {
    if (typingTimer) {
      clearInterval(typingTimer)
      typingTimer = null
    }
    if (streamingMessage.value && targetContent) {
      streamingMessage.value.content = targetContent
    }
  }

  try {
    await streamHarnessChat(
      internalSessionId.value,
      { message: payloadMessage, model_id: currentModelId.value, images },
      (ev: StreamEventPayload) => {
        if (!streamingMessage.value) return

        if (!streamingMessage.value.step_details) {
          streamingMessage.value.step_details = []
        }
        const getOrCreateStep = (stepNum: number) => {
          let st = streamingMessage.value!.step_details!.find((s) => s.step === stepNum)
          if (!st) {
            st = {
              step: stepNum,
              title: `步骤 ${stepNum}`,
              reasoning: '',
              scratchpad: '',
              tool_calls: [],
            }
            streamingMessage.value!.step_details!.push(st)
          }
          return st
        }

        if (ev.event === 'step') {
          currentStepNum = ev.data.step || currentStepNum
          activeThinkingStep.value = null
          getOrCreateStep(currentStepNum)
        } else if (ev.event === 'reasoning') {
          const delta = ev.data.delta || ''
          streamingMessage.value.reasoning_content =
            (streamingMessage.value.reasoning_content || '') + delta
          const st = getOrCreateStep(currentStepNum)
          st.reasoning = (st.reasoning || '') + delta
          activeThinkingStep.value = currentStepNum
          streamingMessage.value.duration_ms = Date.now() - startTime
        } else if (ev.event === 'step_thought') {
          activeThinkingStep.value = null
          const st = getOrCreateStep(ev.data.step || currentStepNum)
          st.scratchpad = ev.data.text || ''
        } else if (ev.event === 'content') {
          activeThinkingStep.value = null
          const delta = ev.data.delta || ''
          appendSmoothContent(delta)
          streamingMessage.value.duration_ms = Date.now() - startTime
        } else if (ev.event === 'tool_start') {
          activeThinkingStep.value = null
          const stepNum = ev.data.step || currentStepNum
          const st = getOrCreateStep(stepNum)
          const newTool = {
            call_id: ev.data.call_id,
            tool: ev.data.tool,
            label: ev.data.label,
            summary: '正在执行...',
            status: 'running' as const,
            step: stepNum,
          }
          streamingMessage.value.tool_calls = streamingMessage.value.tool_calls || []
          streamingMessage.value.tool_calls.push(newTool)
          st.tool_calls.push(newTool)
        } else if (ev.event === 'tool_done') {
          activeThinkingStep.value = null
          const stepNum = ev.data.step || currentStepNum
          const list = streamingMessage.value.tool_calls || []
          const target = list.find((t) => t.call_id === ev.data.call_id)
          if (target) {
            target.status = ev.data.status
            target.summary = ev.data.summary
            target.citations = ev.data.citations
          }
          const st = getOrCreateStep(stepNum)
          const stTarget = st.tool_calls.find((t) => t.call_id === ev.data.call_id)
          if (stTarget) {
            stTarget.status = ev.data.status
            stTarget.summary = ev.data.summary
            stTarget.citations = ev.data.citations
          }
        } else if (ev.event === 'artifact') {
          activeThinkingStep.value = null
          streamingMessage.value.artifacts = streamingMessage.value.artifacts || []
          streamingMessage.value.artifacts.push(ev.data)
        } else if (ev.event === 'todo') {
          if (Array.isArray(ev.data?.todos)) {
            todoList.value = ev.data.todos
          }
        } else if (ev.event === 'done') {
          activeThinkingStep.value = null
          if (ev.data.duration_ms) {
            streamingMessage.value.duration_ms = ev.data.duration_ms
          }
          if (ev.data.step_details && Array.isArray(ev.data.step_details)) {
            streamingMessage.value.step_details = ev.data.step_details
          }
        } else if (ev.event === 'error') {
          activeThinkingStep.value = null
          const errMsg = ev.data?.message || '生成失败'
          ElMessage.error(errMsg)
          if (streamingMessage.value && !streamingMessage.value.content) {
            streamingMessage.value.content = `服务响应提示: ${errMsg}`
          }
        }
      },
      abortController.signal
    )
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      const errMsg = err.message || '流式连接异常中断'
      ElMessage.error(errMsg)
      if (streamingMessage.value && !streamingMessage.value.content) {
        streamingMessage.value.content = `服务响应提示: ${errMsg}`
      }
    }
  } finally {
    if (typingTimer) {
      await new Promise<void>((resolve) => {
        let maxWait = 40
        const check = setInterval(() => {
          maxWait--
          if (!typingTimer || maxWait <= 0 || (streamingMessage.value?.content || '').length >= targetContent.length) {
            clearInterval(check)
            resolve()
          }
        }, 25)
      })
    }
    flushSmoothContent()

    if (streamingMessage.value && (streamingMessage.value.content || streamingMessage.value.reasoning_content || (streamingMessage.value.tool_calls && streamingMessage.value.tool_calls.length > 0))) {
      streamingMessage.value.duration_ms = Date.now() - startTime
      messages.value.push({ ...streamingMessage.value })
    }
    streamingMessage.value = null
    activeThinkingStep.value = null
    isRunning.value = false
    abortController = null
  }
}

// 终止生成
function handleStop() {
  if (abortController) {
    abortController.abort()
    abortController = null
    isRunning.value = false
  }
}

// 切换模型
function handleUpdateModel(modelId: string) {
  currentModelId.value = modelId
  if (internalSessionId.value) {
    updateHarnessSession(internalSessionId.value, { model_id: modelId })
  }
}

function handleSelectPrompt(prompt: string) {
  handleSend(prompt)
}
</script>

<template>
  <div class="harness-chat-panel">
    <!-- 顶部操作栏：纯图标快捷操作组（展开侧栏 + 设置）或 伴读上下文徽标 + 关闭按钮 -->
    <div class="chat-panel-header">
      <div class="header-left">
        <!-- 在全屏 HarnessView 中展现的纯图标展开与设置 -->
        <template v-if="showTopActions">
          <button
            type="button"
            class="top-action-btn"
            title="展开会话历史"
            @click="emit('openSidebar')"
          >
            <PanelLeftOpen class="icon-sm" />
          </button>
          <button
            type="button"
            class="top-action-btn"
            title="系统与 AI 助手设置"
            @click="emit('openSettings')"
          >
            <Settings class="icon-sm" />
          </button>
        </template>

        <!-- 在文章或简历伴生模式下展现的上下文提示胶囊 -->
        <div v-else-if="contextTitle" class="context-pill" :title="contextTitle">
          <Sparkles class="context-icon" :size="13" />
          <span class="context-text">已关联：{{ contextTitle }}</span>
        </div>
      </div>

      <div class="header-right">
        <!-- 伴读模式下的清空对话按钮 -->
        <button
          v-if="!showTopActions && messages.length > 0"
          type="button"
          class="panel-icon-btn"
          title="清空对话"
          @click="handleClearCurrentSession"
        >
          <Trash2 :size="15" />
        </button>

        <!-- 收起 / 关闭卡片按钮 -->
        <button
          v-if="showClose"
          type="button"
          class="panel-icon-btn close-btn"
          title="收起 AI 窗口"
          @click="emit('close')"
        >
          <X :size="16" />
        </button>
      </div>
    </div>

    <!-- 消息流列表 -->
    <HarnessMessageList
      :messages="messages"
      :streaming-message="streamingMessage"
      :is-running="isRunning"
      :active-thinking-step="activeThinkingStep"
      :custom-prompts="quickPrompts"
      :welcome-title="contextTitle ? `AI 智能伴生助手` : 'Starlore 云端智能体'"
      :welcome-desc="contextTitle ? `已自动挂载《${contextTitle}》上下文。你可以随时向我提问关于内容的任何问题或点击下方快捷键。` : '深度联动知识库、演示文稿排版与 Word 报告生成，产物直接交付 MinIO 存储。'"
      @select-prompt="handleSelectPrompt"
    />

    <!-- 悬浮底栏输入卡片 -->
    <HarnessInputArea
      ref="inputAreaRef"
      :models="models"
      :models-loaded="modelsLoaded"
      :current-model-id="currentModelId"
      :is-running="isRunning"
      @send="handleSend"
      @stop="handleStop"
      @update-model="handleUpdateModel"
    >
      <template #dock>
        <HarnessTodoPanel :todos="todoList" :is-running="isRunning" />
      </template>
    </HarnessInputArea>
  </div>
</template>

<style scoped>
.harness-chat-panel {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  position: relative;
  background: var(--surface, rgba(255, 255, 255, 0.78));
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 24px;
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
}

[data-theme="dark"] .harness-chat-panel {
  border-color: var(--border, rgba(255, 255, 255, 0.08));
  background: var(--surface, rgba(20, 20, 24, 0.85));
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.35);
}

/* 顶部操作条：带渐变保护，确保滚动时文字不穿透 */
.chat-panel-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 12px 16px 14px;
  z-index: 35;
  display: flex;
  align-items: center;
  justify-content: space-between;
  pointer-events: none;
  background: linear-gradient(180deg, var(--surface, #ffffff) 60%, transparent 100%);
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
}

[data-theme="dark"] .chat-panel-header {
  background: linear-gradient(180deg, var(--surface, #141418) 60%, transparent 100%);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  pointer-events: auto;
}

.top-action-btn,
.panel-icon-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full, 9999px);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-soft, #5C4D3D);
  background: var(--surface, #ffffff);
  border: 1px solid var(--border, rgba(0, 0, 0, 0.12));
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

[data-theme="dark"] .top-action-btn,
[data-theme="dark"] .panel-icon-btn {
  background: #232328;
  border-color: rgba(255, 255, 255, 0.14);
  color: var(--ink-soft, #b4a89b);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.top-action-btn:hover,
.panel-icon-btn:hover {
  color: var(--ink, #1A1410);
  border-color: rgba(222, 67, 49, 0.4);
  background: var(--surface-hover, #ffffff);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
}

.close-btn:hover {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.4);
}

.icon-sm {
  width: 17px;
  height: 17px;
}

/* 上下文提示胶囊 */
.context-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent, #DE4331) 12%, var(--surface, #ffffff));
  border: 1px solid color-mix(in srgb, var(--accent, #DE4331) 25%, transparent);
  color: var(--accent, #DE4331);
  font-size: 0.76rem;
  font-weight: 600;
  max-width: 260px;
  backdrop-filter: blur(12px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.context-icon {
  flex-shrink: 0;
}

.context-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
