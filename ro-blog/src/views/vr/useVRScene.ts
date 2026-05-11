import { ref, onBeforeUnmount, type Ref } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js'
import { ShaderPass } from 'three/addons/postprocessing/ShaderPass.js'
import gsap from 'gsap'
import { PLANET_CONFIGS, type PlanetConfig, type CategoryStats } from './planetData'

/* ------------------------------------------------------------------ */
/*  Film Grain + Vignette Shader                                       */
/* ------------------------------------------------------------------ */
const FilmGrainShader = {
  uniforms: {
    tDiffuse: { value: null },
    time: { value: 0 },
    noiseIntensity: { value: 0.06 },
    vignetteIntensity: { value: 0.35 },
  },
  vertexShader: `
    varying vec2 vUv;
    void main() {
      vUv = uv;
      gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
  `,
  fragmentShader: `
    uniform sampler2D tDiffuse;
    uniform float time;
    uniform float noiseIntensity;
    uniform float vignetteIntensity;
    varying vec2 vUv;

    float rand(vec2 co) {
      return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
    }

    void main() {
      vec4 color = texture2D(tDiffuse, vUv);
      float noise = rand(vUv + time) * noiseIntensity;
      color.rgb += noise - noiseIntensity * 0.5;
      float dist = distance(vUv, vec2(0.5));
      float vignette = smoothstep(0.8, 0.4, dist);
      color.rgb *= mix(1.0 - vignetteIntensity, 1.0, vignette);
      gl_FragColor = color;
    }
  `,
}

/* ------------------------------------------------------------------ */
/*  Types                                                              */
/* ------------------------------------------------------------------ */
interface PlanetMesh extends THREE.Mesh {
  userData: {
    config: PlanetConfig
    articleCount: number
    lastUpdated: string
    baseY: number
    orbitAngle: number
    glowShell?: THREE.Mesh
    energyRing?: THREE.Mesh
  }
}

interface ShootingStar {
  mesh: THREE.Sprite
  velocity: THREE.Vector3
  life: number
  maxLife: number
  active: boolean
}

/* ------------------------------------------------------------------ */
/*  Composable                                                         */
/* ------------------------------------------------------------------ */
export function useVRScene() {
  /* --- Reactive state --- */
  const loading = ref(true)
  const currentTime = ref('')
  const stats = ref({ planets: 0, articles: 0 })
  const hoveredPlanet = ref<CategoryStats | null>(null)
  const selectedCategory = ref('')

  /* --- Three.js internals --- */
  let scene: THREE.Scene
  let camera: THREE.PerspectiveCamera
  let renderer: THREE.WebGLRenderer
  let controls: OrbitControls
  let composer: EffectComposer
  let filmGrainPass: ShaderPass
  let raycaster: THREE.Raycaster
  let mouse: THREE.Vector2
  let clock: THREE.Clock
  let animationId: number

  /* --- Scene groups --- */
  let bgGroup: THREE.Group
  let centerGroup: THREE.Group
  let planetsGroup: THREE.Group
  let orbitsGroup: THREE.Group
  let shootingGroup: THREE.Group

  /* --- Object refs --- */
  let starField: THREE.Points
  let nebulaField: THREE.Points
  let planetMeshes: PlanetMesh[] = []
  let shootingStars: ShootingStar[] = []
  let centerGlowMat: THREE.ShaderMaterial
  let labelSprites: Map<string, THREE.Sprite> = new Map()
  let articlesByCategory: Map<string, { id: number; title: string }[]> = new Map()

  /* --- Camera animation --- */
  let cameraAnimating = false
  let cameraTarget = new THREE.Vector3()
  let cameraLookTarget = new THREE.Vector3()

  /* ================================================================ */
  /*  1. SCENE INIT                                                    */
  /* ================================================================ */
  function initScene(container: HTMLDivElement) {
    /* Scene */
    scene = new THREE.Scene()
    scene.fog = new THREE.FogExp2(0x050816, 0.0012)

    /* Camera */
    camera = new THREE.PerspectiveCamera(55, window.innerWidth / window.innerHeight, 0.1, 3000)
    camera.position.set(0, 40, 130)

    /* Renderer */
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false, powerPreference: 'high-performance' })
    renderer.setSize(window.innerWidth, window.innerHeight)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.setClearColor(0x050816)
    renderer.toneMapping = THREE.ACESFilmicToneMapping
    renderer.toneMappingExposure = 0.9
    container.appendChild(renderer.domElement)

    /* Controls */
    controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.04
    controls.maxDistance = 350
    controls.minDistance = 40
    controls.autoRotate = true
    controls.autoRotateSpeed = 0.15
    controls.maxPolarAngle = Math.PI * 0.85
    controls.minPolarAngle = Math.PI * 0.15
    controls.target.set(0, 0, 0)

    /* Lights */
    const ambient = new THREE.AmbientLight(0x1a2a4e, 0.5)
    scene.add(ambient)

    const centerLight = new THREE.PointLight(0x3B82F6, 4, 300)
    centerLight.position.set(0, 0, 0)
    scene.add(centerLight)

    const rimLight1 = new THREE.PointLight(0x38BDF8, 1.5, 250)
    rimLight1.position.set(-100, 60, -80)
    scene.add(rimLight1)

    const rimLight2 = new THREE.PointLight(0x93C5FD, 1.2, 250)
    rimLight2.position.set(80, -40, 100)
    scene.add(rimLight2)

    /* Groups */
    bgGroup = new THREE.Group()
    centerGroup = new THREE.Group()
    planetsGroup = new THREE.Group()
    orbitsGroup = new THREE.Group()
    shootingGroup = new THREE.Group()
    scene.add(bgGroup, centerGroup, planetsGroup, orbitsGroup, shootingGroup)

    /* Raycaster */
    raycaster = new THREE.Raycaster()
    mouse = new THREE.Vector2(-999, -999)
    clock = new THREE.Clock()

    /* Post-processing */
    setupPostProcessing()

    /* Events */
    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('click', onClick)
    window.addEventListener('resize', onResize)
    window.addEventListener('touchstart', onTouchStart, { passive: true })
  }

  /* ================================================================ */
  /*  2. POST-PROCESSING                                               */
  /* ================================================================ */
  function setupPostProcessing() {
    composer = new EffectComposer(renderer)

    const renderPass = new RenderPass(scene, camera)
    composer.addPass(renderPass)

    const bloomPass = new UnrealBloomPass(
      new THREE.Vector2(window.innerWidth, window.innerHeight),
      0.7,   // strength
      0.4,   // radius
      0.65,  // threshold
    )
    composer.addPass(bloomPass)

    filmGrainPass = new ShaderPass(FilmGrainShader)
    composer.addPass(filmGrainPass)
  }

  /* ================================================================ */
  /*  3. STAR FIELD                                                    */
  /* ================================================================ */
  function createStarField() {
    /* --- Distant stars --- */
    const count = 5000
    const positions = new Float32Array(count * 3)
    const colors = new Float32Array(count * 3)
    const sizes = new Float32Array(count)

    for (let i = 0; i < count; i++) {
      const r = 250 + Math.random() * 1200
      const theta = Math.random() * Math.PI * 2
      const phi = Math.acos(2 * Math.random() - 1)
      positions[i * 3] = r * Math.sin(phi) * Math.cos(theta)
      positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta)
      positions[i * 3 + 2] = r * Math.cos(phi)

      const c = new THREE.Color()
      const hue = 0.55 + Math.random() * 0.18
      const sat = 0.2 + Math.random() * 0.5
      const lum = 0.5 + Math.random() * 0.5
      c.setHSL(hue, sat, lum)
      colors[i * 3] = c.r
      colors[i * 3 + 1] = c.g
      colors[i * 3 + 2] = c.b
      sizes[i] = 0.4 + Math.random() * 1.8
    }

    const geo = new THREE.BufferGeometry()
    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    geo.setAttribute('color', new THREE.BufferAttribute(colors, 3))

    const mat = new THREE.PointsMaterial({
      size: 1.2,
      vertexColors: true,
      transparent: true,
      opacity: 0.85,
      sizeAttenuation: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    })

    starField = new THREE.Points(geo, mat)
    bgGroup.add(starField)

    /* --- Nebula particles --- */
    const nebulaCount = 800
    const nPos = new Float32Array(nebulaCount * 3)
    const nCol = new Float32Array(nebulaCount * 3)
    const nebulaColors = [
      new THREE.Color('#3B82F6'),
      new THREE.Color('#2563EB'),
      new THREE.Color('#93C5FD'),
      new THREE.Color('#38BDF8'),
      new THREE.Color('#1D4ED8'),
    ]

    for (let i = 0; i < nebulaCount; i++) {
      const r = 150 + Math.random() * 600
      const theta = Math.random() * Math.PI * 2
      const phi = Math.acos(2 * Math.random() - 1)
      nPos[i * 3] = r * Math.sin(phi) * Math.cos(theta)
      nPos[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta)
      nPos[i * 3 + 2] = r * Math.cos(phi)

      const c = nebulaColors[Math.floor(Math.random() * nebulaColors.length)]
      nCol[i * 3] = c.r
      nCol[i * 3 + 1] = c.g
      nCol[i * 3 + 2] = c.b
    }

    const nGeo = new THREE.BufferGeometry()
    nGeo.setAttribute('position', new THREE.BufferAttribute(nPos, 3))
    nGeo.setAttribute('color', new THREE.BufferAttribute(nCol, 3))

    const nMat = new THREE.PointsMaterial({
      size: 4,
      vertexColors: true,
      transparent: true,
      opacity: 0.12,
      sizeAttenuation: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    })

    nebulaField = new THREE.Points(nGeo, nMat)
    bgGroup.add(nebulaField)
  }

  /* ================================================================ */
  /*  4. CENTRAL PLANET                                                */
  /* ================================================================ */
  function createCentralPlanet() {
    /* --- Core sphere with custom shader --- */
    const geo = new THREE.SphereGeometry(6, 64, 64)

    centerGlowMat = new THREE.ShaderMaterial({
      uniforms: {
        time: { value: 0 },
        color1: { value: new THREE.Color('#3B82F6') },
        color2: { value: new THREE.Color('#2563EB') },
        color3: { value: new THREE.Color('#93C5FD') },
      },
      vertexShader: `
        varying vec3 vNormal;
        varying vec3 vPosition;
        varying vec2 vUv;
        void main() {
          vNormal = normalize(normalMatrix * normal);
          vPosition = position;
          vUv = uv;
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        uniform float time;
        uniform vec3 color1;
        uniform vec3 color2;
        uniform vec3 color3;
        varying vec3 vNormal;
        varying vec3 vPosition;
        varying vec2 vUv;

        // Simplex noise helper
        vec3 mod289(vec3 x) { return x - floor(x / 289.0) * 289.0; }
        vec4 mod289(vec4 x) { return x - floor(x / 289.0) * 289.0; }
        vec4 permute(vec4 x) { return mod289((x * 34.0 + 1.0) * x); }
        vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }
        float snoise(vec3 v) {
          const vec2 C = vec2(1.0/6.0, 1.0/3.0);
          const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
          vec3 i  = floor(v + dot(v, C.yyy));
          vec3 x0 = v - i + dot(i, C.xxx);
          vec3 g = step(x0.yzx, x0.xyz);
          vec3 l = 1.0 - g;
          vec3 i1 = min(g.xyz, l.zxy);
          vec3 i2 = max(g.xyz, l.zxy);
          vec3 x1 = x0 - i1 + C.xxx;
          vec3 x2 = x0 - i2 + C.yyy;
          vec3 x3 = x0 - D.yyy;
          i = mod289(i);
          vec4 p = permute(permute(permute(
                    i.z + vec4(0.0, i1.z, i2.z, 1.0))
                  + i.y + vec4(0.0, i1.y, i2.y, 1.0))
                  + i.x + vec4(0.0, i1.x, i2.x, 1.0));
          float n_ = 0.142857142857;
          vec3 ns = n_ * D.wyz - D.xzx;
          vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
          vec4 x_ = floor(j * ns.z);
          vec4 y_ = floor(j - 7.0 * x_);
          vec4 x = x_ * ns.x + ns.yyyy;
          vec4 y = y_ * ns.x + ns.yyyy;
          vec4 h = 1.0 - abs(x) - abs(y);
          vec4 b0 = vec4(x.xy, y.xy);
          vec4 b1 = vec4(x.zw, y.zw);
          vec4 s0 = floor(b0)*2.0 + 1.0;
          vec4 s1 = floor(b1)*2.0 + 1.0;
          vec4 sh = -step(h, vec4(0.0));
          vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy;
          vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww;
          vec3 p0 = vec3(a0.xy, h.x);
          vec3 p1 = vec3(a0.zw, h.y);
          vec3 p2 = vec3(a1.xy, h.z);
          vec3 p3 = vec3(a1.zw, h.w);
          vec4 norm = taylorInvSqrt(vec4(dot(p0,p0),dot(p1,p1),dot(p2,p2),dot(p3,p3)));
          p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;
          vec4 m = max(0.6 - vec4(dot(x0,x0),dot(x1,x1),dot(x2,x2),dot(x3,x3)), 0.0);
          m = m * m;
          return 42.0 * dot(m*m, vec4(dot(p0,x0),dot(p1,x1),dot(p2,x2),dot(p3,x3)));
        }

        void main() {
          float n = snoise(vPosition * 0.8 + time * 0.15) * 0.5 + 0.5;
          float n2 = snoise(vPosition * 1.5 - time * 0.1) * 0.5 + 0.5;
          vec3 baseColor = mix(color1, color2, n);
          baseColor = mix(baseColor, color3, n2 * 0.4);
          // Fresnel rim
          float fresnel = pow(1.0 - abs(dot(vNormal, vec3(0.0, 0.0, 1.0))), 2.5);
          baseColor += vec3(0.3, 0.2, 0.5) * fresnel;
          gl_FragColor = vec4(baseColor, 1.0);
        }
      `,
    })

    const coreMesh = new THREE.Mesh(geo, centerGlowMat)
    centerGroup.add(coreMesh)

    /* --- Outer glow shell --- */
    const glowGeo = new THREE.SphereGeometry(7.5, 32, 32)
    const glowMat = new THREE.MeshBasicMaterial({
      color: 0x3B82F6,
      transparent: true,
      opacity: 0.06,
      side: THREE.BackSide,
    })
    const glowShell = new THREE.Mesh(glowGeo, glowMat)
    centerGroup.add(glowShell)

    /* --- Ring system --- */
    const ringGeo = new THREE.TorusGeometry(10, 0.15, 16, 128)
    const ringMat = new THREE.MeshBasicMaterial({
      color: 0x60A5FA,
      transparent: true,
      opacity: 0.4,
    })
    const ring1 = new THREE.Mesh(ringGeo, ringMat)
    ring1.rotation.x = Math.PI / 2
    centerGroup.add(ring1)

    const ring2Geo = new THREE.TorusGeometry(12, 0.08, 16, 128)
    const ring2Mat = new THREE.MeshBasicMaterial({
      color: 0x38BDF8,
      transparent: true,
      opacity: 0.2,
    })
    const ring2 = new THREE.Mesh(ring2Geo, ring2Mat)
    ring2.rotation.x = Math.PI / 2 + 0.15
    centerGroup.add(ring2)

    const ring3Geo = new THREE.TorusGeometry(14, 0.05, 16, 128)
    const ring3Mat = new THREE.MeshBasicMaterial({
      color: 0x93C5FD,
      transparent: true,
      opacity: 0.12,
    })
    const ring3 = new THREE.Mesh(ring3Geo, ring3Mat)
    ring3.rotation.x = Math.PI / 2 - 0.1
    centerGroup.add(ring3)
  }

  /* ================================================================ */
  /*  5. PROCEDURAL PLANET TEXTURE                                     */
  /* ================================================================ */
  function createPlanetTexture(color: THREE.Color, seed: number): THREE.CanvasTexture {
    const size = 512
    const canvas = document.createElement('canvas')
    canvas.width = size
    canvas.height = size
    const ctx = canvas.getContext('2d')!

    /* Base fill */
    ctx.fillStyle = `rgb(${Math.floor(color.r * 60)}, ${Math.floor(color.g * 60)}, ${Math.floor(color.b * 60)})`
    ctx.fillRect(0, 0, size, size)

    /* Procedural surface detail */
    const rng = (n: number) => {
      const x = Math.sin(seed * 9301 + n * 49297) * 49297
      return x - Math.floor(x)
    }

    for (let i = 0; i < 200; i++) {
      const x = rng(i * 3) * size
      const y = rng(i * 3 + 1) * size
      const r = 5 + rng(i * 3 + 2) * 40
      const alpha = 0.03 + rng(i * 7) * 0.08
      const bright = rng(i * 11) > 0.5
      ctx.beginPath()
      ctx.arc(x, y, r, 0, Math.PI * 2)
      ctx.fillStyle = bright
        ? `rgba(${Math.floor(color.r * 255)}, ${Math.floor(color.g * 255)}, ${Math.floor(color.b * 255)}, ${alpha})`
        : `rgba(0, 0, 0, ${alpha * 1.5})`
      ctx.fill()
    }

    /* Latitude bands */
    for (let i = 0; i < 8; i++) {
      const y = (i / 8) * size
      ctx.fillStyle = `rgba(0, 0, 0, ${0.02 + rng(i * 17) * 0.04})`
      ctx.fillRect(0, y, size, size / 16)
    }

    const texture = new THREE.CanvasTexture(canvas)
    texture.wrapS = THREE.RepeatWrapping
    texture.wrapT = THREE.RepeatWrapping
    return texture
  }

  /* ================================================================ */
  /*  6. CATEGORY PLANETS                                              */
  /* ================================================================ */
  function createCategoryPlanet(config: PlanetConfig, articleCount: number, lastUpdated: string): PlanetMesh {
    const color = new THREE.Color(config.color)
    const emissive = new THREE.Color(config.emissive)

    /* Main sphere */
    const geo = new THREE.SphereGeometry(config.size, 48, 48)
    const texture = createPlanetTexture(color, config.orbitRadius * 13.7)
    const mat = new THREE.MeshStandardMaterial({
      map: texture,
      color: color,
      emissive: emissive,
      emissiveIntensity: 0.25,
      metalness: config.metalness,
      roughness: config.roughness,
      transparent: true,
      opacity: 0.92,
    })

    const mesh = new THREE.Mesh(geo, mat) as PlanetMesh

    /* Initial orbital position */
    const startAngle = Math.random() * Math.PI * 2
    mesh.position.set(
      Math.cos(startAngle) * config.orbitRadius,
      (Math.random() - 0.5) * 8,
      Math.sin(startAngle) * config.orbitRadius,
    )

    mesh.userData = {
      config,
      articleCount,
      lastUpdated,
      baseY: mesh.position.y,
      orbitAngle: startAngle,
    }

    /* Glow shell */
    const glowGeo = new THREE.SphereGeometry(config.size * 1.35, 32, 32)
    const glowMat = new THREE.MeshBasicMaterial({
      color: emissive,
      transparent: true,
      opacity: 0.05,
      side: THREE.BackSide,
    })
    const glowShell = new THREE.Mesh(glowGeo, glowMat)
    mesh.add(glowShell)
    mesh.userData.glowShell = glowShell

    /* Energy ring */
    const ringGeo = new THREE.TorusGeometry(config.size * 1.6, 0.06, 8, 64)
    const ringMat = new THREE.MeshBasicMaterial({
      color: emissive,
      transparent: true,
      opacity: 0.2,
    })
    const energyRing = new THREE.Mesh(ringGeo, ringMat)
    energyRing.rotation.x = Math.PI / 2
    mesh.add(energyRing)
    mesh.userData.energyRing = energyRing

    /* Category label */
    const labelSprite = createLabelSprite(config.name, config.emissive, 38)
    labelSprite.position.y = config.size + 3
    labelSprite.scale.set(14, 3.5, 1)
    mesh.add(labelSprite)
    labelSprites.set(config.name, labelSprite)

    planetsGroup.add(mesh)
    planetMeshes.push(mesh)
    return mesh
  }

  /* ================================================================ */
  /*  7. ORBIT LINES                                                   */
  /* ================================================================ */
  function createOrbitLine(radius: number, color: string) {
    const segments = 128
    const positions = new Float32Array(segments * 3)
    for (let i = 0; i < segments; i++) {
      const angle = (i / segments) * Math.PI * 2
      positions[i * 3] = Math.cos(angle) * radius
      positions[i * 3 + 1] = 0
      positions[i * 3 + 2] = Math.sin(angle) * radius
    }
    const geo = new THREE.BufferGeometry()
    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    const mat = new THREE.LineBasicMaterial({
      color: new THREE.Color(color),
      transparent: true,
      opacity: 0.08,
    })
    const line = new THREE.Line(geo, mat)
    orbitsGroup.add(line)
  }

  /* ================================================================ */
  /*  8. SHOOTING STARS                                                */
  /* ================================================================ */
  function createShootingStars() {
    const starTexture = createShootingStarTexture()
    for (let i = 0; i < 8; i++) {
      const mat = new THREE.SpriteMaterial({
        map: starTexture,
        color: 0xffffff,
        transparent: true,
        opacity: 0,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      })
      const sprite = new THREE.Sprite(mat)
      sprite.scale.set(0, 0, 0)
      sprite.visible = false
      shootingGroup.add(sprite)
      shootingStars.push({
        mesh: sprite,
        velocity: new THREE.Vector3(),
        life: 0,
        maxLife: 0,
        active: false,
      })
    }
  }

  function createShootingStarTexture(): THREE.CanvasTexture {
    const canvas = document.createElement('canvas')
    canvas.width = 256
    canvas.height = 32
    const ctx = canvas.getContext('2d')!
    const gradient = ctx.createLinearGradient(0, 16, 256, 16)
    gradient.addColorStop(0, 'rgba(255,255,255,0)')
    gradient.addColorStop(0.3, 'rgba(96,165,250,0.6)')
    gradient.addColorStop(0.7, 'rgba(59,130,246,0.8)')
    gradient.addColorStop(1, 'rgba(255,255,255,1)')
    ctx.fillStyle = gradient
    ctx.fillRect(0, 8, 256, 16)
    return new THREE.CanvasTexture(canvas)
  }

  function triggerShootingStar() {
    const star = shootingStars.find(s => !s.active)
    if (!star) return

    const startR = 200 + Math.random() * 400
    const theta = Math.random() * Math.PI * 2
    const phi = Math.random() * Math.PI * 0.6 + Math.PI * 0.2
    star.mesh.position.set(
      startR * Math.sin(phi) * Math.cos(theta),
      startR * Math.cos(phi) * 0.5,
      startR * Math.sin(phi) * Math.sin(theta),
    )
    star.velocity.set(
      -star.mesh.position.x * 0.008 + (Math.random() - 0.5) * 2,
      -Math.random() * 1.5 - 0.5,
      -star.mesh.position.z * 0.008 + (Math.random() - 0.5) * 2,
    )
    star.life = 0
    star.maxLife = 1.5 + Math.random() * 1.5
    star.active = true
    star.mesh.visible = true
    star.mesh.material.opacity = 0
    star.mesh.scale.set(0, 0, 0)
  }

  /* ================================================================ */
  /*  9. CAMERA ANIMATION                                             */
  /* ================================================================ */
  function animateCameraTo(targetPos: THREE.Vector3, lookAt: THREE.Vector3, duration = 2) {
    if (cameraAnimating) return
    cameraAnimating = true
    controls.autoRotate = false
    cameraTarget.copy(targetPos)
    cameraLookTarget.copy(lookAt)

    gsap.to(camera.position, {
      x: targetPos.x,
      y: targetPos.y,
      z: targetPos.z,
      duration,
      ease: 'power3.inOut',
      onComplete: () => {
        cameraAnimating = false
      },
    })
    gsap.to(controls.target, {
      x: lookAt.x,
      y: lookAt.y,
      z: lookAt.z,
      duration,
      ease: 'power3.inOut',
    })
  }

  function resetCamera() {
    animateCameraTo(new THREE.Vector3(0, 40, 130), new THREE.Vector3(0, 0, 0), 2)
    setTimeout(() => {
      controls.autoRotate = true
    }, 2200)
  }

  /* ================================================================ */
  /*  10. EVENTS                                                       */
  /* ================================================================ */
  function onMouseMove(e: MouseEvent) {
    mouse.x = (e.clientX / window.innerWidth) * 2 - 1
    mouse.y = -(e.clientY / window.innerHeight) * 2 + 1
  }

  function onTouchStart(e: TouchEvent) {
    if (e.touches.length === 1) {
      const touch = e.touches[0]
      mouse.x = (touch.clientX / window.innerWidth) * 2 - 1
      mouse.y = -(touch.clientY / window.innerHeight) * 2 + 1
    }
  }

  function onClick(e: MouseEvent) {
    mouse.x = (e.clientX / window.innerWidth) * 2 - 1
    mouse.y = -(e.clientY / window.innerHeight) * 2 + 1

    raycaster.setFromCamera(mouse, camera)
    const intersects = raycaster.intersectObjects(planetMeshes, false)
    if (intersects.length > 0) {
      const hit = intersects[0].object as PlanetMesh
      const cfg = hit.userData.config
      selectedCategory.value = cfg.name
    }
  }

  function onResize() {
    camera.aspect = window.innerWidth / window.innerHeight
    camera.updateProjectionMatrix()
    renderer.setSize(window.innerWidth, window.innerHeight)
    composer.setSize(window.innerWidth, window.innerHeight)
  }

  /* ================================================================ */
  /*  11. ANIMATION LOOP                                               */
  /* ================================================================ */
  let shootingTimer = 0

  function animate() {
    animationId = requestAnimationFrame(animate)
    const t = clock.getElapsedTime()
    const dt = clock.getDelta()

    /* --- Update shader uniforms --- */
    if (centerGlowMat) centerGlowMat.uniforms.time.value = t
    if (filmGrainPass) filmGrainPass.uniforms.time.value = t

    /* --- Rotate central planet --- */
    centerGroup.rotation.y = t * 0.03
    centerGroup.rotation.x = Math.sin(t * 0.01) * 0.05

    /* --- Animate category planets --- */
    for (const mesh of planetMeshes) {
      const cfg = mesh.userData.config
      mesh.userData.orbitAngle += cfg.orbitSpeed * 0.01
      const angle = mesh.userData.orbitAngle

      mesh.position.x = Math.cos(angle) * cfg.orbitRadius
      mesh.position.z = Math.sin(angle) * cfg.orbitRadius
      mesh.position.y = mesh.userData.baseY + Math.sin(t * 0.3 + cfg.orbitRadius) * 0.8

      mesh.rotation.y += cfg.rotationSpeed
      mesh.rotation.x = cfg.tilt

      /* Energy ring pulse */
      const ring = mesh.userData.energyRing
      if (ring) {
        ring.rotation.z = t * 0.15
        const scale = 1 + Math.sin(t * 0.8 + cfg.orbitRadius) * 0.05
        ring.scale.set(scale, scale, scale)
      }
    }

    /* --- Star field rotation --- */
    starField.rotation.y = t * 0.005
    starField.rotation.x = Math.sin(t * 0.002) * 0.01
    nebulaField.rotation.y = -t * 0.003

    /* --- Shooting stars --- */
    shootingTimer += dt
    if (shootingTimer > 3 + Math.random() * 5) {
      triggerShootingStar()
      shootingTimer = 0
    }
    for (const star of shootingStars) {
      if (!star.active) continue
      star.life += dt
      const progress = star.life / star.maxLife
      if (progress >= 1) {
        star.active = false
        star.mesh.visible = false
        continue
      }
      star.mesh.position.add(star.velocity.clone().multiplyScalar(dt * 60))
      const fadeIn = Math.min(progress * 5, 1)
      const fadeOut = Math.max(1 - (progress - 0.6) / 0.4, 0)
      star.mesh.material.opacity = fadeIn * fadeOut * 0.9
      const s = 2 + progress * 8
      star.mesh.scale.set(s, s * 0.15, s)
    }

    /* --- Raycasting --- */
    raycaster.setFromCamera(mouse, camera)
    const intersects = raycaster.intersectObjects(planetMeshes, false)
    let foundHover = false
    for (const mesh of planetMeshes) {
      const mat = mesh.material as THREE.MeshStandardMaterial
      const shellMat = mesh.userData.glowShell?.material as THREE.MeshBasicMaterial
      if (intersects.length > 0 && intersects[0].object === mesh) {
        mat.emissiveIntensity = 0.6 + Math.sin(t * 2) * 0.1
        if (shellMat) shellMat.opacity = 0.12
        gsap.to(mesh.scale, { x: 1.2, y: 1.2, z: 1.2, duration: 0.3, ease: 'power2.out' })
        document.body.style.cursor = 'pointer'
        hoveredPlanet.value = {
          name: mesh.userData.config.name,
          count: mesh.userData.articleCount,
          lastUpdated: mesh.userData.lastUpdated,
          config: mesh.userData.config,
        }
        foundHover = true
      } else {
        mat.emissiveIntensity = 0.25
        if (shellMat) shellMat.opacity = 0.05
        gsap.to(mesh.scale, { x: 1, y: 1, z: 1, duration: 0.3, ease: 'power2.out' })
      }
    }
    if (!foundHover) {
      document.body.style.cursor = 'default'
      hoveredPlanet.value = null
    }

    /* --- Camera drift (subtle) --- */
    if (!cameraAnimating) {
      camera.position.y += Math.sin(t * 0.1) * 0.005
    }

    controls.update()
    composer.render()
  }

  /* ================================================================ */
  /*  12. LABEL MANAGEMENT                                             */
  /* ================================================================ */
  function createLabelSprite(text: string, color: string, fontSize = 38, maxWidth = 480): THREE.Sprite {
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

    /* Truncate if too long */
    let displayText = text
    const measured = ctx.measureText(text)
    if (measured.width > maxWidth) {
      while (ctx.measureText(displayText + '...').width > maxWidth && displayText.length > 0) {
        displayText = displayText.slice(0, -1)
      }
      displayText += '...'
    }
    ctx.fillText(displayText, 256, 64)

    const tex = new THREE.CanvasTexture(canvas)
    const sprite = new THREE.Sprite(
      new THREE.SpriteMaterial({ map: tex, transparent: true, opacity: 0.8, depthTest: false }),
    )
    return sprite
  }

  function setArticlesByCategory(articles: Map<string, { id: number; title: string }[]>) {
    articlesByCategory = articles
  }

  function updateLabels(categoryName: string) {
    if (!categoryName || categoryName === '全部') {
      /* Reset to category names */
      for (const mesh of planetMeshes) {
        const cfg = mesh.userData.config
        const sprite = labelSprites.get(cfg.name)
        if (sprite) {
          disposeSpriteTexture(sprite)
          const newSprite = createLabelSprite(cfg.name, cfg.emissive, 38)
          sprite.material = newSprite.material
          sprite.scale.set(14, 3.5, 1)
        }
        /* Show all planets */
        mesh.visible = true
        const orbitLine = orbitsGroup.children.find(c =>
          c instanceof THREE.Line && isOnOrbitRadius(c, cfg.orbitRadius),
        )
        if (orbitLine) orbitLine.visible = true
      }
    } else {
      /* Show articles for the selected category */
      const articles = articlesByCategory.get(categoryName) || []
      for (const mesh of planetMeshes) {
        const cfg = mesh.userData.config
        const sprite = labelSprites.get(cfg.name)
        if (!sprite) continue

        if (cfg.name === categoryName) {
          /* This is the selected category planet - show category name prominently */
          disposeSpriteTexture(sprite)
          const newSprite = createLabelSprite(cfg.name, cfg.emissive, 42)
          sprite.material = newSprite.material
          sprite.scale.set(16, 4, 1)
          mesh.visible = true
        } else {
          /* Other planets show article titles from selected category */
          const articleIdx = planetMeshes.filter(m => m.userData.config.name !== categoryName).indexOf(mesh)
          if (articleIdx >= 0 && articleIdx < articles.length) {
            disposeSpriteTexture(sprite)
            const article = articles[articleIdx]
            const newSprite = createLabelSprite(article.title, '#e2e8f0', 30)
            sprite.material = newSprite.material
            sprite.scale.set(18, 4.5, 1)
            mesh.visible = true
          } else {
            /* More planets than articles - hide extras */
            mesh.visible = articleIdx < articles.length
          }
        }

        /* Show/hide orbit lines */
        const orbitLine = orbitsGroup.children.find(c =>
          c instanceof THREE.Line && isOnOrbitRadius(c, cfg.orbitRadius),
        )
        if (orbitLine) orbitLine.visible = mesh.visible
      }
    }
  }

  function isOnOrbitRadius(obj: THREE.Object3D, radius: number): boolean {
    if (!(obj instanceof THREE.Line)) return false
    const pos = obj.geometry.getAttribute('position')
    if (!pos) return false
    const x = pos.getX(0)
    const z = pos.getZ(0)
    return Math.abs(Math.sqrt(x * x + z * z) - radius) < 1
  }

  function disposeSpriteTexture(sprite: THREE.Sprite) {
    const mat = sprite.material as THREE.SpriteMaterial
    if (mat.map) {
      mat.map.dispose()
    }
  }

  /* ================================================================ */
  /*  13. BUILD SCENE                                                  */
  /* ================================================================ */
  async function buildScene(
    container: HTMLDivElement,
    categories: { name: string; count: number; lastUpdated: string }[],
  ) {
    initScene(container)
    createStarField()
    createCentralPlanet()
    createShootingStars()

    /* Create category planets */
    for (const cat of categories) {
      const config = getPlanetConfig(cat.name)
      createCategoryPlanet(config, cat.count, cat.lastUpdated)
      createOrbitLine(config.orbitRadius, config.emissive)
    }

    stats.value = {
      planets: categories.length,
      articles: categories.reduce((s, c) => s + c.count, 0),
    }

    loading.value = false
    animate()
  }

  /* ================================================================ */
  /*  13. PLANET CONFIG LOOKUP                                         */
  /* ================================================================ */
  function getPlanetConfig(name: string): PlanetConfig {
    return PLANET_CONFIGS.find(p => p.name === name) || {
      name,
      color: '#3B82F6',
      emissive: '#60A5FA',
      size: 3,
      orbitRadius: 50 + Math.random() * 30,
      orbitSpeed: 0.05,
      rotationSpeed: 0.007,
      tilt: (Math.random() - 0.5) * 0.3,
      description: '',
      metalness: 0.6,
      roughness: 0.3,
    }
  }

  /* ================================================================ */
  /*  14. TIME UPDATER                                                 */
  /* ================================================================ */
  let timeInterval: number
  function startTimeUpdater() {
    const update = () => {
      const now = new Date()
      currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    }
    update()
    timeInterval = window.setInterval(update, 1000)
  }

  /* ================================================================ */
  /*  15. CLEANUP                                                      */
  /* ================================================================ */
  function cleanup() {
    cancelAnimationFrame(animationId)
    clearInterval(timeInterval)
    window.removeEventListener('mousemove', onMouseMove)
    window.removeEventListener('click', onClick)
    window.removeEventListener('resize', onResize)
    window.removeEventListener('touchstart', onTouchStart)

    /* Dispose scene */
    scene.traverse(obj => {
      if (obj instanceof THREE.Mesh || obj instanceof THREE.Points) {
        obj.geometry?.dispose()
        if (Array.isArray(obj.material)) {
          obj.material.forEach(m => {
            m.map?.dispose()
            m.dispose()
          })
        } else if (obj.material) {
          (obj.material as THREE.Material).map?.dispose()
          ;(obj.material as THREE.Material).dispose()
        }
      }
    })
    renderer.dispose()
    controls.dispose()
    composer.dispose()
  }

  onBeforeUnmount(cleanup)

  return {
    loading,
    currentTime,
    stats,
    hoveredPlanet,
    selectedCategory,
    buildScene,
    startTimeUpdater,
    animateCameraTo,
    resetCamera,
    updateLabels,
    setArticlesByCategory,
  }
}
