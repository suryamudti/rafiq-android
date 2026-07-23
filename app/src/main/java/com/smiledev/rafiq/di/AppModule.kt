package com.smiledev.rafiq.di

import android.content.Context
import com.smiledev.rafiq.core.DatabaseCopier
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.data.local.BookmarkDatabase
import com.smiledev.rafiq.data.local.BookmarkDao
import com.smiledev.rafiq.data.local.PrayerLogDatabase
import com.smiledev.rafiq.data.local.PrayerLogDao
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.data.remote.AladhanApiService
import com.smiledev.rafiq.data.remote.MetalPriceApiService
import com.smiledev.rafiq.data.repository.AsmaulHusnaRepositoryImpl
import com.smiledev.rafiq.data.repository.BookmarkRepositoryImpl
import com.smiledev.rafiq.data.repository.IslamicCalendarRepositoryImpl
import com.smiledev.rafiq.data.repository.MetalPriceRepositoryImpl
import com.smiledev.rafiq.data.repository.PrayerLogRepositoryImpl
import com.smiledev.rafiq.data.repository.PrayerTimesRepositoryImpl
import com.smiledev.rafiq.data.repository.ProphetRepositoryImpl
import com.smiledev.rafiq.data.repository.QuranRepositoryImpl
import com.smiledev.rafiq.data.repository.ReciterRepositoryImpl
import com.smiledev.rafiq.domain.repository.AsmaulHusnaRepository
import com.smiledev.rafiq.domain.repository.BookmarkRepository
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository
import com.smiledev.rafiq.domain.repository.MetalPriceRepository
import com.smiledev.rafiq.domain.repository.PrayerLogRepository
import com.smiledev.rafiq.domain.repository.PrayerTimesRepository
import com.smiledev.rafiq.domain.repository.ProphetRepository
import com.smiledev.rafiq.domain.repository.QuranRepository
import com.smiledev.rafiq.domain.repository.ReciterRepository
import com.smiledev.rafiq.domain.usecase.CalculateQiblaUseCase
import com.smiledev.rafiq.domain.usecase.CalculateZakatUseCase
import com.smiledev.rafiq.domain.usecase.GetAyahsWithTranslationUseCase
import com.smiledev.rafiq.domain.usecase.GetAsmaulHusnaUseCase
import com.smiledev.rafiq.domain.usecase.GetIslamicEventsUseCase
import com.smiledev.rafiq.domain.usecase.GetPrayerTimesUseCase
import com.smiledev.rafiq.domain.usecase.GetProphetsUseCase
import com.smiledev.rafiq.domain.usecase.GetRecitersUseCase
import com.smiledev.rafiq.domain.usecase.GetSurahsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindQuranRepository(impl: QuranRepositoryImpl): QuranRepository
    @Binds @Singleton abstract fun bindAsmaulHusnaRepository(impl: AsmaulHusnaRepositoryImpl): AsmaulHusnaRepository
    @Binds @Singleton abstract fun bindIslamicCalendarRepository(impl: IslamicCalendarRepositoryImpl): IslamicCalendarRepository
    @Binds @Singleton abstract fun bindPrayerTimesRepository(impl: PrayerTimesRepositoryImpl): PrayerTimesRepository
    @Binds @Singleton abstract fun bindMetalPriceRepository(impl: MetalPriceRepositoryImpl): MetalPriceRepository
    @Binds @Singleton abstract fun bindProphetRepository(impl: ProphetRepositoryImpl): ProphetRepository
    @Binds @Singleton abstract fun bindReciterRepository(impl: ReciterRepositoryImpl): ReciterRepository
    @Binds @Singleton abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository
    @Binds @Singleton abstract fun bindPrayerLogRepository(impl: PrayerLogRepositoryImpl): PrayerLogRepository
}

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
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider

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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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
    @Named("aladhan")
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
    @Named("metalprice")
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
    fun provideAladhanApiService(@Named("aladhan") aladhanRetrofit: Retrofit): AladhanApiService {
        return aladhanRetrofit.create(AladhanApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMetalPriceApiService(@Named("metalprice") metalPriceRetrofit: Retrofit): MetalPriceApiService {
        return metalPriceRetrofit.create(MetalPriceApiService::class.java)
    }

    @Provides @Singleton
    fun provideGetSurahsUseCase(repo: QuranRepository): GetSurahsUseCase = GetSurahsUseCase(repo)

    @Provides @Singleton
    fun provideGetAyahsWithTranslationUseCase(repo: QuranRepository): GetAyahsWithTranslationUseCase = GetAyahsWithTranslationUseCase(repo)

    @Provides @Singleton
    fun provideGetAsmaulHusnaUseCase(repo: AsmaulHusnaRepository): GetAsmaulHusnaUseCase = GetAsmaulHusnaUseCase(repo)

    @Provides @Singleton
    fun provideGetPrayerTimesUseCase(repo: PrayerTimesRepository): GetPrayerTimesUseCase = GetPrayerTimesUseCase(repo)

    @Provides @Singleton
    fun provideGetIslamicEventsUseCase(repo: IslamicCalendarRepository): GetIslamicEventsUseCase = GetIslamicEventsUseCase(repo)

    @Provides @Singleton
    fun provideGetProphetsUseCase(repo: ProphetRepository): GetProphetsUseCase = GetProphetsUseCase(repo)

    @Provides @Singleton
    fun provideGetRecitersUseCase(repo: ReciterRepository): GetRecitersUseCase = GetRecitersUseCase(repo)

    @Provides @Singleton
    fun provideCalculateZakatUseCase(repo: MetalPriceRepository): CalculateZakatUseCase = CalculateZakatUseCase(repo)

    @Provides @Singleton
    fun provideCalculateQiblaUseCase(): CalculateQiblaUseCase = CalculateQiblaUseCase()
}
