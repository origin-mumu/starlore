<script setup lang="ts">
import { RouterView, useRoute, useRouter } from 'vue-router'

import navbar from './components/navbar.vue'
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

const isAppReady = ref(false)
const router = useRouter()
const LIVE2D_ROOT = '/live2d'
const route = useRoute()
const APP_LOADING_MIN_MS = 120
const ICP_RECORD_NUMBER = '豫ICP备2026009410号'
const MIIT_URL = 'https://beian.miit.gov.cn/'
/** 公安备案号（与工信部备案可同时展示） */
const PSB_RECORD_NUMBER = '苏公网安备32021402004612号'
const PSB_QUERY_URL =
  'http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=32021402004612'

declare global {
  interface Window {
    initWidget?: (config: {
      waifuPath: string
      cdnPath: string
      tools?: string[]
      dragEnable?: boolean
      dragDirection?: Array<'x' | 'y'>
      switchType?: 'order' | 'random'
    }) => void
    __live2dWidgetInited?: boolean
  }
}

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

const loadExternalResource = (url: string, type: 'css' | 'js') => {
  return new Promise<void>((resolve, reject) => {
    if (type === 'css') {
      const exists = document.querySelector(`link[href="${url}"]`)
      if (exists) {
        resolve()
        return
      }
      const link = document.createElement('link')
      link.rel = 'stylesheet'
      link.href = url
      link.onload = () => resolve()
      link.onerror = () => reject(new Error(`加载样式失败: ${url}`))
      document.head.appendChild(link)
      return
    }

    const exists = document.querySelector(`script[src="${url}"]`)
    if (exists) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = url
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`加载脚本失败: ${url}`))
    document.body.appendChild(script)
  })
}

const initLive2dWidget = async () => {
  if (window.__live2dWidgetInited) return
  if (window.innerWidth < 768) return

  try {
    await Promise.all([
      loadExternalResource(`${LIVE2D_ROOT}/waifu.css`, 'css'),
      loadExternalResource(`${LIVE2D_ROOT}/Core/live2dcubismcore.min.js`, 'js'),
      loadExternalResource(`${LIVE2D_ROOT}/live2d-sdk.js`, 'js'),
      loadExternalResource(`${LIVE2D_ROOT}/waifu-tips.js`, 'js'),
    ])

    if (typeof window.initWidget !== 'function') return

    window.initWidget({
      waifuPath: `${LIVE2D_ROOT}/waifu-tips.json`,
      cdnPath: `${LIVE2D_ROOT}/Resources/`,
      tools: [],
      dragEnable: false,
      switchType: 'order',
    })
    window.__live2dWidgetInited = true
  } catch (error) {
    console.error('初始化 Live2D 看板娘失败:', error)
  }
}

const updateLive2dVisibility = (_path: string) => {
  const shouldHide = false
  const waifu = document.getElementById('waifu')
  const waifuToggle = document.getElementById('waifu-toggle')

  if (waifu) waifu.style.display = shouldHide ? 'none' : ''
  if (waifuToggle) waifuToggle.style.display = shouldHide ? 'none' : ''
}

let waifuNavBound = false
const tryBindWaifuNavigate = () => {
  if (waifuNavBound) return
  const el = document.getElementById('waifu')
  if (!el) return
  waifuNavBound = true
  el.style.cursor = 'pointer'
  el.title = '点击进入 AI 助手'
  el.addEventListener('click', () => {
    if (route.path === '/echobot') return
    router.push({ name: 'echobot' })
  })
}

let waifuPollTimer: number | null = null

onMounted(() => {
  makeAppReadySoon()
  initLive2dWidget().then(() => {
    tryBindWaifuNavigate()
    let n = 0
    waifuPollTimer = window.setInterval(() => {
      tryBindWaifuNavigate()
      n++
      if (waifuNavBound || n > 40) {
        if (waifuPollTimer) clearInterval(waifuPollTimer)
        waifuPollTimer = null
      }
    }, 400)
  })
  updateLive2dVisibility(route.path)
})

onBeforeUnmount(() => {
  if (waifuPollTimer) clearInterval(waifuPollTimer)
})

watch(
  () => route.path,
  (path) => {
    updateLive2dVisibility(path)
  },
)
</script>

<template>
  <div class="app-container" :class="{ 'app-container--echobot': route.path === '/echobot' || route.path === '/vr' }">
    <div v-if="!isAppReady" class="loading-container">
      <div class="loading-spinner">
        <div class="spinner"></div>
        <p>加载中</p>
      </div>
    </div>
    <navbar v-show="route.path !== '/echobot' && route.path !== '/vr'" />
    <div
      class="router-outlet"
      :class="{ 'router-outlet--echobot': route.path === '/echobot' || route.path === '/vr' }"
    >
      <RouterView />
    </div>
    <footer v-show="route.path !== '/vr'" class="site-footer" :class="{ 'site-footer--echobot': route.path === '/echobot' }">
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
  height: 100vh;
  background: rgba(252, 253, 255, 0.75);
  backdrop-filter: blur(6px);
  z-index: 9999;
  pointer-events: none;
}

.loading-spinner {
  text-align: center;
  color: #3a4c63;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(58, 76, 99, 0.18);
  border-top: 3px solid #5f7fb2;
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
  position: relative;
  margin-top: auto;
  text-align: center;
  padding: 1.05rem 1rem 1.2rem;
  font-size: 0.86rem;
  color: #6b7a90;
  overflow: hidden;
  background-color: #A8CFFF;
}

.site-footer::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    radial-gradient(circle, rgba(255, 255, 255, 0.95) 0.8px, transparent 1.2px),
    radial-gradient(circle, rgba(255, 255, 255, 0.72) 1px, transparent 1.4px),
    radial-gradient(circle, rgba(255, 255, 255, 0.86) 0.9px, transparent 1.3px);
  background-size: 145px 80px, 180px 95px, 210px 105px;
  background-position: 0 0, 40px 28px, 90px 18px;
  opacity: 0.45;
}

/* 与 EchobotView 主背景同一套渐变，避免底栏偏艳蓝 */
.site-footer--echobot {
  /* background-color: transparent; */
  background-image: #cfdcf5;
  color: #5a7394;
}

.site-footer--echobot::before {
  opacity: 0.32;
}

.site-footer a {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0.28rem 0.9rem;
  color: #5e7090;
  text-decoration: none;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.43);
  box-shadow:
    0 8px 20px -14px rgba(33, 49, 74, 0.6),
    inset 0 1px 0 rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(4px);
  transition: all 0.22s ease;
}

.site-footer a:hover {
  color: #2f558f;
  border-color: rgba(171, 197, 235, 0.85);
  background: rgba(255, 255, 255, 0.62);
  transform: translateY(-1px);
}

/* 关闭 waifu.css 的 bottom/transform 过渡，避免离开 Echobot 时缩回右下角的动画 */
:global(#waifu) {
  right: 24px !important;
  left: auto !important;
  top: auto !important;
  bottom: 0 !important;
  transition: none !important;
}

/* AI 助手页：左侧居中放大，隐藏头顶气泡与开关 */
:global(html.echobot-route #waifu-tips) {
  display: none !important;
  visibility: hidden !important;
  opacity: 0 !important;
  pointer-events: none !important;
}

/* bottom 贴备案栏上沿；flex 将 canvas 贴容器底部，避免画布上方留白、脚下一大块空 */
/* transition:none 抵消 waifu.css 的 bottom 3s 入场 */
:global(html.echobot-route #waifu) {
  display: flex !important;
  flex-direction: column !important;
  justify-content: flex-end !important;
  align-items: center !important;
  right: auto !important;
  left: calc(50vw - 200px) !important;
  bottom: calc(5rem + 4px) !important;
  transform: translateX(-50%) scale(1.78) !important;
  transform-origin: bottom center !important;
  z-index: 20 !important;
  max-height: calc(100vh - 5rem - 56px) !important;
  width: auto !important;
  margin-bottom: 0 !important;
  transition: none !important;
  overflow: hidden !important;
  pointer-events: auto !important;
}

:global(html.echobot-route #waifu #live2d) {
  display: block !important;
  flex-shrink: 0 !important;
}

:global(html.echobot-route #waifu:hover) {
  transform: translateX(-50%) scale(1.78) !important;
}

@media (max-width: 900px) {
  :global(html.echobot-route #waifu) {
    left: 50% !important;
    transform: translateX(-50%) scale(1.52) !important;
    bottom: calc(5rem + 2px) !important;
    max-height: calc(100vh - 5rem - 100px) !important;
  }

  :global(html.echobot-route #waifu:hover) {
    transform: translateX(-50%) scale(1.52) !important;
  }
}

:global(html.echobot-route #waifu-toggle) {
  display: none !important;
}

:global(html.echobot-streaming #waifu) {
  animation: waifuThinking 1.15s ease-in-out infinite;
}

@keyframes waifuThinking {
  0%,
  100% {
    filter: brightness(1);
  }
  50% {
    filter: brightness(1.12) drop-shadow(0 0 14px rgba(95, 127, 178, 0.45));
  }
}
</style>
