# Zakat Calculator Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the zakat calculator show results instantly and refresh metal prices in the background instead of blocking on two sequential network calls.

**Architecture:** `CalculateZakatUseCase` becomes a pure function (prices passed in). `MetalPriceRepository` gains a parallel `fetchMetalPrices()` plus an in-memory cache. The ViewModel computes instantly with cache-or-defaults, then recomputes when a background refresh lands. A dedicated OkHttpClient with 5s timeouts caps how long the metals API can stall us.

**Tech Stack:** Kotlin 2.0.0, Hilt (KAPT), Retrofit, OkHttp, kotlinx-coroutines, Compose Material3, JUnit4 + MockK + kotlinx-coroutines-test.

## Global Constraints

- Test commands (run from repo root): `.\gradlew testDebug` (all), `.\gradlew :domain:testDebug`, `.\gradlew :data:testDebug`, `.\gradlew :app:testDebug`.
- No lint or typecheck commands exist. Verification = unit tests + `.\gradlew :app:compileDebugKotlin` / `.\gradlew assembleDebug`.
- Hilt uses KAPT with `correctErrorTypes=true` — do NOT switch to KSP.
- Cross-module smart casts from nullable don't work. When reading a nullable property from another module (`domain`, `data`, `core`), assign to a local `val` or use `!!`/`?:` explicitly; never rely on an `if` smart-cast.
- Never use Java `Math.*` — use `kotlin.math.*`.
- No new dependencies. `retryIO`, `Result`, `AppError`, `DefaultDispatcherProvider` already exist in `:core`.
- Spec: `docs/superpowers/specs/2026-08-11-zakat-performance-design.md`.

---

### Task 1: Domain — `MetalPrices` model and pure `CalculateZakatUseCase`

**Files:**
- Create: `domain/src/main/kotlin/com/smiledev/rafiq/domain/model/MetalPrices.kt`
- Rewrite: `domain/src/main/kotlin/com/smiledev/rafiq/domain/usecase/CalculateZakatUseCase.kt`
- Rewrite: `domain/src/test/kotlin/com/smiledev/rafiq/domain/usecase/CalculateZakatUseCaseTest.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt:313` (make the use case provider zero-arg)

**Interfaces:**
- Produces:
  - `data class MetalPrices(goldPricePerGram: Double, silverPricePerGram: Double)`
  - `val DefaultMetalPrices: MetalPrices` (= 65.0 gold, 0.75 silver)
  - `class CalculateZakatUseCase` with `operator fun invoke(goldWeight: Double, silverWeight: Double, cashValue: Double, currency: String = "USD", prices: MetalPrices): ZakatResult`
  - `data class ZakatResult(goldZakat, silverZakat, cashZakat, totalZakat, goldPricePerGram, silverPricePerGram)` — now the canonical `ZakatResult` (the app module's duplicate is removed in Task 4).

- [x] **Step 1: Write the failing tests**

Create `domain/src/test/kotlin/com/smiledev/rafiq/domain/usecase/CalculateZakatUseCaseTest.kt`:

```kotlin
package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.domain.model.MetalPrices
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateZakatUseCaseTest {

    private val useCase = CalculateZakatUseCase()
    private val prices = MetalPrices(goldPricePerGram = 70.0, silverPricePerGram = 0.9)

    @Test
    fun `all zero when below all nisab thresholds`() {
        val result = useCase(10.0, 50.0, 100.0, "USD", prices)
        assertEquals(0.0, result.goldZakat, 0.001)
        assertEquals(0.0, result.silverZakat, 0.001)
        assertEquals(0.0, result.cashZakat, 0.001)
        assertEquals(0.0, result.totalZakat, 0.001)
    }

    @Test
    fun `calculates gold zakat when above gold nisab`() {
        val result = useCase(100.0, 0.0, 0.0, "USD", prices)
        val expectedGoldZakat = 100.0 * 70.0 * 0.025
        assertEquals(expectedGoldZakat, result.goldZakat, 0.001)
        assertEquals(expectedGoldZakat, result.totalZakat, 0.001)
    }

    @Test
    fun `calculates silver zakat when above silver nisab`() {
        val result = useCase(0.0, 700.0, 0.0, "USD", prices)
        val expectedSilverZakat = 700.0 * 0.9 * 0.025
        assertEquals(expectedSilverZakat, result.silverZakat, 0.001)
        assertEquals(expectedSilverZakat, result.totalZakat, 0.001)
    }

    @Test
    fun `calculates cash zakat when above cash nisab`() {
        val result = useCase(0.0, 0.0, 10000.0, "USD", prices)
        assertEquals(250.0, result.cashZakat, 0.001)
        assertEquals(250.0, result.totalZakat, 0.001)
    }

    @Test
    fun `calculates total zakat when all above nisab`() {
        val result = useCase(100.0, 700.0, 10000.0, "USD", prices)
        val expectedGold = 100.0 * 70.0 * 0.025
        val expectedSilver = 700.0 * 0.9 * 0.025
        val expectedCash = 10000.0 * 0.025
        assertEquals(expectedGold, result.goldZakat, 0.001)
        assertEquals(expectedSilver, result.silverZakat, 0.001)
        assertEquals(expectedCash, result.cashZakat, 0.001)
        assertEquals(expectedGold + expectedSilver + expectedCash, result.totalZakat, 0.001)
    }

    @Test
    fun `converts IDR to USD internally for nisab check`() {
        val result = useCase(0.0, 0.0, 500000.0, "IDR", prices)
        val cashInUsd = 500000.0 / 16000.0
        val cashRateUsd = 70.0 * 85.0
        val rate = 16000.0
        val expectedCashZakat = if (cashInUsd >= cashRateUsd) cashInUsd * 0.025 * rate else 0.0
        assertEquals(expectedCashZakat, result.cashZakat, 0.001)
        assertEquals(70.0 * rate, result.goldPricePerGram, 0.001)
        assertEquals(0.9 * rate, result.silverPricePerGram, 0.001)
    }
}
```

- [x] **Step 2: Run the test to verify it fails to compile**

Run: `.\gradlew :domain:testDebug`
Expected: FAIL — `CalculateZakatUseCase` cannot be invoked with 5 args / `MetalPrices` unresolved.

- [x] **Step 3: Create the model and rewrite the use case**

Create `domain/src/main/kotlin/com/smiledev/rafiq/domain/model/MetalPrices.kt`:

```kotlin
package com.smiledev.rafiq.domain.model

data class MetalPrices(
    val goldPricePerGram: Double,
    val silverPricePerGram: Double
)

val DefaultMetalPrices = MetalPrices(
    goldPricePerGram = 65.0,
    silverPricePerGram = 0.75
)
```

Rewrite `domain/src/main/kotlin/com/smiledev/rafiq/domain/usecase/CalculateZakatUseCase.kt`:

```kotlin
package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.domain.model.MetalPrices

data class ZakatResult(
    val goldZakat: Double = 0.0,
    val silverZakat: Double = 0.0,
    val cashZakat: Double = 0.0,
    val totalZakat: Double = 0.0,
    val goldPricePerGram: Double = 0.0,
    val silverPricePerGram: Double = 0.0
)

class CalculateZakatUseCase {
    operator fun invoke(
        goldWeight: Double,
        silverWeight: Double,
        cashValue: Double,
        currency: String = "USD",
        prices: MetalPrices
    ): ZakatResult {
        val goldNisab = 85.0
        val silverNisab = 595.0
        val exchangeRate = 16000.0
        val rate = if (currency == "IDR") exchangeRate else 1.0
        val cashVUsd = if (currency == "IDR") cashValue / rate else cashValue
        val cashRateUsd = prices.goldPricePerGram * goldNisab

        val goldZakatUsd = if (goldWeight >= goldNisab) goldWeight * prices.goldPricePerGram * 0.025 else 0.0
        val silverZakatUsd = if (silverWeight >= silverNisab) silverWeight * prices.silverPricePerGram * 0.025 else 0.0
        val cashZakatUsd = if (cashVUsd >= cashRateUsd) cashVUsd * 0.025 else 0.0

        return ZakatResult(
            goldZakat = goldZakatUsd * rate,
            silverZakat = silverZakatUsd * rate,
            cashZakat = cashZakatUsd * rate,
            totalZakat = (goldZakatUsd + silverZakatUsd + cashZakatUsd) * rate,
            goldPricePerGram = prices.goldPricePerGram * rate,
            silverPricePerGram = prices.silverPricePerGram * rate
        )
    }
}
```

Note: the `16000.0` exchange rate stays hardcoded (out of scope in the spec).

- [x] **Step 4: Update the DI provider so `:app` still compiles**

In `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt`, change line ~313:

```kotlin
    @Provides @Singleton
    fun provideCalculateZakatUseCase(): CalculateZakatUseCase = CalculateZakatUseCase()
```

(Remove the `repo: MetalPriceRepository` parameter.)

- [x] **Step 5: Run the domain tests**

Run: `.\gradlew :domain:testDebug`
Expected: PASS (6 tests).

- [x] **Step 6: Verify `:app` still compiles**

Run: `.\gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (the ViewModel still calls the repository's per-metal methods, which still exist at this point).

- [x] **Step 7: Commit**

```bash
git add domain/src/main/kotlin/com/smiledev/rafiq/domain/model/MetalPrices.kt domain/src/main/kotlin/com/smiledev/rafiq/domain/usecase/CalculateZakatUseCase.kt domain/src/test/kotlin/com/smiledev/rafiq/domain/usecase/CalculateZakatUseCaseTest.kt app/src/main/java/com/smiledev/rafiq/di/AppModule.kt
git commit -m "feat(domain): pure CalculateZakatUseCase with MetalPrices input"
```

---

### Task 2: Data — parallel fetch with in-memory cache

**Files:**
- Modify: `domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/MetalPriceRepository.kt`
- Modify: `data/src/main/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImpl.kt`
- Rewrite: `data/src/test/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `MetalPrices` (Task 1), `retryIO` from `:core`.
- Produces:
  - `suspend fun fetchMetalPrices(): Result<MetalPrices, AppError>` — parallel gold+silver fetch, caches in memory, returns.
  - `fun getCachedMetalPrices(): MetalPrices?`
  - The two per-metal methods are kept for now (removed in Task 4).

- [x] **Step 1: Write the failing tests**

Rewrite `data/src/test/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImplTest.kt`:

```kotlin
package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.remote.MetalPriceApi
import com.smiledev.rafiq.domain.model.MetalPrices
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MetalPriceRepositoryImplTest {

    private val metalPriceApi: MetalPriceApi = mockk()
    private lateinit var repo: MetalPriceRepositoryImpl

    @Before
    fun setUp() {
        repo = MetalPriceRepositoryImpl(metalPriceApi)
    }

    @Test
    fun `fetchMetalPrices fetches gold and silver and caches result`() = runTest {
        coEvery { metalPriceApi.getGoldPricePerGram() } returns 65.0
        coEvery { metalPriceApi.getSilverPricePerGram() } returns 0.75

        val result = repo.fetchMetalPrices()

        assertTrue(result is Result.Success)
        val prices = (result as Result.Success).data
        assertEquals(65.0, prices.goldPricePerGram, 0.001)
        assertEquals(0.75, prices.silverPricePerGram, 0.001)
        assertEquals(MetalPrices(65.0, 0.75), repo.getCachedMetalPrices())
    }

    @Test
    fun `getCachedMetalPrices is null before any fetch`() {
        assertEquals(null, repo.getCachedMetalPrices())
    }

    @Test
    fun `network error returns AppError and does not cache`() = runTest {
        coEvery { metalPriceApi.getGoldPricePerGram() } throws RuntimeException("Timeout")

        val result = repo.fetchMetalPrices()

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
        assertEquals(null, repo.getCachedMetalPrices())
    }

    @Test
    fun `getGoldPricePerGram returns converted price`() = runTest {
        coEvery { metalPriceApi.getGoldPricePerGram() } returns 65.0

        val result = repo.getGoldPricePerGram()

        val price = (result as Result.Success).data
        assertEquals(65.0, price, 0.001)
    }

    @Test
    fun `getSilverPricePerGram returns converted price`() = runTest {
        coEvery { metalPriceApi.getSilverPricePerGram() } returns 0.75

        val result = repo.getSilverPricePerGram()

        val price = (result as Result.Success).data
        assertEquals(0.75, price, 0.001)
    }
}
```

- [x] **Step 2: Run the data tests to verify they fail**

Run: `.\gradlew :data:testDebug`
Expected: FAIL — `fetchMetalPrices` / `getCachedMetalPrices` unresolved.

- [x] **Step 3: Extend the repository interface**

`domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/MetalPriceRepository.kt`:

```kotlin
package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.MetalPrices

interface MetalPriceRepository {
    suspend fun getGoldPricePerGram(): Result<Double, AppError>
    suspend fun getSilverPricePerGram(): Result<Double, AppError>
    suspend fun fetchMetalPrices(): Result<MetalPrices, AppError>
    fun getCachedMetalPrices(): MetalPrices?
}
```

- [x] **Step 4: Implement parallel fetch + cache in the impl**

`data/src/main/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImpl.kt`:

```kotlin
package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.retryIO
import com.smiledev.rafiq.data.remote.MetalPriceApi
import com.smiledev.rafiq.domain.model.MetalPrices
import com.smiledev.rafiq.domain.repository.MetalPriceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class MetalPriceRepositoryImpl @Inject constructor(
    private val metalPriceApi: MetalPriceApi
) : MetalPriceRepository {

    @Volatile
    private var cachedPrices: MetalPrices? = null

    override suspend fun getGoldPricePerGram(): Result<Double, AppError> {
        return retryIO {
            try {
                Result.Success(metalPriceApi.getGoldPricePerGram())
            } catch (e: Exception) {
                Result.Error(AppError.Network("Failed to fetch gold price", e))
            }
        }
    }

    override suspend fun getSilverPricePerGram(): Result<Double, AppError> {
        return retryIO {
            try {
                Result.Success(metalPriceApi.getSilverPricePerGram())
            } catch (e: Exception) {
                Result.Error(AppError.Network("Failed to fetch silver price", e))
            }
        }
    }

    override suspend fun fetchMetalPrices(): Result<MetalPrices, AppError> {
        return retryIO(times = 2, initialDelay = 50, maxDelay = 300) {
            try {
                val prices = coroutineScope {
                    val gold = async { metalPriceApi.getGoldPricePerGram() }
                    val silver = async { metalPriceApi.getSilverPricePerGram() }
                    MetalPrices(gold.await(), silver.await())
                }
                cachedPrices = prices
                Result.Success(prices)
            } catch (e: Exception) {
                Result.Error(AppError.Network("Failed to fetch metal prices", e))
            }
        }
    }

    override fun getCachedMetalPrices(): MetalPrices? = cachedPrices
}
```

- [x] **Step 5: Run the data tests**

Run: `.\gradlew :data:testDebug`
Expected: PASS (5 tests).

- [x] **Step 6: Commit**

```bash
git add domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/MetalPriceRepository.kt data/src/main/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImpl.kt data/src/test/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImplTest.kt
git commit -m "feat(data): parallel metal price fetch with in-memory cache"
```

---

### Task 3: DI — dedicated OkHttpClient with 5s timeouts for metals API

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt` (add a provider near line 145; change `provideMetalPriceRetrofit` at lines 166-178)

**Interfaces:**
- Produces: `@Named("metalpriceClient") OkHttpClient` — 5s timeouts + User-Agent + BASIC logging.

- [x] **Step 1: Add the dedicated client**

In `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt`, right after `provideOkHttpClient()` (ends line 144), add:

```kotlin
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
```

- [x] **Step 2: Point the metals Retrofit at the dedicated client**

Change `provideMetalPriceRetrofit` to:

```kotlin
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
```

`@Named` is already imported in this file.

- [x] **Step 3: Compile-check `:app`**

Run: `.\gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [x] **Step 4: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/di/AppModule.kt
git commit -m "perf(di): dedicated 5s-timeout OkHttpClient for metals API"
```

---

### Task 4: App — instant-result ViewModel with background refresh

**Files:**
- Rewrite: `app/src/main/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorViewModel.kt`
- Modify: `domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/MetalPriceRepository.kt` (remove per-metal methods)
- Modify: `data/src/main/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImpl.kt` (remove per-metal methods)
- Modify: `data/src/test/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImplTest.kt` (remove per-metal tests)
- Rewrite: `app/src/test/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorViewModelTest.kt`

**Interfaces:**
- Consumes: `CalculateZakatUseCase` (Task 1), `MetalPrices`/`DefaultMetalPrices` (Task 1), `MetalPriceRepository.fetchMetalPrices()` + `getCachedMetalPrices()` (Task 2).
- Produces: `ZakatUiState(goldWeight, silverWeight, cashAmount, selectedCurrency, result: ZakatResult, isUsingFallback: Boolean)`.

- [x] **Step 1: Write the failing ViewModel tests**

Rewrite `app/src/test/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorViewModelTest.kt`:

```kotlin
package com.smiledev.rafiq.ui.zakat

import androidx.lifecycle.SavedStateHandle
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.MetalPrices
import com.smiledev.rafiq.domain.repository.MetalPriceRepository
import com.smiledev.rafiq.domain.usecase.CalculateZakatUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ZakatCalculatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val metalPriceRepository: MetalPriceRepository = mockk()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = testDispatcher
        override val io: CoroutineDispatcher get() = testDispatcher
        override val default: CoroutineDispatcher get() = testDispatcher
        override val unconfined: CoroutineDispatcher get() = testDispatcher
    }

    private fun createViewModel(): ZakatCalculatorViewModel {
        return ZakatCalculatorViewModel(
            CalculateZakatUseCase(),
            metalPriceRepository,
            testDispatcherProvider,
            SavedStateHandle()
        )
    }

    @Test
    fun `calculate shows instant result from defaults then refreshes with fresh prices`() = runTest(testDispatcher) {
        every { metalPriceRepository.getCachedMetalPrices() } returns null
        coEvery { metalPriceRepository.fetchMetalPrices() } returns Result.Success(MetalPrices(60.0, 0.70))

        val viewModel = createViewModel()
        viewModel.updateGold("100.0")
        viewModel.updateSilver("600.0")
        viewModel.updateCash("10000.0")
        viewModel.calculate()

        // Instant: defaults 65.0 gold / 0.75 silver, fallback flag on
        var state = viewModel.uiState.value
        assertEquals(100.0 * 65.0 * 0.025, state.result.goldZakat, 0.01)
        assertEquals(600.0 * 0.75 * 0.025, state.result.silverZakat, 0.01)
        assertEquals(true, state.isUsingFallback)

        advanceUntilIdle()

        // Refreshed: fetched 60.0 / 0.70, fallback flag cleared
        state = viewModel.uiState.value
        assertEquals(150.0, state.result.goldZakat, 0.01)
        assertEquals(10.5, state.result.silverZakat, 0.01)
        assertEquals(250.0, state.result.cashZakat, 0.01)
        assertEquals(410.5, state.result.totalZakat, 0.01)
        assertEquals(false, state.isUsingFallback)
    }

    @Test
    fun `calculate uses cached prices instantly without fallback flag`() = runTest(testDispatcher) {
        every { metalPriceRepository.getCachedMetalPrices() } returns MetalPrices(60.0, 0.70)
        coEvery { metalPriceRepository.fetchMetalPrices() } returns Result.Success(MetalPrices(60.0, 0.70))

        val viewModel = createViewModel()
        viewModel.updateGold("100.0")
        viewModel.updateSilver("600.0")
        viewModel.updateCash("10000.0")
        viewModel.calculate()

        var state = viewModel.uiState.value
        assertEquals(150.0, state.result.goldZakat, 0.01)
        assertEquals(10.5, state.result.silverZakat, 0.01)
        assertEquals(false, state.isUsingFallback)

        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(410.5, state.result.totalZakat, 0.01)
        assertEquals(false, state.isUsingFallback)
    }

    @Test
    fun `calculate in USD computes no zakat when below nisab`() = runTest(testDispatcher) {
        every { metalPriceRepository.getCachedMetalPrices() } returns MetalPrices(60.0, 0.70)
        coEvery { metalPriceRepository.fetchMetalPrices() } returns Result.Success(MetalPrices(60.0, 0.70))

        val viewModel = createViewModel()
        viewModel.updateGold("50.0")
        viewModel.updateSilver("500.0")
        viewModel.updateCash("100.0")
        viewModel.calculate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0.0, state.result.goldZakat, 0.01)
        assertEquals(0.0, state.result.silverZakat, 0.01)
        assertEquals(0.0, state.result.cashZakat, 0.01)
        assertEquals(0.0, state.result.totalZakat, 0.01)
    }

    @Test
    fun `calculate in IDR correctly converts currency and applies conversion rate`() = runTest(testDispatcher) {
        every { metalPriceRepository.getCachedMetalPrices() } returns MetalPrices(60.0, 0.70)
        coEvery { metalPriceRepository.fetchMetalPrices() } returns Result.Success(MetalPrices(60.0, 0.70))

        val viewModel = createViewModel()
        viewModel.updateCash("100000000.0")
        viewModel.updateCurrency("IDR")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2500000.0, state.result.cashZakat, 0.01)
        assertEquals(2500000.0, state.result.totalZakat, 0.01)
        assertEquals(960000.0, state.result.goldPricePerGram, 0.01)
    }
}
```

- [x] **Step 2: Run the app tests to verify they fail**

Run: `.\gradlew :app:testDebug`
Expected: FAIL — `ZakatCalculatorViewModel` constructor signature mismatch / duplicate `ZakatResult` ambiguity / missing `isUsingFallback`.

- [x] **Step 3: Rewrite the ViewModel**

Rewrite `app/src/main/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorViewModel.kt`:

```kotlin
package com.smiledev.rafiq.ui.zakat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.DefaultMetalPrices
import com.smiledev.rafiq.domain.model.MetalPrices
import com.smiledev.rafiq.domain.repository.MetalPriceRepository
import com.smiledev.rafiq.domain.usecase.CalculateZakatUseCase
import com.smiledev.rafiq.domain.usecase.ZakatResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ZakatUiState(
    val goldWeight: String = "",
    val silverWeight: String = "",
    val cashAmount: String = "",
    val selectedCurrency: String = "USD",
    val result: ZakatResult = ZakatResult(),
    val isUsingFallback: Boolean = false
)

@HiltViewModel
class ZakatCalculatorViewModel @Inject constructor(
    private val calculateZakatUseCase: CalculateZakatUseCase,
    private val metalPriceRepository: MetalPriceRepository,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ZakatUiState(
            goldWeight = savedStateHandle.get<String>("goldWeight") ?: "",
            silverWeight = savedStateHandle.get<String>("silverWeight") ?: "",
            cashAmount = savedStateHandle.get<String>("cashAmount") ?: "",
            selectedCurrency = savedStateHandle.get<String>("selectedCurrency") ?: "USD"
        )
    )
    val uiState: StateFlow<ZakatUiState> = _uiState

    init {
        prefetchPrices()
    }

    fun updateGold(value: String) { _uiState.value = _uiState.value.copy(goldWeight = value) }
    fun updateSilver(value: String) { _uiState.value = _uiState.value.copy(silverWeight = value) }
    fun updateCash(value: String) { _uiState.value = _uiState.value.copy(cashAmount = value) }

    fun updateCurrency(value: String) {
        _uiState.value = _uiState.value.copy(selectedCurrency = value)
        calculate()
    }

    fun calculate() {
        val s = _uiState.value
        val cached = metalPriceRepository.getCachedMetalPrices()
        publishResult(s, cached ?: DefaultMetalPrices, isUsingFallback = cached == null)
        refreshPrices()
    }

    private fun prefetchPrices() {
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = metalPriceRepository.fetchMetalPrices()) {
                is Result.Success -> recomputeIfHasInputs(result.data)
                is Result.Error -> Unit
            }
        }
    }

    private fun refreshPrices() {
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = metalPriceRepository.fetchMetalPrices()) {
                is Result.Success -> recomputeIfHasInputs(result.data)
                is Result.Error -> Unit
            }
        }
    }

    private fun recomputeIfHasInputs(prices: MetalPrices) {
        val s = _uiState.value
        val hasInputs = s.goldWeight.isNotBlank() || s.silverWeight.isNotBlank() || s.cashAmount.isNotBlank()
        if (hasInputs) publishResult(s, prices, isUsingFallback = false)
    }

    private fun publishResult(s: ZakatUiState, prices: MetalPrices, isUsingFallback: Boolean) {
        val goldW = s.goldWeight.toDoubleOrNull() ?: 0.0
        val silverW = s.silverWeight.toDoubleOrNull() ?: 0.0
        val cashV = s.cashAmount.toDoubleOrNull() ?: 0.0
        val result = calculateZakatUseCase(goldW, silverW, cashV, s.selectedCurrency, prices)
        _uiState.value = _uiState.value.copy(result = result, isUsingFallback = isUsingFallback)
    }
}
```

- [x] **Step 4: Remove the now-dead per-metal methods**

`domain/.../repository/MetalPriceRepository.kt` — delete `getGoldPricePerGram()` and `getSilverPricePerGram()`; the interface keeps only:

```kotlin
interface MetalPriceRepository {
    suspend fun fetchMetalPrices(): Result<MetalPrices, AppError>
    fun getCachedMetalPrices(): MetalPrices?
}
```

`data/.../repository/MetalPriceRepositoryImpl.kt` — delete `getGoldPricePerGram()`, `getSilverPricePerGram()`, and their `retryIO` wrappers; keep `@Volatile cachedPrices`, `fetchMetalPrices()`, `getCachedMetalPrices()`.

`data/src/test/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImplTest.kt` — delete the `getGoldPricePerGram returns converted price` and `getSilverPricePerGram returns converted price` tests.

- [x] **Step 5: Run the full unit test suite**

Run: `.\gradlew testDebug`
Expected: PASS (domain, data, app tests all green).

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorViewModel.kt app/src/test/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorViewModelTest.kt domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/MetalPriceRepository.kt data/src/main/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImpl.kt data/src/test/kotlin/com/smiledev/rafiq/data/repository/MetalPriceRepositoryImplTest.kt
git commit -m "feat(zakat): instant result with background price refresh"
```

---

### Task 5: App — UI without spinner, with fallback note

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorScreen.kt`

**Interfaces:**
- Consumes: `ZakatUiState.isUsingFallback` (Task 4).

- [x] **Step 1: Remove the spinner/error branch and add the fallback note**

In `app/src/main/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorScreen.kt`:

1. Delete these imports (now unused): `androidx.compose.material3.CircularProgressIndicator`, `androidx.compose.ui.semantics.contentDescription`, `androidx.compose.ui.semantics.semantics`, `com.smiledev.rafiq.core.displayMessage`.
2. Replace the `when` block (currently lines 140-192):

```kotlin
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading" })
                state.error != null -> Text(state.error!!.displayMessage, color = MaterialTheme.colorScheme.error)
                else -> {
```

…with:

```kotlin
            if (state.isUsingFallback) {
                Text(
                    text = "Using last-known price — refreshing…",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
```

3. Delete the trailing `}` of the removed `else` branch (keep the Card's closing structure). The final structure of the bottom section should be:

```kotlin
            if (state.isUsingFallback) {
                Text(
                    text = "Using last-known price — refreshing…",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            val r = state.result
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Zakat Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    if (r.goldZakat > 0) {
                        Text("Gold Zakat: ${formatVal(r.goldZakat)}")
                    } else {
                        Text("Gold: Below nisab (85g)")
                    }
                    if (r.silverZakat > 0) {
                        Text("Silver Zakat: ${formatVal(r.silverZakat)}")
                    } else {
                        Text("Silver: Below nisab (595g)")
                    }
                    if (r.cashZakat > 0) {
                        Text("Cash Zakat: ${formatVal(r.cashZakat)}")
                    } else {
                        Text("Cash: Below nisab threshold")
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Total Zakat Due: ${formatVal(r.totalZakat)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF009688)
                    )
                    if (r.goldPricePerGram > 0) {
                        Text(
                            text = "Gold price: ${formatVal(r.goldPricePerGram)}/g | Silver: ${formatVal(r.silverPricePerGram)}/g",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
```

- [x] **Step 2: Build the app**

Run: `.\gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (removed imports are not referenced).

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorScreen.kt
git commit -m "feat(zakat): always render result, show fallback-price note"
```

---

### Task 6: Full verification

- [x] **Step 1: Run all unit tests**

Run: `.\gradlew testDebug`
Expected: PASS.

- [x] **Step 2: Build the debug APK**

Run: `.\gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [x] **Step 3: Optional — smoke test on emulator**

```powershell
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```

Open Zakat calculator, tap Calculate Zakat: the result card should appear immediately (with the "Using last-known price — refreshing…" note on first run) and update in place when fresh prices arrive.

- [x] **Step 4: Update progress**

Check off every completed step in this plan file. Commit the plan file checkboxes if this plan was committed.

---

## Self-Review

**Spec coverage:**
- Instant result with defaults/cache — Task 4 (ViewModel).
- Parallel fetch — Task 2 (repository).
- In-memory cache only — Task 2.
- Prefetch on `init` — Task 4.
- Dedicated 5s-timeout metals client — Task 3.
- `isUsingFallback` note + no spinner — Task 5.
- `error` field removed from UI state — Task 4.
- Pure `CalculateZakatUseCase` + domain `ZakatResult` + delete app duplicate — Tasks 1 & 4.
- Remove per-metal repo methods — Task 4.
- Tests for all layers — Tasks 1, 2, 4.
- Out of scope respected: exchange rate untouched (Task 1 keeps `16000.0`), no DataStore (in-memory cache only), shared 30s client untouched for other APIs (Task 3 adds a separate client).

**Placeholder scan:** No TBD/TODO; every step has concrete code or exact commands.

**Type consistency:** `MetalPrices(goldPricePerGram, silverPricePerGram)` used identically across Tasks 1-4. `fetchMetalPrices(): Result<MetalPrices, AppError>` and `getCachedMetalPrices(): MetalPrices?` match between Tasks 2 and 4. `CalculateZakatUseCase.invoke(goldWeight, silverWeight, cashValue, currency, prices): ZakatResult` matches between Tasks 1 and 4. `isUsingFallback: Boolean` matches between Tasks 4 and 5.
