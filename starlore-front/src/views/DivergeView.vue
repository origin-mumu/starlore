<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { divergeWord } from '@/api/diverge'
import { getAiQuota } from '@/api/ai'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ─── Types ────────────────────────────────────────────
interface GraphNode {
  id: string
  word: string
  wordEn: string
  x: number
  y: number
  vx: number
  vy: number
  parentId: string | null
  children: string[]
  expanded: boolean
  isRoot: boolean
  scale: number
  opacity: number
  floatOffset: number
}

interface HistoryEntry {
  word: string
  nodes: Record<string, GraphNode>
  rootId: string
  timestamp: number
}

interface UndoEntry {
  parentId: string
  childIds: string[]
}

// ─── State ────────────────────────────────────────────
const nodes = reactive<Record<string, GraphNode>>({})
const rootId = ref<string | null>(null)
const viewTransform = reactive({ x: 0, y: 0, scale: 1 })
const history = ref<HistoryEntry[]>([])
const undoStack = ref<UndoEntry[]>([])
const themeStore = useThemeStore()
const inputWord = ref('')
const inputState = ref<'center' | 'docked'>('center')
const selectedNodeId = ref<string | null>(null)
const loading = ref(false)
const loadingNodeId = ref<string | null>(null)
const historyDrawerOpen = ref(false)
const canvasRef = ref<HTMLElement | null>(null)

// ─── AI 配额 ────────────────────────────────────────────
const aiQuotaDaily = ref(10)
const aiQuotaRemaining = ref(10)
const aiQuotaExceeded = ref(false)
const quotaError = ref('')

async function refreshDivergeQuota() {
  try {
    const res: any = await getAiQuota()
    const q = res.data
    aiQuotaDaily.value = q.dailyLimit
    aiQuotaRemaining.value = q.remaining
    aiQuotaExceeded.value = !q.isAdmin && q.remaining <= 0
  } catch {
    // ignore
  }
}

// Drag state
let isDragging = false
let dragTarget: 'canvas' | string | null = null
let dragStartX = 0
let dragStartY = 0
let dragStartTx = 0
let dragStartTy = 0
let dragNodeStartX = 0
let dragNodeStartY = 0
let hasMoved = false

// Physics
let animFrameId: number | null = null
let dragParentOffsetMap = new Map<string, { dx: number; dy: number }>()
const SPRING_K = 0.08
const SPRING_DAMPING = 0.7
const COLLISION_DIST = 80
const COLLISION_FORCE = 0.5
const INERTIA_DECAY = 0.95

// ─── Computed ─────────────────────────────────────────
const hasNodes = computed(() => Object.keys(nodes).length > 0)
const nodeList = computed(() => Object.values(nodes))
const scalePercent = computed(() => Math.round(viewTransform.scale * 100))

// Convert canvas coords to screen coords
const canvasToScreen = (cx: number, cy: number) => ({
  x: cx * viewTransform.scale + viewTransform.x,
  y: cy * viewTransform.scale + viewTransform.y,
})

// Visible connections (parent → child) in screen coords
const connections = computed(() => {
  const conns: { x1: number; y1: number; x2: number; y2: number; id: string }[] = []
  for (const node of Object.values(nodes)) {
    if (node.parentId && nodes[node.parentId]) {
      const parent = nodes[node.parentId]
      const from = canvasToScreen(parent.x, parent.y)
      const to = canvasToScreen(node.x, node.y)
      conns.push({
        x1: from.x,
        y1: from.y,
        x2: to.x,
        y2: to.y,
        id: `${parent.id}-${node.id}`,
      })
    }
  }
  return conns
})

// Debug

// ─── Helpers ──────────────────────────────────────────
let idCounter = 0
const genId = () => `n-${Date.now()}-${++idCounter}`

const screenToWorld = (sx: number, sy: number) => ({
  x: (sx - viewTransform.x) / viewTransform.scale,
  y: (sy - viewTransform.y) / viewTransform.scale,
})

const getNodeById = (id: string) => nodes[id] || null

// ─── History (localStorage) ──────────────────────────
const HISTORY_KEY = 'diverge-history'

const loadHistory = () => {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    if (raw) history.value = JSON.parse(raw)
  } catch {
    /* ignore */
  }
}

const saveHistory = () => {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value))
}

const snapshotNodes = () => {
  const raw = JSON.parse(JSON.stringify(nodes)) as Record<string, GraphNode>
  // Ensure all child nodes are visible (animation may not have completed when saved)
  for (const val of Object.values(raw)) {
    if (!val.isRoot) {
      val.scale = 1
      val.opacity = 1
    }
  }
  return raw
}

const addToHistory = (word: string) => {
  const entry: HistoryEntry = {
    word,
    nodes: snapshotNodes(),
    rootId: rootId.value!,
    timestamp: Date.now(),
  }
  history.value.unshift(entry)
  if (history.value.length > 20) history.value.pop()
  saveHistory()
}

const updateLatestHistory = () => {
  if (!history.value.length || !rootId.value) return
  const currentWord = nodes[rootId.value]?.word
  if (history.value[0].word !== currentWord) return
  history.value[0].nodes = snapshotNodes()
  history.value[0].timestamp = Date.now()
  saveHistory()
}

const restoreHistory = (entry: HistoryEntry) => {
  // Clear existing keys
  for (const key of Object.keys(nodes)) delete nodes[key]
  rootId.value = null
  selectedNodeId.value = null
  undoStack.value = []
  inputState.value = 'center'
  inputWord.value = ''

  nextTick(() => {
    const restored = JSON.parse(JSON.stringify(entry.nodes))
    // Add each node individually to ensure Vue reactivity
    for (const [key, val] of Object.entries(restored)) {
      const node = val as GraphNode
      nodes[key] = node
      // Restore visibility for all non-root nodes (animation may not have completed when saved)
      if (!node.isRoot) {
        nodes[key].scale = 1
        nodes[key].opacity = 1
      }
    }
    rootId.value = entry.rootId
    inputWord.value = entry.word
    inputState.value = 'docked'
    historyDrawerOpen.value = false
    fitView()
  })
}

const deleteHistory = (index: number) => {
  history.value.splice(index, 1)
  saveHistory()
}

// ─── Canvas Transform ─────────────────────────────────
const zoomAt = (cx: number, cy: number, delta: number) => {
  const oldScale = viewTransform.scale
  const newScale = Math.min(5, Math.max(0.2, oldScale * (1 + delta)))
  const ratio = newScale / oldScale
  viewTransform.x = cx - (cx - viewTransform.x) * ratio
  viewTransform.y = cy - (cy - viewTransform.y) * ratio
  viewTransform.scale = newScale
}

const zoomIn = () => {
  const cx = window.innerWidth / 2
  const cy = window.innerHeight / 2
  zoomAt(cx, cy, 0.2)
}

const zoomOut = () => {
  const cx = window.innerWidth / 2
  const cy = window.innerHeight / 2
  zoomAt(cx, cy, -0.2)
}

const fitView = () => {
  const vals = Object.values(nodes)
  if (!vals.length) return

  let minX = Infinity,
    maxX = -Infinity,
    minY = Infinity,
    maxY = -Infinity
  for (const n of vals) {
    minX = Math.min(minX, n.x - 60)
    maxX = Math.max(maxX, n.x + 60)
    minY = Math.min(minY, n.y - 60)
    maxY = Math.max(maxY, n.y + 60)
  }

  const padding = 120
  const contentW = maxX - minX + padding * 2
  const contentH = maxY - minY + padding * 2
  const vw = window.innerWidth
  const vh = window.innerHeight - 56

  const scale = Math.min(2, Math.max(0.2, Math.min(vw / contentW, vh / contentH)))
  const cx = (minX + maxX) / 2
  const cy = (minY + maxY) / 2

  viewTransform.scale = scale
  viewTransform.x = vw / 2 - cx * scale
  viewTransform.y = vh / 2 - cy * scale + 56
}

const clearCanvas = () => {
  for (const key of Object.keys(nodes)) delete nodes[key]
  rootId.value = null
  selectedNodeId.value = null
  undoStack.value = []
  inputState.value = 'center'
  inputWord.value = ''
}

// ─── Node Layout ──────────────────────────────────────
const layoutChildren = (parentId: string, count: number) => {
  const parent = nodes[parentId]
  if (!parent) return

  const existingChildren = parent.children.filter(id => nodes[id])
  const radius = 160 + count * 10
  const startAngle =
    existingChildren.length > 0
      ? Math.atan2(
          nodes[existingChildren[0]].y - parent.y,
          nodes[existingChildren[0]].x - parent.x
        ) - Math.PI
      : -Math.PI / 2

  const positions: { x: number; y: number }[] = []
  for (let i = 0; i < count; i++) {
    const angle = startAngle + (i / count) * Math.PI * 2
    positions.push({
      x: parent.x + Math.cos(angle) * radius,
      y: parent.y + Math.sin(angle) * radius,
    })
  }
  return positions
}

// ─── API: Expand Node ─────────────────────────────────
const expandNode = async (nodeId: string) => {
  const node = nodes[nodeId]
  if (!node || loading.value) return

  // Toggle collapse if already expanded
  if (node.expanded && node.children.length > 0) {
    collapseNode(nodeId)
    return
  }

  if (!userStore.isLoggedIn) {
    quotaError.value = '请先登录后使用 AI 创意发散功能'
    return
  }

  loading.value = true
  loadingNodeId.value = nodeId
  selectedNodeId.value = null

  try {
    const res = await divergeWord(node.wordEn || node.word)
    const pairs = res.pairs

    const positions = layoutChildren(nodeId, pairs.length)!
    const newChildIds: string[] = []

    pairs.forEach((pair, i) => {
      const childId = genId()
      newChildIds.push(childId)
      nodes[childId] = {
        id: childId,
        word: pair.zh,
        wordEn: pair.en,
        x: positions[i].x,
        y: positions[i].y,
        vx: 0,
        vy: 0,
        parentId: nodeId,
        children: [],
        expanded: false,
        isRoot: false,
        scale: 0,
        opacity: 0,
        floatOffset: Math.random() * Math.PI * 2,
      }

      // Animate in
      setTimeout(
        () => {
          if (nodes[childId]) {
            nodes[childId].scale = 1
            nodes[childId].opacity = 1
          }
        },
        50 + i * 80
      )
    })

    node.children = newChildIds
    node.expanded = true

    undoStack.value.push({ parentId: nodeId, childIds: newChildIds })
    updateLatestHistory()
  } catch (err: any) {
    console.error('Diverge error:', err)
    if (err?.message?.includes('401') || err?.response?.status === 401) {
      quotaError.value = '登录已过期，请重新登录'
    } else {
      quotaError.value = err?.message?.includes('次数已用尽') ? '今日 AI 创意发散次数已用尽' : ''
    }
    refreshDivergeQuota()
  } finally {
    loading.value = false
    loadingNodeId.value = null
  }
}

const collapseNode = (nodeId: string) => {
  const node = nodes[nodeId]
  if (!node) return

  const removeRecursive = (id: string) => {
    const n = nodes[id]
    if (!n) return
    n.children.forEach(removeRecursive)
    delete nodes[id]
  }

  node.children.forEach(removeRecursive)
  node.children = []
  node.expanded = false
  selectedNodeId.value = null
  updateLatestHistory()
}

// ─── Undo (Ctrl+Z) ───────────────────────────────────
const undo = () => {
  const entry = undoStack.value.pop()
  if (!entry) return

  const removeRecursive = (id: string) => {
    const n = nodes[id]
    if (!n) return
    n.children.forEach(removeRecursive)
    delete nodes[id]
  }

  entry.childIds.forEach(removeRecursive)
  const parent = nodes[entry.parentId]
  if (parent) {
    parent.children = []
    parent.expanded = false
  }
  updateLatestHistory()
}

// ─── Search / Input ───────────────────────────────────
const handleSearch = async () => {
  if (!userStore.isLoggedIn) return
  const word = inputWord.value.trim()
  if (!word || loading.value) return

  clearCanvas()
  loading.value = true

  try {
    const res = await divergeWord(word)
    const pairs = res.pairs

    const centerX = window.innerWidth / 2
    const centerY = window.innerHeight / 2

    const id = genId()
    rootId.value = id
    nodes[id] = {
      id,
      word,
      wordEn: word,
      x: centerX,
      y: centerY,
      vx: 0,
      vy: 0,
      parentId: null,
      children: [],
      expanded: false,
      isRoot: true,
      scale: 0,
      opacity: 0,
      floatOffset: 0,
    }

    setTimeout(() => {
      if (nodes[id]) {
        nodes[id].scale = 1
        nodes[id].opacity = 1
      }
    }, 50)

    inputState.value = 'docked'

    // Auto-expand root (need to release loading lock first)
    loading.value = false
    await nextTick()
    await expandNode(id)
    addToHistory(word)
    fitView()
  } catch (err: any) {
    console.error('Search error:', err)
    if (err?.message?.includes('401') || err?.response?.status === 401) {
      quotaError.value = '登录已过期，请重新登录'
    } else {
      quotaError.value = err?.message?.includes('次数已用尽') ? '今日 AI 创意发散次数已用尽' : ''
    }
    refreshDivergeQuota()
  } finally {
    loading.value = false
  }
}

// ─── Bezier Path ──────────────────────────────────────
const bezierPath = (x1: number, y1: number, x2: number, y2: number) => {
  const dx = x2 - x1
  const dy = y2 - y1
  const dist = Math.sqrt(dx * dx + dy * dy) || 1 // prevent NaN
  const curvature = Math.min(dist * 0.3, 80)

  // Perpendicular offset for curve
  const nx = -dy / dist
  const ny = dx / dist

  const cx1 = x1 + dx * 0.25 + nx * curvature * 0.3
  const cy1 = y1 + dy * 0.25 + ny * curvature * 0.3
  const cx2 = x1 + dx * 0.75 + nx * curvature * 0.3
  const cy2 = y1 + dy * 0.75 + ny * curvature * 0.3

  return `M ${x1} ${y1} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${x2} ${y2}`
}

// ─── Mouse / Touch Events (on document) ──────────────
const isUIElement = (el: EventTarget | null): boolean => {
  if (!(el instanceof HTMLElement)) return false
  return !!el.closest(
    '.input-area, .canvas-controls, .history-drawer, .history-overlay, .dark-toggle, .top-bar'
  )
}

const onPointerDown = (e: PointerEvent) => {
  if (isUIElement(e.target)) return

  const nodeEl = (e.target as HTMLElement).closest('.graph-node')
  if (nodeEl) {
    const nodeId = nodeEl.getAttribute('data-node-id')
    if (nodeId) {
      dragTarget = nodeId
      const node = nodes[nodeId]
      dragNodeStartX = node.x
      dragNodeStartY = node.y

      // Record offsets for children (spring physics)
      dragParentOffsetMap.clear()
      // Also snap dragged node to its parent so it springs back
      if (nodes[nodeId].parentId && nodes[nodes[nodeId].parentId!]) {
        const p = nodes[nodes[nodeId].parentId!]
        dragParentOffsetMap.set(nodeId, { dx: nodes[nodeId].x - p.x, dy: nodes[nodeId].y - p.y })
      }
      const collectDescendants = (id: string) => {
        const n = nodes[id]
        if (!n) return
        for (const cid of n.children) {
          if (nodes[cid]) {
            dragParentOffsetMap.set(cid, { dx: nodes[cid].x - n.x, dy: nodes[cid].y - n.y })
            collectDescendants(cid)
          }
        }
      }
      collectDescendants(nodeId)

      startPhysicsLoop()
    }
  } else {
    dragTarget = 'canvas'
    dragStartTx = viewTransform.x
    dragStartTy = viewTransform.y
  }

  dragStartX = e.clientX
  dragStartY = e.clientY
  isDragging = true
  hasMoved = false
}

const onPointerMove = (e: PointerEvent) => {
  if (!isDragging) return

  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true

  if (dragTarget === 'canvas') {
    viewTransform.x = dragStartTx + dx
    viewTransform.y = dragStartTy + dy
  } else if (dragTarget && nodes[dragTarget]) {
    const node = nodes[dragTarget]
    const worldDx = dx / viewTransform.scale
    const worldDy = dy / viewTransform.scale
    node.x = dragNodeStartX + worldDx
    node.y = dragNodeStartY + worldDy
  }
}

const onPointerUp = () => {
  if (dragTarget && dragTarget !== 'canvas' && nodes[dragTarget] && hasMoved) {
    // Give inertia to children
    const parent = nodes[dragTarget]
    const applyInertia = (id: string, depth: number) => {
      const n = nodes[id]
      if (!n) return
      // Small random velocity for organic feel
      // n.vx = (Math.random() - 0.5) * 2
      // n.vy = (Math.random() - 0.5) * 2
      n.vx = 0
      n.vy = 0
      for (const cid of n.children) {
        applyInertia(cid, depth + 1)
      }
    }
    for (const cid of parent.children) {
      applyInertia(cid, 0)
    }
  }

  // If click on node (no drag), toggle selection
  if (dragTarget && dragTarget !== 'canvas' && !hasMoved) {
    if (selectedNodeId.value === dragTarget) {
      selectedNodeId.value = null
    } else {
      selectedNodeId.value = dragTarget
    }
  }

  isDragging = false
  dragTarget = null
  dragParentOffsetMap.clear()
}

// ─── Physics Loop ─────────────────────────────────────
const startPhysicsLoop = () => {
  if (animFrameId) return
  const tick = () => {
    let needsUpdate = false

    for (const [childId, offset] of dragParentOffsetMap) {
      const child = nodes[childId]
      const parent = child.parentId ? nodes[child.parentId] : null
      if (!child || !parent) continue

      // Spring force toward target position
      const targetX = parent.x + offset.dx
      const targetY = parent.y + offset.dy
      const fx = (targetX - child.x) * SPRING_K
      const fy = (targetY - child.y) * SPRING_K

      child.vx = (child.vx + fx) * SPRING_DAMPING
      child.vy = (child.vy + fy) * SPRING_DAMPING
      child.x += child.vx
      child.y += child.vy

      // Collision avoidance with siblings
      for (const [otherId, otherOffset] of dragParentOffsetMap) {
        if (otherId === childId) continue
        const other = nodes[otherId]
        if (!other) continue
        const ddx = child.x - other.x
        const ddy = child.y - other.y
        const dist = Math.sqrt(ddx * ddx + ddy * ddy)
        if (dist < COLLISION_DIST && dist > 0) {
          const force = ((COLLISION_DIST - dist) / COLLISION_DIST) * COLLISION_FORCE
          child.vx += (ddx / dist) * force
          child.vy += (ddy / dist) * force
        }
      }

      if (Math.abs(child.vx) > 0.1 || Math.abs(child.vy) > 0.1) {
        needsUpdate = true
      }
    }

    // Inertia decay for all nodes
    for (const node of Object.values(nodes)) {
      if (Math.abs(node.vx) > 0.05 || Math.abs(node.vy) > 0.05) {
        node.vx *= INERTIA_DECAY
        node.vy *= INERTIA_DECAY
        node.x += node.vx
        node.y += node.vy
        needsUpdate = true
      } else {
        node.vx = 0
        node.vy = 0
      }
    }

    if (needsUpdate || dragParentOffsetMap.size > 0) {
      animFrameId = requestAnimationFrame(tick)
    } else {
      animFrameId = null
    }
  }
  animFrameId = requestAnimationFrame(tick)
}

// ─── Wheel Zoom ───────────────────────────────────────
const onWheel = (e: WheelEvent) => {
  if (isUIElement(e.target)) return
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  zoomAt(e.clientX, e.clientY, delta)
}

// ─── Keyboard ─────────────────────────────────────────
const onKeyDown = (e: KeyboardEvent) => {
  if (e.ctrlKey && e.key === 'z') {
    e.preventDefault()
    undo()
  }
}

// ─── Lifecycle ────────────────────────────────────────
onMounted(() => {
  if (userStore.isLoggedIn) {
    refreshDivergeQuota()
  }
  loadHistory()
  document.addEventListener('pointerdown', onPointerDown)
  document.addEventListener('pointermove', onPointerMove)
  document.addEventListener('pointerup', onPointerUp)
  document.addEventListener('wheel', onWheel, { passive: false })
  document.addEventListener('keydown', onKeyDown)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onPointerDown)
  document.removeEventListener('pointermove', onPointerMove)
  document.removeEventListener('pointerup', onPointerUp)
  document.removeEventListener('wheel', onWheel)
  document.removeEventListener('keydown', onKeyDown)
  if (animFrameId) cancelAnimationFrame(animFrameId)
})
</script>

<template>
  <div class="diverge-root">
    <!-- History toggle -->
    <button class="history-toggle" @click="historyDrawerOpen = !historyDrawerOpen">
      <span>&#9776;</span>
    </button>

    <!-- Welcome state -->
    <div v-if="!hasNodes" class="welcome">
      <div class="welcome-icon">&#10024;</div>
      <h2>创意发散</h2>
      <p v-if="userStore.isLoggedIn">输入一个词，开始发散联想</p>
      <p v-else>
        <router-link to="/login" class="guest-login-link">登录</router-link>后使用 AI 创意发散功能
      </p>
    </div>

    <!-- Canvas -->
    <div
      ref="canvasRef"
      class="canvas"
      :style="{
        transform: `translate(${viewTransform.x}px, ${viewTransform.y}px) scale(${viewTransform.scale})`,
        transformOrigin: '0 0',
      }"
    >
      <!-- Nodes -->
      <div
        v-for="node in nodeList"
        :key="node.id"
        :data-node-id="node.id"
        class="graph-node"
        :class="{
          'graph-node--root': node.isRoot,
          'graph-node--selected': selectedNodeId === node.id,
          'graph-node--loading': loadingNodeId === node.id,
        }"
        :style="{
          left: node.x + 'px',
          top: node.y + 'px',
          transform: `translate(-50%, -50%) scale(${node.scale})`,
          opacity: node.opacity,
        }"
      >
        <div class="node-content">
          <span class="node-zh">{{ node.word }}</span>
          <span class="node-en">{{ node.wordEn }}</span>
        </div>

        <!-- Child count badge -->
        <span v-if="node.children.length > 0 && !node.expanded" class="node-badge">
          {{ node.children.length }}
        </span>

        <!-- Expand button -->
        <button
          v-if="selectedNodeId === node.id"
          class="node-expand-btn"
          @click.stop="expandNode(node.id)"
          :disabled="loading"
        >
          <span v-if="loadingNodeId === node.id" class="spinner-sm"></span>
          <span v-else>{{ node.expanded ? '&#8722;' : '&#43;' }}</span>
        </button>
      </div>
    </div>

    <!-- SVG connections (screen coords, outside canvas) -->
    <svg class="connections-svg">
      <path
        v-for="conn in connections"
        :key="conn.id"
        :d="bezierPath(conn.x1, conn.y1, conn.x2, conn.y2)"
        fill="none"
        :stroke="'var(--accent)'"
        stroke-width="2"
        stroke-opacity="0.8"
      />
    </svg>

    <!-- Input area -->
    <div class="input-area" :class="{ 'input-area--docked': inputState === 'docked' }">
      <form @submit.prevent="handleSearch" class="input-form">
        <input
          v-model="inputWord"
          type="text"
          :placeholder="userStore.isLoggedIn ? '输入一个词，开始发散...' : '请先登录'"
          class="search-input"
          :disabled="loading || !userStore.isLoggedIn"
        />
        <button type="submit" class="search-btn" :disabled="loading || !inputWord.trim()">
          <span v-if="loading" class="spinner-sm"></span>
          <span v-else>&#10140;</span>
        </button>
      </form>
      <div v-if="quotaError" class="quota-error">
        {{ quotaError }}
        <router-link v-if="quotaError.includes('登录')" to="/login" class="quota-login-link">去登录</router-link>
      </div>
      <div v-else class="quota-tip">
        {{ aiQuotaExceeded ? '今日 AI 次数已用尽' : `今日剩余 ${aiQuotaRemaining} 次` }}
      </div>
    </div>

    <!-- Canvas controls -->
    <div class="canvas-controls">
      <button @click="zoomIn" title="放大">+</button>
      <span class="scale-display">{{ scalePercent }}%</span>
      <button @click="zoomOut" title="缩小">&#8722;</button>
      <button @click="fitView" title="适应视图">&#9634;</button>
      <button @click="clearCanvas" title="清空画布">&#10005;</button>
    </div>

    <!-- History drawer overlay -->
    <div v-if="historyDrawerOpen" class="history-overlay" @click="historyDrawerOpen = false"></div>

    <!-- History drawer -->
    <div class="history-drawer" :class="{ open: historyDrawerOpen }">
      <div class="drawer-header">
        <h3>历史记录</h3>
        <button @click="historyDrawerOpen = false" class="drawer-close">&times;</button>
      </div>
      <div class="drawer-body">
        <div v-if="history.length === 0" class="drawer-empty">暂无历史记录</div>
        <div
          v-for="(entry, i) in history"
          :key="entry.timestamp"
          class="history-item"
          @click="restoreHistory(entry)"
        >
          <div class="history-word">{{ entry.word }}</div>
          <div class="history-time">{{ new Date(entry.timestamp).toLocaleString('zh-CN') }}</div>
          <button class="history-delete" @click.stop="deleteHistory(i)">&times;</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ─── Root ──────────────────────────────────────────── */
.diverge-root {
  position: fixed;
  inset: 0;
  top: 60px;
  /* background: var(--canvas); */
  overflow: hidden;
  font-family: 'LXGW WenKai', 'Source Serif 4', serif;
  color: var(--ink);
  user-select: none;
}

/* ─── Canvas ───────────────────────────────────────── */
.canvas {
  position: absolute;
  width: 0;
  height: 0;
  will-change: transform;
  z-index: 2;
}

.connections-svg {
  position: fixed;
  left: 0;
  top: 60px;
  width: 100vw;
  height: calc(100vh - 60px);
  pointer-events: none;
  z-index: 1;
}

/* ─── Nodes ────────────────────────────────────────── */
.graph-node {
  position: absolute;
  cursor: grab;
  transition:
    box-shadow 0.25s ease,
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  animation: nodeFloat 6s ease-in-out infinite;
  animation-delay: calc(var(--float-offset, 0) * 1s);
}

.graph-node:active {
  cursor: grabbing;
}

@keyframes nodeFloat {
  0%,
  100% {
    translate: 0 0;
  }
  50% {
    translate: 0 -4px;
  }
}

.node-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--border);
  border-radius: 50%;
  width: 90px;
  height: 90px;
  justify-content: center;
  padding: 12px;
  box-shadow: var(--shadow-card);
  transition: all 0.25s ease;
}

.graph-node:hover .node-content {
  box-shadow: var(--shadow-card-hover);
  border-color: var(--border-interactive);
}

.graph-node--root .node-content {
  width: 110px;
  height: 110px;
  background: var(--accent-soft);
  border-color: var(--accent);
}

.graph-node--selected .node-content {
  border-color: var(--accent);
  box-shadow:
    0 0 0 3px var(--accent-soft),
    var(--shadow-card-hover);
}

.graph-node--loading .node-content {
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.node-zh {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.2;
  text-align: center;
}

.node-en {
  font-size: 0.65rem;
  color: var(--ink-muted);
  line-height: 1.2;
  text-align: center;
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.graph-node--root .node-zh {
  font-size: 1rem;
}

/* Badge */
.node-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: var(--accent);
  color: #fdfbf5;
  font-size: 0.6rem;
  font-weight: 700;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

/* Expand button */
.node-expand-btn {
  position: absolute;
  bottom: -12px;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent);
  color: #fdfbf5;
  border: 2px solid var(--surface);
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-button);
  transition: all 0.2s ease;
  z-index: 10;
}

.node-expand-btn:hover {
  transform: translateX(-50%) scale(1.15);
}

.node-expand-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinner-sm {
  width: 14px;
  height: 14px;
  border: 2px solid oklch(1 0 0 / 0.3);
  border-top-color: #fdfbf5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ─── Input Area ───────────────────────────────────── */
.input-area {
  position: fixed;
  z-index: 50;
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.input-area--docked {
  top: auto;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
}

.input-form {
  display: flex;
  gap: 8px;
  background: var(--canvas);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-interactive);
  border-radius: 16px;
  padding: 6px 6px 6px 20px;
  box-shadow: var(--shadow-card);
  min-width: 360px;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 1rem;
  color: var(--ink);
  outline: none;
  font-family: inherit;
  min-width: 0;
}

.search-input::placeholder {
  color: var(--ink-muted);
}

.search-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--accent);
  color: #fdfbf5;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.search-btn:hover:not(:disabled) {
  opacity: 0.85;
}

.search-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.quota-tip {
  text-align: center;
  font-size: 0.75rem;
  color: var(--ink-muted, #999);
  margin-top: 6px;
}
.quota-error {
  text-align: center;
  font-size: 0.8rem;
  color: #e74c3c;
  margin-top: 6px;
}
.quota-login-link {
  color: var(--accent);
  font-weight: 600;
  text-decoration: underline;
  margin-left: 4px;
}

/* ─── Canvas Controls ──────────────────────────────── */
.canvas-controls {
  position: fixed;
  bottom: 24px;
  left: 24px;
  z-index: 50;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: var(--canvas);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 6px;
  box-shadow: var(--shadow-card);
}

.canvas-controls button {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: transparent;
  color: var(--ink-soft);
  border: none;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.canvas-controls button:hover {
  background: var(--accent-soft);
  color: var(--accent);
}

.scale-display {
  font-size: 0.65rem;
  color: var(--ink-muted);
  padding: 2px 0;
}

/* ─── History Toggle ───────────────────────────────── */
.history-toggle {
  position: fixed;
  top: 68px;
  right: 16px;
  z-index: 50;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--canvas);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--border);
  color: var(--ink-soft);
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-card);
  transition: all 0.2s ease;
}

.history-toggle:hover {
  background: var(--accent-soft);
  color: var(--accent);
}

/* ─── History Drawer ───────────────────────────────── */
.history-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: oklch(0 0 0 / 0.3);
}

.history-drawer {
  position: fixed;
  top: 60px;
  right: 0;
  bottom: 0;
  width: 320px;
  z-index: 70;
  background: var(--surface);
  border-left: 1px solid var(--border);
  transform: translateX(100%);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: flex;
  flex-direction: column;
}

.history-drawer.open {
  transform: translateX(0);
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.drawer-header h3 {
  font-size: 1rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
}

.drawer-close {
  background: none;
  border: none;
  font-size: 1.3rem;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 4px;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.drawer-empty {
  text-align: center;
  color: var(--ink-muted);
  font-size: 0.88rem;
  padding: 40px 0;
}

.history-item {
  position: relative;
  padding: 12px 36px 12px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s ease;
  margin-bottom: 4px;
}

.history-item:hover {
  background: var(--accent-soft);
}

.history-word {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--ink);
}

.history-time {
  font-size: 0.72rem;
  color: var(--ink-muted);
  margin-top: 2px;
}

.history-delete {
  position: absolute;
  top: 50%;
  right: 10px;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 1.1rem;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 4px;
  opacity: 0;
  transition: all 0.2s ease;
}

.history-item:hover .history-delete {
  opacity: 1;
}

.history-delete:hover {
  color: #c0392b;
}

/* ─── Welcome ──────────────────────────────────────── */
.welcome {
  position: absolute;
  top: 30%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
}

.welcome-icon {
  font-size: 3rem;
  margin-bottom: 16px;
}

.welcome h2 {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 8px;
}

.welcome p {
  font-size: 0.95rem;
  color: var(--ink-muted);
}
.guest-login-link {
  color: var(--accent);
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 2px;
}

/* ─── Responsive ───────────────────────────────────── */
@media (max-width: 600px) {
  .input-form {
    min-width: 280px;
    padding: 5px 5px 5px 14px;
  }

  .search-input {
    font-size: 0.9rem;
  }

  .input-area {
    left: 16px;
    right: 16px;
    transform: none;
    width: auto;
  }

  .input-area--docked {
    bottom: 16px;
    left: 16px;
    right: 16px;
    transform: none;
  }

  .node-content {
    width: 72px;
    height: 72px;
    padding: 8px;
  }

  .graph-node--root .node-content {
    width: 90px;
    height: 90px;
  }

  .node-zh {
    font-size: 0.75rem;
  }

  .node-en {
    font-size: 0.58rem;
  }

  .canvas-controls {
    bottom: 80px;
    left: 12px;
  }

  .dark-toggle {
    top: 62px;
    left: 8px;
  }

  .history-toggle {
    top: 62px;
    right: 8px;
  }

  .history-drawer {
    width: 280px;
  }
}
</style>
