import axios, { type AxiosRequestConfig } from 'axios'

const TOKEN_KEY = 'ro_blog_admin_token'
const ROLE_KEY = 'admin_user_role'
const USER_KEY = 'admin_user_info'

export { TOKEN_KEY, ROLE_KEY, USER_KEY }

/** 会话过期/失效时广播，由 main.ts 统一清理并跳转登录页 */
export const UNAUTHORIZED_EVENT = 'starlore:unauthorized'

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(USER_KEY)
}

const instance = axios.create({ baseURL: '/api', timeout: 60_000 })

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
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
    } else if (err.response?.status === 403) {
      return Promise.reject(new Error(err.response?.data?.detail ?? '需要管理员权限'))
    }
    const message: string =
      err.response?.data?.message ?? err.response?.data?.detail ?? '网络错误，请稍后重试'
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
