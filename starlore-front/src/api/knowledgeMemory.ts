import request from '@/utils/request'

export interface KnowledgeArticleItem {
  articleId: number
  title: string
  description?: string
  category?: string
  cardCount: number
  generated: boolean
  updatedAt?: string
}

export interface KnowledgeGroupItem extends KnowledgeArticleItem {
  dueCount: number
  masteredCount: number
}

export interface KnowledgeCardItem {
  id: number
  articleId: number
  articleTitle: string
  question: string
  answerMarkdown: string
  sourceLineStart: number
  sourceLineEnd: number
  sourceHash: string
  difficulty: 'basic' | 'core' | 'advanced'
  sortOrder: number
  status: string
  reviewStage: string
  intervalDays: number
  reviewCount: number
  lapseCount: number
  nextReviewAt?: string
  lastReviewedAt?: string
}

export interface GenerateKnowledgeResult {
  generated: { articleId: number; title: string; cardCount: number }[]
  errors: { articleId: number; title?: string; message: string }[]
}

export function getKnowledgeArticles() {
  return request.get<never, { data: KnowledgeArticleItem[] }>('/knowledge-memory/articles')
}

export function getKnowledgeGroups() {
  return request.get<never, { data: KnowledgeGroupItem[] }>('/knowledge-memory/groups')
}

export function generateKnowledgeCards(data: {
  articleIds: number[]
  model?: string
  maxCardsPerArticle?: number
}) {
  return request.post<never, { success: boolean; message: string; data: GenerateKnowledgeResult }>(
    '/knowledge-memory/generate',
    data,
  )
}

export function getKnowledgeCards(articleId: number, dueOnly = false) {
  return request.get<never, { data: KnowledgeCardItem[] }>(
    `/knowledge-memory/articles/${articleId}/cards`,
    { params: { dueOnly } },
  )
}

export function reviewKnowledgeCard(
  cardId: number,
  data: { rating: 'again' | 'hard' | 'good'; durationMs?: number },
) {
  return request.post<never, { success: boolean; data: KnowledgeCardItem }>(
    `/knowledge-memory/cards/${cardId}/review`,
    data,
  )
}

export function deleteKnowledgeCard(cardId: number) {
  return request.delete(`/knowledge-memory/cards/${cardId}`)
}
