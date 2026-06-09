<template>
  <div ref="container" class="geo-nexus-globe"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as THREE from 'three'
import { Clock } from 'three'
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js'

const props = withDefaults(defineProps<{
  state?: number // 0=平常, 1=思考, 2=回答
}>(), {
  state: 0,
})

const container = ref<HTMLDivElement>()

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let animationId: number | null = null
let composer: EffectComposer | null = null
let bloomPass: UnrealBloomPass | null = null
let particleSystem: THREE.Points | null = null
let uniforms: Record<string, THREE.IUniform> | null = null

const PARTICLE_COUNT = 12000

/* ─── 着色器 ─── */

const vertexShader = `
  uniform float uTime;
  uniform float uState;
  attribute float aRandom;
  attribute float aSize;
  varying float vAlpha;
  varying vec3 vPosition;
  varying float vDistToCenter;

  float random(vec3 st) { return fract(sin(dot(st.xyz, vec3(12.9898,78.233,45.5432))) * 43758.5453123); }
  float noise(vec3 p) { vec3 i = floor(p); vec3 f = fract(p); f = f*f*(3.0-2.0*f); return mix(mix(mix(random(i + vec3(0,0,0)), random(i + vec3(1,0,0)),f.x), mix(random(i + vec3(0,1,0)), random(i + vec3(1,1,0)),f.x),f.y), mix(mix(random(i + vec3(0,0,1)), random(i + vec3(1,0,1)),f.x), mix(random(i + vec3(0,1,1)), random(i + vec3(1,1,1)),f.x),f.y),f.z); }

  void main() {
    vec3 pos = position;
    vec3 normalPos = normalize(pos);

    // 平常状态：轻柔呼吸
    float normalNoise = sin(pos.x * 0.3 + uTime * 0.8) * cos(pos.y * 0.3 - uTime * 0.8) * 0.8;

    // 思考状态：解构扭曲
    float thinkingNoise = noise(pos * 0.15 + uTime * 0.5) * 5.0 * sin(uTime);

    // 回答状态：舒缓能量光环
    float smoothScan = sin(pos.y * 0.4 - uTime * 3.0) * 1.2;
    float gentleBreath = sin(uTime * 1.5) * 0.6;
    float theta = atan(pos.z, pos.x);
    float softOrbit = cos(theta * 2.0 + uTime * 1.0) * 0.4;
    float answeringNoise = smoothScan + gentleBreath + softOrbit;

    float displacement = 0.0;
    float mixThinking = smoothstep(0.0, 1.0, uState) - smoothstep(1.0, 2.0, uState);
    float mixAnswering = smoothstep(1.0, 2.0, uState);

    displacement += normalNoise * (1.0 - smoothstep(0.0, 1.0, uState));
    displacement += thinkingNoise * mixThinking;
    displacement += answeringNoise * mixAnswering;

    vec3 newPos = pos + normalPos * displacement;
    vec4 mvPosition = modelViewMatrix * vec4(newPos, 1.0);

    vDistToCenter = length(newPos) / 7.0;

    float stateSizeBoost = 1.0 + mixAnswering * 0.15;
    gl_PointSize = aSize * stateSizeBoost * (30.0 / -mvPosition.z);
    gl_Position = projectionMatrix * mvPosition;

    vAlpha = aRandom;
    vPosition = newPos;
  }
`

const fragmentShader = `
  uniform float uState;
  uniform vec3 colorNormal;
  uniform vec3 colorThinking;
  uniform vec3 colorAnswering;
  varying float vAlpha;
  varying vec3 vPosition;
  varying float vDistToCenter;

  void main() {
    vec2 center = gl_PointCoord - vec2(0.5);
    float dist = length(center);
    float circleAlpha = smoothstep(0.5, 0.3, dist);
    if (circleAlpha < 0.01) discard;

    vec3 targetColor = colorNormal;
    float mixThinking = smoothstep(0.0, 1.0, uState) - smoothstep(1.0, 2.0, uState);
    float mixAnswering = smoothstep(1.0, 2.0, uState);
    targetColor = mix(targetColor, colorThinking, mixThinking);
    targetColor = mix(targetColor, colorAnswering, mixAnswering);

    vec3 highlightColor = targetColor + vec3(0.3);
    vec3 finalColor = mix(highlightColor, targetColor, dist * 1.8);
    finalColor += vec3(0.15) * (1.0 - vDistToCenter);

    float finalAlpha = circleAlpha * (0.7 + vAlpha * 0.3);
    gl_FragColor = vec4(finalColor, finalAlpha);
  }
`

/* ─── 初始化 ─── */
function init() {
  if (!container.value) return

  // 场景
  scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x020305, 0.012)

  const rect = container.value.getBoundingClientRect()
  camera = new THREE.PerspectiveCamera(45, rect.width / rect.height, 1, 1000)
  camera.position.z = 45

  // 渲染器
  renderer = new THREE.WebGLRenderer({ antialias: false, alpha: true })
  renderer.setSize(rect.width, rect.height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5))
  renderer.toneMapping = THREE.ReinhardToneMapping
  renderer.toneMappingExposure = 1.0
  container.value.appendChild(renderer.domElement)

  // 后期处理 - Bloom 辉光
  const renderScene = new RenderPass(scene, camera)
  bloomPass = new UnrealBloomPass(
    new THREE.Vector2(rect.width, rect.height),
    0.8, // strength
    0.4, // radius
    0.15 // threshold
  )
  composer = new EffectComposer(renderer)
  composer.addPass(renderScene)
  composer.addPass(bloomPass)

  /* ─── 粒子 ─── */
  const positions = new Float32Array(PARTICLE_COUNT * 3)
  const randoms = new Float32Array(PARTICLE_COUNT)
  const sizes = new Float32Array(PARTICLE_COUNT)

  for (let i = 0; i < PARTICLE_COUNT; i++) {
    const phi = Math.acos(-1 + (2 * i) / PARTICLE_COUNT)
    const theta = Math.sqrt(PARTICLE_COUNT * Math.PI) * phi
    const radius = 7
    positions[i * 3] = radius * Math.cos(theta) * Math.sin(phi)
    positions[i * 3 + 1] = radius * Math.sin(theta) * Math.sin(phi)
    positions[i * 3 + 2] = radius * Math.cos(phi)
    randoms[i] = Math.random()
    sizes[i] = Math.random() * 2.5 + 0.8
  }

  const geometry = new THREE.BufferGeometry()
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geometry.setAttribute('aRandom', new THREE.BufferAttribute(randoms, 1))
  geometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))

  uniforms = {
    uTime: { value: 0.0 },
    uState: { value: props.state },
    colorNormal: { value: new THREE.Color('#00e5ff') },
    colorThinking: { value: new THREE.Color('#8a2be2') },
    colorAnswering: { value: new THREE.Color('#ffb700') },
  }

  const material = new THREE.ShaderMaterial({
    uniforms,
    vertexShader,
    fragmentShader,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  })

  particleSystem = new THREE.Points(geometry, material)
  scene.add(particleSystem)

  /* ─── 动画 ─── */
  const clock = new Clock()
  let targetState = props.state

  function animate() {
    animationId = requestAnimationFrame(animate)
    const elapsedTime = clock.getElapsedTime()
    uniforms!.uTime.value = elapsedTime
    uniforms!.uState.value += (targetState - uniforms!.uState.value) * 0.08

    particleSystem!.rotation.y = elapsedTime * 0.15
    particleSystem!.rotation.z = Math.cos(elapsedTime * 0.1) * 0.1

    composer!.render()
  }
  animate()

  /* ─── 状态切换 ─── */
  ;(renderer as any).__setState = (s: number) => {
    targetState = s
    if (bloomPass) {
      const orig = bloomPass.strength
      bloomPass.strength = 1.8
      setTimeout(() => { bloomPass!.strength = orig }, 250)
    }
  }
}

/* ─── 监听外部 state 变化 ─── */
watch(() => props.state, (val) => {
  if (renderer && (renderer as any).__setState) {
    (renderer as any).__setState(val)
  }
})

function handleResize() {
  if (!container.value || !renderer || !camera || !composer) return
  const w = container.value.clientWidth
  const h = container.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
  composer.setSize(w, h)
}

onMounted(() => {
  init()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (animationId !== null) cancelAnimationFrame(animationId)
  if (renderer) {
    renderer.dispose()
  }
  if (composer) {
    composer.dispose()
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.geo-nexus-globe {
  width: 100%;
  height: 100%;
  position: relative;
  background-color: #020305;
}
</style>
