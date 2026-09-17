<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  createCategoryService,
  deleteCategoryService,
  articleCategoryListService,
  updateCategoryService,
} from '@/api/article'
import type { CategoryItem } from '@/types'

const loading = ref(false)
const categories = ref<CategoryItem[]>([])
const keyword = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  description: '',
})

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return categories.value
  return categories.value.filter(
    (c) =>
      c.name.toLowerCase().includes(kw) ||
      (c.description ?? '').toLowerCase().includes(kw),
  )
})

function openCreate(): void {
  editingId.value = null
  form.name = ''
  form.description = ''
  dialogVisible.value = true
}

function openEdit(item: CategoryItem): void {
  editingId.value = item.id
  form.name = item.name
  form.description = item.description ?? ''
  dialogVisible.value = true
}

async function handleSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateCategoryService(editingId.value, { ...form })
      ElMessage.success('分类已更新')
    } else {
      await createCategoryService({ ...form })
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    await loadCategories()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(item: CategoryItem): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定删除分类「${item.name}」吗？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteCategoryService(item.id)
    ElMessage.success('已删除')
    await loadCategories()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

async function loadCategories(): Promise<void> {
  loading.value = true
  try {
    categories.value = await articleCategoryListService()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载分类失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadCategories)
</script>

<template>
  <div>
    <div class="page-head">
      <el-input
        v-model="keyword"
        placeholder="搜索分类名称 / 描述"
        clearable
        :prefix-icon="Search"
        style="width: 240px"
      />
      <el-button type="primary" :icon="Plus" @click="openCreate">新建星域</el-button>
    </div>

    <div class="table-wrap">
      <el-table v-loading="loading" :data="filtered" empty-text="暂无分类">
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column prop="article_count" label="文章数" width="90" align="right" />
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleDateString('zh-CN') : '-' }}
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
      :title="editingId ? '编辑星域' : '新建星域'"
      width="460px"
    >
      <el-form :model="form" label-width="72px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="30" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
