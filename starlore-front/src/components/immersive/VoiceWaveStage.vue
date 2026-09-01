<script setup lang="ts">
import { ref } from 'vue'
import EmotionBall from '@/components/EmotionBall.vue'

interface Props {
  currentEmotion?: string
}

withDefaults(defineProps<Props>(), {
  currentEmotion: '02',
})

const emotionBallRef = ref<InstanceType<typeof EmotionBall> | null>(null)

defineExpose({
  getEmotionBall: () => emotionBallRef.value,
})
</script>

<template>
  <div class="imm-visual-stage">
    <div class="imm-mascot-container">
      <div class="imm-mascot-aura" aria-hidden="true"></div>
      <EmotionBall
        ref="emotionBallRef"
        :size="380"
        shape="blob"
        :emotion="currentEmotion"
        :sketch="false"
        :show-rings="true"
        :show-style-toggle="true"
        label="Starlore AI 助手"
      />
    </div>
  </div>
</template>

<style scoped>
/* ── 左侧视觉舞台 (左右55开，在可用视口区域上下完美垂直居中) ── */
.imm-visual-stage {
  position: absolute;
  left: 0;
  top: 72px;
  bottom: 0;
  width: 50vw;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
  pointer-events: auto;
  user-select: none;
  box-sizing: border-box;
  padding: 0 32px 40px;
}

.imm-mascot-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-top: 0;
}

.imm-mascot-aura {
  position: absolute;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(var(--accent-rgb, 232, 93, 42), 0.16) 0%, transparent 70%);
  filter: blur(36px);
  pointer-events: none;
  animation: aura-pulse 4s ease-in-out infinite alternate;
}

@keyframes aura-pulse {
  0% { transform: scale(0.9); opacity: 0.5; }
  100% { transform: scale(1.12); opacity: 0.85; }
}

@media (max-width: 900px) {
  .imm-visual-stage {
    display: none;
  }
}
</style>
