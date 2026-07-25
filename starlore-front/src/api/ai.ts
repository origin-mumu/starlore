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

export type ArticleChunkItem = {
  id: number
  articleId: number
  articleTitle?: string
  articleCategory?: string
  chunkIndex: number
  content: string
  tokenCount: number
  isEnabled: number
}

export type ChunkPageResponse = {
  items: ArticleChunkItem[]
  pagination: {
    current: number
    total: number
    pages: number
    limit: number
  }
  categories: string[]
  articles?: { id: number; title: string; category?: string }[]
}

export async function getAllArticleChunks(params: {
  page?: number
  limit?: number
  category?: string
  articleId?: number
  search?: string
} = {}) {
  const queryParams = {
    ...params,
    article_id: params.articleId,
  }
  return (await request.get('/articles/chunks', { params: queryParams })) as {
    success: boolean
    message: string
    data: ChunkPageResponse
  }
}

export async function getArticleChunks(articleId: number) {
  return (await request.get(`/articles/${articleId}/chunks`)) as {
    success: boolean
    message: string
    data: ArticleChunkItem[]
  }
}

export async function updateChunk(chunkId: number, data: { content?: string; isEnabled?: number }) {
  return (await request.put(`/articles/chunks/${chunkId}`, data)) as {
    success: boolean
    message: string
  }
}

export async function reindexArticle(articleId: number) {
  return (await request.post(`/articles/${articleId}/reindex`)) as {
    success: boolean
    message: string
  }
}

export async function reindexAllArticles() {
  return (await request.post('/articles/reindex')) as {
    success: boolean
    message: string
  }
}

export type AgentConfigData = {
  id?: number
  userId?: number
  modelName: string
  similarityThreshold: number
  topK: number
  temperature: number
  enableRerank: number
}

export async function getAgentConfig() {
  return (await request.get('/ai/agent-config')) as {
    success: boolean
    message: string
    data: AgentConfigData
  }
}

export async function updateAgentConfig(data: Partial<AgentConfigData>) {
  return (await request.put('/ai/agent-config', data)) as {
    success: boolean
    message: string
  }
}

export async function submitMessageFeedback(
  messageId: number,
  data: { sessionId: number; rating: 'LIKE' | 'DISLIKE'; feedbackType?: string; comment?: string }
) {
  return (await request.post(`/ai/messages/${messageId}/feedback`, data)) as {
    success: boolean
    message: string
  }
}

export type KnowledgeDocumentRow = {
  id: number
  fileName: string
  fileType: string
  fileSize: number
  fileUrl: string
  extractedText: string
  status: string
  chunkCount: number
  createdAt: string
  updatedAt: string
}

export async function getKnowledgeDocuments() {
  return (await request.get('/ai/documents')) as {
    success: boolean
    documents: KnowledgeDocumentRow[]
  }
}

export async function uploadKnowledgeDocument(formData: FormData) {
  return (await request.post('/ai/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })) as {
    success: boolean
    document: KnowledgeDocumentRow
  }
}

export async function updateKnowledgeDocumentText(id: number, extractedText: string) {
  return (await request.put(`/ai/documents/${id}`, { extractedText })) as {
    success: boolean
  }
}

export async function deleteKnowledgeDocument(id: number) {
  return (await request.delete(`/ai/documents/${id}`)) as {
    success: boolean
  }
}

export async function getDocumentChunks(docId: number) {
  return (await request.get(`/ai/documents/${docId}/chunks`)) as {
    success: boolean
    message?: string
    data: ArticleChunkItem[]
  }
}

export async function parseFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return (await request.post('/ai/parse-file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })) as {
    success: boolean
    filename?: string
    text?: string
    error?: string
  }
}

export async function confirmKnowledgeDocument(data: {
  fileName: string
  fileType?: string
  fileSize?: number
  extractedText: string
}) {
  return (await request.post('/ai/documents/confirm', data)) as {
    success: boolean
    document?: KnowledgeDocumentRow
    message?: string
  }
}
