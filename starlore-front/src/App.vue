<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'

import navbar from './components/navbar.vue'
import BlurredBubbles from './components/BlurredBubbles.vue'
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const isAppReady = ref(false)
const userStore = useUserStore()
const route = useRoute()
const APP_LOADING_MIN_MS = 120
const ICP_RECORD_NUMBER = '豫ICP备2026009410号'
const MIIT_URL = 'https://beian.miit.gov.cn/'
/** 公安备案号（与工信部备案可同时展示） */
const PSB_RECORD_NUMBER = '苏公网安备32021402004612号'
const PSB_QUERY_URL =
  'http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=32021402004612'






const makeAppReadySoon = () => {
  const start = performance.now()
  // 等待至少一帧，确保 DOM 已完成首次绘制
  requestAnimationFrame(() => {
    const elapsed = performance.now() - start
    const remain = Math.max(0, APP_LOADING_MIN_MS - elapsed)
    window.setTimeout(() => {
      isAppReady.value = true
    }, remain)
  })
}


onMounted(() => {
  makeAppReadySoon()
  // 尝试恢复登录状态
  if (userStore.isLoggedIn) {
    userStore.fetchCurrentUser()
  }
})

</script>

<template>
  <div class="app-container" :class="{ 'app-container--echobot': route.path === '/echobot' || route.path === '/vr' }">
    <!-- 全局背景装饰 - 模糊气泡 -->
    <BlurredBubbles />
    <div v-if="!isAppReady" class="loading-container">
      <div class="loading-spinner">
        <div class="spinner"></div>
        <p>加载中</p>
      </div>
    </div>
    <navbar v-show="route.path !== '/echobot' && route.path !== '/vr' && route.path !== '/login'" />
    <div
      class="router-outlet"
      :class="{ 'router-outlet--echobot': route.path === '/echobot' || route.path === '/vr' }"
    >
      <RouterView />
    </div>
    <footer v-show="route.path !== '/vr' && route.path !== '/diverge'" class="site-footer" :class="{ 'site-footer--echobot': route.path === '/echobot' }">
      <div class="footer-beian">
        <a :href="MIIT_URL" target="_blank" rel="noopener noreferrer">
          {{ ICP_RECORD_NUMBER }}
        </a>
        <span class="footer-beian-sep" aria-hidden="true">·</span>
        <a :href="PSB_QUERY_URL" target="_blank" rel="noopener noreferrer">
          {{ PSB_RECORD_NUMBER }}
        </a>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  background-repeat: no-repeat;
  padding-bottom: 0;
}

/* Echobot：整页一屏，避免 body 上下滚动；主内容区在中间自适应高度 */
.app-container--echobot {
  height: 100vh;
  max-height: 100vh;
  min-height: 100vh;
  overflow: hidden;
}

.router-outlet {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.router-outlet--echobot {
  min-height: 0;
  overflow: hidden;
}

:global(html.echobot-route),
:global(html.echobot-route body),
:global(html.echobot-route #app) {
  height: 100%;
  overflow: hidden;
}

.loading-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(250, 246, 238, 0.9);
  z-index: 9999;
  pointer-events: none;
}

.loading-spinner {
  text-align: center;
  color: var(--ink-soft);
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--border);
  border-top: 3px solid var(--accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-spinner p {
  margin: 0;
  font-size: 0.95rem;
}

.footer-beian {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.35rem 0.65rem;
}

.footer-beian-sep {
  opacity: 0.55;
  user-select: none;
}

.site-footer {
  margin-top: auto;
  text-align: center;
  padding: 40px 0 30px;
  font-size: 0.85rem;
  color: var(--ink-muted);
  background: transparent;
  border-top: 1px solid var(--border);
}

.site-footer--echobot {
  border-top-color: var(--border);
}

.site-footer a {
  color: var(--ink-muted);
  text-decoration: none;
  transition: color var(--transition);
}

.site-footer a:hover {
  color: var(--accent);
}

.site-footer--echobot a {
  color: var(--accent);
}

/* ── Mobile: bottom nav spacing ── */
@media (max-width: 768px) {
  .app-container {
    padding-bottom: 60px;
  }
  .site-footer {
    padding-bottom: 70px;
  }
}

</style>
