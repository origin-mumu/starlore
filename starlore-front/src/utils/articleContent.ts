import { marked } from 'marked'
import TurndownService from 'turndown'
import { sanitizeHtml } from './sanitize'

marked.use({ gfm: true, breaks: false })

const turndown = new TurndownService({
  headingStyle: 'atx',
  bulletListMarker: '-',
  codeBlockStyle: 'fenced',
})

export function isLegacyHtml(value: string): boolean {
  return /^\s*<(?:!doctype\s+html|html|body|article|section|div|p|h[1-6]|ul|ol|blockquote|pre|table|figure|img)\b/i.test(value)
}

export function articleContentToMarkdown(value: string): string {
  if (!value || !isLegacyHtml(value)) return value || ''
  return turndown.turndown(value)
}

export function renderArticleContent(value: string): string {
  if (!value) return ''
  const html = isLegacyHtml(value) ? value : (marked.parse(value) as string)
  return sanitizeHtml(html)
}
