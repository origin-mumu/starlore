<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { AlertTriangle, LoaderCircle, X } from '@lucide/vue'

const props = withDefaults(defineProps<{
  show: boolean
  title?: string
  message?: string
  subject?: string
  confirmText?: string
  cancelText?: string
  type?: 'confirm' | 'alert'
  tone?: 'default' | 'danger'
  busy?: boolean
}>(), {
  title: '确认',
  message: '',
  subject: '',
  confirmText: '确定',
  cancelText: '取消',
  type: 'confirm',
  tone: 'default',
  busy: false,
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

const dialogRef = ref<HTMLElement | null>(null)
const cancelButtonRef = ref<HTMLButtonElement | null>(null)
let previousActiveElement: HTMLElement | null = null

function cancel() {
  if (!props.busy) emit('cancel')
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.type === 'confirm') cancel()
}

watch(
  () => props.show,
  async show => {
    if (show) {
      previousActiveElement = document.activeElement as HTMLElement | null
      document.addEventListener('keydown', handleKeydown)
      await nextTick()
      ;(cancelButtonRef.value || dialogRef.value)?.focus()
      return
    }
    document.removeEventListener('keydown', handleKeydown)
    previousActiveElement?.focus()
    previousActiveElement = null
  },
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="show" class="modal-overlay" @click.self="type === 'confirm' && cancel()">
        <section
          ref="dialogRef"
          class="modal-card modal-confirm"
          :class="[`modal-${tone}`, { 'modal-alert': type === 'alert' }]"
          role="dialog"
          aria-modal="true"
          :aria-label="title"
          tabindex="-1"
        >
          <div class="modal-header">
            <div class="modal-heading">
              <span v-if="tone === 'danger'" class="danger-icon" aria-hidden="true">
                <AlertTriangle :size="20" />
              </span>
              <h2>{{ title }}</h2>
            </div>
            <button
              class="modal-close"
              type="button"
              :disabled="busy"
              aria-label="关闭"
              @click="cancel"
            >
              <X :size="16" />
            </button>
          </div>
          <div class="modal-body-content">
            <p class="confirm-text">{{ message }}</p>
            <p v-if="subject" class="confirm-subject" :title="subject">{{ subject }}</p>
            <slot />
          </div>
          <div class="confirm-actions">
            <button
              v-if="type === 'confirm'"
              ref="cancelButtonRef"
              class="btn-outline"
              type="button"
              :disabled="busy"
              @click="cancel"
            >
              {{ cancelText }}
            </button>
            <button
              class="btn-primary"
              :class="{ 'btn-danger': tone === 'danger' }"
              type="button"
              :disabled="busy"
              @click="emit('confirm')"
            >
              <LoaderCircle v-if="busy" class="button-spinner" :size="16" aria-hidden="true" />
              {{ busy ? '正在处理…' : confirmText }}
            </button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  padding: 24px;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-card {
  --modal-bg: #ffffff;
  --modal-danger: #ef4444;
  --modal-danger-soft: rgba(239, 68, 68, 0.1);
  background: var(--modal-bg);
  border: none;
  border-radius: 18px;
  width: 100%;
  max-width: 440px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.16);
  outline: none;
  display: flex;
  flex-direction: column;
}

.modal-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 24px 8px;
}

.modal-heading {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink);
}

.danger-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  color: var(--modal-danger);
  background: var(--modal-danger-soft);
}

.modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: var(--ink-muted);
  transition: all 0.15s ease;
}

.modal-close:hover {
  color: var(--ink);
  background: rgba(0, 0, 0, 0.05);
}

.modal-body-content {
  padding: 12px 24px;
  text-align: left;
}

.confirm-text {
  font-size: 0.92rem;
  color: var(--ink-soft);
  margin: 0 0 12px;
  line-height: 1.65;
}

.confirm-subject {
  overflow: hidden;
  margin: 0 0 14px;
  padding: 10px 14px;
  color: var(--ink);
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  font-size: 0.88rem;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 10px 24px 22px;
  background: transparent;
}

.btn-outline {
  min-height: 38px;
  height: 38px;
  padding: 0 20px;
  border: none;
  border-radius: var(--radius-full, 9999px);
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  background: #f1f5f9;
  color: #475569;
  font-family: inherit;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-outline:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 38px;
  height: 38px;
  padding: 0 22px;
  border: none;
  border-radius: var(--radius-full, 9999px);
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  background: #337BF4;
  color: #ffffff !important;
  font-family: inherit;
  box-shadow: 0 2px 8px rgba(51, 123, 244, 0.28);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-primary:hover {
  background: #2563eb;
  filter: brightness(1.05);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(51, 123, 244, 0.38);
}

.btn-danger {
  background: var(--modal-danger) !important;
  color: #fff !important;
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.28) !important;
}

.btn-danger:hover {
  background: #dc2626 !important;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.24s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-fade-enter-active .modal-card,
.modal-fade-leave-active .modal-card {
  transition: transform 0.24s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.24s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-card,
.modal-fade-leave-to .modal-card {
  opacity: 0;
  transform: scale(0.93) translateY(10px);
}

.button-spinner {
  animation: spin 800ms linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

:global([data-theme='dark'] .modal-overlay) {
  background: rgba(0, 0, 0, 0.65);
}

:global([data-theme='dark'] .modal-card) {
  --modal-bg: rgba(22, 26, 38, 0.96);
  --modal-danger: #f87171;
  --modal-danger-soft: rgba(248, 113, 113, 0.15);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 32px 80px -16px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

:global([data-theme='dark'] .modal-header),
:global([data-theme='dark'] .confirm-actions) {
  border-color: rgba(255, 255, 255, 0.06);
}

:global([data-theme='dark'] .modal-close) {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.08);
}

:global([data-theme='dark'] .modal-close:hover) {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

:global([data-theme='dark'] .confirm-subject) {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

:global([data-theme='dark'] .btn-outline) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.08);
  color: var(--ink-soft);
}

:global([data-theme='dark'] .btn-outline:hover) {
  background: rgba(255, 255, 255, 0.09);
  color: #fff;
}
</style>
