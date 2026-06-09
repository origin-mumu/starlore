<template>
  <nav class="sidebar">
    <div class="sidebar-header">
      <div class="brand-mark"></div>
      <h2 class="logo">Starlore</h2>
      <span class="logo-sub">后台管理</span>
    </div>

    <ul class="nav-menu">
      <li class="nav-item">
        <router-link to="/" class="nav-link" active-class="active">
          <span class="nav-icon">📊</span>
          <span class="nav-text">仪表盘</span>
        </router-link>
      </li>

      <li class="nav-item">
        <router-link to="/admin/articles" class="nav-link" active-class="active">
          <span class="nav-icon">📝</span>
          <span class="nav-text">星迹管理</span>
        </router-link>
      </li>

      <li class="nav-item">
        <router-link to="/admin/categories" class="nav-link" active-class="active">
          <span class="nav-icon">📂</span>
          <span class="nav-text">星域管理</span>
        </router-link>
      </li>

      <li class="nav-item">
        <router-link to="/admin/users" class="nav-link" active-class="active">
          <span class="nav-icon">👥</span>
          <span class="nav-text">观星者管理</span>
        </router-link>
      </li>

      <li class="nav-item">
        <router-link to="/admin/ai-config" class="nav-link" active-class="active">
          <span class="nav-icon">🤖</span>
          <span class="nav-text">AI 配置</span>
        </router-link>
      </li>

      <li class="nav-item">
        <router-link to="/admin/login-logs" class="nav-link" active-class="active">
          <span class="nav-icon">📋</span>
          <span class="nav-text">登录日志</span>
        </router-link>
      </li>
    </ul>

    <div class="sidebar-footer">
      <a href="#" class="nav-link logout-link" @click="handleLogout">
        <span class="nav-icon">🚪</span>
        <span class="nav-text">退出登录</span>
      </a>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const handleLogout = () => {
  localStorage.removeItem('isLoggedIn')
  localStorage.removeItem('ro_blog_admin_token')
  router.push('/login')
}
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width, 240px);
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  background: var(--surface, #FDFBF5);
  border-right: 1px solid var(--border, #E6DCCB);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-sidebar, 4px 0 24px oklch(0.25 0.02 50 / 0.08));
}

.sidebar-header {
  padding: 28px 24px 20px;
  border-bottom: 1px solid var(--border, #E6DCCB);
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.brand-mark {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--accent, #B85C38);
  flex-shrink: 0;
}

.logo {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--ink, #2A2118);
  letter-spacing: -0.02em;
  font-family: 'Source Serif 4', 'LXGW WenKai', serif;
}

.logo-sub {
  font-size: 0.72rem;
  color: var(--ink-muted, #9E8E7A);
  font-weight: 500;
  letter-spacing: 0.03em;
  margin-left: auto;
}

.nav-menu {
  list-style: none;
  padding: 16px 12px;
  margin: 0;
  flex: 1;
  overflow-y: auto;
}

.nav-item {
  margin: 2px 0;
}

.nav-link {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  color: var(--ink-soft, #6B5D4D);
  text-decoration: none;
  border-radius: var(--radius-md, 10px);
  transition: all var(--transition, 0.25s cubic-bezier(0.25, 1, 0.5, 1));
  font-weight: 500;
  font-size: 0.9rem;
}

.nav-link:hover {
  background: var(--canvas-deep, #F2EBD9);
  color: var(--ink, #2A2118);
}

.nav-link.active {
  background: var(--accent-soft, #F0DDD0);
  color: var(--accent, #B85C38);
  font-weight: 600;
}

.nav-icon {
  font-size: 1.1rem;
  margin-right: 12px;
  width: 22px;
  text-align: center;
  flex-shrink: 0;
}

.nav-text {
  font-size: 0.9rem;
  letter-spacing: -0.01em;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--border, #E6DCCB);
}

.logout-link {
  color: var(--ink-muted, #9E8E7A);
  font-size: 0.85rem;
}

.logout-link:hover {
  color: var(--status-error-text, #8B3A2A);
  background: var(--status-error-bg, #F5D6D0);
}

@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    height: auto;
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
    box-shadow: var(--shadow-sm);
  }

  .sidebar-header {
    padding: 12px 16px;
    border-bottom: none;
    border-right: 1px solid var(--border);
  }

  .nav-menu {
    display: flex;
    padding: 8px;
    overflow-x: auto;
    gap: 4px;
  }

  .nav-item {
    margin: 0;
  }

  .nav-link {
    white-space: nowrap;
    padding: 8px 12px;
    font-size: 0.82rem;
  }

  .sidebar-footer {
    border-top: none;
    border-left: 1px solid var(--border);
    padding: 8px;
  }
}
</style>
