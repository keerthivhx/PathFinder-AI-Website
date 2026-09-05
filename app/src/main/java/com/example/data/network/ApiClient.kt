package com.example.data.network

import com.example.BuildConfig
import com.example.data.network.api.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Default production HTTPS backend URL (configured via BuildConfig / production backend)
    private var baseUrl: String = BuildConfig.PRODUCTION_BACKEND_URL

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private var retrofit: Retrofit = buildRetrofit(baseUrl)

    private fun buildRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(if (url.endsWith("/")) url else "$url/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun setBaseUrl(newUrl: String) {
        baseUrl = newUrl
        retrofit = buildRetrofit(newUrl)
        buildingApi = retrofit.create(BuildingApi::class.java)
        adminApi = retrofit.create(AdminApi::class.java)
        locationApi = retrofit.create(LocationApi::class.java)
        navigationApi = retrofit.create(NavigationApi::class.java)
        authApi = retrofit.create(AuthApi::class.java)
    }

    fun getBaseUrl(): String = baseUrl

    var buildingApi: BuildingApi = retrofit.create(BuildingApi::class.java)
        private set

    var adminApi: AdminApi = retrofit.create(AdminApi::class.java)
        private set

    var locationApi: LocationApi = retrofit.create(LocationApi::class.java)
        private set

    var navigationApi: NavigationApi = retrofit.create(NavigationApi::class.java)
        private set

    var authApi: AuthApi = retrofit.create(AuthApi::class.java)
        private set
}
