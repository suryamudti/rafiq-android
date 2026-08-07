# Conventions & Constraints

- **Hilt via KAPT, NOT KSP.** `kapt` plugin with `correctErrorTypes=true`. The
  warning "Kapt currently doesn't support language version 2.0+. Falling back
  to 1.9." is harmless.
- **Kotlin 2.0.0, AGP 8.9.2, Hilt 2.56.2.** Hilt 2.56.2 requires AGP 8.x.
- **Material icons: core only.** `material-icons-core` — NEVER add
  `material-icons-extended`. Available: DateRange, Face, Favorite, List
  (AutoMirrored), LocationOn, Notifications, Person, Place, PlayArrow, Refresh,
  ShoppingCart, Star. `Icons.Filled.Delete` requires an explicit import.
- **DataStore, not SharedPreferences.** All user prefs via `PreferencesManager`
  (`data/src/main/kotlin/com/smiledev/rafiq/data/preferences/PreferencesManager.kt`).
- **Room** singleton `getInstance()` pattern, `fallbackToDestructiveMigration()`.
  In `:data` module, use `api(libs.room.runtime)` (not `implementation`) so `:app`
  can see the `RoomDatabase` supertype transitively.
- **Cross-module smart casts** don't work on nullable types from other modules.
  Use `!!` (if guarded) or `?:` / local `val`.
- **No Java `Math.*`** — use `kotlin.math.*`.
- **Compose** `textDirection` is a `TextStyle` property, not a direct `Text` param.
- **Locale:** `if (Locale.getDefault().language == "id") "id" else "en"` (see
  `core/.../LocaleUtil.kt`, which also treats `"in"` as Indonesian).
- Theme palette (`app/.../theme/Color.kt`) uses Islamic-inspired colors: Teal500,
  Gold700, DeepBlue, WarmBrown, Cream/Sand. On Android 12+, dynamic color wins.
