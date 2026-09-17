import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { http } from '@/utils/request'
import { TOKEN_KEY, ROLE_KEY, USER_KEY, clearSession } from '@/utils/request'
import type { AdminUserVO, LoginPayload, LoginResultVO } from '@/types'

const parseStoredUser = (): AdminUserVO | null => {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as AdminUserVO) : null
  } catch {
    return null
  }
}

/** 登录态与会话凭据的唯一管理入口（仅 admin 角色可进入后台） */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<AdminUserVO | null>(parseStoredUser())
  /** 是否已完成过一次服务端会话校验（/auth/me） */
  const checked = ref(false)

  const isLoggedIn = computed(() => token.value !== '' && user.value?.role === 'admin')

  function setSession(result: LoginResultVO): void {
    token.value = result.token
    user.value = result.user
    checked.value = true
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(ROLE_KEY, result.user.role)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  }

  function logout(): void {
    clearSession()
    token.value = ''
    user.value = null
    checked.value = false
  }

  /** 用服务端 /auth/me 校验本地 token 是否仍有效，并刷新用户信息 */
  async function checkSession(): Promise<boolean> {
    if (!token.value) {
      checked.value = true
      return false
    }
    try {
      const res = await http.get<{ data: AdminUserVO }>('/auth/me')
      const me = (res as { data?: AdminUserVO })?.data ?? (res as unknown as AdminUserVO)
      if (me && me.role === 'admin') {
        user.value = me
        localStorage.setItem(ROLE_KEY, me.role)
        localStorage.setItem(USER_KEY, JSON.stringify(me))
        checked.value = true
        return true
      }
      // 有效登录但不是 admin：视为未授权
      logout()
      return false
    } catch {
      logout()
      return false
    }
  }

  async function login(payload: LoginPayload): Promise<LoginResultVO> {
    const res = await http.post<unknown>('/auth/login', payload)
    const body = res as { data?: LoginResultVO }
    const result = body?.data?.token ? body.data : (res as LoginResultVO)
    if (result.user.role !== 'admin') {
      throw new Error('该账号不是管理员，无法进入后台')
    }
    setSession(result)
    return result
  }

  return { token, user, checked, isLoggedIn, setSession, login, logout, checkSession }
})
