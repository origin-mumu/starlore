import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const THEME_KEY = 'ro_blog_theme'

export type ThemeName = 'light' | 'dark'

function normalizeTheme(val: string | null): ThemeName {
  if (val === 'dark') return 'dark'
  return 'light'
}

export const useThemeStore = defineStore('theme', () => {
  const rawSaved = localStorage.getItem(THEME_KEY)
  const saved = normalizeTheme(rawSaved)
  const current = ref<ThemeName>(saved)

  const setTheme = (name: ThemeName) => {
    current.value = name
    localStorage.setItem(THEME_KEY, name)
    document.documentElement.setAttribute('data-theme', name)
  }

  const toggleTheme = () => {
    setTheme(current.value === 'dark' ? 'light' : 'dark')
  }

  // 初始化同步到 html
  document.documentElement.setAttribute('data-theme', saved)

  // 监听变化自动同步
  watch(current, (val) => {
    document.documentElement.setAttribute('data-theme', val)
  })

  return { current, setTheme, toggleTheme }
})

