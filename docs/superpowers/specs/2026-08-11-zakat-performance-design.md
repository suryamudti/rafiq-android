# Zakat Calculator Performance Design

Date: 2026-08-11

## Problem

Pressing "Calculate Zakat" blocks on two **sequential** network calls to
`api.metals.live` (gold, then silver). Combined with `retryIO` (up to 3 attempts
with backoff, retryIO.kt:5) and 30s OkHttp connect/read timeouts
(AppModule.kt:131-132), a slow or flaky metals API makes the zakat result feel
interminable. There is no caching, so every `calculate()` re-fetches, and
nothing is prefetched when the screen opens.

## Goal

- Show the zakat result **instantly** when the user taps Calculate.
- Refresh with fresh prices **in the background**; recompute when they arrive.
- Prefetch prices on screen open so they are usually cached by the time the
  user calculates.
- Failures never block the UI. When no fresh price is available, compute with
  fallback defaults and show a small "using last-known/default price" note.

## Approach

Instant-result-first with background refresh, using an **in-memory** cache
(no DataStore persistence).

## Design

### 1. Domain layer (`:domain`)

- New data class `MetalPrices(goldPricePerGram: Double, silverPricePerGram: Double)`.
- `MetalPriceRepository` interface:
  - `suspend fun fetchMetalPrices(): Result<MetalPrices, AppError>` — fetches
    gold + silver **in parallel**, caches the result in memory, returns it.
  - `fun getCachedMetalPrices(): MetalPrices?` — returns the in-memory cache.
  - Remove `getGoldPricePerGram()` and `getSilverPricePerGram()` (only the
    zakat use case and ViewModel call them; both migrate).
- `CalculateZakatUseCase` becomes a **pure** function. It no longer fetches:
  - `operator fun invoke(goldWeight: Double, silverWeight: Double,
    cashValue: Double, currency: String, prices: MetalPrices): ZakatResult`
  - Returns `ZakatResult` directly (no `Result` wrapper; no IO).
  - Delete the duplicated `ZakatResult` data class in the app module's
    `ZakatCalculatorViewModel.kt`; the domain one is canonical.

### 2. Data layer (`:data`)

- `MetalPriceRepositoryImpl`:
  - `fetchMetalPrices()` uses `coroutineScope { async { ... } + async { ... } }`
    calling the data-remote `MetalPriceApi.getGoldPricePerGram()` and
    `MetalPriceApi.getSilverPricePerGram()` (those API-level methods **stay**;
    only the repository interface methods are removed) for parallel fetch,
    writes into a `@Volatile var cache: MetalPrices?`, and returns the result.
  - `getCachedMetalPrices()` returns `cache`.
  - Wrap the fetch in `retryIO(times = 2)` with shorter delays
    (e.g. initialDelay 50, maxDelay 300) so background failures stay snappy.
- **Dedicated OkHttpClient for `api.metals.live`** with ~5s connect/read
  timeouts instead of the shared 30s client in `AppModule.kt`.

### 3. ViewModel (`:app`, `ZakatCalculatorViewModel`)

- `calculate()`:
  1. Computes immediately with `getCachedMetalPrices() ?: defaults`
     (defaults: gold 65.0/g, silver 0.75/g — the same values MetalPriceApi
     already uses for empty responses). No spinner.
  2. Launches a background `fetchMetalPrices()`; on success it updates the
     cache and **recomputes + refreshes the result** with the fresh prices.
- `init`: launches a background prefetch so prices are usually cached by the
  time the user calculates.
- New `isUsingFallback: Boolean` in `ZakatUiState`; true when the current
  result was computed from defaults (no cache yet), false after a refresh
  lands.
- The `error: AppError?` field is removed from `ZakatUiState` (nothing displays
  it; background refresh failures only keep `isUsingFallback` true).
- The duplicate `ZakatResult` data class is removed; the ViewModel uses the
  domain one via `CalculateZakatUseCase`.

### 4. UI (`ZakatCalculatorScreen.kt`)

- Remove the `isLoading` spinner branch; the result card always renders.
- Show a small grey note, e.g. "Using last-known price — refreshing…", when
  `isUsingFallback` is true.
- Because a result always renders (computed from cache or defaults), the
  `error` field is no longer displayed in the result area. Background refresh
  failures only keep `isUsingFallback` true; they are not surfaced as errors.

### 5. Tests

- `CalculateZakatUseCaseTest` (`:domain`): pure math assertions, prices passed
  in; drop the repository-mock failure cases.
- `MetalPriceRepositoryImplTest` (`:data`): parallel fetch populates cache;
  `getCachedMetalPrices()` returns it; error path returns `Result.Error`.
- `ZakatCalculatorViewModelTest` (`:app`): instant result from defaults/cache;
  background refresh updates result and clears `isUsingFallback`.

## Out of Scope

- The hardcoded `16000` IDR exchange rate (correctness, not speed).
- Persisting prices via DataStore (in-memory cache only, per decision).
- Any changes to non-zakat features or the shared 30s OkHttp client used by
  other APIs.

## Risks / Notes

- Results can be slightly stale while the background refresh is in flight; the
  fallback note communicates this.
- Removing the per-metal repository methods touches tests; all are updated in
  the same change.
- `api.metals.live` remains the source of truth; the dedicated timeouts only
  cap how long we wait before falling back.
