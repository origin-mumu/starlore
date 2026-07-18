import DOMPurify from 'dompurify'

export function sanitizeHtml(value: unknown): string {
  if (typeof value !== 'string') return ''
  return DOMPurify.sanitize(value, {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['style', 'script', 'iframe', 'object', 'embed'],
    FORBID_ATTR: ['style'],
  })
}
