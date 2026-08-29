<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Sliders, ChevronDown } from '@lucide/vue'

interface Props {
  agentConfigForm: {
    modelName: string
    similarityThreshold: number
    topK: number
    temperature: number
    enableRerank: number
  }
  availableModels: { id: string; name: string; configured?: boolean }[]
  agentConfigSavedHint?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'save'): void
}>()

const modelSelectOpen = ref(false)

const currentModelLabel = computed(() => {
  const m = props.availableModels.find(item => item.id === props.agentConfigForm.modelName)
  return m ? `${m.name} (${m.id})` : props.agentConfigForm.modelName || '请选择模型'
})

function handleOutsideClick() {
  if (modelSelectOpen.value) {
    modelSelectOpen.value = false
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
  <div class="imm-panel-config">
    <section class="imm-cap-section">
      <div class="imm-cap-heading">
        <div>
          <p class="imm-cap-eyebrow">Playground Control</p>
          <h2><Sliders :size="16" style="margin-right: 6px; vertical-align: -2px;" /> Agent 检索与模型参数调优</h2>
        </div>
      </div>
      <p class="imm-cap-copy">动态微调 RAG 知识检索精细度与大模型生成偏好。</p>

      <div class="imm-cfg-form">
        <div class="imm-cfg-card">
          <label class="imm-cfg-label">
            <span>驱动大模型引擎</span>
            <span class="imm-cfg-tag">LLM Engine</span>
          </label>
          <div class="custom-select">
            <div class="custom-select-trigger" @click.stop="modelSelectOpen = !modelSelectOpen">
              <span>{{ currentModelLabel }}</span>
              <ChevronDown class="arrow-icon" :class="{ 'is-open': modelSelectOpen }" />
            </div>
            <Transition name="dropdown-fade">
              <div v-if="modelSelectOpen" class="custom-select-options">
                <div
                  v-for="model in availableModels"
                  :key="model.id"
                  class="custom-select-option"
                  :class="{ active: agentConfigForm.modelName === model.id }"
                  @click="agentConfigForm.modelName = model.id; modelSelectOpen = false"
                >
                  {{ model.name }} ({{ model.id }})
                </div>
              </div>
            </Transition>
          </div>
        </div>

        <div class="imm-cfg-card">
          <div class="imm-cfg-label-row">
            <span class="imm-cfg-label-title">向量检索相似度阈值 (Similarity)</span>
            <span class="imm-cfg-val-badge">{{ agentConfigForm.similarityThreshold }}</span>
          </div>
          <input type="range" v-model.number="agentConfigForm.similarityThreshold" min="0.1" max="0.95" step="0.05" class="imm-cfg-range" />
          <p class="imm-cfg-subtext">自动过滤低于该相似度的噪音切片 (推荐 0.55 - 0.70)</p>
        </div>

        <div class="imm-cfg-card">
          <div class="imm-cfg-label-row">
            <span class="imm-cfg-label-title">检索最大切片数 (Top-K)</span>
            <span class="imm-cfg-val-badge">{{ agentConfigForm.topK }} 条</span>
          </div>
          <input type="range" v-model.number="agentConfigForm.topK" min="1" max="10" step="1" class="imm-cfg-range" />
          <p class="imm-cfg-subtext">限制送入大模型的参考上下文段落数</p>
        </div>

        <div class="imm-cfg-card">
          <div class="imm-cfg-label-row">
            <span class="imm-cfg-label-title">模型随机度 (Temperature)</span>
            <span class="imm-cfg-val-badge">{{ agentConfigForm.temperature }}</span>
          </div>
          <input type="range" v-model.number="agentConfigForm.temperature" min="0.0" max="1.0" step="0.1" class="imm-cfg-range" />
        </div>

        <div class="imm-cfg-card row-toggle">
          <div>
            <span class="imm-cfg-label-title">启用 BM25 混合检索与重排序 (Rerank)</span>
            <p class="imm-cfg-subtext">融合关键词匹配与向量语义算法</p>
          </div>
          <label class="imm-switch">
            <input type="checkbox" :checked="agentConfigForm.enableRerank === 1" @change="agentConfigForm.enableRerank = ($event.target as HTMLInputElement).checked ? 1 : 0" />
            <span class="imm-slider"></span>
          </label>
        </div>

        <button type="button" class="imm-index-button save-btn" @click="emit('save')">
          保存 Agent 调参配置
        </button>
        <p v-if="agentConfigSavedHint" class="imm-cfg-hint">{{ agentConfigSavedHint }}</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.imm-panel-config {
  flex: 1;
  overflow-y: auto;
  padding: 6px 4px 16px;
  max-width: 520px;
  width: 100%;
  margin: 0 auto;
  pointer-events: auto;
}

.imm-cap-section {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 18px 20px;
  margin-bottom: 16px;
  box-shadow: 0 4px 20px -8px rgba(0, 0, 0, 0.06);
}

.imm-cap-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.imm-cap-eyebrow {
  font-size: 10.5px;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  color: var(--accent);
  font-weight: 700;
  margin-bottom: 2px;
}

.imm-cap-heading h2 {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
}

.imm-cap-copy {
  font-size: 12.5px;
  color: var(--ink-muted);
  margin-bottom: 16px;
  line-height: 1.4;
}

.imm-cfg-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.imm-cfg-card {
  background: var(--surface-secondary, rgba(0, 0, 0, 0.02));
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.imm-cfg-card.row-toggle {
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.imm-cfg-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}

.imm-cfg-tag {
  font-size: 10px;
  font-family: 'Fira Code', monospace;
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 1px 6px;
  border-radius: 4px;
  color: var(--ink-muted);
}

.imm-cfg-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.imm-cfg-label-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}

.imm-cfg-val-badge {
  font-size: 11.5px;
  font-family: 'Fira Code', monospace;
  font-weight: 700;
  color: var(--accent);
  background: var(--accent-soft);
  padding: 1px 7px;
  border-radius: 8px;
}

.imm-cfg-range {
  width: 100%;
  accent-color: var(--accent);
  cursor: pointer;
  margin: 2px 0;
}

.imm-cfg-subtext {
  font-size: 11px;
  color: var(--ink-muted);
  margin: 0;
}

.custom-select {
  position: relative;
  width: 100%;
}

.custom-select-trigger {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 7px 12px;
  font-size: 12.5px;
  color: var(--ink);
  cursor: pointer;
  transition: all 0.2s;
}

.custom-select-trigger:hover {
  border-color: var(--border-interactive);
}

.arrow-icon {
  width: 14px;
  height: 14px;
  transition: transform 0.2s;
}
.arrow-icon.is-open {
  transform: rotate(180deg);
}

.custom-select-options {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 10px 28px -6px rgba(0, 0, 0, 0.15);
  z-index: 50;
  max-height: 180px;
  overflow-y: auto;
  padding: 4px;
}

.custom-select-option {
  padding: 7px 10px;
  font-size: 12px;
  color: var(--ink);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.custom-select-option:hover {
  background: var(--hover-bg);
}

.custom-select-option.active {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}

.imm-switch {
  position: relative;
  display: inline-block;
  width: 36px;
  height: 20px;
  flex-shrink: 0;
}
.imm-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}
.imm-slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: var(--border);
  transition: 0.3s;
  border-radius: 20px;
}
.imm-slider:before {
  position: absolute;
  content: '';
  height: 14px;
  width: 14px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}
input:checked + .imm-slider {
  background-color: var(--accent);
}
input:checked + .imm-slider:before {
  transform: translateX(16px);
}

.imm-index-button.save-btn {
  width: auto;
  min-width: 160px;
  margin: 10px auto 0;
  padding: 9px 24px;
  background: var(--accent);
  color: #FFFFFF !important;
  border: none;
  border-radius: var(--radius-full);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--shadow-button);
  transition: all 0.2s;
  display: block;
}

.imm-index-button.save-btn:hover {
  filter: brightness(1.08);
  transform: translateY(-1px);
  box-shadow: var(--shadow-button-hover);
}

.imm-cfg-hint {
  font-size: 12px;
  color: #28c840;
  text-align: center;
  margin: 4px 0 0;
}
</style>
