import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

/** 路由表：全局仅 AppLayout 渲染侧边栏外壳，业务页面经 router-view 插入 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true, requiresAdmin: true, roles: ['admin'] },
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '总览' },
      },
      {
        path: 'admin/ai-usage',
        name: 'aiUsage',
        component: () => import('@/views/ai-usage/AiUsageView.vue'),
        meta: { title: 'AI 用量' },
      },
      {
        path: 'admin/articles',
        name: 'articles',
        component: () => import('@/views/articles/ArticleListView.vue'),
        meta: { title: '星记管理' },
      },
      {
        path: 'admin/articles/add',
        name: 'AddArticle',
        component: () => import('@/views/articles/ArticleFormView.vue'),
        meta: { title: '新建星记' },
      },
      {
        path: 'admin/articles/:id',
        name: 'EditArticle',
        component: () => import('@/views/articles/ArticleFormView.vue'),
        meta: { title: '编辑星记' },
      },
      {
        path: 'admin/categories',
        name: 'categories',
        component: () => import('@/views/categories/CategoryListView.vue'),
        meta: { title: '星域管理' },
      },
      {
        path: 'admin/users',
        name: 'users',
        component: () => import('@/views/users/UserListView.vue'),
        meta: { title: '观星者' },
      },
      {
        path: 'admin/ai-config',
        name: 'AiConfig',
        component: () => import('@/views/ai-config/AiConfigView.vue'),
        meta: { title: 'AI 配置' },
      },
      {
        path: 'admin/login-logs',
        name: 'loginLogs',
        component: () => import('@/views/login-logs/LoginLogView.vue'),
        meta: { title: '登录日志' },
      },
    ],
  },
]
