<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

interface Dot {
  x: number
  row: number
  phase: number
  accent: boolean
  ring: boolean
}

const canvasRef = ref<HTMLCanvasElement | null>(null)
const rootRef = ref<HTMLElement | null>(null)
let context: CanvasRenderingContext2D | null = null
let dots: Dot[] = []
let width = 0
let height = 0
let frameId = 0
let resizeFrameId = 0
let time = Math.random() * 80
let running = false
let inView = true
let reducedMotion = false
let observer: IntersectionObserver | null = null

const rebuild = () => {
  const canvas = canvasRef.value
  const root = rootRef.value
  if (!canvas || !root) return
  const rect = root.getBoundingClientRect()
  width = rect.width
  height = rect.height
  const dpr = Math.min(window.devicePixelRatio || 1, 1.5)
  canvas.width = Math.max(1, Math.round(width * dpr))
  canvas.height = Math.max(1, Math.round(height * dpr))
  context = canvas.getContext('2d')
  context?.setTransform(dpr, 0, 0, dpr, 0, 0)

  const spacing = window.innerWidth < 768 ? 21 : 17
  dots = []
  for (let column = 0; column <= Math.ceil(width / spacing); column++) {
    for (let row = 0; row < 3; row++) {
      dots.push({
        x: column * spacing,
        row,
        phase: Math.random() * Math.PI * 2,
        accent: Math.random() < 0.055,
        ring: Math.random() < 0.08,
      })
    }
  }
}

const draw = () => {
  if (!context) return
  const styles = getComputedStyle(document.documentElement)
  const base = styles.getPropertyValue('--warm').trim()
  const accent = styles.getPropertyValue('--accent-sky').trim()
  context.clearRect(0, 0, width, height)
  const rowGap = height / 4

  for (const dot of dots) {
    const wave = Math.sin(dot.x * 0.018 - time * 1.35 + dot.row * 0.84)
    const drift = Math.sin(dot.x * 0.006 + time * 0.52 + dot.phase)
    const y = rowGap * (dot.row + 1) + wave * 6.5 + drift * 2.5
    const crest = (wave + 1) / 2
    const alpha = 0.08 + crest * 0.32
    const radius = 0.9 + crest * 1.45
    context.globalAlpha = alpha
    context.fillStyle = dot.accent ? accent : base
    context.strokeStyle = dot.accent ? accent : base
    context.lineWidth = 1
    context.beginPath()
    context.arc(dot.x, y, dot.ring ? radius + 1.2 : radius, 0, Math.PI * 2)
    if (dot.ring) context.stroke()
    else context.fill()
  }
  context.globalAlpha = 1
}

const tick = () => {
  time += 0.016
  draw()
  if (running) frameId = requestAnimationFrame(tick)
}

const start = () => {
  if (running || reducedMotion || !inView || document.hidden) return
  running = true
  frameId = requestAnimationFrame(tick)
}

const stop = () => {
  running = false
  if (frameId) cancelAnimationFrame(frameId)
}

const handleVisibility = () => {
  if (document.hidden) stop()
  else start()
}

const handleResize = () => {
  if (resizeFrameId) return
  resizeFrameId = requestAnimationFrame(() => {
    resizeFrameId = 0
    rebuild()
    draw()
  })
}

onMounted(() => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  rebuild()
  draw()
  observer = new IntersectionObserver(entries => {
    inView = entries[0]?.isIntersecting ?? true
    if (inView) start()
    else stop()
  })
  if (rootRef.value) observer.observe(rootRef.value)
  document.addEventListener('visibilitychange', handleVisibility)
  window.addEventListener('resize', handleResize, { passive: true })
  start()
})

onBeforeUnmount(() => {
  stop()
  observer?.disconnect()
  if (resizeFrameId) cancelAnimationFrame(resizeFrameId)
  document.removeEventListener('visibilitychange', handleVisibility)
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div ref="rootRef" class="stellar-dots-band" aria-hidden="true">
    <canvas ref="canvasRef"></canvas>
  </div>
</template>

<style scoped>
.stellar-dots-band {
  height: 70px;
  width: 100%;
  margin: 4px 0 18px;
  -webkit-mask-image: linear-gradient(90deg, transparent, black 9%, black 91%, transparent);
  mask-image: linear-gradient(90deg, transparent, black 9%, black 91%, transparent);
  pointer-events: none;
}

canvas {
  display: block;
  width: 100%;
  height: 100%;
}

@media (max-width: 767px) {
  .stellar-dots-band {
    height: 54px;
    margin-bottom: 8px;
  }
}
</style>
