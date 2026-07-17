package com.starlore.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

@Serializable
data class PaginationInfo(
    val current: Int = 1,
    val page: Int = 1,
    val limit: Int = 10,
    val total: Int = 0,
    val pages: Int = 0
)

@Serializable
data class ArticleSummary(
    val id: Int,
    val userId: Int,
    val authorName: String? = null,
    val title: String,
    val status: String,
    @SerialName("is_public") val isPublic: Boolean? = true,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("cover_image") val coverImage: String? = null,
    @SerialName("view_count") val viewCount: Int? = 0,
    val createdAt: String? = null
)

@Serializable
data class ArticleListResponse(
    val data: List<ArticleSummary>,
    val pagination: PaginationInfo? = null
)

@Serializable
data class ArticleDetail(
    val id: Int,
    val title: String,
    val content: String,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("cover_image") val coverImage: String? = null,
    @SerialName("view_count") val viewCount: Int? = 0,
    val status: String,
    @SerialName("is_public") val isPublic: Boolean? = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CategoryItem(
    val id: Int,
    val userId: Int? = null,
    val name: String,
    val description: String? = null,
    val color: String? = null,
    @SerialName("article_count") val articleCount: Int? = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CategoryListResponse(
    val data: List<CategoryItem>
)

@Serializable
data class CreateArticleRequest(
    val title: String,
    val content: String,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("cover_image") val coverImage: String? = null,
    val status: String? = "published",
    @SerialName("is_public") val isPublic: Boolean? = true
)

@Serializable
data class CategoryRequest(
    val name: String,
    val description: String? = null,
    val color: String? = null
)

interface ArticleApi {
    @GET("articles")
    suspend fun getAllArticles(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("tag") tag: String? = null
    ): ArticleListResponse

    @GET("public/articles")
    suspend fun getPublicArticles(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("tag") tag: String? = null
    ): ArticleListResponse

    @GET("articles/{id}")
    suspend fun getArticleById(@Path("id") id: Int): ApiResponse<ArticleDetail>

    @GET("public/articles/{id}")
    suspend fun getPublicArticleById(@Path("id") id: Int): ApiResponse<ArticleDetail>

    @POST("articles")
    suspend fun createArticle(@Body request: CreateArticleRequest): ApiResponse<ArticleDetail>

    @PUT("articles/{id}")
    suspend fun updateArticle(@Path("id") id: Int, @Body request: CreateArticleRequest): ApiResponse<ArticleDetail>

    @DELETE("articles/{id}")
    suspend fun deleteArticle(@Path("id") id: Int): ApiResponse<String>

    @GET("categories")
    suspend fun getCategories(): CategoryListResponse

    @GET("public/categories")
    suspend fun getPublicCategories(): CategoryListResponse

    @POST("categories")
    suspend fun createCategory(@Body request: CategoryRequest): ApiResponse<CategoryItem>

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body request: CategoryRequest): ApiResponse<CategoryItem>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): ApiResponse<String>
}
