<script setup lang="ts">
import { computed } from 'vue'

/**
 * SPACE 星空流光按钮
 * 复刻自 Uiverse · MIT License · Component by StealthWorm
 * https://uiverse.io/StealthWorm/spotty-horse-48
 *
 * 用于替换全站 AI 入口按钮：
 * - 首页搜索栏「AI 提问」(size="sm")
 * - 文章详情页右下角悬浮「AI 伴读」(size="md")
 * - 简历编辑页顶部「AI 润色助手」(size="sm") 与右下角悬浮「AI 润色」(size="md")
 */
interface Props {
  /** 按钮文字 */
  label?: string
  /** sm: 紧凑型（36px，用于搜索栏/顶栏内嵌）；md: 原版尺寸（208px 胶囊） */
  size?: 'sm' | 'md'
  /** 激活态（如面板已展开时的「收起 AI」）：静止渐变 + 粉色描边 */
  active?: boolean
  /** 悬停提示，缺省时使用 label */
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  label: 'AI',
  size: 'md',
  active: false,
  title: '',
})

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
}>()

const sizeClass = computed(() => `size-${props.size}`)
</script>

<template>
  <button
    type="button"
    class="space-ai-btn"
    :class="[sizeClass, { 'is-active': active }]"
    :title="title || label"
    @click="emit('click', $event)"
  >
    <strong class="space-btn-label">{{ label }}</strong>
    <span class="space-btn-stars-box" aria-hidden="true">
      <span class="space-btn-stars"></span>
    </span>
    <span class="space-btn-glow" aria-hidden="true">
      <span class="space-btn-circle"></span>
      <span class="space-btn-circle"></span>
    </span>
  </button>
</template>

<style scoped>
.space-ai-btn {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  overflow: hidden;
  padding: 0;
  background-size: 300% 300%;
  cursor: pointer;
  backdrop-filter: blur(1rem);
  -webkit-backdrop-filter: blur(1rem);
  border-radius: 5rem;
  transition: 0.5s;
  animation: gradient-301 5s ease infinite;
  border: double 4px transparent;
  background-image: linear-gradient(#212121, #212121),
    linear-gradient(137.48deg, #ffdb3b 10%, #fe53bb 45%, #8f51ea 67%, #0044ff 87%);
  background-origin: border-box;
  background-clip: content-box, border-box;
  font-family: inherit;
}

/* ── 尺寸变体 ── */
.size-md {
  width: 13.5rem;   /* 原版 13rem 内容宽 + 双层 4px 描边 */
  height: 3.5rem;   /* 原版 3rem 内容高 + 双层 4px 描边 */
}

.size-sm {
  height: 36px;
  min-width: 118px;
  padding: 0 18px;
  border-radius: 999px;
}

/* ── 文字 ── */
.space-btn-label {
  z-index: 2;
  font-size: 12px;
  letter-spacing: 5px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: 0 0 4px white;
}

.size-sm .space-btn-label {
  letter-spacing: 3px;
}

/* ── 星空层 ── */
.space-btn-stars-box {
  position: absolute;
  z-index: -1;
  width: 100%;
  height: 100%;
  overflow: hidden;
  transition: 0.5s;
  backdrop-filter: blur(1rem);
  border-radius: 5rem;
}

.space-btn-stars {
  position: relative;
  display: block;
  background: transparent;
  width: 200rem;
  height: 200rem;
}

.space-btn-stars::after {
  content: '';
  position: absolute;
  top: -10rem;
  left: -100rem;
  width: 100%;
  height: 100%;
  animation: anim-star-rotate 90s linear infinite;
  background-image: radial-gradient(#ffffff 1px, transparent 1%);
  background-size: 50px 50px;
}

.space-btn-stars::before {
  content: '';
  position: absolute;
  top: 0;
  left: -50%;
  width: 170%;
  height: 500%;
  animation: anim-star 60s linear infinite;
  background-image: radial-gradient(#ffffff 1px, transparent 1%);
  background-size: 50px 50px;
  opacity: 0.5;
}

/* ── 底部流光 ── */
.space-btn-glow {
  position: absolute;
  display: flex;
  width: 92%;
}

.space-btn-circle {
  width: 100%;
  height: 30px;
  filter: blur(2rem);
  animation: pulse-3011 4s infinite;
  z-index: -1;
}

.space-btn-circle:nth-of-type(1) {
  background: rgba(254, 83, 186, 0.636);
}

.space-btn-circle:nth-of-type(2) {
  background: rgba(142, 81, 234, 0.704);
}

/* ── 交互态 ── */
.space-ai-btn:hover {
  transform: scale(1.1);
}

.space-ai-btn:hover .space-btn-stars-box {
  z-index: 1;
  background-color: #212121;
}

.space-ai-btn:active {
  border: double 4px #fe53bb;
  background-origin: border-box;
  background-clip: content-box, border-box;
  animation: none;
}

.space-ai-btn:active .space-btn-circle {
  background: #fe53bb;
}

/* 激活态（如「收起 AI」）：停止流光动画，粉色实线描边以示区分 */
.space-ai-btn.is-active {
  animation: none;
  border: double 4px #fe53bb;
}

.space-ai-btn.is-active:hover {
  transform: scale(1.03);
}

.space-ai-btn:focus-visible {
  outline: 2px solid #fe53bb;
  outline-offset: 2px;
}

/* ── 动画 ── */
@keyframes anim-star {
  from { transform: translateY(0); }
  to { transform: translateY(-135rem); }
}

@keyframes anim-star-rotate {
  from { transform: rotate(360deg); }
  to { transform: rotate(0); }
}

@keyframes gradient-301 {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes pulse-3011 {
  0% { transform: scale(0.75); box-shadow: 0 0 0 0 rgba(0, 0, 0, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 10px rgba(0, 0, 0, 0); }
  100% { transform: scale(0.75); box-shadow: 0 0 0 0 rgba(0, 0, 0, 0); }
}

@media (prefers-reduced-motion: reduce) {
  .space-ai-btn,
  .space-btn-circle,
  .space-btn-stars::before,
  .space-btn-stars::after {
    animation: none;
  }
}
</style>
