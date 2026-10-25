package com.smiledev.rafiq_quran.di.koin

import android.content.Context
import com.smiledev.rafiq_quran.core.DatabaseCopier
import com.smiledev.rafiq_quran.data.local.BookmarkDatabase
import com.smiledev.rafiq_quran.data.local.HifzDatabase
import com.smiledev.rafiq_quran.data.local.PrayerLogDatabase
import com.smiledev.rafiq_quran.data.local.TasbihDatabase
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.data.repository.AppUpdateRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.AsmaulHusnaRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.BookmarkRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.HadithRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.HifzRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.IslamicCalendarRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.LocationProviderImpl
import com.smiledev.rafiq_quran.data.repository.MetalPriceRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.MosqueRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.PrayerGuidanceRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.PrayerLogRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.PrayerTimesRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.ProphetRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.QuranRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.ReciterRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.SunnahGuidanceRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.TasbihHistoryRepositoryImpl
import com.smiledev.rafiq_quran.data.repository.TasbihRepositoryImpl
import com.smiledev.rafiq_quran.domain.repository.AppUpdateRepository
import com.smiledev.rafiq_quran.domain.repository.AsmaulHusnaRepository
import com.smiledev.rafiq_quran.domain.repository.BookmarkRepository
import com.smiledev.rafiq_quran.domain.repository.HadithRepository
import com.smiledev.rafiq_quran.domain.repository.HifzRepository
import com.smiledev.rafiq_quran.domain.repository.IslamicCalendarRepository
import com.smiledev.rafiq_quran.domain.repository.LocationProvider
import com.smiledev.rafiq_quran.domain.repository.MetalPriceRepository
import com.smiledev.rafiq_quran.domain.repository.MosqueRepository
import com.smiledev.rafiq_quran.domain.repository.PrayerGuidanceRepository
import com.smiledev.rafiq_quran.domain.repository.PrayerLogRepository
import com.smiledev.rafiq_quran.domain.repository.PrayerTimesRepository
import com.smiledev.rafiq_quran.domain.repository.ProphetRepository
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import com.smiledev.rafiq_quran.domain.repository.ReciterRepository
import com.smiledev.rafiq_quran.domain.repository.SunnahGuidanceRepository
import com.smiledev.rafiq_quran.domain.repository.TasbihHistoryRepository
import com.smiledev.rafiq_quran.domain.repository.TasbihRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { PreferencesManager(context = androidContext()) }
    single { DatabaseCopier(context = androidContext()) }

    // Databases & DAOs
    single { BookmarkDatabase.getInstance(context = androidContext()) }
    single { get<BookmarkDatabase>().bookmarkDao() }

    single { PrayerLogDatabase.getInstance(context = androidContext()) }
    single { get<PrayerLogDatabase>().prayerLogDao() }

    single { HifzDatabase.getInstance(context = androidContext()) }
    single { get<HifzDatabase>().hifzDao() }

    single { TasbihDatabase.getInstance(context = androidContext()) }
    single { get<TasbihDatabase>().tasbihDao() }

    // Repositories
    single<QuranRepository> { QuranRepositoryImpl(context = androidContext(), databaseCopier = get()) }
    single<BookmarkRepository> { BookmarkRepositoryImpl(context = androidContext()) }
    single<HadithRepository> { HadithRepositoryImpl(context = androidContext(), databaseCopier = get()) }
    single<HifzRepository> { HifzRepositoryImpl(context = androidContext()) }
    single<TasbihRepository> { TasbihRepositoryImpl(context = androidContext()) }
    single<TasbihHistoryRepository> { TasbihHistoryRepositoryImpl(tasbihDao = get()) }
    single<PrayerTimesRepository> { PrayerTimesRepositoryImpl(aladhanApi = get(), preferencesManager = get()) }
    single<PrayerLogRepository> { PrayerLogRepositoryImpl(context = androidContext()) }
    single<PrayerGuidanceRepository> { PrayerGuidanceRepositoryImpl(context = androidContext()) }
    single<SunnahGuidanceRepository> { SunnahGuidanceRepositoryImpl(context = androidContext()) }
    single<AsmaulHusnaRepository> { AsmaulHusnaRepositoryImpl(context = androidContext()) }
    single<ProphetRepository> { ProphetRepositoryImpl(context = androidContext()) }
    single<IslamicCalendarRepository> { IslamicCalendarRepositoryImpl(context = androidContext()) }
    single<MetalPriceRepository> { MetalPriceRepositoryImpl(metalPriceApi = get()) }
    single<MosqueRepository> { MosqueRepositoryImpl(overpassApi = get()) }
    single<AppUpdateRepository> { AppUpdateRepositoryImpl(apiService = get(), dispatcherProvider = get()) }
    single<ReciterRepository> { ReciterRepositoryImpl(context = androidContext()) }
    single<LocationProvider> { LocationProviderImpl(context = androidContext(), preferencesManager = get()) }
}
