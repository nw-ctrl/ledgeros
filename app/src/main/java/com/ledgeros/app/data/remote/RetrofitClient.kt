package com.ledgeros.app.data.remote

import com.ledgeros.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client for the LedgerOS FastAPI backend.
 *
 * Base URL is injected at compile-time from local.properties via BuildConfig,
 * so no credentials or server addresses are ever stored in source control.
 *
 * Timeouts are kept short (10 s connect, 20 s read) because the backend is
 * used for non-blocking background enrichment only — the UI never waits on it.
 */
object RetrofitClient {

    val api: LedgerApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY   // full request/response in Logcat
            else
                HttpLoggingInterceptor.Level.NONE
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LedgerApiService::class.java)
    }
}
