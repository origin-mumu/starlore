<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import HarnessSidebar from './components/HarnessSidebar.vue'
import HarnessMascotStage from './components/HarnessMascotStage.vue'
import HarnessChatPanel from './components/HarnessChatPanel.vue'
import AgentConfigDrawer from '@/components/AgentConfigDrawer.vue'
import { getAiModels, getAgentConfig, updateAgentConfig } from '@/api/ai'
import {
  createHarnessSession,
  deleteHarnessSession,
  fetchHarnessModels,
  fetchHarnessSessions,
  updateHarnessSession,
} from '@/api/harness'
import type {
  HarnessModelItem,
  HarnessSession,
} from './types'

// 状态管理
const sessions = ref<HarnessSession[]>([])
const currentSessionId = ref<number | null>(null)
const models = ref<HarnessModelItem[]>([])
const currentModelId = ref<string>('')
const isSidebarOpen = ref(true)
const isRunning = ref(false)

// 设置弹窗状态
const configModalOpen = ref(false)
const agentConfigSavedHint = ref('')
const agentConfigForm = reactive({
  modelName: '',
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
          : currentModelId.value || availableModels.value[0]?.id || ''
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
      currentSessionId.value = fetchedSessions[0].id
    } else if (currentModelId.value) {
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
  if (s && s.model_id && models.value.some((m) => m.id === s.model_id)) {
    currentModelId.value = s.model_id
  } else if (models.value.length > 0) {
    currentModelId.value = models.value[0].id
  }
}

// 新建会话
async function handleNewSession() {
  if (isRunning.value) {
    ElMessage.warning('当前任务正在生成中，请稍候')
    return
  }
  if (!currentModelId.value && models.value.length > 0) {
    currentModelId.value = models.value[0].id
  }
  try {
    const newS = await createHarnessSession('新会话', currentModelId.value)
    sessions.value.unshift(newS)
    currentSessionId.value = newS.id
  } catch (err: any) {
    ElMessage.error(err.message || '创建会话失败')
  }
}

// 更新会话
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

// 会话创建或列表变更刷新
async function refreshSessions() {
  try {
    sessions.value = await fetchHarnessSessions()
  } catch {}
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

    <!-- 右侧消息流与输入容器：复用通用的 HarnessChatPanel 对话卡片 -->
    <HarnessChatPanel
      v-model:session-id="currentSessionId"
      v-model:is-running="isRunning"
      :show-top-actions="!isSidebarOpen"
      @open-sidebar="isSidebarOpen = true"
      @open-settings="openAgentConfig"
      @session-created="refreshSessions"
    />

    <!-- 设置弹窗 -->
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
</style>
