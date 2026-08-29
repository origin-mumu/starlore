/* ============================================================
 * math.ts —— 物理数学计算与姿态插值工具函数
 * ============================================================ */

export const TAU = Math.PI * 2

export function clamp(v: number, a: number, b: number): number {
  return v < a ? a : v > b ? b : v
}

export function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t
}

export function rand(a: number, b: number): number {
  return a + Math.random() * (b - a)
}

export function easeInOutCubic(t: number): number {
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
}

export interface SpringState {
  x: number
  v: number
  t: number
}

export function spring(v0: number): SpringState {
  return { x: v0, v: 0, t: v0 }
}

export function springStep(s: SpringState, w: number, z: number, dt: number): void {
  s.v += (-2 * z * w * s.v - w * w * (s.x - s.t)) * dt
  s.x += s.v * dt
  if (!isFinite(s.x) || !isFinite(s.v)) {
    s.x = s.t
    s.v = 0
  }
}

export function lerpRing(a: [number, number][], b: [number, number][], t: number): [number, number][] {
  const out = new Array(a.length)
  for (let i = 0; i < a.length; i++) {
    out[i] = [a[i][0] + (b[i][0] - a[i][0]) * t, a[i][1] + (b[i][1] - a[i][1]) * t]
  }
  return out
}

export const BOUNCE_SEGS = [
  { h: 48, d: 0.5 },
  { h: 28, d: 0.382 },
  { h: 14, d: 0.27 },
  { h: 6, d: 0.177 }
]
export const BOUNCE_TOTAL = BOUNCE_SEGS.reduce((s, q) => s + q.d, 0)

export function hexToRgb(hex: string): [number, number, number] {
  let h = hex.replace('#', '')
  if (h.length === 3) h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2]
  const n = parseInt(h, 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

export function rgbToHex(r: number, g: number, b: number): string {
  return (
    '#' +
    [r, g, b]
      .map((v) => clamp(Math.round(v), 0, 255).toString(16).padStart(2, '0'))
      .join('')
  )
}

export function lerpColor(a: string, b: string, t: number): string {
  if (a === b) return b
  const A = hexToRgb(a)
  const B = hexToRgb(b)
  return rgbToHex(lerp(A[0], B[0], t), lerp(A[1], B[1], t), lerp(A[2], B[2], t))
}

export const DEFAULT_BODY = {
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
export const DEFAULT_EYE = { x: 0, y: 0, scaleX: 1, scaleY: 1, rotate: 0, open: 1, color: '#1A1A1A', lookX: 0, lookY: 0 }

export function defaultPose() {
  return {
    body: Object.assign({}, DEFAULT_BODY),
    left: Object.assign({}, DEFAULT_EYE),
    right: Object.assign({}, DEFAULT_EYE)
  }
}

export function clonePose(p: any) {
  return {
    body: Object.assign({}, p.body),
    left: Object.assign({}, p.left),
    right: Object.assign({}, p.right)
  }
}

export function applySpec(pose: any, spec: any) {
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

export function lerpPose(a: any, b: any, t: number) {
  const out = defaultPose()
  ;(['body', 'left', 'right'] as const).forEach((part) => {
    const pa = a[part]
    const pb = b[part]
    const po = (out as any)[part]
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
    const interval = a.interval || 3800
    const dur = a.dur || 200
    const p = (t + (a.phaseMs || 0) + (eng ? eng._seed * 97 : 0)) % interval
    if (p >= dur) return 0
    return -(a.depth == null ? 1 : a.depth) * Math.sin(Math.PI * (p / dur))
  }
}

export function applyAnim(pose: any, a: any, t: number, eng: any) {
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
