const TOKEN_KEY = 'ro_blog_token'

export function getAuthToken(): string {
  const current = localStorage.getItem(TOKEN_KEY)
  if (current) return current

  // Migrate tokens created by the previous tab-scoped login implementation.
  const legacy = sessionStorage.getItem(TOKEN_KEY)
  if (legacy) {
    localStorage.setItem(TOKEN_KEY, legacy)
    sessionStorage.removeItem(TOKEN_KEY)
  }
  return legacy || ''
}

export function setAuthToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  sessionStorage.removeItem(TOKEN_KEY)
}

export function clearAuthToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_KEY)
}
