<script setup lang="ts">
defineProps<{
  text: string
}>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__icon">
      <slot name="icon" />
    </div>
    <p>{{ text }}</p>
  </div>
</template>
