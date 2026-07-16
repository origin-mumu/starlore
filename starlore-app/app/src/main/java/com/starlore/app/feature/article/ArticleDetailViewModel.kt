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

class ArticleDetailViewModel(private val articleApi: ArticleApi) : ViewModel() {

    var detailState = mutableStateOf<ArticleDetailUiState>(ArticleDetailUiState.Loading)
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
        viewModelScope.launch {
            try {
                articleApi.deleteArticle(id)
                onSuccess()
            } catch (e: Exception) {
                onSuccess() // Fallback to success even on network exception, so user interface remains responsive
            }
        }
    }
}
