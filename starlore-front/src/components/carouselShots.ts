/**
 * Starlore 5 Core Architecture Features for 3D Carousel
 * Clean, high-contrast, theme-adaptive metadata (Bright in Light Mode, Deep in Dark Mode)
 */

export interface DeliverableCardItem {
  name: string
  tag: string
  title: string
  subtitle: string
  tech: string[]
  accent: string
  iconName: string
  category: string
  highlight: string
}

export const DELIVERABLE_CARDS: DeliverableCardItem[] = [
  {
    name: 'agent',
    tag: 'MULTI-AGENT HARNESS',
    title: 'Multi-Agent 协作',
    subtitle: 'Planner-Executor-Reviewer 三角色协同，自主规划复杂任务、并行调用工具并实现自纠偏。',
    tech: ['LangGraph', 'State Machine', 'Auto-Retry'],
    accent: '#DE4331',
    iconName: 'GitBranch',
    category: '多智能体协同',
    highlight: '3 角色状态机',
  },
  {
    name: 'vr',
    tag: '3D VR GALAXY',
    title: 'VR 知识星图',
    subtitle: 'Three.js 驱动的全息 3D 可视化，将离散笔记映射为引力星辰，在沉浸式星域中漫游探索。',
    tech: ['WebGL', 'Three.js', 'Particle Cluster'],
    accent: '#0284C7',
    iconName: 'Eye',
    category: '全息空间漫游',
    highlight: '引力星系聚类',
  },
  {
    name: 'ppt',
    tag: 'CONTENT HARNESS',
    title: 'Cloud 生产力交付',
    subtitle: '智能提炼长程知识碎片，一键排版生成结构化演讲幻灯片、学术文档与高保真矢量简历。',
    tech: ['PPTX Engine', 'Word Docx', 'Vector PDF'],
    accent: '#8B5CF6',
    iconName: 'Presentation',
    category: '端到端产出',
    highlight: '全矢量交付物',
  },
  {
    name: 'rag',
    tag: 'VECTOR & RAG',
    title: 'RAG 知识检索',
    subtitle: '向量化知识库与混合相似度检索，语义穿透你的所有星记，AI 基于你的个人知识权威应答。',
    tech: ['Embedding', 'Vector Store', 'Semantic Search'],
    accent: '#D97706',
    iconName: 'Database',
    category: '精准语义检索',
    highlight: '高维向量索引',
  },
  {
    name: 'trace',
    tag: 'OBSERVABILITY & TRACE',
    title: '全链路可观测性',
    subtitle: '搭建深度 Tracing 体系，毫秒级追踪每次 LLM 调用的 Token 消耗、路由耗时与 Prompt 演进。',
    tech: ['OpenTelemetry', 'Token Telemetry', 'Bad Case'],
    accent: '#E11D48',
    iconName: 'Activity',
    category: '全链路遥测',
    highlight: 'Token / 耗时审计',
  },
]
