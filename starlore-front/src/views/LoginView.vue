<template>
  <div class="auth-page">
    <!-- 宇宙底景星云光晕 -->
    <div class="auth-bg-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <div class="auth-card fade-in-up" :class="{ 'auth-card--wide': activeTab === 'register' }">
      <!-- 品牌标志与标题 -->
      <div class="brand-section">
        <div class="brand-logo">
          <Sparkles class="logo-icon" :size="24" />
        </div>
        <h1 class="brand-title">Starlore</h1>
        <p class="brand-tagline">用理性的光芒照亮人文的星空</p>
      </div>

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
          {{ activeTab === 'login' ? '欢迎回到知识星空' : '开启您的学术与文学探索之旅' }}
        </p>
      </div>

      <!-- 登录表单 -->
      <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="auth-form">
        <div class="form-field">
          <label for="login-username">用户名</label>
          <div class="input-wrapper">
            <User class="input-icon" :size="18" />
            <input
              id="login-username"
              v-model="loginForm.username"
              type="text"
              placeholder="请输入用户名"
              autocomplete="username"
              required
            />
          </div>
        </div>
        <div class="form-field">
          <label for="login-password">密码</label>
          <div class="input-wrapper">
            <Lock class="input-icon" :size="18" />
            <input
              id="login-password"
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              autocomplete="current-password"
              required
            />
            <button
              type="button"
              class="password-toggle"
              @click="showPassword = !showPassword"
              tabindex="-1"
            >
              <Eye v-if="!showPassword" :size="18" />
              <EyeOff v-else :size="18" />
            </button>
          </div>
        </div>
        <button type="submit" class="auth-btn" :disabled="userStore.loading">
          <span v-if="userStore.loading" class="btn-spinner"></span>
          {{ userStore.loading ? '正在跃迁...' : '登 录' }}
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
          <div class="input-wrapper">
            <User class="input-icon" :size="18" />
            <input
              id="reg-username"
              v-model="registerForm.username"
              type="text"
              placeholder="请输入用户名"
              autocomplete="username"
              required
            />
          </div>
        </div>
        <div class="form-field">
          <label for="reg-nickname">昵称</label>
          <div class="input-wrapper">
            <User class="input-icon" :size="18" />
            <input
              id="reg-nickname"
              v-model="registerForm.nickname"
              type="text"
              placeholder="给自己起个昵称"
            />
          </div>
        </div>
        <div class="form-field">
          <label for="reg-password">密码 <span class="required">*</span></label>
          <div class="input-wrapper">
            <Lock class="input-icon" :size="18" />
            <input
              id="reg-password"
              v-model="registerForm.password"
              :type="showRegPassword ? 'text' : 'password'"
              placeholder="至少6位密码"
              autocomplete="new-password"
              required
            />
            <button
              type="button"
              class="password-toggle"
              @click="showRegPassword = !showRegPassword"
              tabindex="-1"
            >
              <Eye v-if="!showRegPassword" :size="18" />
              <EyeOff v-else :size="18" />
            </button>
          </div>
        </div>
        <div class="form-field">
          <label for="reg-confirm">确认密码 <span class="required">*</span></label>
          <div class="input-wrapper">
            <Lock class="input-icon" :size="18" />
            <input
              id="reg-confirm"
              v-model="confirmPassword"
              :type="showRegConfirmPassword ? 'text' : 'password'"
              placeholder="请再次输入密码"
              autocomplete="new-password"
              required
            />
            <button
              type="button"
              class="password-toggle"
              @click="showRegConfirmPassword = !showRegConfirmPassword"
              tabindex="-1"
            >
              <Eye v-if="!showRegConfirmPassword" :size="18" />
              <EyeOff v-else :size="18" />
            </button>
          </div>
        </div>
        <div class="form-field form-field--full">
          <button type="submit" class="auth-btn" :disabled="userStore.loading">
            <span v-if="userStore.loading" class="btn-spinner"></span>
            {{ userStore.loading ? '正在注册...' : '注 册' }}
          </button>
        </div>
        <p v-if="errorMsg" class="auth-error form-field--full">{{ errorMsg }}</p>
      </form>

      <!-- 返回首页 -->
      <div class="auth-footer">
        <router-link to="/" class="back-home">
          <ArrowLeft class="back-icon" :size="16" />
          <span>返回首页</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { User, Lock, Eye, EyeOff, Sparkles, ArrowLeft } from '@lucide/vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const errorMsg = ref('')

const showPassword = ref(false)
const showRegPassword = ref(false)
const showRegConfirmPassword = ref(false)

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
  showPassword.value = false
  showRegPassword.value = false
  showRegConfirmPassword.value = false
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
  /* background: var(--canvas);  */
  padding: 40px 20px;
  overflow: hidden;
}

/* ── Dynamic Nebula Orbs ── */
.auth-bg-orbs {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.15;
  transition: all 1s var(--ease-out-quart);
}

.orb-1 {
  width: min(45vw, 400px);
  height: min(45vw, 400px);
  background: var(--orb-1, var(--accent));
  top: -10%;
  left: -5%;
  animation: float1 20s infinite alternate ease-in-out;
}

.orb-2 {
  width: min(55vw, 500px);
  height: min(55vw, 500px);
  background: var(--orb-2, var(--accent-hover));
  bottom: -10%;
  right: -5%;
  animation: float2 25s infinite alternate ease-in-out;
}

.orb-3 {
  width: min(35vw, 300px);
  height: min(35vw, 300px);
  background: var(--orb-3, var(--accent-soft));
  top: 35%;
  left: 60%;
  animation: float3 18s infinite alternate ease-in-out;
}

@keyframes float1 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(40px, 30px) scale(1.15); }
}

@keyframes float2 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-50px, -40px) scale(1.2); }
}

@keyframes float3 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(30px, -30px) scale(0.9); }
}

/* ── Brand Section ── */
.brand-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16px;
  text-align: center;
}

.brand-logo {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  color: var(--accent);
  border-radius: 14px;
  margin-bottom: 8px;
  box-shadow: 0 6px 18px color-mix(in srgb, var(--accent) 15%, transparent);
  animation: logoPulse 4s infinite ease-in-out;
}

.brand-title {
  font-family: 'LXGW WenKai', 'Source Serif 4', 'Georgia', 'Noto Serif SC', serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
  letter-spacing: -0.02em;
}

.brand-tagline {
  font-family: 'LXGW WenKai', serif;
  font-size: 13px;
  color: var(--ink-soft);
  margin: 4px 0 0 0;
  opacity: 0.8;
}

@keyframes logoPulse {
  0%, 100% { transform: scale(1) rotate(0deg); }
  50% { transform: scale(1.06) rotate(15deg); }
}

/* ── Glass Card ── */
.auth-card {
  position: relative;
  z-index: 1;
  background: var(--glass-bg);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  border: 1px solid var(--border);
  border-radius: 28px;
  box-shadow: var(--shadow-card);
  padding: 32px 32px 24px;
  width: 100%;
  max-width: 460px;
  display: flex;
  flex-direction: column;
  transition: max-width 0.4s var(--ease-out-quart), box-shadow 0.3s, border-color var(--transition);
}

.auth-card:hover {
  box-shadow: var(--shadow-card-hover);
  border-color: var(--border-interactive);
}

.auth-card--wide {
  max-width: 580px;
}

/* ── Header & Tabs ── */
.auth-header {
  text-align: center;
  margin-bottom: 16px;
}

.auth-tabs {
  display: flex;
  background: var(--canvas-deep);
  border-radius: var(--radius-full);
  padding: 4px;
  margin-bottom: 8px;
  border: 1px solid color-mix(in srgb, var(--border) 40%, transparent);
}

.auth-tab {
  flex: 1;
  padding: 8px 0;
  border: none;
  background: transparent;
  font-size: 14px;
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
  box-shadow: 0 4px 14px color-mix(in srgb, var(--accent) 25%, transparent);
}

.auth-tab:hover:not(.active) {
  color: var(--ink);
  background: color-mix(in srgb, var(--accent) 8%, transparent);
}

.auth-subtitle {
  font-size: 13px;
  color: var(--ink-muted);
  margin: 0;
}

/* ── Form ── */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.auth-form--grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-field--full {
  grid-column: 1 / -1;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-field label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-soft);
  padding-left: 4px;
}

.required {
  color: var(--accent);
  font-weight: bold;
}

/* ── Inputs (with icons) ── */
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 16px;
  color: var(--ink-muted);
  pointer-events: none;
  transition: color var(--transition);
}

.input-wrapper input {
  width: 100%;
  padding: 10px 16px 10px 42px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  font-size: 14px;
  background: var(--canvas-deep);
  color: var(--ink);
  outline: none;
  transition: all var(--transition);
  font-family: inherit;
  box-sizing: border-box;
}

.input-wrapper input::placeholder {
  color: var(--ink-muted);
}

/* Edge provides its own password reveal control. The page already renders a
   cross-browser toggle, so hide the native control to avoid overlapping eyes. */
.input-wrapper input::-ms-reveal,
.input-wrapper input::-ms-clear {
  display: none;
}

.input-wrapper input:focus {
  border-color: var(--border-interactive);
  background: var(--surface);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 15%, transparent);
}

.input-wrapper input:focus + .input-icon,
.input-wrapper input:focus ~ .input-icon {
  color: var(--accent);
}

/* 密码切换按钮 */
.password-toggle {
  position: absolute;
  right: 16px;
  border: none;
  background: transparent;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color var(--transition);
}

.password-toggle:hover {
  color: var(--ink);
}

/* ── Button ── */
.auth-btn {
  margin-top: 4px;
  padding: 11px;
  border: none;
  border-radius: var(--radius-full);
  font-size: 15px;
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
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ── Spinner ── */
.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── Error ── */
.auth-error {
  text-align: center;
  font-size: 13px;
  color: #c0392b;
  background: rgba(231, 76, 60, 0.08);
  border: 1px solid rgba(231, 76, 60, 0.2);
  padding: 10px 14px;
  border-radius: var(--radius-md);
  margin: 0;
}

/* ── Footer / Back Link ── */
.auth-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
  text-align: center;
}

.back-home {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--ink-soft);
  text-decoration: none;
  font-weight: 500;
  transition: color var(--transition);
}

.back-home:hover {
  color: var(--accent);
}

.back-icon {
  transition: transform var(--transition);
}

.back-home:hover .back-icon {
  transform: translateX(-4px);
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
  animation: fadeInUp 0.7s var(--ease-out-quart) forwards;
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
