<script setup lang="ts">
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const userStore = useUserStore()
const route = useRoute()

const navItems = computed(() => {
  const items: { name: string; path: string }[] = [
    { name: '首页', path: '/' },
    { name: '星记', path: '/articles' },
  ]
  if (!userStore.isLoggedIn) {
    items.push({ name: '关于我', path: '/about' })
  }
  items.push(
    { name: '星域', path: '/categories' },
    { name: '探索', path: '/vr' },
    { name: '灵感', path: '/diverge' },
    { name: 'AI', path: '/echobot' }
  )
  return items
})

const isActive = (path: string) => route.path === path

const mobileMenuOpen = ref(false)

const themeStore = useThemeStore()

const themes: {
  name: 'default' | 'dark' | 'green' | 'blue'
  label: string
  color: string
}[] = [
  { name: 'green', label: '春暖', color: '#35BFAB' },
  { name: 'default', label: '秋实', color: '#DE4331' },
  { name: 'blue', label: '晴空', color: '#2FCBE7' },
  { name: 'dark', label: '深夜', color: '#2A48F3' },
]

const guestAllowedPaths = ['/', '/about', '/articles', '/categories', '/vr']

const handleLogout = () => {
  userStore.logout()
  mobileMenuOpen.value = false
  moreOpen.value = false
  const path = route.path
  const isGuestAllowed = guestAllowedPaths.some(p => path === p || path.startsWith(p + '/'))
  if (isGuestAllowed) {
    router.go(0)
  } else {
    router.push('/login')
  }
}

// ── Mobile bottom bar ──
const moreOpen = ref(false)

const mobileMainTabs = [
  { name: '首页', path: '/' },
  { name: '星记', path: '/articles' },
  { name: '探索', path: '/vr' },
  { name: 'AI', path: '/echobot' },
]

const mobileMoreItems = computed(() => {
  const all = [
    { name: '关于我', path: '/about' },
    { name: '灵感', path: '/diverge' },
    { name: '星域', path: '/categories' },
  ]
  return all
})
</script>

<template>
  <div>
    <!-- ── Desktop top navbar ── -->
    <nav class="navbar desktop-nav">
      <div class="nav-inner">
        <div class="nav-brand" @click="router.push('/')">
          <span class="brand-mark"></span>
          <span class="brand-text">Starlore</span>
        </div>

        <button class="mobile-toggle" @click="mobileMenuOpen = !mobileMenuOpen" aria-label="菜单">
          <span :class="{ open: mobileMenuOpen }"></span>
        </button>

        <div class="nav-links" :class="{ open: mobileMenuOpen }">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
            @click="mobileMenuOpen = false"
          >
            {{ item.name }}
          </RouterLink>

          <span class="nav-divider"></span>

          <RouterLink
            v-if="
              userStore.isLoggedIn && (userStore.role === 'member' || userStore.role === 'admin')
            "
            to="/resume"
            class="nav-link"
            :class="{ active: isActive('/resume') }"
            @click="mobileMenuOpen = false"
          >
            简历
          </RouterLink>

          <span class="nav-divider"></span>

          <div class="theme-switcher" title="切换主题">
            <button
              v-for="t in themes"
              :key="t.name"
              class="theme-dot"
              :class="{ active: themeStore.current === t.name }"
              :style="{ '--dot-color': t.color }"
              @click="themeStore.setTheme(t.name)"
              :aria-label="t.label"
            ></button>
          </div>

          <span class="nav-divider"></span>

          <div v-if="userStore.isLoggedIn" class="user-area">
            <span class="user-name" @click="router.push('/profile')">{{ userStore.nickname }}</span>
          </div>
          <RouterLink v-else to="/login" class="nav-link" @click="mobileMenuOpen = false"
            >登录</RouterLink
          >
        </div>
      </div>
    </nav>

    <!-- ── Mobile bottom bar (same pill style as desktop) ── -->
    <nav class="navbar mobile-nav">
      <div class="nav-inner">
        <RouterLink
          v-for="tab in mobileMainTabs"
          :key="tab.path"
          :to="tab.path"
          class="nav-link"
          :class="{ active: isActive(tab.path) }"
        >
          {{ tab.name }}
        </RouterLink>

        <!-- "更多" button toggles dropdown -->
        <button
          class="nav-link more-btn"
          :class="{ active: moreOpen }"
          @click="moreOpen = !moreOpen"
        >
          更多
        </button>

        <!-- more dropdown -->
        <div v-if="moreOpen" class="more-dropdown" @click.stop>
          <RouterLink
            v-for="item in mobileMoreItems"
            :key="item.path"
            :to="item.path"
            class="nav-link drop-item"
            :class="{ active: isActive(item.path) }"
            @click="moreOpen = false"
          >
            {{ item.name }}
          </RouterLink>

          <span class="nav-divider drop-divider"></span>

          <RouterLink
            v-if="
              userStore.isLoggedIn && (userStore.role === 'member' || userStore.role === 'admin')
            "
            to="/resume"
            class="nav-link drop-item"
            :class="{ active: isActive('/resume') }"
            @click="moreOpen = false"
          >
            简历
          </RouterLink>

          <div v-if="userStore.isLoggedIn" class="drop-user">
            <span
              class="drop-user-name"
              @click="router.push('/profile'); moreOpen = false"
              >{{ userStore.nickname }}</span
            >
          </div>
          <RouterLink v-else to="/login" class="nav-link drop-item" @click="moreOpen = false">
            登录
          </RouterLink>
        </div>
      </div>
    </nav>

    <!-- click outside to close more dropdown -->
    <div v-if="moreOpen" class="more-backdrop" @click="moreOpen = false"></div>
  </div>
</template>

<style scoped>
/* ── Shared Navbar Pill Style ── */
.navbar {
  position: fixed;
  z-index: 10001;
  background: var(--nav-bg);
  backdrop-filter: blur(16px);
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-card);
}

/* desktop: top-center */
.desktop-nav {
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
}

/* mobile: bottom-center */
.mobile-nav {
  display: none;
  bottom: 14px;
  left: 50%;
  transform: translateX(-50%);
}

.nav-inner {
  display: flex;
  align-items: center;
  gap: 2px;
  height: 46px;
  padding: 0 6px;
  position: relative;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  padding: 0 10px 0 6px;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  background: var(--accent);
  color: #fdfbf5;
  font-weight: 700;
  font-size: 0.8rem;
  border-radius: var(--radius-full);
  letter-spacing: -0.02em;
}

.brand-text {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.02em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2px;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--ink-soft);
  border-radius: 999px;
  transition: all var(--transition);
  letter-spacing: -0.005em;
  white-space: nowrap;
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  text-decoration: none;
}

.nav-link:hover {
  color: var(--ink);
  background: var(--canvas-deep);
}

.nav-link.active {
  color: var(--accent);
  font-weight: 600;
  background: var(--accent-soft);
}

.nav-divider {
  width: 1px;
  height: 16px;
  background: var(--border);
  margin: 0 6px;
}

/* ── Theme Switcher ── */
.theme-switcher {
  display: flex;
  align-items: center;
  gap: 6px;
}

.theme-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid transparent;
  background: var(--dot-color);
  cursor: pointer;
  padding: 0;
  transition: all 0.2s;
  opacity: 0.5;
}

.theme-dot:hover {
  opacity: 0.85;
  transform: scale(1.2);
}

.theme-dot.active {
  opacity: 1;
  border-color: var(--ink);
  box-shadow: 0 0 0 1px var(--canvas);
  transform: scale(1.15);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 8px;
  margin-right: 8px;
}

.user-name {
  font-size: 0.88rem;
  color: var(--ink-soft);
  cursor: pointer;
  transition: color var(--transition);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-name:hover {
  color: var(--accent);
}

.user-logout {
  font-size: 0.82rem;
  color: var(--ink-muted);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  transition: all var(--transition);
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
}

.user-logout:hover {
  color: #9b3a2a;
  background: oklch(0.55 0.15 25 / 0.08);
}

.mobile-toggle {
  display: none;
  width: 32px;
  height: 32px;
  position: relative;
  background: none;
  border: none;
  cursor: pointer;
}

.mobile-toggle span,
.mobile-toggle span::before,
.mobile-toggle span::after {
  display: block;
  width: 20px;
  height: 2px;
  background: var(--ink);
  border-radius: 1px;
  transition: all 0.3s ease;
  position: absolute;
  left: 6px;
}

.mobile-toggle span {
  top: 50%;
  transform: translateY(-50%);
}

.mobile-toggle span::before {
  content: '';
  top: -6px;
}

.mobile-toggle span::after {
  content: '';
  top: 6px;
}

.mobile-toggle span.open {
  background: transparent;
}

.mobile-toggle span.open::before {
  top: 0;
  transform: rotate(45deg);
}

.mobile-toggle span.open::after {
  top: 0;
  transform: rotate(-45deg);
}

/* ── More dropdown (fixed above mobile bottom bar) ── */
.more-dropdown {
  position: fixed;
  bottom: 74px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 2px;
  padding: 8px;
  background: var(--nav-bg);
  border-radius: 20px;
  border: 1px solid var(--border);
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.1);
  min-width: 140px;
  white-space: nowrap;
  z-index: 10002;
}

.drop-item {
  justify-content: center;
  padding: 10px 16px;
}

.drop-divider {
  width: 100%;
  height: 1px;
  margin: 4px 0;
}

.drop-user {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 16px;
}

.drop-user-name {
  font-size: 0.85rem;
  color: var(--ink-soft);
  cursor: pointer;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drop-user-name:hover {
  color: var(--accent);
}

/* backdrop to close dropdown */
.more-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  z-index: 99;
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .desktop-nav {
    display: none;
  }

  .mobile-nav {
    display: block;
  }

  .more-backdrop {
    display: block;
  }

  /* keep legacy mobile-menu styles in case still referenced */
  .mobile-toggle {
    display: block;
  }

  .nav-links {
    display: none;
    position: fixed;
    top: 68px;
    left: 50%;
    transform: translateX(-50%);
    width: calc(100% - 32px);
    max-width: 360px;
    background: oklch(0.97 0.01 80 / 0.98);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    flex-direction: column;
    align-items: stretch;
    padding: 12px;
    gap: 2px;
    border-radius: 20px;
    border: 1px solid var(--border);
    box-shadow: 0 8px 32px oklch(0.25 0.02 50 / 0.12);
  }

  .nav-links.open {
    display: flex;
  }

  .nav-link {
    padding: 10px 14px;
    border-radius: 999px;
    justify-content: center;
  }

  .nav-divider {
    width: 100%;
    height: 1px;
    margin: 6px 0;
  }

  .theme-switcher {
    justify-content: center;
    padding: 4px 0;
  }

  .user-area {
    margin-left: 0;
    padding: 4px 14px;
    justify-content: center;
  }
}
</style>
