import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { clearAuthToken, getAuthToken } from '@/utils/authToken'

let articleDetailPromise: ReturnType<typeof importArticleDetail> | undefined

function importArticleDetail() {
  return import('../views/ArticleDetail.vue')
}

export function preloadArticleDetail() {
  articleDetailPromise ??= importArticleDetail()
  return articleDetailPromise
}

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
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
      meta: {
        title: '关于我 - Starlore',
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
        requiresAuth: false,
        guestAllowed: true
      }
    },
    {
      path: '/articles/:id',
      name: 'articleDetail',
      component: preloadArticleDetail,
      meta: {
        title: '文章详情 - Starlore',
        requiresAuth: false,
        guestAllowed: true
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
      redirect: '/harness'
    },
    {
      path: '/harness',
      name: 'harness',
      component: () => import('../views/harness/HarnessView.vue'),
      meta: {
        title: '云端智能体 - Starlore',
        requiresAuth: true
      }
    },
    {
      path: '/knowledge-memory',
      name: 'knowledgeMemory',
      component: () => import('../views/KnowledgeMemoryView.vue'),
      meta: {
        title: '知识记忆 - Starlore',
        requiresAuth: true
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
        requiresAuth: true
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
      path: '/companion-studio',
      alias: '/companion',
      redirect: '/harness'
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
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition || { top: 0 }
  }
})
function isExpiredJwt(token: string): boolean {
  try {
    const payloadPart = token.split('.')[1]
    if (!payloadPart) return true
    const payload = JSON.parse(atob(payloadPart.replace(/-/g, '+').replace(/_/g, '/')))
    return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

router.beforeEach(async (to) => {
  const title = to.meta.title as string
  if (title) {
    document.title = title
  }

  const token = getAuthToken()
  const requiresAuth = to.meta.requiresAuth !== false
  const guestAllowed = to.meta.guestAllowed === true

  // 访客允许的页面 -> 直接放行
  if (guestAllowed) {
    return true
  }

  // 未登录且访问需要认证的页面 -> 跳转登录
  if (requiresAuth && (!token || isExpiredJwt(token))) {
    clearAuthToken()
    return { name: 'login' }
  }

  const requiredRoles = to.meta.requiredRole as string[] | undefined
  if (requiredRoles?.length && token) {
    const userStore = useUserStore()
    if (!userStore.user) await userStore.fetchCurrentUser()
    if (!userStore.user || !requiredRoles.includes(userStore.role)) {
      return { name: 'home' }
    }
  }

  // 已登录且访问登录页 -> 跳转首页
  if (to.name === 'login' && token && !isExpiredJwt(token)) {
    return { name: 'home' }
  }
})

export default router
