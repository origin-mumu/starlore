import request from '@/utils/request.js'

// 文章分类列表查询
export function articleCategoryListService() {
  return request.get('/categories')
}

export function createCategoryService(data={}) {
  return request.post('/categories', data)
}
export function updateCategoryService(id: number, data={}) {
  return request.put(`/categories/${id}`, data)
}
export function deleteCategoryService(id: number) {
  return request.delete(`/categories/${id}`)
}

// 获取所有文章（支持分页和搜索）
export function getAllArticlesService(params = {}) {
  return request.get('/articles', { params })
}

// 获取文章详情
export function getArticleByIdService(id: number) {
  return request.get(`/articles/${id}`)
}

// 创建新文章
export function createArticleService(data={}) {
  return request.post('/articles', data)
}

// 更新文章
export function updateArticleService(id: number, data={}) {
  return request.put(`/articles/${id}`, data)
}

// 删除文章
export function deleteArticleService(id : number) {
  return request.delete(`/articles/${id}`)
}

// 获取知识库统计信息
export function getBlogStatsService() {
  return request.get('/articles/stats/summary')
}

// 切换文章状态（发布/草稿）
export function toggleArticleStatusService(id : number, status: string) {
  return request.patch(`/articles/${id}/status`, { status })
}

// 批量删除文章
export function batchDeleteArticlesService(ids : number[]) {
  return request.post('/articles/batch-delete', { ids })
}

// 上传图片
export function uploadImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/upload/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}


