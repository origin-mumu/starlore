import request from '@/utils/request'

export interface LoginParams {
    username: string
    password: string
}

export interface RegisterParams {
    username: string
    password: string
    email?: string
    nickname?: string
}

export interface UpdateProfileParams {
    nickname?: string
    avatar?: string
    bio?: string
    email?: string
    location?: string
    website?: string
    github?: string
}

export interface ChangePasswordParams {
    oldPassword: string
    newPassword: string
}

export interface UserInfo {
    id: number
    username: string
    nickname: string
    email: string | null
    avatar: string | null
    bio: string | null
    location: string | null
    website: string | null
    github: string | null
    role: string
}

export interface AuthResult {
    token: string
    user: UserInfo
}

// 登录
export function loginService(params: LoginParams) {
    return request.post<never, { message: string; data: AuthResult }>('/auth/login', params)
}

// 注册
export function registerService(params: RegisterParams) {
    return request.post<never, { message: string; data: AuthResult }>('/auth/register', params)
}

// 获取当前用户信息
export function getCurrentUserService() {
    return request.get<never, { data: UserInfo }>('/auth/me')
}

// 更新用户信息
export function updateProfileService(params: UpdateProfileParams) {
    return request.put<never, { message: string; data: UserInfo }>('/auth/profile', params)
}

// 修改密码
export function changePasswordService(params: ChangePasswordParams) {
    return request.put<never, { message: string }>('/auth/profile', params)
}
