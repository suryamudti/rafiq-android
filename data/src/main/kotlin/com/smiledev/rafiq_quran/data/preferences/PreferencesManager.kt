package com.smiledev.rafiq_quran.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-based preferences manager for the Rafiq Quran application.
 * Provides observable [Flow]s for all user preferences with typed accessors
 * and validation for numeric inputs (latitude/longitude).
 *
 * Preferences are stored in the "rafiq_settings" DataStore and survive app restarts.
 * Default values are applied when a preference has not been explicitly set.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Key for the theme mode preference ("system", "light", or "dark"). */
        val THEME_MODE = stringPreferencesKey("theme_mode")
        /** Key for the prayer calculation method (integer index matching calculated methods). */
        val PRAYER_CALCULATION_METHOD = intPreferencesKey("prayer_calculation_method")
        /** Key for the user's latitude coordinate as a string. */
        val LATITUDE = stringPreferencesKey("latitude")
        /** Key for the user's longitude coordinate as a string. */
        val LONGITUDE = stringPreferencesKey("longitude")
        /** Key for the user's city name. */
        val CITY_NAME = stringPreferencesKey("city_name")
        /** Key for the last selected reciter ID. */
        val LAST_SELECTED_RECITER = intPreferencesKey("last_selected_reciter")
        /** Key for whether prayer notifications are enabled. */
        val PRAYER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("prayer_notifications_enabled")
        /** Key for the translation language code ("id", "en", etc.). */
        val TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
        /** Key for the Ayah font size in sp. */
        val AYAH_FONT_SIZE = intPreferencesKey("ayah_font_size")
        /** Key for the Translation font size in sp. */
        val TRANSLATION_FONT_SIZE = intPreferencesKey("translation_font_size")
        /** Key for the last read Surah number. */
        val LAST_READ_SURA = intPreferencesKey("last_read_sura")
        /** Key for the last read Ayah number. */
        val LAST_READ_AYA = intPreferencesKey("last_read_aya")
        /** Key for the set of favorite Prophet IDs as a string set. */
        val FAVORITE_PROPHET_IDS = stringSetPreferencesKey("favorite_prophet_ids")
        /** Key for the Story font size in sp. */
        val STORY_FONT_SIZE = intPreferencesKey("story_font_size")
    }

    /**
     * Returns the current theme mode as a [Flow].
     * Defaults to "system" if not set.
     */
    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    /**
     * Returns the prayer calculation method as a [Flow].
     * Defaults to index 2 (the standard/primary method) if not set.
     */
    val prayerCalculationMethod: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PRAYER_CALCULATION_METHOD] ?: 2
    }

    /**
     * Returns the user's latitude as a [Flow].
     * Defaults to "" if not set. Input validation is performed via [setLatitude].
     */
    val latitude: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LATITUDE] ?: ""
    }

    /**
     * Returns the user's longitude as a [Flow].
     * Defaults to "" if not set. Input validation is performed via [setLongitude].
     */
    val longitude: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LONGITUDE] ?: ""
    }

    /**
     * Returns the user's city name as a [Flow].
     * Defaults to "" if not set. Characters are sanitized via [setCityName].
     */
    val cityName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CITY_NAME] ?: ""
    }

    /**
     * Returns the last selected reciter ID as a [Flow].
     * Defaults to 1 if not set.
     */
    val lastSelectedReciter: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LAST_SELECTED_RECITER] ?: 1
    }

    /**
     * Returns whether prayer notifications are enabled as a [Flow].
     * Defaults to true if not set.
     */
    val prayerNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PRAYER_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * Returns the translation language as a [Flow].
     * Defaults to "system" if not set.
     */
    val translationLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TRANSLATION_LANGUAGE] ?: "system"
    }

    /**
     * Returns the Ayah font size as a [Flow].
     * Defaults to 22 sp if not set.
     */
    val ayahFontSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[AYAH_FONT_SIZE] ?: 22
    }

    /**
     * Returns the Translation font size as a [Flow].
     * Defaults to 15 sp if not set.
     */
    val translationFontSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TRANSLATION_FONT_SIZE] ?: 15
    }

    /**
     * Returns the last read Surah number as a [Flow].
     * Defaults to 0 if not set.
     */
    val lastReadSura: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LAST_READ_SURA] ?: 0
    }

    /**
     * Returns the last read Ayah number as a [Flow].
     * Defaults to 0 if not set.
     */
    val lastReadAya: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LAST_READ_AYA] ?: 0
    }

    /**
     * Returns the set of favorite Prophet IDs as a [Flow].
     * Returns an empty set if not set. IDs are parsed from strings.
     */
    val favoriteProphetIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[FAVORITE_PROPHET_IDS].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    /**
     * Returns the Story font size as a [Flow].
     * Defaults to 16 sp if not set.
     */
    val storyFontSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[STORY_FONT_SIZE] ?: 16
    }

    /**
     * Sets the theme mode preference.
     *
     * @param mode the theme mode: "system", "light", or "dark"
     */
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }

    /**
     * Sets the prayer calculation method.
     *
     * @param method the integer index of the calculation method
     */
    suspend fun setPrayerCalculationMethod(method: Int) {
        context.dataStore.edit { prefs -> prefs[PRAYER_CALCULATION_METHOD] = method }
    }

    /**
     * Sets the user's latitude after validating the format.
     * Accepts coordinates in the format "-DD.D" (e.g., "-6.2088").
     * Silently ignores invalid formats.
     *
     * @param lat the latitude string to validate and store
     */
    suspend fun setLatitude(lat: String) {
        if (!lat.matches(Regex("^-?\\d{1,3}\\.\\d+$"))) return
        context.dataStore.edit { prefs -> prefs[LATITUDE] = lat }
    }

    /**
     * Sets the user's longitude after validating the format.
     * Accepts coordinates in the format "-DD.D" (e.g., "106.8456").
     * Silently ignores invalid formats.
     *
     * @param lng the longitude string to validate and store
     */
    suspend fun setLongitude(lng: String) {
        if (!lng.matches(Regex("^-?\\d{1,3}\\.\\d+$"))) return
        context.dataStore.edit { prefs -> prefs[LONGITUDE] = lng }
    }

    /**
     * Sets the user's city name, sanitizing to printable ASCII characters.
     * Filters to characters with codes in the range 32..126 (space through tilde).
     *
     * @param name the city name to store (will be sanitized)
     */
    suspend fun setCityName(name: String) {
        val sanitized = name.trim().filter { it.code in 32..126 }
        context.dataStore.edit { prefs -> prefs[CITY_NAME] = sanitized }
    }

    /**
     * Sets the last selected reciter ID.
     *
     * @param reciterId the ID of the reciter to store
     */
    suspend fun setLastSelectedReciter(reciterId: Int) {
        context.dataStore.edit { prefs -> prefs[LAST_SELECTED_RECITER] = reciterId }
    }

    /**
     * Sets whether prayer notifications are enabled.
     *
     * @param enabled true to enable notifications, false to disable
     */
    suspend fun setPrayerNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PRAYER_NOTIFICATIONS_ENABLED] = enabled }
    }

    /**
     * Sets the translation language.
     *
     * @param lang the language code (e.g., "id" for Indonesian, "en" for English)
     */
    suspend fun setTranslationLanguage(lang: String) {
        context.dataStore.edit { prefs -> prefs[TRANSLATION_LANGUAGE] = lang }
    }

    /**
     * Sets the Ayah font size.
     *
     * @param size the font size in sp
     */
    suspend fun setAyahFontSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[AYAH_FONT_SIZE] = size }
    }

    /**
     * Sets the Translation font size.
     *
     * @param size the font size in sp
     */
    suspend fun setTranslationFontSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[TRANSLATION_FONT_SIZE] = size }
    }

    /**
     * Sets the last read position (Surah and Ayah).
     *
     * @param sura the Surah number
     * @param aya the Ayah number
     */
    suspend fun setLastReadPosition(sura: Int, aya: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_READ_SURA] = sura
            prefs[LAST_READ_AYA] = aya
        }
    }

    /**
     * Toggles a prophet ID in the favorite prophet IDs set.
     * If the ID is already in the set, it is removed; otherwise it is added.
     *
     * @param id the prophet ID to toggle
     */
    suspend fun toggleFavoriteProphet(id: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_PROPHET_IDS].orEmpty()
            val updated = if (id.toString() in current) current - id.toString() else current + id.toString()
            prefs[FAVORITE_PROPHET_IDS] = updated
        }
    }

    /**
     * Sets the Story font size.
     *
     * @param size the font size in sp
     */
    suspend fun setStoryFontSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[STORY_FONT_SIZE] = size }
    }
}
