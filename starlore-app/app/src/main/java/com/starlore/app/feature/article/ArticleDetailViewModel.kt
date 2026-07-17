package com.starlore.app.feature.article

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starlore.app.data.api.ArticleApi
import com.starlore.app.data.api.ArticleDetail
import kotlinx.coroutines.launch

sealed interface ArticleDetailUiState {
    object Loading : ArticleDetailUiState
    data class Success(val article: ArticleDetail) : ArticleDetailUiState
    data class Error(val message: String) : ArticleDetailUiState
}

sealed interface ArticleDeleteUiState {
    object Idle : ArticleDeleteUiState
    object Deleting : ArticleDeleteUiState
    data class Error(val message: String) : ArticleDeleteUiState
}

class ArticleDetailViewModel(private val articleApi: ArticleApi) : ViewModel() {

    var detailState = mutableStateOf<ArticleDetailUiState>(ArticleDetailUiState.Loading)
        private set

    var deleteState = mutableStateOf<ArticleDeleteUiState>(ArticleDeleteUiState.Idle)
        private set

    fun loadArticle(id: Int) {
        detailState.value = ArticleDetailUiState.Loading
        viewModelScope.launch {
            try {
                val response = try {
                    articleApi.getArticleById(id)
                } catch (e: Exception) {
                    articleApi.getPublicArticleById(id)
                }
                val data = response.data
                if (data != null) {
                    detailState.value = ArticleDetailUiState.Success(data)
                } else {
                    detailState.value = ArticleDetailUiState.Error(response.message ?: "Failed to find article details")
                }
            } catch (e: Exception) {
                detailState.value = ArticleDetailUiState.Error(e.message ?: "Network error occurred")
            }
        }
    }

    fun deleteArticle(id: Int, onSuccess: () -> Unit) {
        if (deleteState.value is ArticleDeleteUiState.Deleting) return

        deleteState.value = ArticleDeleteUiState.Deleting
        viewModelScope.launch {
            try {
                articleApi.deleteArticle(id)
                deleteState.value = ArticleDeleteUiState.Idle
                onSuccess()
            } catch (e: Exception) {
                deleteState.value = ArticleDeleteUiState.Error(
                    e.message?.takeIf { it.isNotBlank() } ?: "无法删除文章，请稍后重试"
                )
            }
        }
    }
}
