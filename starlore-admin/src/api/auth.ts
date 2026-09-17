import { http } from '@/utils/request'
import type { LoginPayload, LoginResultVO, LoginLogQuery, LoginLogListVO } from '@/types'

export async function loginService(payload: LoginPayload): Promise<LoginResultVO> {
  const res = await http.post<any>('/auth/login', payload)
  return (res?.data?.token ? res.data : res) as LoginResultVO
}

export function getLoginLogsService(query: LoginLogQuery): Promise<LoginLogListVO> {
  return http.get('/admin/login-logs', { params: query })
}
