export interface PlanetConfig {
  name: string
  color: string
  emissive: string
  size: number
  orbitRadius: number
  orbitSpeed: number
  rotationSpeed: number
  tilt: number
  description: string
  metalness: number
  roughness: number
}

export interface CategoryStats {
  name: string
  count: number
  lastUpdated: string
  config: PlanetConfig
}

export const PLANET_CONFIGS: PlanetConfig[] = [
  {
    name: '技术',
    color: '#3B82F6',
    emissive: '#60A5FA',
    size: 4,
    orbitRadius: 45,
    orbitSpeed: 0.06,
    rotationSpeed: 0.008,
    tilt: 0.15,
    description: '代码、踩坑、折腾',
    metalness: 0.7,
    roughness: 0.25,
  },
  {
    name: '生活',
    color: '#67E8F9',
    emissive: '#22D3EE',
    size: 3.5,
    orbitRadius: 60,
    orbitSpeed: 0.05,
    rotationSpeed: 0.007,
    tilt: -0.1,
    description: '一些有的没的',
    metalness: 0.5,
    roughness: 0.4,
  },
  {
    name: '随笔',
    color: '#F472B6',
    emissive: '#EC4899',
    size: 3,
    orbitRadius: 75,
    orbitSpeed: 0.045,
    rotationSpeed: 0.009,
    tilt: 0.2,
    description: '想到哪写到哪',
    metalness: 0.6,
    roughness: 0.3,
  },
  {
    name: '项目',
    color: '#FBBF24',
    emissive: '#F59E0B',
    size: 3.8,
    orbitRadius: 90,
    orbitSpeed: 0.04,
    rotationSpeed: 0.006,
    tilt: -0.2,
    description: '做过的项目记录',
    metalness: 0.65,
    roughness: 0.28,
  },
  {
    name: '设计',
    color: '#34D399',
    emissive: '#10B981',
    size: 3.2,
    orbitRadius: 105,
    orbitSpeed: 0.035,
    rotationSpeed: 0.01,
    tilt: 0.12,
    description: '好看就行',
    metalness: 0.55,
    roughness: 0.35,
  },
  {
    name: '阅读',
    color: '#FB923C',
    emissive: '#F97316',
    size: 2.8,
    orbitRadius: 120,
    orbitSpeed: 0.03,
    rotationSpeed: 0.008,
    tilt: -0.08,
    description: '读过的东西',
    metalness: 0.5,
    roughness: 0.4,
  },
]
