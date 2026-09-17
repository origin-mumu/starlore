import axios, { type AxiosRequestConfig } from 'axios'

const TOKEN_KEY = 'ro_blog_admin_token'
const ROLE_KEY = 'admin_user_role'
const LOGGED_IN_KEY = 'isLoggedIn'

export { TOKEN_KEY, ROLE_KEY, LOGGED_IN_KEY }

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(LOGGED_IN_KEY)
}

const instance = axios.create({ baseURL: '/api' })

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (result) => result.data,
  (err) => {
    if (err.response?.status === 401) {
      clearSession()
      window.location.href = '/login'
    }
    const message: string = err.response?.data?.message ?? '网络错误'
    return Promise.reject(new Error(message))
  },
)

/**
 * 响应拦截器已解包 axios 外壳，业务函数通过此助手
 * 以真实 VO 类型向调用方承诺返回值。
 */
export const http = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config) as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config) as Promise<T>
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config) as Promise<T>
  },
  patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.patch(url, data, config) as Promise<T>
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config) as Promise<T>
  },
}

export default instance
