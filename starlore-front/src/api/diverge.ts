import request from '@/utils/request'

export const divergeWord = (word: string) => {
  return request.post('/ai/diverge', { word }) as Promise<{ pairs: { en: string; zh: string }[] }>
}
