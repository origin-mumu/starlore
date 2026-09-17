<script setup lang="ts">
import { Close } from '@element-plus/icons-vue'

defineProps<{
  title: string
  width?: number
}>()

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <Teleport to="body">
    <div class="app-modal__overlay" @click.self="emit('close')">
      <div class="app-modal__panel" :style="{ maxWidth: (width ?? 520) + 'px' }">
        <header class="app-modal__header">
          <h3 class="app-modal__title">{{ title }}</h3>
          <button class="app-modal__close" type="button" aria-label="关闭" @click="emit('close')">
            <el-icon :size="15"><Close /></el-icon>
          </button>
        </header>
        <div class="app-modal__body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="app-modal__footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.app-modal__overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.35);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  animation: overlay-in var(--transition-normal);
}

@keyframes overlay-in {
  from {
    opacity: 0;
  }
}

.app-modal__panel {
  width: 100%;
  max-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  /* 浅色模式必须纯白不透底，避免遮罩暗色透出发灰 */
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-modal);
  animation: panel-in var(--transition-spring);
}

@keyframes panel-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.app-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.app-modal__title {
  font-size: 1.08rem;
  font-weight: 700;
  color: var(--text-primary);
}

.app-modal__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-pill);
  color: var(--text-muted);
  transition: all var(--transition-fast);
}

.app-modal__close:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
  transform: rotate(90deg);
}

.app-modal__body {
  padding: 18px 24px;
  overflow-y: auto;
}

.app-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 24px 20px;
}
</style>
