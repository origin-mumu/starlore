const TOKEN_KEY = 'ro_blog_token'

export function getAuthToken(): string {
  const current = sessionStorage.getItem(TOKEN_KEY)
  if (current) return current

  const legacy = localStorage.getItem(TOKEN_KEY)
  if (legacy) {
    sessionStorage.setItem(TOKEN_KEY, legacy)
    localStorage.removeItem(TOKEN_KEY)
  }
  return legacy || ''
}

export function setAuthToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
  localStorage.removeItem(TOKEN_KEY)
}

export function clearAuthToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_KEY)
}
