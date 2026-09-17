<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Cpu } from '@element-plus/icons-vue'
import {
  getAiConfigsService,
  createAiConfigService,
  updateAiConfigService,
  deleteAiConfigService,
} from '@/api/ai-config'
import type { AiConfigItem, AiConfigPayload } from '@/types'
import AppModal from '@/components/common/AppModal.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadingState from '@/components/common/LoadingState.vue'

const configs = ref<AiConfigItem[]>([])
const loading = ref(false)
const saving = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentId = ref<number | null>(null)

const form = reactive<AiConfigPayload>({
  modelKey: '',
  modelName: '',
  apiUrl: '',
  modelId: '',
  apiKey: '',
  enabled: true,
})

const fetchConfigs = async (): Promise<void> => {
  loading.value = true
  try {
    configs.value = (await getAiConfigsService()) ?? []
  } catch (error) {
    console.error('获取AI配置失败:', error)
  } finally {
    loading.value = false
  }
}

const resetForm = (): void => {
  Object.assign(form, {
    modelKey: '',
    modelName: '',
    apiUrl: '',
    modelId: '',
    apiKey: '',
    enabled: true,
  })
  currentId.value = null
}

const openCreateDialog = (): void => {
  resetForm()
  dialogMode.value = 'create'
  dialogVisible.value = true
}

const openEditDialog = (config: AiConfigItem): void => {
  dialogMode.value = 'edit'
  currentId.value = config.id
  Object.assign(form, {
    modelKey: config.modelKey,
    modelName: config.modelName,
    apiUrl: config.apiUrl,
    modelId: config.modelId,
    apiKey: '',
    enabled: config.enabled,
  })
  dialogVisible.value = true
}

const saveConfig = async (): Promise<void> => {
  if (!form.modelKey.trim() || !form.modelId.trim()) {
    ElMessage.warning('请填写模型标识与模型 ID')
    return
  }
  saving.value = true
  try {
    if (dialogMode.value === 'edit' && currentId.value !== null) {
      // API 永不回传已存储密钥；留空表示保持原密钥不变
      const payload: Partial<AiConfigPayload> = { ...form }
      if (!form.apiKey) delete payload.apiKey
      await updateAiConfigService(currentId.value, payload)
    } else {
      await createAiConfigService({ ...form })
    }
    dialogVisible.value = false
    ElMessage.success('保存成功')
    await fetchConfigs()
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

const deleteConfig = async (id: number): Promise<void> => {
  if (!confirm('确定要删除该配置吗？')) return
  try {
    await deleteAiConfigService(id)
    ElMessage.success('删除成功')
    await fetchConfigs()
  } catch (error) {
    console.error('删除配置失败:', error)
    ElMessage.error('删除失败，请重试')
  }
}

const maskKey = (key: string): string => {
  if (!key) return '未设置'
  if (key.length <= 8) return '****'
  return `${key.slice(0, 4)}****${key.slice(-4)}`
}

const formatDate = (dateString: string): string =>
  dateString ? new Date(dateString).toLocaleDateString('zh-CN') : '-'

onMounted(() => {
  void fetchConfigs()
})
</script>

<template>
  <div class="ai-config">
    <header class="page-header">
      <div>
        <h1 class="page-title">AI 配置</h1>
        <p class="page-subtitle">大模型接入参数管理</p>
      </div>
      <button class="btn btn--primary" type="button" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        添加模型
      </button>
    </header>

    <LoadingState v-if="loading" />

    <EmptyState v-else-if="configs.length === 0" text="暂无 AI 模型配置，请点击右上角添加">
      <template #icon>
        <el-icon><Cpu /></el-icon>
      </template>
    </EmptyState>

    <div v-else class="data-table" style="--table-cols: 1fr 1fr 1.8fr 1fr 0.9fr 0.7fr 0.6fr 1fr">
      <div class="data-table__header">
        <div>模型标识</div>
        <div>显示名称</div>
        <div>API 地址</div>
        <div>模型 ID</div>
        <div>API 密钥</div>
        <div>状态</div>
        <div>创建时间</div>
        <div>操作</div>
      </div>
      <div v-for="config in configs" :key="config.id" class="data-table__row">
        <div><span class="pill pill--brand">{{ config.modelKey }}</span></div>
        <div><span class="cell-primary">{{ config.modelName }}</span></div>
        <div><span class="ai-config__url">{{ config.apiUrl }}</span></div>
        <div>{{ config.modelId }}</div>
        <div><span class="ai-config__key">{{ maskKey(config.apiKey) }}</span></div>
        <div>
          <span class="pill" :class="config.enabled ? 'pill--success' : 'pill--neutral'">
            {{ config.enabled ? '已启用' : '已禁用' }}
          </span>
        </div>
        <div>{{ formatDate(config.createdAt) }}</div>
        <div>
          <div class="ai-config__actions">
            <button class="btn btn--secondary btn--sm" type="button" @click="openEditDialog(config)">
              编辑
            </button>
            <button class="btn btn--danger btn--sm" type="button" @click="deleteConfig(config.id)">
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <AppModal
      :title="dialogMode === 'create' ? '添加模型配置' : '编辑模型配置'"
      :width="560"
      @close="dialogVisible = false"
    >
      <div class="ai-config__form">
        <div class="ai-config__form-row">
          <div class="form-field">
            <label>模型标识</label>
            <input v-model="form.modelKey" class="form-input" type="text" placeholder="如 qwen-plus、mimo" />
          </div>
          <div class="form-field">
            <label>显示名称</label>
            <input v-model="form.modelName" class="form-input" type="text" placeholder="如 通义千问 Plus" />
          </div>
        </div>
        <div class="form-field">
          <label>API 地址</label>
          <input
            v-model="form.apiUrl"
            class="form-input"
            type="text"
            placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
          />
        </div>
        <div class="ai-config__form-row">
          <div class="form-field">
            <label>模型 ID</label>
            <input v-model="form.modelId" class="form-input" type="text" placeholder="如 qwen-plus" />
          </div>
          <div class="form-field">
            <label>API 密钥</label>
            <input
              v-model="form.apiKey"
              class="form-input"
              type="password"
              :placeholder="dialogMode === 'edit' ? '留空则保持原密钥不变' : 'sk-...'"
              autocomplete="off"
            />
          </div>
        </div>
        <label class="ai-config__switch">
          <input v-model="form.enabled" type="checkbox" />
          <span>{{ form.enabled ? '已启用' : '已禁用' }}</span>
        </label>
      </div>
      <template #footer>
        <button class="btn btn--secondary" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn--primary" type="button" :disabled="saving" @click="saveConfig">
          {{ saving ? '保存中...' : '确认' }}
        </button>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.ai-config__url {
  font-size: 0.78rem;
  color: var(--text-muted);
  word-break: break-all;
}

.ai-config__key {
  font-family: var(--font-family-mono);
  font-size: 0.8rem;
  color: var(--text-muted);
}

.ai-config__actions {
  display: flex;
  gap: 8px;
}

.ai-config__form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ai-config__form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.ai-config__switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  color: var(--text-secondary);
  cursor: pointer;
  user-select: none;
}

.ai-config__switch input {
  accent-color: var(--brand-primary);
  width: 16px;
  height: 16px;
}
</style>
