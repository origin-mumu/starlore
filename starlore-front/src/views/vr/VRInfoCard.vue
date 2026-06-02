<script setup lang="ts">
import type { CategoryStats } from './planetData'

defineProps<{
  planet: CategoryStats
}>()
</script>

<template>
  <div class="info-card">
    <div class="info-card__header">
      <span class="info-card__dot" :style="{ background: planet.config.emissive }"></span>
      <span class="info-card__name">{{ planet.name }}</span>
    </div>
    <p class="info-card__desc">{{ planet.config.description }}</p>
    <div class="info-card__meta">
      <span class="info-card__meta-item">
        <span class="info-card__meta-icon">&#9679;</span>
        {{ planet.count }} 篇星迹
      </span>
      <span class="info-card__meta-divider"></span>
      <span class="info-card__meta-item">
        <span class="info-card__meta-icon">&#9681;</span>
        更新于 {{ planet.lastUpdated }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.info-card {
  padding: 14px 20px;
  background: rgba(10, 16, 34, 0.95);
  border: 1px solid oklch(0.55 0.15 35 / 0.4);
  border-radius: 14px;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  min-width: 200px;
  pointer-events: none;
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.5),
    inset 0 0 30px oklch(0.55 0.15 35 / 0.05);
}

.info-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.info-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 8px currentColor;
}

.info-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 1px;
  text-shadow: 0 0 12px rgba(183, 178, 255, 0.6);
}

.info-card__desc {
  font-size: 12.5px;
  color: #ffffff;
  margin: 0 0 10px;
  letter-spacing: 0.5px;
  text-shadow: 0 0 10px rgba(183, 178, 255, 0.4);
}

.info-card__meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-card__meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #ffffff;
  text-shadow: 0 0 8px rgba(183, 178, 255, 0.4);
}

.info-card__meta-icon {
  font-size: 10px;
  color: oklch(0.55 0.15 35 / 0.6);
}

.info-card__meta-divider {
  width: 1px;
  height: 12px;
  background: oklch(0.55 0.15 35 / 0.15);
}
</style>
