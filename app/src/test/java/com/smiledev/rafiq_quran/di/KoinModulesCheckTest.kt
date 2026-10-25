package com.smiledev.rafiq_quran.di

import android.content.Context
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.data.remote.AladhanApi
import com.smiledev.rafiq_quran.di.koin.appKoinModules
import com.smiledev.rafiq_quran.domain.repository.QuranRepository
import com.smiledev.rafiq_quran.domain.usecase.GetSurahsUseCase
import com.smiledev.rafiq_quran.domain.util.TodayProvider
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get

class KoinModulesCheckTest : KoinTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun verifyKoinModulesInitializeCleanly() {
        val mockContext: Context = mockk(relaxed = true)
        val koinApp = startKoin {
            androidContext(mockContext)
            modules(appKoinModules)
        }
        assertNotNull(koinApp)
        assertNotNull(koinApp.koin)

        // Verify Core
        val dispatcherProvider: DispatcherProvider = get()
        assertNotNull(dispatcherProvider)

        val todayProvider: TodayProvider = get()
        assertNotNull(todayProvider)

        // Verify Network & Preferences
        val aladhanApi: AladhanApi = get()
        assertNotNull(aladhanApi)

        val preferencesManager: PreferencesManager = get()
        assertNotNull(preferencesManager)

        // Verify Repository & UseCase
        val quranRepo: QuranRepository = get()
        assertNotNull(quranRepo)

        val getSurahsUseCase: GetSurahsUseCase = get()
        assertNotNull(getSurahsUseCase)
    }
}
