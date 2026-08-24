/* ============================================================
 * engine.ts —— 驱动层（rAF 循环、临界阻尼弹簧、注视平滑追踪、表情切换）
 * ============================================================ */

import { EB_RINGS } from './rings'
import { EMOTION_GROUPS, EMOTION_SEED, EmotionSeed, EmotionGroup } from './emotions'
import { createBall, BallInstance } from './ball'

const EXPR = EB_RINGS.EXPRESSIONS
const TAU = Math.PI * 2
const FALLBACK_ID = '02'

function clamp(v: number, a: number, b: number) {
  return v < a ? a : v > b ? b : v
}
function lerp(a: number, b: number, t: number) {
  return a + (b - a) * t
}
function rand(a: number, b: number) {
  return a + Math.random() * (b - a)
}
function easeInOutCubic(t: number) {
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
}

interface SpringState {
  x: number
  v: number
  t: number
}

function spring(v0: number): SpringState {
  return { x: v0, v: 0, t: v0 }
}

function springStep(s: SpringState, w: number, z: number, dt: number) {
  s.v += (-2 * z * w * s.v - w * w * (s.x - s.t)) * dt
  s.x += s.v * dt
  if (!isFinite(s.x) || !isFinite(s.v)) {
    s.x = s.t
    s.v = 0
  }
}

function lerpRing(a: [number, number][], b: [number, number][], t: number): [number, number][] {
  const out = new Array(a.length)
  for (let i = 0; i < a.length; i++) {
    out[i] = [a[i][0] + (b[i][0] - a[i][0]) * t, a[i][1] + (b[i][1] - a[i][1]) * t]
  }
  return out
}

const BOUNCE_SEGS = [
  { h: 48, d: 0.5 },
  { h: 28, d: 0.382 },
  { h: 14, d: 0.27 },
  { h: 6, d: 0.177 }
]
const BOUNCE_TOTAL = BOUNCE_SEGS.reduce((s, q) => s + q.d, 0)

function hexToRgb(hex: string): [number, number, number] {
  let h = hex.replace('#', '')
  if (h.length === 3) h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2]
  const n = parseInt(h, 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

function rgbToHex(r: number, g: number, b: number): string {
  return (
    '#' +
    [r, g, b]
      .map((v) => clamp(Math.round(v), 0, 255).toString(16).padStart(2, '0'))
      .join('')
  )
}

function lerpColor(a: string, b: string, t: number): string {
  if (a === b) return b
  const A = hexToRgb(a),
    B = hexToRgb(b)
  return rgbToHex(lerp(A[0], B[0], t), lerp(A[1], B[1], t), lerp(A[2], B[2], t))
}

const DEFAULT_BODY = {
  x: 0,
  y: 0,
  scale: 1,
  rotate: 0,
  color: '#F3F0EA',
  breathe: 0.01,
  ribbons: 0,
  confetti: 0,
  sketch: 0,
  zzz: 0,
  orbit: 0
}
const DEFAULT_EYE = { x: 0, y: 0, scaleX: 1, scaleY: 1, rotate: 0, open: 1, color: '#1A1A1A', lookX: 0, lookY: 0 }

function defaultPose() {
  return {
    body: Object.assign({}, DEFAULT_BODY),
    left: Object.assign({}, DEFAULT_EYE),
    right: Object.assign({}, DEFAULT_EYE)
  }
}

function clonePose(p: any) {
  return {
    body: Object.assign({}, p.body),
    left: Object.assign({}, p.left),
    right: Object.assign({}, p.right)
  }
}

function applySpec(pose: any, spec: any) {
  if (!spec) return pose
  if (spec.body) Object.assign(pose.body, spec.body)
  const e = spec.eyes
  if (e) {
    if (e.both) {
      Object.assign(pose.left, e.both)
      Object.assign(pose.right, e.both)
    }
    if (e.left) Object.assign(pose.left, e.left)
    if (e.right) Object.assign(pose.right, e.right)
  }
  return pose
}

function lerpPose(a: any, b: any, t: number) {
  const out = defaultPose()
  ;(['body', 'left', 'right'] as const).forEach((part) => {
    const pa = a[part],
      pb = b[part],
      po = (out as any)[part]
    for (const k in pb) {
      const vb = pb[k]
      if (typeof vb === 'number') po[k] = lerp(pa[k] != null ? pa[k] : vb, vb, t)
      else if (k === 'color') po[k] = lerpColor(pa[k] || vb, vb, t)
      else po[k] = vb
    }
  })
  return out
}

const ANIM_TYPES: Record<string, (a: any, t: number, eng?: any) => number> = {
  sine: (a, t) => a.amp * Math.sin((TAU * t) / (a.period || 2000) + (a.phase || 0)),
  pulse: (a, t) => a.amp * 0.5 * (1 - Math.cos((TAU * t) / (a.period || 1000) + (a.phase || 0))),
  jitter: (a, t, eng) => {
    const s = (t / 1000) * (a.speed || 8)
    let v =
      ((Math.sin(s * 3.1 + eng._seed) +
        Math.sin(s * 5.7 + eng._seed * 2.3) +
        Math.sin(s * 9.3 + eng._seed * 4.1)) /
        3) *
      a.amp
    if (a.decay) v *= clamp(1 - t / a.decay, 0, 1)
    return v
  },
  scan: (a, t) => {
    const per = a.period || 800
    const p = ((t + (a.phaseMs || 0)) % per) / per
    const tri = p < 0.5 ? p * 4 - 1 : 3 - p * 4
    return a.amp * tri
  },
  glance: (a, t) => {
    const per = a.period || 3600
    const ph = TAU * (((t + (a.phaseMs || 0)) % per) / per) + (a.phase || 0)
    return a.amp * Math.tanh(2.8 * Math.sin(ph))
  },
  blink: (a, t, eng) => {
    const interval = a.interval || 3800,
      dur = a.dur || 200
    const p = (t + (a.phaseMs || 0) + (eng ? eng._seed * 97 : 0)) % interval
    if (p >= dur) return 0
    return -(a.depth == null ? 1 : a.depth) * Math.sin(Math.PI * (p / dur))
  }
}

function applyAnim(pose: any, a: any, t: number, eng: any) {
  const fn = ANIM_TYPES[a.type]
  if (!fn) return
  const v = fn(a, t, eng)
  const targets =
    a.target === 'eyes'
      ? [pose.left, pose.right]
      : a.target === 'body'
      ? [pose.body]
      : a.target === 'left'
      ? [pose.left]
      : a.target === 'right'
      ? [pose.right]
      : []
  for (let i = 0; i < targets.length; i++) {
    const tg = targets[i]
    if (a.prop === 'scale') {
      if (tg === pose.body) tg.scale += v
      else {
        tg.scaleX += v
        tg.scaleY += v
      }
    } else if (a.prop in tg) {
      tg[a.prop] += v
    }
  }
}

const GROUPS = EMOTION_GROUPS.slice()
const registry = new Map<string, any>()
const order: string[] = []

function normalize(raw: EmotionSeed) {
  const base = applySpec(defaultPose(), raw)
  let pool = (raw.pool || [0, 8]).filter((i) => i >= 0 && i < EXPR.length)
  if (!pool.length) pool = [0]
  const def: any = {
    id: raw.id,
    name: raw.name,
    group: raw.group,
    desc: raw.desc || '',
    en: raw.en || null,
    gaze: raw.gaze !== false,
    transition: raw.transition != null ? raw.transition : 500,
    pool: pool,
    poolMs: raw.poolMs || [9000, 16000],
    poolSpeed: raw.poolSpeed || 6,
    blinkMs: raw.blinkMs !== undefined ? raw.blinkMs : [6000, 14000],
    openness: raw.openness != null ? raw.openness : 1,
    antics: !!raw.antics,
    base: base,
    anims: (raw.anims || []).map((a) => Object.assign({}, a)),
    sequence: null,
    raw: raw
  }
  if (raw.sequence) {
    const frames = raw.sequence.frames
      .map((f) => ({
        at: f.at || 0,
        pose: applySpec(clonePose(base), f)
      }))
      .sort((x, y) => x.at - y.at)
    def.sequence = { frames: frames, settle: raw.sequence.settle || 'base' }
  }
  return def
}

function register(raw: EmotionSeed) {
  const def = normalize(raw)
  if (!registry.has(def.id)) order.push(def.id)
  registry.set(def.id, def)
  return { ok: true, id: def.id }
}

// 初始化注册内置表情
EMOTION_SEED.forEach((item) => register(item))

const ticker = {
  set: new Set<Engine>(),
  raf: 0,
  add(e: Engine) {
    this.set.add(e)
    if (!this.raf) this.raf = requestAnimationFrame(ticker.loop)
  },
  remove(e: Engine) {
    this.set.delete(e)
  },
  loop(now: number) {
    ticker.raf = 0
    ticker.set.forEach((e) => e._tick(now))
    if (ticker.set.size) ticker.raf = requestAnimationFrame(ticker.loop)
  }
}

export interface EngineOptions {
  emotion?: string
  shape?: 'blob' | 'wedge' | 'gem'
  color?: string
  eyeColor?: string
  eyeScale?: number
  sketch?: boolean | number
  lite?: boolean
  autostart?: boolean
  label?: string
  fallbackId?: string
  idle?: boolean | { standbyAfter?: number; sleepAfter?: number; standbyId?: string; sleepId?: string }
}

export class Engine {
  ball: BallInstance
  _seed: number
  _events: Record<string, Function[]> = {}
  _gaze = { x: 0, y: 0, tx: 0, ty: 0 }
  _style = { sketch: 0 }
  _theme: { body: string; eyes: string } | null = null
  _eyeScale: number
  _lastTick = 0
  _spin: SpringState | null = null

  _ringSrc: [[number, number][], [number, number][]] = [EXPR[0][0], EXPR[0][1]]
  _ringDst: [[number, number][], [number, number][]] = [EXPR[0][0], EXPR[0][1]]
  _ringCur: [[number, number][], [number, number][]] = this._ringDst
  _ringSpring: SpringState = spring(1)
  _ringSpeed = 7
  _exprIdx = 0
  _poolPos = 0
  _poolNext = 0

  _open: SpringState = spring(1)
  _blinkQ: { at: number; v: number }[] = []
  _blinkNext = Infinity

  _anticNext = 0
  _bounceAt = -1

  _def: any = null
  _lastPose: any = null
  _prevPose: any = null
  _transStart = 0
  _transDur = 0
  _emoStart = 0
  _seq: any = null
  _active = false
  _touring = false
  _tourTimer: any = 0
  _fallbackId: string
  _lastActivity: number
  _idle: any = null
  _dt = 1 / 60

  constructor(target: HTMLElement | string, opts: EngineOptions = {}) {
    const el = typeof target === 'string' ? (document.querySelector(target) as HTMLElement) : target
    if (!el) throw new Error('EmotionBall: target container not found')

    this.ball = createBall(el, Object.assign({}, opts, {
      lite: opts.lite != null ? opts.lite : opts.autostart === false
    }))
    this._seed = Math.random() * 100
    this._eyeScale = opts.eyeScale || 1
    this._fallbackId = opts.fallbackId || FALLBACK_ID
    this._lastActivity = performance.now()
    if (opts.color) {
      this._theme = { body: opts.color, eyes: opts.eyeColor || '#FFFFFF' }
    }

    if (opts.idle) {
      this._idle = Object.assign(
        { standbyAfter: 60000, sleepAfter: 180000, standbyId: '02', sleepId: '00' },
        opts.idle === true ? {} : opts.idle
      )
    }

    if (opts.sketch != null) {
      this._style.sketch = typeof opts.sketch === 'boolean' ? (opts.sketch ? 1 : 0) : opts.sketch
    }

    this.setEmotion(opts.emotion || this._fallbackId, { auto: true })
    if (opts.autostart !== false) this.setActive(true)
    else this.renderStatic()
  }

  on(evt: string, cb: Function) {
    ;(this._events[evt] = this._events[evt] || []).push(cb)
    return this
  }

  off(evt: string, cb: Function) {
    const list = this._events[evt]
    if (list) {
      const i = list.indexOf(cb)
      if (i >= 0) list.splice(i, 1)
    }
    return this
  }

  _emit(evt: string, payload: any) {
    ;(this._events[evt] || []).slice().forEach((cb) => {
      try {
        cb(payload)
      } catch (e) {
        console.error(e)
      }
    })
  }

  get emotionId(): string | null {
    return this._def ? this._def.id : null
  }

  get touring(): boolean {
    return this._touring
  }

  setEmotion(id: string, o: any = {}) {
    let def = registry.get(id)
    if (!def) {
      def = registry.get(this._fallbackId)
      if (!def) return false
    }
    const now = performance.now()
    const prevId = this._def ? this._def.id : null
    this._prevPose = this._lastPose ? clonePose(this._lastPose) : null
    this._def = def
    this._emoStart = now
    this._transStart = now
    this._transDur = this._prevPose ? def.transition : 0
    this._seq = def.sequence
      ? { frames: def.sequence.frames, settle: def.sequence.settle, done: false }
      : null
    if (!o.auto) this._lastActivity = now

    this._poolPos = 0
    this._setExpr(def.pool[0], def.poolSpeed >= 10 ? 10 : 8)
    this._poolNext = now + rand(def.poolMs[0], def.poolMs[1])
    if (prevId !== null && prevId !== def.id && def.blinkMs) this._blinkNow(now)
    this._blinkNext = def.blinkMs ? now + rand(def.blinkMs[0], def.blinkMs[1]) : Infinity
    this._anticNext = now + rand(2500, 5000)

    this._emit('change', { id: def.id, def: def, auto: !!o.auto })
    if (this._active) {
      const fx = def.base.body
      if (fx.ribbons > 0) this.spin(fx.ribbons >= 1 ? 2 : 1)
      if (fx.confetti > 0) this.burst(20)
    }
    if (!this._active) this.renderStatic()
    return true
  }

  handleAIMessage(msg: any) {
    let obj = msg
    if (typeof msg === 'string') {
      try {
        obj = JSON.parse(msg)
      } catch (e) {
        this.setEmotion(this._fallbackId)
        return false
      }
    }
    if (!obj || typeof obj !== 'object' || typeof obj.emotionId !== 'string') {
      this.setEmotion(this._fallbackId)
      return false
    }
    const ok = this.setEmotion(obj.emotionId)
    if (obj.tips) this._emit('tips', { text: String(obj.tips) })
    return ok
  }

  startTour(ids: string[], interval: number = 2500) {
    this.stopTour()
    if (!ids || !ids.length) return
    this._touring = true
    let i = 0
    this.setEmotion(ids[0], { auto: true })
    this._tourTimer = setInterval(() => {
      i = (i + 1) % ids.length
      this.setEmotion(ids[i], { auto: true })
    }, interval)
  }

  stopTour() {
    if (this._tourTimer) {
      clearInterval(this._tourTimer)
      this._tourTimer = 0
    }
    this._touring = false
    this._lastActivity = performance.now()
  }

  resetIdle() {
    this._lastActivity = performance.now()
  }

  setGaze(nx: number, ny: number) {
    this._gaze.tx = clamp(nx, -1, 1) * 24
    this._gaze.ty = clamp(ny, -1, 1) * 15
    return this
  }

  clearGaze() {
    this._gaze.tx = 0
    this._gaze.ty = 0
    return this
  }

  setStyle(style: any) {
    Object.assign(this._style, style || {})
    if (!this._active) this.renderStatic()
    return this
  }

  spin(turns: number = 1, dir?: number) {
    if (this._spin) return this
    const d = dir || (Math.random() < 0.5 ? -1 : 1)
    this._spin = { x: 0, v: 0, t: Math.max(1, Math.round(turns || 1)) * TAU * d }
    return this
  }

  burst(count: number = 20) {
    if (this.ball.burst) this.ball.burst(count)
    return this
  }

  bounce() {
    if (this._bounceAt < 0) this._bounceAt = performance.now()
    return this
  }

  _setExpr(idx: number, speed?: number) {
    if (idx === this._exprIdx && this._ringSpring.x >= 0.999) return
    const s = clamp(this._ringSpring.x, 0, 1)
    this._ringSrc = [
      lerpRing(this._ringSrc[0], this._ringDst[0], s),
      lerpRing(this._ringSrc[1], this._ringDst[1], s)
    ]
    this._ringDst = [EXPR[idx][0], EXPR[idx][1]]
    this._ringSpring.x = 0
    this._ringSpring.v = 0
    this._ringSpring.t = 1
    this._ringSpeed = speed || 7
    this._exprIdx = idx
  }

  _blinkNow(t: number) {
    this._blinkQ.push({ at: t, v: 0.05 }, { at: t + 70, v: 0.05 }, { at: t + 150, v: 1.08 }, { at: t + 300, v: 1 })
    if (Math.random() < 0.14) {
      this._blinkQ.push({ at: t + 370, v: 0.05 }, { at: t + 480, v: 1 })
    }
  }

  setActive(on: boolean) {
    if (on === this._active) return
    this._active = on
    if (on) ticker.add(this)
    else ticker.remove(this)
  }

  renderStatic() {
    this._transDur = 0
    this._ringSpring.x = 1
    this._ringSpring.v = 0
    this._open.x = this._def ? this._def.openness : 1
    this._open.v = 0
    const seq = this._seq
    this._seq = null
    this._tick(performance.now())
    this._seq = seq
  }

  destroy() {
    this.stopTour()
    this.setActive(false)
    this._events = {}
    this.ball.destroy()
  }

  _tick(now: number) {
    this._dt = this._lastTick ? clamp((now - this._lastTick) / 1000, 0.001, 0.05) : 1 / 60
    this._lastTick = now
    if (this._idle && !this._touring) this._checkIdle(now)
    const pose = this._compose(now, 0)
    this.ball.applyPose(pose)
    this._lastPose = pose
  }

  _checkIdle(now: number) {
    const idle = this._idle
    const elapsed = now - this._lastActivity
    const cur = this.emotionId
    if (elapsed >= idle.sleepAfter) {
      if (cur !== idle.sleepId) this.setEmotion(idle.sleepId, { auto: true })
    } else if (elapsed >= idle.standbyAfter) {
      if (cur !== idle.standbyId && cur !== idle.sleepId) {
        this.setEmotion(idle.standbyId, { auto: true })
      }
    }
  }

  _compose(now: number, depth: number): any {
    const def = this._def
    const t = now - this._emoStart
    let pose: any

    if (this._seq) {
      const res = this._seqPose(t, now)
      if (res === 'switch') {
        return depth < 4 ? this._compose(now, depth + 1) : clonePose(this._def.base)
      }
      pose = res || clonePose(def.base)
    } else {
      pose = clonePose(def.base)
    }

    const br = pose.body.breathe || 0
    if (br) {
      const ph = (TAU * now) / 3600
      pose.body.scale += br * Math.sin(ph)
      pose.body.y += br * 55 * Math.sin(ph + 0.6)
    }

    for (let i = 0; i < def.anims.length; i++) applyAnim(pose, def.anims[i], t, this)

    pose.body.sketch = Math.max(pose.body.sketch || 0, this._style.sketch || 0)

    const dt = this._dt || 1 / 60

    if (this._active && now >= this._poolNext) {
      if (def.pool.length > 1) {
        this._poolPos = (this._poolPos + 1 + Math.floor(rand(0, def.pool.length - 1))) % def.pool.length
        this._setExpr(def.pool[this._poolPos], def.poolSpeed)
      }
      this._poolNext = now + rand(def.poolMs[0], def.poolMs[1])
    }

    if (this._active && def.blinkMs && now >= this._blinkNext) {
      this._blinkNow(now)
      this._blinkNext = now + rand(def.blinkMs[0], def.blinkMs[1])
    }
    let openKey = null
    while (this._blinkQ.length && now >= this._blinkQ[0].at) {
      openKey = this._blinkQ[0].v
      this._blinkQ.shift()
    }
    this._open.t = openKey != null ? openKey : this._blinkQ.length ? this._open.t : def.openness

    if (this._active && def.antics && now >= this._anticNext) {
      if (!this._spin && this._bounceAt < 0) {
        const pick = Math.random()
        if (pick < 0.45) this.spin(1)
        else if (pick < 0.8) this.bounce()
        else this._blinkNow(now)
      }
      this._anticNext = now + rand(9000, 18000)
    }

    const steps = Math.max(1, Math.ceil(dt / (1 / 120)))
    const j = dt / steps
    for (let si = 0; si < steps; si++) {
      springStep(this._ringSpring, this._ringSpeed, 1, j)
      springStep(this._open, 26, 1, j)
      if (this._spin) {
        springStep(this._spin, 6.2, 1, j)
        if (Math.abs(this._spin.t - this._spin.x) < 0.01 && Math.abs(this._spin.v) < 0.05) {
          this._spin = null
        }
      }
    }
    pose.body.yaw = this._spin ? this._spin.x : 0

    if (this._bounceAt >= 0) {
      const be = (now - this._bounceAt) / 1000
      if (be >= BOUNCE_TOTAL) {
        this._bounceAt = -1
      } else {
        let acc = 0,
          bi = 0
        while (bi < BOUNCE_SEGS.length && be >= acc + BOUNCE_SEGS[bi].d) {
          acc += BOUNCE_SEGS[bi].d
          bi++
        }
        const seg = BOUNCE_SEGS[Math.min(bi, BOUNCE_SEGS.length - 1)]
        const bn = (be - acc) / seg.d
        pose.body.y += -4 * seg.h * bn * (1 - bn)
      }
    }

    if (this._ringSpring.x < 0.999 || this._ringSpring.v > 0.001 || this._ringSpring.v < -0.001) {
      const rs = clamp(this._ringSpring.x, 0, 1.35)
      this._ringCur = [
        lerpRing(this._ringSrc[0], this._ringDst[0], rs),
        lerpRing(this._ringSrc[1], this._ringDst[1], rs)
      ]
    } else if (this._ringCur !== this._ringDst) {
      this._ringCur = this._ringDst
    }
    pose.left.ring = this._ringCur[0]
    pose.right.ring = this._ringCur[1]

    const k = 1 - Math.exp(-5.66 * dt)
    const gx = def.gaze !== false ? this._gaze.tx : 0
    const gy = def.gaze !== false ? this._gaze.ty : 0
    this._gaze.x += (gx - this._gaze.x) * k
    this._gaze.y += (gy - this._gaze.y) * k
    pose.left.lookX += this._gaze.x
    pose.right.lookX += this._gaze.x
    pose.left.lookY += this._gaze.y
    pose.right.lookY += this._gaze.y

    if (def.gaze !== false) {
      const w = now / 1000
      pose.left.lookX += 1.4 * Math.sin(0.42 * w) + 0.5 * Math.sin(1.0 * w)
      pose.right.lookX += 1.4 * Math.sin(0.42 * w + 1) + 0.5 * Math.sin(1.0 * w + 2)
      pose.left.lookY += 0.9 * Math.sin(0.58 * w)
      pose.right.lookY += 0.9 * Math.sin(0.58 * w + 1)
    }

    if (this._eyeScale !== 1) {
      pose.left.scaleX *= this._eyeScale
      pose.left.scaleY *= this._eyeScale
      pose.right.scaleX *= this._eyeScale
      pose.right.scaleY *= this._eyeScale
    }

    if (this._theme) {
      pose.body.color = this._theme.body
      if (pose.left.color === DEFAULT_EYE.color) pose.left.color = this._theme.eyes
      if (pose.right.color === DEFAULT_EYE.color) pose.right.color = this._theme.eyes
    }

    const openS = clamp(this._open.x, 0.02, 1.5)
    pose.left.open = clamp(pose.left.open, 0, 1.3) * openS
    pose.right.open = clamp(pose.right.open, 0, 1.3) * openS
    pose.left.scaleX = Math.max(pose.left.scaleX, 0.05)
    pose.left.scaleY = Math.max(pose.left.scaleY, 0.05)
    pose.right.scaleX = Math.max(pose.right.scaleX, 0.05)
    pose.right.scaleY = Math.max(pose.right.scaleY, 0.05)

    const tt = now - this._transStart
    if (this._transDur > 0 && tt < this._transDur && this._prevPose) {
      pose = lerpPose(this._prevPose, pose, easeInOutCubic(tt / this._transDur))
    }
    return pose
  }

  _seqPose(t: number, now: number) {
    const seq = this._seq
    const frames = seq.frames
    const last = frames[frames.length - 1]

    if (t >= last.at) {
      if (!seq.done) {
        seq.done = true
        const s = seq.settle
        if (s === 'base') {
          this._prevPose = this._lastPose ? clonePose(this._lastPose) : clonePose(last.pose)
          this._transStart = now
          this._transDur = this._def.transition || 500
          this._seq = null
          return null
        }
        if (s && typeof s === 'object' && s.next) {
          this.setEmotion(s.next, { auto: true })
          return 'switch'
        }
      }
      return clonePose(last.pose)
    }

    if (t <= frames[0].at) return clonePose(frames[0].pose)
    for (let i = 0; i < frames.length - 1; i++) {
      const a = frames[i],
        b = frames[i + 1]
      if (t >= a.at && t < b.at) {
        const k = easeInOutCubic((t - a.at) / (b.at - a.at))
        return lerpPose(a.pose, b.pose, k)
      }
    }
    return clonePose(last.pose)
  }
}

export function createEmotionBall(target: HTMLElement | string, opts?: EngineOptions): Engine {
  return new Engine(target, opts)
}
