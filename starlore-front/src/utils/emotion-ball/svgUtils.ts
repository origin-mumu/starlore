/* ============================================================
 * svgUtils.ts —— SVG DOM 创建、几何与色彩处理工具
 * ============================================================ */

import { EB_RINGS } from './rings'

export const SVGNS = 'http://www.w3.org/2000/svg'
export const TAU = Math.PI * 2

export const HEAD_C = EB_RINGS.HEAD_C /* 114.2705 */
export const EYE_HALF = EB_RINGS.EYE_HALF /* 21 */
export const EXPR = EB_RINGS.EXPRESSIONS
export const STAR_GOLD = EB_RINGS.STAR_GOLD
export const CONFETTI_COLORS = ['#f9705c', '#5b95f0', '#3fbe86', '#f5b13f', '#9a72ee', '#35c3bd']

export const STAR_PATH = (function () {
  const pts = []
  for (let e = 0; e < 10; e++) {
    const a = -Math.PI / 2 + (e * Math.PI) / 5
    const r = e % 2 === 0 ? 1 : 0.42
    pts.push((Math.cos(a) * r).toFixed(3) + ' ' + (Math.sin(a) * r).toFixed(3))
  }
  return 'M' + pts.join('L') + 'Z'
})()

export function el(tag: string, attrs: Record<string, string>): SVGElement {
  const node = document.createElementNS(SVGNS, tag)
  for (const k in attrs) node.setAttribute(k, attrs[k])
  return node
}

export function r2(v: number): number {
  return Math.round(v * 100) / 100
}

export function clamp(v: number, a: number, b: number): number {
  return v < a ? a : v > b ? b : v
}

export function rand(a: number, b: number): number {
  return a + Math.random() * (b - a)
}

export function shade(hex: string, amt: number): string {
  let h = hex.replace('#', '')
  if (h.length === 3) h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2]
  const n = parseInt(h, 16)
  let r = (n >> 16) & 255
  let g = (n >> 8) & 255
  let b = n & 255
  const target = amt < 0 ? 0 : 255
  const a = Math.abs(amt)
  r = Math.round(r + (target - r) * a)
  g = Math.round(g + (target - g) * a)
  b = Math.round(b + (target - b) * a)
  return '#' + ((1 << 24) | (r << 16) | (g << 8) | b).toString(16).slice(1)
}

export function ringPath(ring: [number, number][]): string {
  let s = 'M'
  for (let i = 0; i < ring.length; i++) {
    s += (i ? 'L' : '') + ring[i][0].toFixed(2) + ' ' + ring[i][1].toFixed(2)
  }
  return s + 'Z'
}

export function centroid(ring: [number, number][]): [number, number] {
  let x = 0, y = 0
  for (let i = 0; i < ring.length; i++) {
    x += ring[i][0]
    y += ring[i][1]
  }
  return [x / ring.length, y / ring.length]
}
