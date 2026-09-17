<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'

defineProps<{
  text?: string
}>()
</script>

<template>
  <div class="loading-state">
    <el-icon :size="18"><Loading /></el-icon>
    <span>{{ text ?? '加载中...' }}</span>
  </div>
</template>
