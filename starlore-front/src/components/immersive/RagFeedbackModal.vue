<script setup lang="ts">
import { ref } from 'vue'
import { X } from '@lucide/vue'

interface Props {
  open: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit', data: { type: string; comment: string }): void
}>()

const feedbackType = ref('NOT_RELEVANT')
const feedbackComment = ref('')

function handleSubmit() {
  emit('submit', {
    type: feedbackType.value,
    comment: feedbackComment.value,
  })
}
</script>

<template>
  <div v-if="open" class="imm-feedback-backdrop" @click.self="emit('close')">
    <div class="imm-feedback-modal">
      <div class="imm-feedback-header">
        <h3>反馈回答质量</h3>
        <button class="imm-close-btn" @click="emit('close')"><X :size="16" /></button>
      </div>
      <div class="imm-feedback-body">
        <label class="imm-form-label">请选择主要问题类型：</label>
        <div class="imm-radio-group">
          <label><input type="radio" v-model="feedbackType" value="NOT_RELEVANT" /> 知识库未检索到正确资料</label>
          <label><input type="radio" v-model="feedbackType" value="HALLUCINATION" /> 包含大模型凭空幻觉内容</label>
          <label><input type="radio" v-model="feedbackType" value="WRONG_FACT" /> 事实或语法描述有误</label>
          <label><input type="radio" v-model="feedbackType" value="OTHER" /> 其他意见</label>
        </div>
        <label class="imm-form-label">补充说明 (选填)：</label>
        <textarea v-model="feedbackComment" placeholder="请输入具体意见或修正建议..." class="imm-feedback-input"></textarea>
      </div>
      <div class="imm-feedback-footer">
        <button class="imm-btn-cancel" @click="emit('close')">取消</button>
        <button class="imm-btn-submit" @click="handleSubmit">提交评价</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.imm-feedback-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.imm-feedback-modal {
  background: #ffffff;
  border: none;
  border-radius: 18px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.16);
  width: 100%;
  max-width: 460px;
  overflow: hidden;
  animation: modalCardPop 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.imm-feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 24px 8px;
}

.imm-feedback-header h3 {
  font-size: 1.12rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink);
  margin: 0;
}

.imm-close-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 0;
  transition: all 0.15s ease;
}

.imm-close-btn:hover {
  background: #f1f5f9;
  color: var(--ink);
}

.imm-feedback-body {
  padding: 12px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.imm-form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}

.imm-radio-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: rgba(0, 0, 0, 0.025);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 14px;
  padding: 12px 16px;
}

.imm-radio-group label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--ink-soft);
  cursor: pointer;
  user-select: none;
}

.imm-radio-group input[type="radio"] {
  accent-color: #337BF4;
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.imm-feedback-input {
  width: 100%;
  box-sizing: border-box;
  height: 90px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 14px;
  padding: 12px 14px;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--ink);
  background: rgba(0, 0, 0, 0.025);
  resize: none;
  font-family: inherit;
  transition: all 0.2s;
}

.imm-feedback-input:focus {
  outline: none;
  border-color: #337BF4;
  box-shadow: 0 0 0 3px rgba(51, 123, 244, 0.15);
  background: #fff;
}

.imm-feedback-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 10px 24px 22px;
  background: transparent;
}

.imm-btn-cancel,
.imm-btn-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  height: 38px;
  border-radius: var(--radius-full, 9999px);
  cursor: pointer;
  font-weight: 600;
  font-size: 0.84rem;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
}

.imm-btn-cancel {
  padding: 0 20px;
  background: #f1f5f9;
  border: none;
  color: #475569;
}

.imm-btn-cancel:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.imm-btn-submit {
  padding: 0 22px;
  background: #337BF4;
  border: none;
  color: #ffffff !important;
  box-shadow: 0 2px 8px rgba(51, 123, 244, 0.28);
}

.imm-btn-submit:hover {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(51, 123, 244, 0.38);
}

:global([data-theme="dark"]) .imm-feedback-backdrop {
  background: rgba(0, 0, 0, 0.65);
}

:global([data-theme="dark"]) .imm-feedback-modal {
  background: #181b26;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.6);
}

:global([data-theme="dark"]) .imm-btn-cancel {
  background: #242a38;
  color: #cbd5e1;
}

:global([data-theme="dark"]) .imm-btn-cancel:hover {
  background: #333d52;
  color: #ffffff;
}

:global([data-theme="dark"]) .imm-radio-group,
:global([data-theme="dark"]) .imm-feedback-input {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

:global([data-theme="dark"]) .imm-feedback-input:focus {
  background: rgba(255, 255, 255, 0.07);
}

:global([data-theme="dark"]) .imm-btn-cancel {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.08);
  color: var(--ink-soft);
}
</style>
