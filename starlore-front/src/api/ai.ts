import request from '@/utils/request'

export type AiModelInfo = {
  id: string
  name: string
  configured: boolean
}

export type CharacterCard = {
  key: string
  name: string
  description: string
  systemPrompt: string
}

export type AiSessionRow = {
  id: number
  title: string
  characterKey: string
  modelId: string
  createdAt: string
  updatedAt: string
}

export type AiMessageRow = {
  id: number
  role: 'user' | 'assistant' | 'system'
  content: string
  agentTrace?: string
  createdAt: string
}

export async function getAiModels() {
  return (await request.get('/ai/models')) as {
    success?: boolean
    models: AiModelInfo[]
  }
}

export async function getCharacterCards() {
  return (await request.get('/ai/character-cards')) as {
    success: boolean
    cards: CharacterCard[]
  }
}

export async function listAiSessions() {
  return (await request.get('/ai/sessions')) as {
    success: boolean
    sessions: AiSessionRow[]
  }
}

export async function createAiSession(body: {
  title?: string
  characterKey?: string
  modelId?: string
}) {
  return (await request.post('/ai/sessions', body)) as {
    success: boolean
    session: AiSessionRow
  }
}

export async function updateAiSession(
  id: number,
  body: { title?: string; characterKey?: string; modelId?: string },
) {
  return (await request.patch(`/ai/sessions/${id}`, body)) as { success: boolean }
}

export async function deleteAiSession(id: number) {
  return (await request.delete(`/ai/sessions/${id}`)) as { success: boolean }
}

export async function getSessionMessages(id: number) {
  return (await request.get(`/ai/sessions/${id}/messages`)) as {
    success: boolean
    session: AiSessionRow
    messages: AiMessageRow[]
  }
}

export async function appendChatPair(
  sessionId: number,
  userContent: string,
  assistantContent: string,
  agentTrace?: string,
) {
  return (await request.post(`/ai/sessions/${sessionId}/append`, {
    userContent,
    assistantContent,
    agentTrace,
  })) as { success: boolean }
}

export function buildMultiAgentSseUrl(model: string): string {
  return `/api/ai/multi-agent-sse?model=${model}`
}

export async function getAgentMetrics() {
  return (await request.get('/ai/agent-metrics')) as {
    success: boolean
    metrics: Record<string, any>
  }
}

export async function getAgentBadCases(page = 1, size = 20) {
  return (await request.get(`/ai/agent-bad-cases?page=${page}&size=${size}`)) as {
    success: boolean
    data: any[]
    total: number
  }
}

export interface AiQuota {
  dailyLimit: number
  used: number
  remaining: number
  isAdmin: boolean
}

export async function getAiQuota() {
  return (await request.get('/ai/quota')) as {
    data: AiQuota
  }
}

export type McpToolInfo = {
  server: string
  name: string
  description: string
  inputSchema: Record<string, unknown>
}

export async function getMcpTools() {
  return (await request.get('/ai/mcp/tools')) as {
    success: boolean
    servers: string[]
    tools: McpToolInfo[]
    count: number
  }
}

export async function reindexKnowledgeBase() {
  return (await request.post('/ai/reindex')) as {
    success: boolean
    message: string
    count: number
  }
}

export type RagEvaluationScores = {
  faithfulness: number
  answerRelevance: number
  contextPrecision: number
  contextRecall: number
  overall: number
}

export type RagEvaluationResult = {
  success: boolean
  evaluator: string
  scores: RagEvaluationScores
  contexts: { articleId: number; title: string }[]
}

export async function evaluateRag(body: {
  question: string
  answer: string
  groundTruth?: string
  topK?: number
  articleIds?: number[]
}) {
  return (await request.post('/ai/rag/evaluate', body)) as RagEvaluationResult
}
