<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { PanelLeftOpen } from '@lucide/vue'
import HarnessSidebar from './components/HarnessSidebar.vue'
import HarnessMessageList from './components/HarnessMessageList.vue'
import HarnessInputArea from './components/HarnessInputArea.vue'
import {
  createHarnessSession,
  deleteHarnessSession,
  fetchHarnessMessages,
  fetchHarnessModels,
  fetchHarnessSessions,
  streamHarnessChat,
  updateHarnessSession,
} from '@/api/harness'
import type {
  HarnessMessage,
  HarnessModelItem,
  HarnessSession,
  StreamEventPayload,
} from './types'

// 状态管理
const sessions = ref<HarnessSession[]>([])
const currentSessionId = ref<number | null>(null)
const messages = ref<HarnessMessage[]>([])
const models = ref<HarnessModelItem[]>([])
const currentModelId = ref<string>('deepseek-chat')
const isSidebarOpen = ref(true)
const isRunning = ref(false)

// 当前流式生成中的消息
const streamingMessage = ref<HarnessMessage | null>(null)
let abortController: AbortController | null = null

const inputAreaRef = ref<InstanceType<typeof HarnessInputArea> | null>(null)

// 当前活跃会话
const currentSession = computed(() => {
  return sessions.value.find((s) => s.id === currentSessionId.value) || null
})

// 初始化拉取数据
onMounted(async () => {
  try {
    const [fetchedModels, fetchedSessions] = await Promise.all([
      fetchHarnessModels(),
      fetchHarnessSessions(),
    ])
    models.value = fetchedModels
    if (fetchedModels.length > 0) {
      currentModelId.value = fetchedModels[0].id
    }

    sessions.value = fetchedSessions
    if (fetchedSessions.length > 0) {
      await selectSession(fetchedSessions[0].id)
    } else {
      await handleNewSession()
    }
  } catch (err: any) {
    ElMessage.error(err.message || '初始化 Harness 失败')
  }
})

// 切换会话
async function selectSession(sessionId: number) {
  if (isRunning.value) {
    ElMessage.warning('当前任务正在生成中，请稍候')
    return
  }
  currentSessionId.value = sessionId
  const s = sessions.value.find((item) => item.id === sessionId)
  if (s && s.model_id) {
    currentModelId.value = s.model_id
  }
  try {
    messages.value = await fetchHarnessMessages(sessionId)
  } catch (err: any) {
    ElMessage.error(err.message || '加载消息失败')
  }
}

// 新建会话
async function handleNewSession() {
  if (isRunning.value) {
    ElMessage.warning('当前任务正在生成中，请稍候')
    return
  }
  try {
    const newS = await createHarnessSession('新会话', currentModelId.value)
    sessions.value.unshift(newS)
    currentSessionId.value = newS.id
    messages.value = []
  } catch (err: any) {
    ElMessage.error(err.message || '创建会话失败')
  }
}

// 更新会话（重命名/置顶）
async function handleUpdateSession(
  sessionId: number,
  payload: { title?: string; pinned?: boolean }
) {
  try {
    const updated = await updateHarnessSession(sessionId, payload)
    const idx = sessions.value.findIndex((s) => s.id === sessionId)
    if (idx !== -1) {
      sessions.value[idx] = updated
    }
  } catch (err: any) {
    ElMessage.error(err.message || '更新会话失败')
  }
}

// 删除会话
async function handleDeleteSession(sessionId: number) {
  try {
    await deleteHarnessSession(sessionId)
    sessions.value = sessions.value.filter((s) => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      if (sessions.value.length > 0) {
        await selectSession(sessions.value[0].id)
      } else {
        await handleNewSession()
      }
    }
    ElMessage.success('已删除会话')
  } catch (err: any) {
    ElMessage.error(err.message || '删除失败')
  }
}

// 点击快捷提示词直接填入并发送
function handleSelectPrompt(prompt: string) {
  handleSend(prompt)
}

// 发送消息核心逻辑
async function handleSend(userText: string, images?: string[]) {
  if (!currentSessionId.value || isRunning.value) return

  // 1. 本地立即追加用户消息
  const userMsg: HarnessMessage = {
    role: 'user',
    content: userText,
    images: images && images.length > 0 ? [...images] : undefined,
  }
  messages.value.push(userMsg)

  // 2. 初始化流式响应消息
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
      currentSessionId.value,
      { message: userText, model_id: currentModelId.value, images },
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
          getOrCreateStep(currentStepNum)
        } else if (ev.event === 'reasoning') {
          const delta = ev.data.delta || ''
          streamingMessage.value.reasoning_content =
            (streamingMessage.value.reasoning_content || '') + delta
          const st = getOrCreateStep(currentStepNum)
          st.reasoning = (st.reasoning || '') + delta
          streamingMessage.value.duration_ms = Date.now() - startTime
        } else if (ev.event === 'step_thought') {
          const st = getOrCreateStep(ev.data.step || currentStepNum)
          st.scratchpad = ev.data.text || ''
        } else if (ev.event === 'content') {
          const delta = ev.data.delta || ''
          appendSmoothContent(delta)
          streamingMessage.value.duration_ms = Date.now() - startTime
        } else if (ev.event === 'tool_start') {
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
          streamingMessage.value.artifacts = streamingMessage.value.artifacts || []
          streamingMessage.value.artifacts.push(ev.data)
        } else if (ev.event === 'done') {
          if (ev.data.duration_ms) {
            streamingMessage.value.duration_ms = ev.data.duration_ms
          }
          if (ev.data.step_details && Array.isArray(ev.data.step_details)) {
            streamingMessage.value.step_details = ev.data.step_details
          }
        } else if (ev.event === 'error') {
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
    // 等待打字机平滑流出剩余尾部字词，避免流关闭瞬间闪现
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
    // 归档当前流式消息
    if (streamingMessage.value && (streamingMessage.value.content || streamingMessage.value.reasoning_content || (streamingMessage.value.tool_calls && streamingMessage.value.tool_calls.length > 0))) {
      streamingMessage.value.duration_ms = Date.now() - startTime
      messages.value.push({ ...streamingMessage.value })
    }
    streamingMessage.value = null
    isRunning.value = false
    abortController = null

    // 重新拉取会话列表以刷新标题与修改时间
    try {
      sessions.value = await fetchHarnessSessions()
    } catch {}
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
  if (currentSessionId.value) {
    updateHarnessSession(currentSessionId.value, { model_id: modelId })
  }
}
</script>

<template>
  <div class="harness-page">
    <!-- 侧边栏大圆角矩形卡片 -->
    <HarnessSidebar
      :sessions="sessions"
      :current-session-id="currentSessionId"
      :is-open="isSidebarOpen"
      @select-session="selectSession"
      @new-session="handleNewSession"
      @delete-session="handleDeleteSession"
      @update-session="handleUpdateSession"
      @toggle-sidebar="isSidebarOpen = !isSidebarOpen"
    />

    <!-- 右侧消息流与输入容器 -->
    <main class="harness-main">
      <!-- 侧边栏折叠时呈现的极简展开浮动按钮 -->
      <button
        v-if="!isSidebarOpen"
        type="button"
        class="floating-sidebar-toggle"
        title="展开会话列表"
        @click="isSidebarOpen = true"
      >
        <PanelLeftOpen class="icon-sm" />
        <span>历史会话</span>
      </button>

      <HarnessMessageList
        :messages="messages"
        :streaming-message="streamingMessage"
        :is-running="isRunning"
        @select-prompt="handleSelectPrompt"
      />

      <!-- 悬浮底栏输入卡片 -->
      <HarnessInputArea
        ref="inputAreaRef"
        :models="models"
        :current-model-id="currentModelId"
        :is-running="isRunning"
        @send="handleSend"
        @stop="handleStop"
        @update-model="handleUpdateModel"
      />
    </main>
  </div>
</template>

<style scoped>
.harness-page {
  height: 100%;
  width: 100%;
  display: flex;
  background: transparent;
  color: var(--ink, #1A1410);
  overflow: hidden;
  position: relative;
  padding-top: 76px;
  padding-left: 20px;
  padding-right: 20px;
  padding-bottom: 20px;
  gap: 18px;
  box-sizing: border-box;
}

.harness-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  position: relative;
  background: var(--surface, rgba(255, 255, 255, 0.75));
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 24px;
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
}

[data-theme="dark"] .harness-main {
  border-color: var(--border, rgba(255, 255, 255, 0.08));
  background: var(--surface, rgba(20, 20, 24, 0.85));
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.35);
}

.floating-sidebar-toggle {
  position: absolute;
  top: 10px;
  left: 16px;
  z-index: 35;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-full, 9999px);
  font-size: 12.5px;
  font-weight: 550;
  color: var(--ink-soft, #5C4D3D);
  background: var(--surface, rgba(255, 255, 255, 0.85));
  border: 1px solid rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.floating-sidebar-toggle:hover {
  color: var(--ink, #1A1410);
  border-color: rgba(222, 67, 49, 0.3);
  background: var(--surface-hover, #ffffff);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.09);
}

[data-theme="dark"] .floating-sidebar-toggle {
  color: #a1a1aa;
  background: var(--surface, rgba(24, 24, 28, 0.88));
  border-color: var(--border, rgba(255, 255, 255, 0.08));
}

[data-theme="dark"] .floating-sidebar-toggle:hover {
  color: #ffffff;
  background: rgba(36, 36, 42, 0.95);
}

.icon-sm {
  width: 16px;
  height: 16px;
}
</style>
