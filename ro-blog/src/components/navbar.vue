<script setup lang="ts">
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ref } from 'vue'
const router = useRouter()
const navItems = [
  { name: '首页', path: '/', icon: '' },
  { name: '关于', path: '/about', icon: '' },
  { name: '博客', path: '/articles', icon: '' },
  { name: '分类', path: '/categories', icon: '' },
]
const route = useRoute()
const isActive = (path: string) => {
  return route.path == path
}

// --- 核心逻辑开始 ---
const navMenuRef = ref<HTMLElement | null>(null)

// 鼠标移动处理函数
const handleMouseMove = (e: MouseEvent) => {
  const menu = navMenuRef.value
  if (!menu) return

  // 获取容器的位置信息
  const rect = menu.getBoundingClientRect()

  // 计算鼠标相对于容器左上角的坐标
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top

  // 将坐标写入 CSS 变量，供样式使用
  menu.style.setProperty('--mouse-x', `${x}px`)
  menu.style.setProperty('--mouse-y', `${y}px`)
}
</script>
<template>
  <div class="navbar">
    <div class="nav-container">
      <div class="nav-brand" @click="router.push('/')">
        <span>RO Blog</span>
      </div>
      <ul ref="navMenuRef" class="nav-menu spotlight-menu" @mousemove="handleMouseMove">
        <li v-for="(item, index) in navItems" :key="index" class="nav-item">
          <RouterLink
            :to="item.path"
            class="nav-link"
            :class="{ 'nav-active': isActive(item.path) }"
          >
            <span class="nav-text">{{ item.name }}</span>
          </RouterLink>
        </li>
      </ul>
      <div class="more" :class="{ 'nav-active': isActive('/vr') }" @click="router.push('/vr')">
        <span>MORE</span>
      </div>
    </div>
  </div>
</template>
<style scoped>
.navbar {
  z-index: 100;
  position: fixed;
  top: 10px;
  left: 50%;
  width: auto;
  transform: translateX(-50%);
  --spotlight-color: rgba(156, 184, 225, 0.35);
}
.nav-container {
  display: flex;
  margin: 0 auto;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  width: 100%;
}
.nav-brand,
.nav-menu, 
.more {
  background: rgba(232, 251, 255, 0.4);
}

.nav-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: large;
  font-weight: bold;
  box-shadow: 0 10px 24px -18px rgba(33, 49, 74, 0.42);
  padding: 0px 15px;
  border-radius: 60px;
  
  backdrop-filter: blur(5px);
  height: 40px;
  border: 1px solid rgba(219, 228, 243, 0.95);
  transition: all 0.3s ease;
  cursor: pointer;
  overflow: hidden;
}
.nav-brand span {
  transition: all 0.3s ease;
}
.nav-brand:hover {
  transform: scale(1.1);
  margin: 0 10px;
}

.nav-menu::before,
.nav-brand::before,
.more::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;

  background: radial-gradient(
    240px circle at var(--mouse-x, 50%) var(--mouse-y, 50%),
    var(--spotlight-color),
    transparent 60%
  );

  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
  z-index: -100;
}

.nav-menu:hover::before,
.nav-brand:hover::before,
.more:hover::before {
  opacity: 1;
}


.nav-menu {
  display: flex;
  align-items: center;
  justify-content: center;
  list-style: none;
  box-shadow: 0 10px 24px -18px rgba(33, 49, 74, 0.42);
  padding: 0px 15px;
  border-radius: 60px;
  margin: 0 20px;
 
  backdrop-filter: blur(5px);
  height: 40px;
  font-weight: bold;
  border: 1px solid rgba(219, 228, 243, 0.95);
  overflow: hidden;
}
.nav-item {
  margin: 0 10px;
}
.nav-link {
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  border-radius: 60px;
  padding: 3px 15px;
  white-space: nowrap;
  color: #2e3b52;
  transition: all 0.3s ease;
  display: inline-flex;
  font-size: 1.1rem;
}

.nav-link:hover {
  transform: scale(1.1);
  margin: 0 10px;
}
.nav-active {
  transition: all 0.3s ease;
  background: linear-gradient(135deg, rgba(235, 243, 255, 0.95) 0%, rgba(214, 228, 248, 0.92) 100%);
  border: 1px solid rgba(147, 174, 214, 0.7);
  color: #35507c;
  transform: scale(1.1);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.75),
    0 10px 18px -14px rgba(33, 49, 74, 0.55);
}

.more {
  border: 1px solid rgba(219, 228, 243, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px -18px rgba(33, 49, 74, 0.42);
  padding: 0px 15px;
  border-radius: 60px;
  cursor: pointer;
  backdrop-filter: blur(5px);
  height: 40px;
  transition: all 0.3s ease;
  font-weight: bold;
  overflow: hidden;
}
.more:hover {
  transform: scale(1.1);
  margin: 0 10px;
}
</style>
