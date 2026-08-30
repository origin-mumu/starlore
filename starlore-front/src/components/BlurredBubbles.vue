<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const canvasRef = ref<HTMLCanvasElement | null>(null)

// 全局背景使用的主题气泡颜色（浅色模式具备多色彩搭配，深色模式具备极光夜空感）
const themeBubbles: Record<string, string[]> = {
  light: ['#f7da3987', '#8fdbe9', '#fffef8'],
  dark: ['#16007B', '#2A48F3', '#35BFAB', '#51D0B9', '#8B5CF6'],
}

// 滤镜强度
const themeFilter: Record<string, string> = {
  light: 'blur(50px)',
  dark: 'blur(40px)',
}

// Simplex Noise
function makeNoise2D(random = Math.random) {
  const p = new Uint8Array(256)
  for (let i = 0; i < 256; i++) p[i] = (random() * 256) | 0

  function grad2(hash: number, x: number, y: number) {
    const h = hash & 7
    const u = h < 4 ? x : y
    const v = h < 4 ? y : x
    return (h & 1 ? -u : u) + (h & 2 ? -2 * v : 2 * v)
  }

  const G2 = (3.0 - Math.sqrt(3.0)) / 6.0
  const F2 = 0.5 * (Math.sqrt(3.0) - 1.0)

  return function noise2D(xin: number, yin: number) {
    let n0 = 0, n1 = 0, n2 = 0
    const s = (xin + yin) * F2
    const i = Math.floor(xin + s)
    const j = Math.floor(yin + s)
    const t = (i + j) * G2
    const X0 = i - t
    const Y0 = j - t
    const x0 = xin - X0
    const y0 = yin - Y0

    const i1 = x0 > y0 ? 1 : 0
    const j1 = x0 > y0 ? 0 : 1

    const x1 = x0 - i1 + G2
    const y1 = y0 - j1 + G2
    const x2 = x0 - 1 + 2 * G2
    const y2 = y0 - 1 + 2 * G2

    const ii = i & 255
    const jj = j & 255

    const t0 = 0.5 - x0 * x0 - y0 * y0
    if (t0 >= 0) {
      const gi0 = p[ii + p[jj]]
      const t0_4 = t0 * t0 * t0 * t0
      n0 = t0_4 * grad2(gi0, x0, y0)
    }

    const t1 = 0.5 - x1 * x1 - y1 * y1
    if (t1 >= 0) {
      const gi1 = p[ii + i1 + p[jj + j1]]
      const t1_4 = t1 * t1 * t1 * t1
      n1 = t1_4 * grad2(gi1, x1, y1)
    }

    const t2 = 0.5 - x2 * x2 - y2 * y2
    if (t2 >= 0) {
      const gi2 = p[ii + 1 + p[jj + 1]]
      const t2_4 = t2 * t2 * t2 * t2
      n2 = t2_4 * grad2(gi2, x2, y2)
    }

    return 40 * (n0 + n1 + n2)
  }
}

function rand(a: number, b: number) {
  return a + Math.random() * (b - a)
}

interface Bubble {
  x: number
  y: number
  r: number
  color: string
  vx: number
  vy: number
  jitter: number
  blur: number
}

// 动画状态
const noise = makeNoise2D()
let bubbles: Bubble[] = []
let animRef: number | null = null
let lastTime = 0
let accumulatedTime = 0
let width = 0
let height = 0

const COUNT = 6
const MIN_RADIUS = 250
const MAX_RADIUS = 400
const BOTTOM_BAND_START = 0.8
const SPEED = 0.12
const NOISE_SCALE = 0.0008
const NOISE_TIME_SCALE = 0.00015
const TARGET_FPS = 6

function createBubbles(canvasWidth: number, canvasHeight: number) {
  bubbles = []
  const colors = themeBubbles[themeStore.current] || themeBubbles.light
  const minDist = Math.max(MIN_RADIUS * 0.2, 80)
  const maxTries = 5000
  let tries = 0

  while (bubbles.length < COUNT && tries < maxTries) {
    tries++
    const r = rand(MIN_RADIUS, MAX_RADIUS)
    const x = rand(-r / 2, canvasWidth + r / 2)
    const y = rand(canvasHeight * BOTTOM_BAND_START, canvasHeight * 1.2)

    let ok = true
    for (const b of bubbles) {
      const dx = b.x - x
      const dy = b.y - y
      if (Math.hypot(dx, dy) < (b.r + r) * 0.6 || Math.hypot(dx, dy) < minDist) {
        ok = false
        break
      }
    }

    if (ok) {
      bubbles.push({
        x, y, r,
        color: colors[bubbles.length % colors.length],
        vx: rand(-0.2, 0.2),
        vy: rand(-0.2, 0.2),
        jitter: rand(0.6, 1.2),
        blur: rand(200, 400)
      })
    }
  }
}

function updatePhysics(t: number) {
  const bandMin = height * BOTTOM_BAND_START
  const bandMax = height * 1.5

  for (let i = 0; i < bubbles.length; i++) {
    const b = bubbles[i]

    const n = noise(b.x * NOISE_SCALE, b.y * NOISE_SCALE + t * NOISE_TIME_SCALE)
    const angle = n * Math.PI * 2
    const fx = Math.cos(angle) * SPEED * b.jitter
    const fy = Math.sin(angle) * SPEED * b.jitter

    let sx = 0, sy = 0
    for (let j = 0; j < bubbles.length; j++) {
      if (j !== i) {
        const o = bubbles[j]
        const dx = b.x - o.x
        const dy = b.y - o.y
        const d2 = dx * dx + dy * dy
        const minD = (b.r + o.r) * 0.4
        if (d2 < minD * minD && d2 > 0.001) {
          const d = Math.sqrt(d2)
          const push = (minD - d) / minD
          sx += (dx / d) * push * 0.8
          sy += (dy / d) * push * 0.8
        }
      }
    }

    let bx = 0, by = 0
    if (b.y < bandMin) by += (bandMin - b.y) * 0.01
    if (b.y > bandMax) by -= (b.y - bandMax) * 0.01

    b.vx += fx + sx + bx
    b.vy += fy + sy + by

    const damping = 0.95
    b.vx *= damping
    b.vy *= damping

    const maxVel = 2
    const vel = Math.hypot(b.vx, b.vy)
    if (vel > maxVel) {
      b.vx = (b.vx / vel) * maxVel
      b.vy = (b.vy / vel) * maxVel
    }

    b.x += b.vx
    b.y += b.vy

    if (b.x < -b.r - b.blur / 3) b.x = width + b.r + b.blur / 3
    if (b.x > width + b.r + b.blur / 3) b.x = -b.r - b.blur / 3

    b.y = Math.min(Math.max(b.y, bandMin - b.r * 0.25), bandMax + b.r * 0.25)
  }
}

function draw(ctx: CanvasRenderingContext2D) {
  ctx.clearRect(0, 0, width, height)

  const themeAlpha: Record<string, number> = {
    light: 0.8,
    dark: 0.45,
  }
  const alpha = themeAlpha[themeStore.current] ?? 0.32

  for (const b of bubbles) {
    ctx.save()
    ctx.filter = `blur(${b.blur}px)`
    ctx.globalAlpha = alpha
    ctx.beginPath()
    ctx.fillStyle = b.color
    ctx.arc(b.x, b.y, b.r, 0, Math.PI * 2)
    ctx.fill()
    ctx.restore()
  }
}

function animate(t: number) {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const frameInterval = 1000 / TARGET_FPS
  const deltaTime = lastTime ? t - lastTime : 0
  lastTime = t
  accumulatedTime += deltaTime

  if (accumulatedTime >= frameInterval) {
    accumulatedTime = 0
    updatePhysics(t)
    draw(ctx)
  }

  animRef = requestAnimationFrame(animate)
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return

  const dpr = Math.min(2, window.devicePixelRatio || 1)
  width = canvas.clientWidth
  height = canvas.clientHeight
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)

  const ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.setTransform(1, 0, 0, 1, 0, 0)
    ctx.scale(dpr, dpr)
  }

  createBubbles(width, height)
}

function updateBubbleColors() {
  const colors = themeBubbles[themeStore.current] || themeBubbles.light
  bubbles.forEach((b, i) => {
    b.color = colors[i % colors.length]
  })

  // 更新 canvas 的滤镜强度
  const canvas = canvasRef.value
  if (canvas) {
    const filter = themeFilter[themeStore.current] || themeFilter.light
    canvas.style.filter = filter
  }
}

watch(() => themeStore.current, updateBubbleColors)

onMounted(() => {
  resize()
  animate(0)
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  if (animRef) {
    cancelAnimationFrame(animRef)
  }
  window.removeEventListener('resize', resize)
})
</script>

<template>
  <canvas ref="canvasRef" class="blurred-bubbles" aria-hidden="true" />
</template>

<style scoped>
.blurred-bubbles {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -2;
  filter: blur(50px);
  pointer-events: none;
}
</style>
