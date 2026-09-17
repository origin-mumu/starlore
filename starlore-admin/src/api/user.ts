import { http } from '@/utils/request'
import type { UserItem, UserPayload, ReindexResultVO } from '@/types'

export async function getUserListService(): Promise<UserItem[]> {
  const res = await http.get<any>('/admin/users')
  return Array.isArray(res) ? res : (res?.data ?? [])
}

export async function getUserByIdService(id: number): Promise<UserItem> {
  const res = await http.get<any>(`/admin/users/${id}`)
  return (res?.data?.username !== undefined ? res.data : res) as UserItem
}

export function updateUserService(id: number, data: UserPayload): Promise<void> {
  return http.put(`/admin/users/${id}`, data)
}

export function deleteUserService(id: number): Promise<void> {
  return http.delete(`/admin/users/${id}`)
}

/** 重建指定用户的语义搜索索引 */
export function reindexService(userId: number): Promise<ReindexResultVO> {
  return http.post(`/ai/reindex?targetUserId=${userId}`)
}
