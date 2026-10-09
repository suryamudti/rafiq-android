package com.smiledev.rafiq_quran.ui.settings

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.AppUpdateInfo
import com.smiledev.rafiq_quran.domain.usecase.CheckAppUpdateUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase = mockk(relaxed = true)

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

    @Test
    fun `checkForUpdate when update available sets updateInfo`() = runTest(testDispatcher) {
        val updateInfo = AppUpdateInfo(
            currentVersion = "1.0",
            latestVersion = "1.0.74",
            isUpdateAvailable = true,
            releaseName = "Release v1.0.74",
            releaseUrl = "https://github.com/suryamudti/rafiq-android/releases/tag/v1.0.74"
        )
        coEvery { checkAppUpdateUseCase(any()) } returns Result.Success(updateInfo)

        val vm = SettingsViewModel(
            preferencesManager = preferencesManager,
            checkAppUpdateUseCase = checkAppUpdateUseCase,
            context = null,
            dispatcherProvider = testDispatcherProvider
        )
        advanceUntilIdle()

        vm.checkForUpdate()
        advanceUntilIdle()

        assertEquals(updateInfo, vm.uiState.value.updateInfo)
        assertFalse(vm.uiState.value.isCheckingUpdate)
        assertNull(vm.uiState.value.updateStatus)

        vm.dismissUpdateDialog()
        assertNull(vm.uiState.value.updateInfo)
    }

    @Test
    fun `checkForUpdate when up to date sets UP_TO_DATE status`() = runTest(testDispatcher) {
        val updateInfo = AppUpdateInfo(
            currentVersion = "1.0.74",
            latestVersion = "1.0.74",
            isUpdateAvailable = false,
            releaseName = "Release v1.0.74",
            releaseUrl = "https://github.com/suryamudti/rafiq-android/releases/tag/v1.0.74"
        )
        coEvery { checkAppUpdateUseCase(any()) } returns Result.Success(updateInfo)

        val vm = SettingsViewModel(
            preferencesManager = preferencesManager,
            checkAppUpdateUseCase = checkAppUpdateUseCase,
            context = null,
            dispatcherProvider = testDispatcherProvider
        )
        advanceUntilIdle()

        vm.checkForUpdate()
        advanceUntilIdle()

        assertNull(vm.uiState.value.updateInfo)
        assertEquals(UpdateCheckStatus.UP_TO_DATE, vm.uiState.value.updateStatus)

        vm.clearUpdateStatus()
        assertNull(vm.uiState.value.updateStatus)
    }

    @Test
    fun `checkForUpdate when error sets ERROR status`() = runTest(testDispatcher) {
        coEvery { checkAppUpdateUseCase(any()) } returns Result.Error(AppError.Network("Network failed"))

        val vm = SettingsViewModel(
            preferencesManager = preferencesManager,
            checkAppUpdateUseCase = checkAppUpdateUseCase,
            context = null,
            dispatcherProvider = testDispatcherProvider
        )
        advanceUntilIdle()

        vm.checkForUpdate()
        advanceUntilIdle()

        assertNull(vm.uiState.value.updateInfo)
        assertEquals(UpdateCheckStatus.ERROR, vm.uiState.value.updateStatus)
    }
}
