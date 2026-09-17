<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import type { Component } from 'vue'
import {
  Odometer,
  Document,
  FolderOpened,
  User,
  Cpu,
  Tickets,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

interface NavItem {
  name: string
  path: string
  label: string
  icon: Component
}

const NAV_ITEMS: NavItem[] = [
  { name: 'home', path: '/', label: '仪表盘', icon: Odometer },
  { name: 'articles', path: '/admin/articles', label: '星记管理', icon: Document },
  { name: 'categories', path: '/admin/categories', label: '星域管理', icon: FolderOpened },
  { name: 'users', path: '/admin/users', label: '观星者管理', icon: User },
  { name: 'ai-config', path: '/admin/ai-config', label: 'AI 配置', icon: Cpu },
  { name: 'loginLogs', path: '/admin/login-logs', label: '登录日志', icon: Tickets },
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const handleLogout = (): void => {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar__header">
      <div class="sidebar__brand-mark">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" aria-hidden="true">
          <path
            d="M12 2l2.4 6.3L21 9.3l-5 4.4 1.5 6.6L12 16.8 6.5 20.3 8 13.7 3 9.3l6.6-1L12 2z"
          />
        </svg>
      </div>
      <div class="sidebar__brand-text">
        <h2 class="sidebar__logo">Starlore</h2>
        <span class="sidebar__logo-sub">后台管理</span>
      </div>
    </div>

    <nav class="sidebar__nav">
      <router-link
        v-for="item in NAV_ITEMS"
        :key="item.name"
        :to="item.path"
        class="sidebar__link"
        active-class="is-active"
      >
        <el-icon :size="17" class="sidebar__icon"><component :is="item.icon" /></el-icon>
        <span class="sidebar__text">{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="sidebar__footer">
      <button class="sidebar__link sidebar__logout" type="button" @click="handleLogout">
        <el-icon :size="17" class="sidebar__icon"><SwitchButton /></el-icon>
        <span class="sidebar__text">退出登录</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
/* 悬浮胶囊侧边栏：脱离顶底、独立圆角悬浮、毛玻璃材质 */
.sidebar {
  position: sticky;
  top: 16px;
  width: var(--sidebar-width);
  height: calc(100vh - 32px);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--surface-glass);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: var(--border-glass);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.sidebar__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 22px 20px 18px;
}

.sidebar__brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--brand-primary), var(--brand-secondary));
  color: #fff;
  box-shadow: var(--shadow-button);
}

.sidebar__brand-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sidebar__logo {
  font-size: 1.08rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.sidebar__logo-sub {
  font-size: 0.7rem;
  font-weight: 500;
  color: var(--text-muted);
  letter-spacing: 0.08em;
}

.sidebar__nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 8px 14px;
  overflow-y: auto;
}

.sidebar__link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 0.9rem;
  font-weight: 500;
  text-decoration: none;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.sidebar__link:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
}

.sidebar__link:hover .sidebar__icon {
  transform: translateY(-1px);
}

.sidebar__link.is-active {
  background: var(--brand-subtle);
  color: var(--brand-primary);
  font-weight: 600;
}

/* 激活态左侧 3px 品牌光条 */
.sidebar__link.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: var(--radius-pill);
  background: linear-gradient(180deg, var(--brand-primary), var(--brand-secondary));
}

.sidebar__icon {
  flex-shrink: 0;
  transition: transform var(--transition-fast);
}

.sidebar__footer {
  padding: 12px 14px;
}

.sidebar__logout {
  width: 100%;
  color: var(--text-muted);
  font-family: inherit;
}

.sidebar__logout:hover {
  background: var(--color-danger-subtle);
  color: var(--color-danger);
}

@media (max-width: 768px) {
  .sidebar {
    position: static;
    width: 100%;
    height: auto;
  }

  .sidebar__nav {
    flex-direction: row;
    overflow-x: auto;
  }

  .sidebar__link.is-active::before {
    display: none;
  }

  .sidebar__footer {
    display: none;
  }
}
</style>
