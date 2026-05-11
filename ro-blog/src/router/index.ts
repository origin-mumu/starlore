import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: {
        title: '首页 - RO-BLOG'
      }
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
      meta: {
        title: '关于我 - RO-BLOG'
      }
    },
    {
      path: '/articles',
      name: 'articles',
      component: () => import('../views/ArticlesView.vue'),
      meta: {
        title: '文章列表 - RO-BLOG'
      }
    },
    {
      path: '/articles/:id',
      name: 'articleDetail',
      component: () => import('../views/ArticleDetail.vue'),
      meta: {
        title: '文章详情 - RO-BLOG'
      }
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('../views/Categories.vue'),
      meta: {
        title: '分类 - RO-BLOG'
      }
    },
    {
      path: '/echobot',
      name: 'echobot',
      component: () => import('../views/EchobotView.vue'),
      meta: {
        title: 'AI 助手 - RO-BLOG'
      }
    },
    {
      path: '/vr',
      name: 'vr',
      component: () => import('../views/VRView.vue'),
      meta: {
        title: 'VR 展厅 - RO-BLOG'
      }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('../views/NotFound.vue'),
      meta: {
        title: '页面未找到 - RO-BLOG'
      }
    }
  ],
  scrollBehavior() {
    return { top: 0, behavior: 'smooth' }
  }
})

router.beforeEach((to) => {
  const title = to.meta.title as string
  if (title) {
    document.title = title
  }
})

export default router