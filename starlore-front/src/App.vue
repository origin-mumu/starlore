<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'

import navbar from './components/navbar.vue'
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
    <!-- 全局背景装饰 -->
    <!-- Starfield -->
    <div class="ink-stars" aria-hidden="true">
      <div class="ink-stars__layer ink-stars__layer--sm"></div>
      <div class="ink-stars__layer ink-stars__layer--md"></div>
      <div class="ink-stars__layer ink-stars__layer--lg"></div>
    </div>
    <!-- Orbs + particles -->
    <div class="ink-orbs" aria-hidden="true">
      <div class="ink-orb ink-orb--1"></div>
      <div class="ink-orb ink-orb--2"></div>
      <div class="ink-orb ink-orb--3"></div>
      <div class="orb-particle orb-particle--1a"></div>
      <div class="orb-particle orb-particle--1b"></div>
      <div class="orb-particle orb-particle--1c"></div>
      <div class="orb-particle orb-particle--1d"></div>
      <div class="orb-particle orb-particle--1e"></div>
      <div class="orb-particle orb-particle--2a"></div>
      <div class="orb-particle orb-particle--2b"></div>
      <div class="orb-particle orb-particle--2c"></div>
      <div class="orb-particle orb-particle--2d"></div>
      <div class="orb-particle orb-particle--2e"></div>
      <div class="orb-particle orb-particle--3a"></div>
      <div class="orb-particle orb-particle--3b"></div>
      <div class="orb-particle orb-particle--3c"></div>
      <div class="orb-particle orb-particle--3d"></div>
      <div class="orb-particle orb-particle--3e"></div>
    </div>
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
/* ── Starfield ── */
.ink-stars {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -2;
  overflow: hidden;
}

.ink-stars__layer {
  position: absolute;
  inset: 0;
  width: 1px;
  height: 1px;
  border-radius: 50%;
}

/* 小星：密集、微弱闪烁 */
.ink-stars__layer--sm {
  background: transparent;
  box-shadow:
    2vw 3vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    5vw 18vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    8vw 42vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    11vw 7vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    14vw 63vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    17vw 85vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    20vw 28vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    23vw 52vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    26vw 91vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    29vw 12vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    32vw 75vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    35vw 38vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    38vw 62vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    41vw 8vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    44vw 48vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    47vw 82vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    50vw 22vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    53vw 55vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    56vw 95vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    59vw 15vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    62vw 72vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    65vw 35vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    68vw 58vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    71vw 5vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    74vw 88vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    77vw 42vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    80vw 68vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    83vw 18vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    86vw 78vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    89vw 32vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    92vw 92vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    95vw 52vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    98vw 25vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    3vw 95vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    7vw 32vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    13vw 78vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    18vw 5vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    22vw 45vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    27vw 68vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    33vw 92vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    37vw 15vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    42vw 85vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    48vw 28vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    54vw 72vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    60vw 3vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    66vw 48vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    72vw 92vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    78vw 22vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    84vw 58vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    90vw 82vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    96vw 12vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    4vw 55vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    10vw 35vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    16vw 88vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    25vw 65vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    31vw 8vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    40vw 42vh 0 0 var(--star-color, rgba(180,170,160,0.35)),
    55vw 38vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    63vw 82vh 0 0 var(--star-color, rgba(180,170,160,0.3)),
    75vw 52vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    88vw 68vh 0 0 var(--star-color, rgba(180,170,160,0.35));
  animation: twinkleSm 4s ease-in-out infinite alternate;
}

/* 中星：适中密度 */
.ink-stars__layer--md {
  width: 2px;
  height: 2px;
  box-shadow:
    4vw 12vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    12vw 48vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    20vw 82vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    28vw 22vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    36vw 65vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    44vw 8vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    52vw 55vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    60vw 38vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    68vw 92vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    76vw 18vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    84vw 72vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    92vw 42vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    8vw 88vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    16vw 32vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    24vw 58vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    32vw 75vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    40vw 15vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    48vw 62vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    56vw 28vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    64vw 85vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    72vw 5vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    80vw 48vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    88vw 22vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    96vw 78vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    6vw 35vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    18vw 72vh 0 0 var(--star-color, rgba(180,170,160,0.45)),
    30vw 45vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    42vw 92vh 0 0 var(--star-color, rgba(180,170,160,0.4)),
    58vw 68vh 0 0 var(--star-color, rgba(180,170,160,0.5)),
    82vw 35vh 0 0 var(--star-color, rgba(180,170,160,0.45));
  animation: twinkleMd 6s ease-in-out infinite alternate;
}

/* 大星：稀疏、明亮闪烁 */
.ink-stars__layer--lg {
  width: 3px;
  height: 3px;
  box-shadow:
    7vw 22vh 0 0 var(--star-color, rgba(180,170,160,0.7)),
    19vw 55vh 0 0 var(--star-color, rgba(180,170,160,0.6)),
    31vw 88vh 0 0 var(--star-color, rgba(180,170,160,0.7)),
    43vw 12vh 0 0 var(--star-color, rgba(180,170,160,0.6)),
    55vw 45vh 0 0 var(--star-color, rgba(180,170,160,0.7)),
    67vw 78vh 0 0 var(--star-color, rgba(180,170,160,0.6)),
    79vw 32vh 0 0 var(--star-color, rgba(180,170,160,0.7)),
    91vw 62vh 0 0 var(--star-color, rgba(180,170,160,0.6)),
    15vw 8vh 0 0 var(--star-color, rgba(180,170,160,0.7)),
    37vw 72vh 0 0 var(--star-color, rgba(180,170,160,0.65)),
    50vw 92vh 0 0 var(--star-color, rgba(180,170,160,0.7)),
    62vw 18vh 0 0 var(--star-color, rgba(180,170,160,0.6)),
    74vw 58vh 0 0 var(--star-color, rgba(180,170,160,0.7)),
    86vw 85vh 0 0 var(--star-color, rgba(180,170,160,0.65)),
    97vw 38vh 0 0 var(--star-color, rgba(180,170,160,0.7));
  animation: twinkleLg 6s ease-in-out infinite alternate;
}

@keyframes twinkleSm {
  0%   { opacity: 0.4; }
  50%  { opacity: 0.8; }
  100% { opacity: 0.5; }
}

@keyframes twinkleMd {
  0%   { opacity: 0.35; }
  40%  { opacity: 0.75; }
  70%  { opacity: 0.5; }
  100% { opacity: 0.8; }
}

@keyframes twinkleLg {
  0%   { opacity: 0.5; }
  30%  { opacity: 1; }
  70%  { opacity: 0.6; }
  100% { opacity: 0.9; }
}

/* ── 全局背景浮光 orbs ── */
.ink-orbs {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -1;
  overflow: hidden;
}

.ink-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.ink-orb::after {
  content: '';
  position: absolute;
  inset: -20%;
  border-radius: 50%;
  border: 1.5px solid var(--orb-glow, rgba(255,255,255,0.15));
  animation: haloGlow 5s ease-in-out infinite alternate;
}

@keyframes haloGlow {
  0%   { opacity: 0.3; transform: scale(1); }
  50%  { opacity: 0.7; transform: scale(1.03); }
  100% { opacity: 0.4; transform: scale(0.98); }
}

.ink-orb--1 {
  width: 520px;
  height: 520px;
  background: var(--orb-1);
  top: -180px;
  right: -120px;
  opacity: 0.75;
  filter: blur(110px);
  animation: floatOrb1 14s ease-in-out infinite;
}
.ink-orb--1::after {
  border-color: var(--orb-glow, rgba(255,255,255,0.15));
  animation-delay: 0s;
  animation-duration: 5s;
}

.ink-orb--2 {
  width: 380px;
  height: 380px;
  background: var(--orb-2);
  bottom: -100px;
  left: -80px;
  opacity: 0.75;
  filter: blur(110px);
  animation: floatOrb2 18s ease-in-out infinite;
}
.ink-orb--2::after {
  border-color: var(--orb-glow, rgba(255,255,255,0.12));
  animation-delay: 1.8s;
  animation-duration: 6s;
}

.ink-orb--3 {
  width: 440px;
  height: 440px;
  background: var(--orb-3);
  top: 55%;
  right: -140px;
  opacity: 0.75;
  filter: blur(110px);
  animation: floatOrb3 16s ease-in-out infinite;
}
.ink-orb--3::after {
  border-color: var(--orb-glow, rgba(255,255,255,0.13));
  animation-delay: 3.2s;
  animation-duration: 5.5s;
}

@keyframes floatOrb1 {
  0%, 100% { transform: translate(0, 0); }
  33%      { transform: translate(30px, 20px); }
  66%      { transform: translate(-20px, -15px); }
}

@keyframes floatOrb2 {
  0%, 100% { transform: translate(0, 0); }
  33%      { transform: translate(-25px, 15px); }
  66%      { transform: translate(20px, -20px); }
}

@keyframes floatOrb3 {
  0%, 100% { transform: translate(0, 0); }
  33%      { transform: translate(15px, -25px); }
  66%      { transform: translate(-30px, 10px); }
}

/* ── Orb Particles ── */
.orb-particle {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  opacity: 0;
  animation-fill-mode: both;
}

/* Orb-1 particles (top-right) */
.orb-particle--1a { width: 4px; height: 4px; background: var(--orb-1); top: 60px; right: 40px; animation: particleDrift1 8s ease-in-out infinite 0s; }
.orb-particle--1b { width: 3px; height: 3px; background: var(--orb-1); top: 30px; right: 160px; animation: particleDrift2 10s ease-in-out infinite 1.5s; }
.orb-particle--1c { width: 5px; height: 5px; background: var(--orb-1); top: 140px; right: 20px; animation: particleDrift3 9s ease-in-out infinite 3s; }
.orb-particle--1d { width: 3px; height: 3px; background: var(--orb-1); top: 100px; right: 100px; animation: particleDrift1 11s ease-in-out infinite 2s; }
.orb-particle--1e { width: 4px; height: 4px; background: var(--orb-1); top: 20px; right: 80px; animation: particleDrift2 7s ease-in-out infinite 4s; }

/* Orb-2 particles (bottom-left) */
.orb-particle--2a { width: 4px; height: 4px; background: var(--orb-2); bottom: 40px; left: 50px; animation: particleDrift2 9s ease-in-out infinite 0.5s; }
.orb-particle--2b { width: 3px; height: 3px; background: var(--orb-2); bottom: 80px; left: 20px; animation: particleDrift3 11s ease-in-out infinite 2s; }
.orb-particle--2c { width: 5px; height: 5px; background: var(--orb-2); bottom: 20px; left: 140px; animation: particleDrift1 8s ease-in-out infinite 3.5s; }
.orb-particle--2d { width: 3px; height: 3px; background: var(--orb-2); bottom: 120px; left: 80px; animation: particleDrift2 10s ease-in-out infinite 1s; }
.orb-particle--2e { width: 4px; height: 4px; background: var(--orb-2); bottom: 60px; left: 100px; animation: particleDrift3 7s ease-in-out infinite 4.5s; }

/* Orb-3 particles (middle-right) */
.orb-particle--3a { width: 4px; height: 4px; background: var(--orb-3); top: 52%; right: 60px; animation: particleDrift3 8s ease-in-out infinite 1s; }
.orb-particle--3b { width: 3px; height: 3px; background: var(--orb-3); top: 58%; right: 20px; animation: particleDrift1 10s ease-in-out infinite 2.5s; }
.orb-particle--3c { width: 5px; height: 5px; background: var(--orb-3); top: 48%; right: 120px; animation: particleDrift2 9s ease-in-out infinite 0s; }
.orb-particle--3d { width: 3px; height: 3px; background: var(--orb-3); top: 62%; right: 80px; animation: particleDrift3 11s ease-in-out infinite 3.5s; }
.orb-particle--3e { width: 4px; height: 4px; background: var(--orb-3); top: 55%; right: 150px; animation: particleDrift1 7s ease-in-out infinite 5s; }

@keyframes particleDrift1 {
  0%   { opacity: 0; transform: translate(0, 0) scale(0.5); }
  15%  { opacity: 0.8; }
  50%  { opacity: 0.6; transform: translate(20px, -30px) scale(1); }
  85%  { opacity: 0.8; }
  100% { opacity: 0; transform: translate(-10px, -50px) scale(0.5); }
}

@keyframes particleDrift2 {
  0%   { opacity: 0; transform: translate(0, 0) scale(0.5); }
  15%  { opacity: 0.7; }
  50%  { opacity: 0.5; transform: translate(-25px, 20px) scale(1.1); }
  85%  { opacity: 0.7; }
  100% { opacity: 0; transform: translate(15px, -40px) scale(0.5); }
}

@keyframes particleDrift3 {
  0%   { opacity: 0; transform: translate(0, 0) scale(0.4); }
  15%  { opacity: 0.9; }
  50%  { opacity: 0.5; transform: translate(15px, 25px) scale(1); }
  85%  { opacity: 0.7; }
  100% { opacity: 0; transform: translate(-20px, -35px) scale(0.6); }
}

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

</style>
