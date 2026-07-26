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
              <AlertTriangle :size="18" />
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
            <X :size="18" />
          </button>
        </div>
        <p class="confirm-text">{{ message }}</p>
        <p v-if="subject" class="confirm-subject" :title="subject">{{ subject }}</p>
        <slot />
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
            {{ busy ? '正在删除…' : confirmText }}
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
  z-index: 1000;
  padding: 20px;
  background: color-mix(in oklch, var(--ink) 46%, transparent);
  backdrop-filter: blur(5px) saturate(0.82);
  -webkit-backdrop-filter: blur(5px) saturate(0.82);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-card {
  --modal-bg: rgb(255, 255, 255);
  --modal-raised: rgb(247, 247, 247);
  --modal-danger: oklch(0.57 0.17 32);
  --modal-danger-soft: oklch(0.955 0.025 32);
  background: var(--modal-bg);
  border: 1px solid color-mix(in oklch, var(--ink) 10%, transparent);
  border-radius: 20px;
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
  padding: 24px;
  box-shadow: 0 24px 70px color-mix(in oklch, var(--ink) 22%, transparent);
  outline: none;
}
.modal-confirm {
  max-width: 420px;
  text-align: center;
}
.modal-header {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}
.modal-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}
.modal-header h2 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  line-height: 1.35;
  color: var(--ink);
}
.danger-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  color: var(--modal-danger);
  background: var(--modal-danger-soft);
}
.modal-close {
  position: absolute;
  top: 0;
  right: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  color: var(--ink-muted);
  transition: background 180ms var(--ease-out-quart), color 180ms var(--ease-out-quart);
}
.modal-close:hover {
  color: var(--ink);
  background: var(--surface-hover);
}
.confirm-text {
  max-width: 38ch;
  font-size: 0.9rem;
  color: var(--ink-soft);
  margin: 0 auto 14px;
  line-height: 1.65;
}
.confirm-subject {
  overflow: hidden;
  margin: 0 0 22px;
  padding: 11px 12px;
  color: var(--ink);
  background: var(--modal-raised);
  border: 1px solid color-mix(in oklch, var(--modal-danger) 9%, var(--border));
  border-radius: 12px;
  font-size: 0.88rem;
  font-weight: 650;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.btn-outline {
  min-height: 42px;
  padding: 9px 20px;
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  background: rgb(245, 245, 245);
  color: var(--ink);
  font-family: inherit;
  transition: all 0.2s;
}
.btn-outline:hover {
  border-color: var(--border-interactive);
  background: var(--surface-hover);
}
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 42px;
  padding: 9px 20px;
  border: none;
  border-radius: 999px;
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  background: var(--accent);
  color: oklch(0.985 0.004 70);
  font-family: inherit;
  transition: transform 180ms var(--ease-out-quart), opacity 180ms var(--ease-out-quart);
}
.btn-danger {
  background: var(--modal-danger) !important;
  color: rgb(255, 255, 255) !important;
  box-shadow: 0 8px 18px color-mix(in oklch, var(--modal-danger) 20%, transparent);
}
.btn-primary:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}
.btn-outline:focus-visible,
.btn-primary:focus-visible,
.modal-close:focus-visible {
  outline: 3px solid color-mix(in oklch, var(--accent) 22%, transparent);
  outline-offset: 2px;
}
.btn-outline:disabled,
.btn-primary:disabled,
.modal-close:disabled {
  cursor: not-allowed;
  opacity: 0.58;
  transform: none;
}
.button-spinner {
  animation: spin 800ms linear infinite;
}
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 180ms var(--ease-out-quart);
}
.modal-fade-enter-active .modal-card,
.modal-fade-leave-active .modal-card {
  transition: transform 180ms var(--ease-out-quart), opacity 180ms var(--ease-out-quart);
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
.modal-fade-enter-from .modal-card,
.modal-fade-leave-to .modal-card {
  opacity: 0;
  transform: translateY(8px) scale(0.985);
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
@media (max-width: 480px) {
  .modal-overlay {
    align-items: flex-end;
    padding: 12px;
  }
  .modal-card {
    padding: 20px;
    border-radius: 20px;
  }
  .confirm-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}
:global([data-theme='dark'] .modal-card) {
  --modal-bg: oklch(0.19 0.025 285);
  --modal-raised: oklch(0.235 0.03 285);
  --modal-danger: oklch(0.7 0.16 30);
  --modal-danger-soft: oklch(0.27 0.055 28);
  border-color: color-mix(in oklch, var(--modal-danger) 18%, var(--border));
  box-shadow: 0 24px 70px oklch(0.06 0.02 285 / 0.62);
}
:global([data-theme='dark'] .modal-overlay) {
  background: oklch(0.055 0.025 285 / 0.68);
  backdrop-filter: blur(5px) saturate(0.72);
  -webkit-backdrop-filter: blur(5px) saturate(0.72);
}
@media (prefers-reduced-motion: reduce) {
  .modal-fade-enter-active,
  .modal-fade-leave-active,
  .modal-fade-enter-active .modal-card,
  .modal-fade-leave-active .modal-card {
    transition-duration: 0.01ms;
  }
}
</style>
