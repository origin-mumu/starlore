import { http } from '@/utils/request'
import type { AiConfigItem, AiConfigPayload } from '@/types'

export async function getAiConfigsService(): Promise<AiConfigItem[]> {
  const res = await http.get<any>('/ai-config')
  return Array.isArray(res) ? res : (res?.data ?? [])
}

export async function getAiConfigByIdService(id: number): Promise<AiConfigItem> {
  const res = await http.get<any>(`/ai-config/${id}`)
  return (res?.data?.modelKey !== undefined ? res.data : res) as AiConfigItem
}

export function createAiConfigService(data: AiConfigPayload): Promise<void> {
  return http.post('/ai-config', data)
}

export function updateAiConfigService(id: number, data: Partial<AiConfigPayload>): Promise<void> {
  return http.put(`/ai-config/${id}`, data)
}

export function deleteAiConfigService(id: number): Promise<void> {
  return http.delete(`/ai-config/${id}`)
}
