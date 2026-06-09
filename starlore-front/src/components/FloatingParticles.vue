<template>
  <div ref="container" class="floating-particles"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'

const props = withDefaults(defineProps<{
  color?: string
  particleCount?: number
}>(), {
  color: '#6D63FF',
  particleCount: 2000,
})

const container = ref<HTMLDivElement>()

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let animationId: number | null = null
let material: THREE.ShaderMaterial | null = null

const AMBIENT_COUNT = props.particleCount
const SHELL_RADIUS = 30

const vertexShader = `
  attribute float aSize;
  attribute float aAlpha;
  attribute float aPhase;
  varying float vAlpha;
  varying float vPhase;
  uniform float uTime;
  void main() {
    vec4 mvPos = modelViewMatrix * vec4(position, 1.0);
    float pulse = 0.7 + 0.3 * sin(uTime * 1.5 + aPhase);
    gl_PointSize = aSize * pulse * (150.0 / -mvPos.z);
    gl_Position = projectionMatrix * mvPos;
    vAlpha = aAlpha * (0.6 + 0.4 * sin(uTime * 0.8 + aPhase + 1.0));
    vPhase = aPhase;
  }
`

const fragmentShader = `
  varying float vAlpha;
  varying float vPhase;
  uniform vec3 uColor;
  uniform float uTime;
  void main() {
    float d = length(gl_PointCoord - 0.5) * 2.0;
    float alpha = 1.0 - smoothstep(0.0, 1.0, d);
    alpha = pow(alpha, 2.0);
    float glow = exp(-d * 3.0) * 0.5;
    alpha = (alpha + glow) * vAlpha * 0.7;
    gl_FragColor = vec4(uColor, alpha);
  }
`

function createParticleField(): THREE.Points {
  const count = AMBIENT_COUNT
  const pos = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  const alphas = new Float32Array(count)
  const phases = new Float32Array(count)

  for (let i = 0; i < count; i++) {
    const phi = Math.acos(2 * Math.random() - 1)
    const theta = Math.random() * Math.PI * 2
    const r = SHELL_RADIUS * (0.3 + Math.random() * 0.7)
    pos[i * 3] = r * Math.sin(phi) * Math.cos(theta)
    pos[i * 3 + 1] = r * Math.cos(phi)
    pos[i * 3 + 2] = r * Math.sin(phi) * Math.sin(theta)
    sizes[i] = 0.3 + Math.random() * 1.2
    alphas[i] = 0.1 + Math.random() * 0.4
    phases[i] = Math.random() * Math.PI * 2
  }

  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  geo.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
  geo.setAttribute('aAlpha', new THREE.BufferAttribute(alphas, 1))
  geo.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))

  material = new THREE.ShaderMaterial({
    vertexShader,
    fragmentShader,
    uniforms: {
      uTime: { value: 0 },
      uColor: { value: new THREE.Color(props.color) },
    },
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  })

  return new THREE.Points(geo, material)
}

function createNebulaLayer(): THREE.Points {
  const count = 300
  const pos = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  const alphas = new Float32Array(count)
  const phases = new Float32Array(count)

  for (let i = 0; i < count; i++) {
    const phi = Math.acos(2 * Math.random() - 1)
    const theta = Math.random() * Math.PI * 2
    const r = SHELL_RADIUS * (0.5 + Math.random() * 0.5)
    pos[i * 3] = r * Math.sin(phi) * Math.cos(theta)
    pos[i * 3 + 1] = r * Math.cos(phi)
    pos[i * 3 + 2] = r * Math.sin(phi) * Math.sin(theta)
    sizes[i] = 2 + Math.random() * 4
    alphas[i] = 0.02 + Math.random() * 0.06
    phases[i] = Math.random() * Math.PI * 2
  }

  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  geo.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
  geo.setAttribute('aAlpha', new THREE.BufferAttribute(alphas, 1))
  geo.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))

  const mat = new THREE.ShaderMaterial({
    vertexShader,
    fragmentShader,
    uniforms: {
      uTime: { value: 0 },
      uColor: { value: new THREE.Color(props.color).multiplyScalar(0.6) },
    },
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  })

  return new THREE.Points(geo, mat)
}

function init() {
  if (!container.value) return

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(50, container.value.clientWidth / container.value.clientHeight, 0.1, 100)
  camera.position.set(0, 0, 8)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(container.value.clientWidth, container.value.clientHeight)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5))
  renderer.setClearColor(0x000000, 0)
  container.value.appendChild(renderer.domElement)

  scene.add(createParticleField())
  scene.add(createNebulaLayer())

  let isDragging = false
  let prevMouse = { x: 0, y: 0 }
  let theta = 0, phi = Math.PI / 2, radius = 8

  const onDown = (e: MouseEvent) => {
    isDragging = true
    prevMouse = { x: e.clientX, y: e.clientY }
  }
  const onMove = (e: MouseEvent) => {
    if (!isDragging) return
    theta -= (e.clientX - prevMouse.x) * 0.005
    phi = Math.max(0.3, Math.min(2.8, phi - (e.clientY - prevMouse.y) * 0.005))
    prevMouse = { x: e.clientX, y: e.clientY }
  }
  const onUp = () => { isDragging = false }
  const onWheel = (e: WheelEvent) => {
    radius = Math.max(4, Math.min(15, radius + e.deltaY * 0.01))
  }

  renderer.domElement.addEventListener('mousedown', onDown)
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
  renderer.domElement.addEventListener('wheel', onWheel, { passive: true })

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

    if (!isDragging) theta += delta * 0.1

    camera!.position.x = radius * Math.sin(phi) * Math.cos(theta)
    camera!.position.y = radius * Math.cos(phi)
    camera!.position.z = radius * Math.sin(phi) * Math.sin(theta)
    camera!.lookAt(0, 0, 0)

    if (material) material.uniforms.uTime.value = elapsed

    scene?.children.forEach(child => {
      if (child instanceof THREE.Points && child.material instanceof THREE.ShaderMaterial) {
        child.rotation.y += delta * 0.02
      }
    })

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
.floating-particles {
  width: 100%;
  height: 100%;
  position: relative;
}
.floating-particles :deep(canvas) {
  display: block;
}
</style>
