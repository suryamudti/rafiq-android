package com.smiledev.rafiq_quran.ui.settings

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val themeFlow = MutableStateFlow("system")
    private val langFlow = MutableStateFlow("system")
    private val hiddenFlow = MutableStateFlow(setOf("zakat"))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { preferencesManager.themeMode } returns themeFlow
        every { preferencesManager.translationLanguage } returns langFlow
        every { preferencesManager.hiddenDashboardKeys } returns hiddenFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hidden keys appear in ui state`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(preferencesManager, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(setOf("zakat"), vm.uiState.value.hiddenKeys)
    }

    @Test
    fun `toggle delegates to preferences`() = runTest(testDispatcher) {
        coEvery { preferencesManager.toggleDashboardKey(any()) } returns Unit

        val vm = SettingsViewModel(preferencesManager, testDispatcherProvider)
        advanceUntilIdle()

        vm.toggleDashboardKey("qibla")
        advanceUntilIdle()

        coVerify { preferencesManager.toggleDashboardKey("qibla") }
    }

    @Test
    fun `reset clears hidden set`() = runTest(testDispatcher) {
        coEvery { preferencesManager.setDashboardHidden(any()) } returns Unit

        val vm = SettingsViewModel(preferencesManager, testDispatcherProvider)
        advanceUntilIdle()

        vm.resetDashboard()
        advanceUntilIdle()

        coVerify { preferencesManager.setDashboardHidden(emptySet()) }
    }
}
