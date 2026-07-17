package com.starlore.app.feature.article

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starlore.app.data.api.ArticleApi
import com.starlore.app.data.api.ArticleDetail
import com.starlore.app.data.api.CategoryItem
import com.starlore.app.data.api.CategoryRequest
import com.starlore.app.data.api.CreateArticleRequest
import kotlinx.coroutines.launch

sealed interface EditorUiState {
    object Idle : EditorUiState
    object Loading : EditorUiState
    object Saving : EditorUiState
    data class Ready(val article: ArticleDetail? = null) : EditorUiState
    data class Saved(val article: ArticleDetail) : EditorUiState
    data class Error(val message: String) : EditorUiState
}

class ArticleManageViewModel(private val articleApi: ArticleApi) : ViewModel() {
    var editorState = mutableStateOf<EditorUiState>(EditorUiState.Idle)
        private set
    var categories = mutableStateOf<List<CategoryItem>>(emptyList())
        private set
    var categoriesLoading = mutableStateOf(false)
        private set
    var categoryError = mutableStateOf<String?>(null)
        private set

    fun loadEditor(articleId: Int?) {
        loadCategories()
        if (articleId == null) {
            editorState.value = EditorUiState.Ready()
            return
        }
        editorState.value = EditorUiState.Loading
        viewModelScope.launch {
            try {
                val article = articleApi.getArticleById(articleId).data
                    ?: error("文章不存在")
                editorState.value = EditorUiState.Ready(article)
            } catch (e: Exception) {
                editorState.value = EditorUiState.Error(e.message ?: "文章加载失败")
            }
        }
    }

    fun saveArticle(articleId: Int?, request: CreateArticleRequest) {
        editorState.value = EditorUiState.Saving
        viewModelScope.launch {
            try {
                val response = if (articleId == null) {
                    articleApi.createArticle(request)
                } else {
                    articleApi.updateArticle(articleId, request)
                }
                val article = response.data ?: error(response.message ?: "保存失败")
                editorState.value = EditorUiState.Saved(article)
            } catch (e: Exception) {
                editorState.value = EditorUiState.Error(e.message ?: "保存失败")
            }
        }
    }

    fun loadCategories() {
        categoriesLoading.value = true
        categoryError.value = null
        viewModelScope.launch {
            try {
                categories.value = articleApi.getCategories().data
            } catch (e: Exception) {
                categoryError.value = e.message ?: "分类加载失败"
            } finally {
                categoriesLoading.value = false
            }
        }
    }

    fun saveCategory(id: Int?, name: String, description: String, onDone: () -> Unit) {
        categoryError.value = null
        viewModelScope.launch {
            try {
                val request = CategoryRequest(name.trim(), description.trim().ifBlank { null })
                if (id == null) articleApi.createCategory(request) else articleApi.updateCategory(id, request)
                loadCategories()
                onDone()
            } catch (e: Exception) {
                categoryError.value = e.message ?: "分类保存失败"
            }
        }
    }

    fun deleteCategory(id: Int, onDone: () -> Unit) {
        categoryError.value = null
        viewModelScope.launch {
            try {
                articleApi.deleteCategory(id)
                loadCategories()
                onDone()
            } catch (e: Exception) {
                categoryError.value = e.message ?: "分类删除失败"
            }
        }
    }
}
