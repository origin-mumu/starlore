<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { Orbit } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('欢迎回来，管理员')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    void router.replace(redirect)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="brand-mark">
          <Orbit :size="26" :stroke-width="2.2" />
        </div>
        <h1>Starlore 管理控制台</h1>
        <p>仅管理员账号可登录后台</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 16px;
  background:
    radial-gradient(circle at 82% 8%, var(--brand-glow-soft), transparent 28%),
    radial-gradient(circle at 15% 92%, var(--accent-glow-soft), transparent 26%),
    var(--bg-app);
}

.login-card {
  width: min(400px, 100%);
  padding: 40px 38px 36px;
  background: var(--surface-glass);
  border: var(--border-glass);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  backdrop-filter: blur(18px);
}

@media (max-width: 480px) {
  .login-card {
    padding: 28px 20px 24px;
  }
}

.login-brand {
  margin-bottom: 26px;
  text-align: center;
}

.brand-mark {
  display: grid;
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
  place-items: center;
  background: linear-gradient(145deg, var(--brand-subtle), rgba(110, 231, 183, 0.12));
  border-radius: 16px;
  box-shadow: var(--shadow-button);
  color: var(--brand-primary);
}

.login-brand h1 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--text-primary);
}

.login-brand p {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-muted);
}

.login-btn {
  width: 100%;
  margin-top: 6px;
  letter-spacing: 4px;
}
</style>
