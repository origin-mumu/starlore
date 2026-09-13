<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, type ComputedRef } from 'vue'
import {
  GitBranch,
  Eye,
  Database,
  BrainCircuit,
  Workflow,
  Activity,
  Presentation,
  Sparkles,
} from '@lucide/vue'
import type { DeliverableCardItem } from './carouselShots'

const props = defineProps<{
  item: DeliverableCardItem
  corner: number
  sink: number
  off: boolean
  index?: number
}>()

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v))

const stillness = () =>
  typeof window !== 'undefined' &&
  !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const springOf = (tune: number) => ({
  k: 0.08 + (tune / 100) * 0.16,
  d: 0.62 + (tune / 100) * 0.2,
})

function useSpring(target: ComputedRef<number>, tune = 50, instant = false) {
  const at = ref(target.value)
  const cur = ref(target.value)
  const vel = ref(0)
  let raf = 0

  const start = () => {
    if (instant) {
      cur.value = target.value
      vel.value = 0
      at.value = target.value
      return
    }
    const { k, d } = springOf(tune)
    let prev = 0
    const tick = (t: number) => {
      const dt = prev ? clamp((t - prev) / 16.67, 0, 2.5) : 1
      prev = t
      vel.value += (target.value - cur.value) * k * dt
      vel.value *= Math.pow(d, dt)
      cur.value += vel.value * dt
      if (Math.abs(target.value - cur.value) < 0.02 && Math.abs(vel.value) < 0.02) {
        cur.value = target.value
        vel.value = 0
        at.value = target.value
        raf = 0
        return
      }
      at.value = cur.value
      raf = requestAnimationFrame(tick)
    }
    if (raf) cancelAnimationFrame(raf)
    raf = requestAnimationFrame(tick)
  }

  watch(target, () => {
    start()
  })

  onMounted(() => {
    start()
  })

  onUnmounted(() => {
    if (raf) cancelAnimationFrame(raf)
    raf = 0
  })

  return at
}

const skin = ref<HTMLDivElement | null>(null)
const pt = ref({ x: 0, y: 0 })
const on = ref(false)
const still = stillness()

const live = computed(() => on.value && !props.off)
const targetX = computed(() => (live.value ? pt.value.x : 0))
const targetY = computed(() => (live.value ? pt.value.y : 0))
const targetLit = computed(() => (live.value ? 1 : 0))

const sx = useSpring(targetX, 50, still)
const sy = useSpring(targetY, 50, still)
const lit = useSpring(targetLit, 50, still)

const deep = computed(() => clamp(props.sink, 0, 100) / 100)
const max = computed(() => deep.value * 8)

const rx = computed(() => -sy.value * max.value)
const ry = computed(() => sx.value * max.value)

const px = computed(() => ((sx.value + 1) / 2) * 100)
const py = computed(() => ((sy.value + 1) / 2) * 100)
const rim = computed(() => deep.value * 0.25 * lit.value)

const track = (e: PointerEvent) => {
  const el = skin.value
  if (!el) return
  const b = el.getBoundingClientRect()
  pt.value = {
    x: clamp(((e.clientX - b.left) / b.width) * 2 - 1, -1, 1),
    y: clamp(((e.clientY - b.top) / b.height) * 2 - 1, -1, 1),
  }
  on.value = true
}

const handlePointerOut = (e: PointerEvent) => {
  const el = skin.value
  const to = e.relatedTarget as Node | null
  if (!el || !to || !el.contains(to)) on.value = false
}

const handlePointerCancel = () => {
  on.value = false
}

const cardTransform = computed(() => ({
  borderRadius: `${props.corner}px`,
  transform: `translateZ(${(-6 * deep.value * lit.value).toFixed(2)}px) rotateX(${rx.value.toFixed(2)}deg) rotateY(${ry.value.toFixed(2)}deg)`,
}))

const sheenStyle = computed(() => ({
  borderRadius: `${props.corner}px`,
  backgroundImage: `radial-gradient(60% 50% at ${px.value.toFixed(1)}% ${py.value.toFixed(1)}%, rgba(255, 255, 255, ${rim.value.toFixed(3)}) 0%, rgba(255, 255, 255, 0) 100%)`,
}))

const iconComponent = computed(() => {
  switch (props.item.iconName) {
    case 'GitBranch':
      return GitBranch
    case 'Eye':
      return Eye
    case 'Presentation':
      return Presentation
    case 'BrainCircuit':
      return BrainCircuit
    case 'Database':
      return Database
    case 'Workflow':
      return Workflow
    case 'Activity':
      return Activity
    default:
      return Sparkles
  }
})

const itemIndexStr = computed(() => {
  if (typeof props.index === 'number') {
    return `0${props.index + 1}`
  }
  const map: Record<string, string> = {
    agent: '01',
    vr: '02',
    ppt: '03',
    rag: '04',
    trace: '05',
  }
  return map[props.item.name] || '01'
})
</script>

<template>
  <div
    ref="skin"
    class="unified-card"
    :style="cardTransform"
    @pointermove="track"
    @pointerout="handlePointerOut"
    @pointercancel="handlePointerCancel"
  >
    <!-- Top Accent Bar (统一的顶边微光条) -->
    <div class="card-accent-bar" :style="{ backgroundColor: item.accent }"></div>

    <div class="card-body">
      <!-- 1. Header: Index and Category -->
      <div class="card-header">
        <div class="card-index-tag">
          <span class="index-num">{{ itemIndexStr }}</span>
          <span class="index-dot" :style="{ backgroundColor: item.accent }"></span>
          <span class="index-text">{{ item.tag }}</span>
        </div>
        <span class="card-cat-name">{{ item.category }}</span>
      </div>

      <!-- 2. Minimal Center Visual: Unified Sleek Icon Container -->
      <div class="card-icon-section">
        <div class="icon-avatar-wrap">
          <component :is="iconComponent" :size="28" :style="{ color: item.accent }" />
        </div>
        <span class="highlight-pill" :style="{ borderColor: item.accent, color: item.accent }">
          {{ item.highlight }}
        </span>
      </div>

      <!-- 3. Content: Title and Concise Explanation -->
      <div class="card-content-section">
        <h3 class="card-title">{{ item.title }}</h3>
        <p class="card-desc">{{ item.subtitle }}</p>
      </div>

      <!-- 4. Footer: Unified Tech Tags -->
      <div class="card-footer">
        <div class="tech-chips">
          <span v-for="tag in item.tech" :key="tag" class="chip-item">
            {{ tag }}
          </span>
        </div>
      </div>
    </div>

    <!-- Soft Reflection Sheen -->
    <span class="car-sheen" aria-hidden="true" :style="sheenStyle" />
  </div>
</template>

<style scoped>
/* ── 统一纯净极简卡片（5张卡片严格同一视觉样式） ── */
.unified-card {
  position: relative;
  width: 100%;
  height: 100%;
  background: #FFFFFF;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  box-shadow:
    0 10px 28px -6px rgba(15, 23, 42, 0.07),
    0 2px 8px -2px rgba(15, 23, 42, 0.04);
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.2s ease;
  user-select: none;
}

:global([data-theme="dark"]) .unified-card {
  background: #141828;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow:
    0 16px 36px -8px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(255, 255, 255, 0.05);
}

.unified-card:hover {
  border-color: rgba(15, 23, 42, 0.16);
  box-shadow:
    0 20px 38px -10px rgba(15, 23, 42, 0.12),
    0 4px 12px -2px rgba(15, 23, 42, 0.06);
}

:global([data-theme="dark"]) .unified-card:hover {
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow:
    0 22px 45px -10px rgba(0, 0, 0, 0.8),
    0 0 20px -4px rgba(255, 255, 255, 0.08);
}

/* 顶部强调线 */
.card-accent-bar {
  height: 3px;
  width: 100%;
  flex-shrink: 0;
}

.card-body {
  flex: 1;
  padding: 22px 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  text-align: left;
  z-index: 1;
}

/* 1. Header */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-index-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border-radius: 6px;
  background: #F1F5F9;
  border: 1px solid #E2E8F0;
}

:global([data-theme="dark"]) .card-index-tag {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}

.index-num {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.68rem;
  font-weight: 750;
  color: #0F172A;
}

:global([data-theme="dark"]) .index-num {
  color: #F8FAFC;
}

.index-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
}

.index-text {
  font-size: 0.6rem;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-weight: 700;
  color: #64748B;
  letter-spacing: 0.04em;
}

:global([data-theme="dark"]) .index-text {
  color: #94A3B8;
}

.card-cat-name {
  font-size: 0.72rem;
  color: #64748B;
  font-weight: 600;
}

:global([data-theme="dark"]) .card-cat-name {
  color: #94A3B8;
}

/* 2. Center Icon Section */
.card-icon-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 12px 0;
  gap: 10px;
}

.icon-avatar-wrap {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
}

:global([data-theme="dark"]) .icon-avatar-wrap {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.4);
}

.highlight-pill {
  font-size: 0.68rem;
  font-weight: 650;
  padding: 2px 10px;
  border-radius: 9999px;
  border: 1px solid;
  background: rgba(255, 255, 255, 0.7);
  letter-spacing: 0.03em;
}

:global([data-theme="dark"]) .highlight-pill {
  background: rgba(0, 0, 0, 0.3);
}

/* 3. Content Section */
.card-content-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-title {
  margin: 0;
  font-size: 1.12rem;
  font-weight: 800;
  color: #0F172A;
  line-height: 1.3;
  letter-spacing: -0.015em;
}

:global([data-theme="dark"]) .card-title {
  color: #FFFFFF;
}

.card-desc {
  margin: 0;
  font-size: 0.78rem;
  color: #475569;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

:global([data-theme="dark"]) .card-desc {
  color: #94A3B8;
}

/* 4. Footer Section */
.card-footer {
  padding-top: 14px;
  border-top: 1px solid #F1F5F9;
}

:global([data-theme="dark"]) .card-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

.tech-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.chip-item {
  font-size: 0.64rem;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-weight: 600;
  color: #475569;
  background: #F1F5F9;
  border: 1px solid #E2E8F0;
  padding: 2px 7px;
  border-radius: 4px;
}

:global([data-theme="dark"]) .chip-item {
  color: #CBD5E1;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}

/* Specular Reflection Sheen Layer */
.car-sheen {
  position: absolute;
  inset: 0;
  display: block;
  pointer-events: none;
}
</style>
