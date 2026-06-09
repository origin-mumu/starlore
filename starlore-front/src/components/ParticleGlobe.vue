<template>
  <div ref="container" class="particle-globe-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'

const props = withDefaults(defineProps<{
  color?: string
  particleCount?: number
}>(), {
  color: '#6D63FF',
  particleCount: 3000,
})

const container = ref<HTMLDivElement>()

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let animationId: number | null = null

// Materials that need uniform updates
let atmosphereMat: THREE.ShaderMaterial | null = null
let starMat: THREE.ShaderMaterial | null = null
let starPoints: THREE.Points | null = null
let pulseData: { t: number; speed: number; curve: THREE.QuadraticBezierCurve3 }[] = [] // declared first
let pulseMeshes: THREE.Mesh[] = []

const GLOBE_RADIUS = 1.8
const ATMO_RADIUS = 1.95
const STAR_COUNT = 1500
const SHELL_RADIUS = 40

function isLand(lat: number, lng: number): number {
  const regions: [number, number, number, number][] = [
    [15, 70, -170, -50],
    [-55, 12, -80, -35],
    [35, 70, -10, 40],
    [-35, 37, -20, 50],
    [10, 75, 40, 180],
    [-40, -10, 110, 155],
    [-10, 10, 95, 140],
    [50, 70, 130, 180],
    [-85, -60, -70, -20],
  ]
  for (const [latMin, latMax, lngMin, lngMax] of regions) {
    if (lat >= latMin && lat <= latMax && lng >= lngMin && lng <= lngMax) {
      return 0.6 + Math.random() * 0.4
    }
  }
  return Math.random() * 0.15
}

function latLngToVec3(lat: number, lng: number, radius: number): THREE.Vector3 {
  const phi = (90 - lat) * (Math.PI / 180)
  const theta = (lng + 180) * (Math.PI / 180)
  return new THREE.Vector3(
    -radius * Math.sin(phi) * Math.cos(theta),
    radius * Math.cos(phi),
    radius * Math.sin(phi) * Math.sin(theta),
  )
}

function createGlobeParticles(color: string): THREE.Points {
  const count = props.particleCount
  const pos = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  const phi = Math.PI * (3 - Math.sqrt(5))

  for (let i = 0; i < count; i++) {
    const y = 1 - (i / (count - 1)) * 2
    const r = Math.sqrt(1 - y * y)
    const theta = phi * i
    pos[i * 3] = Math.cos(theta) * r * GLOBE_RADIUS
    pos[i * 3 + 1] = y * GLOBE_RADIUS
    pos[i * 3 + 2] = Math.sin(theta) * r * GLOBE_RADIUS

    const lat = Math.asin(y) * (180 / Math.PI)
    const lng = Math.atan2(pos[i * 3 + 2], pos[i * 3]) * (180 / Math.PI)
    sizes[i] = isLand(lat, lng) * (0.025 + Math.random() * 0.04)
  }

  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  geo.setAttribute('size', new THREE.BufferAttribute(sizes, 1))

  const mat = new THREE.PointsMaterial({
    size: 0.022,
    color: new THREE.Color(color),
    blending: THREE.AdditiveBlending,
    depthWrite: true,
    transparent: true,
    opacity: 0.85,
    sizeAttenuation: true,
  })

  return new THREE.Points(geo, mat)
}

function createAtmosphere(color: string): THREE.Mesh {
  const vs = `
    varying vec3 vNormal;
    varying vec3 vViewDir;
    void main() {
      vec4 mvPos = modelViewMatrix * vec4(position, 1.0);
      vNormal = normalize(mat3(modelViewMatrix) * normal);
      vViewDir = normalize(-mvPos.xyz);
      gl_Position = projectionMatrix * mvPos;
    }
  `
  const fs = `
    varying vec3 vNormal;
    varying vec3 vViewDir;
    uniform vec3 uColor;
    uniform float uTime;
    void main() {
      float fresnel = 1.0 - abs(dot(vNormal, vViewDir));
      fresnel = pow(fresnel, 3.5);
      float alpha = fresnel * 0.35 * (0.8 + 0.2 * sin(uTime * 0.5));
      gl_FragColor = vec4(uColor, alpha);
    }
  `
  atmosphereMat = new THREE.ShaderMaterial({
    vertexShader: vs,
    fragmentShader: fs,
    uniforms: {
      uColor: { value: new THREE.Color(color) },
      uTime: { value: 0 },
    },
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  })
  return new THREE.Mesh(new THREE.SphereGeometry(ATMO_RADIUS, 64, 64), atmosphereMat)
}

function createStarField(color: string): THREE.Points {
  const vs = `
    attribute float aSize;
    attribute float aPhase;
    varying float vAlpha;
    varying float vBright;
    uniform float uTime;
    void main() {
      vec4 mvPos = modelViewMatrix * vec4(position, 1.0);
      gl_PointSize = aSize * (180.0 / -mvPos.z);
      gl_PointSize *= 0.55 + 0.45 * sin(uTime * 1.8 + aPhase);
      gl_Position = projectionMatrix * mvPos;
      vAlpha = 0.4 + 0.6 * sin(uTime * 1.3 + aPhase + 1.7);
      vBright = aSize;
    }
  `
  const fs = `
    varying float vAlpha;
    varying float vBright;
    uniform vec3 uColor;
    void main() {
      float d = length(gl_PointCoord - 0.5) * 2.0;
      float alpha = (1.0 - smoothstep(0.15, 1.0, d)) * vAlpha * (0.35 + 0.65 * vBright);
      gl_FragColor = vec4(uColor, alpha * 0.85);
    }
  `

  const phi = Math.PI * (3 - Math.sqrt(5))
  const pos = new Float32Array(STAR_COUNT * 3)
  const sizes = new Float32Array(STAR_COUNT)
  const phases = new Float32Array(STAR_COUNT)

  for (let i = 0; i < STAR_COUNT; i++) {
    const y = 1 - (i / (STAR_COUNT - 1)) * 2
    const r = Math.sqrt(1 - y * y)
    const theta = phi * i + (Math.random() - 0.5) * 0.15
    pos[i * 3] = Math.cos(theta) * r * SHELL_RADIUS
    pos[i * 3 + 1] = y * SHELL_RADIUS
    pos[i * 3 + 2] = Math.sin(theta) * r * SHELL_RADIUS
    sizes[i] = 0.2 + Math.random() * 0.8
    phases[i] = Math.random() * Math.PI * 2
  }

  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  geo.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
  geo.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))

  starMat = new THREE.ShaderMaterial({
    vertexShader: vs,
    fragmentShader: fs,
    uniforms: {
      uTime: { value: 0 },
      uColor: { value: new THREE.Color(color) },
    },
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  })

  starPoints = new THREE.Points(geo, starMat)
  return starPoints
}

function createNeuralNetwork(color: string, scene: THREE.Scene) {
  const nodeCount = 12
  const nodes: { lat: number; lng: number }[] = []
  for (let i = 0; i < nodeCount; i++) {
    nodes.push({
      lat: (Math.random() - 0.5) * 160,
      lng: (Math.random() - 0.5) * 340,
    })
  }

  const connections: { from: number; to: number }[] = []
  for (let i = 0; i < nodeCount; i++) {
    for (let j = i + 1; j < nodeCount; j++) {
      if (Math.random() < 0.35) connections.push({ from: i, to: j })
    }
  }

  const curves: THREE.QuadraticBezierCurve3[] = []
  const lineMat = new THREE.LineBasicMaterial({
    color: new THREE.Color(color),
    transparent: true,
    opacity: 0.2,
    depthWrite: true,
  })

  for (const conn of connections) {
    const start = latLngToVec3(nodes[conn.from].lat, nodes[conn.from].lng, GLOBE_RADIUS)
    const end = latLngToVec3(nodes[conn.to].lat, nodes[conn.to].lng, GLOBE_RADIUS)
    const mid = new THREE.Vector3().addVectors(start, end).multiplyScalar(0.5)
    mid.normalize().multiplyScalar(GLOBE_RADIUS * 1.45)
    const curve = new THREE.QuadraticBezierCurve3(start, mid, end)
    curves.push(curve)
    const pts = curve.getPoints(50)
    scene.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(pts), lineMat))
  }

  // Node markers
  const nodeGeo = new THREE.IcosahedronGeometry(0.03, 0)
  const nodeMat = new THREE.MeshBasicMaterial({ color: new THREE.Color(color) })
  for (const node of nodes) {
    const mesh = new THREE.Mesh(nodeGeo, nodeMat)
    mesh.position.copy(latLngToVec3(node.lat, node.lng, GLOBE_RADIUS))
    scene.add(mesh)
  }

  // Pulse dots
  const pulseGeo = new THREE.SphereGeometry(0.012, 4, 4)
  const pulseMat = new THREE.MeshBasicMaterial({ color: 0xffffff })
  const count = Math.min(40, connections.length * 3)

  pulseData = []
  pulseMeshes = []
  for (let i = 0; i < count; i++) {
    const mesh = new THREE.Mesh(pulseGeo, pulseMat)
    scene.add(mesh)
    pulseMeshes.push(mesh)
    pulseData.push({
      t: Math.random(),
      speed: 0.08 + Math.random() * 0.2,
      curve: curves[i % curves.length],
    })
  }
}

function init() {
  if (!container.value) return

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(42, container.value.clientWidth / container.value.clientHeight, 0.1, 80)
  camera.position.set(0, 0.6, 5.5)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(container.value.clientWidth, container.value.clientHeight)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5))
  renderer.setClearColor(0x000000, 0)
  container.value.appendChild(renderer.domElement)

  scene.add(createGlobeParticles(props.color))
  scene.add(createAtmosphere(props.color))
  scene.add(createStarField(props.color))
  createNeuralNetwork(props.color, scene)

  // Manual orbit
  let isDragging = false
  let prevMouse = { x: 0, y: 0 }
  let theta = 0, phi = Math.PI / 4, radius = 5.5

  const onDown = (e: MouseEvent) => {
    isDragging = true
    prevMouse = { x: e.clientX, y: e.clientY }
  }
  const onMove = (e: MouseEvent) => {
    if (!isDragging) return
    theta -= (e.clientX - prevMouse.x) * 0.005
    phi = Math.max(0.25, Math.min(2.4, phi - (e.clientY - prevMouse.y) * 0.005))
    prevMouse = { x: e.clientX, y: e.clientY }
  }
  const onUp = () => { isDragging = false }
  const onWheel = (e: WheelEvent) => {
    radius = Math.max(3.2, Math.min(9, radius + e.deltaY * 0.005))
  }

  renderer.domElement.addEventListener('mousedown', onDown)
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
  renderer.domElement.addEventListener('wheel', onWheel, { passive: true })

  // Cleanup function
  ;(renderer as any).__cleanup = () => {
    renderer?.domElement.removeEventListener('mousedown', onDown)
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
    renderer?.domElement.removeEventListener('wheel', onWheel)
  }

  const clock = new THREE.Clock()

  const animate = () => {
    animationId = requestAnimationFrame(animate)
    const delta = clock.getDelta()
    const elapsed = clock.getElapsedTime()

    if (!isDragging) theta += delta * 0.15

    if (camera) {
      camera.position.x = radius * Math.sin(phi) * Math.cos(theta)
      camera.position.y = radius * Math.cos(phi)
      camera.position.z = radius * Math.sin(phi) * Math.sin(theta)
      camera.lookAt(0, 0, 0)
    }

    if (atmosphereMat) atmosphereMat.uniforms.uTime.value = elapsed
    if (starMat) starMat.uniforms.uTime.value = elapsed
    if (starPoints) starPoints.rotation.y += delta * 0.015

    for (let i = 0; i < pulseMeshes.length; i++) {
      const pd = pulseData[i]
      pd.t += pd.speed * delta
      if (pd.t > 1) pd.t -= 1
      const pt = pd.curve.getPoint(pd.t)
      pulseMeshes[i].position.copy(pt)
    }

    renderer?.render(scene!, camera!)
  }

  animate()
}

function handleResize() {
  if (!container.value || !renderer || !camera) return
  camera.aspect = container.value.clientWidth / container.value.clientHeight
  camera.updateProjectionMatrix()
  renderer.setSize(container.value.clientWidth, container.value.clientHeight)
}

onMounted(() => {
  init()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (animationId !== null) cancelAnimationFrame(animationId)
  if (renderer) {
    const cleanup = (renderer as any).__cleanup
    if (cleanup) cleanup()
    renderer.dispose()
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.particle-globe-container {
  width: 100%;
  height: 100%;
  position: relative;
}
.particle-globe-container :deep(canvas) {
  display: block;
}
</style>
