<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createAiConfigService,
  deleteAiConfigService,
  getAiConfigsService,
  updateAiConfigService,
} from '@/api/ai-config'
import type { AiConfigItem } from '@/types'

const loading = ref(false)
const configs = ref<AiConfigItem[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
/** 正在编辑的配置 id；null 表示新建 */
const editingId = ref<number | null>(null)

const form = reactive({
  modelKey: '',
  modelName: '',
  apiUrl: '',
  modelId: '',
  apiKey: '',
  enabled: true,
})

/** 编辑时 apiKey 留空 = 保留服务端已有 key（后端不回传明文） */
function openCreate(): void {
  editingId.value = null
  Object.assign(form, { modelKey: '', modelName: '', apiUrl: '', modelId: '', apiKey: '', enabled: true })
  dialogVisible.value = true
}

function openEdit(item: AiConfigItem): void {
  editingId.value = item.id
  Object.assign(form, {
    modelKey: item.modelKey,
    modelName: item.modelName,
    apiUrl: item.apiUrl,
    modelId: item.modelId,
    apiKey: '',
    enabled: item.enabled,
  })
  dialogVisible.value = true
}

async function handleSave(): Promise<void> {
  if (!form.modelKey || !form.modelName || !form.apiUrl || !form.modelId) {
    ElMessage.warning('modelKey / modelName / apiUrl / modelId 均为必填')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      const payload = { ...form }
      if (!payload.apiKey) delete (payload as Partial<typeof form>).apiKey
      await updateAiConfigService(editingId.value, payload)
      ElMessage.success('配置已更新')
    } else {
      await createAiConfigService({ ...form })
      ElMessage.success('配置已创建')
    }
    dialogVisible.value = false
    await loadConfigs()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleToggle(item: AiConfigItem): Promise<void> {
  try {
    await updateAiConfigService(item.id, { enabled: item.enabled })
    ElMessage.success(item.enabled ? '已启用' : '已停用')
  } catch (err) {
    item.enabled = !item.enabled
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  }
}

async function handleDelete(item: AiConfigItem): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定删除模型配置「${item.modelName}」吗？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteAiConfigService(item.id)
    ElMessage.success('已删除')
    await loadConfigs()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function loadConfigs(): Promise<void> {
  loading.value = true
  try {
    configs.value = await getAiConfigsService()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载配置失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadConfigs)
</script>

<template>
  <div>
    <div class="page-head">
      <span class="total-hint">已接入 {{ configs.length }} 家模型服务</span>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增配置</el-button>
    </div>

    <div class="table-wrap">
      <el-table v-loading="loading" :data="configs" empty-text="暂无配置">
        <el-table-column prop="modelName" label="服务名称" min-width="120" />
        <el-table-column prop="modelKey" label="Key" min-width="110" />
        <el-table-column prop="apiUrl" label="API 地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="modelId" label="默认模型" min-width="130" />
        <el-table-column label="API Key" width="110">
          <template #default>******</template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="handleToggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑模型配置' : '新增模型配置'"
      width="520px"
    >
      <el-form :model="form" label-width="92px">
        <el-form-item label="服务名称" required>
          <el-input v-model="form.modelName" placeholder="如：newapi / DeepSeek 官方" />
        </el-form-item>
        <el-form-item label="modelKey" required>
          <el-input v-model="form.modelKey" placeholder="唯一标识，如 modelapi" />
        </el-form-item>
        <el-form-item label="API 地址" required>
          <el-input v-model="form.apiUrl" placeholder="如：https://api.deepseek.com/v1" />
        </el-form-item>
        <el-form-item label="默认模型" required>
          <el-input v-model="form.modelId" placeholder="该服务的默认模型 ID" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="editingId ? '留空则保留原 Key' : 'sk-...'"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.total-hint {
  font-size: 12.5px;
  color: var(--text-muted);
}
</style>
