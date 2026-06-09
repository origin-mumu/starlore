import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: {
        title: '首页 - Starlore',
        requiresAuth: false,
        guestAllowed: true
      }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: {
        title: '个人中心 - Starlore',
        requiresAuth: true
      }
    },
    {
      path: '/articles',
      name: 'articles',
      component: () => import('../views/ArticlesView.vue'),
      meta: {
        title: '文章列表 - Starlore',
        requiresAuth: true
      }
    },
    {
      path: '/articles/:id',
      name: 'articleDetail',
      component: () => import('../views/ArticleDetail.vue'),
      meta: {
        title: '文章详情 - Starlore',
        requiresAuth: true
      }
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('../views/Categories.vue'),
      meta: {
        title: '星域 - Starlore',
        requiresAuth: false,
        guestAllowed: true
      }
    },
    {
      path: '/echobot',
      name: 'echobot',
      component: () => import('../views/EchobotView.vue'),
      meta: {
        title: 'AI 助手 - Starlore',
        requiresAuth: false,
        guestAllowed: true
      }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: {
        title: '登录 - Starlore',
        requiresAuth: false
      }
    },
    {
      path: '/diverge',
      name: 'diverge',
      component: () => import('../views/DivergeView.vue'),
      meta: {
        title: '创意发散 - Starlore',
        requiresAuth: false,
        guestAllowed: true
      }
    },
    {
      path: '/resume',
      name: 'resume',
      component: () => import('../views/ResumeListView.vue'),
      meta: {
        title: '简历管理 - Starlore',
        requiresAuth: true,
        requiredRole: ['member', 'admin']
      }
    },
    {
      path: '/resume/edit/:id?',
      name: 'resumeEdit',
      component: () => import('../views/ResumeEditView.vue'),
      meta: {
        title: '编辑简历 - Starlore',
        requiresAuth: true,
        requiredRole: ['member', 'admin']
      }
    },
    {
      path: '/articles/edit/:id?',
      name: 'articleEdit',
      component: () => import('../views/ArticleEditView.vue'),
      meta: {
        title: '编辑文章 - Starlore',
        requiresAuth: true
      }
    },
    {
      path: '/vr',
      name: 'vr',
      component: () => import('../views/VRView.vue'),
      meta: {
        title: '知识星域 - Starlore',
        requiresAuth: false,
        guestAllowed: true
      }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('../views/NotFound.vue'),
      meta: {
        title: '页面未找到 - Starlore',
        requiresAuth: false
      }
    }
  ],
  scrollBehavior() {
    return { top: 0, behavior: 'smooth' }
  }
})

const TOKEN_KEY = 'ro_blog_token'

router.beforeEach((to) => {
  const title = to.meta.title as string
  if (title) {
    document.title = title
  }

  const token = localStorage.getItem(TOKEN_KEY)
  const requiresAuth = to.meta.requiresAuth !== false
  const guestAllowed = to.meta.guestAllowed === true

  // 访客允许的页面 -> 直接放行
  if (guestAllowed) {
    return true
  }

  // 未登录且访问需要认证的页面 -> 跳转登录
  if (requiresAuth && !token) {
    return { name: 'login' }
  }

  // 已登录且访问登录页 -> 跳转首页
  if (to.name === 'login' && token) {
    return { name: 'home' }
  }
})

export default router
