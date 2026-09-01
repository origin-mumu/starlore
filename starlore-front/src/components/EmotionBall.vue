<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { mountReplica, type ReplicaMount } from '@/replica/host'
import { useThemeStore } from '@/stores/theme'
import { useCompanionStore } from '@/stores/companion'

interface Props {
  emotion?: string
  state?: string
  shape?: string
  color?: string
  eyeColor?: string
  size?: number | string
  follow?: boolean
  showStyleToggle?: boolean
  interactive?: boolean
  label?: string
  showRings?: boolean
  initialSpin?: boolean
  autoTricks?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  emotion: '',
  state: '',
  shape: '',
  follow: true,
  showStyleToggle: false,
  interactive: true,
  showRings: false,
  initialSpin: true,
  autoTricks: true,
  label: 'Starlore AI 助手',
})

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
  (e: 'ready', live: boolean): void
}>()

const themeStore = useThemeStore()
const companionStore = useCompanionStore()
const svgRef = ref<SVGSVGElement | null>(null)
let replica: ReplicaMount | null = null
let isAlive = true

// 兼容老版本的数字情绪编号映射
const EMOTION_MAP: Record<string, string> = {
  '01': 'idle',
  '02': 'curious',
  '03': 'listening',
  '04': 'thinking',
  '05': 'happy',
  '06': 'excited',
  '12': 'sad',
  '19': 'celebrate',
}

const currentShape = computed(() => {
  if (props.shape) return props.shape
  return companionStore.shape || 'blob'
})

const currentState = computed(() => {
  const raw = props.state || props.emotion || companionStore.expression || 'curious'
  return EMOTION_MAP[raw] || raw
})

// 默认颜色使用 #7CFB5F 到 #4172D9 渐变，若用户在设置中自定义了颜色则优先使用用户的全局设置
const currentColor = computed(() => {
  if (props.color) return props.color
  if (companionStore.color) return companionStore.color
  return 'g:7cfb5f-4172d9-135'
})

const currentPaper = computed(() => {
  if (props.eyeColor) return props.eyeColor
  if (companionStore.eyeColor) return companionStore.eyeColor
  return themeStore.current === 'dark' ? '#0A051F' : '#FFFFFF'
})

const parsedSize = computed(() => {
  if (!props.size) return 220
  return typeof props.size === 'number' ? props.size : parseInt(props.size, 10) || 220
})

const sizeStyle = computed(() => ({
  width: `${parsedSize.value}px`,
  height: `${parsedSize.value}px`,
}))

function getReplicaInput() {
  return {
    state: currentState.value,
    shape: currentShape.value,
    color: currentColor.value,
    follow: props.interactive && props.follow && companionStore.follow,
    paper: currentPaper.value,
    size: parsedSize.value,
    autoTricks: props.autoTricks && companionStore.autoTricks,
  }
}

async function initReplica() {
  if (!svgRef.value) return
  try {
    const mount = await mountReplica(svgRef.value, getReplicaInput(), true)
    if (!isAlive) {
      mount?.destroy()
      return
    }
    replica = mount
    emit('ready', !!replica)
    if (props.initialSpin) {
      setTimeout(() => {
        replica?.spin(1)
      }, 400)
    }
  } catch (err) {
    console.error('Failed to mount GrokCharacter replica:', err)
  }
}

const handleClick = (e: MouseEvent) => {
  if (replica && props.interactive) {
    replica.spin(1)
  }
  emit('click', e)
}

const handleMouseEnter = () => {
  if (replica && props.interactive) {
    replica.bounce()
  }
}

const availableShapes = ['blob', 'bean', 'egg', 'cloud', 'leaf', 'squircle']
const currentShapeIndex = ref(0)

const cycleShape = () => {
  currentShapeIndex.value = (currentShapeIndex.value + 1) % availableShapes.length
  const nextShape = availableShapes[currentShapeIndex.value]
  companionStore.saveConfig({ shape: nextShape })
  replica?.setShape(nextShape)
  replica?.spin(0.5)
}

// 暴露外部可调用的方法
const setEmotion = (nameOrId: string) => {
  const resolved = EMOTION_MAP[nameOrId] || nameOrId
  replica?.setState(resolved)
}

const setState = (name: string) => {
  replica?.setState(name)
}

const setShape = (shape: string) => {
  replica?.setShape(shape)
}

const setColor = (color: string) => {
  replica?.setColor(color)
}

const spin = (turns: number = 1) => {
  replica?.spin(turns)
}

const bounce = () => {
  replica?.bounce()
}

const burst = () => {
  replica?.burst()
}

const orbitGaze = (ms?: number) => {
  replica?.orbitGaze(ms)
}

defineExpose({
  setEmotion,
  setState,
  setShape,
  setColor,
  spin,
  bounce,
  burst,
  orbitGaze,
  getReplica: () => replica,
  getEngine: () => replica,
})

watch(
  () => [
    currentState.value,
    props.shape,
    currentColor.value,
    currentPaper.value,
    props.interactive,
    props.follow,
    parsedSize.value,
    props.autoTricks,
  ],
  () => {
    replica?.apply(getReplicaInput())
  },
)

onMounted(() => {
  isAlive = true
  initReplica()
})

onUnmounted(() => {
  isAlive = false
  replica?.destroy()
  replica = null
})
const handleInteractiveSpin = () => {
  replica?.spin(1)
}
</script>

<template>
  <div class="emotion-ball-wrapper">
    <div
      class="emotion-ball-container"
      :style="sizeStyle"
      :title="label || 'Starlore AI 助手'"
      @click="handleClick"
      @mouseenter="handleMouseEnter"
    >
      <!-- 可选周围星轨光环装饰 -->
      <div v-if="showRings" class="orb-orbit-glow" aria-hidden="true">
        <div class="orbit-ring orbit-ring-1"></div>
        <div class="orbit-ring orbit-ring-2"></div>
      </div>

      <!-- SVG 角色舞台 -->
      <svg
        ref="svgRef"
        class="emotion-ball-svg"
        :width="parsedSize"
        :height="parsedSize"
        role="img"
        :aria-label="label"
      />
    </div>

    <!-- 底部极简切换器 -->
    <div
      v-if="showStyleToggle"
      class="style-toggle-bar"
      role="toolbar"
      aria-label="AI形态切换"
      @click.stop
    >
      <button
        type="button"
        class="style-toggle-btn"
        title="切换形态"
        @click="cycleShape"
      >
        <span class="dot-shape-icon" />
        <span class="toggle-text">切换形态</span>
      </button>
      <button
        type="button"
        class="style-toggle-btn"
        title="转个圈"
        @click="handleInteractiveSpin"
      >
        <span class="toggle-text">互动</span>
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
  position: relative;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  user-select: none;
  cursor: pointer;
  touch-action: manipulation;
}

.emotion-ball-svg {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
  filter: drop-shadow(0 16px 32px rgba(0, 0, 0, 0.16));
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.emotion-ball-container:hover .emotion-ball-svg {
  transform: scale(1.03);
  filter: drop-shadow(0 20px 38px rgba(0, 0, 0, 0.22));
}

.emotion-ball-container:active .emotion-ball-svg {
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
  from {
    transform: rotate(25deg) scaleY(0.55);
  }
  to {
    transform: rotate(385deg) scaleY(0.55);
  }
}

@keyframes orbit-spin-rev {
  from {
    transform: rotate(-35deg) scaleY(0.65);
  }
  to {
    transform: rotate(-395deg) scaleY(0.65);
  }
}

/* 风格切换悬浮胶囊 */
.style-toggle-bar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: var(--surface);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border);
  border-radius: 999px;
  box-shadow: var(--shadow-sm);
  z-index: 5;
  transition: all 0.2s ease;
}

.style-toggle-bar:hover {
  border-color: var(--border-interactive);
  box-shadow: var(--shadow-card);
  transform: translateY(-1px);
}

.style-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: transparent;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 0.76rem;
  color: var(--ink-soft);
  font-family: inherit;
  transition: all 0.18s ease;
}

.style-toggle-btn:hover {
  color: var(--accent);
  background: var(--surface-hover);
}

.dot-shape-icon {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
}

.toggle-text {
  font-weight: 600;
}
</style>
