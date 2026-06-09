<script setup lang="ts">
withDefaults(defineProps<{
  show: boolean
  title?: string
  message?: string
  confirmText?: string
  cancelText?: string
  type?: 'confirm' | 'alert'
}>(), {
  title: '确认',
  message: '',
  confirmText: '确定',
  cancelText: '取消',
  type: 'confirm',
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="modal-overlay" @click.self="type === 'confirm' && emit('cancel')">
      <div class="modal-card modal-confirm" :class="{ 'modal-alert': type === 'alert' }">
        <div class="modal-header">
          <h2>{{ title }}</h2>
          <button class="modal-close" @click="emit('cancel')">&times;</button>
        </div>
        <p class="confirm-text">{{ message }}</p>
        <slot />
        <div class="confirm-actions">
          <button v-if="type === 'confirm'" class="btn-outline" @click="emit('cancel')">{{ cancelText }}</button>
          <button class="btn-primary" @click="emit('confirm')">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-card {
  background: var(--surface, #fff);
  border-radius: 20px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  padding: 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.modal-confirm {
  max-width: 400px;
  text-align: center;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.modal-header h2 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--ink);
}
.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--ink-muted);
}
.confirm-text {
  font-size: 0.95rem;
  color: var(--ink-soft);
  margin: 0 0 24px;
  line-height: 1.6;
}
.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.btn-outline {
  padding: 8px 24px;
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  color: var(--ink);
  font-family: inherit;
  transition: all 0.2s;
}
.btn-outline:hover {
  border-color: var(--accent, #6d63ff);
  color: var(--accent, #6d63ff);
}
.btn-primary {
  padding: 8px 24px;
  border: none;
  border-radius: 999px;
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  background: var(--accent, #B85C38);
  color: #fff;
  font-family: inherit;
  transition: all 0.2s;
}
.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}
</style>
