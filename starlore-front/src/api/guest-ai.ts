import request from '@/utils/request'

export type GuestDivergeResponse = {
  pairs: { en: string; zh: string }[]
  remaining: number
}

export type GuestChatResponse = {
  content: string
  reasoningContent?: string
  remaining: number
}

export type GuestQuotaResponse = {
  limit: number
  remaining: number
}

export async function guestDiverge(word: string): Promise<GuestDivergeResponse> {
  return (await request.post('/public/ai/diverge', { word })) as GuestDivergeResponse
}

export async function guestChat(
  message: string,
  history: { role: 'user' | 'assistant'; content: string }[],
  character: string
): Promise<GuestChatResponse> {
  return (await request.post('/public/ai/guest-chat', { message, history, character })) as GuestChatResponse
}

export async function getGuestQuota(): Promise<GuestQuotaResponse> {
  return (await request.get('/public/ai/guest-quota')) as GuestQuotaResponse
}
