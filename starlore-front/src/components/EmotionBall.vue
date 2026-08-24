<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { createEmotionBall, Engine } from '@/utils/emotion-ball'

interface Props {
  emotion?: string
  shape?: 'blob' | 'wedge' | 'gem'
  color?: string
  eyeColor?: string
  eyeScale?: number
  size?: number | string
  sketch?: boolean
  showStyleToggle?: boolean
  interactive?: boolean
  label?: string
  showRings?: boolean
  initialSpin?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  emotion: '02',
  shape: 'blob',
  sketch: true,
  showStyleToggle: false,
  interactive: true,
  showRings: false,
  initialSpin: true,
  label: 'Starlore AI Companion'
})

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
  (e: 'change', data: any): void
  (e: 'update:sketch', value: boolean): void
}>()

const containerRef = ref<HTMLElement | null>(null)
const isSketch = ref(props.sketch)
let engine: Engine | null = null

const sizeStyle = computed(() => {
  if (!props.size) return {}
  const val = typeof props.size === 'number' ? `${props.size}px` : props.size
  return { width: val, height: val }
})

// 鼠标注视追踪处理
const handlePointerMove = (e: MouseEvent) => {
  if (!engine || !containerRef.value || !props.interactive) return
  const rect = containerRef.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  
  // 视口相对偏移归一化到 [-1, 1]
  const dx = (e.clientX - centerX) / (window.innerWidth / 2)
  const dy = (e.clientY - centerY) / (window.innerHeight / 2)
  
  // 钳制防止眼睛飞出身体
  const clampV = (v: number) => (v < -0.85 ? -0.85 : v > 0.85 ? 0.85 : v)
  engine.setGaze(clampV(dx), clampV(dy))
}

const handleClick = (e: MouseEvent) => {
  if (engine && props.interactive) {
    // 旋转甩彩带互动
    engine.spin(1)
  }
  emit('click', e)
}

const handleDoubleClick = (e: MouseEvent) => {
  e.stopPropagation()
  toggleSketch()
}

const handleMouseEnter = () => {
  if (engine && props.interactive) {
    // 悬停时轻微弹跳或提神
    engine.resetIdle()
  }
}

// 切换 饱满实体 / 科技线稿 风格
const setSketchMode = (val: boolean) => {
  if (isSketch.value === val) return
  isSketch.value = val
  engine?.setStyle({ sketch: val ? 1 : 0 })
  engine?.spin(0.6)
  engine?.burst(10)
  emit('update:sketch', val)
}

const toggleSketch = () => {
  setSketchMode(!isSketch.value)
}

// 暴露外部可调用的方法
const setEmotion = (id: string) => {
  engine?.setEmotion(id)
}

const spin = (turns: number = 1) => {
  engine?.spin(turns)
}

const bounce = () => {
  engine?.bounce()
}

const burst = (count: number = 20) => {
  engine?.burst(count)
}

defineExpose({
  setEmotion,
  spin,
  bounce,
  burst,
  setSketchMode,
  toggleSketch,
  getEngine: () => engine
})

watch(
  () => props.emotion,
  (newEmo) => {
    if (engine && newEmo) {
      engine.setEmotion(newEmo)
    }
  }
)

watch(
  () => props.sketch,
  (newVal) => {
    if (isSketch.value !== newVal) {
      isSketch.value = newVal
      engine?.setStyle({ sketch: newVal ? 1 : 0 })
    }
  }
)

onMounted(() => {
  if (!containerRef.value) return
  
  engine = createEmotionBall(containerRef.value, {
    emotion: props.emotion,
    shape: props.shape,
    color: props.color,
    eyeColor: props.eyeColor,
    eyeScale: props.eyeScale,
    sketch: isSketch.value,
    label: props.label,
    idle: true
  })

  engine.on('change', (data: any) => {
    emit('change', data)
  })

  if (props.interactive) {
    window.addEventListener('pointermove', handlePointerMove, { passive: true })
  }

  // 进场打招呼
  if (props.initialSpin) {
    setTimeout(() => {
      engine?.spin(1)
    }, 600)
  }
})

onUnmounted(() => {
  if (props.interactive) {
    window.removeEventListener('pointermove', handlePointerMove)
  }
  engine?.destroy()
  engine = null
})
</script>

<template>
  <div class="emotion-ball-wrapper">
    <div
      class="emotion-ball-container"
      :class="{ 'is-sketch': isSketch }"
      :style="sizeStyle"
      title="点击甩彩带 · 双击切换风格"
      @click="handleClick"
      @dblclick="handleDoubleClick"
      @mouseenter="handleMouseEnter"
    >
      <!-- 可选周围星轨光环装饰 -->
      <div v-if="showRings" class="orb-orbit-glow" aria-hidden="true">
        <div class="orbit-ring orbit-ring-1"></div>
        <div class="orbit-ring orbit-ring-2"></div>
      </div>
      
      <!-- SVG 动画容器 -->
      <div ref="containerRef" class="emotion-ball-stage" />
    </div>

    <!-- 底部极简双点风格切换器 (实心 · 空心) -->
    <div v-if="showStyleToggle" class="style-toggle-bar" role="radiogroup" aria-label="展示风格切换" @click.stop>
      <button
        type="button"
        class="style-toggle-dot-btn"
        :class="{ active: !isSketch }"
        role="radio"
        :aria-checked="!isSketch"
        title="彩色饱满"
        @click="setSketchMode(false)"
      >
        <span class="dot-solid" />
      </button>
      <button
        type="button"
        class="style-toggle-dot-btn"
        :class="{ active: isSketch }"
        role="radio"
        :aria-checked="isSketch"
        title="科技线稿"
        @click="setSketchMode(true)"
      >
        <span class="dot-hollow" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.emotion-ball-wrapper {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.emotion-ball-container {
  --sketch-ink: var(--ink, #1C1920);
  position: relative;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  user-select: none;
  cursor: pointer;
  touch-action: manipulation;
}

.emotion-ball-container.is-sketch {
  --sketch-ink: color-mix(in srgb, var(--ink, #1C1920) 88%, transparent);
}

.emotion-ball-container.is-sketch :deep(svg > g path) {
  filter: drop-shadow(0 0 1px color-mix(in srgb, var(--ink, #1C1920) 25%, transparent));
}

.emotion-ball-stage {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.emotion-ball-stage :deep(svg) {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
  filter: drop-shadow(0 18px 36px rgba(28, 25, 32, 0.12));
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.emotion-ball-container:hover .emotion-ball-stage :deep(svg) {
  transform: scale(1.03);
  filter: drop-shadow(0 22px 42px rgba(28, 25, 32, 0.18));
}

.emotion-ball-container:active .emotion-ball-stage :deep(svg) {
  transform: scale(0.97);
}

/* 宇宙星轨光环装饰 */
.orb-orbit-glow {
  position: absolute;
  inset: -15%;
  pointer-events: none;
  z-index: 0;
}

.orbit-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px dashed color-mix(in srgb, var(--ink, #1C1920) 18%, transparent);
  pointer-events: none;
}

.orbit-ring-1 {
  transform: rotate(25deg) scaleY(0.55);
  animation: orbit-spin 36s linear infinite;
}

.orbit-ring-2 {
  inset: 10%;
  transform: rotate(-35deg) scaleY(0.65);
  animation: orbit-spin-rev 28s linear infinite;
}

@keyframes orbit-spin {
  from { transform: rotate(25deg) scaleY(0.55); }
  to { transform: rotate(385deg) scaleY(0.55); }
}

@keyframes orbit-spin-rev {
  from { transform: rotate(-35deg) scaleY(0.65); }
  to { transform: rotate(-395deg) scaleY(0.65); }
}

/* 风格切换悬浮胶囊 (实心/空心极简双点) */
.style-toggle-bar {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  background: color-mix(in srgb, var(--panel-bg, #FFFFFF) 78%, transparent);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid color-mix(in srgb, var(--ink, #1C1920) 10%, transparent);
  border-radius: 999px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.05);
  z-index: 5;
  transition: all 0.2s ease;
}

.style-toggle-bar:hover {
  border-color: color-mix(in srgb, var(--ink, #1C1920) 22%, transparent);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.09);
  transform: translateY(-1px);
}

.style-toggle-dot-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  opacity: 0.45;
  transition: all 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.style-toggle-dot-btn:hover {
  opacity: 0.85;
  transform: scale(1.15);
}

.style-toggle-dot-btn.active {
  opacity: 1;
  background: color-mix(in srgb, var(--ink, #1C1920) 7%, transparent);
  transform: scale(1.1);
}

/* 实心点 */
.dot-solid {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #F9705C, #5B95F0);
  box-shadow: 0 1px 3px rgba(91, 149, 240, 0.4);
}

/* 空心点 */
.dot-hollow {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: transparent;
  border: 1.6px solid var(--ink, #1C1920);
}
</style>
