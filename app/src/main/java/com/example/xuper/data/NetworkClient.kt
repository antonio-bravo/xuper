package com.example.xuper.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {
    val defaultClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val fastClient: OkHttpClient by lazy {
        defaultClient.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
    }
}
