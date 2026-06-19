<script lang="ts" setup>
import { onMounted, ref, nextTick } from 'vue'
import { getBlogStatsService } from '@/api/article'
import SideBar from '@/components/sideBar.vue'
import { Article } from '@/type/Article'
import DummyCard from '@/components/dummyCard.vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import ParticleGlobe from '@/components/ParticleGlobe.vue'
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
  Bot,
  Shield,
  BarChart3,
  Terminal,
  Code2,
  FileArchive,
  Rocket,
  Cpu,
  Search,
  ArrowDown,
  CheckCircle,
  AlertTriangle,
  Clock,
  Check,
} from '@lucide/vue'

const userStore = useUserStore()
const themeStore = useThemeStore()

const themeColors: Record<string, string> = {
  default: '#E85D2A',
  dark: '#6385FF',
  green: '#4A8C5C',
  pink: '#D4638F',
}

const themeSecondaryColors: Record<string, string> = {
  default: '#FFAE19', // Amber
  dark: '#E5B869',    // Soft starlight gold
  green: '#C49A3A',   // Olive gold
  pink: '#FFE4E1',    // Misty rose
}

const demoArticles: Article[] = [
  {
    id: -1,
    title: '欢迎来到 Starlore',
    summary: '这是一个演示星记，登录后可以查看真实内容',
    category: '演示',
    createdAt: new Date().toISOString(),
    coverImage: '',
  } as any,
  {
    id: -2,
    title: '探索知识星域',
    summary: '在 VR 星图中浏览你的知识版图',
    category: '演示',
    createdAt: new Date().toISOString(),
    coverImage: '',
  } as any,
  {
    id: -3,
    title: 'AI 创意发散',
    summary: '用 AI 帮你拓展思维边界',
    category: '演示',
    createdAt: new Date().toISOString(),
    coverImage: '',
  } as any,
]

const features = [
  {
    icon: GitBranch,
    title: 'Multi-Agent 协作',
    desc: 'Planner-Executor-Reviewer 三角色协作架构，自动拆解复杂任务、并行调用工具、结果自我纠错。',
    tech: 'LangGraph · State Machine · Auto-Retry',
    link: '/echobot',
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
    link: '/echobot',
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
    link: '/echobot',
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
    log: [
      '> [Planner] Received complex query: "分析最近三篇关于 RAG 性能优化的星记，并输出一份集成报告"',
      '> [Planner] Query analyzed. Decomposing into sub-tasks:',
      '  Sub-task 1: Fetch target articles (ID: 104, 107, 112) from database.',
      '  Sub-task 2: Call Semantic Search tool to extract key RAG optimizations.',
      '  Sub-task 3: Summarize and generate final integrate report using DeepSeek model.',
      '> [Planner] Routing sub-tasks to Executor. Graph state initialized.'
    ]
  },
  {
    name: 'Executor 执行',
    title: '并行工具调用与信息提炼',
    log: [
      '> [Executor] Sub-task 1 active: fetching articles...',
      '  [Tool: Database] Query successful. 3 documents loaded.',
      '> [Executor] Sub-task 2 active: semantic search extraction...',
      '  [Tool: VectorDB] Querying for: "RAG optimization context chunk"',
      '  [Tool: VectorDB] Returned 5 matching text chunks. High confidence.',
      '> [Executor] Sub-task 3 active: generating integration report...',
      '  [LLM Call] Prompt token size: 4200, Output token size: 840. Time: 2.1s',
      '> [Executor] Integration report compiled. Passing output to Reviewer.'
    ]
  },
  {
    name: 'Reviewer 审查',
    title: '结果校验与自我纠正',
    log: [
      '> [Reviewer] Auditing integration report content...',
      '> [Reviewer] Check 1: Completeness - OK (All 3 articles analyzed).',
      '> [Reviewer] Check 2: Hallucination mitigation - Warning detected!',
      '  Found unsupported statement in Section 2 regarding "100x retrieval speedup".',
      '  [Auto-Correction] Invoking LLM feedback loop to rewrite Section 2.',
      '  [LLM Correct] Corrected statement to "10x retrieval speedup".',
      '> [Reviewer] Final check - OK. Compiling response payload. Success.'
    ]
  }
]

const data = ref()
const articles = ref<Article[]>()

const handleCardMouseMove = (e: MouseEvent) => {
  const card = e.currentTarget as HTMLElement
  const rect = card.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  card.style.setProperty('--x', `${x}px`)
  card.style.setProperty('--y', `${y}px`)
}

const observeScroll = () => {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('scroll-revealed')
        observer.unobserve(entry.target)
      }
    })
  }, {
    threshold: 0.08,
    rootMargin: '0px 0px -40px 0px'
  })

  const targets = document.querySelectorAll('.scroll-reveal')
  targets.forEach((el) => observer.observe(el))
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    articles.value = demoArticles
    nextTick(() => {
      observeScroll()
    })
    return
  }
  try {
    const res = await getBlogStatsService()
    data.value = res.data
    articles.value = res.data?.popularArticles ?? []
  } catch (err) {
    console.error('获取博客统计失败:', err)
    articles.value = []
  }
})
</script>

<template>
  <div class="page-container">
    <!-- Landing Page for Non-Logged-In Users -->
    <template v-if="!userStore.isLoggedIn">
      <!-- Hero Section -->
      <section class="landing-hero">
        <div class="container hero-grid">
          <!-- Left Text Content -->
          <div class="hero-text-content">
            <div class="kicker-pill fade-in-up">
              <span class="pill-dot"></span>
              <span class="pill-text">✦ Starlore Knowledge OS v2.0</span>
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
              <router-link to="/login" class="btn-primary btn-lg">
                <Terminal :size="16" />
                开始探索
              </router-link>
              <a href="#features" class="btn-secondary btn-lg">
                <Code2 :size="16" />
                技术架构
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

          <!-- Right Interactive WebGL Globe -->
          <div class="hero-visual-container fade-in-up" style="animation-delay: 0.25s">
            <ParticleGlobe 
              :key="themeStore.current" 
              :color="themeColors[themeStore.current] || '#E85D2A'" 
              :secondaryColor="themeSecondaryColors[themeStore.current] || '#FFAE19'"
            />
          </div>
        </div>
      </section>

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
              class="feature-card scroll-reveal"
              :class="{ 'feature-card--large': feature.large }"
              :style="{ 
                transitionDelay: `${i * 0.08}s`,
                '--hover-glow-color': feature.glow 
              }"
              @mousemove="handleCardMouseMove"
            >
              <div class="feature-header">
                <div class="feature-icon">
                  <component :is="feature.icon" :size="24" />
                </div>
                <span class="feature-tech">{{ feature.tech }}</span>
              </div>
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-desc">{{ feature.desc }}</p>
              <router-link v-if="feature.link" :to="feature.link" class="feature-link">
                探索 <ArrowRight :size="14" />
              </router-link>
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
                        <span class="node-desc">调用 DeepSeek-V3 提炼并学术化汇总生成报告</span>
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
              <p class="use-case-desc">
                整理课程笔记、论文资料，用 AI 快速检索知识点，构建个人知识体系。
              </p>
            </div>
            <div class="use-case-card scroll-reveal" style="transition-delay: 0.2s">
              <div class="use-case-icon"><Code :size="36" /></div>
              <h3 class="use-case-title">开发者</h3>
              <p class="use-case-desc">
                记录技术笔记、调试经验，用 RAG 检索历史问题，提升开发效率。
              </p>
            </div>
            <div class="use-case-card scroll-reveal" style="transition-delay: 0.3s">
              <div class="use-case-icon"><Microscope :size="36" /></div>
              <h3 class="use-case-title">研究者</h3>
              <p class="use-case-desc">
                管理文献综述、实验数据，用 AI 辅助分析，发现知识间的关联。
              </p>
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
    </template>

    <!-- Logged-In User Home Page -->
    <template v-else>
      <!-- Hero Section -->
      <section class="hero-section">
        <div class="hero-container">
          <div class="hero-content fade-in-up">
            <span class="hero-kicker">PERSONAL STARLORE</span>
            <h1 class="hero-title-user">记录创造的<br />每一刻</h1>
            <p class="hero-desc-user">代码、设计、思考。在这里分享我的学习旅程和项目实践。</p>
            <div class="hero-actions">
              <router-link to="/articles" class="btn-primary">阅读星记</router-link>
            </div>
          </div>

          <!-- 右侧装饰区域 -->
          <div class="hero-visual fade-in-up" style="animation-delay: 0.3s">
            <!-- 浮动装饰点 -->
            <div class="visual-dot visual-dot--1"></div>
            <div class="visual-dot visual-dot--2"></div>
            <div class="visual-dot visual-dot--3"></div>

            <!-- RAG 搜索卡片 -->
            <div class="visual-card visual-card--search">
              <div class="card-header-mini">
                <div class="terminal-dots">
                  <span class="dot-red"></span>
                  <span class="dot-yellow"></span>
                  <span class="dot-green"></span>
                </div>
                <span class="mini-title">Quick Search</span>
              </div>
              <div class="search-content-mini">
                <div class="search-input-mock">
                  <span class="search-icon">🔍</span>
                  <span class="search-text">RAG: "知识图谱优化"</span>
                </div>
                <div class="search-results-mock">
                  <div class="result-item-mock">
                    <span class="result-dot">✦</span>
                    <span class="result-name">3D 沉浸式图谱设计</span>
                    <span class="result-relevance">98%</span>
                  </div>
                  <div class="result-item-mock">
                    <span class="result-dot">✦</span>
                    <span class="result-name">LangGraph 状态机定义</span>
                    <span class="result-relevance">89%</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 笔记卡片 -->
            <div class="visual-card visual-card--note">
              <div class="note-header">
                <div class="note-icon"><FileArchive /></div>
                <div class="note-title">今日笔记</div>
              </div>
              <div class="note-lines">
                <div class="note-line"></div>
                <div class="note-line"></div>
                <div class="note-line"></div>
                <div class="note-line"></div>
              </div>
            </div>

            <!-- 品牌卡片 -->
            <div class="visual-card visual-card--tag">
              <div class="tag-icon">✦</div>
              <div class="tag-text">Starlore</div>
              <div class="tag-sub">AI-Powered Knowledge</div>
            </div>
          </div>
        </div>
      </section>

      <!-- Articles Section -->
      <section class="section-parchment">
        <div class="container">
          <div class="section-header-logged fade-in-up" style="animation-delay: 0.25s">
            <h2 class="section-heading">最新星记</h2>
            <router-link to="/articles" class="see-all">查看全部 <span>→</span></router-link>
          </div>
          <div class="content-layout">
            <main class="articles-grid">
              <div
                v-if="articles && articles.length > 0"
                v-for="(article, index) in articles"
                :key="article.id"
                class="fade-in-up"
                :style="{ animationDelay: `${0.3 + index * 0.08}s` }"
              >
                <DummyCard v-bind="article" />
              </div>
              <div v-else class="empty-state fade-in-up" style="animation-delay: 0.3s">
                <p class="empty-title">还没有星记</p>
                <p class="empty-desc">开始写你的第一篇星记吧</p>
                <router-link to="/articles/edit" class="btn-primary">写星记</router-link>
              </div>
            </main>
            <SideBar />
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
/* ── Landing Hero ── */
.hero-grid {
  display: grid;
  grid-template-columns: 55fr 45fr;
  gap: 48px;
  align-items: center;
  text-align: left;
  padding: 140px 0 80px;
}

.hero-text-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.kicker-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: var(--warm-soft);
  border: 1px solid var(--border-interactive);
  border-radius: var(--radius-full);
  margin-bottom: 24px;
}

.kicker-pill .pill-dot {
  width: 6px;
  height: 6px;
  background: var(--accent);
  border-radius: 50%;
  animation: pulse-pill 2s infinite;
}

.kicker-pill .pill-text {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 0.05em;
  font-family: 'Fira Code', 'Consolas', monospace;
}

@keyframes pulse-pill {
  0% { transform: scale(0.9); opacity: 0.6; }
  50% { transform: scale(1.1); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.6; }
}

/* 3D Visual container */
.hero-visual-container {
  display: flex;
  justify-content: center;
  align-items: center;
  perspective: 1000px;
}

.instrument-panel {
  width: 100%;
  aspect-ratio: 1;
  max-width: 440px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.01));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-xl);
  box-shadow: 
    0 30px 60px rgba(0, 0, 0, 0.25),
    inset 0 0 20px rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  animation: floatPanel 8s ease-in-out infinite;
}

.instrument-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.7rem;
  color: var(--ink-muted);
}

.instrument-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 8px var(--accent);
}

.instrument-title {
  flex: 1;
  font-weight: 500;
  text-align: left;
}

.instrument-coords {
  opacity: 0.7;
}

.globe-viewport {
  flex: 1;
  position: relative;
  min-height: 0;
  cursor: grab;
}

.globe-viewport:active {
  cursor: grabbing;
}

.instrument-footer {
  padding: 10px 16px;
  background: rgba(0, 0, 0, 0.15);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.68rem;
  color: var(--ink-muted);
}

.scan-status {
  letter-spacing: 0.05em;
}

.radar-line {
  width: 24px;
  height: 2px;
  background: var(--accent);
  position: relative;
  overflow: hidden;
  border-radius: 1px;
}

.radar-line::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: white;
  animation: scan 1.5s infinite linear;
}

@keyframes scan {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@keyframes floatPanel {
  0%, 100% {
    transform: translateY(0) rotateX(0deg) rotateY(0deg);
  }
  50% {
    transform: translateY(-8px) rotateX(2deg) rotateY(-1deg);
  }
}

/* Hero Text */
.hero-title {
  font-size: clamp(3rem, 7vw, 5rem);
  font-weight: 900;
  letter-spacing: -0.04em;
  color: var(--ink);
  margin: 0 0 16px;
  line-height: 1.1;
  text-align: left;
}

.hero-title-accent {
  background: var(--accent-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-tagline {
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--accent);
  margin: 0 0 20px;
  text-align: left;
}

.hero-desc {
  font-size: 1.05rem;
  color: var(--ink-soft);
  max-width: 65ch;
  margin: 0 0 40px;
  line-height: 1.8;
  text-align: left;
}

.hero-actions {
  display: flex;
  gap: 14px;
  justify-content: flex-start;
  flex-wrap: wrap;
}

.btn-lg {
  padding: 14px 28px;
  font-size: 0.95rem;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

/* Stats */
.hero-stats {
  display: flex;
  justify-content: flex-start;
  gap: 48px;
  margin-top: 64px;
  padding-top: 48px;
  border-top: 1px solid var(--border);
  width: 100%;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.stat-value {
  font-size: 2rem;
  font-weight: 800;
  color: var(--accent);
  font-family: 'Fira Code', 'Consolas', monospace;
}

.stat-label {
  font-size: 0.82rem;
  color: var(--ink-muted);
  font-weight: 500;
}

/* ── Section Tag ── */
.section-tag {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: var(--accent);
  margin-bottom: 12px;
  font-family: 'Fira Code', 'Consolas', monospace;
}

.section-header {
  text-align: center;
  margin-bottom: 56px;
}

.section-subtitle {
  font-size: 1.05rem;
  color: var(--ink-muted);
  margin: 8px 0 0;
}

/* ── Bento Grid ── */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 32px;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  transition: all var(--transition);
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(400px circle at var(--x, 0px) var(--y, 0px), var(--hover-glow-color, rgba(232, 93, 42, 0.08)), transparent 80%);
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 
    var(--shadow-card-hover),
    0 10px 30px var(--hover-glow-color, rgba(232, 93, 42, 0.05));
  border: 1px solid transparent;
  background: 
    linear-gradient(var(--glass-bg), var(--glass-bg)) padding-box,
    var(--accent-gradient) border-box;
}

.feature-card--large {
  grid-column: span 2;
}

.feature-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.feature-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--warm-soft);
  border-radius: var(--radius-md);
  color: var(--accent);
}

.feature-tech {
  font-size: 0.7rem;
  font-family: 'Fira Code', 'Consolas', monospace;
  color: var(--ink-muted);
  padding: 4px 10px;
  background: var(--canvas);
  border-radius: var(--radius-sm);
}

.feature-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 10px;
  text-align: left;
}

.feature-desc {
  font-size: 0.92rem;
  color: var(--ink-soft);
  line-height: 1.7;
  margin: 0 0 24px;
  flex: 1;
  text-align: left;
}

.feature-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  transition: gap var(--transition);
}

.feature-link:hover {
  gap: 10px;
}

/* ── Agent Showcase ── */
.agent-showcase-section {
  padding: 80px 0;
}

.showcase-container {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 32px;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  overflow: hidden;
  padding: 24px;
}

.showcase-nav {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.showcase-nav-btn {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  text-align: left;
  transition: all var(--transition);
}

.showcase-nav-btn:hover {
  background: rgba(255, 255, 255, 0.4);
  border-color: var(--border-interactive);
}

.showcase-nav-btn--active {
  background: var(--surface) !important;
  border-color: var(--accent) !important;
  box-shadow: var(--shadow-sm);
}

.step-num {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--ink-muted);
  transition: color var(--transition);
}

.showcase-nav-btn--active .step-num {
  color: var(--accent);
}

.step-meta {
  display: flex;
  flex-direction: column;
}

.step-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ink);
}

.step-title {
  font-size: 0.78rem;
  color: var(--ink-soft);
  margin-top: 2px;
}

.showcase-terminal {
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 480px;
}

.terminal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: rgba(0, 0, 0, 0.03);
  border-bottom: 1px solid var(--border);
  font-size: 0.8rem;
  color: var(--ink-soft);
}

.observatory-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--ink);
}

.observatory-icon {
  color: var(--accent);
}

.terminal-dots {
  display: flex;
  gap: 6px;
}

.terminal-dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot-red { background: #ff5f57; }
.dot-yellow { background: #febc2e; }
.dot-green { background: #28c840; }

.terminal-status {
  color: var(--accent);
  font-weight: 600;
  font-size: 0.72rem;
  letter-spacing: 0.5px;
  animation: pulse-pill 2s infinite;
}

.observatory-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  text-align: left;
  display: flex;
  flex-direction: column;
}

/* Pipeline views */
.pipeline-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  opacity: 0;
  transform: translateY(10px);
  animation: slideInPipeline 0.4s ease-out forwards;
}

@keyframes slideInPipeline {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.pipeline-step-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 4px;
}

/* Step 1: Planner specific styles */
.visual-intent-card {
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 14px 18px;
  box-shadow: var(--shadow-sm);
}

[data-theme="dark"] .visual-intent-card {
  background: rgba(255, 255, 255, 0.05);
}

.intent-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--accent);
  text-transform: uppercase;
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}

.intent-card-body {
  font-size: 0.95rem;
  color: var(--ink);
  line-height: 1.6;
}

.highlight-text {
  font-weight: 700;
  color: var(--accent);
  border-bottom: 2px solid var(--accent);
  padding-bottom: 1px;
}

.pipeline-connector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 0.78rem;
  color: var(--ink-soft);
  padding: 4px 0;
}

.connector-arrow-icon {
  color: var(--accent);
  animation: bounceArrow 1.5s infinite;
}

@keyframes bounceArrow {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(3px); }
}

.connector-text {
  font-weight: 600;
  font-family: 'LXGW WenKai', serif;
}

.tasks-horizontal-flow {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.task-flow-node {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: var(--shadow-sm);
  transition: transform 0.3s var(--ease-out-quart);
}

[data-theme="dark"] .task-flow-node {
  background: rgba(255, 255, 255, 0.04);
}

.task-flow-node:hover {
  transform: translateY(-2px);
  border-color: var(--border-interactive);
}

.node-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.node-db {
  background: rgba(232, 93, 42, 0.1);
  color: var(--accent-1);
}
.node-search {
  background: rgba(74, 140, 92, 0.1);
  color: var(--accent-2);
}
.node-llm {
  background: rgba(123, 154, 255, 0.1);
  color: #7B9AFF;
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.node-name {
  font-size: 0.72rem;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-weight: 700;
  color: var(--ink-soft);
}

.node-desc {
  font-size: 0.8rem;
  line-height: 1.4;
  color: var(--ink);
}

.pipeline-status-badge {
  display: inline-flex;
  align-items: center;
  align-self: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--accent-soft);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  font-size: 0.8rem;
  color: var(--accent);
  font-weight: 700;
  box-shadow: var(--shadow-sm);
}

/* Step 2: Executor specific styles */
.executor-cards-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  flex: 1;
}

.executor-card {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: var(--shadow-sm);
  transition: transform 0.3s var(--ease-out-quart), border-color 0.3s;
}

[data-theme="dark"] .executor-card {
  background: rgba(255, 255, 255, 0.04);
}

.executor-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
}

.exec-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.exec-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  color: var(--ink);
  font-size: 0.85rem;
}

.exec-title svg {
  color: var(--accent);
}

.exec-badge {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.badge-success {
  background: rgba(40, 200, 64, 0.12);
  color: #28c840;
}

.exec-detail {
  font-size: 0.8rem;
  color: var(--ink-soft);
  line-height: 1.5;
  margin: 0;
  flex: 1;
}

.exec-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--border);
  padding-top: 8px;
  font-size: 0.72rem;
  font-family: 'Fira Code', 'Consolas', monospace;
  color: var(--ink-muted);
}

.exec-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Step 3: Reviewer specific styles */
.reviewer-audit-flow {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.audit-item {
  display: flex;
  gap: 14px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 14px 18px;
  box-shadow: var(--shadow-sm);
  transition: transform 0.3s;
}

[data-theme="dark"] .audit-item {
  background: rgba(255, 255, 255, 0.04);
}

.audit-item:hover {
  transform: translateX(2px);
}

.audit-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 0.85rem;
  flex-shrink: 0;
}

.audit-success .audit-badge {
  background: rgba(40, 200, 64, 0.12);
  color: #28c840;
}

.audit-success-final .audit-badge {
  background: rgba(123, 154, 255, 0.15);
  color: #7B9AFF;
}

.audit-warning .audit-badge {
  background: rgba(254, 188, 46, 0.15);
  color: #febc2e;
}

.audit-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.audit-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--ink);
}

.audit-desc {
  font-size: 0.8rem;
  color: var(--ink-soft);
  line-height: 1.5;
}

/* Correction Box */
.audit-correction-ui {
  margin-top: 8px;
  background: var(--canvas);
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.correction-tag {
  font-size: 0.68rem;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-weight: 700;
  color: var(--accent);
}

.diff-comparison {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.78rem;
  font-family: 'Fira Code', 'Consolas', monospace;
}

.diff-old {
  color: #ff5f57;
  text-decoration: line-through;
  background: rgba(255, 95, 87, 0.1);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.diff-arrow {
  color: var(--ink-muted);
}

.diff-new {
  color: #28c840;
  background: rgba(40, 200, 64, 0.1);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-weight: 700;
}

/* ── Scroll Reveal System ── */
.scroll-reveal {
  opacity: 0;
  transform: translateY(30px);
  transition: 
    opacity 0.8s var(--ease-out-quart), 
    transform 0.8s var(--ease-out-quart);
}

.scroll-reveal.scroll-revealed {
  opacity: 1;
  transform: translateY(0);
}

/* ── Tech Stack Section ── */
.tech-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  max-width: 700px;
  margin: 0 auto 56px;
}

.tech-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  transition: all var(--transition);
}

.tech-item:hover {
  border-color: var(--accent);
  box-shadow: var(--shadow-card);
}

.tech-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  border-radius: var(--radius-sm);
  color: var(--accent);
  flex-shrink: 0;
}

.tech-info {
  display: flex;
  flex-direction: column;
}

.tech-label {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ink);
}

.tech-desc {
  font-size: 0.75rem;
  color: var(--ink-muted);
}

/* ── Use Cases Section ── */
.use-cases-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.use-case-card {
  text-align: center;
  padding: 32px 20px;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  transition: all var(--transition);
}

.use-case-card:hover {
  transform: translateY(-6px);
  box-shadow: 
    var(--shadow-card-hover),
    0 12px 24px rgba(232, 93, 42, 0.05);
  border-color: var(--accent);
}

.use-case-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: var(--accent-soft);
  border-radius: var(--radius-lg);
  color: var(--accent);
  transition: all var(--transition);
}

.use-case-card:hover .use-case-icon {
  transform: scale(1.1) rotate(5deg);
  background: var(--accent);
  color: var(--surface);
}

.use-case-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 10px;
}

.use-case-desc {
  font-size: 0.88rem;
  color: var(--ink-soft);
  line-height: 1.6;
  margin: 0;
}

/* ── Vision Section ── */
.vision-section {
  padding: 80px 0;
  text-align: center;
}

.vision-content {
  max-width: 680px;
  margin: 0 auto;
  padding: 56px 40px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  background: var(--glass-bg);
  box-shadow: 0 8px 40px oklch(0.5 0.01 200 / 0.06);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  position: relative;
  overflow: hidden;
  z-index: 1;
}

.vision-content::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, var(--accent-soft) 0%, transparent 60%);
  opacity: 0.25;
  pointer-events: none;
  z-index: -1;
  animation: rotateBg 25s linear infinite;
}

@keyframes rotateBg {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.vision-title {
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 800;
  color: var(--ink);
  margin: 16px 0 24px;
  line-height: 1.3;
}

.vision-desc {
  font-size: 1.1rem;
  color: var(--ink-soft);
  line-height: 2;
  max-width: 600px;
  margin: 0 auto 40px;
}

.vision-values {
  display: flex;
  justify-content: center;
  gap: 48px;
}

.value-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.value-icon {
  color: var(--accent);
}

.value-text {
  font-size: 1rem;
  font-weight: 600;
  color: var(--ink);
}

/* ── Contact Section ── */
.contact-content {
  text-align: center;
  max-width: 500px;
  margin: 0 auto;
}

.contact-links {
  margin-top: 32px;
}

.contact-item {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 16px 32px;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  text-decoration: none;
  color: var(--ink);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  transition: all var(--transition);
}

.contact-item:hover {
  border-color: var(--accent);
  box-shadow: var(--shadow-card);
}

.contact-icon {
  color: var(--accent);
}

.contact-label {
  font-size: 0.85rem;
  color: var(--ink-muted);
}

.contact-value {
  font-size: 1rem;
  font-weight: 600;
  color: var(--accent);
}

/* ── CTA Section ── */
.cta-section {
  padding: 80px 0;
  text-align: center;
}

.cta-content {
  max-width: 520px;
  margin: 0 auto;
  padding: 48px 40px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  background: var(--glass-bg);
  box-shadow: 
    0 20px 50px rgba(0, 0, 0, 0.08),
    0 0 30px var(--accent-soft);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
}

.cta-code {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: #181825;
  border-radius: var(--radius-full);
  margin-bottom: 24px;
  font-family: 'Fira Code', 'Consolas', monospace;
}

.cta-code-prompt {
  color: #89b4fa;
}

.cta-code-text {
  color: #a6e3a1;
}

.cta-title {
  font-size: clamp(1.6rem, 4vw, 2.2rem);
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 12px;
}

.cta-desc {
  font-size: 1.05rem;
  color: var(--ink-soft);
  margin: 0 0 32px;
}

/* ── Logged-In Styles ── */
.hero-section {
  padding: 140px 0 64px;
}

.hero-content {
  max-width: 600px;
}

.hero-kicker {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent);
  margin-bottom: 20px;
}

.hero-title-user {
  font-size: clamp(2.4rem, 5vw, 3.6rem);
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.025em;
  color: var(--ink);
  margin-bottom: 20px;
}

.hero-desc-user {
  font-size: 1.1rem;
  font-weight: 400;
  line-height: 1.8;
  color: var(--ink-soft);
  margin-bottom: 36px;
  max-width: 480px;
}

.hero-container {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 clamp(16px, 4vw, 32px);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  align-items: center;
}

/* ── Hero Visual Decorations ── */
.hero-visual {
  position: relative;
  height: 400px;
}

.visual-card {
  position: absolute;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 20px;
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  animation: floatCard 6s ease-in-out infinite;
}

.visual-card--search {
  top: 20px;
  right: 20px;
  width: 290px;
  padding: 0;
  overflow: hidden;
}

.card-header-mini {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.12);
  border-bottom: 1px solid var(--border);
}

.mini-title {
  flex: 1;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.65rem;
  color: var(--ink-muted);
  text-align: left;
}

.search-content-mini {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.search-input-mock {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.15);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 6px 10px;
}

.search-icon {
  font-size: 0.8rem;
  opacity: 0.6;
}

.search-text {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.72rem;
  color: var(--accent);
  font-weight: 600;
}

.search-results-mock {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-item-mock {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.74rem;
  color: var(--ink-soft);
  text-align: left;
}

.result-dot {
  color: var(--accent);
  font-size: 0.8rem;
}

.result-name {
  flex: 1;
  font-weight: 500;
}

.result-relevance {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.65rem;
  color: var(--warm);
  font-weight: 700;
  background: var(--accent-soft);
  padding: 1px 4px;
  border-radius: 3px;
}

.visual-card--note {
  bottom: 40px;
  left: 0;
  width: 220px;
  animation-delay: -2s;
}

.visual-card--note .note-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.visual-card--note .note-icon {
  width: 28px;
  height: 28px;
  background: var(--accent-soft);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
}

.visual-card--note .note-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--ink);
}

.visual-card--note .note-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.note-line {
  height: 6px;
  background: var(--canvas-deep);
  border-radius: 3px;
}

.note-line:nth-child(1) {
  width: 100%;
}
.note-line:nth-child(2) {
  width: 85%;
}
.note-line:nth-child(3) {
  width: 92%;
}
.note-line:nth-child(4) {
  width: 60%;
}

.visual-card--tag {
  top: 50%;
  right: -20px;
  transform: translateY(-50%);
  padding: 12px 18px;
  animation: floatCardTag 6s ease-in-out infinite;
  animation-delay: -4s;
}

.visual-card--tag .tag-icon {
  font-size: 1.5rem;
  margin-bottom: 8px;
}

.visual-card--tag .tag-text {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--ink);
}

.visual-card--tag .tag-sub {
  font-size: 0.72rem;
  color: var(--ink-muted);
  margin-top: 2px;
}

.visual-dot {
  position: absolute;
  border-radius: 50%;
  background: var(--accent-soft);
}

.visual-dot--1 {
  width: 80px;
  height: 80px;
  top: -20px;
  right: 120px;
  opacity: 0.5;
  animation: floatDot 8s ease-in-out infinite;
}

.visual-dot--2 {
  width: 40px;
  height: 40px;
  bottom: 80px;
  right: 60px;
  opacity: 0.3;
  animation: floatDot 6s ease-in-out infinite -3s;
}

.visual-dot--3 {
  width: 24px;
  height: 24px;
  top: 60px;
  left: 80px;
  background: var(--tag-bg);
  opacity: 0.6;
  animation: floatDot 7s ease-in-out infinite -1s;
}

@keyframes floatCard {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes floatCardTag {
  0%,
  100% {
    transform: translateY(-50%) translateX(0);
  }
  50% {
    transform: translateY(-50%) translateX(-8px);
  }
}

@keyframes floatDot {
  0%,
  100% {
    transform: translate(0, 0);
  }
  33% {
    transform: translate(10px, -15px);
  }
  66% {
    transform: translate(-8px, 10px);
  }
}

.section-header-logged {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.see-all {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--accent);
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all var(--transition);
}

.see-all:hover {
  gap: 8px;
}
.see-all span {
  transition: transform var(--transition);
}
.see-all:hover span {
  transform: translateX(3px);
}

.content-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 32px;
  align-items: flex-start;
}

.articles-grid {
  columns: 2;
  column-gap: 20px;
}

.articles-grid > :deep(*) {
  break-inside: avoid;
  margin-bottom: 20px;
}

.empty-state {
  column-span: all;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  padding: 32px 24px;
  text-align: center;
  background: var(--surface);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
}

.empty-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: var(--ink);
}
.empty-desc {
  margin: 8px 0 16px;
  color: var(--ink-muted);
  font-size: 15px;
}

/* ── Responsive ── */
@media (max-width: 1024px) {
  .hero-grid {
    grid-template-columns: 1fr;
    gap: 40px;
    padding: 100px 0 60px;
    text-align: center;
  }
  .hero-text-content {
    align-items: center;
  }
  .hero-title {
    text-align: center;
  }
  .hero-tagline {
    text-align: center;
  }
  .hero-desc {
    text-align: center;
    margin-left: auto;
    margin-right: auto;
  }
  .hero-actions {
    justify-content: center;
  }
  .hero-stats {
    justify-content: center;
    gap: 32px;
  }
  .bento-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .feature-card--large {
    grid-column: span 2;
  }
  .showcase-container {
    grid-template-columns: 1fr;
  }
  .use-cases-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .content-layout {
    grid-template-columns: 1fr;
  }
  .articles-grid {
    columns: 1;
  }
  .hero-container {
    grid-template-columns: 1fr;
  }
  .hero-visual {
    display: none;
  }
}

@media (max-width: 600px) {
  .bento-grid {
    grid-template-columns: 1fr;
  }
  .feature-card--large {
    grid-column: span 1;
  }
  .landing-hero {
    padding: 100px 0 60px;
  }
  .hero-title {
    font-size: 2.8rem;
  }
  .hero-stats {
    gap: 24px;
    flex-wrap: wrap;
  }
  .stat-value {
    font-size: 1.5rem;
  }
  .use-cases-grid {
    grid-template-columns: 1fr;
  }
  .vision-section {
    padding: 60px 0;
  }
  .vision-values {
    gap: 24px;
  }
  .hero-section {
    padding: 110px 0 40px;
  }
  .instrument-panel {
    max-width: 320px;
  }
}

/* Landing Page Primary Gradient Button overrides */
.landing-hero .btn-primary,
.cta-section .btn-primary {
  background: var(--accent-gradient) !important;
  color: #FDFBF5 !important;
  border: none !important;
  box-shadow: 0 4px 14px var(--accent-soft) !important;
}

.landing-hero .btn-primary:hover,
.cta-section .btn-primary:hover {
  filter: brightness(1.08) !important;
  box-shadow: 0 6px 20px var(--accent-soft) !important;
  transform: translateY(-2px) !important;
}
</style>
