import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const THEME_KEY = 'ro_blog_theme'

export type ThemeName = 'default' | 'white' | 'dark' | 'green' | 'blue' | 'pink'

export const useThemeStore = defineStore('theme', () => {
  const saved = (localStorage.getItem(THEME_KEY) as ThemeName) || 'default'
  const current = ref<ThemeName>(saved)

  const setTheme = (name: ThemeName) => {
    current.value = name
    localStorage.setItem(THEME_KEY, name)
    document.documentElement.setAttribute('data-theme', name)
  }

  // 初始化时同步到 html
  if (saved !== 'default') {
    document.documentElement.setAttribute('data-theme', saved)
  }

  // 监听变化自动同步
  watch(current, (val) => {
    document.documentElement.setAttribute('data-theme', val)
  })

  return { current, setTheme }
})
