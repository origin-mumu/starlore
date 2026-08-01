<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="brand-mark"></div>
        <h1>Starlore</h1>
        <p>后台管理系统</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="loginForm.username"
            type="text"
            placeholder="请输入用户名"
            required
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
      </form>

      <div class="login-footer">
        <p>请使用知识库账号登录</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')

const loginForm = ref({
  username: '',
  password: '',
})

const handleLogin = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const res: any = await request.post('/auth/login', {
      username: loginForm.value.username,
      password: loginForm.value.password,
    })

    const token = res.data?.token
    const user = res.data?.user
    if (!token || !user) {
      errorMessage.value = '登录失败，返回数据异常'
      return
    }

    if (user.role !== 'admin') {
      errorMessage.value = '仅管理员可登录后台管理系统'
      return
    }

    localStorage.setItem('ro_blog_admin_token', token)
    localStorage.setItem('isLoggedIn', 'true')
    localStorage.setItem('admin_user_role', user.role)
    router.push('/admin/articles')
  } catch (err: any) {
    errorMessage.value = err.message || '用户名或密码错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--canvas);
  padding: var(--space-5);
}

.login-card {
  background: var(--surface);
  padding: var(--space-7) var(--space-6);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-card);
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: var(--space-6);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.brand-mark {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--accent);
}

.login-header h1 {
  color: var(--ink);
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  font-family: 'Source Serif 4', 'LXGW WenKai', serif;
  letter-spacing: -0.02em;
}

.login-header p {
  color: var(--ink-muted);
  font-size: 0.88rem;
  margin: 0;
}

.form-group {
  margin-bottom: var(--space-5);
}

.form-group label {
  display: block;
  margin-bottom: var(--space-2);
  color: var(--ink-soft);
  font-weight: 500;
  font-size: 0.9rem;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 0.95rem;
  font-family: inherit;
  transition: border-color var(--transition), box-shadow var(--transition);
  box-sizing: border-box;
  background: var(--surface);
  color: var(--ink);
}

.form-group input:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px oklch(0.55 0.15 35 / 0.12);
}

.form-group input::placeholder {
  color: var(--ink-muted);
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: var(--accent);
  color: #FDFBF5;
  border: none;
  border-radius: var(--radius-md);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: var(--shadow-button);
  margin-top: var(--space-2);
}

.login-btn:hover:not(:disabled) {
  background: var(--accent-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-button-hover);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-message {
  margin-top: var(--space-4);
  padding: 12px;
  background: var(--status-error-bg);
  color: var(--status-error-text);
  border-radius: var(--radius-sm);
  font-size: 0.88rem;
  text-align: center;
}

.login-footer {
  margin-top: var(--space-5);
  text-align: center;
  font-size: 0.78rem;
  color: var(--ink-muted);
}
</style>
