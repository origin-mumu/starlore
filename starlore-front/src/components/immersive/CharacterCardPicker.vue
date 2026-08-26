<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Bot } from '@lucide/vue'
import type { CharacterCard } from '@/api/ai'

interface Props {
  characterCards: CharacterCard[]
  selectedCharacterKey: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'select', key: string): void
}>()

const charPickerOpen = ref(false)

function pick(key: string) {
  emit('select', key)
  charPickerOpen.value = false
}

function handleOutsideClick() {
  if (charPickerOpen.value) {
    charPickerOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('click', handleOutsideClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleOutsideClick)
})
</script>

<template>
  <div class="imm-toolbar">
    <div class="imm-char-picker" :class="{ open: charPickerOpen }">
      <button
        type="button"
        class="imm-char-trigger"
        @click.stop="charPickerOpen = !charPickerOpen"
      >
        <span>{{
          characterCards.find(c => c.key === selectedCharacterKey)?.name || '角色卡'
        }}</span>
        <svg width="12" height="12" viewBox="0 0 24 24">
          <path fill="currentColor" d="M7 10l5 5 5-5H7z" />
        </svg>
      </button>
      <div v-show="charPickerOpen" class="imm-char-panel">
        <button
          v-for="c in characterCards"
          :key="c.key"
          type="button"
          class="imm-char-opt"
          :class="{ active: c.key === selectedCharacterKey }"
          @click.stop="pick(c.key)"
        >
          <span class="imm-char-name">{{ c.name }}</span>
          <span class="imm-char-desc">{{ c.description }}</span>
        </button>
      </div>
    </div>
    <span class="imm-agent-btn active" title="多Agent协作：Planner→Executor→Reviewer">
      <Bot :size="13" />
      Multi-Agent
    </span>
  </div>
</template>

<style scoped>
.imm-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0 10px;
  pointer-events: auto;
  flex-shrink: 0;
}

.imm-char-picker {
  position: relative;
}

.imm-char-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: var(--radius-full, 20px);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--ink);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  backdrop-filter: blur(8px);
}
.imm-char-trigger:hover {
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
  border-color: var(--border-interactive);
}
.imm-char-picker.open .imm-char-trigger {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.imm-char-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  width: 240px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  padding: 6px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.imm-char-opt {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 8px 10px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
}
.imm-char-opt:hover {
  background: var(--hover-bg, rgba(0, 0, 0, 0.03));
}
.imm-char-opt.active {
  background: var(--accent-soft);
}

.imm-char-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}

.imm-char-desc {
  font-size: 11px;
  color: var(--ink-muted);
  margin-top: 2px;
}

.imm-agent-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border-radius: var(--radius-full, 20px);
  font-size: 11px;
  font-weight: 600;
  border: 1px solid rgba(232, 93, 42, 0.35);
  color: var(--accent);
  background: rgba(232, 93, 42, 0.08);
  margin-left: auto;
  cursor: default;
}
</style>
