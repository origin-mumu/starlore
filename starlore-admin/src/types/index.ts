/**
 * 全局 TypeScript 类型契约 (DTO / VO)
 * 所有 API 请求与响应必须引用此处声明，严禁 any。
 */

/* ── 通用响应外壳 ─────────────────────────── */

export interface PageMeta {
  current: number
  total: number
  pages: number
}

/* ── 认证 ─────────────────────────────────── */

export interface LoginPayload {
  username: string
  password: string
}

export interface AdminUserVO {
  id: number
  username: string
  role: 'admin' | 'member' | 'user' | string
}

export interface LoginResultVO {
  token: string
  user: AdminUserVO
}

/* ── 星记 (Article) ───────────────────────── */

export type ArticleStatus = 'published' | 'draft'

export interface ArticleItem {
  id: number
  userId: number
  authorName: string
  title: string
  description: string
  category: string
  tags: string[]
  cover_image: string
  view_count: number
  status: ArticleStatus
  createdAt: string
  updatedAt: string
}

export interface ArticleListQuery {
  page: number
  limit: number
  search?: string
  category?: string
  status?: ArticleStatus | 'all'
  authorUserId?: number
}

export interface ArticleListVO {
  data: ArticleItem[]
  pagination: PageMeta
}

export interface ArticlePayload {
  title: string
  content: string
  category: string
  description: string
  status: ArticleStatus
}

export interface ArticleDetailVO {
  id: number
  title: string
  content: string
  category: string
  description: string
  status: ArticleStatus
}

/* ── 星域 (Category) ──────────────────────── */

export interface CategoryItem {
  id: number
  userId: number
  name: string
  description: string
  article_count: number
  createdAt: string
  updatedAt: string
}

export interface CategoryPayload {
  name: string
  description?: string
}

/* ── 首页统计 ─────────────────────────────── */

export interface PopularArticle {
  id: number
  title: string
  view_count: number
}

export interface PopularCategory {
  name: string
  article_count: number
}

export interface BlogStatsVO {
  totalArticles: number
  totalCategories: number
  totalViews: number
  popularArticles: PopularArticle[]
  popularCategories: PopularCategory[]
}

/* ── 登录日志 ─────────────────────────────── */

export interface LoginLogItem {
  id: number
  userId: number
  username: string
  ip: string
  country: string
  province: string
  city: string
  loginTime: string
}

export interface LoginLogQuery {
  page: number
  limit: number
  username?: string
  dateFrom?: string
  dateTo?: string
}

export interface LoginLogListVO {
  data: LoginLogItem[]
  total: number
  pages: number
}

/* ── 观星者 (User) ────────────────────────── */

export interface UserItem {
  id: number
  username: string
  nickname: string
  email: string | null
  avatar: string | null
  bio: string | null
  location: string | null
  website: string | null
  github: string | null
  role: 'admin' | 'member' | 'user' | string
  aiDailyLimit: number
  createdAt: string
  updatedAt: string
}

export interface UserPayload {
  nickname: string
  email: string
  bio: string
  location: string
  website: string
  github: string
  role: string
  aiDailyLimit: number
}

export interface ReindexResultVO {
  message?: string
}

/* ── AI 模型配置 ──────────────────────────── */

export interface AiConfigItem {
  id: number
  modelKey: string
  modelName: string
  apiUrl: string
  modelId: string
  apiKey: string
  enabled: boolean
  createdAt: string
}

export interface AiConfigPayload {
  modelKey: string
  modelName: string
  apiUrl: string
  modelId: string
  apiKey: string
  enabled: boolean
}
