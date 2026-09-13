<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { PanelLeftOpen, Settings } from '@lucide/vue'
import HarnessSidebar from './components/HarnessSidebar.vue'
import HarnessMessageList from './components/HarnessMessageList.vue'
import HarnessInputArea from './components/HarnessInputArea.vue'
import HarnessMascotStage from './components/HarnessMascotStage.vue'
import AgentConfigDrawer from '@/components/AgentConfigDrawer.vue'
import { getAiModels, getAgentConfig, updateAgentConfig } from '@/api/ai'
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

// 设置弹窗状态
const configModalOpen = ref(false)
const agentConfigSavedHint = ref('')
const agentConfigForm = reactive({
  modelName: 'deepseek-chat',
  similarityThreshold: 0.6,
  topK: 5,
  temperature: 0.7,
  enableRerank: 1,
})
const availableModels = ref<{ id: string; name: string; configured?: boolean }[]>([])

async function openAgentConfig() {
  configModalOpen.value = true
  agentConfigSavedHint.value = ''
  try {
    const modelRes = await getAiModels()
    if (modelRes && modelRes.models && modelRes.models.length) {
      availableModels.value = modelRes.models.filter((m) => !m.id.includes('embedding'))
    }
  } catch {}
  try {
    const res = await getAgentConfig()
    if (res.success && res.data) {
      const dbModel = res.data.modelName
      agentConfigForm.modelName =
        dbModel && availableModels.value.some((m) => m.id === dbModel)
          ? dbModel
          : currentModelId.value || 'deepseek-chat'
      agentConfigForm.similarityThreshold = res.data.similarityThreshold ?? 0.6
      agentConfigForm.topK = res.data.topK ?? 5
      agentConfigForm.temperature = res.data.temperature ?? 0.7
      agentConfigForm.enableRerank = res.data.enableRerank ?? 1
    }
  } catch {}
}

async function saveAgentConfig() {
  agentConfigSavedHint.value = ''
  try {
    await updateAgentConfig(agentConfigForm)
    agentConfigSavedHint.value = '✓ 参数保存成功！'
    setTimeout(() => {
      configModalOpen.value = false
      agentConfigSavedHint.value = ''
    }, 1200)
  } catch (e: any) {
    agentConfigSavedHint.value = '保存失败: ' + (e.message || '未知错误')
  }
}

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
    <!-- 左侧统一容器：控制从 270px 平滑扩展到 50%，推动右侧窗口左边框向右平滑移动 -->
    <div class="harness-left-pane" :class="{ 'is-collapsed': !isSidebarOpen }">
      <!-- 侧边栏：收起时向右退场，展开时从右滑入进场 -->
      <Transition name="sidebar-slide">
        <HarnessSidebar
          v-if="isSidebarOpen"
          :sessions="sessions"
          :current-session-id="currentSessionId"
          :is-open="isSidebarOpen"
          class="left-pane-sidebar"
          @select-session="selectSession"
          @new-session="handleNewSession"
          @delete-session="handleDeleteSession"
          @update-session="handleUpdateSession"
          @toggle-sidebar="isSidebarOpen = !isSidebarOpen"
          @open-settings="openAgentConfig"
        />
      </Transition>

      <!-- AI 小球舞台：收起时从左向右进场，展开时向左退场 -->
      <Transition name="mascot-slide">
        <HarnessMascotStage
          v-if="!isSidebarOpen"
          :is-running="isRunning"
          class="left-pane-mascot"
        />
      </Transition>
    </div>

    <!-- 右侧消息流与输入容器：撑满剩余空间，左边框随左侧容器伸缩而平滑左右移动 -->
    <main class="harness-main">
      <!-- 侧边栏折叠时呈现的左上角纯图标操作按钮组（不要文字：展开历史 + 设置） -->
      <Transition name="actions-fade">
        <div v-if="!isSidebarOpen" class="harness-top-actions">
          <button
            type="button"
            class="top-action-btn"
            title="展开会话历史"
            @click="isSidebarOpen = true"
          >
            <PanelLeftOpen class="icon-sm" />
          </button>
          <button
            type="button"
            class="top-action-btn"
            title="系统与 AI 助手设置"
            @click="openAgentConfig"
          >
            <Settings class="icon-sm" />
          </button>
        </div>
      </Transition>

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

    <!-- 设置弹窗 (来自 echobot 的 AgentConfigDrawer) -->
    <AgentConfigDrawer
      :open="configModalOpen"
      :agent-config-form="agentConfigForm"
      :available-models="availableModels"
      :agent-config-saved-hint="agentConfigSavedHint"
      @close="configModalOpen = false"
      @save="saveAgentConfig"
    />
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

/* 左侧统一伸缩容器：展开时 270px，收起时占 50% */
.harness-left-pane {
  height: 100%;
  width: 270px;
  flex: 0 0 270px;
  position: relative;
  overflow: hidden;
  transition: width 0.38s cubic-bezier(0.4, 0, 0.2, 1), flex-basis 0.38s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 20;
}

.harness-left-pane.is-collapsed {
  width: calc(50% - 9px);
  flex: 0 0 calc(50% - 9px);
}

@media (max-width: 900px) {
  .harness-left-pane {
    display: none;
  }
}

.left-pane-sidebar {
  position: absolute;
  top: 0;
  left: 0;
  width: 270px;
  height: 100%;
  z-index: 20;
}

.left-pane-mascot {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 10;
}

/* 侧边栏退场/进场动画：向右退场 / 从右进场 */
.sidebar-slide-enter-active {
  transition: opacity 0.32s ease, transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.sidebar-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.sidebar-slide-enter-from,
.sidebar-slide-leave-to {
  opacity: 0;
  transform: translateX(50px);
}

/* AI 小球退场/进场动画：从左向右进场 / 向左退场 */
.mascot-slide-enter-active {
  transition: opacity 0.38s ease 0.05s, transform 0.38s cubic-bezier(0.4, 0, 0.2, 1) 0.05s;
}
.mascot-slide-leave-active {
  transition: opacity 0.22s ease, transform 0.26s cubic-bezier(0.4, 0, 0.2, 1);
}
.mascot-slide-enter-from,
.mascot-slide-leave-to {
  opacity: 0;
  transform: translateX(-50px);
}

/* 右侧主聊天卡片：flex: 1 撑满其余宽度，左边框自然平滑跟随左侧容器移动 */
.harness-main {
  flex: 1 1 0;
  min-width: 0;
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

/* 顶部纯图标快捷操作组（展开 + 设置） */
.harness-top-actions {
  position: absolute;
  top: 14px;
  left: 16px;
  z-index: 35;
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-action-btn {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full, 9999px);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-soft, #5C4D3D);
  background: var(--surface, rgba(255, 255, 255, 0.85));
  border: 1px solid rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.top-action-btn:hover {
  color: var(--ink, #1A1410);
  border-color: rgba(222, 67, 49, 0.3);
  background: var(--surface-hover, #ffffff);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.09);
}

[data-theme="dark"] .top-action-btn {
  color: #a1a1aa;
  background: var(--surface, rgba(24, 24, 28, 0.88));
  border-color: var(--border, rgba(255, 255, 255, 0.08));
}

[data-theme="dark"] .top-action-btn:hover {
  color: #ffffff;
  background: rgba(36, 36, 42, 0.95);
  border-color: rgba(222, 67, 49, 0.4);
}

.icon-sm {
  width: 16px;
  height: 16px;
}

/* 顶部操作按钮淡入淡出 */
.actions-fade-enter-active,
.actions-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.actions-fade-enter-from,
.actions-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
