import { ref, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import gsap from 'gsap'
import { PLANET_CONFIGS, type PlanetConfig, type CategoryStats } from './planetData'
import {
  type PlanetItem,
  type PlanetGroup,
  createGlowTexture,
  createLabelSprite,
  getPlanetConfig
} from './sceneHelpers'

/* ------------------------------------------------------------------ */
/*  Composable                                                         */
/* ------------------------------------------------------------------ */
export function useVRScene() {
  const loading = ref(true)
  const currentTime = ref('')
  const stats = ref({ planets: 0, articles: 0 })
  const hoveredPlanet = ref<CategoryStats | null>(null)
  const selectedCategory = ref('')
  const currentViewMode = ref<'all' | string>('all')
  const moreClicked = ref(false)
  const coreDblClicked = ref(false)

  let scene: THREE.Scene
  let camera: THREE.PerspectiveCamera
  let renderer: THREE.WebGLRenderer
  let controls: OrbitControls
  let raycaster: THREE.Raycaster
  let mouse: THREE.Vector2
  let clock: THREE.Clock
  let animationId: number

  let coreGroup: THREE.Group
  let planetMeshes: PlanetGroup[] = []
  let orbitGroups: THREE.Group[] = []
  let labelSprites: Map<string, THREE.Sprite> = new Map()
  let articlesByCategory: Map<string, { id: number; title: string }[]> = new Map()

  /* Data stored for scene rebuild */
  let storedCategories: { name: string; count: number; lastUpdated: string }[] = []


  /* ================================================================ */
  /*  1. SCENE INIT                                                    */
  /* ================================================================ */
  function initScene(container: HTMLDivElement) {
    scene = new THREE.Scene()
    scene.background = new THREE.Color('#0a0812')
    scene.fog = new THREE.Fog('#0a0812', 8, 28)

    camera = new THREE.PerspectiveCamera(48, window.innerWidth / window.innerHeight, 0.5, 55)
    camera.position.set(4.2, 5.8, 9.5)
    camera.lookAt(0, -0.2, 0)

    const isMobile = window.innerWidth < 768
    renderer = new THREE.WebGLRenderer({ antialias: false, alpha: false, powerPreference: 'high-performance' })
    renderer.setSize(window.innerWidth, window.innerHeight)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, isMobile ? 1.2 : 1.5))
    renderer.shadowMap.enabled = false
    container.appendChild(renderer.domElement)

    controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.08
    controls.autoRotate = true
    controls.autoRotateSpeed = isMobile ? 0.06 : 0.1
    controls.minDistance = 4.2
    controls.maxDistance = 16
    controls.minPolarAngle = 0
    controls.maxPolarAngle = Math.PI
    controls.target.set(0, -0.1, 0)
    controls.update()

    scene.add(new THREE.AmbientLight('#8B7CAA', 1.0))
    const pointLight = new THREE.PointLight('#B7B2FF', 1.2, 20)
    pointLight.position.set(5, 8, 6)
    scene.add(pointLight)

    raycaster = new THREE.Raycaster()
    mouse = new THREE.Vector2(-999, -999)
    clock = new THREE.Clock()

    window.addEventListener('mousemove', onMouseMove, { passive: true })
    window.addEventListener('dblclick', onDblClick)
    window.addEventListener('resize', onResize)
    window.addEventListener('touchstart', onTouchStart, { passive: true })
  }

  /* ================================================================ */
  /*  2. CORE (Wireframe Octahedron + Glow)                            */
  /* ================================================================ */
  function createCore() {
    coreGroup = new THREE.Group()

    const octaGeom = new THREE.OctahedronGeometry(1.3, 1)
    const coreWire = new THREE.LineSegments(
      new THREE.WireframeGeometry(octaGeom),
      new THREE.LineBasicMaterial({ color: '#d4b87a', transparent: true, opacity: 0.85 }),
    )
    coreGroup.add(coreWire)

    const glowSprite = new THREE.Sprite(
      new THREE.SpriteMaterial({
        map: createGlowTexture('#f0d080'),
        blending: THREE.AdditiveBlending,
        depthWrite: false,
        transparent: true,
        opacity: 0.35,
      }),
    )
    glowSprite.scale.set(3.8, 3.8, 1)
    coreGroup.add(glowSprite)

    const outerGlow = new THREE.Sprite(
      new THREE.SpriteMaterial({
        map: createGlowTexture('#c8a060'),
        blending: THREE.AdditiveBlending,
        depthWrite: false,
        transparent: true,
        opacity: 0.15,
      }),
    )
    outerGlow.scale.set(5.5, 5.5, 1)
    coreGroup.add(outerGlow)

    scene.add(coreGroup)
  }

  /* ================================================================ */
  /*  4. CLEAR PLANETS (for rebuild)                                   */
  /* ================================================================ */
  function clearPlanets() {
    for (const mesh of planetMeshes) {
      mesh.parent?.remove(mesh)
      mesh.traverse(child => {
        if (child instanceof THREE.Mesh || child instanceof THREE.Points) {
          child.geometry?.dispose()
          if (Array.isArray(child.material)) {
            child.material.forEach(m => { (m as any).map?.dispose(); m.dispose() })
          } else if (child.material) {
            (child.material as any).map?.dispose()
            ;(child.material as THREE.Material).dispose()
          }
        }
        if (child instanceof THREE.Sprite) {
          const mat = child.material as THREE.SpriteMaterial
          mat.map?.dispose()
          mat.dispose()
        }
      })
    }
    for (const orbit of orbitGroups) {
      orbit.traverse(child => {
        if (child instanceof THREE.Line) {
          child.geometry?.dispose()
          child.material?.dispose()
        }
      })
      scene.remove(orbit)
    }
    for (const [, sprite] of labelSprites) {
      const mat = sprite.material as THREE.SpriteMaterial
      mat.map?.dispose()
      mat.dispose()
    }
    planetMeshes = []
    orbitGroups = []
    labelSprites.clear()
  }

  /* ================================================================ */
  /*  5. STARS                                                         */
  /* ================================================================ */
  let stars: THREE.Points
  function createStars() {
    const isMobile = window.innerWidth < 768
    const count = isMobile ? 100 : 250
    const positions = new Float32Array(count * 3)
    for (let i = 0; i < count * 3; i += 3) {
      const r = 13 + Math.random() * 20
      const th = Math.random() * Math.PI * 2
      const ph = Math.acos(2 * Math.random() - 1)
      positions[i] = Math.cos(th) * Math.sin(ph) * r
      positions[i + 1] = Math.sin(ph) * r * 0.6
      positions[i + 2] = Math.sin(th) * Math.sin(ph) * r
    }
    const geo = new THREE.BufferGeometry()
    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    stars = new THREE.Points(
      geo,
      new THREE.PointsMaterial({ color: '#B7B2FF', size: 0.08, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true, opacity: 0.5 }),
    )
    scene.add(stars)
  }

  /* ================================================================ */
  /*  6. "MORE" PLANET                                                 */
  /* ================================================================ */
  function createMorePlanet(label: string, remaining: number) {
    const idx = planetMeshes.length
    const orbitRadius = 2.0 + idx * 1.0
    const orbitGroup = new THREE.Group()
    scene.add(orbitGroup)
    orbitGroups.push(orbitGroup)

    /* Orbit ring */
    const ringPoints = Array.from({ length: 65 }, (_, i) => {
      const a = (i / 64) * Math.PI * 2
      return new THREE.Vector3(Math.cos(a) * orbitRadius, 0, Math.sin(a) * orbitRadius)
    })
    const ringGeo = new THREE.BufferGeometry().setFromPoints(ringPoints)
    const ringLine = new THREE.Line(
      ringGeo,
      new THREE.LineBasicMaterial({ color: '#FFD700', transparent: true, opacity: 0.3 }),
    )
    orbitGroup.add(ringLine)

    /* Planet group */
    const planetGroup = new THREE.Group() as unknown as PlanetGroup

    /* Icosahedron for "more" — different shape */
    const geom = new THREE.IcosahedronGeometry(0.46, 1)
    const wire = new THREE.Mesh(
      geom,
      new THREE.MeshBasicMaterial({ color: '#FFD700', wireframe: true, transparent: true, opacity: 1.0 }),
    )
    planetGroup.add(wire)

    /* Glow */
    const glow = new THREE.Sprite(
      new THREE.SpriteMaterial({
        map: createGlowTexture('#FFD700'),
        blending: THREE.AdditiveBlending,
        depthWrite: false,
        transparent: true,
        opacity: 0.35,
      }),
    )
    glow.scale.set(1.8, 1.8, 1)
    planetGroup.add(glow)

    const startAngle = Math.random() * Math.PI * 2
    planetGroup.position.x = Math.cos(startAngle) * orbitRadius
    planetGroup.position.z = Math.sin(startAngle) * orbitRadius

    const config: PlanetConfig = {
      name: label,
      color: '#FFD700',
      emissive: '#FFD700',
      size: 3,
      orbitRadius,
      orbitSpeed: 0.12 + Math.random() * 0.08,
      rotationSpeed: 0.006,
      tilt: 0,
      description: `还有 ${remaining} 个`,
      metalness: 0.3,
      roughness: 0.6,
    }

    planetGroup.userData = {
      config,
      articleCount: remaining,
      lastUpdated: '-',
      orbitAngle: startAngle,
      orbitRadius,
      speed: config.orbitSpeed,
      planetItem: { id: 'more', name: label, type: 'more' as const },
    }

    orbitGroup.add(planetGroup)
    planetMeshes.push(planetGroup)
  }

  /* ================================================================ */
  /*  7. LABEL SPRITES + SWITCH VIEW                                   */
  /* ================================================================ */


  function setArticlesByCategory(articles: Map<string, { id: number; title: string }[]>) {
    articlesByCategory = articles
  }

  /**
   * Switch the scene to show either all categories or articles in a category.
   * - '全部' | '': show up to 5 category planets + "more" if >5
   * - categoryName: show up to 5 article planets + "more" if >5
   */
  function switchView(categoryName: string) {
    clearPlanets()
    currentViewMode.value = categoryName

    if (!categoryName || categoryName === '全部') {
      /* --- Show categories (max 5 + more) --- */
      const cats = storedCategories.slice(0, 5)
      cats.forEach((cat, i) => {
        const config = getPlanetConfig(cat.name)
        createCompactPlanet(config, cat.count, cat.lastUpdated, i)
        if (planetMeshes.length > 0) {
          const last = planetMeshes[planetMeshes.length - 1]
          last.userData.planetItem = { id: cat.name, name: cat.name, type: 'category', count: cat.count, lastUpdated: cat.lastUpdated }
        }
      })
      const remaining = storedCategories.length - 5
      if (remaining > 0) {
        createMorePlanet(`更多...`, remaining)
      }
    } else {
      /* --- Show articles in this category (max 5 + more) --- */
      const articles = articlesByCategory.get(categoryName) || []

      /* Show up to 5 article planets (no separate category sphere) */
      const shownArticles = articles.slice(0, 5)
      shownArticles.forEach((article, i) => {
        const config = getPlanetConfig(article.title)
        createCompactPlanet(config, 0, '', i)
        if (planetMeshes.length > 0) {
          const last = planetMeshes[planetMeshes.length - 1]
          last.userData.planetItem = { id: article.id, name: article.title, type: 'article', articleId: article.id }
        }
      })

      const remaining = articles.length - 5
      if (remaining > 0) {
        createMorePlanet(`更多文章...`, remaining)
      }
    }

    /* Recreate label sprites for all planets */
    for (const mesh of planetMeshes) {
      const cfg = mesh.userData.config
      const pi = mesh.userData.planetItem
      let labelText = cfg.name
      let labelColor = cfg.emissive
      let labelSize = 18

      if (pi?.type === 'more') {
        labelText = pi.name
        labelColor = '#FFD700'
        labelSize = 16
      } else if (pi?.type === 'article') {
        labelText = pi.name
        labelColor = '#e2e8f0'
        labelSize = 15
      }

      /* Truncate long labels — max 4 Chinese characters */
      if (labelText.length > 5) {
        labelText = labelText.slice(0, 5) + '...'
      }

      const sprite = createLabelSprite(labelText, labelColor, labelSize)
      sprite.position.y = 1.2
      sprite.scale.set(pi?.type === 'article' ? 11 : pi?.type === 'more' ? 8 : 10, pi?.type === 'article' ? 3 : 2.5, 1)
      mesh.add(sprite)
      labelSprites.set(cfg.name, sprite)
    }
  }

  /**
   * Create a planet at a compact orbit radius based on index,
   * ignoring the config's (potentially huge) orbitRadius.
   */
  function createCompactPlanet(config: PlanetConfig, articleCount: number, lastUpdated: string, index: number) {
    const orbitRadius = 2.0 + index * 1.0
    const speed = 0.2 + Math.random() * 0.1

    const orbitGroup = new THREE.Group()
    scene.add(orbitGroup)
    orbitGroups.push(orbitGroup)

    /* Orbit ring */
    const ringPoints = Array.from({ length: 65 }, (_, i) => {
      const a = (i / 64) * Math.PI * 2
      return new THREE.Vector3(Math.cos(a) * orbitRadius, 0, Math.sin(a) * orbitRadius)
    })
    const ringGeo = new THREE.BufferGeometry().setFromPoints(ringPoints)
    const ringLine = new THREE.Line(
      ringGeo,
      new THREE.LineBasicMaterial({ color: '#3a2e55', transparent: true, opacity: 0.45 }),
    )
    orbitGroup.add(ringLine)

    /* Planet group */
    const planetGroup = new THREE.Group() as PlanetGroup

    /* Wireframe octahedron */
    const octaGeom = new THREE.OctahedronGeometry(0.5, 1)
    const wireframeMesh = new THREE.Mesh(
      octaGeom,
      new THREE.MeshBasicMaterial({ color: config.emissive, wireframe: true, transparent: true, opacity: 1.0 }),
    )
    planetGroup.add(wireframeMesh)

    /* Glow sprite */
    const glow = new THREE.Sprite(
      new THREE.SpriteMaterial({
        map: createGlowTexture(config.emissive),
        blending: THREE.AdditiveBlending,
        depthWrite: false,
        transparent: true,
        opacity: 0.4,
      }),
    )
    glow.scale.set(2.0, 2.0, 1)
    planetGroup.add(glow)

    const startAngle = Math.random() * Math.PI * 2
    planetGroup.position.x = Math.cos(startAngle) * orbitRadius
    planetGroup.position.z = Math.sin(startAngle) * orbitRadius

    planetGroup.userData = {
      config,
      articleCount,
      lastUpdated,
      orbitAngle: startAngle,
      orbitRadius,
      speed,
      planetItem: { id: config.name, name: config.name, type: 'category' as const },
    }

    orbitGroup.add(planetGroup)
    planetMeshes.push(planetGroup)
  }

  /** @deprecated Use switchView instead */
  function updateLabels(_categoryName: string) {
    switchView(_categoryName)
  }

  /* ================================================================ */
  /*  8. CAMERA ANIMATION                                             */
  /* ================================================================ */
  let cameraAnimating = false

  function animateCameraTo(targetPos: THREE.Vector3, lookAt: THREE.Vector3, duration = 2) {
    if (cameraAnimating) return
    cameraAnimating = true
    controls.autoRotate = false
    gsap.to(camera.position, { x: targetPos.x, y: targetPos.y, z: targetPos.z, duration, ease: 'power3.inOut', onComplete: () => { cameraAnimating = false } })
    gsap.to(controls.target, { x: lookAt.x, y: lookAt.y, z: lookAt.z, duration, ease: 'power3.inOut' })
  }

  function resetCamera() {
    animateCameraTo(new THREE.Vector3(4.2, 5.8, 9.5), new THREE.Vector3(0, -0.1, 0), 2)
    setTimeout(() => { controls.autoRotate = true }, 2200)
  }

  /* ================================================================ */
  /*  9. EVENTS                                                        */
  /* ================================================================ */
  function onMouseMove(e: MouseEvent) {
    mouse.x = (e.clientX / window.innerWidth) * 2 - 1
    mouse.y = -(e.clientY / window.innerHeight) * 2 + 1
  }

  let lastTouchTime = 0
  function onTouchStart(e: TouchEvent) {
    if (e.touches.length === 1) {
      mouse.x = (e.touches[0].clientX / window.innerWidth) * 2 - 1
      mouse.y = -(e.touches[0].clientY / window.innerHeight) * 2 + 1

      const now = Date.now()
      if (now - lastTouchTime < 350) {
        onDblClick({ clientX: e.touches[0].clientX, clientY: e.touches[0].clientY } as MouseEvent)
      }
      lastTouchTime = now
    }
  }

  function onDblClick(e: MouseEvent) {
    mouse.x = (e.clientX / window.innerWidth) * 2 - 1
    mouse.y = -(e.clientY / window.innerHeight) * 2 + 1
    raycaster.setFromCamera(mouse, camera)

    /* Check core planet first */
    const coreIntersects = raycaster.intersectObject(coreGroup, true)
    if (coreIntersects.length > 0) {
      coreDblClicked.value = true
      return
    }

    const intersects = raycaster.intersectObjects(planetMeshes, true)
    if (intersects.length > 0) {
      let obj: THREE.Object3D | null = intersects[0].object
      while (obj) {
        if (planetMeshes.includes(obj as PlanetGroup)) {
          const planetItem = (obj as PlanetGroup).userData.planetItem
          if (planetItem?.type === 'more') {
            moreClicked.value = true
          } else {
            moreClicked.value = false
            selectedCategory.value = planetItem?.name || (obj as PlanetGroup).userData.config.name
          }
          break
        }
        obj = obj.parent
      }
    }
  }

  function onResize() {
    camera.aspect = window.innerWidth / window.innerHeight
    camera.updateProjectionMatrix()
    renderer.setSize(window.innerWidth, window.innerHeight)
  }

  /* ================================================================ */
  /*  10. ANIMATION LOOP                                               */
  /* ================================================================ */
  let frameCount = 0

  function animate() {
    animationId = requestAnimationFrame(animate)
    const dt = Math.min(clock.getDelta(), 0.12)

    controls.update()

    /* Core rotation */
    coreGroup.rotation.y += dt * 0.08
    coreGroup.rotation.x += dt * 0.02

    /* Planet orbit + rotation */
    for (const p of planetMeshes) {
      p.userData.orbitAngle += p.userData.speed * dt
      const a = p.userData.orbitAngle
      const r = p.userData.orbitRadius
      p.position.x = Math.cos(a) * r
      p.position.z = Math.sin(a) * r
      p.rotation.y += dt * 0.15
      p.rotation.x += dt * 0.06
    }

    /* Stars */
    stars.rotation.y += dt * 0.01

    /* Raycasting (every 5 frames) */
    frameCount++
    if (frameCount % 5 === 0) {
      raycaster.setFromCamera(mouse, camera)
      const intersects = raycaster.intersectObjects(planetMeshes, true)
      let found = false
      for (const mesh of planetMeshes) {
        let hit = false
        for (const inter of intersects) {
          let obj: THREE.Object3D | null = inter.object
          while (obj) {
            if (obj === mesh) { hit = true; break }
            obj = obj.parent
          }
          if (hit) break
        }
        if (hit) {
          gsap.to(mesh.scale, { x: 1.12, y: 1.12, z: 1.12, duration: 0.3, ease: 'power2.out' })
          document.body.style.cursor = 'pointer'
          hoveredPlanet.value = {
            name: mesh.userData.config.name,
            count: mesh.userData.articleCount,
            lastUpdated: mesh.userData.lastUpdated,
            config: mesh.userData.config,
          }
          found = true
        } else {
          gsap.to(mesh.scale, { x: 1, y: 1, z: 1, duration: 0.3, ease: 'power2.out' })
        }
      }
      if (!found) {
        document.body.style.cursor = 'default'
        hoveredPlanet.value = null
      }
    }

    renderer.render(scene, camera)
  }

  /* ================================================================ */
  /*  10. BUILD SCENE                                                  */
  /* ================================================================ */
  async function buildScene(
    container: HTMLDivElement,
    categories: { name: string; count: number; lastUpdated: string }[],
  ) {
    storedCategories = categories
    initScene(container)
    createStars()
    createCore()

    /* Use switchView for initial render (全部 mode) */
    switchView('全部')

    stats.value = {
      planets: categories.length,
      articles: categories.reduce((s, c) => s + c.count, 0),
    }

    loading.value = false
    animate()
  }



  /* ================================================================ */
  /*  11. TIME UPDATER                                                 */
  /* ================================================================ */
  let timeInterval: number
  function startTimeUpdater() {
    const update = () => {
      currentTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    }
    update()
    timeInterval = window.setInterval(update, 1000)
  }

  /* ================================================================ */
  /*  12. CLEANUP                                                      */
  /* ================================================================ */
  function cleanup() {
    cancelAnimationFrame(animationId)
    clearInterval(timeInterval)
    window.removeEventListener('mousemove', onMouseMove)
    window.removeEventListener('dblclick', onDblClick)
    window.removeEventListener('resize', onResize)
    window.removeEventListener('touchstart', onTouchStart)
    scene.traverse(obj => {
      if (obj instanceof THREE.Mesh || obj instanceof THREE.Points) {
        obj.geometry?.dispose()
        if (Array.isArray(obj.material)) {
          obj.material.forEach(m => { (m as any).map?.dispose(); m.dispose() })
        } else if (obj.material) {
          (obj.material as any).map?.dispose(); (obj.material as THREE.Material).dispose()
        }
      }
    })
    renderer.dispose()
    controls.dispose()
  }

  onBeforeUnmount(cleanup)

  return {
    loading,
    currentTime,
    stats,
    hoveredPlanet,
    selectedCategory,
    currentViewMode,
    moreClicked,
    coreDblClicked,
    buildScene,
    startTimeUpdater,
    animateCameraTo,
    resetCamera,
    updateLabels,
    switchView,
    setArticlesByCategory,
  }
}
