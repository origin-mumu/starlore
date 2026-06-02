import axios from 'axios'

const baseURL = '/api'
const TOKEN_KEY = 'ro_blog_admin_token'
const instance = axios.create({ baseURL })

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers = config.headers ?? {}
      ;(config.headers as Record<string, string>)['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (err) => Promise.reject(err),
)

instance.interceptors.response.use(
  (result) => {
    return result.data
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('isLoggedIn')
      window.location.href = '/login'
    }
    return Promise.reject(new Error(err.response?.data?.message || '网络错误'))
  },
)

export default instance
