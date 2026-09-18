/**
 * Harness 前端 API 模块与 SSE 流式客户端
 */

import request from '@/utils/request'
import { getAuthToken } from '@/utils/authToken'
import type {
  HarnessMessage,
  HarnessModelItem,
  HarnessSession,
  HarnessTodoItem,
  StreamEventPayload,
} from '@/views/harness/types'

export async function fetchHarnessModels(): Promise<HarnessModelItem[]> {
  const res = (await request.get('/harness/models')) as { items: HarnessModelItem[] }
  return res?.items || []
}

export async function fetchHarnessSessions(): Promise<HarnessSession[]> {
  const res = (await request.get('/harness/sessions')) as { items: HarnessSession[] }
  return res?.items || []
}

export async function createHarnessSession(
  title?: string,
  modelId?: string
): Promise<HarnessSession> {
  return (await request.post('/harness/sessions', {
    title: title || '新会话',
    model_id: modelId,
  })) as HarnessSession
}

export async function updateHarnessSession(
  sessionId: number,
  payload: { title?: string; pinned?: boolean; model_id?: string }
): Promise<HarnessSession> {
  return (await request.patch(`/harness/sessions/${sessionId}`, payload)) as HarnessSession
}

export async function deleteHarnessSession(sessionId: number): Promise<boolean> {
  await request.delete(`/harness/sessions/${sessionId}`)
  return true
}

export async function fetchHarnessMessages(sessionId: number): Promise<HarnessMessage[]> {
  const res = (await request.get(`/harness/sessions/${sessionId}/messages`)) as HarnessMessage[]
  return Array.isArray(res) ? res : []
}

export async function fetchHarnessTodos(sessionId: number): Promise<HarnessTodoItem[]> {
  const res = (await request.get(`/harness/sessions/${sessionId}/todo`)) as {
    success: boolean
    todos: HarnessTodoItem[]
  }
  return Array.isArray(res?.todos) ? res.todos : []
}

export async function streamHarnessChat(
  sessionId: number,
  payload: { message: string; model_id?: string; images?: string[] },
  onEvent: (event: StreamEventPayload) => void,
  signal?: AbortSignal
): Promise<void> {
  const token = getAuthToken()
  const response = await fetch(`/api/harness/sessions/${sessionId}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
    signal,
  })

  if (!response.ok) {
    throw new Error(`请求失败: ${response.status} ${response.statusText}`)
  }

  const reader = response.body?.getReader()
  if (!reader) throw new Error('流式读取器不可用')

  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() || '' // 保留未成行的半包

    for (const block of blocks) {
      const trimmed = block.trim()
      if (!trimmed) continue

      let eventType: string = 'message'
      let dataStr: string = ''

      for (const line of trimmed.split('\n')) {
        if (line.startsWith('event:')) {
          eventType = line.replace(/^event:\s*/, '').trim()
        } else if (line.startsWith('data:')) {
          dataStr = line.replace(/^data:\s*/, '').trim()
        }
      }

      if (dataStr) {
        try {
          const parsedData = JSON.parse(dataStr)
          onEvent({ event: eventType as any, data: parsedData })
        } catch {
          onEvent({ event: eventType as any, data: dataStr })
        }
      }
    }
  }
}
