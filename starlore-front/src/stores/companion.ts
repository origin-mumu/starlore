import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface CompanionConfig {
  shape: string
  color: string
  expression: string
  eyeColor?: string
  autoTricks?: boolean
  follow?: boolean
}

const STORAGE_KEY = 'starlore_custom_companion_config'

export const useCompanionStore = defineStore('companion', () => {
  // 从 localStorage 恢复或使用默认值
  const saved = (() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) return JSON.parse(raw) as Partial<CompanionConfig>
    } catch {}
    return {}
  })()

  const shape = ref<string>(saved.shape || 'blob')
  const color = ref<string>(saved.color || '')
  const expression = ref<string>(saved.expression || 'curious')
  const eyeColor = ref<string>(saved.eyeColor || '')
  const autoTricks = ref<boolean>(saved.autoTricks ?? true)
  const follow = ref<boolean>(saved.follow ?? true)

  function saveConfig(next: Partial<CompanionConfig>) {
    if (next.shape !== undefined) shape.value = next.shape
    if (next.color !== undefined) color.value = next.color
    if (next.expression !== undefined) expression.value = next.expression
    if (next.eyeColor !== undefined) eyeColor.value = next.eyeColor
    if (next.autoTricks !== undefined) autoTricks.value = next.autoTricks
    if (next.follow !== undefined) follow.value = next.follow

    const toPersist: CompanionConfig = {
      shape: shape.value,
      color: color.value,
      expression: expression.value,
      eyeColor: eyeColor.value,
      autoTricks: autoTricks.value,
      follow: follow.value,
    }

    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(toPersist))
      // 同时写入 grok-icon-study 兼容键名，便于 Studio 内部读取
      localStorage.setItem('forme', shape.value)
      if (color.value) localStorage.setItem('couleur', color.value)
      localStorage.setItem('expression', expression.value)
      if (eyeColor.value) localStorage.setItem('oeil', eyeColor.value)
    } catch {}
  }

  function resetDefault() {
    saveConfig({
      shape: 'blob',
      color: '',
      expression: 'curious',
      eyeColor: '',
      autoTricks: true,
      follow: true,
    })
  }

  return {
    shape,
    color,
    expression,
    eyeColor,
    autoTricks,
    follow,
    saveConfig,
    resetDefault,
  }
})
