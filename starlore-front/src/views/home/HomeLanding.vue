<script lang="ts" setup>
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { gsap } from 'gsap'
import StellarDotsBand from '@/components/StellarDotsBand.vue'
import EmotionBall from '@/components/EmotionBall.vue'
import {
  Zap,
  BrainCircuit,
  Eye,
  Workflow,
  GitBranch,
  Cpu,
  Layers,
  CheckCircle2,
  Presentation,
  ArrowRight,
  Sparkles,
  Mail,
  Database,
  Activity,
  ShieldCheck,
  FileSpreadsheet,
  FileText,
  Clock,
} from '@lucide/vue'

const guestVisual = ref<HTMLElement | null>(null)
let guestVisualContext: gsap.Context | null = null

const stats = [
  { value: '3-Agent', label: '角色协同状态机' },
  { value: '100%', label: '真实排版可交付' },
  { value: '3D WebGL', label: '空间引力知识网络' },
  { value: 'ms 级', label: '全链路可观测性' },
]

// ── 5 大核心能力卡片轮播 (同一样式、修长优雅、平滑滑动) ──
const activeCardIndex = ref(0)
let carouselTimer: number | null = null
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)

const capabilityCards = [
  {
    id: '01',
    badge: '01 · MULTI-AGENT HARNESS',
    title: 'Multi-Agent 协作调度',
    desc: '原生基于 LangGraph 状态机打造。集成 Planner 意图规划、Executor 并行工具调用与 Reviewer 事实审计，任务执行中自主反思自纠偏，突破单模型幻觉瓶颈。',
    chips: ['三角色闭环', 'LangGraph 状态机', 'Auto-Retry 自纠偏'],
    icon: GitBranch,
    themeClass: 'theme-blue',
  },
  {
    id: '02',
    badge: '02 · 3D VR GALAXY',
    title: '3D VR 知识星图',
    desc: 'Three.js 与 WebGL 驱动的全息可视化星系。将笔记根据语义相似度映射为具有引力牵引的星辰，在沉浸式星空中漫游与发散灵感。',
    chips: ['Three.js 引擎', '空间引力星网', '粒子拓扑聚类'],
    icon: Eye,
    themeClass: 'theme-cyan',
  },
  {
    id: '03',
    badge: '03 · ARTIFACT DELIVERY',
    title: '真实交付物 (Harness)',
    desc: '坚决拒绝概念玩具。智能体提取长程思考切片，一键端到端编译为商业级演示文稿（PPTX）、Word 调研报告与高保真 PDF 简历。',
    chips: ['PPTX 编译引擎', 'Word Docx 报告', '高保真矢量渲染'],
    icon: Presentation,
    themeClass: 'theme-purple',
  },
  {
    id: '04',
    badge: '04 · VECTOR RAG',
    title: '精准向量 RAG 检索',
    desc: '个人笔记高维向量索引与混合相似度检索。语义穿透全量知识切片，AI 基于你的个人第一手资料权威应答，告别胡编乱造。',
    chips: ['高维 Embedding', '混合相似度检索', '权威溯源防幻觉'],
    icon: Database,
    themeClass: 'theme-amber',
  },
  {
    id: '05',
    badge: '05 · OBSERVABILITY',
    title: '全链路可观测性',
    desc: '毫秒级链路追踪（OpenTelemetry）。精准记录每次 LLM 调用的 Token 消耗、工具路由耗时与 Prompt 演进，Bad Case 自动沉淀反哺。',
    chips: ['OpenTelemetry', 'Token 耗时遥测', 'Few-Shot 反哺'],
    icon: Activity,
    themeClass: 'theme-rose',
  },
]

const getCardOffset = (idx: number) => {
  const total = capabilityCards.length
  let offset = ((idx - activeCardIndex.value) % total + total) % total
  if (offset > total / 2) offset -= total
  return offset
}

// ── 鼠标拖拽、滚轮与手势转动手势 ──
const isDragging = ref(false)
const dragStartX = ref(0)
const dragCurrentOffset = ref(0)
let hasMovedSignificantly = false

const getCardStyle = (idx: number) => {
  const offset = getCardOffset(idx)
  const isCenter = offset === 0
  const isNeighbor = Math.abs(offset) === 1
  const isHidden = Math.abs(offset) > 1

  const step = windowWidth.value < 640 ? 270 : windowWidth.value < 1024 ? 320 : 360

  const liveDrag = isDragging.value ? dragCurrentOffset.value : 0
  const translateX = Math.round(offset * step + liveDrag)
  const scale = isCenter ? 1 : isNeighbor ? 0.9 : 0.8
  const opacity = isCenter ? 1 : isNeighbor ? 0.65 : 0
  const zIndex = isCenter ? 10 : isNeighbor ? 5 : 1
  const pointerEvents: 'none' | 'auto' = isHidden ? 'none' : 'auto'

  return {
    transform: `translateX(${translateX}px) scale(${scale})`,
    opacity: String(opacity),
    zIndex: String(zIndex),
    pointerEvents,
    transition: isDragging.value
      ? 'none'
      : 'transform 0.85s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.85s ease, box-shadow 0.85s ease, border-color 0.85s ease',
  }
}

const nextCard = () => {
  activeCardIndex.value = (activeCardIndex.value + 1) % capabilityCards.length
}

const prevCard = () => {
  activeCardIndex.value =
    (activeCardIndex.value - 1 + capabilityCards.length) % capabilityCards.length
}

const setCard = (index: number) => {
  activeCardIndex.value = index
}

// ── 自动平滑转动 (Auto-Play) ──
const startCarouselAutoPlay = () => {
  stopCarouselAutoPlay()
  carouselTimer = window.setInterval(() => {
    nextCard()
  }, 5000)
}

const stopCarouselAutoPlay = () => {
  if (carouselTimer !== null) {
    clearInterval(carouselTimer)
    carouselTimer = null
  }
}

// ── 鼠标拖拽转动 (Mouse Drag) ──
const onMouseDown = (e: MouseEvent) => {
  if (e.button !== 0) return
  isDragging.value = true
  dragStartX.value = e.clientX
  dragCurrentOffset.value = 0
  hasMovedSignificantly = false
  stopCarouselAutoPlay()

  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
}

const onMouseMove = (e: MouseEvent) => {
  if (!isDragging.value) return
  const diff = e.clientX - dragStartX.value
  if (Math.abs(diff) > 5) {
    hasMovedSignificantly = true
  }
  dragCurrentOffset.value = diff * 0.8
}

const onMouseUp = () => {
  if (!isDragging.value) return
  isDragging.value = false
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)

  const diff = dragCurrentOffset.value
  dragCurrentOffset.value = 0

  if (diff < -35) {
    nextCard()
  } else if (diff > 35) {
    prevCard()
  }
  startCarouselAutoPlay()
}

// ── 鼠标滚轮转动 (Mouse Wheel) ──
let wheelLock = false
const onWheel = (e: WheelEvent) => {
  const delta = Math.abs(e.deltaX) > Math.abs(e.deltaY) ? e.deltaX : e.deltaY
  if (Math.abs(delta) < 20 || wheelLock) return

  wheelLock = true
  stopCarouselAutoPlay()
  if (delta > 0) {
    nextCard()
  } else {
    prevCard()
  }
  setTimeout(() => {
    wheelLock = false
    startCarouselAutoPlay()
  }, 380)
}

// ── 触摸滑动转动 (Touch Swipe) ──
const onTouchStart = (e: TouchEvent) => {
  isDragging.value = true
  dragStartX.value = e.touches[0].clientX
  dragCurrentOffset.value = 0
  hasMovedSignificantly = false
  stopCarouselAutoPlay()
}

const onTouchMove = (e: TouchEvent) => {
  if (!isDragging.value) return
  const diff = e.touches[0].clientX - dragStartX.value
  if (Math.abs(diff) > 5) {
    hasMovedSignificantly = true
  }
  dragCurrentOffset.value = diff * 0.8
}

const onTouchEnd = () => {
  if (!isDragging.value) return
  isDragging.value = false
  const diff = dragCurrentOffset.value
  dragCurrentOffset.value = 0

  if (diff < -35) {
    nextCard()
  } else if (diff > 35) {
    prevCard()
  }
  startCarouselAutoPlay()
}

const onCardClick = (idx: number) => {
  if (hasMovedSignificantly) return
  setCard(idx)
}

const onResize = () => {
  windowWidth.value = window.innerWidth
}

// ── Agent 演练预设场景 ──
const activePromptIndex = ref(0)
const agentPrompts = [
  {
    label: 'RAG 性能优化研报',
    query: '分析最近三篇关于 RAG 性能优化的星记，并输出一份集成报告',
    subtasks: [
      { id: '01', name: '加载目标星记', desc: '从底层检索并加载星记 ID: 104, 107, 112 知识切片' },
      { id: '02', name: '向量混合检索', desc: '基于 Top-K 召回与语义相似度重排序获取优化策略' },
      { id: '03', name: '综合推理排版', desc: '调用 DeepSeek 提炼工程建议并渲染学术标准版式' },
    ],
    execCards: [
      { title: 'SQLite 知识抽取', desc: '从数据库安全提取三篇目标笔记的 Markdown 内容与元数据。', time: '0.4s', detail: '3 篇星记' },
      { title: '高维向量相似度检索', desc: '召回最相关的前 5 段工程优化切片，完成语义对齐。', time: '0.8s', detail: '5 段切片' },
      { title: 'DeepSeek 知识推理', desc: '综合多篇笔记上下文，生成无幻觉的结构化优化方案。', time: '2.1s', detail: '5,040 Tokens' },
    ],
    auditDiff: { old: '"100x retrieval speedup"', new: '"10x retrieval speedup (基准实测)"' },
    auditCheck: '确认 ID: 104, 107, 112 三篇源星记的主旨均在最终交付报告中覆盖。',
  },
  {
    label: '微服务架构演进 PPT',
    query: '提取微服务高并发演进的 4 个关键阶段，自动生成结构化演讲幻灯片',
    subtasks: [
      { id: '01', name: '星图语义聚类', desc: '遍历微服务分类，抓取相关星记的架构拓扑与演进细节' },
      { id: '02', name: '分镜大纲编排', desc: '构建 12 页结构化 PPT 分镜逻辑与压测数据指标' },
      { id: '03', name: 'PPTX 矢量渲染', desc: '调用排版微服务，交付可直接演示的商业级幻灯片文件' },
    ],
    execCards: [
      { title: '星图引力聚类', desc: '抓取微服务演进相关星记，生成架构拓扑关系网。', time: '0.6s', detail: '8 篇笔记' },
      { title: '分镜逻辑编排', desc: '生成 12 页幻灯片脚本与对比图表数据结构。', time: '1.2s', detail: '12 页脚本' },
      { title: 'PPTX 矢量编译', desc: '调用无头排版微服务编译为商业级演示文稿二进制文件。', time: '1.8s', detail: '4.8 MB 文件' },
    ],
    auditDiff: { old: '"单体直接切换为微服务"', new: '"渐进式服务拆分与分布式分库分表"' },
    auditCheck: '校验幻灯片章节逻辑与压测基准数据前后一致性。',
  },
  {
    label: '异步并发 Bug 诊断',
    query: '诊断 FastAPI 异步连接池在高负载下的连接泄漏问题并提供防御性补丁',
    subtasks: [
      { id: '01', name: '可观测性追踪', desc: '抓取 OpenTelemetry 链路中的 504 超时异常堆栈' },
      { id: '02', name: '隔离沙箱重现', desc: '在沙箱中重现 async with 连接池未安全释放边界' },
      { id: '03', name: '生成安全补丁', desc: '生成带自动重试的修复补丁与回归测试用例' },
    ],
    execCards: [
      { title: 'Trace 日志回放', desc: '抓取连接泄漏时刻前后 20 条异常堆栈与调用时序。', time: '0.3s', detail: 'Trace #8821' },
      { title: '沙箱压力复现', desc: '隔离模拟 100 并发场景，精准定位异步死锁调用栈。', time: '1.5s', detail: '100 并发压测' },
      { title: '单元测试生成', desc: '输出防护代码补丁，并执行并通过自动化回归测试。', time: '1.9s', detail: '用例通过率 100%' },
    ],
    auditDiff: { old: '"盲目扩容连接池到 1000"', new: '"重构上下文管理器确保异常时安全归还连接"' },
    auditCheck: '回归压测通过，连接池连接泄漏漏洞已彻底修复。',
  },
]

const currentAgentStep = ref(0)
const agentSteps = [
  { num: '01', name: 'Planner 任务规划', desc: '意图拆解与 DAG 路由' },
  { num: '02', name: 'Executor 并行执行', desc: '工具调用与多维提炼' },
  { num: '03', name: 'Reviewer 质量审计', desc: '自纠偏与交付验收' },
]

// 滚动显示监听
const observeScroll = () => {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('scroll-revealed')
          observer.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.15 },
  )

  document.querySelectorAll('.scroll-reveal').forEach((el) => {
    observer.observe(el)
  })
}

onMounted(() => {
  window.addEventListener('resize', onResize)
  startCarouselAutoPlay()

  nextTick(() => {
    observeScroll()

    if (guestVisual.value) {
      guestVisualContext = gsap.context(() => {
        gsap.fromTo(
          guestVisual.value,
          { opacity: 0, scale: 0.95 },
          { opacity: 1, scale: 1, duration: 0.9, ease: 'power2.out' },
        )
      }, guestVisual.value)
    }
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)
  stopCarouselAutoPlay()
  if (guestVisualContext) guestVisualContext.revert()
})
</script>

<template>
  <div class="landing-page-wrap">
    <!-- ════════ 1. HERO SECTION (星域启航首屏) ════════ -->
    <section class="landing-hero">
      <div class="container hero-container">
        <!-- Left: Brand Narrative -->
        <div class="hero-left-content">
          <div class="hero-pill-tag">
            <span class="pill-pulse-dot"></span>
            <span class="pill-text">AI-POWERED PERSONAL KNOWLEDGE UNIVERSE</span>
          </div>

          <h1 class="hero-brand-title">
            <span class="title-accent">Star</span>lore
          </h1>

          <p class="hero-tagline">
            让知识形成星系 · 让思考成为真实生产力
          </p>

          <p class="hero-desc">
            拒绝概念玩具。Starlore 融合 <strong>Multi-Agent 协同调度</strong>、<strong>3D VR 知识星图</strong> 与 <strong>全链路可观测性</strong>。<br />
            通过 Planner 规划 → Executor 执行 → Reviewer 审查，将碎片星记升维为端到端交付的 PPT、研报与学术文档。
          </p>

          <div class="hero-action-buttons">
            <router-link to="/login" class="btn-primary-action">
              <Zap :size="16" />
              <span>立即开启探索</span>
            </router-link>

            <a href="#features" class="btn-secondary-action">
              <span>浏览核心能力</span>
              <ArrowRight :size="15" />
            </a>
          </div>

          <!-- Stats Strip -->
          <div class="hero-stats-row">
            <div v-for="stat in stats" :key="stat.label" class="stat-pill-item">
              <span class="stat-val">{{ stat.value }}</span>
              <span class="stat-lbl">{{ stat.label }}</span>
            </div>
          </div>
        </div>

        <!-- Right: Pure Hero Companion (舒展独立伴侣球，不再被生硬卡片遮挡) -->
        <div ref="guestVisual" class="hero-right-visual">
          <div class="companion-stage">
            <div class="ambient-nebula-glow" aria-hidden="true"></div>
            <EmotionBall
              :size="320"
              shape="blob"
              emotion="02"
              :show-rings="true"
              :show-style-toggle="true"
              label="Starlore Companion Core"
            />
          </div>
        </div>
      </div>
    </section>

    <!-- Star Separation Band -->
    <div class="container">
      <StellarDotsBand />
    </div>

    <!-- ════════ 2. 核心架构：现代便当盒网格 (Bento Grid) ════════ -->
    <section id="features" class="section-architecture scroll-reveal">
      <div class="container">
        <div class="section-header">
          <span class="section-tag">CORE CAPABILITIES</span>
          <h2 class="section-heading">五大核心生产力引擎</h2>
          <p class="section-subtitle">从离散知识切片到空间星辰，再到智能体端到端交付物的全流程架构</p>
        </div>

        <!-- Modern Card Carousel (统一修长卡片 + 自动平滑轮播 + 鼠标拖拽/滚轮转动) -->
        <div
          class="cards-carousel-container"
          :class="{ 'is-grabbing': isDragging }"
          @mousedown="onMouseDown"
          @wheel.passive="onWheel"
          @touchstart.passive="onTouchStart"
          @touchmove.passive="onTouchMove"
          @touchend="onTouchEnd"
          @mouseenter="stopCarouselAutoPlay"
          @mouseleave="startCarouselAutoPlay"
        >
          <!-- Carousel Viewport & 3D/2D Smooth Stage -->
          <div class="carousel-stage-viewport">
            <div
              v-for="(card, idx) in capabilityCards"
              :key="card.id"
              class="carousel-slide-card"
              :class="{
                'is-active': idx === activeCardIndex,
                'is-neighbor': Math.abs(getCardOffset(idx)) === 1,
              }"
              :style="getCardStyle(idx)"
              @click="onCardClick(idx)"
            >
              <div class="carousel-card-inner">
                <!-- Top Badge Row -->
                <div class="carousel-badge-bar">
                  <span class="carousel-badge-tag" :class="card.themeClass">
                    {{ card.badge }}
                  </span>
                  <span class="carousel-card-status">● Ready</span>
                </div>

                <!-- 52px Icon Box -->
                <div class="carousel-icon-box" :class="card.themeClass">
                  <component :is="card.icon" :size="26" />
                </div>

                <!-- Title & Description -->
                <h3 class="carousel-card-title">{{ card.title }}</h3>
                <p class="carousel-card-desc">{{ card.desc }}</p>

                <!-- Tags Flow -->
                <div class="carousel-tags-flow">
                  <span
                    v-for="chip in card.chips"
                    :key="chip"
                    class="carousel-chip-tag"
                  >
                    {{ chip }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Bottom Control Dock (去除了左/右箭头，仅保留居中 5 个指示圆点) -->
          <div class="carousel-control-dock">
            <div class="dock-dots-track">
              <button
                v-for="(card, idx) in capabilityCards"
                :key="card.id"
                class="dock-dot-btn"
                :class="{ 'is-active': idx === activeCardIndex }"
                @click="setCard(idx)"
                :aria-label="`切换到 ${card.title}`"
              >
                <span class="dock-dot-pill"></span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ════════ 3. 流线型流水线协作演练 (Streamlined Pipeline) ════════ -->
    <section id="sandbox" class="section-pipeline-demo scroll-reveal">
      <div class="container">
        <div class="section-header">
          <span class="section-tag">INTERACTIVE PIPELINE</span>
          <h2 class="section-heading">Multi-Agent 协作演练</h2>
          <p class="section-subtitle">切换不同任务场景与执行节点，实时体验智能体如何拆解意图、并行调用工具与自纠偏审计</p>
        </div>

        <!-- Streamlined Pipeline Canvas -->
        <div class="streamlined-canvas">
          <!-- 1. Top Controls: Scenario Switcher -->
          <div class="canvas-header-bar">
            <div class="canvas-meta-status">
              <span class="meta-dot"></span>
              <span class="meta-text">PIPELINE TRACKER · LIVE</span>
            </div>

            <div class="scenario-pill-selector">
              <button
                v-for="(p, idx) in agentPrompts"
                :key="p.label"
                class="scenario-pill-btn"
                :class="{ 'scenario-pill-btn--active': activePromptIndex === idx }"
                @click="activePromptIndex = idx"
              >
                <Sparkles :size="12" />
                <span>{{ p.label }}</span>
              </button>
            </div>
          </div>

          <!-- 2. Horizontal 3-Step Flow Stepper -->
          <div class="stepper-track">
            <button
              v-for="(step, idx) in agentSteps"
              :key="step.num"
              class="step-track-node"
              :class="{ 'step-track-node--active': currentAgentStep === idx }"
              @click="currentAgentStep = idx"
            >
              <div class="node-num-badge">{{ step.num }}</div>
              <div class="node-text-wrap">
                <span class="node-main-name">{{ step.name }}</span>
                <span class="node-sub-name">{{ step.desc }}</span>
              </div>
            </button>
          </div>

          <!-- 3. Dynamic Stage Display Pane -->
          <div class="canvas-stage-pane">
            <!-- Stage 1: Planner -->
            <div v-if="currentAgentStep === 0" class="stage-slide" key="planner">
              <div class="user-query-card">
                <div class="query-card-tag">
                  <Zap :size="12" />
                  <span>用户输入意图 (USER QUERY)</span>
                </div>
                <div class="query-card-text">
                  "{{ agentPrompts[activePromptIndex].query }}"
                </div>
              </div>

              <div class="stage-section-label">
                <Workflow :size="14" />
                <span>Router Agent DAG 拓扑子任务拆解结果</span>
              </div>

              <div class="cards-triad-grid">
                <div
                  v-for="task in agentPrompts[activePromptIndex].subtasks"
                  :key="task.id"
                  class="triad-card"
                >
                  <div class="triad-card-top">
                    <span class="triad-id-badge">SUBTASK {{ task.id }}</span>
                    <span class="triad-status-text">Ready</span>
                  </div>
                  <h4 class="triad-card-title">{{ task.name }}</h4>
                  <p class="triad-card-desc">{{ task.desc }}</p>
                </div>
              </div>
            </div>

            <!-- Stage 2: Executor -->
            <div v-else-if="currentAgentStep === 1" class="stage-slide" key="executor">
              <div class="stage-section-label">
                <Cpu :size="14" />
                <span>并行工具调用与信息提炼 (Parallel Worker Pool)</span>
              </div>

              <div class="cards-triad-grid">
                <div
                  v-for="(card, i) in agentPrompts[activePromptIndex].execCards"
                  :key="i"
                  class="triad-card"
                >
                  <div class="triad-card-top">
                    <span class="triad-worker-badge">Worker {{ i + 1 }}</span>
                    <span class="triad-success-tag">✓ 执行成功</span>
                  </div>
                  <h4 class="triad-card-title">{{ card.title }}</h4>
                  <p class="triad-card-desc">{{ card.desc }}</p>
                  <div class="triad-meta-row">
                    <span><Clock :size="11" /> {{ card.time }}</span>
                    <span>{{ card.detail }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Stage 3: Reviewer -->
            <div v-else-if="currentAgentStep === 2" class="stage-slide" key="reviewer">
              <div class="stage-section-label">
                <ShieldCheck :size="14" />
                <span>质量审计与自动纠偏反馈环 (Hallucination Detection)</span>
              </div>

              <div class="reviewer-flow-deck">
                <!-- Completeness Check -->
                <div class="review-item-row review-pass">
                  <div class="review-status-icon">✓</div>
                  <div class="review-content">
                    <div class="review-title">知识完整性覆盖校验通过</div>
                    <div class="review-desc">{{ agentPrompts[activePromptIndex].auditCheck }}</div>
                  </div>
                  <span class="review-pill pill-ok">100% 覆盖</span>
                </div>

                <!-- Diff Correction Box -->
                <div class="review-diff-card">
                  <div class="diff-card-meta">
                    <span class="diff-badge">自动纠偏反馈环 (Feedback Loop)</span>
                    <span class="diff-tip">检测到可能夸大表述，触发 Few-Shot 纠偏反馈：</span>
                  </div>
                  <div class="diff-comparison-box">
                    <div class="diff-col diff-before">
                      <span class="diff-col-lbl">原始生成 (Before)</span>
                      <span class="diff-col-code">{{ agentPrompts[activePromptIndex].auditDiff.old }}</span>
                    </div>
                    <div class="diff-arrow-node">→</div>
                    <div class="diff-col diff-after">
                      <span class="diff-col-lbl">审计校准 (After)</span>
                      <span class="diff-col-code">{{ agentPrompts[activePromptIndex].auditDiff.new }}</span>
                    </div>
                  </div>
                </div>

                <!-- Final Verification -->
                <div class="review-item-row review-final">
                  <div class="review-status-icon star-icon">★</div>
                  <div class="review-content">
                    <div class="review-title">最终成果就绪 (Delivery Ready)</div>
                    <div class="review-desc">全链路质量审计完成，已安全编译并推送到客户端。</div>
                  </div>
                  <span class="review-pill pill-purple">VERIFIED</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ════════ 4. LAUNCH DECK (纯净行动号召，告别突兀黑框) ════════ -->
    <section class="section-launch-deck scroll-reveal">
      <div class="container">
        <div class="launch-hero-card">
          <div class="launch-glow-aura" aria-hidden="true"></div>
          <h2 class="launch-headline">准备好构建你的全栈智能知识宇宙了吗？</h2>
          <p class="launch-subline">
            无需繁琐配置，立即开始记录、在 3D 星空中漫游，并体验 Multi-Agent 带来的真实生产力蜕变。
          </p>
          <div class="launch-btn-group">
            <router-link to="/login" class="btn-launch-primary">
              <Zap :size="16" />
              <span>立即开启探索</span>
            </router-link>
          </div>
        </div>

        <!-- ════════ 5. MODERN PLATFORM FOOTER ════════ -->
        <footer class="starlore-site-footer">
          <div class="footer-columns-grid">
            <!-- Col 1: Brand Info -->
            <div class="footer-brand-col">
              <div class="footer-brand-logo">
                <span class="brand-accent">Star</span>lore
              </div>
              <p class="footer-brand-desc">
                AI-Powered Personal Knowledge Universe.<br />
                让碎片化的知识形成星系，让思考成为生产力。
              </p>
              <div class="footer-contact-link">
                <a href="mailto:872709652@qq.com" class="footer-email-btn">
                  <Mail :size="14" />
                  <span>872709652@qq.com</span>
                </a>
              </div>
            </div>

            <!-- Col 2: Products -->
            <div class="footer-nav-col">
              <h5 class="footer-col-heading">产品功能</h5>
              <router-link to="/vr" class="footer-text-link">3D 知识星图</router-link>
              <router-link to="/harness" class="footer-text-link">智能体生产力 (Harness)</router-link>
              <router-link to="/articles" class="footer-text-link">星记知识库</router-link>
              <router-link to="/diverge" class="footer-text-link">灵感发散引擎</router-link>
            </div>

            <!-- Col 3: Technology -->
            <div class="footer-nav-col">
              <h5 class="footer-col-heading">技术架构</h5>
              <span class="footer-plain-text">Vue 3 · Vite 7</span>
              <span class="footer-plain-text">Three.js · WebGL</span>
              <span class="footer-plain-text">LangGraph · Multi-Agent</span>
              <span class="footer-plain-text">DeepSeek · Fast RAG</span>
            </div>

            <!-- Col 4: Platform -->
            <div class="footer-nav-col">
              <h5 class="footer-col-heading">关于与服务</h5>
              <router-link to="/about" class="footer-text-link">关于作者与项目</router-link>
              <span class="footer-plain-text">Starlore 排版微服务</span>
              <span class="footer-plain-text">系统状态: 正常运行</span>
              <span class="footer-plain-text">版本: 2.0-Production</span>
            </div>
          </div>

          <div class="footer-legal-bar">
            <p>© 2026 Starlore. All rights reserved. Designed with Craft, Clarity & Focus.</p>
          </div>
        </footer>
      </div>
    </section>
  </div>
</template>

<style scoped src="./HomeLanding.css"></style>
