<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

interface Star {
  x: number
  y: number
  radius: number
  alpha: number
  phase: number
  speed: number
  accent: boolean
}

interface Meteor {
  x: number
  y: number
  vx: number
  vy: number
  life: number
  maxLife: number
}

const canvasRef = ref<HTMLCanvasElement | null>(null)
let context: CanvasRenderingContext2D | null = null
let stars: Star[] = []
let meteors: Meteor[] = []
let frameId = 0
let resizeFrameId = 0
let lastTime = 0
let meteorDelay = 0
let width = 0
let height = 0
let dpr = 1
let running = false
let reducedMotion = false
let themeObserver: MutationObserver | null = null
let starColor = 'rgba(180, 160, 140, 0.4)'
let accentColor = '#2fcbe7'

const readThemeColors = () => {
  const styles = getComputedStyle(document.documentElement)
  starColor = styles.getPropertyValue('--star-color').trim() || starColor
  accentColor = styles.getPropertyValue('--accent-sky').trim() || accentColor
}

const buildStars = () => {
  const count = window.innerWidth < 768 ? 34 : 76
  stars = Array.from({ length: count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    radius: 0.45 + Math.random() * 1.5,
    alpha: 0.18 + Math.random() * 0.52,
    phase: Math.random() * Math.PI * 2,
    speed: 0.35 + Math.random() * 0.65,
    accent: Math.random() < 0.09,
  }))
}

const resize = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  width = window.innerWidth
  height = window.innerHeight
  dpr = Math.min(window.devicePixelRatio || 1, 1.5)
  canvas.width = Math.max(1, Math.round(width * dpr))
  canvas.height = Math.max(1, Math.round(height * dpr))
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  context = canvas.getContext('2d')
  context?.setTransform(dpr, 0, 0, dpr, 0, 0)
  buildStars()
}

const spawnMeteor = () => {
  if (meteors.length >= 2) return
  const maxLife = 1.25 + Math.random() * 0.8
  meteors.push({
    x: width * (0.18 + Math.random() * 0.72),
    y: -28,
    vx: -(64 + Math.random() * 54),
    vy: 116 + Math.random() * 78,
    life: maxLife,
    maxLife,
  })
}

const draw = (now: number) => {
  if (!context) return
  const dt = Math.min((now - lastTime) / 1000 || 0, 0.05)
  lastTime = now
  context.clearRect(0, 0, width, height)

  for (const star of stars) {
    const pulse = reducedMotion ? 0.7 : 0.58 + Math.sin(now * 0.0007 * star.speed + star.phase) * 0.22
    context.globalAlpha = Math.max(0.08, star.alpha * pulse)
    context.fillStyle = star.accent ? accentColor : starColor
    context.beginPath()
    context.arc(star.x, star.y, star.radius, 0, Math.PI * 2)
    context.fill()
  }

  if (!reducedMotion) {
    meteorDelay -= dt
    if (meteorDelay <= 0) {
      spawnMeteor()
      meteorDelay = 5.5 + Math.random() * 8
    }

    for (let index = meteors.length - 1; index >= 0; index--) {
      const meteor = meteors[index]
      meteor.x += meteor.vx * dt
      meteor.y += meteor.vy * dt
      meteor.life -= dt
      if (meteor.life <= 0 || meteor.y > height + 40) {
        meteors.splice(index, 1)
        continue
      }

      const alpha = Math.sin((meteor.life / meteor.maxLife) * Math.PI) * 0.45
      for (let dot = 0; dot < 6; dot++) {
        const falloff = Math.pow(0.64, dot)
        context.globalAlpha = alpha * falloff
        context.fillStyle = dot === 0 ? accentColor : starColor
        context.beginPath()
        context.arc(
          meteor.x - meteor.vx * 0.055 * dot,
          meteor.y - meteor.vy * 0.055 * dot,
          Math.max(0.55, 1.8 - dot * 0.22),
          0,
          Math.PI * 2,
        )
        context.fill()
      }
    }
  }

  context.globalAlpha = 1
  if (running) frameId = requestAnimationFrame(draw)
}

const start = () => {
  if (running) return
  running = true
  lastTime = performance.now()
  frameId = requestAnimationFrame(draw)
}

const stop = () => {
  running = false
  if (frameId) cancelAnimationFrame(frameId)
  frameId = 0
}

const handleVisibility = () => {
  if (document.hidden) stop()
  else start()
}

const handleResize = () => {
  if (resizeFrameId) return
  resizeFrameId = requestAnimationFrame(() => {
    resizeFrameId = 0
    resize()
  })
}

onMounted(() => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  readThemeColors()
  resize()
  themeObserver = new MutationObserver(readThemeColors)
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme'],
  })
  document.addEventListener('visibilitychange', handleVisibility)
  window.addEventListener('resize', handleResize, { passive: true })
  start()
})

onBeforeUnmount(() => {
  stop()
  themeObserver?.disconnect()
  if (resizeFrameId) cancelAnimationFrame(resizeFrameId)
  document.removeEventListener('visibilitychange', handleVisibility)
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="cosmic-backdrop" aria-hidden="true">
    <canvas ref="canvasRef"></canvas>
    <svg class="orbit-sketch" viewBox="0 0 1200 760" preserveAspectRatio="xMidYMid slice">
      <g class="orbit-sketch__drift">
        <ellipse cx="840" cy="330" rx="460" ry="176" />
        <ellipse cx="840" cy="330" rx="320" ry="122" />
        <path d="M190 630 C360 350 590 155 1010 72" />
        <path d="M270 716 C515 420 760 250 1170 210" />
        <circle cx="510" cy="252" r="5" class="orbit-node orbit-node--warm" />
        <circle cx="1018" cy="206" r="4" class="orbit-node" />
        <circle cx="705" cy="454" r="3.5" class="orbit-node" />
      </g>
    </svg>
  </div>
</template>

<style scoped>
.cosmic-backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  opacity: 0.78;
  -webkit-mask-image: linear-gradient(to bottom, black 0%, black 74%, transparent 100%);
  mask-image: linear-gradient(to bottom, black 0%, black 74%, transparent 100%);
}

canvas,
.orbit-sketch {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.orbit-sketch {
  color: color-mix(in oklch, var(--accent-sky) 60%, var(--ink-muted));
  opacity: 0.26;
}

.orbit-sketch__drift {
  transform-origin: 70% 43%;
  animation: orbit-drift 34s linear infinite alternate;
}

.orbit-sketch ellipse,
.orbit-sketch path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
  vector-effect: non-scaling-stroke;
}

.orbit-sketch ellipse {
  stroke-dasharray: 2 12;
}

.orbit-sketch path {
  opacity: 0.52;
}

.orbit-node {
  fill: var(--accent-sky);
  stroke: color-mix(in oklch, var(--accent-sky) 42%, transparent);
  stroke-width: 9;
}

.orbit-node--warm {
  fill: var(--warm);
  stroke: color-mix(in oklch, var(--warm) 38%, transparent);
}

@keyframes orbit-drift {
  from {
    transform: translate3d(-1.5%, 0, 0) rotate(-1.2deg) scale(1);
  }
  to {
    transform: translate3d(1.5%, -1%, 0) rotate(1.4deg) scale(1.035);
  }
}

@media (max-width: 767px) {
  .cosmic-backdrop {
    opacity: 0.58;
  }

  .orbit-sketch {
    transform: translateX(20%);
    opacity: 0.2;
  }
}

@media (prefers-reduced-motion: reduce) {
  .orbit-sketch__drift {
    animation: none;
  }
}
</style>
