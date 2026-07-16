package com.starlore.app.data.api

import com.starlore.app.feature.settings.util.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

data class SseEvent(val event: String, val data: String)

object ApiClient {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val token = SettingsManager.authToken
                if (token.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    fun buildRetrofit(): Retrofit {
        val baseUrl = SettingsManager.backendBaseUrl.let { url ->
            if (url.endsWith("/")) url else "$url/"
        }
        val contentType = "application/json; charset=utf-8".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun parseSseFlow(responseBody: ResponseBody): Flow<SseEvent> = flow {
        val source = responseBody.source()
        try {
            var currentEvent = ""
            while (true) {
                val line = source.readUtf8Line() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("event:")) {
                    currentEvent = trimmed.substring(6).trim()
                } else if (trimmed.startsWith("data:")) {
                    val data = trimmed.substring(5).trim()
                    emit(SseEvent(event = currentEvent, data = data))
                    currentEvent = "" // reset
                }
            }
        } finally {
            source.close()
        }
    }.flowOn(Dispatchers.IO)
}
