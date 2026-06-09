<template>
  <div class="auth-page">
    <div class="auth-card fade-in-up" :class="{ 'auth-card--wide': activeTab === 'register' }">
      <div class="auth-header">
        <div class="auth-tabs">
          <button
            class="auth-tab"
            :class="{ active: activeTab === 'login' }"
            @click="switchTab('login')"
          >
            登录
          </button>
          <button
            class="auth-tab"
            :class="{ active: activeTab === 'register' }"
            @click="switchTab('register')"
          >
            注册
          </button>
        </div>
        <p class="auth-subtitle">
          {{ activeTab === 'login' ? '欢迎回到 Starlore' : '加入 Starlore，开启你的知识之旅' }}
        </p>
      </div>

      <!-- 登录表单 -->
      <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="auth-form">
        <div class="form-field">
          <label for="login-username">用户名</label>
          <input
            id="login-username"
            v-model="loginForm.username"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            required
          />
        </div>
        <div class="form-field">
          <label for="login-password">密码</label>
          <input
            id="login-password"
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            required
          />
        </div>
        <button type="submit" class="auth-btn" :disabled="userStore.loading">
          <span v-if="userStore.loading" class="btn-spinner"></span>
          {{ userStore.loading ? '登录中...' : '登 录' }}
        </button>
        <p v-if="errorMsg" class="auth-error">{{ errorMsg }}</p>
      </form>

      <!-- 注册表单 -->
      <form
        v-if="activeTab === 'register'"
        @submit.prevent="handleRegister"
        class="auth-form auth-form--grid"
      >
        <div class="form-field">
          <label for="reg-username">用户名 <span class="required">*</span></label>
          <input
            id="reg-username"
            v-model="registerForm.username"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            required
          />
        </div>
        <div class="form-field">
          <label for="reg-nickname">昵称</label>
          <input
            id="reg-nickname"
            v-model="registerForm.nickname"
            type="text"
            placeholder="给自己起个昵称（选填）"
          />
        </div>
        <div class="form-field">
          <label for="reg-password">密码 <span class="required">*</span></label>
          <input
            id="reg-password"
            v-model="registerForm.password"
            type="password"
            placeholder="至少6位密码"
            autocomplete="new-password"
            required
          />
        </div>
        <div class="form-field">
          <label for="reg-confirm">确认密码 <span class="required">*</span></label>
          <input
            id="reg-confirm"
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            autocomplete="new-password"
            required
          />
        </div>
        <div class="form-field form-field--full">
          <button type="submit" class="auth-btn" :disabled="userStore.loading">
            <span v-if="userStore.loading" class="btn-spinner"></span>
            {{ userStore.loading ? '注册中...' : '注 册' }}
          </button>
        </div>
        <p v-if="errorMsg" class="auth-error form-field--full">{{ errorMsg }}</p>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const errorMsg = ref('')

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
})
const confirmPassword = ref('')

const switchTab = (tab: 'login' | 'register') => {
  activeTab.value = tab
  errorMsg.value = ''
}

const handleLogin = async () => {
  errorMsg.value = ''
  if (!loginForm.username.trim() || !loginForm.password) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  try {
    await userStore.login({
      username: loginForm.username.trim(),
      password: loginForm.password,
    })
    router.push('/')
  } catch (e: any) {
    errorMsg.value = e.message || '登录失败，请重试'
  }
}

const handleRegister = async () => {
  errorMsg.value = ''
  if (!registerForm.username.trim() || !registerForm.password) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  if (registerForm.password.length < 6) {
    errorMsg.value = '密码长度至少6位'
    return
  }
  if (registerForm.password !== confirmPassword.value) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  try {
    await userStore.register({
      username: registerForm.username.trim(),
      password: registerForm.password,
      nickname: registerForm.nickname.trim() || undefined,
    })
    router.push('/')
  } catch (e: any) {
    errorMsg.value = e.message || '注册失败，请重试'
  }
}
</script>

<style scoped>
/* ── Page ── */
.auth-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  padding: 20px;
  overflow: hidden;
}

/* ── Glass Card ── */
.auth-card {
  position: relative;
  z-index: 1;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 24px;

  box-shadow:
    0 12px 40px oklch(0.55 0.15 35 / 0.06),
    0 2px 8px oklch(0.55 0.15 35 / 0.04);
  padding: 48px 40px 40px;
  width: 100%;
  max-width: 420px;
  height: 500px;
  display: flex;
  flex-direction: column;
}

.auth-card--wide {
  max-width: 580px;
}

/* ── Header ── */
.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

/* ── Tabs (pill container) ── */
.auth-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 16px;
  background: var(--canvas-deep);

  border-radius: var(--radius-full);
  padding: 4px;
}

.auth-tab {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: transparent;
  font-size: 15px;
  font-weight: 500;
  color: var(--ink-muted);
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: all var(--transition);
  font-family: inherit;
}

.auth-tab.active {
  background: var(--accent);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 4px 16px color-mix(in srgb, var(--accent) 22%, transparent);
}

.auth-tab:hover:not(.active) {
  color: var(--ink);
  background: var(--accent-soft);
}

.auth-subtitle {
  font-size: 14px;
  color: var(--ink-muted);
  margin: 0;
}

/* ── Form ── */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
  flex: 1;
}

/* 注册表单两列布局 */
.auth-form--grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-field--full {
  grid-column: 1 / -1;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field label {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink);
}

.required {
  color: #c0392b;
}

/* ── Inputs (pill-shaped, glass background) ── */
.form-field input {
  padding: 12px 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  font-size: 15px;
  background: var(--surface);
  color: var(--ink);
  outline: none;
  transition: all var(--transition);
  font-family: inherit;
  box-sizing: border-box;
}

.form-field input::placeholder {
  color: var(--ink-muted);
}

.form-field input:focus {
  border-color: var(--border-focus);
  box-shadow: 0 0 0 3px oklch(0.55 0.15 35 / 0.12);
  background: var(--surface-hover);
}

.auth-btn {
  margin-top: 4px;
  padding: 14px;
  border: none;
  border-radius: var(--radius-full);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  color: #ffffff;
  background: var(--accent);
  box-shadow: var(--shadow-button);
  transition: all var(--transition);
  font-family: inherit;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.auth-btn:hover:not(:disabled) {
  box-shadow: var(--shadow-button-hover);
  transform: translateY(-2px);
}

.auth-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 8px 24px oklch(0.55 0.15 35 / 0.16);
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ── Spinner ── */
.btn-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── Error ── */
.auth-error {
  text-align: center;
  font-size: 14px;
  color: #c0392b;
  background: rgba(231, 76, 60, 0.08);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  margin: 0;
}

/* ── Footer / Back Link ── */
.auth-footer {
  margin-top: auto;
  padding-top: 20px;
  text-align: center;
}

.back-home {
  font-size: 15px;
  color: var(--accent);
  text-decoration: none;
  font-weight: 500;
  transition: color var(--transition);
}

.back-home:hover {
  color: var(--accent-hover);
  text-decoration: underline;
}

/* ── Responsive ── */
@media (max-width: 640px) {
  .auth-card--wide {
    max-width: 420px;
  }
  .auth-form--grid {
    grid-template-columns: 1fr;
  }
}

/* ── Fade-in animation ── */
.fade-in-up {
  opacity: 0;
  transform: translateY(24px);
  animation: fadeInUp 0.7s ease-out forwards;
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
