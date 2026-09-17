<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginService } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { Loading } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMessage = ref('')

const loginForm = reactive({
  username: '',
  password: '',
})

const handleLogin = async (): Promise<void> => {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await loginService({ ...loginForm }) as any
    const loginData = result?.data?.token ? result.data : result
    const token = loginData?.token
    const user = loginData?.user

    if (!token || !user) {
      errorMessage.value = '登录失败，返回数据异常'
      return
    }
    if (user.role !== 'admin') {
      errorMessage.value = '仅管理员可登录后台管理系统'
      return
    }
    userStore.setSession(token, user.role)
    router.push('/admin/articles')
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : '用户名或密码错误'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-card__header">
        <div class="login-card__brand-mark">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true">
            <path d="M12 2l2.4 6.3L21 9.3l-5 4.4 1.5 6.6L12 16.8 6.5 20.3 8 13.7 3 9.3l6.6-1L12 2z" />
          </svg>
        </div>
        <h1 class="login-card__title">Starlore</h1>
        <p class="login-card__subtitle">后台管理系统</p>
      </div>

      <form class="login-card__form" @submit.prevent="handleLogin">
        <div class="form-field">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="loginForm.username"
            class="form-input"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            required
          />
        </div>

        <div class="form-field">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="loginForm.password"
            class="form-input"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            required
          />
        </div>

        <button class="btn btn--primary login-card__submit" type="submit" :disabled="loading">
          <el-icon v-if="loading" class="is-loading"><Loading /></el-icon>
          <span>{{ loading ? '登录中...' : '登录' }}</span>
        </button>

        <Transition name="error-fade">
          <div v-if="errorMessage" class="login-card__error">{{ errorMessage }}</div>
        </Transition>
      </form>

      <p class="login-card__footer">请使用知识库账号登录</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 44px 36px 30px;
  background: var(--surface-glass);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: var(--border-glass);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.login-card__header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  margin-bottom: 28px;
  text-align: center;
}

.login-card__brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--brand-primary), var(--brand-secondary));
  color: #fff;
  box-shadow: var(--shadow-button);
  margin-bottom: 4px;
}

.login-card__title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.login-card__subtitle {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.login-card__form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.login-card__submit {
  width: 100%;
  padding: 12px;
  font-size: 0.95rem;
  margin-top: 4px;
}

.login-card__error {
  padding: 10px 14px;
  background: var(--color-danger-subtle);
  color: var(--color-danger);
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  text-align: center;
}

.error-fade-enter-active {
  transition: all var(--transition-normal);
}

.error-fade-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}

.login-card__footer {
  margin-top: 22px;
  text-align: center;
  font-size: 0.78rem;
  color: var(--text-muted);
}
</style>
