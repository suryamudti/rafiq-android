package com.smiledev.rafiq_quran.di.koin

import com.smiledev.rafiq_quran.domain.usecase.CalculateQiblaUseCase
import com.smiledev.rafiq_quran.domain.usecase.CalculateZakatUseCase
import com.smiledev.rafiq_quran.domain.usecase.CheckAppUpdateUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetAsmaulHusnaUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetAyahsWithTranslationUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetIslamicEventsUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetPrayerGuidanceUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetPrayerTimesUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetProphetsUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetRecitersUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetSunnahGuidanceUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetSurahsUseCase
import com.smiledev.rafiq_quran.domain.usecase.GetTasbihItemsUseCase
import com.smiledev.rafiq_quran.domain.util.SystemTodayProvider
import com.smiledev.rafiq_quran.domain.util.TodayProvider
import org.koin.dsl.module

val domainModule = module {
    single<TodayProvider> { SystemTodayProvider }

    factory { CalculateQiblaUseCase() }
    factory { CalculateZakatUseCase() }
    factory { CheckAppUpdateUseCase(repository = get()) }
    factory { GetAsmaulHusnaUseCase(asmaulHusnaRepository = get()) }
    factory { GetAyahsWithTranslationUseCase(quranRepository = get()) }
    factory { GetIslamicEventsUseCase(islamicCalendarRepository = get()) }
    factory { GetPrayerGuidanceUseCase(repository = get()) }
    factory { GetPrayerTimesUseCase(prayerTimesRepository = get()) }
    factory { GetProphetsUseCase(prophetRepository = get()) }
    factory { GetRecitersUseCase(reciterRepository = get()) }
    factory { GetSunnahGuidanceUseCase(repository = get()) }
    factory { GetSurahsUseCase(quranRepository = get()) }
    factory { GetTasbihItemsUseCase(tasbihRepository = get()) }
}
