<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { buildAgentSseUrl } from '@/api/ai'
import { useTTS } from '@/composables/useTTS'
import { Volume2, VolumeX } from '@lucide/vue'

type ChatMsg = {
  role: 'user' | 'assistant'
  content: string
  reasoningContent?: string
  imageUrl?: string
}

const props = defineProps<{
  messages: ChatMsg[]
  systemPrompt: string
  agentMode: boolean
}>()

const emit = defineEmits<{
  close: []
  send: [text: string]
}>()

const { ttsEnabled, toggleTTS, feedStreamChunk, flushStreamBuffer, reset: resetTTS } = useTTS()

/* ─── 状态 ─── */
type Mode = 'idle' | 'listening' | 'thinking' | 'speaking'
const currentMode = ref<Mode>('idle')
const statusText = ref('System Ready')
const isRecording = ref(false)
const showHint = ref(true)
const isSending = ref(false)

/* ─── 显示用消息列表（同步父组件） ─── */
const chatScrollRef = ref<HTMLElement | null>(null)

/* ─── Canvas refs ─── */
const mainCanvasRef = ref<HTMLCanvasElement | null>(null)
const waveCanvasRef = ref<HTMLCanvasElement | null>(null)

/* ─── 语音识别 ─── */
const speechSupported = ref(false)
let speechRecognition: any = null
const interimText = ref('')
let abortCtrl: AbortController | null = null

function initSpeech() {
  const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SR) { speechSupported.value = false; return }
  speechSupported.value = true
  speechRecognition = new SR()
  speechRecognition.lang = 'zh-CN'
  speechRecognition.continuous = false
  speechRecognition.interimResults = true
  speechRecognition.onresult = (ev: any) => {
    let t = ''
    for (let i = 0; i < ev.results.length; i++) t += ev.results[i][0].transcript
    interimText.value = t
  }
  speechRecognition.onend = () => {
    if (currentMode.value === 'listening') {
      const text = interimText.value.trim()
      interimText.value = ''
      if (text) handleVoiceSend(text)
      else setMode('idle')
    }
  }
  speechRecognition.onerror = () => {
    interimText.value = ''
    setMode('idle')
  }
}

function toggleVoice() {
  if (currentMode.value === 'thinking' || currentMode.value === 'speaking') return
  if (!speechSupported.value) return

  if (!isRecording.value) {
    abortCtrl?.abort()
    isRecording.value = true
    setMode('listening')
    speechRecognition?.start()
  } else {
    isRecording.value = false
    speechRecognition?.stop()
  }
}

/** 构建 API 消息（与 EchobotView 逻辑一致） */
function buildApiMessages(): { role: string; content: string }[] {
  const out: { role: string; content: string }[] = []
  const sys = props.systemPrompt.trim()
  if (sys) out.push({ role: 'system', content: sys })
  for (const m of props.messages) {
    if (m.role === 'assistant' && !m.content.trim()) continue
    out.push({ role: m.role, content: m.content })
  }
  return out
}

/** 语音识别后发送文字给 AI，复用父组件的消息列表 */
async function handleVoiceSend(text: string) {
  if (isSending.value) return
  isSending.value = true

  // 往父组件的消息列表里加用户消息
  props.messages.push({ role: 'user', content: text })
  scrollChat()
  setMode('thinking')

  // 加 AI 占位消息
  props.messages.push({ role: 'assistant', content: '', reasoningContent: '' })
  const aiIdx = props.messages.length - 1

  abortCtrl = new AbortController()
  resetTTS()

  try {
    const token = localStorage.getItem('ro_blog_token')
    const authHeaders: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }

    const history = buildApiMessages()
    let res: Response

    if (props.agentMode) {
      res = await fetch(buildAgentSseUrl('deepseek-v4-flash'), {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify(history),
        signal: abortCtrl.signal,
      })
    } else {
      res = await fetch(
        `/api/ai/sse?model=deepseek-v4-flash&messages=${encodeURIComponent(JSON.stringify(history))}`,
        {
          headers: authHeaders,
          signal: abortCtrl.signal,
        },
      )
    }

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`)
    }

    const reader = res.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed || !trimmed.startsWith('data:')) continue
        try {
          const data = JSON.parse(trimmed.slice(5).trim())
          if (data.error) {
            props.messages[aiIdx].content = `错误：${data.error}`
            reader.cancel()
            break
          }
          if (data.reasoning_content) {
            props.messages[aiIdx].reasoningContent =
              (props.messages[aiIdx].reasoningContent || '') + data.reasoning_content
          }
          if (data.content) {
            props.messages[aiIdx].content += data.content
            scrollChat()
            feedStreamChunk(data.content)
          }
        } catch {}
      }
    }
  } catch (e: any) {
    resetTTS()
    if (e.name !== 'AbortError') {
      props.messages[aiIdx].content += `\n\n[错误: ${e.message}]`
    }
  }

  flushStreamBuffer()
  isSending.value = false
  // 回复完成后短暂停留 speaking 动画再回 idle
  setMode('speaking')
  setTimeout(() => setMode('idle'), 1500)
}

function setMode(mode: Mode) {
  currentMode.value = mode
  showHint.value = mode === 'idle'
  if (mode === 'idle') {
    statusText.value = 'System Ready'
    isRecording.value = false
  } else if (mode === 'listening') {
    statusText.value = 'Listening...'
  } else if (mode === 'thinking') {
    statusText.value = 'Processing'
  } else if (mode === 'speaking') {
    statusText.value = 'Transmitting'
  }
}

function scrollChat() {
  nextTick(() => {
    chatScrollRef.value?.scrollTo({ top: chatScrollRef.value.scrollHeight, behavior: 'auto' })
  })
}

/* ─── Markdown ─── */
marked.use({ breaks: false, gfm: true })
const renderer = new marked.Renderer()
renderer.code = ({ text, lang }: { text: string; lang?: string }) => {
  const la = lang ? ` class="language-${lang}"` : ''
  try {
    const h = lang ? hljs.highlight(text, { language: lang }).value : hljs.highlightAuto(text).value
    return `<pre><code${la}>${h}</code></pre>`
  } catch {
    return `<pre><code${la}>${hljs.highlightAuto(text).value}</code></pre>`
  }
}
marked.use({ renderer })

function fmt(s: string): string {
  if (!s) return ''
  try {
    return (marked.parse(s.trim()) as string)
      .replace(/<table>/g, '<table class="chat-table">')
      .replace(/<p>\s*<\/p>/g, '')
      .trim()
  } catch {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>')
  }
}

/* ═══════════════════════════════════════════
   3D 粒子球 + 背景星星
   ═══════════════════════════════════════════ */
const SPHERE_RADIUS = 180
const PARTICLE_COUNT = 800
const BG_STAR_COUNT = 250

let animId = 0
let width = 0
let height = 0
let rotX = 0
let rotY = 0
let isDragging = false
let lastMX = 0
let lastMY = 0
let targetSpeed = 1
let curSpeed = 1
let audioLevel = 0

const modeColorMap: Record<Mode, string> = {
  idle: '#ef4444',
  listening: '#a78bfa',
  thinking: '#ffcc00',
  speaking: '#ffffff',
}

interface Star { x: number; y: number; size: number; speed: number; twinkle: number; phase: number }
let bgStars: Star[] = []

class OrbP {
  bx: number; by: number; bz: number
  x = 0; y = 0; z = 0
  type: 'signal' | 'core' | 'void'
  color: string
  size: number

  constructor() {
    const u = Math.random(), v = Math.random()
    const r = Math.pow(Math.random(), 1 / 3) * SPHERE_RADIUS
    const th = 2 * Math.PI * u, ph = Math.acos(2 * v - 1)
    this.bx = r * Math.sin(ph) * Math.cos(th)
    this.by = r * Math.sin(ph) * Math.sin(th)
    this.bz = r * Math.cos(ph)
    const rnd = Math.random()
    if (rnd < 0.4) { this.type = 'signal'; this.color = '#ff4d4d' }
    else if (rnd < 0.8) { this.type = 'core'; this.color = '#ffffff' }
    else { this.type = 'void'; this.color = '#000000' }
    this.size = Math.random() * 1.5 + 0.5
  }

  update(time: number, rx: number, ry: number, mode: Mode) {
    if (this.type === 'signal') this.color = modeColorMap[mode] || '#ff4d4d'
    const wave = Math.sin(time * 0.002 + (this.bx + this.by + this.bz) * 0.01) * (15 + audioLevel * 50)
    const ru = Math.sqrt(this.bx ** 2 + this.by ** 2 + this.bz ** 2) + 0.001
    const sc = mode === 'thinking' ? 0.9 : 1
    const rf = (ru * sc + wave) / ru
    let tx = this.bx * rf, ty = this.by * rf, tz = this.bz * rf
    const cx = Math.cos(rx), sx = Math.sin(rx)
    const cy = Math.cos(ry), sy = Math.sin(ry)
    const y1 = ty * cx - tz * sx, z1 = ty * sx + tz * cx
    this.x = tx * cy + z1 * sy; this.y = y1; this.z = -tx * sy + z1 * cy
  }

  draw(ctx: CanvasRenderingContext2D, cx: number, cy: number) {
    const p = 600 / (600 - Math.max(-300, Math.min(this.z, 590)))
    const dx = this.x * p + cx, dy = this.y * p + cy, ds = Math.max(0, this.size * p)
    if (this.type === 'void') {
      ctx.strokeStyle = 'rgba(255,255,255,0.15)'; ctx.lineWidth = 0.5
      ctx.beginPath(); ctx.arc(dx, dy, ds, 0, Math.PI * 2); ctx.stroke()
    } else {
      ctx.fillStyle = this.color; ctx.globalAlpha = Math.max(0.1, p - 0.4)
      ctx.beginPath(); ctx.arc(dx, dy, ds, 0, Math.PI * 2); ctx.fill()
    }
  }
}

let orbParticles: OrbP[] = []

/* ═══════════════════════════════════════════
   声波渲染
   ═══════════════════════════════════════════ */
const waveLayers = [
  { speed: 0.005, freq: 0.006, alpha: 0.8, offset: 0, scale: 1.0 },
  { speed: 0.0075, freq: 0.008, alpha: 0.5, offset: 2, scale: 0.8 },
  { speed: 0.004, freq: 0.005, alpha: 0.3, offset: 4, scale: 0.6 },
  { speed: 0.01, freq: 0.01, alpha: 0.2, offset: 6, scale: 0.4 },
]

let targetWaveW = 160, curWaveW = 160
let targetAmp = 0, curAmp = 0
let targetMorph = 1, curMorph = 1

function renderWave(time: number) {
  const c = waveCanvasRef.value
  if (!c) return
  const ctx = c.getContext('2d')
  if (!ctx) return
  const w = c.width, h = c.height, cy = h / 2, cx = w / 2
  ctx.clearRect(0, 0, w, h)

  const mode = currentMode.value
  if (mode === 'idle') { targetWaveW = 160; targetMorph = 1; targetAmp = 0 }
  else if (mode === 'listening') { targetWaveW = w * 0.7; targetMorph = 0; targetAmp = 0.3 + Math.sin(time * 0.005) * 0.1 }
  else if (mode === 'thinking') { targetWaveW = w * 0.5; targetMorph = 0; targetAmp = 0.2 + Math.random() * 0.1 }
  else if (mode === 'speaking') { targetWaveW = w * 0.9; targetMorph = 0; targetAmp = 0.2 + audioLevel * 0.8 }

  curMorph += (targetMorph - curMorph) * 0.08
  curWaveW += (targetWaveW - curWaveW) * 0.1
  curAmp += (targetAmp - curAmp) * 0.1
  const waveAlpha = 1 - curMorph

  // 液态流光球（idle 状态）
  if (curMorph > 0.005) {
    ctx.save()
    ctx.translate(cx, cy)
    ctx.scale(1 + (1 - curMorph) * 6, 1 - (1 - curMorph) * 0.8)
    ctx.globalAlpha = curMorph
    const orbSize = 55 + Math.sin(time * 0.003) * 4
    ctx.globalCompositeOperation = 'screen'
    ctx.rotate(time * 0.001)
    const colors = [
      { r: 255, g: 128, b: 181, radius: orbSize * 1.3, offset: 0, dist: 12 },
      { r: 129, g: 230, b: 217, radius: orbSize * 1.2, offset: 2.1, dist: 18 },
      { r: 167, g: 139, b: 250, radius: orbSize * 1.4, offset: 4.2, dist: 10 },
    ]
    colors.forEach((item, idx) => {
      ctx.save()
      ctx.rotate(time * 0.002 * (idx % 2 === 0 ? 1 : -1) + item.offset)
      ctx.translate(item.dist, 0)
      const g = ctx.createRadialGradient(0, 0, 0, 0, 0, item.radius)
      g.addColorStop(0, `rgba(${item.r},${item.g},${item.b},0.9)`)
      g.addColorStop(0.5, `rgba(${item.r},${item.g},${item.b},0.4)`)
      g.addColorStop(1, `rgba(${item.r},${item.g},${item.b},0)`)
      ctx.fillStyle = g
      ctx.beginPath(); ctx.arc(0, 0, item.radius, 0, Math.PI * 2); ctx.fill()
      ctx.restore()
    })
    ctx.globalCompositeOperation = 'source-over'
    const cg = ctx.createRadialGradient(0, 0, 0, 0, 0, orbSize * 0.7)
    cg.addColorStop(0, 'rgba(255,255,255,1)')
    cg.addColorStop(0.3, 'rgba(255,255,255,0.7)')
    cg.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = cg
    ctx.beginPath(); ctx.arc(0, 0, orbSize * 0.7, 0, Math.PI * 2); ctx.fill()
    ctx.restore()
  }

  // 声波
  if (waveAlpha > 0.005) {
    ctx.save()
    ctx.globalAlpha = waveAlpha
    ctx.globalCompositeOperation = 'screen'
    const grad = ctx.createLinearGradient(0, 0, w, 0)
    grad.addColorStop(0.1, '#ff80b5')
    grad.addColorStop(0.5, '#a78bfa')
    grad.addColorStop(0.9, '#81e6d9')
    const sx = cx - curWaveW / 2, ex = cx + curWaveW / 2
    waveLayers.forEach(layer => {
      ctx.beginPath(); ctx.moveTo(sx, cy)
      for (let x = sx; x <= ex; x += 2) {
        const prog = (x - sx) / curWaveW
        const env = Math.pow(Math.sin(prog * Math.PI), 2.5)
        const yOff = Math.sin(x * layer.freq + time * layer.speed + layer.offset) * (h / 2 * curAmp * layer.scale * env)
        ctx.lineTo(x, cy + yOff)
      }
      for (let x = ex; x >= sx; x -= 2) {
        const prog = (x - sx) / curWaveW
        const env = Math.pow(Math.sin(prog * Math.PI), 2.5)
        const yOff = Math.sin(x * layer.freq + time * layer.speed + layer.offset + Math.PI) * (h / 2 * curAmp * layer.scale * env)
        ctx.lineTo(x, cy + yOff)
      }
      ctx.closePath()
      ctx.fillStyle = grad
      ctx.globalAlpha = layer.alpha * waveAlpha * 1.2
      ctx.fill()
    })
    ctx.restore()
  }
}

/* ═══════════════════════════════════════════
   主动画循环
   ═══════════════════════════════════════════ */
function resizeMain() {
  const c = mainCanvasRef.value
  if (!c) return
  width = window.innerWidth
  height = window.innerHeight
  c.width = width; c.height = height
  bgStars = Array.from({ length: BG_STAR_COUNT }, () => ({
    x: Math.random() * width, y: Math.random() * height,
    size: Math.random() * 1.5 + 0.2, speed: Math.random() * 0.3 + 0.05,
    twinkle: Math.random() * 0.003 + 0.001, phase: Math.random() * Math.PI * 2,
  }))
}

function resizeWave() {
  const c = waveCanvasRef.value
  if (!c) return
  const p = c.parentElement
  if (!p) return
  c.width = p.clientWidth * 2
  c.height = p.clientHeight * 2
}

function animate(time: number) {
  const c = mainCanvasRef.value
  if (!c) return
  const ctx = c.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, width, height)
  ctx.fillStyle = '#ffffff'

  bgStars.forEach(s => {
    s.y -= s.speed; if (s.y < 0) s.y = height
    ctx.globalAlpha = Math.max(0.05, 0.4 + Math.sin(time * s.twinkle + s.phase) * 0.4)
    ctx.beginPath(); ctx.arc(s.x, s.y, s.size, 0, Math.PI * 2); ctx.fill()
  })
  ctx.globalAlpha = 1

  curSpeed += (targetSpeed - curSpeed) * 0.05

  if (currentMode.value === 'speaking') {
    audioLevel += (Math.max(0, Math.sin(time * 0.015) * Math.sin(time * 0.005) * Math.random()) * 1.5 - audioLevel) * 0.2
  } else {
    audioLevel += (0 - audioLevel) * 0.1
  }

  if (!isDragging) { rotY += 0.005 * curSpeed; rotX += 0.002 * curSpeed }

  for (const p of orbParticles) p.update(time, rotX, rotY, currentMode.value)
  orbParticles.sort((a, b) => a.z - b.z)
  for (const p of orbParticles) p.draw(ctx, width / 2, height / 2)

  renderWave(time)
  animId = requestAnimationFrame(animate)
}

/* ─── 鼠标拖拽 ─── */
function onDown(e: MouseEvent | TouchEvent) {
  isDragging = true
  const cx = 'clientX' in e ? e.clientX : e.touches[0].clientX
  const cy = 'clientY' in e ? e.clientY : e.touches[0].clientY
  lastMX = cx; lastMY = cy
}
function onMove(e: MouseEvent | TouchEvent) {
  if (!isDragging) return
  const cx = 'clientX' in e ? e.clientX : e.touches[0].clientX
  const cy = 'clientY' in e ? e.clientY : e.touches[0].clientY
  rotY += (cx - lastMX) * 0.01; rotX -= (cy - lastMY) * 0.01
  lastMX = cx; lastMY = cy
}
function onUp() { isDragging = false }

/* ─── 生命周期 ─── */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => {
  orbParticles = Array.from({ length: PARTICLE_COUNT }, () => new OrbP())
  resizeMain()
  resizeWave()
  window.addEventListener('resize', resizeMain)
  window.addEventListener('resize', resizeWave)
  window.addEventListener('keydown', handleKeydown)
  animId = requestAnimationFrame(animate)
  initSpeech()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resizeMain)
  window.removeEventListener('resize', resizeWave)
  window.removeEventListener('keydown', handleKeydown)
  speechRecognition?.abort()
  abortCtrl?.abort()
})

watch(() => props.messages.length, () => scrollChat())
</script>

<template>
  <div class="immersive-overlay">
    <div class="glow-bg" :style="{ backgroundColor: modeColorMap[currentMode] || '#ef4444' }"></div>

    <canvas
      ref="mainCanvasRef"
      class="main-canvas"
      @mousedown="onDown"
      @mousemove="onMove"
      @mouseup="onUp"
      @mouseleave="onUp"
      @touchstart.prevent="onDown"
      @touchmove.prevent="onMove"
      @touchend="onUp"
    ></canvas>

    <button class="close-btn" title="退出沉浸模式 (Esc)" @click="emit('close')">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
      </svg>
    </button>

    <button
      class="tts-toggle-btn"
      :class="{ active: ttsEnabled }"
      :title="ttsEnabled ? '关闭 AI 朗读' : '开启 AI 朗读'"
      @click="toggleTTS()"
    >
      <Volume2 v-if="ttsEnabled" :size="18" />
      <VolumeX v-else :size="18" />
    </button>

    <div class="mic-wrapper">
      <div class="wave-container" @click="toggleVoice" title="点击开始/结束说话">
        <canvas ref="waveCanvasRef" class="wave-canvas"></canvas>
        <div class="interaction-hint" :class="{ hidden: !showHint }">[ 点击以语音交流 ]</div>
      </div>
      <div class="status-text" :class="currentMode">{{ statusText }}</div>
      <div v-if="interimText" class="interim-text">{{ interimText }}</div>
    </div>

    <div class="chat-panel">
      <div class="chat-header">Session Transcript //</div>
      <div ref="chatScrollRef" class="chat-messages">
        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="msg"
          :class="msg.role"
        >
          <div class="text" v-html="fmt(msg.content)"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.immersive-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background-color: #020205;
  background-image:
    radial-gradient(circle at 15% 25%, rgba(45, 20, 70, 0.25) 0%, transparent 40%),
    radial-gradient(circle at 85% 75%, rgba(15, 45, 80, 0.25) 0%, transparent 40%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: #fff;
  overflow: hidden;
}

.glow-bg {
  position: absolute;
  width: 500px;
  height: 500px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.3;
  pointer-events: none;
  transition: background-color 0.8s ease;
  z-index: 5;
}

.main-canvas {
  display: block;
  position: absolute;
  inset: 0;
  z-index: 10;
  cursor: grab;
}
.main-canvas:active { cursor: grabbing; }

.close-btn {
  position: absolute;
  top: 24px;
  left: 24px;
  z-index: 50;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.15);
  background: rgba(255,255,255,0.06);
  color: rgba(255,255,255,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  backdrop-filter: blur(8px);
}
.close-btn:hover {
  background: rgba(255,255,255,0.12);
  color: #fff;
  border-color: rgba(255,255,255,0.3);
}

.tts-toggle-btn {
  position: absolute;
  top: 24px;
  left: 76px;
  z-index: 50;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.15);
  background: rgba(255,255,255,0.06);
  color: rgba(255,255,255,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  backdrop-filter: blur(8px);
}
.tts-toggle-btn:hover {
  background: rgba(255,255,255,0.12);
  color: #fff;
  border-color: rgba(255,255,255,0.3);
}
.tts-toggle-btn.active {
  background: rgba(139, 92, 246, 0.25);
  border-color: rgba(139, 92, 246, 0.5);
  color: #a78bfa;
  box-shadow: 0 0 12px rgba(139, 92, 246, 0.2);
}

.mic-wrapper {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.wave-container {
  width: 360px;
  height: 160px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
}

.wave-canvas {
  display: block;
  width: 100%;
  height: 100%;
  pointer-events: none;
  transition: opacity 0.3s;
}
.wave-container:hover .wave-canvas { filter: brightness(1.3); }

.interaction-hint {
  position: absolute;
  bottom: 10px;
  font-size: 12px;
  letter-spacing: 2px;
  color: rgba(255,255,255,0.6);
  pointer-events: none;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  animation: breathe-text 2s infinite ease-in-out;
  opacity: 1;
  transform: translateY(0);
}
.interaction-hint.hidden {
  opacity: 0;
  transform: translateY(15px);
  animation: none;
}
@keyframes breathe-text {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.9; text-shadow: 0 0 10px rgba(255,255,255,0.4); }
}

.status-text {
  font-size: 11px;
  letter-spacing: 4px;
  text-transform: uppercase;
  color: rgba(255,255,255,0.3);
  font-weight: bold;
  text-shadow: 0 2px 10px rgba(0,0,0,0.5);
  transition: color 0.3s;
}
.status-text.listening { color: #a78bfa; }
.status-text.thinking { color: #ffcc00; }
.status-text.speaking { color: #81e6d9; }

.interim-text {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
  max-width: 300px;
  text-align: center;
  animation: fadeInUp 0.3s ease;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-panel {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 400px;
  padding: 40px 32px 120px 32px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  z-index: 20;
  pointer-events: none;
  background: linear-gradient(to right, transparent, rgba(0,0,0,0.4) 80%);
  -webkit-mask-image: linear-gradient(to bottom, transparent 0%, black 12%, black 100%);
  mask-image: linear-gradient(to bottom, transparent 0%, black 12%, black 100%);
}

.chat-header {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 3px;
  color: rgba(255,255,255,0.25);
  margin-bottom: 20px;
  text-align: right;
  text-transform: uppercase;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-right: 8px;
  pointer-events: auto;
}
.chat-messages::-webkit-scrollbar { width: 0; }

.msg { display: flex; flex-direction: column; max-width: 100%; }
.msg.user { align-self: flex-end; align-items: flex-end; }
.msg.user .text {
  color: rgba(255,255,255,0.6);
  font-size: 14px;
  line-height: 1.6;
  text-align: right;
  text-shadow: 0 2px 4px rgba(0,0,0,0.8);
}
.msg.assistant { align-self: flex-start; }
.msg.assistant .text {
  color: #ffffff;
  font-size: 14px;
  line-height: 1.7;
  font-weight: 300;
  text-shadow: 0 2px 10px rgba(0,0,0,0.8);
}

.msg .text :deep(pre) {
  margin: 0.3rem 0;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 0.78rem;
  line-height: 1.45;
  background: rgba(255,255,255,0.06);
  padding: 0.5rem 0.7rem;
}
.msg .text :deep(code) {
  background: rgba(255,255,255,0.08);
  padding: 0.1rem 0.25rem;
  border-radius: 3px;
  font-size: 0.82em;
  font-family: 'Fira Code', 'Consolas', monospace;
}
.msg .text :deep(pre code) { background: transparent; padding: 0; }
.msg .text :deep(p) { margin: 0.1rem 0; }
.msg .text :deep(p:first-child) { margin-top: 0; }
.msg .text :deep(p:last-child) { margin-bottom: 0; }
.msg .text :deep(ul), .msg .text :deep(ol) { padding-left: 1.2rem; margin: 0.15rem 0; }

@media (max-width: 768px) {
  .chat-panel { width: 100%; padding: 60px 20px 140px 20px; }
  .wave-container { width: 280px; height: 120px; }
}
</style>
