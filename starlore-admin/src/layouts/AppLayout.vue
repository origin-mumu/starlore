<script setup lang="ts">
import AppSidebar from '@/components/business/AppSidebar.vue'
</script>

<template>
  <div class="admin-shell">
    <AppSidebar />
    <main class="main-stage">
      <router-view />
    </main>
  </div>
</template>
