export interface Article {
  id: number
  title: string
  description: string
  createdAt: string
  category: string
  cover_image?: string
  tags?: string[]
}
