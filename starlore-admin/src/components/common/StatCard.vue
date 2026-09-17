<script setup lang="ts">
defineProps<{
  label: string
  value: number | string
}>()
</script>

<template>
  <div class="stat-card bento-card bento-card--hover">
    <div class="stat-card__icon">
      <slot />
    </div>
    <div class="stat-card__info">
      <div class="stat-card__value">{{ value }}</div>
      <div class="stat-card__label">{{ label }}</div>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.stat-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--brand-primary), var(--brand-hover));
  color: #fff;
  box-shadow: var(--shadow-button);
}

.stat-card__icon :deep(svg) {
  width: 22px;
  height: 22px;
}

.stat-card__value {
  font-size: 1.7rem;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.stat-card__label {
  font-size: 0.83rem;
  color: var(--text-muted);
}
</style>
