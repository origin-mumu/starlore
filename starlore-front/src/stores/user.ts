import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
    loginService,
    registerService,
    getCurrentUserService,
    updateProfileService,
    type LoginParams,
    type RegisterParams,
    type UpdateProfileParams,
    type UserInfo,
} from '@/api/auth'

const TOKEN_KEY = 'ro_blog_token'

export const useUserStore = defineStore('user', () => {
    const user = ref<UserInfo | null>(null)
    const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
    const loading = ref(false)

    const isLoggedIn = computed(() => !!token.value)
    const nickname = computed(() => user.value?.nickname || user.value?.username || '')
    const role = computed(() => user.value?.role || 'user')

    // 设置 token
    const setToken = (newToken: string) => {
        token.value = newToken
        localStorage.setItem(TOKEN_KEY, newToken)
    }

    // 清除登录状态
    const clearAuth = () => {
        token.value = ''
        user.value = null
        localStorage.removeItem(TOKEN_KEY)
    }

    // 登录
    const login = async (params: LoginParams) => {
        loading.value = true
        try {
            const res = await loginService(params)
            setToken(res.data.token)
            user.value = res.data.user
            return res
        } finally {
            loading.value = false
        }
    }

    // 注册
    const register = async (params: RegisterParams) => {
        loading.value = true
        try {
            const res = await registerService(params)
            setToken(res.data.token)
            user.value = res.data.user
            return res
        } finally {
            loading.value = false
        }
    }

    // 获取当前用户
    const fetchCurrentUser = async () => {
        if (!token.value) return
        loading.value = true
        try {
            const res = await getCurrentUserService()
            // 兼容: res.data.user → res.data → res
            user.value = (res as any).data?.user ?? (res as any).data ?? (res as any)
        } catch {
            // token 失效，清除
            clearAuth()
        } finally {
            loading.value = false
        }
    }

    // 更新用户信息
    const updateProfile = async (params: UpdateProfileParams) => {
        loading.value = true
        try {
            const res = await updateProfileService(params)
            // 更新后重新拉取最新用户数据（确保字段一致）
            await fetchCurrentUser()
            return res
        } finally {
            loading.value = false
        }
    }

    // 登出
    const logout = () => {
        clearAuth()
    }

    return {
        user,
        token,
        loading,
        isLoggedIn,
        nickname,
        role,
        login,
        register,
        fetchCurrentUser,
        updateProfile,
        logout,
    }
})
