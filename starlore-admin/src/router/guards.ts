import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 全局路由守卫：
 * - 首次导航先经服务端校验会话（防本地伪造 role / token 过期）
 * - 后台所有页面 requiresAdmin：仅 admin 角色可进入
 */
export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()

    if (!auth.checked) {
      await auth.checkSession()
    }

    const isAdminArea = to.matched.some((r) => r.meta.requiresAdmin)

    if (to.name === 'login') {
      return auth.isLoggedIn ? '/' : true
    }

    if (isAdminArea && !auth.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    // RBAC 预留：路由 meta.roles 声明允许的角色
    const roles = to.meta.roles as string[] | undefined
    if (roles && roles.length > 0 && !roles.includes(auth.user?.role ?? '')) {
      return auth.isLoggedIn ? '/' : { path: '/login', query: { redirect: to.fullPath } }
    }

    return true
  })
}
