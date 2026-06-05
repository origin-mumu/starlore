import request from '@/utils/request'

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
  createdAt: string
  updatedAt: string
}

// 获取用户列表
export function getUserListService() {
  return request.get('/admin/users')
}

// 获取单个用户
export function getUserByIdService(id: number) {
  return request.get(`/admin/users/${id}`)
}

// 更新用户
export function updateUserService(id: number, data: Partial<UserItem>) {
  return request.put(`/admin/users/${id}`, data)
}

// 删除用户
export function deleteUserService(id: number) {
  return request.delete(`/admin/users/${id}`)
}

// 重建语义搜索索引
export function reindexService(userId: number) {
  return request.post(`/ai/reindex?targetUserId=${userId}`)
}
