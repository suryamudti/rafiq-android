package com.smiledev.rafiq_quran.di

import android.content.Context
import com.smiledev.rafiq_quran.core.DatabaseCopier
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.data.local.BookmarkDatabase
import com.smiledev.rafiq_quran.data.local.BookmarkDao
import com.smiledev.rafiq_quran.data.local.PrayerLogDatabase
import com.smiledev.rafiq_quran.data.local.PrayerLogDao
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.data.remote.AladhanApiService
import com.smiledev.rafiq_quran.data.remote.EQuranApiService
import com.smiledev.rafiq_quran.data.remote.IslamicAppApiService
import com.smiledev.rafiq_quran.data.remote.MetalPriceApiService
import com.smiledev.rafiq_quran.data.remote.OverpassApi
import com.smiledev.rafiq_quran.data.remote.OverpassApiService
import com.smiledev.rafiq_quran.data.repository.AsmaulHusnaRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.BookmarkRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.HadithRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.IslamicCalendarRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.LocationProviderImpl
import com.smiledev.rafiq_quran.data.repository.MetalPriceRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.MosqueRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.PrayerLogRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.PrayerTimesRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.ProphetRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.QuranRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.ReciterRepositoryImpl
import com.smiledev.rafiq_quran.domain.repository.AsmaulHusnaRepository
import com.smiledev.rafiq_quran.domain.repository.BookmarkRepository
import com.smiledev.rafiq_quran.domain.repository.HadithRepository
import com.smiledev.rafiq_quran.domain.repository.IslamicCalendarRepository
import com.smiledev.rafiq_quran.domain.repository.LocationProvider
import com.smiledev.rafiq_quran.domain.repository.MetalPriceRepository
import com.smiledev.rafiq_quran.domain.repository.MosqueRepository
import com.smiledev.rafiq_quran.domain.repository.PrayerLogRepository
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import com.smiledev.rafiq_quran.domain.repository.ProphetRepository
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import com.smiledev.rafiq_quran.domain.repository.ReciterRepository
import com.smiledev.rafiq_quran.domain.util.SystemTodayProvider
import com.smiledev.rafiq_quran.domain.util.TodayProvider
import com.smiledev.rafiq_quran.domain.usecase.CalculateQiblaUseCase
import com.smiledev.rafiq_quran.domain.usecase.CalculateZakatUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetAyahsWithTranslationUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetAsmaulHusnaUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetIslamicEventsUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetPrayerTimesUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetProphetsUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetRecitersUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetSurahsUseCase
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
    @Binds @Singleton abstract fun bindHadithRepository(impl: HadithRepositoryImpl): HadithRepository
    @Binds @Singleton abstract fun bindReciterRepository(impl: ReciterRepositoryImpl): ReciterRepository
    @Binds @Singleton abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository
    @Binds @Singleton abstract fun bindPrayerLogRepository(impl: PrayerLogRepositoryImpl): PrayerLogRepository
    @Binds @Singleton abstract fun bindMosqueRepository(impl: MosqueRepositoryImpl): MosqueRepository
    @Binds @Singleton abstract fun bindLocationProvider(impl: LocationProviderImpl): LocationProvider
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
    fun provideTodayProvider(): TodayProvider = SystemTodayProvider

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
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "RafiqApp/1.0 (Android Islamic App)")
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
    @Named("metalpriceClient")
    fun provideMetalPriceOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "RafiqApp/1.0 (Android Islamic App)")
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
        @Named("metalpriceClient") client: OkHttpClient,
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

    @Provides
    @Singleton
    @Named("islamicapp")
    fun provideIslamicAppRetrofit(
        client: OkHttpClient,
        gson: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.islamic.app/")
            .client(client)
            .addConverterFactory(gson)
            .build()
    }

    @Provides
    @Singleton
    fun provideIslamicAppApiService(@Named("islamicapp") retrofit: Retrofit): IslamicAppApiService {
        return retrofit.create(IslamicAppApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("equran")
    fun provideEQuranRetrofit(
        client: OkHttpClient,
        gson: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://equran.id/")
            .client(client)
            .addConverterFactory(gson)
            .build()
    }

    @Provides
    @Singleton
    fun provideEQuranApiService(@Named("equran") retrofit: Retrofit): EQuranApiService {
        return retrofit.create(EQuranApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("overpass")
    fun provideOverpassRetrofit(
        client: OkHttpClient,
        gson: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .client(client)
            .addConverterFactory(gson)
            .build()
    }

    @Provides
    @Singleton
    @Named("overpass-mirror")
    fun provideOverpassMirrorRetrofit(
        client: OkHttpClient,
        gson: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://overpass.kumi.systems/api/")
            .client(client)
            .addConverterFactory(gson)
            .build()
    }

    @Provides
    @Singleton
    @Named("overpass-mirror-2")
    fun provideOverpassMirror2Retrofit(
        client: OkHttpClient,
        gson: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://maps.mail.ru/osm/tools/overpass/api/")
            .client(client)
            .addConverterFactory(gson)
            .build()
    }

    @Provides
    @Singleton
    fun provideOverpassApiServices(
        @Named("overpass") overpassRetrofit: Retrofit,
        @Named("overpass-mirror") overpassMirrorRetrofit: Retrofit,
        @Named("overpass-mirror-2") overpassMirror2Retrofit: Retrofit
    ): List<@JvmSuppressWildcards OverpassApiService> {
        return listOf(
            overpassRetrofit.create(OverpassApiService::class.java),
            overpassMirrorRetrofit.create(OverpassApiService::class.java),
            overpassMirror2Retrofit.create(OverpassApiService::class.java)
        )
    }

    @Provides @Singleton
    fun provideOverpassApi(services: List<@JvmSuppressWildcards OverpassApiService>): OverpassApi = OverpassApi(services)

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
    fun provideCalculateZakatUseCase(): CalculateZakatUseCase = CalculateZakatUseCase()

    @Provides @Singleton
    fun provideCalculateQiblaUseCase(): CalculateQiblaUseCase = CalculateQiblaUseCase()
}
