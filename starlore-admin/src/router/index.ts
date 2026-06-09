import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login.vue'),
    },
    {
      path: '/admin/articles',
      name: 'articles',
      component: () => import('../views/articles/list.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/categories',
      name: 'categories',
      component: () => import('../views/categories/list.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/articles/add',
      name: 'AddArticle',
      component: () => import('../views/articles/add.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/articles/:id',
      name: 'EditArticle',
      component: () => import('../views/articles/edit.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/ai-config',
      name: 'AiConfig',
      component: () => import('../views/ai-config/index.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/users',
      name: 'users',
      component: () => import('../views/users/list.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/login-logs',
      name: 'loginLogs',
      component: () => import('../views/login-logs/index.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true'
  const role = localStorage.getItem('admin_user_role')

  // 未登录 → 登录页
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
    return
  }

  // 已登录但不是管理员 → 清登录状态并跳转登录页
  if (to.meta.requiresAuth && role !== 'admin') {
    localStorage.removeItem('isLoggedIn')
    localStorage.removeItem('ro_blog_admin_token')
    localStorage.removeItem('admin_user_role')
    next('/login')
    return
  }

  next()
})

export default router
