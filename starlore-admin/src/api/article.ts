import { http } from '@/utils/request'
import type {
  ArticleListQuery,
  ArticleListVO,
  ArticlePayload,
  ArticleDetailVO,
  ArticleStatus,
  CategoryItem,
  CategoryPayload,
  BlogStatsVO,
} from '@/types'

/* ── 星域 (Category) ──────────────────────── */

export async function articleCategoryListService(): Promise<CategoryItem[]> {
  const res = await http.get<any>('/categories')
  return Array.isArray(res) ? res : (res?.data ?? [])
}

export function createCategoryService(data: CategoryPayload): Promise<void> {
  return http.post('/categories', data)
}

export function updateCategoryService(id: number, data: CategoryPayload): Promise<void> {
  return http.put(`/categories/${id}`, data)
}

export function deleteCategoryService(id: number): Promise<void> {
  return http.delete(`/categories/${id}`)
}

/* ── 星记 (Article) ───────────────────────── */

export function getAllArticlesService(query: ArticleListQuery): Promise<ArticleListVO> {
  return http.get('/articles', { params: query })
}

export async function getArticleByIdService(id: number): Promise<ArticleDetailVO> {
  const res = await http.get<any>(`/articles/${id}`)
  return (res?.data?.title !== undefined ? res.data : res) as ArticleDetailVO
}

export function createArticleService(data: ArticlePayload): Promise<void> {
  return http.post('/articles', data)
}

export function updateArticleService(id: number, data: ArticlePayload): Promise<void> {
  return http.put(`/articles/${id}`, data)
}

export function deleteArticleService(id: number): Promise<void> {
  return http.delete(`/articles/${id}`)
}

export function toggleArticleStatusService(id: number, status: ArticleStatus): Promise<void> {
  return http.patch(`/articles/${id}/status`, { status })
}

/* ── 统计 ─────────────────────────────────── */

export async function getBlogStatsService(): Promise<BlogStatsVO> {
  const res = await http.get<any>('/articles/stats/summary')
  return (res?.data?.totalArticles !== undefined ? res.data : res) as BlogStatsVO
}
