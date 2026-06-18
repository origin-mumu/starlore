<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { getBlogStatsService } from '@/api/article'
import SideBar from '@/components/sideBar.vue'
import { Article } from '@/type/Article'
import DummyCard from '@/components/dummyCard.vue'
import { useUserStore } from '@/stores/user'
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
} from '@lucide/vue'

const userStore = useUserStore()

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
  },
  {
    icon: Activity,
    title: '全链路可观测性',
    desc: '自研 Tracing 体系，追踪每次 LLM 调用的 Token 消耗、路由耗时与 Prompt 演进。',
    tech: 'Trace · Observability · Bad Case Mining',
    link: '/echobot',
  },
  {
    icon: Database,
    title: 'RAG 知识检索',
    desc: '向量化知识库，语义检索你的所有星记，AI 基于你的知识回答问题。',
    tech: 'Embedding · Vector Store · Semantic Search',
    link: '/articles',
  },
  {
    icon: BrainCircuit,
    title: '多模态 AI 对话',
    desc: '支持文本、图片、语音多模态输入，DeepSeek & MiMo 大模型驱动，SSE 流式响应。',
    tech: 'SSE Stream · Vision API · Tool Calling',
    link: '/echobot',
  },
  {
    icon: Eye,
    title: 'VR 知识星图',
    desc: 'Three.js 驱动的 3D 可视化，将知识映射为星辰，在沉浸式星域中探索。',
    tech: 'WebGL · Three.js · Particle System',
    link: '/vr',
  },
  {
    icon: Workflow,
    title: '创意发散引擎',
    desc: 'AI 驱动的思维导图，从一个关键词发散出无限可能，辅助创意和决策。',
    tech: 'Graph Layout · AI Generation · Real-time',
    link: '/diverge',
  },
]

const stats = [
  { value: '3-Agent', label: '多智能体协作' },
  { value: '10+', label: 'Function 工具' },
  { value: 'Trace', label: '全链路追踪' },
  { value: '3D', label: '知识可视化' },
]

const data = ref()
const articles = ref<Article[]>()

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    articles.value = demoArticles
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
        <div class="container">
          <div class="hero-terminal fade-in-up">
            <div class="terminal-bar">
              <span class="terminal-dot terminal-dot--red"></span>
              <span class="terminal-dot terminal-dot--yellow"></span>
              <span class="terminal-dot terminal-dot--green"></span>
              <span class="terminal-title">starlore.sh</span>
            </div>
            <div class="terminal-body">
              <div class="terminal-line">
                <span class="terminal-prompt">$</span>
                <span class="terminal-cmd">starlore init --universe</span>
              </div>
              <div class="terminal-line terminal-output">
                <span class="terminal-success">✓</span> Multi-Agent Graph compiled
              </div>
              <div class="terminal-line terminal-output">
                <span class="terminal-success">✓</span> Trace observability enabled
              </div>
              <div class="terminal-line terminal-output">
                <span class="terminal-success">✓</span> Vector store connected
              </div>
              <div class="terminal-line terminal-output">
                <span class="terminal-success">✓</span> VR Renderer ready
              </div>
              <div class="terminal-line">
                <span class="terminal-prompt">$</span>
                <span class="terminal-cursor">_</span>
              </div>
            </div>
          </div>

          <h1 class="hero-title fade-in-up" style="animation-delay: 0.2s">
            <span class="hero-title-accent">Star</span>lore
          </h1>
          <p class="hero-tagline fade-in-up" style="animation-delay: 0.3s">
            AI-Powered Personal Knowledge Universe
          </p>
          <p class="hero-desc fade-in-up" style="animation-delay: 0.4s">
            融合 Multi-Agent 协作、全链路可观测性、RAG 知识检索、3D 可视化的智能知识系统。<br />
            Planner 规划 → Executor 执行 → Reviewer 审查，让 AI 真正理解你的知识。
          </p>
          <div class="hero-actions fade-in-up" style="animation-delay: 0.5s">
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
          <div class="hero-stats fade-in-up" style="animation-delay: 0.6s">
            <div v-for="stat in stats" :key="stat.label" class="stat-item">
              <span class="stat-value">{{ stat.value }}</span>
              <span class="stat-label">{{ stat.label }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section id="features" class="section-parchment">
        <div class="container">
          <div class="section-header fade-in-up">
            <span class="section-tag">CORE FEATURES</span>
            <h2 class="section-heading">核心能力</h2>
            <p class="section-subtitle">六大技术模块，构建智能知识系统</p>
          </div>
          <div class="features-grid features-grid-3">
            <div
              v-for="(feature, i) in features"
              :key="feature.title"
              class="feature-card fade-in-up"
              :style="{ animationDelay: `${0.1 + i * 0.1}s` }"
            >
              <div class="feature-header">
                <div class="feature-icon">
                  <component :is="feature.icon" :size="24" />
                </div>
                <span class="feature-tech">{{ feature.tech }}</span>
              </div>
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-desc">{{ feature.desc }}</p>
              <router-link :to="feature.link" class="feature-link">
                探索 <ArrowRight :size="14" />
              </router-link>
            </div>
          </div>
        </div>
      </section>

      <!-- Use Cases Section -->
      <section class="section-white">
        <div class="container">
          <div class="section-header fade-in-up">
            <span class="section-tag">USE CASES</span>
            <h2 class="section-heading">使用场景</h2>
            <p class="section-subtitle">适合不同人群的知识管理需求</p>
          </div>
          <div class="use-cases-grid">
            <div class="use-case-card fade-in-up" style="animation-delay: 0.1s">
              <div class="use-case-icon"><GraduationCap :size="36" /></div>
              <h3 class="use-case-title">学生学习</h3>
              <p class="use-case-desc">
                整理课程笔记、论文资料，用 AI 快速检索知识点，构建个人知识体系。
              </p>
            </div>
            <div class="use-case-card fade-in-up" style="animation-delay: 0.2s">
              <div class="use-case-icon"><Code :size="36" /></div>
              <h3 class="use-case-title">开发者</h3>
              <p class="use-case-desc">
                记录技术笔记、调试经验，用 RAG 检索历史问题，提升开发效率。
              </p>
            </div>
            <div class="use-case-card fade-in-up" style="animation-delay: 0.3s">
              <div class="use-case-icon"><Microscope :size="36" /></div>
              <h3 class="use-case-title">研究者</h3>
              <p class="use-case-desc">
                管理文献综述、实验数据，用 AI 辅助分析，发现知识间的关联。
              </p>
            </div>
            <div class="use-case-card fade-in-up" style="animation-delay: 0.4s">
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
          <div class="vision-content fade-in-up">
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
          <div class="contact-content fade-in-up">
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
          <div class="cta-content fade-in-up">
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

            <!-- 代码卡片 -->
            <div class="visual-card visual-card--code">
              <div class="code-line">
                <span class="code-num">1</span>
                <span><span class="code-keyword">const</span> starlore = {</span>
              </div>
              <div class="code-line">
                <span class="code-num">2</span>
                <span>&nbsp;&nbsp;name: <span class="code-string">"Starlore"</span>,</span>
              </div>
              <div class="code-line">
                <span class="code-num">3</span>
                <span>&nbsp;&nbsp;type: <span class="code-string">"knowledge"</span>,</span>
              </div>
              <div class="code-line">
                <span class="code-num">4</span>
                <span>&nbsp;&nbsp;<span class="code-comment">// 记录每一刻</span></span>
              </div>
              <div class="code-line">
                <span class="code-num">5</span>
                <span>&nbsp;&nbsp;create: () => <span class="code-string">"✨"</span></span>
              </div>
              <div class="code-line">
                <span class="code-num">6</span>
                <span>}</span>
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
.landing-hero {
  padding: 120px 0 80px;
  text-align: center;
}

/* Terminal */
.hero-terminal {
  max-width: 480px;
  margin: 0 auto 48px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: 0 20px 60px oklch(0.25 0.02 50 / 0.15);
  text-align: left;
}

.terminal-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #1e1e2e;
}

.terminal-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.terminal-dot--red {
  background: #ff5f57;
}
.terminal-dot--yellow {
  background: #febc2e;
}
.terminal-dot--green {
  background: #28c840;
}

.terminal-title {
  flex: 1;
  text-align: center;
  font-size: 0.75rem;
  color: #6c6c8a;
  font-family: 'Fira Code', 'Consolas', monospace;
}

.terminal-body {
  padding: 20px;
  background: #181825;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.85rem;
  line-height: 1.8;
}

.terminal-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.terminal-prompt {
  color: #89b4fa;
}

.terminal-cmd {
  color: #cdd6f4;
}

.terminal-output {
  color: #a6adc8;
  padding-left: 20px;
}

.terminal-success {
  color: #a6e3a1;
}

.terminal-cursor {
  color: #f5e0dc;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* Hero Text */
.hero-title {
  font-size: clamp(3.5rem, 10vw, 6rem);
  font-weight: 900;
  letter-spacing: -0.05em;
  color: var(--ink);
  margin: 0 0 16px;
  line-height: 1;
}

.hero-title-accent {
  background: linear-gradient(135deg, var(--accent), var(--warm));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-tagline {
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--accent);
  margin: 0 0 20px;
}

.hero-desc {
  font-size: 1.05rem;
  color: var(--ink-soft);
  max-width: 560px;
  margin: 0 auto 40px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 14px;
  justify-content: center;
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
  justify-content: center;
  gap: 48px;
  margin-top: 64px;
  padding-top: 48px;
  border-top: 1px solid var(--border);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
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

/* ── Features Section ── */
.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.features-grid-3 {
  grid-template-columns: repeat(3, 1fr);
}

.feature-card {
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
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--accent);
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
  background: var(--accent-soft);
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
}

.feature-desc {
  font-size: 0.92rem;
  color: var(--ink-soft);
  line-height: 1.7;
  margin: 0 0 24px;
  flex: 1;
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
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
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
  box-shadow: 0 8px 40px oklch(0.5 0.01 200 / 0.06);
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

.visual-card--code {
  top: 20px;
  right: 20px;
  width: 280px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.78rem;
  line-height: 1.8;
  color: var(--ink-soft);
}

.visual-card--code .code-line {
  display: flex;
  gap: 8px;
}

.visual-card--code .code-num {
  color: var(--ink-muted);
  user-select: none;
  min-width: 20px;
  text-align: right;
}

.visual-card--code .code-keyword {
  color: var(--accent);
}

.visual-card--code .code-string {
  color: #4a8c5c;
}

.visual-card--code .code-comment {
  color: var(--ink-muted);
  font-style: italic;
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
  .features-grid {
    grid-template-columns: 1fr;
  }
  .features-grid-3 {
    grid-template-columns: repeat(2, 1fr);
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
  .features-grid-3 {
    grid-template-columns: 1fr;
  }
  .landing-hero {
    padding: 100px 0 60px;
  }
  .hero-title {
    font-size: 3rem;
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
}
</style>
