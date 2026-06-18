import request from '@/utils/request'

// ============ 分类 API ============

export function getCategoriesService() {
  return request.get('/categories')
}

export function getPublicCategoriesService() {
  return request.get('/public/categories')
}


export function createCategoryService(data: { name: string; description?: string; color?: string }) {
  return request.post('/categories', data)
}

export function updateCategoryService(id: number, data: { name?: string; description?: string; color?: string }) {
  return request.put(`/categories/${id}`, data)
}

export function deleteCategoryService(id: number) {
  return request.delete(`/categories/${id}`)
}

// ============ 文章 API ============

export function getAllArticlesService(params = {}) {
  return request.get('/articles', { params })
}

export function getPublicArticlesService(params = {}) {
  return request.get('/public/articles', { params })
}

export function getArticleByIdService(id: number) {
  return request.get(`/articles/${id}`)
}

export function getPublicArticleByIdService(id: number) {
  return request.get(`/public/articles/${id}`)
}


export interface CreateArticleData {
  title: string
  content: string
  description?: string
  category?: string
  tags?: string[]
  coverImage?: string
  status?: string
  is_public?: boolean
}

export function createArticleService(data: CreateArticleData) {
  return request.post('/articles', data)
}

export function updateArticleService(id: number, data: Partial<CreateArticleData>) {
  return request.put(`/articles/${id}`, data)
}

export function deleteArticleService(id: number) {
  return request.delete(`/articles/${id}`)
}

export function getBlogStatsService() {
  return request.get('/articles/stats/summary')
}

export function getPublicBlogStatsService() {
  return request.get('/public/stats')
}


export function uploadImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<never, { data: { url: string }; message: string }>('/upload/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
