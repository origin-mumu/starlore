import * as THREE from 'three'
import { PLANET_CONFIGS, type PlanetConfig } from './planetData'

export interface PlanetItem {
  id: string | number
  name: string
  type: 'category' | 'article' | 'more'
  count?: number
  lastUpdated?: string
  articleId?: number
}

export interface PlanetGroup extends THREE.Group {
  userData: {
    config: PlanetConfig
    articleCount: number
    lastUpdated: string
    orbitAngle: number
    orbitRadius: number
    speed: number
    planetItem: PlanetItem
  }
}

export function createGlowTexture(colorStr: string): THREE.CanvasTexture {
  const size = 32
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')!
  const g = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2)
  g.addColorStop(0, colorStr)
  g.addColorStop(0.4, colorStr)
  g.addColorStop(0.8, 'transparent')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, size, size)
  return new THREE.CanvasTexture(canvas)
}

export function createLabelSprite(text: string, color: string, fontSize = 24): THREE.Sprite {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')!
  canvas.width = 512
  canvas.height = 128
  ctx.clearRect(0, 0, 512, 128)
  ctx.font = `bold ${fontSize}px "PingFang SC", "Microsoft YaHei", sans-serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.shadowColor = 'rgba(0,0,0,0.9)'
  ctx.shadowBlur = 12
  ctx.fillStyle = color

  let displayText = text
  const measured = ctx.measureText(text)
  if (measured.width > 480) {
    while (ctx.measureText(displayText + '...').width > 480 && displayText.length > 0) {
      displayText = displayText.slice(0, -1)
    }
    displayText += '...'
  }
  ctx.fillText(displayText, 256, 64)

  const tex = new THREE.CanvasTexture(canvas)
  return new THREE.Sprite(
    new THREE.SpriteMaterial({ map: tex, transparent: true, opacity: 0.8, depthTest: false }),
  )
}

export function getPlanetConfig(name: string): PlanetConfig {
  return PLANET_CONFIGS.find(p => p.name === name) || {
    name,
    color: '#A59DFF',
    emissive: '#B8B3FF',
    size: 3,
    orbitRadius: 3 + Math.random() * 2,
    orbitSpeed: 0.15 + Math.random() * 0.1,
    rotationSpeed: 0.007,
    tilt: 0,
    description: '',
    metalness: 0.6,
    roughness: 0.3,
  }
}
