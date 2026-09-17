<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Odometer,
  DataAnalysis,
  Document,
  FolderOpened,
  User,
  MagicStick,
  Lock,
  SwitchButton,
  Moon,
  Sunny,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

interface NavItem {
  path: string
  label: string
  icon: typeof Odometer
}

const NAV_ITEMS: NavItem[] = [
  { path: '/', label: '总览', icon: Odometer },
  { path: '/admin/ai-usage', label: 'AI 用量', icon: DataAnalysis },
  { path: '/admin/articles', label: '星记管理', icon: Document },
  { path: '/admin/categories', label: '星域管理', icon: FolderOpened },
  { path: '/admin/users', label: '观星者', icon: User },
  { path: '/admin/ai-config', label: 'AI 配置', icon: MagicStick },
  { path: '/admin/login-logs', label: '登录日志', icon: Lock },
]

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isDark = ref(document.documentElement.classList.contains('dark'))

function toggleTheme(): void {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('starlore-admin-theme', isDark.value ? 'dark' : 'light')
}

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

async function handleLogout(): Promise<void> {
  auth.logout()
  ElMessage.success('已退出登录')
  void router.replace('/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="app-sidebar">
      <div class="brand">
        <div class="brand-mark">星</div>
        <div class="brand-text">
          <span class="brand-name">Starlore</span>
          <span class="brand-sub">管理控制台</span>
        </div>
      </div>

      <nav class="nav-list">
        <RouterLink
          v-for="item in NAV_ITEMS"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="admin-chip">
          <span class="admin-name">{{ auth.user?.username ?? 'admin' }}</span>
          <span class="admin-role">管理员</span>
        </div>
      </div>
    </aside>

    <div class="main-stage">
      <header class="top-bar">
        <h1 class="page-title">{{ (route.meta.title as string) ?? '总览' }}</h1>
        <div class="top-actions">
          <button class="ghost-btn" :title="isDark ? '切换浅色' : '切换深色'" @click="toggleTheme">
            <el-icon><component :is="isDark ? Sunny : Moon" /></el-icon>
          </button>
          <button class="ghost-btn danger" title="退出登录" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
          </button>
        </div>
      </header>
      <main class="page-viewport">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
@import './admin-shell.css';
</style>
