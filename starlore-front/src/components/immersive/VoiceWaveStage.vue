<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import EmotionBall from '@/components/EmotionBall.vue'

interface Props {
  currentEmotion?: string
  currentMode?: 'idle' | 'listening' | 'thinking' | 'speaking'
  statusText?: string
  interimText?: string
  showHint?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  currentEmotion: '02',
  currentMode: 'idle',
  statusText: 'SYSTEM READY',
  interimText: '',
  showHint: true,
})

const emit = defineEmits<{
  (e: 'toggleVoice'): void
}>()

const emotionBallRef = ref<InstanceType<typeof EmotionBall> | null>(null)
const waveCanvasRef = ref<HTMLCanvasElement | null>(null)

let animId = 0
let audioLevel = 0

const waveLayers = [
  { speed: 0.005, freq: 0.006, alpha: 0.8, offset: 0, scale: 1.0 },
  { speed: 0.0075, freq: 0.008, alpha: 0.5, offset: 2, scale: 0.8 },
  { speed: 0.004, freq: 0.005, alpha: 0.3, offset: 4, scale: 0.6 },
  { speed: 0.01, freq: 0.01, alpha: 0.2, offset: 6, scale: 0.4 },
]

let targetWaveW = 160,
  curWaveW = 160
let targetAmp = 0,
  curAmp = 0
let targetMorph = 1,
  curMorph = 1

function renderWave(time: number) {
  const c = waveCanvasRef.value
  if (!c) return
  const ctx = c.getContext('2d')
  if (!ctx) return
  const w = c.width,
    h = c.height,
    cy = h / 2,
    cx = w / 2
  ctx.clearRect(0, 0, w, h)

  const mode = props.currentMode
  if (mode === 'idle') {
    targetWaveW = 160
    targetMorph = 1
    targetAmp = 0
  } else if (mode === 'listening') {
    targetWaveW = w * 0.7
    targetMorph = 0
    targetAmp = 0.3 + Math.sin(time * 0.005) * 0.1
  } else if (mode === 'thinking') {
    targetWaveW = w * 0.5
    targetMorph = 0
    targetAmp = 0.2 + Math.random() * 0.1
  } else if (mode === 'speaking') {
    targetWaveW = w * 0.9
    targetMorph = 0
    targetAmp = 0.2 + audioLevel * 0.8
  }

  curMorph += (targetMorph - curMorph) * 0.08
  curWaveW += (targetWaveW - curWaveW) * 0.1
  curAmp += (targetAmp - curAmp) * 0.1
  const waveAlpha = 1 - curMorph

  // 液态流光球（idle 状态）
  if (curMorph > 0.005) {
    ctx.save()
    ctx.translate(cx, cy)
    ctx.scale(1 + (1 - curMorph) * 6, 1 - (1 - curMorph) * 0.8)
    ctx.globalAlpha = curMorph
    const orbSize = 55 + Math.sin(time * 0.003) * 4
    ctx.globalCompositeOperation = 'screen'
    ctx.rotate(time * 0.001)
    const colors = [
      { r: 255, g: 128, b: 181, radius: orbSize * 1.3, offset: 0, dist: 12 },
      { r: 129, g: 230, b: 217, radius: orbSize * 1.2, offset: 2.1, dist: 18 },
      { r: 167, g: 139, b: 250, radius: orbSize * 1.4, offset: 4.2, dist: 10 },
    ]
    colors.forEach((item, idx) => {
      ctx.save()
      ctx.rotate(time * 0.002 * (idx % 2 === 0 ? 1 : -1) + item.offset)
      ctx.translate(item.dist, 0)
      const g = ctx.createRadialGradient(0, 0, 0, 0, 0, item.radius)
      g.addColorStop(0, `rgba(${item.r},${item.g},${item.b},0.9)`)
      g.addColorStop(0.5, `rgba(${item.r},${item.g},${item.b},0.4)`)
      g.addColorStop(1, `rgba(${item.r},${item.g},${item.b},0.0)`)
      ctx.fillStyle = g
      ctx.beginPath()
      ctx.arc(0, 0, item.radius, 0, Math.PI * 2)
      ctx.fill()
      ctx.restore()
    })
    ctx.globalCompositeOperation = 'source-over'
    const cg = ctx.createRadialGradient(0, 0, 0, 0, 0, orbSize * 0.7)
    cg.addColorStop(0, 'rgba(255,255,255,1)')
    cg.addColorStop(0.3, 'rgba(255,255,255,0.7)')
    cg.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = cg
    ctx.beginPath()
    ctx.arc(0, 0, orbSize * 0.7, 0, Math.PI * 2)
    ctx.fill()
    ctx.restore()
  }

  // 声波
  if (waveAlpha > 0.005) {
    ctx.save()
    ctx.globalAlpha = waveAlpha
    ctx.globalCompositeOperation = 'screen'
    const grad = ctx.createLinearGradient(0, 0, w, 0)
    grad.addColorStop(0.1, '#ff80b5')
    grad.addColorStop(0.5, '#a78bfa')
    grad.addColorStop(0.9, '#81e6d9')
    const sx = cx - curWaveW / 2,
      ex = cx + curWaveW / 2
    waveLayers.forEach(layer => {
      ctx.beginPath()
      ctx.moveTo(sx, cy)
      for (let x = sx; x <= ex; x += 2) {
        const prog = (x - sx) / curWaveW
        const env = Math.pow(Math.sin(prog * Math.PI), 2.5)
        const yOff =
          Math.sin(x * layer.freq + time * layer.speed + layer.offset) *
          ((h / 2) * curAmp * layer.scale * env)
        ctx.lineTo(x, cy + yOff)
      }
      for (let x = ex; x >= sx; x -= 2) {
        const prog = (x - sx) / curWaveW
        const env = Math.pow(Math.sin(prog * Math.PI), 2.5)
        const yOff =
          Math.sin(x * layer.freq + time * layer.speed + layer.offset + Math.PI) *
          ((h / 2) * curAmp * layer.scale * env)
        ctx.lineTo(x, cy + yOff)
      }
      ctx.closePath()
      ctx.fillStyle = grad
      ctx.globalAlpha = layer.alpha * waveAlpha * 1.2
      ctx.fill()
    })
    ctx.restore()
  }
}

function resizeWave() {
  const c = waveCanvasRef.value
  if (!c) return
  const p = c.parentElement
  if (!p) return
  c.width = p.clientWidth * 2
  c.height = p.clientHeight * 2
}

function animate(time: number) {
  if (props.currentMode === 'speaking') {
    audioLevel +=
      (Math.max(0, Math.sin(time * 0.015) * Math.sin(time * 0.005) * Math.random()) * 1.5 -
        audioLevel) *
      0.2
  } else {
    audioLevel += (0 - audioLevel) * 0.1
  }

  renderWave(time)
  animId = requestAnimationFrame(animate)
}

onMounted(() => {
  resizeWave()
  window.addEventListener('resize', resizeWave)
  animId = requestAnimationFrame(animate)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resizeWave)
})

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
        label="Starlore AI 智能小球"
      />
      <div class="imm-mascot-caption">
        <span class="imm-mascot-badge">✦ STARLORE AI COMPANION</span>
      </div>
    </div>

    <div class="mic-wrapper">
      <div class="wave-container" @click="emit('toggleVoice')" title="点击开始/结束说话">
        <canvas ref="waveCanvasRef" class="wave-canvas"></canvas>
        <div class="interaction-hint" :class="{ hidden: !showHint }">[ 点击以语音交流 ]</div>
      </div>
      <div class="status-text" :class="currentMode">{{ statusText }}</div>
      <div v-if="interimText" class="interim-text">{{ interimText }}</div>
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

.imm-mascot-caption {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.imm-mascot-badge {
  font-size: 11px;
  letter-spacing: 2.5px;
  font-weight: 600;
  color: var(--ink-muted);
  background: var(--surface-translucent, rgba(255, 255, 255, 0.15));
  backdrop-filter: blur(12px);
  border: 1px solid var(--border);
  padding: 4px 14px;
  border-radius: 20px;
  box-shadow: var(--shadow-sm);
}

.mic-wrapper {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.wave-container {
  width: 280px;
  height: 72px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
}

.wave-canvas {
  display: block;
  width: 100%;
  height: 100%;
  pointer-events: none;
  transition: opacity 0.3s;
}
.wave-container:hover .wave-canvas {
  filter: brightness(1.3);
}

.interaction-hint {
  position: absolute;
  bottom: 2px;
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--ink-muted);
  pointer-events: none;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  animation: breathe-text 2s infinite ease-in-out;
  opacity: 1;
  transform: translateY(0);
}
.interaction-hint.hidden {
  opacity: 0;
  transform: translateY(15px);
  animation: none;
}
@keyframes breathe-text {
  0%,
  100% {
    opacity: 0.4;
  }
  50% {
    opacity: 0.9;
    text-shadow: 0 0 10px rgba(255, 255, 255, 0.4);
  }
}

.status-text {
  font-size: 11px;
  letter-spacing: 4px;
  text-transform: uppercase;
  color: var(--ink-muted);
  font-weight: bold;
  transition: color 0.3s;
}
.status-text.listening {
  color: var(--accent);
}
.status-text.thinking {
  color: var(--warm);
}
.status-text.speaking {
  color: var(--accent);
}

.interim-text {
  font-size: 13px;
  color: var(--ink-soft);
  max-width: 300px;
  text-align: center;
  animation: fadeInUp 0.3s ease;
}
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .imm-visual-stage {
    display: none;
  }
}
</style>
