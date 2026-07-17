package com.starlore.app.data.di

import com.starlore.app.data.api.ApiClient
import com.starlore.app.data.api.AuthApi
import com.starlore.app.data.api.ArticleApi
import com.starlore.app.data.api.AiApi
import com.starlore.app.feature.article.ArticleDetailViewModel
import com.starlore.app.feature.article.ArticlesViewModel
import com.starlore.app.feature.article.ArticleManageViewModel
import com.starlore.app.feature.auth.AuthViewModel
import com.starlore.app.feature.chat.ChatViewModel
import com.starlore.app.feature.diverge.DivergeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val starloreAppModule = module {
    // API instances created dynamically to reflect any base URL changes in settings
    single { ApiClient.buildRetrofit() }
    single { get<retrofit2.Retrofit>().create(AuthApi::class.java) }
    single { get<retrofit2.Retrofit>().create(ArticleApi::class.java) }
    single { get<retrofit2.Retrofit>().create(AiApi::class.java) }

    viewModelOf(::AuthViewModel)
    viewModelOf(::ArticlesViewModel)
    viewModelOf(::ArticleDetailViewModel)
    viewModelOf(::ArticleManageViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::DivergeViewModel)
}
