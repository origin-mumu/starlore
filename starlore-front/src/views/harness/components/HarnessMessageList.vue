<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { ArrowDown, Sparkles } from '@lucide/vue'
import HarnessMessageItem from './HarnessMessageItem.vue'
import type { HarnessMessage } from '../types'

const props = defineProps<{
  messages: HarnessMessage[]
  streamingMessage?: HarnessMessage | null
  isRunning?: boolean
}>()

const emit = defineEmits<{
  selectPrompt: [prompt: string]
}>()

const scrollContainer = ref<HTMLElement | null>(null)
const showScrollToBottom = ref(false)
let userHasScrolledUp = false

function checkScroll() {
  if (!scrollContainer.value) return
  const { scrollTop, scrollHeight, clientHeight } = scrollContainer.value
  const distFromBottom = scrollHeight - scrollTop - clientHeight
  if (distFromBottom > 120) {
    userHasScrolledUp = true
    showScrollToBottom.value = true
  } else {
    userHasScrolledUp = false
    showScrollToBottom.value = false
  }
}

function scrollToBottom(smooth = true) {
  if (!scrollContainer.value) return
  scrollContainer.value.scrollTo({
    top: scrollContainer.value.scrollHeight,
    behavior: smooth ? 'smooth' : 'auto',
  })
  userHasScrolledUp = false
  showScrollToBottom.value = false
}

watch(
  [
    () => props.messages.length,
    () => props.streamingMessage?.content,
    () => props.streamingMessage?.reasoning_content,
    () => props.streamingMessage?.tool_calls?.length,
    () => props.streamingMessage?.artifacts?.length,
    () => props.isRunning,
  ],
  async () => {
    if (!userHasScrolledUp) {
      await nextTick()
      scrollToBottom(false)
    }
  }
)

onMounted(() => {
  scrollToBottom(false)
})

const defaultPrompts = [
  '根据知识库关于微服务治理的文章，生成一份 6 页的技术答辩 PPT',
  '提炼星域知识库中的微服务架构方案，输出为一份规范 Word 报告',
  '检索知识库中关于容器化部署的内容并做深度总结',
]
</script>

<template>
  <div
    ref="scrollContainer"
    class="harness-message-list"
    @scroll="checkScroll"
  >
    <!-- 空状态：Codex 风格极简欢迎页面 -->
    <div
      v-if="messages.length === 0 && !streamingMessage"
      class="welcome-container"
    >
      <div class="welcome-icon-box">
        <Sparkles class="welcome-icon" />
      </div>
      <h2 class="welcome-title">
        Starlore 云端智能体
      </h2>
      <p class="welcome-desc">
        深度联动知识库、演示文稿排版与 Word 报告生成，产物直接交付 MinIO 存储。
      </p>

      <!-- 引导建议快捷提示词 -->
      <div class="prompt-suggestions">
        <button
          v-for="(p, idx) in defaultPrompts"
          :key="idx"
          type="button"
          class="prompt-btn"
          @click="emit('selectPrompt', p)"
        >
          {{ p }}
        </button>
      </div>
    </div>

    <!-- 正式消息瀑布流 -->
    <div v-else class="messages-flow">
      <HarnessMessageItem
        v-for="(msg, idx) in messages"
        :key="msg.id || idx"
        :message="msg"
      />

      <!-- 当前正在流式接收的活跃消息 -->
      <HarnessMessageItem
        v-if="streamingMessage"
        :message="streamingMessage"
        :is-running="isRunning"
      />
    </div>

    <!-- 浮动【回到底部】小按钮 -->
    <button
      v-if="showScrollToBottom"
      type="button"
      class="scroll-bottom-btn"
      title="回到底部"
      @click="scrollToBottom(true)"
    >
      <ArrowDown class="icon-sm" />
    </button>
  </div>
</template>

<style scoped>
.harness-message-list {
  flex: 1;
  overflow-y: auto;
  position: relative;
  width: 100%;
  height: 100%;
  padding-bottom: 150px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.18) transparent;
}

.harness-message-list::-webkit-scrollbar {
  width: 6px;
}

.harness-message-list::-webkit-scrollbar-track {
  background: transparent;
}

.harness-message-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.16);
  border-radius: 9999px;
  transition: background 0.2s ease;
}

.harness-message-list::-webkit-scrollbar-thumb:hover {
  background: var(--accent, #DE4331);
}

[data-theme="dark"] .harness-message-list {
  scrollbar-color: rgba(255, 255, 255, 0.22) transparent;
}

[data-theme="dark"] .harness-message-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
}

[data-theme="dark"] .harness-message-list::-webkit-scrollbar-thumb:hover {
  background: var(--accent, #DE4331);
}

.welcome-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;
  max-width: 520px;
  margin: 0 auto;
  user-select: none;
}

.welcome-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: rgba(37, 99, 235, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  margin-bottom: 16px;
}

.welcome-icon {
  width: 24px;
  height: 24px;
}

.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: #18181b;
  margin: 0 0 6px 0;
}

[data-theme="dark"] .welcome-title {
  color: #f4f4f5;
}

.welcome-desc {
  font-size: 13px;
  color: #71717a;
  margin: 0 0 24px 0;
  line-height: 1.5;
}

.prompt-suggestions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.prompt-btn {
  text-align: left;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  color: #3f3f46;
  transition: all 0.2s;
  cursor: pointer;
  line-height: 1.4;
}

.prompt-btn:hover {
  background: #ffffff;
  border-color: rgba(37, 99, 235, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

[data-theme="dark"] .prompt-btn {
  background: rgba(24, 24, 27, 0.7);
  border-color: rgba(255, 255, 255, 0.08);
  color: #d4d4d8;
}

[data-theme="dark"] .prompt-btn:hover {
  background: #27272a;
  border-color: rgba(96, 165, 250, 0.3);
}

.messages-flow {
  padding: 20px 0 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scroll-bottom-btn {
  position: absolute;
  bottom: 120px;
  right: 24px;
  z-index: 25;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #52525b;
  cursor: pointer;
  transition: all 0.2s;
}

.scroll-bottom-btn:hover {
  background: #f4f4f5;
  transform: translateY(-2px);
}

[data-theme="dark"] .scroll-bottom-btn {
  background: #27272a;
  border-color: rgba(255, 255, 255, 0.12);
  color: #e4e4e7;
}

.icon-sm {
  width: 16px;
  height: 16px;
}
</style>
