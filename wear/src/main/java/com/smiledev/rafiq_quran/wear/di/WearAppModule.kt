package com.smiledev.rafiq_quran.wear.di

import android.content.Context
import com.smiledev.rafiq_quran.core.DatabaseCopier
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.data.remote.AladhanApi
import com.smiledev.rafiq_quran.data.remote.AladhanApiService
import com.smiledev.rafiq_quran.data.repository.LocationProviderImpl
import com.smiledev.rafiq_quran.data.repository.PrayerTimesRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.QuranRepositoryImpl
import com.smiledev.rafiq_quran.domain.repository.LocationProvider
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import com.smiledev.rafiq_quran.domain.usecase.CalculateQiblaUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetAyahsWithTranslationUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetPrayerTimesUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetSurahsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WearRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindQuranRepository(impl: QuranRepositoryImpl): QuranRepository

    @Binds
    @Singleton
    abstract fun bindPrayerTimesRepository(impl: PrayerTimesRepositoryImpl): PrayerTimesRepository

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: LocationProviderImpl): LocationProvider
}

@Module
@InstallIn(SingletonComponent::class)
object WearAppModule {

    @Provides
    @Singleton
    fun provideDatabaseCopier(@ApplicationContext context: Context): DatabaseCopier {
        return DatabaseCopier(context)
    }

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "RafiqWearApp/1.0 (Wear OS Islamic App)")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideAladhanApiService(client: OkHttpClient): AladhanApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladhanApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAladhanApi(service: AladhanApiService): AladhanApi = AladhanApi(service)

    @Provides
    @Singleton
    fun provideGetSurahsUseCase(repo: QuranRepository): GetSurahsUseCase = GetSurahsUseCase(repo)

    @Provides
    @Singleton
    fun provideGetAyahsWithTranslationUseCase(repo: QuranRepository): GetAyahsWithTranslationUseCase =
        GetAyahsWithTranslationUseCase(repo)

    @Provides
    @Singleton
    fun provideGetPrayerTimesUseCase(repo: PrayerTimesRepository): GetPrayerTimesUseCase =
        GetPrayerTimesUseCase(repo)

    @Provides
    @Singleton
    fun provideCalculateQiblaUseCase(): CalculateQiblaUseCase = CalculateQiblaUseCase()
}
