# Architecture

Multi-module Gradle project (`settings.gradle.kts`). Four modules:

| Module | Path | Responsibility |
|---|---|---|
| :core | core/src/main/kotlin/com/smiledev/rafiq/core/ | Result.kt, AppError.kt, DispatcherProvider.kt, retryIO.kt, LocaleUtil.kt |
| :domain | domain/src/main/kotlin/com/smiledev/rafiq/domain/ | Repository interfaces, use cases, domain models (Surah, Ayah, PrayerTimings, Mosque, ...) |
| :data | data/src/main/kotlin/com/smiledev/rafiq/data/ | Repository Impls, Room DBs/DAOs, Retrofit APIs, PreferencesManager (DataStore). Note: DatabaseCopier.kt lives here too, but in package `com.smiledev.rafiq.core` (file `data/src/main/kotlin/com/smiledev/rafiq/core/DatabaseCopier.kt`). |
| :app | app/src/main/java/com/smiledev/rafiq/ | DI (di/AppModule.kt), UI (ui/<feature>/Screen.kt + ViewModel.kt), theme/, service/, Navigation.kt |

## Navigation3 (type-safe, 16 routes)

- `app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt`: every route is a
  `@Serializable` `data object`/`data class : NavKey` (e.g. `Dashboard`,
  `Quran(initialTab)`, `Ayah(suraNumber, suraName, scrollToAya)`).
- `app/src/main/java/com/smiledev/rafiq/Navigation.kt`: `MainNavigation()`
  builds `rememberNavBackStack(Dashboard)`, a `NavDisplay` with
  `entryProvider { entry<Key> { XxxScreen(...) } }` per route. Back is
  `backStack.removeLastOrNull()`; forward is `backStack.add(navKey)`.
- To add a screen: (1) add a `NavKey` token, (2) add `entry<Key> { ... }` to
  the provider, (3) create `ui/<feature>/XxxScreen.kt` + `XxxViewModel.kt`.

## MVVM screen pattern

- `@Composable fun XxxScreen(onBack: () -> Unit, viewModel: XxxViewModel = hiltViewModel(), modifier)`.
- `@HiltViewModel class XxxViewModel @Inject constructor(...)` with
  `MutableStateFlow<XxxUiState>` + `val uiState: StateFlow<XxxUiState>`.
- Data loads on `init` via `viewModelScope.launch(Dispatchers.IO)`.
- `XxxUiState` is typically a `data class` with `isLoading`, `error`, and data.

## DI

- `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt`: `@Module`
  `@InstallIn(SingletonComponent::class)` with `@Binds` for repository impls
  (`bindPrayerTimesRepository()`, `bindQuranRepository()`, etc.) and `@Provides`
  for Retrofit services (AladhanApiService, EQuranApiService, MetalPriceApiService,
  OverpassApiService, IslamicAppApiService), OkHttpClient, and Room DBs.
- Cross-module smart casts from nullable do NOT work; use `!!`/`?:`/local `val`.
