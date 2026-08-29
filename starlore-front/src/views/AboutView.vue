<script setup lang="ts">
import {
  GitFork,
  ExternalLink,
  Code,
  Server,
  Wrench,
  MessageCircle,
  GitBranch,
  Database,
  Cpu,
  Globe,
  ArrowRight,
  Layers,
} from '@lucide/vue'

const projects = [
  {
    name: 'Starlore',
    desc: 'AI 驱动的个人知识宇宙。融合 Multi-Agent 协作、全链路可观测性、RAG 语义检索、3D 可视化的智能知识管理系统。',
    tech: ['Vue 3', 'Spring Boot', 'LangGraph', 'Three.js', 'Flutter'],
    url: 'https://github.com/origin-mumu/starlore',
  },
  {
    name: 'RoUI',
    desc: '基于 Vue 3 开发的轻量级、可定制的 UI 组件库，帮助开发者快速构建美观的 Web 应用界面。',
    tech: ['Vue 3', 'TypeScript', 'Vite'],
    url: 'https://github.com/origin-mumu/roui',
  },
  {
    name: 'AIGC Platform',
    desc: '集成多种大语言模型的智能对话平台，支持上下文记忆与多模态交互，提供流畅的 AI 辅助创作体验。',
    tech: ['Vue 3', 'Spring AI', 'SSE', 'Multi-Model'],
    url: 'https://github.com/origin-mumu/ro-ai-chat',
  },
  {
    name: '星图运营中台',
    desc: '基于 Vue 3 + Element Plus 的现代化运营中台系统，提供完整的权限管理、数据可视化、用户管理等功能。',
    tech: ['Vue 3', 'Element Plus', 'ECharts', 'RBAC'],
    url: 'https://github.com/origin-mumu/NexusAdmin',
  },
  {
    name: '练习题生成与批改系统',
    desc: '智能化的练习题生成与自动批改系统，支持多种题型、难度分级，为师生提供高效的学习评估工具。',
    tech: ['Java', 'Spring Boot', 'MySQL'],
    url: 'https://gitee.com/software-engineering-2023183/202318314-jia-xinke',
  },
]

const techStack = [
  {
    icon: Code,
    category: '前端',
    items: [
      'Vue 3 + TypeScript + Vite',
      'Composition API / Pinia / Vue Router',
      'Flex / Grid 布局 / CSS 变量主题系统',
      'Three.js 3D 可视化',
    ],
  },
  {
    icon: Server,
    category: '后端',
    items: [
      'Spring Boot 3 + MyBatis-Plus',
      'JWT 鉴权 / 拦截器 / 统一异常处理',
      'Spring AI + Function Calling + SSE',
      'MySQL 数据库设计与 SQL 优化',
    ],
  },
  {
    icon: Cpu,
    category: 'AI 集成',
    items: [
      'Multi-Agent 协作（Planner-Executor-Reviewer）',
      '全链路可观测性追踪',
      'RAG 语义检索 + 向量存储',
      '多模态对话（文本 / 图片 / 语音）',
    ],
  },
  {
    icon: Wrench,
    category: '工具与协作',
    items: [
      'Git 版本管理 + GitHub Actions',
      'Vite 构建 / Maven 项目管理',
      'Postman / ApiFox 接口调试',
      '前后端联调与 API 文档',
    ],
  },
]

const githubStats = [
  { value: '4+', label: '开源项目' },
  { value: 'Vue 3', label: '主力前端框架' },
  { value: 'Spring', label: '主力后端框架' },
]
</script>

<template>
  <div class="page-container about-page">
    <!-- Hero -->
    <section class="about-hero fade-in-up">
      <div class="container">
        <span class="about-kicker">ABOUT ME</span>
        <h1 class="about-title">热爱创造的<span class="accent">全栈开发者</span></h1>
        <p class="about-motto">「未完成的课题会重复出现，直到你做出改变。」</p>
        <p class="about-desc">
          专注于 Vue 3 + Spring Boot 全栈开发，热爱探索 AI 与知识管理的交叉领域。<br />
          从组件库到 AI Agent，从 3D 可视化到移动端，用代码构建每一个想法。
        </p>
        <div class="about-actions">
          <a href="https://github.com/origin-mumu" target="_blank" class="btn-primary btn-lg">
            <GitFork :size="18" />
            GitHub 主页
          </a>
        </div>
      </div>
    </section>

    <!-- Tech Stack -->
    <section class="section-parchment">
      <div class="container">
        <div class="section-header fade-in-up">
          <span class="section-tag">TECH STACK</span>
          <h2 class="section-heading">技术栈</h2>
        </div>
        <div class="tech-grid fade-in-up" style="animation-delay: 0.15s">
          <div v-for="tech in techStack" :key="tech.category" class="tech-card starlore-spotlight">
            <div class="tech-card-header">
              <div class="tech-card-icon">
                <component :is="tech.icon" :size="20" />
              </div>
              <h3 class="tech-card-title">{{ tech.category }}</h3>
            </div>
            <ul class="tech-card-list">
              <li v-for="item in tech.items" :key="item">{{ item }}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- Projects -->
    <section>
      <div class="container">
        <div class="section-header fade-in-up">
          <span class="section-tag">PROJECTS</span>
          <h2 class="section-heading">项目经历</h2>
          <p class="section-subtitle">独立开发与团队协作的实践积累</p>
        </div>
        <div class="projects-grid">
          <div
            v-for="(project, i) in projects"
            :key="project.name"
            class="project-card starlore-spotlight fade-in-up"
            :style="{ animationDelay: `${0.1 + i * 0.08}s` }"
          >
            <div class="project-card-top">
              <Layers :size="20" class="project-icon" />
              <h3 class="project-name">{{ project.name }}</h3>
            </div>
            <p class="project-desc">{{ project.desc }}</p>
            <div class="project-tags">
              <span v-for="tag in project.tech" :key="tag" class="project-tag">{{ tag }}</span>
            </div>
            <a v-if="project.url" :href="project.url" target="_blank" class="project-link">
              <GitFork :size="14" />
              查看源码
              <ExternalLink :size="12" />
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- Open Source -->
    <section class="section-parchment">
      <div class="container">
        <div class="section-header fade-in-up">
          <span class="section-tag">OPEN SOURCE</span>
          <h2 class="section-heading">开源贡献</h2>
        </div>
        <div class="opensource-content fade-in-up" style="animation-delay: 0.1s">
          <div class="opensource-card starlore-spotlight">
            <div class="opensource-header">
              <GitBranch :size="20" class="opensource-icon" />
              <h3>GitHub 活跃仓库</h3>
            </div>
            <p class="opensource-desc">所有项目均开源在 GitHub，欢迎 Star、Issue 和 PR。</p>
            <div class="github-stats">
              <div v-for="stat in githubStats" :key="stat.label" class="github-stat">
                <span class="github-stat-value">{{ stat.value }}</span>
                <span class="github-stat-label">{{ stat.label }}</span>
              </div>
            </div>
            <a
              href="https://github.com/origin-mumu?tab=repositories"
              target="_blank"
              class="btn-primary"
            >
              <GitFork :size="16" />
              查看全部仓库
              <ArrowRight :size="14" />
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- Contact -->
    <section>
      <div class="container">
        <div class="contact-block fade-in-up">
          <h2 class="contact-title">一起交流</h2>
          <p class="contact-desc">欢迎通过 GitHub 与我联系，期待技术交流与合作机会。</p>
          <a href="https://github.com/origin-mumu" target="_blank" class="btn-primary btn-lg">
            <GitFork :size="18" />
            GitHub
          </a>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped src="./AboutView.css"></style>
