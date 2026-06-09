<template>
  <div class="notfound-page">
    <!-- Decorative background orbs -->
    <div class="ink-orbs" aria-hidden="true">
      <div class="ink-orb ink-orb--1"></div>
      <div class="ink-orb ink-orb--2"></div>
      <div class="ink-orb ink-orb--3"></div>
    </div>

    <div class="container">
      <div class="notfound-content ink-glass-card fade-in-up">
        <div class="error-code">404</div>
        <h1 class="error-title">页面未找到</h1>
        <p class="error-description">抱歉，您访问的页面不存在或已被移动。</p>
        <button class="btn-primary" @click="goHome">返回首页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
/* ── Page ── */
.notfound-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  padding: 20px;
  overflow: hidden;
}

/* ── Background orbs (scoped copy) ── */
.ink-orbs {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.ink-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.ink-orb--1 {
  width: 520px;
  height: 520px;
  background: #E8C4A0;
  top: -180px;
  right: -120px;
  opacity: 0.75;
  filter: blur(110px);
  animation: floatOrb1 14s ease-in-out infinite;
}

.ink-orb--2 {
  width: 380px;
  height: 380px;
  background: #F0D9C4;
  bottom: -100px;
  left: -80px;
  opacity: 0.75;
  filter: blur(110px);
  animation: floatOrb2 18s ease-in-out infinite;
}

.ink-orb--3 {
  width: 440px;
  height: 440px;
  background: #FFE7D1;
  top: 55%;
  right: -140px;
  opacity: 0.75;
  filter: blur(110px);
  animation: floatOrb3 16s ease-in-out infinite;
}

@keyframes floatOrb1 {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(30px, 20px); }
  66% { transform: translate(-20px, -15px); }
}

@keyframes floatOrb2 {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(-25px, 15px); }
  66% { transform: translate(20px, -20px); }
}

@keyframes floatOrb3 {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(15px, -25px); }
  66% { transform: translate(-30px, 10px); }
}

/* ── Content Card (glass) ── */
.notfound-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 500px;
  padding: 56px 48px;
}

/* ── 404 Number ── */
.error-code {
  font-size: 6rem;
  font-weight: 800;
  background: var(--accent);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 16px;
  letter-spacing: -0.03em;
}

.error-title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 12px;
  letter-spacing: -0.015em;
}

.error-description {
  font-size: 17px;
  color: var(--ink-muted);
  margin-bottom: 36px;
  line-height: 1.6;
}

/* ── Fade-in animation ── */
.fade-in-up {
  opacity: 0;
  transform: translateY(24px);
  animation: fadeInUp 0.7s ease-out forwards;
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .error-code {
    font-size: 4rem;
  }

  .error-title {
    font-size: 1.5rem;
  }

  .notfound-content {
    padding: 40px 28px;
  }
}
</style>
