# Rafiq Android — Agent Guidance

## Build & Run

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew assembleDebug
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```

Emulator: `Medium_Phone_API_35`. No lint, typecheck, or test commands currently set up.

## Architecture Quirks

- **Navigation3** — type-safe nav via `@Serializable data object/class` NavKey tokens in `NavigationKeys.kt`. All 15 routes use `entryProvider { entry<Key> { ... } }` pattern in `Navigation.kt`.
- **Hilt (KAPT)** — NOT KSP. Uses `kapt` plugin with `correctErrorTypes=true`. The warning "Kapt currently doesn't support language version 2.0+. Falling back to 1.9." is harmless.
- **Kotlin 2.0.21** pinned because Hilt 2.56.2 embedded `kotlin-metadata-jvm` maxes at 2.0.0.
- **AGP 8.9.2** — Hilt 2.56.2 requires AGP 8.x, NOT 9.x.
- **Material Icons: core only** — `material-icons-core`, never use extended set. Available: `DateRange`, `Face`, `Favorite`, `List` (AutoMirrored), `LocationOn`, `Notifications`, `Person`, `Place`, `PlayArrow`, `Refresh`, `ShoppingCart`, `Star`.
- **DataStore** — all user prefs via `PreferencesManager`, NOT SharedPreferences.
- **Room** — singleton `getInstance()` pattern, `fallbackToDestructiveMigration()`.

## Asset Loading Gotchas

- **Translation DB names** contain `/` (e.g. `translations/en.sahih.db`). `DatabaseCopier` flattens to `_` before calling `getDatabasePath()` because Android rejects path separators in DB names.
- **Arabic font**: load via `FontFamily(Font(R.font.me_quran))`. Never use `fontResource()`.
- **Quran DB** (`quran-uthmani.db`) has all TEXT columns. Bismillah is a nullable `String?` stored as actual Arabic text.

## Screen Patterns

- Every screen is a `@Composable fun XxxScreen(onBack: () -> Unit, viewModel: XxxViewModel = hiltViewModel(), modifier)` called from `Navigation.kt`.
- Back nav: `{ backStack.removeLastOrNull() }` or `Text("Back", Modifier.clickable(onClick = onBack).padding(16.dp))` in TopAppBar.
- ViewModels: `@HiltViewModel` constructor injection, `MutableStateFlow<XxxUiState>` + `val uiState: StateFlow<XxxUiState>`.
- Data loaded on `init` via `viewModelScope.launch(Dispatchers.IO)`.
- Locale: `if (Locale.getDefault().language == "id") "id" else "en"`.

## API Endpoints

- **Aladhan** (`https://api.aladhan.com/`): `v1/timings/{date}?latitude=&longitude=&method=20`. Default coords: Jakarta (-6.2088, 106.8456).
- **Metals.live** (`https://api.metals.live/`): `v1/spot/gold` and `v1/spot/silver`. Prices in USD/oz, converted to per-gram.

## OsmDroid

Requires init before MapView creation:
```kotlin
Configuration.getInstance().apply {
    userAgentValue = context.packageName
    osmdroidBasePath = context.cacheDir
    osmdroidTileCache = context.cacheDir.resolve("tiles")
}
```

## File Structure

- `data/models/` — data classes (DTOs)
- `data/repository/` — reads from assets or APIs
- `ui/<feature>/` — Screen.kt + ViewModel.kt per feature
- `theme/` — Material3 colors/theme
- `di/AppModule.kt` — all Hilt `@Provides` bindings
- `core/DatabaseCopier.kt` — copies assets DBs to device
- `service/` — Media3 foreground service, WorkManager worker
- `thoughts/` — plan docs, NOT source code

## Key Constraints

- Never add `material-icons-extended` dependency.
- Never replace KAPT with KSP without checking Hilt/Room max supported metadata version.
- Never use Java `Math.*` — use `kotlin.math.*`.
- Compose `textDirection` is a `TextStyle` property, not a direct `Text` param.
- `Icons.Filled.Delete` is available in `material-icons-core` only if imported explicitly.
- `PrayerLogScreen` uses `Switch` for toggling each of the 5 daily prayers; `BookmarkListScreen` uses `Favorite` icon for the dashboard card.
- Theme palette (`Color.kt`) uses Islamic-inspired colors: Teal500, Gold700, DeepBlue, WarmBrown, Cream/Sand backgrounds. On Android 12+, dynamic color takes priority.
