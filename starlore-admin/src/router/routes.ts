import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

/** 路由表：全局仅 AppLayout 渲染侧边栏外壳，业务页面经 router-view 插入 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/dashboard/DashboardView.vue'),
      },
      {
        path: 'admin/articles',
        name: 'articles',
        component: () => import('@/views/articles/ArticleListView.vue'),
      },
      {
        path: 'admin/articles/add',
        name: 'AddArticle',
        component: () => import('@/views/articles/ArticleFormView.vue'),
      },
      {
        path: 'admin/articles/:id',
        name: 'EditArticle',
        component: () => import('@/views/articles/ArticleFormView.vue'),
      },
      {
        path: 'admin/categories',
        name: 'categories',
        component: () => import('@/views/categories/CategoryListView.vue'),
      },
      {
        path: 'admin/users',
        name: 'users',
        component: () => import('@/views/users/UserListView.vue'),
      },
      {
        path: 'admin/ai-config',
        name: 'AiConfig',
        component: () => import('@/views/ai-config/AiConfigView.vue'),
      },
      {
        path: 'admin/login-logs',
        name: 'loginLogs',
        component: () => import('@/views/login-logs/LoginLogView.vue'),
      },
    ],
  },
]
