package com.starlore.app.feature.article

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starlore.app.data.api.ArticleApi
import com.starlore.app.data.api.ArticleSummary
import com.starlore.app.data.api.CategoryItem
import kotlinx.coroutines.launch

sealed interface ArticlesUiState {
    object Loading : ArticlesUiState
    data class Success(val articles: List<ArticleSummary>) : ArticlesUiState
    data class Error(val message: String) : ArticlesUiState
}

sealed interface CategoriesUiState {
    object Loading : CategoriesUiState
    data class Success(val categories: List<CategoryItem>) : CategoriesUiState
    data class Error(val message: String) : CategoriesUiState
}

class ArticlesViewModel(private val articleApi: ArticleApi) : ViewModel() {

    var articlesState = mutableStateOf<ArticlesUiState>(ArticlesUiState.Loading)
        private set

    var categoriesState = mutableStateOf<CategoriesUiState>(CategoriesUiState.Loading)
        private set

    var selectedCategory = mutableStateOf<String?>(null)
    var searchQuery = mutableStateOf("")

    var isRefreshing = mutableStateOf(false)
        private set

    init {
        loadData()
    }

    fun loadData() {
        fetchCategories()
        fetchArticles()
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch {
            try {
                val categories = try {
                    articleApi.getCategories()
                } catch (_: Exception) {
                    articleApi.getPublicCategories()
                }
                categoriesState.value = CategoriesUiState.Success(categories.data)

                val articles = try {
                    articleApi.getAllArticles(
                        category = selectedCategory.value,
                        search = searchQuery.value.takeIf { it.isNotEmpty() }
                    )
                } catch (_: Exception) {
                    articleApi.getPublicArticles(
                        category = selectedCategory.value,
                        search = searchQuery.value.takeIf { it.isNotEmpty() }
                    )
                }
                articlesState.value = ArticlesUiState.Success(articles.data)
            } catch (e: Exception) {
                articlesState.value = ArticlesUiState.Error(e.message ?: "刷新失败")
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun selectCategory(categoryName: String?) {
        selectedCategory.value = categoryName
        fetchArticles()
    }

    fun search(query: String) {
        searchQuery.value = query
        fetchArticles()
    }

    fun fetchArticles() {
        articlesState.value = ArticlesUiState.Loading
        viewModelScope.launch {
            try {
                // Try fetching auth articles, fallback to public articles if needed
                val response = try {
                    articleApi.getAllArticles(
                        category = selectedCategory.value,
                        search = searchQuery.value.takeIf { it.isNotEmpty() }
                    )
                } catch (e: Exception) {
                    articleApi.getPublicArticles(
                        category = selectedCategory.value,
                        search = searchQuery.value.takeIf { it.isNotEmpty() }
                    )
                }
                articlesState.value = ArticlesUiState.Success(response.data)
            } catch (e: Exception) {
                articlesState.value = ArticlesUiState.Error(e.message ?: "Failed to load articles")
            }
        }
    }

    fun fetchCategories() {
        categoriesState.value = CategoriesUiState.Loading
        viewModelScope.launch {
            try {
                val response = try {
                    articleApi.getCategories()
                } catch (e: Exception) {
                    articleApi.getPublicCategories()
                }
                categoriesState.value = CategoriesUiState.Success(response.data)
            } catch (e: Exception) {
                categoriesState.value = CategoriesUiState.Error(e.message ?: "Failed to load categories")
            }
        }
    }
}
