<template>
  <div class="auth-page">
    <!-- 宇宙底景星云光晕 -->
    <div class="auth-bg-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <div class="liquid-editorial" aria-hidden="true">
      <span class="editorial-kicker">PERSONAL KNOWLEDGE SPACE</span>
      <strong>STAR<br />LORE</strong>
      <span class="editorial-caption">
        <em>记录</em>
        <span>整理</span>
        <span>再发现</span>
      </span>
    </div>

    <div class="liquid-stage" :class="{ 'liquid-stage--wide': activeTab === 'register' }">
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

      <button
        type="button"
        class="drop-action drop-action--mode"
        @click="switchTab(activeTab === 'login' ? 'register' : 'login')"
      >
        <span>{{ activeTab === 'login' ? '注册' : '登录' }}</span>
      </button>
      <router-link to="/" class="drop-action drop-action--home">
        <span>返回<br />首页</span>
      </router-link>
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

<style scoped src="./LoginView.css"></style>
