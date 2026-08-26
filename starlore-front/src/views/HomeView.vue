<script lang="ts" setup>
import { useUserStore } from '@/stores/user'
import HomeLanding from './home/HomeLanding.vue'
import HomeDashboard from './home/HomeDashboard.vue'

const userStore = useUserStore()
</script>

<template>
  <div class="page-container">
    <!-- Landing Page for Non-Logged-In Users -->
    <HomeLanding v-if="!userStore.isLoggedIn" />

    <!-- Logged-In User Home Page -->
    <HomeDashboard v-else />
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  position: relative;
}
</style>
