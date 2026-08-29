<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import {
  ArrowLeft,
  BookOpen,
  Check,
  ChevronRight,
  CircleCheck,
  Clock3,
  FileText,
  Layers3,
  LoaderCircle,
  Plus,
  Search,
  Sparkles,
  X,
} from '@lucide/vue'

import {
  generateKnowledgeCards,
  getKnowledgeArticles,
  getKnowledgeCards,
  getKnowledgeGroups,
  reviewKnowledgeCard,
  type KnowledgeArticleItem,
  type KnowledgeCardItem,
  type KnowledgeGroupItem,
} from '@/api/knowledgeMemory'

type Screen = 'groups' | 'cards' | 'review'
type GenerationProgress = {
  completed: number
  total: number
  currentTitle: string
  generatedArticles: number
  generatedCards: number
  failedArticles: number
}

const screen = ref<Screen>('groups')
const loading = ref(true)
const cardsLoading = ref(false)
const generating = ref(false)
const pickerOpen = ref(false)
const articleSearch = ref('')
const articleCategory = ref('全部')
const groupSearch = ref('')
const groupFilter = ref<'all' | 'due' | 'mastered'>('all')
const groupCategory = ref('全部')
const selectedArticleIds = ref<Set<number>>(new Set())
const draftArticleIds = ref<Set<number>>(new Set())
const generationProgress = ref<GenerationProgress | null>(null)
const articles = ref<KnowledgeArticleItem[]>([])
const groups = ref<KnowledgeGroupItem[]>([])
const activeGroup = ref<KnowledgeGroupItem | null>(null)
const cards = ref<KnowledgeCardItem[]>([])
const expandedCardId = ref<number | null>(null)
const notice = ref<{ tone: 'success' | 'error'; message: string } | null>(null)

const reviewCards = ref<KnowledgeCardItem[]>([])
const reviewIndex = ref(0)
const answerVisible = ref(false)
const reviewStartedAt = ref(0)
const reviewing = ref(false)

const filteredArticles = computed(() => {
  const keyword = articleSearch.value.trim().toLowerCase()
  return articles.value.filter(
    article =>
      (articleCategory.value === '全部' || (article.category || '未分类') === articleCategory.value) &&
      (!keyword ||
        article.title.toLowerCase().includes(keyword) ||
        (article.description || '').toLowerCase().includes(keyword) ||
        (article.category || '').toLowerCase().includes(keyword)),
  )
})

const articleCategories = computed(() => [
  '全部',
  ...Array.from(new Set(articles.value.map(article => article.category || '未分类'))),
])
const groupCategories = computed(() => [
  '全部',
  ...Array.from(new Set(groups.value.map(group => group.category || '未分类'))),
])
const selectedArticles = computed(() =>
  articles.value.filter(article => selectedArticleIds.value.has(article.articleId)),
)
const generationPercentage = computed(() => {
  const progress = generationProgress.value
  if (!progress?.total) return 0
  return Math.round((progress.completed / progress.total) * 100)
})

const totalCards = computed(() => groups.value.reduce((sum, group) => sum + group.cardCount, 0))
const totalDue = computed(() => groups.value.reduce((sum, group) => sum + group.dueCount, 0))
const totalMastered = computed(() => groups.value.reduce((sum, group) => sum + group.masteredCount, 0))
const visibleGroups = computed(() => {
  const keyword = groupSearch.value.trim().toLowerCase()
  return groups.value.filter(group => {
    const matchesKeyword =
      !keyword ||
      group.title.toLowerCase().includes(keyword) ||
      (group.description || '').toLowerCase().includes(keyword) ||
      (group.category || '').toLowerCase().includes(keyword)
    const matchesFilter =
      groupFilter.value === 'all' ||
      (groupFilter.value === 'due' && group.dueCount > 0) ||
      (groupFilter.value === 'mastered' && group.masteredCount >= group.cardCount)
    const matchesCategory =
      groupCategory.value === '全部' || (group.category || '未分类') === groupCategory.value
    return matchesKeyword && matchesFilter && matchesCategory
  })
})
const currentReviewCard = computed(() => reviewCards.value[reviewIndex.value] || null)
const reviewComplete = computed(
  () => reviewCards.value.length > 0 && reviewIndex.value >= reviewCards.value.length,
)

function renderMarkdown(source: string) {
  return DOMPurify.sanitize(marked.parse(source) as string)
}

function setNotice(tone: 'success' | 'error', message: string) {
  notice.value = { tone, message }
  window.setTimeout(() => {
    if (notice.value?.message === message) notice.value = null
  }, 5000)
}

async function loadPage() {
  loading.value = true
  try {
    const [articleResponse, groupResponse] = await Promise.all([
      getKnowledgeArticles(),
      getKnowledgeGroups(),
    ])
    articles.value = articleResponse.data || []
    groups.value = groupResponse.data || []
  } catch (error) {
    setNotice('error', error instanceof Error ? error.message : '知识记忆加载失败')
  } finally {
    loading.value = false
  }
}

function toggleArticle(articleId: number) {
  const next = new Set(draftArticleIds.value)
  if (next.has(articleId)) next.delete(articleId)
  else next.add(articleId)
  draftArticleIds.value = next
}

function selectVisibleArticles() {
  const next = new Set(draftArticleIds.value)
  const allSelected = filteredArticles.value.every(article => next.has(article.articleId))
  for (const article of filteredArticles.value) {
    if (allSelected) next.delete(article.articleId)
    else next.add(article.articleId)
  }
  draftArticleIds.value = next
}

function openArticlePicker() {
  draftArticleIds.value = new Set(selectedArticleIds.value)
  pickerOpen.value = true
}

function confirmArticleSelection() {
  selectedArticleIds.value = new Set(draftArticleIds.value)
  pickerOpen.value = false
}

async function generateSelectedArticles() {
  if (!selectedArticleIds.value.size || generating.value) return
  const articleIds = Array.from(selectedArticleIds.value)
  const failedIds = new Set<number>()
  generating.value = true
  generationProgress.value = {
    completed: 0,
    total: articleIds.length,
    currentTitle: '',
    generatedArticles: 0,
    generatedCards: 0,
    failedArticles: 0,
  }
  try {
    for (const articleId of articleIds) {
      const article = articles.value.find(item => item.articleId === articleId)
      if (generationProgress.value) {
        generationProgress.value.currentTitle = article?.title || `文章 ${articleId}`
      }
      try {
        const response = await generateKnowledgeCards({
          articleIds: [articleId],
          maxCardsPerArticle: 20,
        })
        const generated = response.data?.generated || []
        const errors = response.data?.errors || []
        if (generated.length && generationProgress.value) {
          generationProgress.value.generatedArticles += 1
          generationProgress.value.generatedCards += generated.reduce((sum, item) => sum + item.cardCount, 0)
        }
        if (errors.length || !generated.length) {
          failedIds.add(articleId)
          if (generationProgress.value) generationProgress.value.failedArticles += 1
        }
      } catch {
        failedIds.add(articleId)
        if (generationProgress.value) generationProgress.value.failedArticles += 1
      } finally {
        if (generationProgress.value) generationProgress.value.completed += 1
      }
    }
    const completed = generationProgress.value
    selectedArticleIds.value = failedIds
    await loadPage()
    if (failedIds.size) {
      setNotice('error', `已生成 ${completed?.generatedArticles || 0} 篇，${failedIds.size} 篇失败，可保留失败文章后重试`)
    } else {
      setNotice('success', `${completed?.generatedArticles || 0} 篇文章已生成 ${completed?.generatedCards || 0} 张知识卡片`)
    }
  } finally {
    generating.value = false
    window.setTimeout(() => {
      generationProgress.value = null
    }, 1200)
  }
}

async function openGroup(group: KnowledgeGroupItem) {
  activeGroup.value = group
  screen.value = 'cards'
  cardsLoading.value = true
  expandedCardId.value = null
  try {
    const response = await getKnowledgeCards(group.articleId)
    cards.value = response.data || []
  } catch (error) {
    setNotice('error', error instanceof Error ? error.message : '题卡加载失败')
  } finally {
    cardsLoading.value = false
  }
}

async function startReview(group: KnowledgeGroupItem, dueOnly = true) {
  cardsLoading.value = true
  try {
    let response = await getKnowledgeCards(group.articleId, dueOnly)
    let items = response.data || []
    if (!items.length && dueOnly) {
      response = await getKnowledgeCards(group.articleId, false)
      items = response.data || []
    }
    if (!items.length) {
      setNotice('error', '这篇文章暂时没有可复习的知识卡片')
      return
    }
    activeGroup.value = group
    reviewCards.value = items
    reviewIndex.value = 0
    answerVisible.value = false
    reviewStartedAt.value = Date.now()
    screen.value = 'review'
  } catch (error) {
    setNotice('error', error instanceof Error ? error.message : '复习题卡加载失败')
  } finally {
    cardsLoading.value = false
  }
}

function revealAnswer() {
  answerVisible.value = true
}

async function rateCard(rating: 'again' | 'hard' | 'good') {
  const card = currentReviewCard.value
  if (!card || reviewing.value) return
  reviewing.value = true
  try {
    await reviewKnowledgeCard(card.id, {
      rating,
      durationMs: Math.max(0, Date.now() - reviewStartedAt.value),
    })
    reviewIndex.value += 1
    answerVisible.value = false
    reviewStartedAt.value = Date.now()
    if (reviewIndex.value >= reviewCards.value.length) await loadPage()
  } catch (error) {
    setNotice('error', error instanceof Error ? error.message : '复习进度保存失败')
  } finally {
    reviewing.value = false
  }
}

function backToGroups() {
  screen.value = 'groups'
  activeGroup.value = null
  cards.value = []
}

function startNextReview() {
  const target = groups.value.find(group => group.dueCount > 0) || groups.value[0]
  if (target) startReview(target)
}

onMounted(loadPage)
</script>

<template>
  <main class="memory-page">
    <div class="memory-shell">
      <div v-if="notice" class="notice" :class="`notice--${notice.tone}`" role="status">
        {{ notice.message }}
      </div>

      <template v-if="screen === 'groups'">
        <section class="page-header">
          <div class="header-panel">
            <div class="header-main">
              <div class="header-titles">
                <span class="header-kicker">MEMORY PRACTICE</span>
                <h1>知识记忆</h1>
                <p>从已有星记生成问答题卡，按文章独立复习，答案始终保留原文。</p>
              </div>
              <button class="primary-action" type="button" :disabled="generating" @click="openArticlePicker">
                <Plus :size="17" aria-hidden="true" />
                生成题卡
              </button>
            </div>
            <div class="header-tabs" aria-label="知识记忆说明">
              <span class="header-tab header-tab--active"><Layers3 :size="14" /> 按文章复习</span>
              <span class="header-tab"><FileText :size="14" /> 原文作答</span>
              <span class="header-tab"><Sparkles :size="14" /> AI 整理</span>
            </div>
          </div>
        </section>

        <section class="memory-content">
          <div class="content-layout">
            <div class="memory-main">
              <section v-if="selectedArticles.length || generating" class="generation-panel ink-glass-card">
                <div class="generation-heading">
                  <div>
                    <span class="section-label">生成记忆卡片</span>
                    <h2>{{ generating ? '正在逐篇整理文章' : `已选择 ${selectedArticles.length} 篇文章` }}</h2>
                    <p v-if="generating && generationProgress">
                      当前：{{ generationProgress.currentTitle }}
                    </p>
                    <p v-else>确认文章后再调用 AI，每篇文章独立生成，不会混合内容。</p>
                  </div>
                  <button v-if="!generating" class="text-action" type="button" @click="openArticlePicker">
                    修改选择
                  </button>
                </div>

                <template v-if="generating && generationProgress">
                  <div class="generation-progress" role="progressbar" :aria-valuenow="generationPercentage" aria-valuemin="0" aria-valuemax="100">
                    <span :style="{ width: `${generationPercentage}%` }"></span>
                  </div>
                  <div class="generation-status">
                    <span>{{ generationProgress.completed }} / {{ generationProgress.total }} 篇完成</span>
                    <span>已生成 {{ generationProgress.generatedCards }} 张</span>
                    <span v-if="generationProgress.failedArticles" class="generation-failed">
                      {{ generationProgress.failedArticles }} 篇失败
                    </span>
                  </div>
                </template>
                <template v-else>
                  <div class="selected-article-list">
                    <span v-for="article in selectedArticles" :key="article.articleId">
                      <FileText :size="13" /> {{ article.title }}
                    </span>
                  </div>
                  <div class="generation-actions">
                    <span>预计最多生成 {{ selectedArticles.length * 20 }} 张题卡</span>
                    <button class="primary-action" type="button" @click="generateSelectedArticles">
                      <Sparkles :size="17" aria-hidden="true" /> 生成记忆卡片
                    </button>
                  </div>
                </template>
              </section>

              <section class="group-section">
                <label v-if="groups.length" class="group-search">
                  <Search :size="16" aria-hidden="true" />
                  <input v-model="groupSearch" type="search" placeholder="搜索文章题库" />
                  <span>{{ visibleGroups.length }} 篇</span>
                </label>

                <div v-if="groups.length" class="group-toolbar">
                  <div class="filter-pills" aria-label="筛选知识题库">
                    <button :class="{ active: groupFilter === 'all' }" type="button" @click="groupFilter = 'all'">全部</button>
                    <button :class="{ active: groupFilter === 'due' }" type="button" @click="groupFilter = 'due'">待复习</button>
                    <button :class="{ active: groupFilter === 'mastered' }" type="button" @click="groupFilter = 'mastered'">已掌握</button>
                  </div>
                  <label class="category-select">
                    <span>文章分类</span>
                    <select v-model="groupCategory" aria-label="按文章分类筛选题库">
                      <option v-for="category in groupCategories" :key="category" :value="category">{{ category }}</option>
                    </select>
                  </label>
                </div>

                <div v-if="loading" class="state-panel ink-glass-card">
                  <LoaderCircle class="spinning" :size="24" aria-hidden="true" />
                  <span>正在读取知识题库</span>
                </div>
                <div v-else-if="!groups.length" class="empty-card ink-glass-card">
                  <span class="empty-icon"><Layers3 :size="25" aria-hidden="true" /></span>
                  <div>
                    <span class="section-label">从第一篇开始</span>
                    <h2>把你的求职笔记变成可回答的问题</h2>
                    <p>选择已有文章，AI 只负责整理问题，答案从原文逐行保留。</p>
                  </div>
                  <button class="secondary-action" type="button" @click="openArticlePicker">
                    选择文章 <ChevronRight :size="17" aria-hidden="true" />
                  </button>
                </div>
                <div v-else-if="!visibleGroups.length" class="no-result ink-glass-card">
                  <Search :size="22" aria-hidden="true" />
                  <h3>没有符合条件的题库</h3>
                  <button type="button" @click="groupSearch = ''; groupFilter = 'all'; groupCategory = '全部'">清除筛选</button>
                </div>
                <div v-else class="group-grid">
                  <article v-for="group in visibleGroups" :key="group.articleId" class="group-card ink-glass-card">
                    <button class="group-card-main" type="button" @click="openGroup(group)">
                      <span class="group-card-top">
                        <span class="category-chip">{{ group.category || '未分类' }}</span>
                        <span class="due-chip" :class="{ 'due-chip--done': !group.dueCount }">
                          <Clock3 v-if="group.dueCount" :size="13" />
                          <CircleCheck v-else :size="13" />
                          {{ group.dueCount ? `${group.dueCount} 张待复习` : '今日已完成' }}
                        </span>
                      </span>
                      <span class="group-card-copy">
                        <strong>{{ group.title }}</strong>
                        <span>{{ group.description || '来自原文的知识问答题卡。' }}</span>
                      </span>
                      <span class="group-card-meta">
                        <span><Layers3 :size="14" /> {{ group.cardCount }} 张题卡</span>
                        <span>{{ group.masteredCount }} 张已掌握</span>
                      </span>
                    </button>
                    <div class="group-card-footer">
                      <button type="button" @click="openGroup(group)">查看题卡</button>
                      <button class="card-review-action" type="button" @click="startReview(group)">
                        开始背诵 <ChevronRight :size="16" />
                      </button>
                    </div>
                  </article>
                </div>
              </section>
            </div>

            <aside class="memory-sidebar">
              <section class="today-card ink-glass-card">
                <div class="sidebar-heading">
                  <span class="sidebar-icon"><BookOpen :size="18" aria-hidden="true" /></span>
                  <span>今日复习</span>
                </div>
                <strong>{{ totalDue }}</strong>
                <p v-if="totalDue">张题卡等待复习，从最需要巩固的内容开始。</p>
                <p v-else-if="groups.length">今天的任务已经完成，也可以继续自由复习。</p>
                <p v-else>生成题卡后，这里会整理今天的复习任务。</p>
                <button type="button" :disabled="!groups.length" @click="startNextReview">
                  {{ totalDue ? '开始今日复习' : groups.length ? '自由复习' : '暂无复习任务' }}
                </button>
              </section>

              <section class="overview-card ink-glass-card">
                <h2>记忆概览</h2>
                <dl>
                  <div><dt>文章题库</dt><dd>{{ groups.length }}</dd></div>
                  <div><dt>知识题卡</dt><dd>{{ totalCards }}</dd></div>
                  <div><dt>已掌握</dt><dd>{{ totalMastered }}</dd></div>
                </dl>
              </section>

              <section class="source-note">
                <Sparkles :size="16" aria-hidden="true" />
                <div>
                  <strong>忠于原文</strong>
                  <p>完整 Markdown 交给 AI 识别问题，答案直接从来源文章提取。</p>
                </div>
              </section>
            </aside>
          </div>
        </section>

        <Teleport to="body">
          <div
            v-if="pickerOpen"
            class="picker-backdrop"
            role="presentation"
            @click.self="pickerOpen = false"
          >
            <section class="picker-dialog" role="dialog" aria-modal="true" aria-labelledby="picker-title">
              <header class="picker-dialog-header">
                <div>
                  <span class="section-label">创建题库</span>
                  <h2 id="picker-title">选择知识来源</h2>
                  <p>可一次多选，系统会逐篇生成，不会混合不同文章的内容。</p>
                </div>
                <button class="dialog-close" type="button" aria-label="关闭" @click="pickerOpen = false">
                  <X :size="19" aria-hidden="true" />
                </button>
              </header>

              <div class="picker-dialog-tools">
                <label class="search-field">
                  <Search :size="17" aria-hidden="true" />
                  <input v-model="articleSearch" type="search" placeholder="搜索文章标题、描述或分类" />
                </label>
                <button class="text-action" type="button" @click="selectVisibleArticles">
                  <Check :size="16" aria-hidden="true" />
                  全选当前结果
                </button>
              </div>

              <div class="article-category-pills" aria-label="按文章分类筛选">
                <button
                  v-for="category in articleCategories"
                  :key="category"
                  :class="{ active: articleCategory === category }"
                  type="button"
                  @click="articleCategory = category"
                >
                  {{ category }}
                </button>
              </div>

              <div class="article-options">
                <button
                  v-for="article in filteredArticles"
                  :key="article.articleId"
                  class="article-option"
                  :class="{ 'article-option--selected': draftArticleIds.has(article.articleId) }"
                  type="button"
                  @click="toggleArticle(article.articleId)"
                >
                  <span class="selection-control" aria-hidden="true">
                    <Check v-if="draftArticleIds.has(article.articleId)" :size="15" />
                  </span>
                  <span class="article-option-copy">
                    <strong>{{ article.title }}</strong>
                    <span>{{ article.category || '未分类' }} · {{ article.generated ? `${article.cardCount} 张，重新生成将覆盖` : '尚未生成' }}</span>
                  </span>
                </button>
                <div v-if="!filteredArticles.length" class="picker-empty">当前分类下没有符合条件的文章</div>
              </div>

              <footer class="picker-footer">
                <span>已选择 <strong>{{ draftArticleIds.size }}</strong> 篇</span>
                <div class="picker-footer-actions">
                  <button class="cancel-action" type="button" @click="pickerOpen = false">取消</button>
                  <button
                    class="primary-action"
                    type="button"
                    :disabled="!draftArticleIds.size"
                    @click="confirmArticleSelection"
                  >
                    <Check :size="18" aria-hidden="true" />
                    确认选择
                  </button>
                </div>
              </footer>
            </section>
          </div>
        </Teleport>
      </template>

      <template v-else-if="screen === 'cards'">
        <header class="detail-heading ink-glass-card">
          <button class="back-action" type="button" @click="backToGroups">
            <ArrowLeft :size="18" aria-hidden="true" /> 返回知识记忆
          </button>
          <div class="detail-title">
            <p class="eyebrow">来源文章</p>
            <h1>{{ activeGroup?.title }}</h1>
            <p>{{ cards.length }} 张题卡，答案保留原文 Markdown。</p>
          </div>
          <button v-if="activeGroup" class="primary-action" type="button" @click="startReview(activeGroup)">
            <BookOpen :size="18" aria-hidden="true" /> 开始背诵
          </button>
        </header>

        <div v-if="cardsLoading" class="state-panel">
          <LoaderCircle class="spinning" :size="24" />
          <span>正在读取题卡</span>
        </div>
        <section v-else class="card-list">
          <article v-for="(card, index) in cards" :key="card.id" class="question-row ink-glass-card">
            <button
              class="question-toggle"
              type="button"
              @click="expandedCardId = expandedCardId === card.id ? null : card.id"
            >
              <span class="question-number">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="question-copy">
                <strong>{{ card.question }}</strong>
                <span>原文第 {{ card.sourceLineStart }} 至 {{ card.sourceLineEnd }} 行</span>
              </span>
              <ChevronRight class="question-chevron" :class="{ 'question-chevron--open': expandedCardId === card.id }" :size="19" />
            </button>
            <div v-if="expandedCardId === card.id" class="answer-markdown" v-html="renderMarkdown(card.answerMarkdown)"></div>
          </article>
        </section>
      </template>

      <template v-else>
        <header class="review-heading ink-glass-card">
          <button class="back-action" type="button" @click="activeGroup ? openGroup(activeGroup) : backToGroups()">
            <ArrowLeft :size="18" aria-hidden="true" /> 退出背诵
          </button>
          <div v-if="!reviewComplete" class="review-progress-summary">
            <span>背诵进度</span>
            <strong>{{ Math.min(reviewIndex + 1, reviewCards.length) }} / {{ reviewCards.length }}</strong>
            <span class="review-progress-track" aria-hidden="true">
              <span :style="{ width: `${(Math.min(reviewIndex + 1, reviewCards.length) / reviewCards.length) * 100}%` }"></span>
            </span>
          </div>
        </header>

        <section v-if="reviewComplete" class="review-complete ink-glass-card">
          <CircleCheck :size="36" aria-hidden="true" />
          <h1>本轮背诵完成</h1>
          <p>复习结果已经保存，下次会优先出现没有记牢的题卡。</p>
          <button class="primary-action" type="button" @click="backToGroups">返回知识记忆</button>
        </section>

        <div v-else-if="currentReviewCard" class="review-stage">
          <section class="review-question-card ink-glass-card">
            <div class="review-source">
              <FileText :size="16" aria-hidden="true" />
              {{ currentReviewCard.articleTitle }}
            </div>
            <h1>{{ currentReviewCard.question }}</h1>
            <button v-if="!answerVisible" class="reveal-action" type="button" @click="revealAnswer">
              显示原文答案
            </button>
          </section>

          <section v-if="answerVisible" class="review-answer-card ink-glass-card">
            <div class="answer-card-heading">
              <div>
                <span class="section-label">原文答案</span>
                <h2>对照你的回答</h2>
              </div>
              <span>第 {{ currentReviewCard.sourceLineStart }} 至 {{ currentReviewCard.sourceLineEnd }} 行</span>
            </div>
            <div class="review-answer answer-markdown" v-html="renderMarkdown(currentReviewCard.answerMarkdown)"></div>
            <div class="rating-row" aria-label="选择记忆程度">
              <button type="button" :disabled="reviewing" @click="rateCard('again')">没记住</button>
              <button type="button" :disabled="reviewing" @click="rateCard('hard')">有点模糊</button>
              <button class="rating-good" type="button" :disabled="reviewing" @click="rateCard('good')">
                <LoaderCircle v-if="reviewing" class="spinning" :size="16" />
                记住了
              </button>
            </div>
          </section>
        </div>
      </template>
    </div>
  </main>
</template>

<style scoped src="./KnowledgeMemoryView.css"></style>

