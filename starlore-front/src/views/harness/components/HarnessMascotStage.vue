<script setup lang="ts">
import { ref, watch } from 'vue'
import EmotionBall from '@/components/EmotionBall.vue'
import { useCompanionStore } from '@/stores/companion'

interface Props {
  isRunning?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isRunning: false,
})

const companionStore = useCompanionStore()
const emotionBallRef = ref<InstanceType<typeof EmotionBall> | null>(null)
const currentEmotion = ref('02')
let emotionTimer: ReturnType<typeof setTimeout> | null = null

function setEmotion(emoId: string, durationMs?: number) {
  if (emotionTimer) {
    clearTimeout(emotionTimer)
    emotionTimer = null
  }
  currentEmotion.value = emoId
  if (durationMs && durationMs > 0) {
    emotionTimer = setTimeout(() => {
      currentEmotion.value = companionStore.expression || '02'
      emotionTimer = null
    }, durationMs)
  }
}

// 监听生成状态联动情绪
watch(
  () => props.isRunning,
  (running, prev) => {
    if (running) {
      setEmotion('04') // 思考中
    } else if (prev) {
      // 刚刚完成，短暂开怀一笑
      setEmotion('05', 2500)
    }
  }
)

function handleMascotClick() {
  // 点击小球触发自转互动
  emotionBallRef.value?.spin(1)
  setEmotion('06', 2000)
}

defineExpose({
  getEmotionBall: () => emotionBallRef.value,
})
</script>

<template>
  <div class="harness-mascot-stage">
    <div class="harness-mascot-container">
      <div class="harness-mascot-aura" aria-hidden="true"></div>
      <EmotionBall
        ref="emotionBallRef"
        :size="380"
        shape="blob"
        :emotion="currentEmotion"
        :sketch="false"
        :show-rings="true"
        :show-style-toggle="true"
        :follow="true"
        :interactive="true"
        label="Starlore AI 助手"
      />
    </div>
  </div>
</template>

<style scoped>
.harness-mascot-stage {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  user-select: none;
  box-sizing: border-box;
  padding: 0 24px;
  z-index: 10;
}

.harness-mascot-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.harness-mascot-aura {
  position: absolute;
  width: 440px;
  height: 440px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(var(--accent-rgb, 222, 67, 49), 0.16) 0%, transparent 70%);
  filter: blur(36px);
  pointer-events: none;
  animation: aura-pulse 4s ease-in-out infinite alternate;
}

@keyframes aura-pulse {
  0% {
    transform: scale(0.88);
    opacity: 0.45;
  }
  100% {
    transform: scale(1.15);
    opacity: 0.85;
  }
}

@media (max-width: 900px) {
  .harness-mascot-stage {
    display: none;
  }
}
</style>
