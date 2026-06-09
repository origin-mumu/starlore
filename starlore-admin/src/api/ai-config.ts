import request from '@/utils/request.js'

export function getAiConfigsService() {
  return request.get('/ai-config')
}

export function getAiConfigByIdService(id: number) {
  return request.get(`/ai-config/${id}`)
}

export function createAiConfigService(data = {}) {
  return request.post('/ai-config', data)
}

export function updateAiConfigService(id: number, data = {}) {
  return request.put(`/ai-config/${id}`, data)
}

export function deleteAiConfigService(id: number) {
  return request.delete(`/ai-config/${id}`)
}
