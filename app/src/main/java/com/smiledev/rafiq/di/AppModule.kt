package com.smiledev.rafiq.di

import android.content.Context
import com.smiledev.rafiq.core.DatabaseCopier
import com.smiledev.rafiq.data.local.BookmarkDatabase
import com.smiledev.rafiq.data.local.BookmarkDao
import com.smiledev.rafiq.data.local.PrayerLogDatabase
import com.smiledev.rafiq.data.local.PrayerLogDao
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.data.remote.AladhanApiService
import com.smiledev.rafiq.data.remote.MetalPriceApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabaseCopier(@ApplicationContext context: Context): DatabaseCopier {
        return DatabaseCopier(context)
    }

    @Provides
    @Singleton
    fun provideBookmarkDatabase(@ApplicationContext context: Context): BookmarkDatabase {
        return BookmarkDatabase.getInstance(context)
    }

    @Provides
    fun provideBookmarkDao(database: BookmarkDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun providePrayerLogDatabase(@ApplicationContext context: Context): PrayerLogDatabase {
        return PrayerLogDatabase.getInstance(context)
    }

    @Provides
    fun providePrayerLogDao(database: PrayerLogDatabase): PrayerLogDao {
        return database.prayerLogDao()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideGsonConverterFactory(): GsonConverterFactory {
        return GsonConverterFactory.create()
    }

    @Provides
    @Singleton
    fun provideAladhanRetrofit(
        client: OkHttpClient,
        gson: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/")
            .client(client)
            .addConverterFactory(gson)
            .build()
    }

    @Provides
    @Singleton
    fun provideMetalPriceRetrofit(
        client: OkHttpClient,
        gson: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.metals.live/")
            .client(client)
            .addConverterFactory(gson)
            .build()
    }

    @Provides
    @Singleton
    fun provideAladhanApiService(aladhanRetrofit: Retrofit): AladhanApiService {
        return aladhanRetrofit.create(AladhanApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMetalPriceApiService(metalPriceRetrofit: Retrofit): MetalPriceApiService {
        return metalPriceRetrofit.create(MetalPriceApiService::class.java)
    }
}
