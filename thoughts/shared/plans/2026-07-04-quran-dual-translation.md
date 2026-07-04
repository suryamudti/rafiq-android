# Quran Dual Translation Implementation Plan

This plan details the steps to fetch and display Bahasa Indonesia, English, or both translations based on a user preference setting in the Settings screen.

## Decisions Made (User Review)

> [!IMPORTANT]
> - **Settings Toggle:** We will add a "Quran Translation" setting to the settings screen. Users can choose between:
>   - **System default:** Follows device locale (ID for Indonesian locale, EN for others).
>   - **Bahasa Indonesia:** Always display Bahasa Indonesia translation.
>   - **English:** Always display English translation.
>   - **Both (Bahasa & English):** Display both translations stacked under each verse with distinct visual badges.
> - **Performance & Instant updates:** The repository layer will always query both translation databases and populate `translationId` and `translationEn` fields on the `AyahData` model. The UI will instantly switch layout based on the active setting, preventing any database reload delays.

---

## Proposed Changes

### Data Layer & Preferences

#### [MODIFY] [PreferencesManager.kt](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq/data/preferences/PreferencesManager.kt)
- Add preference key `TRANSLATION_LANGUAGE` as a string preference.
- Expose `translationLanguage: Flow<String>` (defaults to `"system"`).
- Add `suspend fun setTranslationLanguage(lang: String)`.

#### [MODIFY] [AyahData.kt](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq/data/models/AyahData.kt)
- Add new fields:
  - `translationId: String? = null` (Bahasa Indonesia translation)
  - `translationEn: String? = null` (English translation)
- Keep existing `translation: String? = null` for backward compatibility.

#### [MODIFY] [QuranRepository.kt](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq/data/repository/QuranRepository.kt)
- Cache databases for both languages:
  ```kotlin
  private var translationIdDb: SQLiteDatabase? = null
  private var translationEnDb: SQLiteDatabase? = null
  ```
- Update `getTranslationDatabase(localeCode: String)` to retrieve, initialize, and cache both databases separately without closing one another.
- Update `getAyahsWithTranslation` to:
  - Fetch Indonesian translation: `getTranslationForSura(suraNumber, "id")`
  - Fetch English translation: `getTranslationForSura(suraNumber, "en")`
  - Enrich `AyahData` with both translations (`translationId`, `translationEn`).
  - Keep `translation` populated with the current system locale's translation.

### ViewModels

#### [MODIFY] [QuranViewModel.kt](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq/ui/quran/QuranViewModel.kt)
- Add `translationLanguage` to `QuranUiState`.
- Collect `preferencesManager.translationLanguage` in `init` block and update the UI state.

#### [MODIFY] [SettingsViewModel.kt](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq/ui/settings/SettingsViewModel.kt)
- Add `translationLanguage` to `SettingsUiState` (defaults to `"system"`).
- Collect `preferencesManager.translationLanguage` in `init` block and update the state.
- Add `fun setTranslationLanguage(lang: String)` which launches a coroutine to update the preference.

### UI Screens

#### [MODIFY] [SettingsScreen.kt](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq/ui/settings/SettingsScreen.kt)
- Add a new "Quran Translation" settings group.
- Render options using radio buttons:
  - System default
  - Bahasa Indonesia
  - English
  - Both (Bahasa & English)

#### [MODIFY] [AyahScreen.kt](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq/ui/quran/AyahScreen.kt)
- Pass the active `translationLanguage` from `QuranUiState` to `VerseCell`.
- Update `VerseCell` to determine what translations to display:
  - If language setting is `"both"`, render both ID and EN stacked with colored badges.
  - If language setting is `"id"`, display the Indonesian translation.
  - If language setting is `"en"`, display the English translation.
  - If language setting is `"system"`, fallback to Indonesian for Indonesian locale, English for other locales.

---

## Verification Plan

### Automated Tests
- [x] Run full compile check and unit tests:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  .\gradlew testDebug
  ```

### Manual Verification
- Deploy to the emulator:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
  ```
- Navigate to the Settings tab, select "English" translation, go back to a Surah, and verify that translations are shown in English.
- Go back to Settings, select "Bahasa Indonesia", verify translations update to Indonesian.
- Select "Both (Bahasa & English)", verify both translations are displayed stacked with "ID" and "EN" labels.
