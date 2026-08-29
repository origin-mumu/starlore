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
  highlightedNodes?: Array<number>
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

const zoom = ref(1.0)
const panX = ref(0)
const panY = ref(0)
let draggingNode: KnowledgeNode | null = null
let isPanning = false
let lastMouseX = 0
let lastMouseY = 0

let width = 0
let height = 0
let stars: Star[] = []
let meteors: Meteor[] = []
let knowledgeNodes: KnowledgeNode[] = []
let hoveredNode: KnowledgeNode | null = null
let time = 0
let animationId = 0
let currentDamping = 0.1

// Mouse state
let mouseX = 0
let mouseY = 0
let targetMouseX = 0
let targetMouseY = 0
let realMouseX = 0
let realMouseY = 0
let lastTouchTime = 0

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

  // Physics properties
  x = 0
  y = 0
  vx = 0
  vy = 0
  fx = 0
  fy = 0
  isDragging = false

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
    return { x: this.x, y: this.y }
  }

  draw(ctx: CanvasRenderingContext2D) {
    const isHovered = this === hoveredNode
    const isActive = isHovered || this.highlighted
    const alpha = this.dimmed ? 0.2 : 1

    ctx.save()
    ctx.globalAlpha = alpha
    ctx.translate(this.x, this.y)

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
    ctx.fillText(this.label, this.x, this.y + this.size * 3 + 12)
    ctx.shadowBlur = 0
  }

  checkHover(mx: number, my: number) {
    const dist = Math.hypot(mx - this.x, my - this.y)
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
  currentDamping = 0.2
  const oldMap = new Map<number | string, { x: number; y: number; vx: number; vy: number }>()
  for (const n of knowledgeNodes) {
    oldMap.set(n.id, { x: n.x, y: n.y, vx: n.vx, vy: n.vy })
  }

  knowledgeNodes = props.nodes.map(d => {
    const node = new KnowledgeNode(d)
    const old = oldMap.get(d.id)
    if (old) {
      node.x = old.x
      node.y = old.y
      node.vx = old.vx
      node.vy = old.vy
    } else {
      node.x = d.rx * (width || window.innerWidth)
      node.y = d.ry * (height || window.innerHeight)
      node.vx = 0
      node.vy = 0
    }
    return node
  })
  updateHighlightState()
}

function updatePhysics() {
  // 1. Reset forces
  for (const node of knowledgeNodes) {
    node.fx = 0
    node.fy = 0
  }

  // 2. Repulsion (between all nodes)
  const repulsionStrength = 2200
  const len = knowledgeNodes.length
  for (let i = 0; i < len; i++) {
    const n1 = knowledgeNodes[i]
    for (let j = i + 1; j < len; j++) {
      const n2 = knowledgeNodes[j]
      const dx = n2.x - n1.x
      const dy = n2.y - n1.y
      const distSq = dx * dx + dy * dy + 1500
      const dist = Math.sqrt(distSq)
      if (dist < 350) {
        const force = (repulsionStrength * (n1.size + n2.size)) / distSq
        const fx = (dx / dist) * force
        const fy = (dy / dist) * force
        n1.fx -= fx
        n1.fy -= fy
        n2.fx += fx
        n2.fy += fy
      }
    }
  }

  // 3. Attraction (along links)
  const springStrength = 0.04
  const linkDistance = 130
  for (const [fromId, toId] of props.links) {
    const n1 = knowledgeNodes.find(n => n.id === fromId)
    const n2 = knowledgeNodes.find(n => n.id === toId)
    if (n1 && n2) {
      const dx = n2.x - n1.x
      const dy = n2.y - n1.y
      const dist = Math.hypot(dx, dy) + 0.1
      const force = (dist - linkDistance) * springStrength
      const fx = (dx / dist) * force
      const fy = (dy / dist) * force
      n1.fx += fx
      n1.fy += fy
      n2.fx -= fx
      n2.fy -= fy
    }
  }

  // 4. Centering Force (gravity)
  const gravityStrength = 0.012
  const cx = width / 2
  const cy = height / 2
  for (const node of knowledgeNodes) {
    const dx = cx - node.x
    const dy = cy - node.y
    node.fx += dx * gravityStrength
    node.fy += dy * gravityStrength
  }

  // 5. Integrate positions
  const targetDamping = 0.85
  if (currentDamping < targetDamping) {
    currentDamping += 0.012
  }
  for (const node of knowledgeNodes) {
    if (node.isDragging) continue
    node.vx = (node.vx + node.fx) * currentDamping
    node.vy = (node.vy + node.fy) * currentDamping
    node.x += node.vx
    node.y += node.vy

    // Restrict to viewport boundaries
    const pad = 40
    if (node.x < pad) { node.x = pad; node.vx = 0 }
    if (node.x > width - pad) { node.x = width - pad; node.vx = 0 }
    if (node.y < pad) { node.y = pad; node.vy = 0 }
    if (node.y > height - pad) { node.y = height - pad; node.vy = 0 }
  }
}

function updateHighlightState() {
  const highlights = props.highlightedNodes
  const hasHighlights = highlights && highlights.length > 0
  for (const node of knowledgeNodes) {
    if (!hasHighlights) {
      node.highlighted = false
      node.dimmed = false
    } else {
      node.highlighted = highlights.includes(node.id)
      node.dimmed = !highlights.includes(node.id)
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

// Get mapped coordinates in graph space from screen coordinates
function getGraphCoords(mx: number, my: number) {
  const parallaxX = mouseX * 25
  const parallaxY = mouseY * 25
  const gx = (mx - (width / 2 + panX.value + parallaxX)) / zoom.value + width / 2
  const gy = (my - (height / 2 + panY.value + parallaxY)) / zoom.value + height / 2
  return { x: gx, y: gy }
}

function animate() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 1. Run physics step
  updatePhysics()

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

  // 2. Save context and apply zoom/pan matrix
  ctx.save()
  const parallaxX = mouseX * 25
  const parallaxY = mouseY * 25
  ctx.translate(width / 2 + panX.value + parallaxX, height / 2 + panY.value + parallaxY)
  ctx.scale(zoom.value, zoom.value)
  ctx.translate(-width / 2, -height / 2)

  // Knowledge node positions in graph space
  const nodePositions = new Map<number, { x: number; y: number }>()
  for (const node of knowledgeNodes) {
    nodePositions.set(node.id, node.getScreenPos())
  }

  // Draw links — hover 时只显示与当前节点直接相连的线
  for (const [fromId, toId] of props.links) {
    const fromNode = knowledgeNodes.find(n => n.id === fromId)
    const toNode = knowledgeNodes.find(n => n.id === toId)
    const pos1 = nodePositions.get(fromId)
    const pos2 = nodePositions.get(toId)
    if (pos1 && pos2 && fromNode && toNode) {
      const isConnected = hoveredNode && (hoveredNode.id === fromId || hoveredNode.id === toId)

      // 有 hover 时，只画与 hover 节点相连的线，其余隐藏
      if (hoveredNode && !isConnected) continue

      ctx.beginPath()
      ctx.moveTo(pos1.x, pos1.y)
      ctx.lineTo(pos2.x, pos2.y)
      if (isConnected) {
        const grad = ctx.createLinearGradient(pos1.x, pos1.y, pos2.x, pos2.y)
        grad.addColorStop(0, fromNode.color + '99')
        grad.addColorStop(1, toNode.color + '99')
        ctx.strokeStyle = grad
        ctx.lineWidth = 1.2
        ctx.shadowBlur = 5
        ctx.shadowColor = hoveredNode?.color || '#ffffff'
      } else {
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)'
        ctx.lineWidth = 0.5
        ctx.shadowBlur = 0
      }
      ctx.stroke()
      ctx.shadowBlur = 0
    }
  }

  // Check hover (using transformed graph coordinates)
  hoveredNode = null
  const graphMouse = getGraphCoords(realMouseX, realMouseY)
  for (let i = knowledgeNodes.length - 1; i >= 0; i--) {
    if (knowledgeNodes[i].checkHover(graphMouse.x, graphMouse.y)) {
      hoveredNode = knowledgeNodes[i]
      break
    }
  }

  // Draw nodes
  for (const node of knowledgeNodes) {
    node.draw(ctx)
  }

  // 3. Restore context
  ctx.restore()

  // Update tooltip (drawn in screen space)
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

// ==================== Events ====================

function onMouseMove(e: MouseEvent) {
  targetMouseX = (e.clientX / width) * 2 - 1
  targetMouseY = (e.clientY / height) * 2 - 1
  
  const dx = e.clientX - lastMouseX
  const dy = e.clientY - lastMouseY
  lastMouseX = e.clientX
  lastMouseY = e.clientY

  realMouseX = e.clientX
  realMouseY = e.clientY

  const graphCoords = getGraphCoords(e.clientX, e.clientY)

  if (draggingNode) {
    draggingNode.x = graphCoords.x
    draggingNode.y = graphCoords.y
    draggingNode.vx = 0
    draggingNode.vy = 0
  } else if (isPanning) {
    panX.value += dx
    panY.value += dy
  }
}

function onTouchMove(e: TouchEvent) {
  if (e.touches.length > 0) {
    const t = e.touches[0]
    targetMouseX = (t.clientX / width) * 2 - 1
    targetMouseY = (t.clientY / height) * 2 - 1

    const dx = t.clientX - lastMouseX
    const dy = t.clientY - lastMouseY
    lastMouseX = t.clientX
    lastMouseY = t.clientY

    realMouseX = t.clientX
    realMouseY = t.clientY

    const graphCoords = getGraphCoords(t.clientX, t.clientY)

    if (draggingNode) {
      draggingNode.x = graphCoords.x
      draggingNode.y = graphCoords.y
      draggingNode.vx = 0
      draggingNode.vy = 0
    } else if (isPanning) {
      panX.value += dx
      panY.value += dy
    }
  }
}

function onMouseDown(e: MouseEvent) {
  lastMouseX = e.clientX
  lastMouseY = e.clientY

  const graphCoords = getGraphCoords(e.clientX, e.clientY)
  
  let hitNode: KnowledgeNode | null = null
  for (let i = knowledgeNodes.length - 1; i >= 0; i--) {
    if (knowledgeNodes[i].checkHover(graphCoords.x, graphCoords.y)) {
      hitNode = knowledgeNodes[i]
      break
    }
  }

  if (hitNode) {
    draggingNode = hitNode
    draggingNode.isDragging = true
  } else {
    isPanning = true
  }
}

function onMouseUp() {
  if (draggingNode) {
    draggingNode.isDragging = false
    draggingNode = null
  }
  isPanning = false
}

function onTouchStart(e: TouchEvent) {
  if (e.touches.length > 0) {
    const t = e.touches[0]
    lastMouseX = t.clientX
    lastMouseY = t.clientY
    realMouseX = t.clientX
    realMouseY = t.clientY

    const graphCoords = getGraphCoords(t.clientX, t.clientY)

    let hitNode: KnowledgeNode | null = null
    for (let i = knowledgeNodes.length - 1; i >= 0; i--) {
      if (knowledgeNodes[i].checkHover(graphCoords.x, graphCoords.y)) {
        hitNode = knowledgeNodes[i]
        break
      }
    }

    if (hitNode) {
      draggingNode = hitNode
      draggingNode.isDragging = true
    } else {
      isPanning = true
    }

    const now = Date.now()
    if (now - lastTouchTime < 350) {
      onDblClick({ clientX: t.clientX, clientY: t.clientY } as MouseEvent)
    }
    lastTouchTime = now
  }
}

function onTouchEnd() {
  if (draggingNode) {
    draggingNode.isDragging = false
    draggingNode = null
  }
  isPanning = false
}

function onWheel(e: WheelEvent) {
  e.preventDefault()
  const zoomFactor = 1.08
  const nextZoom = e.deltaY < 0 ? zoom.value * zoomFactor : zoom.value / zoomFactor
  zoom.value = Math.max(0.3, Math.min(3.0, nextZoom))
}

function onDblClick(e: MouseEvent) {
  const graphCoords = getGraphCoords(e.clientX, e.clientY)
  let hitNode = false
  for (const node of knowledgeNodes) {
    if (node.checkHover(graphCoords.x, graphCoords.y)) {
      hitNode = true
      break
    }
  }
  if (!hitNode) {
    zoom.value = 1.0
    panX.value = 0
    panY.value = 0
  }
}

function onClick(e: MouseEvent) {
  const graphCoords = getGraphCoords(e.clientX, e.clientY)
  let hitNode: KnowledgeNode | null = null
  for (let i = knowledgeNodes.length - 1; i >= 0; i--) {
    if (knowledgeNodes[i].checkHover(graphCoords.x, graphCoords.y)) {
      hitNode = knowledgeNodes[i]
      break
    }
  }

  if (hitNode) {
    modalTitle.value = hitNode.label
    modalDesc.value = hitNode.desc
    modalLink.value = hitNode.link || ''
    modalVisible.value = true
    emit('node-click', hitNode)
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

watch(() => props.highlightedNodes, () => {
  updateHighlightState()
}, { deep: true })

onMounted(() => {
  resize()
  initNodes()
  window.addEventListener('resize', resize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('touchmove', onTouchMove)
  window.addEventListener('mouseup', onMouseUp)
  window.addEventListener('touchend', onTouchEnd)
  
  const canvas = canvasRef.value
  if (canvas) {
    canvas.addEventListener('wheel', onWheel, { passive: false })
  }
  
  animationId = requestAnimationFrame(animate)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', resize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('touchmove', onTouchMove)
  window.removeEventListener('mouseup', onMouseUp)
  window.removeEventListener('touchend', onTouchEnd)
  
  const canvas = canvasRef.value
  if (canvas) {
    canvas.removeEventListener('wheel', onWheel)
  }
  
  for (const meteor of meteors) {
    clearTimeout(meteor.resetTimer)
  }
})
</script>

<template>
  <div class="starfield-container">
    <canvas
      ref="canvasRef"
      class="starfield-canvas"
      @mousedown="onMouseDown"
      @mouseup="onMouseUp"
      @mouseleave="onMouseUp"
      @touchstart="onTouchStart"
      @touchend="onTouchEnd"
      @touchcancel="onTouchEnd"
      @dblclick="onDblClick"
      @click="onClick"
    ></canvas>

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

<style scoped src="./StarfieldCanvas.css"></style>

