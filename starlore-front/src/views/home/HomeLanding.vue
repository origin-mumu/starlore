<script lang="ts" setup>
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { gsap } from 'gsap'
import StellarDotsBand from '@/components/StellarDotsBand.vue'
import EmotionBall from '@/components/EmotionBall.vue'
import {
  ArrowRight,
  Zap,
  BrainCircuit,
  Eye,
  Workflow,
  Database,
  GraduationCap,
  Code,
  Microscope,
  PenTool,
  Sparkles,
  Globe,
  Link,
  Mail,
  GitBranch,
  Activity,
  Terminal,
  Code2,
  Cpu,
  Search,
  ArrowDown,
  Clock,
} from '@lucide/vue'

const guestVisual = ref<HTMLElement | null>(null)
let guestVisualContext: gsap.Context | null = null

const features = [
  {
    icon: GitBranch,
    title: 'Multi-Agent 协作',
    desc: 'Planner-Executor-Reviewer 三角色协作架构，自动拆解复杂任务、并行调用工具、结果自我纠错。',
    tech: 'LangGraph · State Machine · Auto-Retry',
    link: '/harness',
    large: true,
    glow: 'rgba(232, 93, 42, 0.15)',
  },
  {
    icon: Eye,
    title: 'VR 知识星图',
    desc: 'Three.js 驱动的 3D 可视化，将知识映射为星辰，在沉浸式星域中探索。',
    tech: 'WebGL · Three.js · Particle System',
    link: '/vr',
    large: false,
    glow: 'rgba(59, 125, 216, 0.15)',
  },
  {
    icon: BrainCircuit,
    title: '多模态 AI 对话',
    desc: '支持文本、图片、语音多模态输入，DeepSeek & MiMo 大模型驱动，SSE 流式响应。',
    tech: 'SSE Stream · Vision API · Tool Calling',
    link: '/harness',
    large: true,
    glow: 'rgba(123, 154, 255, 0.15)',
  },
  {
    icon: Database,
    title: 'RAG 知识检索',
    desc: '向量化知识库，语义检索你的所有星记，AI 基于你的知识回答问题。',
    tech: 'Embedding · Vector Store · Semantic Search',
    link: '/articles',
    large: false,
    glow: 'rgba(245, 166, 35, 0.15)',
  },
  {
    icon: Workflow,
    title: '创意发散引擎',
    desc: 'AI 驱动的思维导图，从一个关键词发散出无限可能，辅助创意和决策。',
    tech: 'Graph Layout · AI Generation · Real-time',
    link: '/diverge',
    large: false,
    glow: 'rgba(74, 140, 92, 0.15)',
  },
  {
    icon: Activity,
    title: '全链路可观测性',
    desc: '搭建 Tracing 体系，追踪每次 LLM 调用的 Token 消耗、路由耗时与 Prompt 演进。',
    tech: 'Trace · Observability · Bad Case Mining',
    link: '/harness',
    large: true,
    glow: 'rgba(212, 99, 143, 0.15)',
  },
]

const stats = [
  { value: '3-Agent', label: '多智能体协作' },
  { value: '10+', label: 'Function 工具' },
  { value: 'Trace', label: '全链路追踪' },
  { value: '3D', label: '知识可视化' },
]

const currentAgentStep = ref(0)
const agentSteps = [
  {
    name: 'Planner 规划',
    title: '任务拆解与路由规划',
  },
  {
    name: 'Executor 执行',
    title: '并行工具调用与信息提炼',
  },
  {
    name: 'Reviewer 审查',
    title: '结果校验与自我纠正',
  },
]

const handleCardMouseMove = (e: MouseEvent) => {
  const card = e.currentTarget as HTMLElement
  const rect = card.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  card.style.setProperty('--x', `${x}px`)
  card.style.setProperty('--y', `${y}px`)
}

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
    {
      threshold: 0.08,
      rootMargin: '0px 0px -40px 0px',
    },
  )

  const targets = document.querySelectorAll('.scroll-reveal')
  targets.forEach((el) => observer.observe(el))
}

onMounted(() => {
  nextTick(() => {
    observeScroll()
    if (!guestVisual.value) return
    guestVisualContext = gsap.context(() => {
      if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        gsap.to('.reference-orb-ring', {
          rotation: 360,
          duration: 28,
          repeat: -1,
          ease: 'none',
          transformOrigin: '50% 50%',
        })
      }
    }, guestVisual.value)
  })
})

onUnmounted(() => {
  guestVisualContext?.revert()
})
</script>

<template>
  <div class="landing-page-wrap">
    <!-- Hero Section -->
    <section class="landing-hero">
      <div class="container hero-grid">
        <!-- Left Text Content -->
        <div class="hero-text-content">
          <div class="kicker-pill fade-in-up">
            <span class="pill-dot"></span>
            <span class="pill-text">✦ 让知识形成自己的星系</span>
          </div>

          <h1 class="hero-title fade-in-up" style="animation-delay: 0.1s">
            <span class="hero-title-accent">Star</span>lore
          </h1>
          <p class="hero-tagline fade-in-up" style="animation-delay: 0.2s">
            AI-Powered Personal Knowledge Universe
          </p>
          <p class="hero-desc fade-in-up" style="animation-delay: 0.3s">
            融合 Multi-Agent 协作、全链路可观测性、RAG 知识检索与 3D 可视化的智能知识系统。<br />
            通过 Planner 规划 → Executor 执行 → Reviewer 审查，让 AI 真正深入理解您的碎片化知识。
          </p>
          <div class="hero-actions fade-in-up" style="animation-delay: 0.4s">
            <router-link to="/login" class="hero-action-btn hero-action-btn--primary">
              <Terminal :size="16" />
              <span>开始探索</span>
            </router-link>
            <a href="#features" class="hero-action-btn hero-action-btn--secondary">
              <Code2 :size="16" />
              <span>技术架构</span>
            </a>
          </div>

          <!-- Stats -->
          <div class="hero-stats fade-in-up" style="animation-delay: 0.5s">
            <div v-for="stat in stats" :key="stat.label" class="stat-item">
              <span class="stat-value">{{ stat.value }}</span>
              <span class="stat-label">{{ stat.label }}</span>
            </div>
          </div>
        </div>

        <!-- Interactive Emotion Ball Hero Visual -->
        <div
          ref="guestVisual"
          class="hero-visual-container fade-in-up"
          style="animation-delay: 0.25s"
        >
          <div class="hero-emotion-ball-wrapper">
            <div class="hero-ball-aura" aria-hidden="true"></div>
            <EmotionBall
              :size="330"
              shape="blob"
              emotion="02"
              :show-rings="true"
              :show-style-toggle="true"
              label="Starlore AI Companion"
            />
            <div class="hero-ball-caption">
              <span class="hero-ball-tag">✦ STARLORE AI COMPANION</span>
              <span class="hero-ball-sub">CAPTURE · CONNECT · REDISCOVER</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="container">
      <StellarDotsBand />
    </div>

    <!-- Features Section -->
    <section id="features" class="section-parchment">
      <div class="container">
        <div class="section-header scroll-reveal">
          <span class="section-tag">CORE FEATURES</span>
          <h2 class="section-heading">核心能力</h2>
          <p class="section-subtitle">六大技术模块，构建智能知识系统</p>
        </div>
        <div class="bento-grid">
          <div
            v-for="(feature, i) in features"
            :key="feature.title"
            class="feature-card starlore-spotlight scroll-reveal"
            :class="{ 'feature-card--large': feature.large }"
            :style="{
              transitionDelay: `${i * 0.08}s`,
              '--hover-glow-color': feature.glow,
            }"
            @mousemove="handleCardMouseMove"
          >
            <div class="feature-header">
              <div class="feature-icon">
                <component :is="feature.icon" :size="22" />
              </div>
            </div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-desc">{{ feature.desc }}</p>
            <div class="feature-footer">
              <div class="feature-tech-chips">
                <span
                  v-for="tag in feature.tech.split(' · ')"
                  :key="tag"
                  class="tech-chip"
                >{{ tag }}</span>
              </div>
              <router-link v-if="feature.link" :to="feature.link" class="feature-link">
                探索 <ArrowRight :size="14" />
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Agent Showcase Section -->
    <section class="agent-showcase-section scroll-reveal">
      <div class="container">
        <div class="section-header">
          <span class="section-tag">AGENT WORKFLOW</span>
          <h2 class="section-heading">Multi-Agent 协作演练</h2>
          <p class="section-subtitle">点击下方不同阶段，实时观测智能体如何拆解、执行和审计您的任务</p>
        </div>

        <div class="showcase-container">
          <!-- Left Steps Selector -->
          <div class="showcase-nav">
            <button
              v-for="(step, idx) in agentSteps"
              :key="step.name"
              class="showcase-nav-btn"
              :class="{ 'showcase-nav-btn--active': currentAgentStep === idx }"
              @click="currentAgentStep = idx"
            >
              <div class="step-num">0{{ idx + 1 }}</div>
              <div class="step-meta">
                <span class="step-name">{{ step.name }}</span>
                <span class="step-title">{{ step.title }}</span>
              </div>
            </button>
          </div>

          <!-- Right Visual Workflow Panel -->
          <div class="showcase-terminal">
            <div class="terminal-header">
              <div class="terminal-dots">
                <span class="dot-red"></span>
                <span class="dot-yellow"></span>
                <span class="dot-green"></span>
              </div>
              <div class="observatory-header-title">
                <BrainCircuit :size="14" class="observatory-icon" />
                <span>工作流观测台 · Pipeline Tracker</span>
              </div>
              <span class="terminal-status">ACTIVE PIPELINE</span>
            </div>
            <div class="observatory-content">
              <!-- STEP 1: Planner -->
              <div v-if="currentAgentStep === 0" class="pipeline-view" key="planner">
                <div class="pipeline-step-title">意图拆解与子任务规划</div>

                <div class="visual-intent-card">
                  <div class="intent-card-header">
                    <Zap :size="12" class="tag-icon" />
                    <span>用户输入意图 (User Query)</span>
                  </div>
                  <div class="intent-card-body">
                    "分析最近三篇关于 <span class="highlight-text">RAG 性能优化</span> 的星记，并输出一份集成报告"
                  </div>
                </div>

                <div class="pipeline-connector">
                  <ArrowDown :size="14" class="connector-arrow-icon" />
                  <span class="connector-text">意图路由器 (Router Agent) 规划</span>
                </div>

                <div class="tasks-horizontal-flow">
                  <div class="task-flow-node">
                    <div class="node-badge node-db"><Database :size="14" /></div>
                    <div class="node-info">
                      <span class="node-name">子任务 01</span>
                      <span class="node-desc">从数据库加载指定星记 (ID: 104, 107, 112)</span>
                    </div>
                  </div>

                  <div class="task-flow-node">
                    <div class="node-badge node-search"><Search :size="14" /></div>
                    <div class="node-info">
                      <span class="node-name">子任务 02</span>
                      <span class="node-desc">召回向量数据库中 RAG 优化上下文片段</span>
                    </div>
                  </div>

                  <div class="task-flow-node">
                    <div class="node-badge node-llm"><Cpu :size="14" /></div>
                    <div class="node-info">
                      <span class="node-name">子任务 03</span>
                      <span class="node-desc">调用 DeepSeek 提炼并学术化汇总生成报告</span>
                    </div>
                  </div>
                </div>

                <div class="pipeline-connector">
                  <ArrowDown :size="14" class="connector-arrow-icon" />
                  <span class="connector-text">拓扑状态机初始化完成</span>
                </div>

                <div class="pipeline-status-badge">
                  <Workflow :size="12" />
                  <span>目标：路由任务流至 Executor 并行执行</span>
                </div>
              </div>

              <!-- STEP 2: Executor -->
              <div v-else-if="currentAgentStep === 1" class="pipeline-view" key="executor">
                <div class="pipeline-step-title">并行工具调用与多维提炼</div>

                <div class="executor-cards-container">
                  <!-- Task 1 -->
                  <div class="executor-card">
                    <div class="exec-header">
                      <div class="exec-title">
                        <Database :size="14" class="icon-db" />
                        <span>SQLite 知识提取</span>
                      </div>
                      <span class="exec-badge badge-success">✓ 成功</span>
                    </div>
                    <p class="exec-detail">从底层数据库加载三篇目标星记的 Markdown 格式文本内容。</p>
                    <div class="exec-meta">
                      <span><Clock :size="10" /> 耗时: 0.4s</span>
                      <span>加载: 3 篇</span>
                    </div>
                  </div>

                  <!-- Task 2 -->
                  <div class="executor-card">
                    <div class="exec-header">
                      <div class="exec-title">
                        <Search :size="14" class="icon-search" />
                        <span>向量相似度混合检索</span>
                      </div>
                      <span class="exec-badge badge-success">✓ 成功</span>
                    </div>
                    <p class="exec-detail">执行相似度搜索，提取最相关的 5 段 RAG 性能优化文档切片。</p>
                    <div class="exec-meta">
                      <span><Clock :size="10" /> 耗时: 0.8s</span>
                      <span>召回: 5 段</span>
                    </div>
                  </div>

                  <!-- Task 3 -->
                  <div class="executor-card">
                    <div class="exec-header">
                      <div class="exec-title">
                        <Cpu :size="14" class="icon-cpu" />
                        <span>DeepSeek 推理生成</span>
                      </div>
                      <span class="exec-badge badge-success">✓ 成功</span>
                    </div>
                    <p class="exec-detail">调用 LLM 综合语义上下文，生成整合的性能优化技术建议报告。</p>
                    <div class="exec-meta">
                      <span><Clock :size="10" /> 耗时: 2.1s</span>
                      <span>Token: 5,040</span>
                    </div>
                  </div>
                </div>

                <div class="pipeline-connector">
                  <ArrowDown :size="14" class="connector-arrow-icon" />
                  <span class="connector-text">执行输出已封装</span>
                </div>

                <div class="pipeline-status-badge">
                  <Zap :size="12" />
                  <span>目标：提交输出草稿给 Reviewer 审计</span>
                </div>
              </div>

              <!-- STEP 3: Reviewer -->
              <div v-else-if="currentAgentStep === 2" class="pipeline-view" key="reviewer">
                <div class="pipeline-step-title">输出质量审计与反馈修正</div>

                <div class="reviewer-audit-flow">
                  <!-- Audit 1 -->
                  <div class="audit-item audit-success">
                    <div class="audit-badge">✓</div>
                    <div class="audit-info">
                      <div class="audit-name">完整性覆盖校验 (Completeness Check)</div>
                      <div class="audit-desc">通过。确认 ID: 104, 107, 112 三篇源星记的主旨均在报告中覆盖。</div>
                    </div>
                  </div>

                  <!-- Audit 2 -->
                  <div class="audit-item audit-warning">
                    <div class="audit-badge">!</div>
                    <div class="audit-info">
                      <div class="audit-name">事实一致性校验 (Hallucination Detection)</div>
                      <div class="audit-desc">检测到幻觉风险！关于 Section 2 中检索提速的表述。</div>

                      <div class="audit-correction-ui">
                        <span class="correction-tag">自动纠偏反馈环 (Feedback Loop)</span>
                        <div class="diff-comparison">
                          <span class="diff-old">"100x retrieval speedup"</span>
                          <span class="diff-arrow">→</span>
                          <span class="diff-new">"10x retrieval speedup"</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Audit 3 -->
                  <div class="audit-item audit-success-final">
                    <div class="audit-badge">★</div>
                    <div class="audit-info">
                      <div class="audit-name">最终交付物就绪 (Response Compilation)</div>
                      <div class="audit-desc">编译成功。生成一份高质量的技术集成报告并推送到客户端。</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Use Cases Section -->
    <section class="section-white">
      <div class="container">
        <div class="section-header scroll-reveal">
          <span class="section-tag">USE CASES</span>
          <h2 class="section-heading">使用场景</h2>
          <p class="section-subtitle">适合不同人群的知识管理需求</p>
        </div>
        <div class="use-cases-grid">
          <div class="use-case-card scroll-reveal" style="transition-delay: 0.1s">
            <div class="use-case-icon"><GraduationCap :size="36" /></div>
            <h3 class="use-case-title">学生学习</h3>
            <p class="use-case-desc">整理课程笔记、论文资料，用 AI 快速检索知识点，构建个人知识体系。</p>
          </div>
          <div class="use-case-card scroll-reveal" style="transition-delay: 0.2s">
            <div class="use-case-icon"><Code :size="36" /></div>
            <h3 class="use-case-title">开发者</h3>
            <p class="use-case-desc">记录技术笔记、调试经验，用 RAG 检索历史问题，提升开发效率。</p>
          </div>
          <div class="use-case-card scroll-reveal" style="transition-delay: 0.3s">
            <div class="use-case-icon"><Microscope :size="36" /></div>
            <h3 class="use-case-title">研究者</h3>
            <p class="use-case-desc">管理文献综述、实验数据，用 AI 辅助分析，发现知识间的关联。</p>
          </div>
          <div class="use-case-card scroll-reveal" style="transition-delay: 0.4s">
            <div class="use-case-icon"><PenTool :size="36" /></div>
            <h3 class="use-case-title">内容创作者</h3>
            <p class="use-case-desc">收集灵感素材、写作素材，用 AI 发散创意，提升创作效率。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Vision Section -->
    <section class="vision-section">
      <div class="container">
        <div class="vision-content scroll-reveal">
          <span class="section-tag">OUR VISION</span>
          <h2 class="vision-title">让知识像星辰一样闪耀</h2>
          <p class="vision-desc">
            我们相信，每一份知识都值得被记录和探索。<br />
            Starlore 致力于打造一个智能、沉浸式的个人知识宇宙，<br />
            让碎片化的知识形成星座，在星空中找到它们的联系。
          </p>
          <div class="vision-values">
            <div class="value-item">
              <Sparkles :size="32" class="value-icon" />
              <span class="value-text">智能</span>
            </div>
            <div class="value-item">
              <Globe :size="32" class="value-icon" />
              <span class="value-text">沉浸</span>
            </div>
            <div class="value-item">
              <Link :size="32" class="value-icon" />
              <span class="value-text">连接</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Contact Section -->
    <section class="section-parchment">
      <div class="container">
        <div class="contact-content scroll-reveal">
          <span class="section-tag">CONTACT</span>
          <h2 class="section-heading">联系我们</h2>
          <p class="section-subtitle">有任何问题或建议，欢迎联系</p>
          <div class="contact-links">
            <a href="mailto:872709652@qq.com" class="contact-item">
              <Mail :size="24" class="contact-icon" />
              <span class="contact-label">邮箱</span>
              <span class="contact-value">872709652@qq.com</span>
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta-section">
      <div class="container">
        <div class="cta-content scroll-reveal">
          <div class="cta-code">
            <span class="cta-code-prompt">></span>
            <span class="cta-code-text">starlore.start()</span>
          </div>
          <h2 class="cta-title">准备好构建你的知识宇宙了吗？</h2>
          <p class="cta-desc">登录后解锁完整功能，开始记录和探索</p>
          <router-link to="/login" class="btn-primary btn-lg">
            <Zap :size="16" />
            立即登录
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped src="./HomeLanding.css"></style>
