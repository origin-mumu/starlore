// Element Plus 基础样式先加载，tokens.css 的主题变量覆盖才不会被冲掉
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import '@/styles/tokens.css'
import '@/styles/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { UNAUTHORIZED_EVENT, clearSession } from './utils/request'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia).use(router)

// 主题初始化：跟随上次选择（EP dark vars 挂在 html.dark 上）
if (localStorage.getItem('starlore-admin-theme') === 'dark') {
  document.documentElement.classList.add('dark')
}

// 会话失效：统一清理并携带回跳地址进入登录页
window.addEventListener(UNAUTHORIZED_EVENT, () => {
  const auth = useAuthStore(pinia)
  auth.logout()
  const current = router.currentRoute.value
  if (current.name !== 'login') {
    void router.replace({ path: '/login', query: { redirect: current.fullPath } })
  }
})

// 兜底：任何残留会话数据异常时可直接清理
window.addEventListener('storage', (e) => {
  if (e.key === 'ro_blog_admin_token' && !e.newValue) clearSession()
})

app.mount('#app')
