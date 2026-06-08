<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps<{
  /** 0=idle, 1=thinking, 2=speaking */
  state: number
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)

const PARTICLE_COUNT = 1200
const SPHERE_RADIUS = 180

let width = 0
let height = 0
let particles: OrbParticle[] = []
let animationId = 0

// Rotation
let rotX = 0
let rotY = 0
let isDragging = false
let lastMouseX = 0
let lastMouseY = 0

// State
let targetSpeedMultiplier = 1
let currentSpeedMultiplier = 1
let audioLevel = 0

// Mode colors
const modeColors: Record<number, string> = {
  0: '#ff4d4d', // idle - red
  1: '#ffcc00', // thinking - yellow
  2: '#ffffff', // speaking - white
}

class OrbParticle {
  baseX: number
  baseY: number
  baseZ: number
  x: number
  y: number
  z: number
  type: 'signal' | 'core' | 'void'
  color: string
  size: number

  constructor() {
    const u = Math.random()
    const v = Math.random()
    const theta = 2 * Math.PI * u
    const phi = Math.acos(2 * v - 1)
    const r = Math.pow(Math.random(), 1 / 3) * SPHERE_RADIUS

    this.baseX = r * Math.sin(phi) * Math.cos(theta)
    this.baseY = r * Math.sin(phi) * Math.sin(theta)
    this.baseZ = r * Math.cos(phi)
    this.x = this.baseX
    this.y = this.baseY
    this.z = this.baseZ

    const rand = Math.random()
    if (rand < 0.4) {
      this.type = 'signal'
      this.color = '#ff4d4d'
    } else if (rand < 0.8) {
      this.type = 'core'
      this.color = '#ffffff'
    } else {
      this.type = 'void'
      this.color = '#000000'
    }

    this.size = Math.random() * 1.5 + 0.5
  }

  update(time: number, currentRotX: number, currentRotY: number, mode: number) {
    // Update signal particle color based on state
    if (this.type === 'signal') {
      this.color = modeColors[mode] || '#ff4d4d'
    }

    // Audio wave effect
    const audioWave = audioLevel * 50
    const wave = Math.sin(time * 0.002 + (this.baseX + this.baseY + this.baseZ) * 0.01) * (15 + audioWave)
    const radialUnit = Math.sqrt(this.baseX ** 2 + this.baseY ** 2 + this.baseZ ** 2) + 0.001

    // Thinking mode: sphere contracts slightly
    const sphereScale = mode === 1 ? 0.95 : 1
    const effectiveR = radialUnit * sphereScale + wave
    const rFactor = effectiveR / radialUnit

    let tx = this.baseX * rFactor
    let ty = this.baseY * rFactor
    let tz = this.baseZ * rFactor

    // Apply rotation X
    const cosX = Math.cos(currentRotX)
    const sinX = Math.sin(currentRotX)
    const y1 = ty * cosX - tz * sinX
    const z1 = ty * sinX + tz * cosX
    ty = y1
    tz = z1

    // Apply rotation Y
    const cosY = Math.cos(currentRotY)
    const sinY = Math.sin(currentRotY)
    const x2 = tx * cosY + tz * sinY
    const z2 = -tx * sinY + tz * cosY
    tx = x2
    tz = z2

    this.x = tx
    this.y = ty
    this.z = tz
  }

  draw(ctx: CanvasRenderingContext2D, cx: number, cy: number) {
    const pz = Math.max(-300, Math.min(this.z, 590))
    const perspective = 600 / (600 - pz)
    const drawX = this.x * perspective + cx
    const drawY = this.y * perspective + cy
    const drawSize = Math.max(0, this.size * perspective)

    if (this.type === 'void') {
      ctx.globalAlpha = 1
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.2)'
      ctx.lineWidth = 0.5
      ctx.beginPath()
      ctx.arc(drawX, drawY, drawSize, 0, Math.PI * 2)
      ctx.stroke()
    } else {
      ctx.fillStyle = this.color
      ctx.globalAlpha = Math.max(0.1, perspective - 0.4)
      ctx.beginPath()
      ctx.arc(drawX, drawY, drawSize, 0, Math.PI * 2)
      ctx.fill()
    }
  }
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  const parent = canvas.parentElement
  if (!parent) return
  width = parent.clientWidth
  height = parent.clientHeight
  canvas.width = width
  canvas.height = height
}

function animate(time: number) {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, width, height)

  const centerX = width / 2
  const centerY = height / 2
  const mode = props.state

  // Smooth speed transition
  currentSpeedMultiplier += (targetSpeedMultiplier - currentSpeedMultiplier) * 0.05

  // Simulate audio volume for speaking mode
  if (mode === 2) {
    const rawAudio = Math.sin(time * 0.01) * Math.sin(time * 0.003) * Math.random()
    const targetAudio = Math.max(0, rawAudio) * 1.5
    audioLevel += (targetAudio - audioLevel) * 0.2
  } else {
    audioLevel += (0 - audioLevel) * 0.1
  }

  // Auto rotation
  if (!isDragging) {
    rotY += 0.005 * currentSpeedMultiplier
    rotX += 0.002 * currentSpeedMultiplier
  }

  // Update particles
  for (const p of particles) {
    p.update(time, rotX, rotY, mode)
  }

  // Sort by Z for depth
  particles.sort((a, b) => a.z - b.z)

  // Draw particles
  for (const p of particles) {
    p.draw(ctx, centerX, centerY)
  }

  animationId = requestAnimationFrame(animate)
}

// Watch state changes to update speed multiplier
watch(() => props.state, (newState) => {
  if (newState === 0) {
    targetSpeedMultiplier = 1
  } else if (newState === 1) {
    targetSpeedMultiplier = 4.5
  } else if (newState === 2) {
    targetSpeedMultiplier = 1.2
  }
})

// Mouse/touch events
function onPointerDown(e: MouseEvent | TouchEvent) {
  isDragging = true
  const clientX = 'clientX' in e ? e.clientX : e.touches[0].clientX
  const clientY = 'clientY' in e ? e.clientY : e.touches[0].clientY
  lastMouseX = clientX
  lastMouseY = clientY
}

function onPointerMove(e: MouseEvent | TouchEvent) {
  if (!isDragging) return
  const clientX = 'clientX' in e ? e.clientX : e.touches[0].clientX
  const clientY = 'clientY' in e ? e.clientY : e.touches[0].clientY
  rotY += (clientX - lastMouseX) * 0.01
  rotX -= (clientY - lastMouseY) * 0.01
  lastMouseX = clientX
  lastMouseY = clientY
}

function onPointerUp() {
  isDragging = false
}

onMounted(() => {
  // Initialize particles
  for (let i = 0; i < PARTICLE_COUNT; i++) {
    particles.push(new OrbParticle())
  }

  resize()
  window.addEventListener('resize', resize)

  // Start animation
  animationId = requestAnimationFrame(animate)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', resize)
})
</script>

<template>
  <div class="ai-core-container">
    <div class="glow-background" :style="{ backgroundColor: modeColors[state] || '#ff4d4d' }"></div>
    <canvas
      ref="canvasRef"
      class="ai-core-canvas"
      @mousedown="onPointerDown"
      @mousemove="onPointerMove"
      @mouseup="onPointerUp"
      @mouseleave="onPointerUp"
      @touchstart.prevent="onPointerDown"
      @touchmove.prevent="onPointerMove"
      @touchend="onPointerUp"
    ></canvas>
  </div>
</template>

<style scoped>
.ai-core-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #050505;
}

.glow-background {
  position: absolute;
  width: 420px;
  height: 420px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.35;
  pointer-events: none;
  transition: background-color 0.8s ease;
}

.ai-core-canvas {
  display: block;
  position: relative;
  z-index: 10;
  cursor: grab;
  width: 100%;
  height: 100%;
}

.ai-core-canvas:active {
  cursor: grabbing;
}
</style>
