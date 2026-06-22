import axios from 'axios'

const baseURL = '/api'
const TOKEN_KEY = 'ro_blog_token'
const GUEST_ALLOWED_PATHS = ['/', '/categories', '/vr']
const instance = axios.create({ baseURL })

instance.interceptors.request.use(
  (config) => {
    if (typeof localStorage !== 'undefined') {
      config.headers = config.headers ?? {}

      // 附加 JWT token
      const token = localStorage.getItem(TOKEN_KEY)
      if (token) {
        ; (config.headers as Record<string, string>)['Authorization'] = `Bearer ${token}`
      }
    }
    return config
  },
  (err) => {
    return Promise.reject(err)
  },
)

instance.interceptors.response.use(
  (result) => {
    return result.data
  },
  (err) => {
    // 401 未授权 -> 清除登录状态
    if (err.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      const path = window.location.pathname
      // 访客允许的页面不跳转，仅清除 token
      const isGuestAllowed = GUEST_ALLOWED_PATHS.some(p => path === p || path.startsWith(p + '/'))
      if (!isGuestAllowed && path !== '/login') {
        window.location.href = '/login'
      }
    }
    // 403 禁止访问 -> 提示并返回首页
    if (err.response?.status === 403) {
      const msg = err.response?.data?.message || '无权限访问该功能'
      const el = document.createElement('div')
      el.textContent = msg
      el.style.cssText = 'position:fixed;top:20px;left:50%;transform:translateX(-50%);z-index:9999;background:#e74c3c;color:#fff;padding:12px 24px;border-radius:8px;font-size:14px;font-family:inherit;box-shadow:0 4px 16px rgba(0,0,0,0.2);animation:fadeIn 0.3s'
      document.body.appendChild(el)
      setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity 0.3s'; setTimeout(() => el.remove(), 300) }, 3000)
      if (window.location.pathname !== '/') {
        window.location.href = '/'
      }
    }
    return Promise.reject(new Error(err.response?.data?.message || '网络错误'))
  },
)

export default instance