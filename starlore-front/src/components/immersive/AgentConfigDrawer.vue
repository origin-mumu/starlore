<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import {
  Sliders,
  ChevronDown,
  Sparkles,
  Zap,
  Check,
  RotateCw,
  X,
} from '@lucide/vue'
import { useCompanionStore } from '@/stores/companion'
import EmotionBall from '@/components/EmotionBall.vue'
import {
  COLOR_IDS,
  colorHex,
  encodeFlat,
  encodeGrad,
  encodeRadial,
  parseInk,
} from '@/replica/catalog'

interface Props {
  open: boolean
  agentConfigForm: {
    modelName: string
    similarityThreshold: number
    topK: number
    temperature: number
    enableRerank: number
  }
  availableModels: { id: string; name: string; configured?: boolean }[]
  agentConfigSavedHint?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save'): void
}>()

const companionStore = useCompanionStore()
const activeSubTab = ref<'mascot' | 'params'>('mascot')
const modelSelectOpen = ref(false)
const previewBallRef = ref<InstanceType<typeof EmotionBall> | null>(null)
const companionSavedHint = ref('')

// 颜色模式: 'flat' (纯色) | 'grad' (线性渐变) | 'radial' (径向渐变)
const colorMode = ref<'flat' | 'grad' | 'radial'>('flat')
const flatHex = ref('#0e74e0')
const gradFrom = ref('#ff8838')
const gradTo = ref('#0e74e0')
const gradAngle = ref(135)

// 表单草稿状态
const draftConfig = reactive({
  shape: companionStore.shape || 'blob',
  color: companionStore.color || '',
  expression: companionStore.expression || 'curious',
  follow: companionStore.follow ?? true,
  autoTricks: companionStore.autoTricks ?? true,
})

// 解析初始色彩模式
function syncColorFromDraft() {
  const spec = parseInk(draftConfig.color)
  if (spec.kind === 'preset') {
    colorMode.value = 'flat'
    flatHex.value = colorHex(spec.id)
  } else if (spec.kind === 'flat') {
    colorMode.value = 'flat'
    flatHex.value = spec.hex
  } else if (spec.kind === 'grad') {
    colorMode.value = 'grad'
    gradFrom.value = spec.from
    gradTo.value = spec.to
    gradAngle.value = spec.angle
  } else if (spec.kind === 'radial') {
    colorMode.value = 'radial'
    gradFrom.value = spec.from
    gradTo.value = spec.to
  }
}

watch(
  () => props.open,
  (val) => {
    if (val) {
      draftConfig.shape = companionStore.shape || 'blob'
      draftConfig.color = companionStore.color || ''
      draftConfig.expression = companionStore.expression || 'curious'
      draftConfig.follow = companionStore.follow ?? true
      draftConfig.autoTricks = companionStore.autoTricks ?? true
      syncColorFromDraft()
      fetchProvidersAndModels()
    }
  },
  { immediate: true }
)

// 8 种支持的形态
const SHAPE_OPTIONS = [
  { id: 'blob', name: '水滴' },
  { id: 'bean', name: '蚕豆' },
  { id: 'egg', name: '星蛋' },
  { id: 'cloud', name: '软云' },
  { id: 'leaf', name: '嫩叶' },
  { id: 'squircle', name: '方圆' },
  { id: 'teardrop', name: '星泪' },
  { id: 'capsule', name: '胶囊' },
]

// 预设纯色色盘
const COLOR_PRESETS = [
  { id: 'cyan', name: '极光青蓝', hex: '#1aa8c4' },
  { id: 'blue', name: '深海湛蓝', hex: '#0E74E0' },
  { id: 'violet', name: '星云幽紫', hex: '#804EE0' },
  { id: 'magenta', name: '赛博品红', hex: '#E02A88' },
  { id: 'red', name: '星火赤红', hex: '#E02135' },
  { id: 'orange', name: '耀阳金橙', hex: '#E05B00' },
  { id: 'yellow', name: '星辉灿黄', hex: '#E08600' },
  { id: 'green', name: '星际薄荷', hex: '#009957' },
  { id: 'black', name: '深空暗曜', hex: '#111111' },
]

// 渐变预设角度
const ANGLE_PRESETS = [0, 45, 90, 135, 180, 270]

// 预设神态 (纯文字)
const EXPRESSION_OPTIONS = [
  { id: 'curious', name: '好奇' },
  { id: 'idle', name: '待命' },
  { id: 'happy', name: '欢快' },
  { id: 'excited', name: '兴奋' },
  { id: 'thinking', name: '沉思' },
  { id: 'listening', name: '倾听' },
  { id: 'orbit', name: '星轨' },
  { id: 'celebrate', name: '庆祝' },
  { id: 'proud', name: '自豪' },
  { id: 'shy', name: '害羞' },
]

// 应用纯色
function selectFlatPreset(hex: string) {
  flatHex.value = hex
  colorMode.value = 'flat'
  draftConfig.color = encodeFlat(hex)
}

function handleCustomFlatColor(e: Event) {
  const val = (e.target as HTMLInputElement).value
  flatHex.value = val
  draftConfig.color = encodeFlat(val)
}

// 应用渐变
function updateGradient() {
  if (colorMode.value === 'grad') {
    draftConfig.color = encodeGrad(gradFrom.value, gradTo.value, gradAngle.value)
  } else if (colorMode.value === 'radial') {
    draftConfig.color = encodeRadial(gradFrom.value, gradTo.value)
  }
}

function switchColorMode(mode: 'flat' | 'grad' | 'radial') {
  colorMode.value = mode
  if (mode === 'flat') {
    draftConfig.color = encodeFlat(flatHex.value)
  } else if (mode === 'grad') {
    draftConfig.color = encodeGrad(gradFrom.value, gradTo.value, gradAngle.value)
  } else if (mode === 'radial') {
    draftConfig.color = encodeRadial(gradFrom.value, gradTo.value)
  }
}

import { getAiProviders, getProviderModels, type AiProviderInfo, type ProviderModelInfo } from '@/api/ai'

const providers = ref<AiProviderInfo[]>([])
const selectedProviderKey = ref<string>('')
const providerModels = ref<ProviderModelInfo[]>([])
const isLoadingModels = ref(false)
const providerSelectOpen = ref(false)

const currentProvider = computed(() => {
  return providers.value.find((p) => p.key === selectedProviderKey.value)
})

const currentProviderLabel = computed(() => {
  if (currentProvider.value) {
    return `${currentProvider.value.name} (${currentProvider.value.key})`
  }
  return selectedProviderKey.value || '选择 AI 厂商'
})

const defaultFallbackModels = [
  { id: 'deepseek-chat', name: 'deepseek-chat (DeepSeek-V3)' },
  { id: 'deepseek-reasoner', name: 'deepseek-reasoner (DeepSeek-R1 深度思考)' },
  { id: 'deepseek-coder', name: 'deepseek-coder (代码大模型)' },
]

const currentModelLabel = computed(() => {
  const allModels = providerModels.value.length ? providerModels.value : defaultFallbackModels
  const m = allModels.find((item) => item.id === props.agentConfigForm.modelName)
  if (m) return `${m.name} (${m.id})`
  const fallback = props.availableModels.find((item) => item.id === props.agentConfigForm.modelName)
  if (fallback) return `${fallback.name} (${fallback.id})`
  return props.agentConfigForm.modelName || '请选择模型'
})

async function fetchProvidersAndModels() {
  try {
    const res = await getAiProviders()
    if (res.success && res.providers && res.providers.length) {
      providers.value = res.providers
    } else {
      providers.value = [
        { key: 'deepseek', name: 'DeepSeek', apiUrl: 'https://api.deepseek.com', defaultModel: 'deepseek-chat', enabled: true, configured: true }
      ]
    }
  } catch (err) {
    console.error('获取厂商失败:', err)
    providers.value = [
      { key: 'deepseek', name: 'DeepSeek', apiUrl: 'https://api.deepseek.com', defaultModel: 'deepseek-chat', enabled: true, configured: true }
    ]
  }

  const currentModel = props.agentConfigForm.modelName || 'deepseek-chat'
  let matched = providers.value.find(p =>
    p.key === currentModel ||
    p.defaultModel === currentModel ||
    (currentModel && (currentModel.toLowerCase().includes(p.key.toLowerCase()) || p.name.toLowerCase().includes(currentModel.toLowerCase())))
  )
  if (!matched && providers.value.length) {
    matched = providers.value[0]
  }
  if (matched) {
    selectedProviderKey.value = matched.key
    await fetchModelsForProvider(matched.key, false)
  }
}

async function fetchModelsForProvider(providerKey: string, autoSelectFirst = false) {
  if (!providerKey) return
  isLoadingModels.value = true
  try {
    const res = await getProviderModels(providerKey)
    if (res.success && res.models && res.models.length) {
      providerModels.value = res.models
      if (autoSelectFirst || !providerModels.value.some(m => m.id === props.agentConfigForm.modelName)) {
        const defaultM = providerModels.value[0]
        if (defaultM) {
          props.agentConfigForm.modelName = defaultM.id
        }
      }
    } else {
      providerModels.value = defaultFallbackModels
    }
  } catch (err) {
    console.error('拉取官方模型失败:', err)
    providerModels.value = defaultFallbackModels
  } finally {
    isLoadingModels.value = false
  }
}

function handleSelectProvider(p: AiProviderInfo) {
  selectedProviderKey.value = p.key
  providerSelectOpen.value = false
  fetchModelsForProvider(p.key, true)
}

function handleRefreshModels() {
  if (selectedProviderKey.value) {
    fetchModelsForProvider(selectedProviderKey.value, false)
  }
}

function handleOutsideClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.custom-select')) {
    modelSelectOpen.value = false
    providerSelectOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('click', handleOutsideClick)
  fetchProvidersAndModels()
})

function handleSaveAll() {
  if (activeSubTab.value === 'mascot') {
    companionStore.saveConfig({
      shape: draftConfig.shape,
      color: draftConfig.color,
      expression: draftConfig.expression,
      follow: draftConfig.follow,
      autoTricks: draftConfig.autoTricks,
    })
    companionSavedHint.value = '✓ AI 伴侣形象已保存！'
    setTimeout(() => {
      companionSavedHint.value = ''
      emit('close')
    }, 900)
  } else {
    emit('save')
  }
}

function spinPreview() {
  previewBallRef.value?.spin(1)
}

function bouncePreview() {
  previewBallRef.value?.bounce()
}

onMounted(() => {
  window.addEventListener('click', handleOutsideClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleOutsideClick)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="open" class="pure-modal-backdrop" @click.self="emit('close')">
        <div class="pure-modal-card" role="dialog" aria-modal="true">
          <!-- 弹窗顶部栏：无分割线 -->
          <header class="pure-modal-header">
            <div class="header-left">
              <h3>AI 伴侣与模型设置</h3>
              <!-- 子标签切换胶囊 -->
              <div class="tab-pills" role="tablist">
                <button
                  type="button"
                  class="tab-pill"
                  :class="{ active: activeSubTab === 'mascot' }"
                  @click="activeSubTab = 'mascot'"
                >
                  <Sparkles :size="13" />
                  <span>AI 形象个性</span>
                </button>
                <button
                  type="button"
                  class="tab-pill"
                  :class="{ active: activeSubTab === 'params' }"
                  @click="activeSubTab = 'params'"
                >
                  <Sliders :size="13" />
                  <span>模型与 RAG 调参</span>
                </button>
              </div>
            </div>

            <button class="pure-close-btn" @click="emit('close')" aria-label="关闭">
              <X :size="18" />
            </button>
          </header>

          <!-- 弹窗内容主体：固定容器高度与平滑切换 -->
          <div class="pure-modal-body">
            <!-- ── 子面板 1: AI 形象个性 ── -->
            <div v-show="activeSubTab === 'mascot'" class="mascot-layout">
              <!-- 左侧：实时小球预览区 (上下绝对居中) -->
              <div class="preview-stage-wrap">
                <div class="preview-stage">
                  <EmotionBall
                    ref="previewBallRef"
                    :size="140"
                    :shape="draftConfig.shape"
                    :color="draftConfig.color"
                    :emotion="draftConfig.expression"
                    :follow="draftConfig.follow"
                    :auto-tricks="draftConfig.autoTricks"
                    :show-rings="true"
                  />
                </div>

                <div class="stage-actions">
                  <button type="button" class="stage-btn" @click="spinPreview">
                    <RotateCw :size="12" />
                    <span>转圈</span>
                  </button>
                  <button type="button" class="stage-btn" @click="bouncePreview">
                    <Zap :size="12" />
                    <span>弹跳</span>
                  </button>
                </div>
              </div>

              <!-- 右侧：控件配置区 -->
              <div class="controls-wrap">
                <!-- 形态选择 -->
                <div class="form-row">
                  <label class="form-label">形态几何 (Shape)</label>
                  <div class="shape-grid">
                    <button
                      v-for="s in SHAPE_OPTIONS"
                      :key="s.id"
                      type="button"
                      class="shape-btn"
                      :class="{ active: draftConfig.shape === s.id }"
                      @click="draftConfig.shape = s.id"
                    >
                      {{ s.name }}
                    </button>
                  </div>
                </div>

                <!-- 色彩与渐变工坊 (Color Studio) -->
                <div class="form-row">
                  <div class="label-with-mode">
                    <label class="form-label">色彩与材质 (Color Studio)</label>
                    <div class="color-mode-pills">
                      <button
                        type="button"
                        class="mode-pill"
                        :class="{ active: colorMode === 'flat' }"
                        @click="switchColorMode('flat')"
                      >
                        纯色
                      </button>
                      <button
                        type="button"
                        class="mode-pill"
                        :class="{ active: colorMode === 'grad' }"
                        @click="switchColorMode('grad')"
                      >
                        线性渐变
                      </button>
                      <button
                        type="button"
                        class="mode-pill"
                        :class="{ active: colorMode === 'radial' }"
                        @click="switchColorMode('radial')"
                      >
                        径向渐变
                      </button>
                    </div>
                  </div>

                  <!-- 模式 1: 纯色与预设色块 -->
                  <div v-if="colorMode === 'flat'" class="color-row">
                    <button
                      v-for="c in COLOR_PRESETS"
                      :key="c.id"
                      type="button"
                      class="color-dot"
                      :class="{ active: flatHex.toLowerCase() === c.hex.toLowerCase() }"
                      :style="{ background: c.hex }"
                      :title="c.name"
                      @click="selectFlatPreset(c.hex)"
                    >
                      <Check v-if="flatHex.toLowerCase() === c.hex.toLowerCase()" :size="12" class="check-icon" />
                    </button>

                    <!-- 自定义纯色拾色器 -->
                    <label class="color-custom-label" title="自定义 HEX 颜色">
                      <span class="custom-color-indicator" :style="{ background: flatHex }"></span>
                      <input type="color" :value="flatHex" class="native-color-input" @input="handleCustomFlatColor" />
                    </label>
                  </div>

                  <!-- 模式 2: 线性渐变配置 -->
                  <div v-else-if="colorMode === 'grad'" class="gradient-panel">
                    <div class="grad-stops-row">
                      <div class="grad-stop-item">
                        <span class="stop-label">起始色</span>
                        <label class="color-custom-label" :style="{ background: gradFrom }">
                          <input type="color" v-model="gradFrom" class="native-color-input" @input="updateGradient" />
                        </label>
                      </div>
                      <div class="grad-bar-preview" :style="{ background: `linear-gradient(${gradAngle}deg, ${gradFrom}, ${gradTo})` }"></div>
                      <div class="grad-stop-item">
                        <span class="stop-label">终止色</span>
                        <label class="color-custom-label" :style="{ background: gradTo }">
                          <input type="color" v-model="gradTo" class="native-color-input" @input="updateGradient" />
                        </label>
                      </div>
                    </div>

                    <div class="angle-control-row">
                      <div class="angle-label-val">
                        <span>角度 (Angle)</span>
                        <span class="angle-tag">{{ gradAngle }}°</span>
                      </div>
                      <input
                        type="range"
                        v-model.number="gradAngle"
                        min="0"
                        max="360"
                        step="5"
                        class="pure-range"
                        @input="updateGradient"
                      />
                      <div class="angle-presets">
                        <button
                          v-for="a in ANGLE_PRESETS"
                          :key="a"
                          type="button"
                          class="angle-btn"
                          :class="{ active: gradAngle === a }"
                          @click="gradAngle = a; updateGradient()"
                        >
                          {{ a }}°
                        </button>
                      </div>
                    </div>
                  </div>

                  <!-- 模式 3: 径向渐变配置 -->
                  <div v-else-if="colorMode === 'radial'" class="gradient-panel">
                    <div class="grad-stops-row">
                      <div class="grad-stop-item">
                        <span class="stop-label">中心色</span>
                        <label class="color-custom-label" :style="{ background: gradFrom }">
                          <input type="color" v-model="gradFrom" class="native-color-input" @input="updateGradient" />
                        </label>
                      </div>
                      <div class="grad-bar-preview" :style="{ background: `radial-gradient(circle at 50% 50%, ${gradFrom}, ${gradTo})` }"></div>
                      <div class="grad-stop-item">
                        <span class="stop-label">外边缘色</span>
                        <label class="color-custom-label" :style="{ background: gradTo }">
                          <input type="color" v-model="gradTo" class="native-color-input" @input="updateGradient" />
                        </label>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 基础神态选择 (纯文字标签) -->
                <div class="form-row">
                  <label class="form-label">常态神情 (Default Expression)</label>
                  <div class="expression-row">
                    <button
                      v-for="exp in EXPRESSION_OPTIONS"
                      :key="exp.id"
                      type="button"
                      class="expression-btn"
                      :class="{ active: draftConfig.expression === exp.id }"
                      @click="draftConfig.expression = exp.id"
                    >
                      <span>{{ exp.name }}</span>
                    </button>
                  </div>
                </div>

                <!-- 交互开关 -->
                <div class="switches-grid">
                  <div class="switch-row">
                    <div>
                      <span class="switch-title">鼠标视线追随</span>
                    </div>
                    <label class="pure-switch">
                      <input type="checkbox" v-model="draftConfig.follow" />
                      <span class="pure-slider"></span>
                    </label>
                  </div>

                  <div class="switch-row">
                    <div>
                      <span class="switch-title">自发微动作</span>
                    </div>
                    <label class="pure-switch">
                      <input type="checkbox" v-model="draftConfig.autoTricks" />
                      <span class="pure-slider"></span>
                    </label>
                  </div>
                </div>
              </div>
            </div>

            <!-- ── 子面板 2: 模型与 RAG 调参 ── -->
            <div v-show="activeSubTab === 'params'" class="params-layout">
              <div class="form-grid-2">
                <!-- 厂商选择 (Provider) -->
                <div class="form-field">
                  <label class="form-label">AI 厂商 (Provider)</label>
                  <div class="custom-select">
                    <div class="custom-select-trigger" @click.stop="providerSelectOpen = !providerSelectOpen; modelSelectOpen = false">
                      <span>{{ currentProviderLabel }}</span>
                      <ChevronDown class="arrow-icon" :class="{ 'is-open': providerSelectOpen }" />
                    </div>
                    <Transition name="dropdown-fade">
                      <div v-if="providerSelectOpen" class="custom-select-options">
                        <div
                          v-for="p in providers"
                          :key="p.key"
                          class="custom-select-option"
                          :class="{ active: selectedProviderKey === p.key }"
                          @click="handleSelectProvider(p)"
                        >
                          {{ p.name }} ({{ p.key }})
                        </div>
                      </div>
                    </Transition>
                  </div>
                  <p class="field-hint">从数据库中配置的厂商服务切换</p>
                </div>

                <!-- 驱动大模型 (LLM Model) -->
                <div class="form-field">
                  <div class="label-with-val">
                    <label class="form-label">驱动大模型 (LLM Model)</label>
                    <button
                      type="button"
                      class="refresh-models-btn"
                      :class="{ 'is-spinning': isLoadingModels }"
                      title="从官方接口拉取最新模型列表"
                      @click.stop="handleRefreshModels"
                    >
                      <RotateCw :size="12" />
                      <span>{{ isLoadingModels ? '同步中...' : '同步官方模型' }}</span>
                    </button>
                  </div>
                  <div class="custom-select">
                    <div class="custom-select-trigger" @click.stop="modelSelectOpen = !modelSelectOpen; providerSelectOpen = false">
                      <span>{{ currentModelLabel }}</span>
                      <ChevronDown class="arrow-icon" :class="{ 'is-open': modelSelectOpen }" />
                    </div>
                    <Transition name="dropdown-fade">
                      <div v-if="modelSelectOpen" class="custom-select-options">
                        <div
                          v-for="model in (providerModels.length ? providerModels : defaultFallbackModels)"
                          :key="model.id"
                          class="custom-select-option"
                          :class="{ active: agentConfigForm.modelName === model.id }"
                          @click="
                            agentConfigForm.modelName = model.id;
                            modelSelectOpen = false
                          "
                        >
                          {{ model.name }} ({{ model.id }})
                        </div>
                      </div>
                    </Transition>
                  </div>
                  <p class="field-hint">由厂商官方接口动态提供最新模型</p>
                </div>

                <div class="form-field">
                  <div class="label-with-val">
                    <label class="form-label">模型随机度 (Temperature)</label>
                    <span class="val-tag">{{ agentConfigForm.temperature }}</span>
                  </div>
                  <input
                    type="range"
                    v-model.number="agentConfigForm.temperature"
                    min="0.0"
                    max="1.0"
                    step="0.1"
                    class="pure-range"
                  />
                  <p class="field-hint">值越低回答越聚焦精准，越高越具发散创造力</p>
                </div>

                <div class="form-field">
                  <div class="label-with-val">
                    <label class="form-label">向量检索相似度阈值</label>
                    <span class="val-tag">{{ agentConfigForm.similarityThreshold }}</span>
                  </div>
                  <input
                    type="range"
                    v-model.number="agentConfigForm.similarityThreshold"
                    min="0.1"
                    max="0.95"
                    step="0.05"
                    class="pure-range"
                  />
                  <p class="field-hint">过滤低于该分数的噪音切片 (推荐 0.55 - 0.70)</p>
                </div>

                <div class="form-field">
                  <div class="label-with-val">
                    <label class="form-label">检索最大切片数 (Top-K)</label>
                    <span class="val-tag">{{ agentConfigForm.topK }} 条</span>
                  </div>
                  <input
                    type="range"
                    v-model.number="agentConfigForm.topK"
                    min="1"
                    max="10"
                    step="1"
                    class="pure-range"
                  />
                  <p class="field-hint">限制送入大模型的参考上下文切片数量</p>
                </div>
              </div>

              <div class="switch-row" style="margin-top: 14px;">
                <div>
                  <span class="switch-title">启用 BM25 混合检索与重排序 (Rerank)</span>
                  <p class="switch-desc">融合关键词匹配与向量语义多路重排</p>
                </div>
                <label class="pure-switch">
                  <input
                    type="checkbox"
                    :checked="agentConfigForm.enableRerank === 1"
                    @change="
                      agentConfigForm.enableRerank = ($event.target as HTMLInputElement).checked ? 1 : 0
                    "
                  />
                  <span class="pure-slider"></span>
                </label>
              </div>
            </div>
          </div>

          <!-- 弹窗底部操作栏：无分割线 -->
          <footer class="pure-modal-footer">
            <div class="footer-hint">
              <span v-if="companionSavedHint" class="hint-success">{{ companionSavedHint }}</span>
              <span v-else-if="agentConfigSavedHint" class="hint-success">{{ agentConfigSavedHint }}</span>
            </div>

            <div class="footer-actions">
              <button type="button" class="btn-cancel" @click="emit('close')">取消</button>
              <button type="button" class="btn-save" @click="handleSaveAll">
                保存配置
              </button>
            </div>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 纯净暗调遮罩背景：高 z-index 覆盖全屏包括顶栏 */
.pure-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

/* 纯白弹窗卡片：固定宽高以防止 Tab 切换时尺寸剧烈跳动 */
.pure-modal-card {
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.16);
  width: 760px;
  max-width: calc(100vw - 32px);
  height: 560px;
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalScaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalScaleIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(6px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 顶部栏：无 border-bottom */
.pure-modal-header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 26px 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h3 {
  margin: 0;
  font-size: 1.12rem;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.01em;
}

/* 标签切换胶囊 */
.tab-pills {
  display: flex;
  gap: 4px;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 999px;
}

.tab-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 0.76rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tab-pill.active {
  background: #ffffff;
  color: #337BF4;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.pure-close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pure-close-btn:hover {
  background: #f1f5f9;
  color: #1e293b;
}

/* 内容主体：弹性撑满并支持滚动 */
.pure-modal-body {
  flex: 1 1 0;
  min-height: 0;
  overflow-y: auto;
  padding: 6px 26px 12px;
  display: flex;
  flex-direction: column;
}

/* 伴侣形象双列布局：左侧上下居中 */
.mascot-layout {
  flex: 1;
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 20px;
  align-items: center;
}

.preview-stage-wrap {
  align-self: center;
  justify-self: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 10px;
  background: #f8fafc;
  border-radius: 16px;
  width: 100%;
}

.preview-stage {
  width: 130px;
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stage-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.stage-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.stage-btn:hover {
  border-color: #337BF4;
  color: #337BF4;
}

/* 控件区 */
.controls-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: #334155;
}

/* 4列形态药丸 */
.shape-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 5px;
}

.shape-btn {
  padding: 5px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  color: #475569;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: center;
}

.shape-btn:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.shape-btn.active {
  border-color: #337BF4;
  background: #eef6ff;
  color: #337BF4;
}

/* 色彩与渐变工坊 */
.label-with-mode {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.color-mode-pills {
  display: flex;
  gap: 2px;
  background: #f1f5f9;
  padding: 2px;
  border-radius: 999px;
}

.mode-pill {
  padding: 2px 8px;
  font-size: 0.68rem;
  font-weight: 600;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mode-pill.active {
  background: #ffffff;
  color: #337BF4;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.color-row {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
}

.color-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s ease;
}

.color-dot:hover {
  transform: scale(1.15);
}

.color-dot.active {
  border-color: #337BF4;
  transform: scale(1.12);
}

.check-icon {
  color: #ffffff;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.4));
}

.color-custom-label {
  position: relative;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #cbd5e1;
  overflow: hidden;
}

.custom-color-indicator {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.native-color-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
  width: 100%;
  height: 100%;
}

/* 渐变面板 */
.gradient-panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #f8fafc;
  padding: 8px 12px;
  border-radius: 12px;
}

.grad-stops-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.grad-stop-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stop-label {
  font-size: 0.68rem;
  font-weight: 600;
  color: #64748b;
}

.grad-bar-preview {
  flex: 1;
  height: 14px;
  border-radius: 999px;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
}

.angle-control-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.angle-label-val {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.68rem;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}

.angle-tag {
  color: #337BF4;
  background: #eef6ff;
  padding: 0 4px;
  border-radius: 4px;
}

.angle-presets {
  display: flex;
  gap: 3px;
}

.angle-btn {
  padding: 1px 5px;
  font-size: 0.64rem;
  font-weight: 600;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  cursor: pointer;
}

.angle-btn.active {
  background: #337BF4;
  border-color: #337BF4;
  color: #ffffff;
}

/* 神态药丸 */
.expression-row {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.expression-btn {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.expression-btn:hover {
  background: #f8fafc;
}

.expression-btn.active {
  border-color: #337BF4;
  background: #eef6ff;
  color: #337BF4;
}

/* 开关行 */
.switches-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
  padding: 6px 10px;
  border-radius: 10px;
}

.switch-title {
  font-size: 0.74rem;
  font-weight: 600;
  color: #334155;
}

/* 调参网格 */
.params-layout {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 18px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.label-with-val {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.val-tag {
  font-size: 0.72rem;
  font-weight: 700;
  color: #337BF4;
  background: #eef6ff;
  padding: 1px 6px;
  border-radius: 6px;
}

.pure-range {
  width: 100%;
  accent-color: #337BF4;
  cursor: pointer;
  margin: 2px 0;
}

.form-field {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-field:focus-within {
  z-index: 30;
}

.label-with-val {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.val-tag {
  font-size: 0.72rem;
  font-weight: 700;
  color: #337BF4;
  background: #eef6ff;
  padding: 1px 6px;
  border-radius: 6px;
}

.pure-range {
  width: 100%;
  accent-color: #337BF4;
  cursor: pointer;
  margin: 2px 0;
}

.field-hint {
  font-size: 0.66rem;
  color: #94a3b8;
  margin: 0;
}

/* 下拉框 (记忆看板药丸风格) */
.custom-select {
  position: relative;
  width: 100%;
  user-select: none;
  z-index: 10;
}

.custom-select:has(.custom-select-options) {
  z-index: 500;
}

.custom-select-trigger {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  min-height: 38px;
  height: 38px;
  padding: 0 14px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s ease;
}

.custom-select-trigger:hover {
  border-color: #337BF4;
}

.arrow-icon {
  width: 14px;
  height: 14px;
  color: #94a3b8;
  transition: transform 0.15s ease;
}
.arrow-icon.is-open {
  transform: rotate(180deg);
  color: #337BF4;
}

.custom-select-options {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: #ffffff !important;
  border: 1px solid #cbd5e1 !important;
  border-radius: 14px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.18), 0 2px 8px rgba(0, 0, 0, 0.06) !important;
  z-index: 99999 !important;
  max-height: 220px;
  overflow-y: auto;
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.custom-select-option {
  padding: 8px 12px;
  font-size: 0.8rem;
  font-weight: 550;
  color: #475569;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.12s ease;
}

.custom-select-option:hover {
  background: rgba(51, 123, 244, 0.08);
  color: #337BF4;
}

.custom-select-option.active {
  background: #337BF4;
  color: #ffffff !important;
  font-weight: 650;
}

/* 纯净开关 */
.pure-switch {
  position: relative;
  display: inline-block;
  width: 34px;
  height: 18px;
  flex-shrink: 0;
}
.pure-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}
.pure-slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd5e1;
  transition: 0.25s;
  border-radius: 20px;
}
.pure-slider:before {
  position: absolute;
  content: '';
  height: 12px;
  width: 12px;
  left: 3px;
  bottom: 3px;
  background-color: #ffffff;
  transition: 0.25s;
  border-radius: 50%;
}
input:checked + .pure-slider {
  background-color: #337BF4;
}
input:checked + .pure-slider:before {
  transform: translateX(16px);
}

/* 底部操作栏：无 border-top */
.pure-modal-footer {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 26px 18px;
}

.footer-hint {
  font-size: 0.76rem;
  color: #16a34a;
  font-weight: 600;
}

.footer-actions {
  display: flex;
  gap: 10px;
}

.btn-cancel {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  height: 38px;
  padding: 0 20px;
  font-size: 0.84rem;
  font-weight: 600;
  border-radius: 999px;
  border: none;
  background: #f1f5f9;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-cancel:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.btn-save {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  height: 38px;
  padding: 0 22px;
  font-size: 0.84rem;
  font-weight: 600;
  border-radius: 999px;
  border: none;
  background: #337BF4;
  color: #ffffff !important;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(51, 123, 244, 0.28);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-save:hover {
  background: #2563eb;
  box-shadow: 0 4px 14px rgba(51, 123, 244, 0.38);
  transform: translateY(-1px);
}

/* 暗色模式适配 */
:global([data-theme="dark"]) .pure-modal-card {
  background: #181b26;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.6);
}

:global([data-theme="dark"]) .pure-modal-header h3 {
  color: #f1f5f9;
}

:global([data-theme="dark"]) .tab-pills,
:global([data-theme="dark"]) .color-mode-pills {
  background: #0f121a;
}

:global([data-theme="dark"]) .tab-pill.active,
:global([data-theme="dark"]) .mode-pill.active {
  background: #242938;
  color: #60a5fa;
}

:global([data-theme="dark"]) .preview-stage-wrap,
:global([data-theme="dark"]) .gradient-panel,
:global([data-theme="dark"]) .switch-row {
  background: #0f121a;
}

:global([data-theme="dark"]) .stage-btn,
:global([data-theme="dark"]) .shape-btn,
:global([data-theme="dark"]) .expression-btn,
:global([data-theme="dark"]) .angle-btn,
:global([data-theme="dark"]) .custom-select-trigger {
  background: #242938;
  border-color: #333d52;
  color: #cbd5e1;
}

:global([data-theme="dark"]) .shape-btn.active,
:global([data-theme="dark"]) .expression-btn.active {
  background: #1e3a8a;
  border-color: #337BF4;
  color: #93c5fd;
}

:global([data-theme="dark"]) .custom-select-options {
  background: #242938;
  border-color: #333d52;
}

:global([data-theme="dark"]) .custom-select-option:hover {
  background: #333d52;
}

:global([data-theme="dark"]) .form-label,
:global([data-theme="dark"]) .switch-title {
  color: #e2e8f0;
}

:global([data-theme="dark"]) .btn-cancel {
  background: #242a38;
  color: #cbd5e1;
}

:global([data-theme="dark"]) .btn-cancel:hover {
  background: #333d52;
  color: #ffffff;
}

.refresh-models-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: rgba(51, 123, 244, 0.08);
  border: 1px solid rgba(51, 123, 244, 0.2);
  color: #337BF4;
  border-radius: 6px;
  padding: 3px 9px;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.refresh-models-btn:hover {
  background: #337BF4;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(51, 123, 244, 0.28);
}

.refresh-models-btn.is-spinning svg {
  animation: spin 0.8s linear infinite;
}

:global([data-theme="dark"]) .refresh-models-btn {
  background: rgba(51, 123, 244, 0.15);
  border-color: rgba(51, 123, 244, 0.3);
  color: #60a5fa;
}

:global([data-theme="dark"]) .refresh-models-btn:hover {
  background: #337BF4;
  color: #ffffff;
}

</style>
