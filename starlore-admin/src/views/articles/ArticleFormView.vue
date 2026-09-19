<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getArticleByIdService,
  createArticleService,
  updateArticleService,
  articleCategoryListService,
} from '@/api/article'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import type { ArticleStatus } from '@/types'

const route = useRoute()
const router = useRouter()

const articleId = computed(() => {
  const raw = route.params.id
  return raw ? Number(Array.isArray(raw) ? raw[0] : raw) : null
})

const formRef = ref<FormInstance>()
const saving = ref(false)
const categories = ref<string[]>([])

const form = reactive({
  title: '',
  category: '',
  status: 'draft' as ArticleStatus,
  description: '',
  content: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入摘要', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }],
}

async function loadArticle(): Promise<void> {
  if (!articleId.value) return
  try {
    const detail = await getArticleByIdService(articleId.value)
    form.title = detail.title
    form.category = detail.category
    form.status = detail.status
    form.description = detail.description
    form.content = detail.content || ''
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '文章加载失败')
  }
}

async function handleSave(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (articleId.value) {
      await updateArticleService(articleId.value, { ...form })
      ElMessage.success('保存成功')
    } else {
      await createArticleService({ ...form })
      ElMessage.success('创建成功')
    }
    void router.push('/admin/articles')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const list = await articleCategoryListService().catch(() => [])
  categories.value = list.map((c) => c.name)
  await loadArticle()
})
</script>

<template>
  <div class="article-form">
    <div class="bento-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题" maxlength="120" show-word-limit />
        </el-form-item>

        <div class="inline-fields">
          <el-form-item label="分类" prop="category">
            <el-select v-model="form.category" placeholder="选择星域分类" style="width: 200px">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio-button value="draft">草稿</el-radio-button>
              <el-radio-button value="published">发布</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>

        <el-form-item label="摘要" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="简要概括文章内容，将展示在列表与详情页"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="正文" prop="content">
          <MarkdownEditor v-model="form.content" />
        </el-form-item>
      </el-form>

      <div class="form-actions">
        <el-button @click="router.push('/admin/articles')">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ articleId ? '保存修改' : '创建文章' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.inline-fields {
  display: flex;
  gap: 28px;
  flex-wrap: wrap;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid var(--border-soft);
}
</style>
