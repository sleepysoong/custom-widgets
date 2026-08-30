package com.customwidgets.app.di

import android.content.Context
import com.customwidgets.app.ai.AiConfigStore
import com.customwidgets.app.ai.AiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideAiService(okHttpClient: OkHttpClient): AiService {
        return AiService(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideAiConfigStore(@ApplicationContext context: Context): AiConfigStore {
        return AiConfigStore(context)
    }
}
