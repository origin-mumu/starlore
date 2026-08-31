<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft, Check, Sparkles } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import StudioWorkspace from '@/companion-studio/StudioWorkspace.vue'
import { useCompanionStore } from '@/stores/companion'

const router = useRouter()
const companionStore = useCompanionStore()

function handleGoBack() {
  router.push('/')
}

function handleSaveAndApply() {
  ElMessage.success({
    message: '✨ AI 伴侣形象已更新，全站已实时生效！',
    duration: 2500,
  })
  setTimeout(() => {
    router.push('/')
  }, 400)
}
</script>

<template>
  <div class="companion-studio-page">
    <!-- 顶部悬浮操作栏 -->
    <header class="studio-top-bar">
      <div class="top-bar-left">
        <button type="button" class="back-btn" @click="handleGoBack" title="返回首页">
          <ArrowLeft :size="17" />
          <span>返回首页</span>
        </button>
        <div class="brand-divider"></div>
        <div class="studio-title">
          <Sparkles :size="18" class="title-icon" />
          <h1>AI 伴侣工坊 · Companion Studio</h1>
        </div>
      </div>

      <div class="top-bar-right">
        <div class="live-sync-badge">
          <span class="pulse-dot"></span>
          <span>修改实时生效</span>
        </div>
        <button type="button" class="apply-save-btn" @click="handleSaveAndApply">
          <Check :size="16" />
          <span>应用并完成</span>
        </button>
      </div>
    </header>

    <!-- Studio 核心工作台 -->
    <main class="studio-main-container">
      <StudioWorkspace />
    </main>
  </div>
</template>

<style scoped>
@import '@/companion-studio/styles.css';

.companion-studio-page {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  background: var(--paper, #f9f9f9);
  color: var(--ink, #17203a);
  overflow: hidden;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 顶部操作条 */
.studio-top-bar {
  position: relative;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--line, #e3e5ea);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
}

.top-bar-left,
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid var(--line, #e3e5ea);
  border-radius: 999px;
  color: var(--ink, #17203a);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.back-btn:hover {
  background: rgba(0, 0, 0, 0.04);
  border-color: rgba(0, 0, 0, 0.15);
  transform: translateX(-1px);
}

.brand-divider {
  width: 1px;
  height: 20px;
  background: var(--line, #e3e5ea);
}

.studio-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.studio-title h1 {
  font-size: 0.95rem;
  font-weight: 700;
  margin: 0;
  letter-spacing: -0.01em;
  color: var(--ink, #17203a);
}

.title-icon {
  color: #3b82f6;
}

.live-sync-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 600;
  color: #059669;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: pulse-ring 2s infinite;
}

@keyframes pulse-ring {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

.apply-save-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  background: #17203a;
  color: #ffffff;
  border: none;
  border-radius: 999px;
  font-size: 0.84rem;
  font-weight: 650;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(23, 32, 58, 0.25);
  transition: all 0.18s ease;
}

.apply-save-btn:hover {
  background: #253358;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(23, 32, 58, 0.35);
}

.apply-save-btn:active {
  transform: translateY(0);
}

/* 主工作台区域 */
.studio-main-container {
  position: relative;
  flex: 1;
  width: 100%;
  height: calc(100% - 52px);
  overflow: hidden;
}
</style>
