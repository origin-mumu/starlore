import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { TOKEN_KEY, ROLE_KEY, LOGGED_IN_KEY, clearSession } from '@/utils/request'

/** 登录态与会话凭据的唯一管理入口 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const role = ref(localStorage.getItem(ROLE_KEY) ?? '')

  const isLoggedIn = computed(() => token.value !== '' && role.value === 'admin')

  function setSession(nextToken: string, nextRole: string): void {
    token.value = nextToken
    role.value = nextRole
    localStorage.setItem(TOKEN_KEY, nextToken)
    localStorage.setItem(ROLE_KEY, nextRole)
    localStorage.setItem(LOGGED_IN_KEY, 'true')
  }

  function logout(): void {
    clearSession()
    token.value = ''
    role.value = ''
  }

  return { token, role, isLoggedIn, setSession, logout }
})
