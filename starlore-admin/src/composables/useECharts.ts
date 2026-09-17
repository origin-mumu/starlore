import { onUnmounted, shallowRef, type Ref } from 'vue'
import type { EChartsOption } from 'echarts'

type EChartsInstance = import('echarts').ECharts

/**
 * ECharts 生命周期托管：懒加载实例、窗口 resize 自适应、
 * 卸载时移除监听并销毁实例。
 */
export function useECharts(containerRef: Ref<HTMLElement | undefined>) {
  const chart = shallowRef<EChartsInstance | null>(null)

  const ensureInstance = async (): Promise<EChartsInstance | null> => {
    if (!containerRef.value) return null
    if (!chart.value) {
      const echarts = await import('echarts')
      if (!containerRef.value) return null
      chart.value = echarts.init(containerRef.value)
    }
    return chart.value
  }

  const render = async (option: EChartsOption): Promise<void> => {
    const instance = await ensureInstance()
    instance?.setOption(option)
  }

  const resize = (): void => {
    chart.value?.resize()
  }

  window.addEventListener('resize', resize)

  onUnmounted(() => {
    window.removeEventListener('resize', resize)
    chart.value?.dispose()
    chart.value = null
  })

  return { chart, render }
}
