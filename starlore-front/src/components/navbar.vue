<script setup lang="ts">
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { Sun, Moon } from '@lucide/vue'

const router = useRouter()
const userStore = useUserStore()
const route = useRoute()

const navItems = computed(() => {
  const items: { name: string; path: string }[] = [{ name: '首页', path: '/' }]
  if (!userStore.isLoggedIn) {
    items.push({ name: '关于我', path: '/about' })
  }
  items.push({ name: '星记', path: '/articles' })
  items.push({ name: '记忆', path: '/knowledge-memory' })
  items.push(
    { name: '星域', path: '/categories' },
    { name: '探索', path: '/vr' },
    { name: '灵感', path: '/diverge' },
    { name: 'AI', path: '/echobot' },
    { name: '工坊', path: '/companion-studio' }
  )
  return items
})

const isActive = (path: string) => route.path === path

const mobileMenuOpen = ref(false)

const themeStore = useThemeStore()

const guestAllowedPaths = ['/', '/about', '/articles', '/categories', '/vr', '/companion-studio', '/companion']

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
  { name: '记忆', path: '/knowledge-memory' },
  { name: 'AI', path: '/echobot' },
]

const mobileMoreItems = computed(() => {
  const all = [
    { name: '关于我', path: '/about' },
    { name: '工坊', path: '/companion-studio' },
    { name: '灵感', path: '/diverge' },
    { name: '星域', path: '/categories' },
    { name: '探索', path: '/vr' },
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

          <button
            class="theme-toggle-btn"
            @click="themeStore.toggleTheme()"
            :title="themeStore.current === 'dark' ? '切换至浅色模式' : '切换至深色模式'"
            :aria-label="themeStore.current === 'dark' ? '切换至浅色模式' : '切换至深色模式'"
          >
            <Sun v-if="themeStore.current === 'dark'" class="theme-icon" :size="16" />
            <Moon v-else class="theme-icon" :size="16" />
          </button>

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

<style scoped src="./navbar.css"></style>

