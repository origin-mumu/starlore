import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function setupRouterGuards(router: Router): void {
  router.beforeEach((to) => {
    if (!to.meta.requiresAuth) return true
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return '/login'
    return true
  })
}
