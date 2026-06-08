<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  nodes: Array<{
    id: number
    label: string
    desc: string
    rx: number
    ry: number
    size: number
    color: string
    type: 'category' | 'article'
    link?: string
  }>
  links: Array<[number, number]>
  highlightedCategory?: string
}>()

const emit = defineEmits<{
  (e: 'node-click', node: any): void
}>()

const router = useRouter()
const canvasRef = ref<HTMLCanvasElement | null>(null)
const tooltipText = ref('')
const tooltipX = ref(0)
const tooltipY = ref(0)
const tooltipVisible = ref(false)
const modalVisible = ref(false)
const modalTitle = ref('')
const modalDesc = ref('')
const modalLink = ref('')

let width = 0
let height = 0
let stars: Star[] = []
let meteors: Meteor[] = []
let knowledgeNodes: KnowledgeNode[] = []
let hoveredNode: KnowledgeNode | null = null
let time = 0
let animationId = 0

// Mouse state
let mouseX = 0
let mouseY = 0
let targetMouseX = 0
let targetMouseY = 0
let realMouseX = 0
let realMouseY = 0

const starColors = ['#ffffff', '#ffe9c4', '#d4fbff', '#f4f5f0']

// ==================== Classes ====================

class Star {
  x = 0
  y = 0
  size = 0
  speedY = 0
  baseOpacity = 0
  opacity = 0
  blinkSpeed = 0
  blinkDir = 1
  color = '#fff'
  parallaxFactor = 0

  constructor() {
    this.reset(true)
  }

  reset(initial = false) {
    this.x = Math.random() * width
    this.y = initial ? Math.random() * height : -10
    this.size = Math.random() * 2
    this.speedY = (Math.random() * 0.2 + 0.05) * (this.size * 0.8)
    this.baseOpacity = Math.random() * 0.6 + 0.4
    this.opacity = this.baseOpacity
    this.blinkSpeed = Math.random() * 0.02 + 0.005
    this.blinkDir = Math.random() > 0.5 ? 1 : -1
    this.color = starColors[Math.floor(Math.random() * starColors.length)]
    this.parallaxFactor = this.size * 0.5
  }

  update() {
    this.y += this.speedY
    this.opacity += this.blinkSpeed * this.blinkDir
    if (this.opacity >= 1) { this.opacity = 1; this.blinkDir = -1 }
    else if (this.opacity <= 0.1) { this.opacity = 0.1; this.blinkDir = 1 }
    if (this.y > height + 10) this.reset()
  }

  draw(ctx: CanvasRenderingContext2D) {
    const offsetX = mouseX * 50 * this.parallaxFactor
    const offsetY = mouseY * 50 * this.parallaxFactor
    ctx.beginPath()
    ctx.arc(this.x + offsetX, this.y + offsetY, this.size, 0, Math.PI * 2)
    ctx.fillStyle = this.color
    ctx.globalAlpha = this.opacity
    ctx.fill()
    ctx.globalAlpha = 1
  }
}

class Meteor {
  x = 0
  y = 0
  length = 0
  speed = 0
  angle = 0
  opacity = 0
  active = false
  resetTimer = 0

  constructor() {
    this.scheduleReset()
  }

  scheduleReset() {
    this.active = false
    this.opacity = 0
    this.resetTimer = window.setTimeout(() => {
      this.active = true
      this.opacity = 1
      this.x = Math.random() * width + width * 0.2
      this.y = -50
      this.length = Math.random() * 80 + 40
      this.speed = Math.random() * 5 + 10
      this.angle = (Math.random() * 15 + 35) * (Math.PI / 180)
    }, Math.random() * 5000 + 2000)
  }

  update() {
    if (!this.active) return
    this.x -= this.speed * Math.cos(this.angle)
    this.y += this.speed * Math.sin(this.angle)
    this.opacity -= 0.015
    if (this.opacity <= 0 || this.x < -100 || this.y > height + 100) {
      this.scheduleReset()
    }
  }

  draw(ctx: CanvasRenderingContext2D) {
    if (!this.active || this.opacity <= 0) return
    const endX = this.x + this.length * Math.cos(this.angle)
    const endY = this.y - this.length * Math.sin(this.angle)
    const gradient = ctx.createLinearGradient(this.x, this.y, endX, endY)
    gradient.addColorStop(0, `rgba(255, 255, 255, ${this.opacity})`)
    gradient.addColorStop(1, 'rgba(255, 255, 255, 0)')
    ctx.beginPath()
    ctx.moveTo(this.x, this.y)
    ctx.lineTo(endX, endY)
    ctx.strokeStyle = gradient
    ctx.lineWidth = 1.5
    ctx.lineCap = 'round'
    ctx.stroke()
  }
}

class KnowledgeNode {
  id: number
  label: string
  desc: string
  rx: number
  ry: number
  size: number
  color: string
  type: 'category' | 'article'
  link?: string
  seed: number
  highlighted: boolean
  dimmed: boolean

  constructor(data: any) {
    this.id = data.id
    this.label = data.label
    this.desc = data.desc
    this.rx = data.rx
    this.ry = data.ry
    this.size = data.size
    this.color = data.color
    this.type = data.type
    this.link = data.link
    this.seed = Math.random() * Math.PI * 2
    this.highlighted = false
    this.dimmed = false
  }

  getScreenPos() {
    const baseX = this.rx * width
    const baseY = this.ry * height
    const parallaxFactor = 0.4
    const offsetX = mouseX * 50 * parallaxFactor
    const offsetY = mouseY * 50 * parallaxFactor
    return { x: baseX + offsetX, y: baseY + offsetY }
  }

  draw(ctx: CanvasRenderingContext2D) {
    const pos = this.getScreenPos()
    const isHovered = this === hoveredNode
    const isActive = isHovered || this.highlighted
    const alpha = this.dimmed ? 0.2 : 1

    ctx.save()
    ctx.globalAlpha = alpha
    ctx.translate(pos.x, pos.y)

    // 1. Glow
    const glowRadius = isActive ? this.size * 6 : this.size * 3
    const gradient = ctx.createRadialGradient(0, 0, 0, 0, 0, glowRadius)
    const r = parseInt(this.color.slice(1, 3), 16)
    const g = parseInt(this.color.slice(3, 5), 16)
    const b = parseInt(this.color.slice(5, 7), 16)
    gradient.addColorStop(0, `rgba(${r}, ${g}, ${b}, ${isActive ? 0.5 : 0.15})`)
    gradient.addColorStop(1, `rgba(${r}, ${g}, ${b}, 0)`)
    ctx.beginPath()
    ctx.arc(0, 0, glowRadius, 0, Math.PI * 2)
    ctx.fillStyle = gradient
    ctx.fill()

    // 2. Inner dashed ring (rotating)
    ctx.save()
    ctx.rotate(time * 0.0005 + this.seed)
    ctx.beginPath()
    ctx.arc(0, 0, this.size * 1.5, 0, Math.PI * 2)
    ctx.strokeStyle = isActive ? '#ffffff' : this.color
    ctx.lineWidth = 1
    ctx.setLineDash([3, 4])
    ctx.stroke()
    ctx.restore()

    // 3. Outer solid ring (counter-rotating)
    ctx.save()
    ctx.rotate(-time * 0.0003 - this.seed)
    ctx.beginPath()
    ctx.arc(0, 0, this.size * 2.5, 0, Math.PI * 2)
    ctx.strokeStyle = `rgba(255, 255, 255, ${isActive ? 0.5 : 0.1})`
    ctx.lineWidth = 0.5
    ctx.setLineDash([])
    ctx.stroke()
    // Satellite dot
    ctx.beginPath()
    ctx.arc(this.size * 2.5, 0, 1.5, 0, Math.PI * 2)
    ctx.fillStyle = isActive ? '#ffffff' : this.color
    ctx.fill()
    ctx.restore()

    // 4. Center core
    ctx.beginPath()
    ctx.arc(0, 0, isActive ? 3 : 2, 0, Math.PI * 2)
    ctx.fillStyle = '#ffffff'
    ctx.shadowBlur = 10
    ctx.shadowColor = this.color
    ctx.fill()
    ctx.shadowBlur = 0

    ctx.restore()

    // 5. Label
    ctx.fillStyle = isActive ? '#ffffff' : 'rgba(255, 255, 255, 0.5)'
    ctx.font = isActive ? "400 15px 'Segoe UI', sans-serif" : "300 13px 'Segoe UI', sans-serif"
    ctx.textAlign = 'center'
    ctx.shadowBlur = 6
    ctx.shadowColor = '#000000'
    ctx.fillText(this.label, pos.x, pos.y + this.size * 3 + 12)
    ctx.shadowBlur = 0
  }

  checkHover() {
    const pos = this.getScreenPos()
    const dist = Math.hypot(realMouseX - pos.x, realMouseY - pos.y)
    return dist < this.size * 4
  }
}

// ==================== Init ====================

function initStars() {
  stars = []
  const numStars = Math.floor((width * height) / 1500)
  for (let i = 0; i < numStars; i++) {
    stars.push(new Star())
  }
  meteors = []
  for (let i = 0; i < 3; i++) {
    meteors.push(new Meteor())
  }
}

function initNodes() {
  knowledgeNodes = props.nodes.map(d => new KnowledgeNode(d))
  updateHighlightState()
}

function updateHighlightState() {
  const highlighted = props.highlightedCategory
  for (const node of knowledgeNodes) {
    if (!highlighted) {
      node.highlighted = false
      node.dimmed = false
    } else {
      node.highlighted = node.label === highlighted
      node.dimmed = node.label !== highlighted
    }
  }
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  width = window.innerWidth
  height = window.innerHeight
  canvas.width = width
  canvas.height = height
  initStars()
}

// ==================== Animation ====================

function animate() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, width, height)
  time += 16

  // Smooth mouse
  mouseX += (targetMouseX - mouseX) * 0.05
  mouseY += (targetMouseY - mouseY) * 0.05

  // Stars
  for (const star of stars) {
    star.update()
    star.draw(ctx)
  }

  // Meteors
  for (const meteor of meteors) {
    meteor.update()
    meteor.draw(ctx)
  }

  // Knowledge node positions
  const nodePositions = new Map<number, { x: number; y: number }>()
  for (const node of knowledgeNodes) {
    nodePositions.set(node.id, node.getScreenPos())
  }

  // Draw links
  for (const [fromId, toId] of props.links) {
    const fromNode = knowledgeNodes.find(n => n.id === fromId)
    const toNode = knowledgeNodes.find(n => n.id === toId)
    const pos1 = nodePositions.get(fromId)
    const pos2 = nodePositions.get(toId)
    if (pos1 && pos2 && fromNode && toNode) {
      ctx.beginPath()
      ctx.moveTo(pos1.x, pos1.y)
      ctx.lineTo(pos2.x, pos2.y)
      if (hoveredNode && (hoveredNode.id === fromId || hoveredNode.id === toId)) {
        const grad = ctx.createLinearGradient(pos1.x, pos1.y, pos2.x, pos2.y)
        grad.addColorStop(0, fromNode.color + '99')
        grad.addColorStop(1, toNode.color + '99')
        ctx.strokeStyle = grad
        ctx.lineWidth = 1.2
        ctx.shadowBlur = 5
        ctx.shadowColor = hoveredNode.color
      } else {
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)'
        ctx.lineWidth = 0.5
        ctx.shadowBlur = 0
      }
      ctx.stroke()
      ctx.shadowBlur = 0
    }
  }

  // Check hover
  hoveredNode = null
  for (let i = knowledgeNodes.length - 1; i >= 0; i--) {
    if (knowledgeNodes[i].checkHover()) {
      hoveredNode = knowledgeNodes[i]
      break
    }
  }

  // Draw nodes
  for (const node of knowledgeNodes) {
    node.draw(ctx)
  }

  // Update tooltip
  if (hoveredNode && !modalVisible.value) {
    tooltipText.value = '点击查看: ' + hoveredNode.label
    tooltipX.value = realMouseX
    tooltipY.value = realMouseY - 20
    tooltipVisible.value = true
    canvas.style.cursor = 'pointer'
  } else {
    tooltipVisible.value = false
    canvas.style.cursor = 'default'
  }

  animationId = requestAnimationFrame(animate)
}

// ==================== Events ====================

function onMouseMove(e: MouseEvent) {
  targetMouseX = (e.clientX / width) * 2 - 1
  targetMouseY = (e.clientY / height) * 2 - 1
  realMouseX = e.clientX
  realMouseY = e.clientY
}

function onTouchMove(e: TouchEvent) {
  if (e.touches.length > 0) {
    targetMouseX = (e.touches[0].clientX / width) * 2 - 1
    targetMouseY = (e.touches[0].clientY / height) * 2 - 1
    realMouseX = e.touches[0].clientX
    realMouseY = e.touches[0].clientY
  }
}

function onClick() {
  if (hoveredNode) {
    modalTitle.value = hoveredNode.label
    modalDesc.value = hoveredNode.desc
    modalLink.value = hoveredNode.link || ''
    modalVisible.value = true
    emit('node-click', hoveredNode)
  } else if (modalVisible.value) {
    closeModal()
  }
}

function closeModal() {
  modalVisible.value = false
}

function navigateToLink() {
  if (modalLink.value) {
    router.push(modalLink.value)
    closeModal()
  }
}

// ==================== Lifecycle ====================

watch(() => props.nodes, () => {
  initNodes()
}, { deep: true })

watch(() => props.highlightedCategory, () => {
  updateHighlightState()
})

onMounted(() => {
  resize()
  initNodes()
  window.addEventListener('resize', resize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('touchmove', onTouchMove)
  animationId = requestAnimationFrame(animate)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', resize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('touchmove', onTouchMove)
  for (const meteor of meteors) {
    clearTimeout(meteor.resetTimer)
  }
})
</script>

<template>
  <div class="starfield-container">
    <canvas ref="canvasRef" class="starfield-canvas" @click="onClick"></canvas>

    <!-- Tooltip -->
    <div
      class="starfield-tooltip"
      :style="{
        left: tooltipX + 'px',
        top: tooltipY + 'px',
        opacity: tooltipVisible ? 1 : 0
      }"
    >
      {{ tooltipText }}
    </div>

    <!-- Modal -->
    <Transition name="modal-fade">
      <div v-if="modalVisible" class="starfield-modal-overlay" @click.self="closeModal">
        <div class="starfield-modal">
          <h2 class="starfield-modal__title">{{ modalTitle }}</h2>
          <p class="starfield-modal__desc">{{ modalDesc }}</p>
          <div class="starfield-modal__actions">
            <button class="starfield-modal__btn" @click="closeModal">关闭</button>
            <button
              v-if="modalLink"
              class="starfield-modal__btn starfield-modal__btn--primary"
              @click="navigateToLink"
            >
              查看详情
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.starfield-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: radial-gradient(ellipse at bottom, #0d1d31 0%, #0c0d13 100%);
}

.starfield-canvas {
  display: block;
  width: 100%;
  height: 100%;
}

.starfield-tooltip {
  position: absolute;
  background: rgba(13, 29, 49, 0.9);
  border: 1px solid rgba(100, 200, 255, 0.5);
  color: #fff;
  padding: 10px 15px;
  border-radius: 8px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
  transform: translate(-50%, -120%);
  font-size: 14px;
  box-shadow: 0 0 15px rgba(0, 150, 255, 0.3);
  z-index: 10;
  backdrop-filter: blur(4px);
  white-space: nowrap;
}

.starfield-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
}

.starfield-modal {
  background: linear-gradient(135deg, rgba(15, 32, 55, 0.95), rgba(8, 15, 25, 0.95));
  border: 1px solid rgba(100, 200, 255, 0.3);
  padding: 30px;
  border-radius: 12px;
  color: #fff;
  max-width: 400px;
  width: 80%;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.8), 0 0 20px rgba(0, 150, 255, 0.2);
}

.starfield-modal__title {
  margin: 0 0 15px 0;
  color: #d4fbff;
  font-weight: 400;
  letter-spacing: 1px;
  font-size: 1.2rem;
}

.starfield-modal__desc {
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 20px;
  font-size: 0.92rem;
}

.starfield-modal__actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.starfield-modal__btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: #fff;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 0.88rem;
}

.starfield-modal__btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.starfield-modal__btn--primary {
  background: var(--accent, #b85c38);
}

.starfield-modal__btn--primary:hover {
  background: var(--accent-hover, #a04e2e);
}

.modal-fade-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.modal-fade-leave-active {
  transition: all 0.2s ease-in;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
.modal-fade-enter-from .starfield-modal {
  transform: scale(0.9);
}
</style>
