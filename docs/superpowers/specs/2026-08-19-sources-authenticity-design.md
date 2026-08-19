# Sources & Authenticity Screen — Design

**Date:** 2026-08-19
**Branch:** `feat/source-authenticity`
**Status:** Approved

## 1. Overview

Add a **Sources & Authenticity** screen to Rafiq that documents the provenance of the
three core data types the app displays: **Quran**, **Hadith**, and **Prayer Times**.
The screen is purely informational — it tells the user *what* each source is, *why* it
is authentic (shahih), who translated it, and where to find the original online.

Scope is **browse + read only**. No data-layer changes, no new permissions, no network
calls. The screen is static content rendered from a small in-file data structure.

## 2. Content Model

Defined as private data classes inside `SourcesScreen.kt`:

```kotlin
private data class SourceItem(
    val title: String,          // resource-backed string
    val description: String,    // resource-backed string
    val authenticity: String,   // resource-backed string, e.g. "Authentic collection"
    val translator: String?,    // optional translator credit (null hides the row)
    val linkUrl: String?,       // optional external URL (null hides the link row)
)

private data class SourceSection(
    val title: String,          // resource-backed string
    val items: List<SourceItem>,
)
```

Three sections — Quran (3 items), Hadith (2 items), Prayer Times (2 items):

### Quran
| Item | Description | Authenticity | Translator | Link |
|---|---|---|---|---|
| Arabic text (Uthmani) | Standard Uthmani script, bundled `quran-uthmani.db` | Authentic Quranic text | — | quran.com |
| English translation | Bundled `translations/en.sahih.db` | Recognized translation | Saheeh International | quran.com |
| Indonesian translation | Bundled `translations/id.indonesian.db` | Official government translation | Indonesian Ministry of Religious Affairs (Kemenag) | quran.kemenag.go.id |

### Hadith
| Item | Description | Authenticity | Translator | Link |
|---|---|---|---|---|
| Sahih al-Bukhari | Arabic + EN + ID, bundled `hadith.db` | Authentic collection (Shahih) | EN: Muhsin Khan | sunnah.com |
| Sahih Muslim | Arabic + EN + ID, bundled `hadith.db` | Authentic collection (Shahih) | EN: Abdul Hamid Siddiqui | sunnah.com |

### Prayer Times
| Item | Description | Authenticity | Translator | Link |
|---|---|---|---|---|
| Aladhan API | `v1/timings/{date}` with calculation method 20 | Method 20 = KEMENAG standard | — | aladhan.com |
| Default location | Jakarta (-6.2088, 106.8456) | Used when no location saved | — | — |

## 3. Architecture & Navigation

Follows the established screen pattern:

1. **NavKey** — add `@Serializable data object Sources : NavKey` to
   `NavigationKeys.kt` (mirrors `Settings` at `NavigationKeys.kt:26`).
2. **Nav entry** — add `entry<Sources> { SourcesScreen(onBack = ...) }` to
   `Navigation.kt` (mirrors `entry<Settings>` at `Navigation.kt:186-192`).
3. **Settings menu item** — in `SettingsScreen.kt`, add a "Sources & Authenticity"
   row styled like `MoreFeatureItem` (`SettingsScreen.kt:131-148`) that calls the
   existing `onNavigate(Sources)` — no signature change needed.
4. **Screen** — new `app/src/main/java/com/smiledev/rafiq_quran/ui/sources/SourcesScreen.kt`:
   `@Composable fun SourcesScreen(onBack: () -> Unit, modifier: Modifier = Modifier)`.
   No ViewModel — content is static.

## 4. Screen Layout

- **TopAppBar** with "Back" using existing `R.string.back` (pattern from
  `SettingsScreen.kt:58`).
- **Scrollable `Column`**: a section header per section, then one card per item.
- Each item card shows: title, authenticity label, description, optional translator
  credit, and (when `linkUrl != null`) a link row.
- **Link handling**: `context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))`
  matching `QiblaScreen.kt:200-201`, wrapped in `try/catch` for
  `ActivityNotFoundException` with a `Toast` fallback.
- Visual styling reuses the Settings screen patterns and theme colors
  (`Color.kt` palette; Android 12+ dynamic color takes priority).

## 5. Internationalization

All user-visible strings added to **both** resource files (they must stay parallel):

- `app/src/main/res/values/strings.xml` (English)
- `app/src/main/res/values-id/strings.xml` (Indonesian)

New keys: screen title, "Sources & Authenticity" menu label, three section titles,
per-item titles/descriptions/authenticity labels, translator credits, and the
"Open source" link label. Indonesian translations: "Shahih" for authentic,
"Kemenag" for the ministry.

## 6. Error Handling

- No data loading, so no loading/error states.
- Link open failure → `Toast` ("Could not open link"), app continues.
- `linkUrl == null` simply hides the link row.

## 7. Testing

- Static content; no new unit or instrumented tests required.
- Verification: `.\gradlew assembleDebug` must pass (JAVA_HOME =
  `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`).
- Manual check: open Settings → Sources & Authenticity, verify EN/ID labels and that
  each link opens the browser.