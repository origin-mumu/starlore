<script setup lang="ts">
import { ref } from 'vue'

const expanded = ref(false)
</script>

<template>
  <div class="assistant" :class="{ 'assistant--expanded': expanded }">
    <button class="assistant__trigger" @click="expanded = !expanded">?</button>
    <Transition name="panel-fade">
      <div v-if="expanded" class="assistant__panel">
        <p class="assistant__hint">拖拽 - 旋转</p>
        <p class="assistant__hint">滚轮 - 缩放</p>
        <p class="assistant__hint">点星球 - 看文章</p>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.assistant {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.assistant__trigger {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(59, 130, 246, 0.25);
  background: rgba(10, 16, 34, 0.7);
  color: rgba(96, 165, 250, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: all 0.25s ease;
  font-size: 14px;
  font-weight: 600;
  font-family: serif;
}

.assistant__trigger:hover {
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.4);
  color: #93C5FD;
  transform: scale(1.08);
}

.assistant__panel {
  padding: 12px 16px;
  background: rgba(10, 16, 34, 0.85);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 12px;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  min-width: 160px;
}

.assistant__hint {
  margin: 0;
  padding: 4px 0;
  font-size: 11.5px;
  color: rgba(200, 210, 230, 0.55);
  letter-spacing: 0.3px;
}

.panel-fade-enter-active {
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.panel-fade-leave-active {
  transition: all 0.15s ease-in;
}
.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.95);
}
</style>
