<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Nav from '@/components/nav.vue'
import {
  getAiConfigsService,
  createAiConfigService,
  updateAiConfigService,
  deleteAiConfigService,
} from '@/api/ai-config'

interface AiConfig {
  id: number
  modelKey: string
  modelName: string
  apiUrl: string
  modelId: string
  apiKey: string
  enabled: boolean
  createdAt: string
}

const configs = ref<AiConfig[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加模型配置')
const currentId = ref<number | null>(null)

const form = ref({
  modelKey: '',
  modelName: '',
  apiUrl: '',
  modelId: '',
  apiKey: '',
  enabled: true,
})

const fetchConfigs = async () => {
  try {
    loading.value = true
    const res = await getAiConfigsService()
    configs.value = res.data || []
  } catch (error) {
    console.error('获取AI配置失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchConfigs()
})

const resetForm = () => {
  form.value = { modelKey: '', modelName: '', apiUrl: '', modelId: '', apiKey: '', enabled: true }
  currentId.value = null
}

const addConfig = () => {
  resetForm()
  dialogTitle.value = '添加模型配置'
  dialogVisible.value = true
}

const editConfig = (config: AiConfig) => {
  currentId.value = config.id
  dialogTitle.value = '编辑模型配置'
  form.value = {
    modelKey: config.modelKey,
    modelName: config.modelName,
    apiUrl: config.apiUrl,
    modelId: config.modelId,
    apiKey: config.apiKey || '',
    enabled: config.enabled,
  }
  dialogVisible.value = true
}

const saveConfig = async () => {
  try {
    loading.value = true
    if (currentId.value) {
      const payload: Record<string, unknown> = { ...form.value }
      // The API never returns stored secrets. A blank value keeps the existing key unchanged.
      if (!form.value.apiKey) delete payload.apiKey
      await updateAiConfigService(currentId.value, payload)
    } else {
      await createAiConfigService(form.value)
    }
    dialogVisible.value = false
    fetchConfigs()
  } catch (error) {
    console.error('保存配置失败:', error)
  } finally {
    loading.value = false
  }
}

const deleteConfig = async (id: number) => {
  if (!confirm('确定要删除该配置吗？')) return
  try {
    await deleteAiConfigService(id)
    fetchConfigs()
  } catch (error) {
    console.error('删除配置失败:', error)
  }
}

const maskKey = (key: string) => {
  if (!key) return '未设置'
  if (key.length <= 8) return '****'
  return key.substring(0, 4) + '****' + key.substring(key.length - 4)
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="home-layout">
    <div class="sidebar-container">
      <Nav></Nav>
    </div>
    <div class="content-container">
      <div class="content-box">
        <div class="page-header">
          <h1 class="page-title">AI 配置</h1>
          <button class="add-btn" @click="addConfig">
            <span class="btn-icon">➕</span>
            添加模型
          </button>
        </div>

        <div class="articles-table">
          <div v-if="loading" class="loading">加载中...</div>

          <div v-else-if="configs.length === 0" class="empty-state">
            <div class="empty-icon">🤖</div>
            <p>暂无 AI 模型配置，请点击上方按钮添加</p>
          </div>

          <div v-else class="table-container">
            <div class="table-header">
              <div class="header-cell">模型标识</div>
              <div class="header-cell">显示名称</div>
              <div class="header-cell">API 地址</div>
              <div class="header-cell">模型 ID</div>
              <div class="header-cell">API 密钥</div>
              <div class="header-cell">状态</div>
              <div class="header-cell">操作</div>
            </div>

            <div class="table-body">
              <div v-for="config in configs" :key="config.id" class="table-row">
                <div class="table-cell">
                  <span class="model-key">{{ config.modelKey }}</span>
                </div>
                <div class="table-cell">{{ config.modelName }}</div>
                <div class="table-cell url-cell">{{ config.apiUrl }}</div>
                <div class="table-cell">{{ config.modelId }}</div>
                <div class="table-cell">
                  <span class="key-mask">{{ maskKey(config.apiKey) }}</span>
                </div>
                <div class="table-cell">
                  <span :class="['status-badge', config.enabled ? 'published' : 'draft']">
                    {{ config.enabled ? '已启用' : '已禁用' }}
                  </span>
                </div>
                <div class="table-cell">
                  <div class="action-buttons">
                    <button class="action-btn edit-btn" @click="editConfig(config)">编辑</button>
                    <button class="action-btn delete-btn" @click="deleteConfig(config.id)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580">
          <div class="form-group">
            <label class="form-label">模型标识</label>
            <el-input v-model="form.modelKey" placeholder="如 qwen-plus、mimo" clearable />
          </div>
          <div class="form-group">
            <label class="form-label">显示名称</label>
            <el-input v-model="form.modelName" placeholder="如 通义千问 Plus" clearable />
          </div>
          <div class="form-group">
            <label class="form-label">API 地址</label>
            <el-input v-model="form.apiUrl" placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions" clearable />
          </div>
          <div class="form-group">
            <label class="form-label">模型 ID</label>
            <el-input v-model="form.modelId" placeholder="如 qwen-plus、mimo-v2-flash" clearable />
          </div>
          <div class="form-group">
            <label class="form-label">API 密钥</label>
            <el-input v-model="form.apiKey" placeholder="sk-..." show-password clearable />
          </div>
          <div class="form-group">
            <label class="form-label">启用状态</label>
            <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
          </div>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="dialogVisible = false">取消</el-button>
              <el-button type="primary" @click="saveConfig">确认</el-button>
            </div>
          </template>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--border);
}

.page-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
  letter-spacing: -0.02em;
}

.add-btn {
  background: var(--accent);
  color: #FDFBF5;
  border: none;
  padding: 10px 22px;
  border-radius: var(--radius-md);
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: var(--shadow-button);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.add-btn:hover {
  background: var(--accent-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-button-hover);
}

.articles-table {
  margin-bottom: var(--space-6);
}

.loading,
.empty-state {
  text-align: center;
  padding: var(--space-8) var(--space-5);
  color: var(--ink-muted);
  font-size: 1rem;
}

.empty-icon {
  font-size: 2.5rem;
  margin-bottom: var(--space-4);
}

.table-container {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 1fr 1fr 2fr 1fr 1fr 0.7fr 1fr;
  background: var(--canvas-deep);
  border-bottom: 1px solid var(--border);
  font-weight: 600;
  color: var(--ink);
  font-size: 0.88rem;
}

.header-cell {
  padding: 14px 16px;
  text-align: left;
}

.table-body {
  background: var(--surface);
}

.table-row {
  display: grid;
  grid-template-columns: 1fr 1fr 2fr 1fr 1fr 0.7fr 1fr;
  border-bottom: 1px solid var(--border);
  transition: background-color var(--transition);
}

.table-row:hover {
  background-color: var(--canvas-deep);
}

.table-row:last-child {
  border-bottom: none;
}

.table-cell {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  font-size: 0.88rem;
  color: var(--ink-soft);
}

.model-key {
  background: var(--tag-bg);
  color: var(--accent);
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 0.82rem;
}

.url-cell {
  font-size: 0.78rem;
  color: var(--ink-muted);
  word-break: break-all;
}

.key-mask {
  font-family: 'Fira Code', monospace;
  color: var(--ink-muted);
  font-size: 0.82rem;
}

.status-badge {
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 500;
}

.status-badge.published {
  background: var(--status-published-bg);
  color: var(--status-published-text);
}

.status-badge.draft {
  background: var(--status-draft-bg);
  color: var(--status-draft-text);
}

.action-buttons {
  display: flex;
  gap: var(--space-2);
}

.action-btn {
  padding: 6px 14px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition);
}

.edit-btn {
  background: var(--accent-soft);
  color: var(--accent);
}

.edit-btn:hover {
  background: var(--accent);
  color: #FDFBF5;
}

.delete-btn {
  background: var(--status-error-bg);
  color: var(--status-error-text);
}

.delete-btn:hover {
  background: var(--status-error-text);
  color: #FDFBF5;
}

.form-group {
  margin-bottom: var(--space-4);
}

.form-label {
  display: block;
  margin-bottom: var(--space-2);
  font-weight: 500;
  color: var(--ink);
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .table-header,
  .table-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .header-cell,
  .table-cell {
    padding: 8px 12px;
  }
}
</style>
