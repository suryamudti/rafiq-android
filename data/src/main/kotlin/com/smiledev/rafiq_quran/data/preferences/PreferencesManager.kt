package com.smiledev.rafiq_quran.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class CachedPrayerTimes(
    val date: String,
    val latitude: Double,
    val longitude: Double,
    val calculationMethod: Int,
    val data: PrayerTimesData
) {
    fun isValidFor(reqDate: String, reqLat: Double, reqLon: Double, reqMethod: Int): Boolean {
        return date == reqDate &&
            calculationMethod == reqMethod &&
            abs(latitude - reqLat) < 0.01 &&
            abs(longitude - reqLon) < 0.01
    }
}

private val Context.dataStore by preferencesDataStore(name = "rafiq_settings")

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
        /** Key for the set of favorite Asmaul Husna IDs as a string set. */
        val FAVORITE_ASMAUL_HUSNA_IDS = stringSetPreferencesKey("favorite_asmaul_husna_ids")
        /** Key for the Story font size in sp. */
        val STORY_FONT_SIZE = intPreferencesKey("story_font_size")
        /** Key for cached prayer times date. */
        val PRAYER_CACHE_DATE = stringPreferencesKey("prayer_cache_date")
        /** Key for cached prayer times latitude. */
        val PRAYER_CACHE_LAT = stringPreferencesKey("prayer_cache_lat")
        /** Key for cached prayer times longitude. */
        val PRAYER_CACHE_LON = stringPreferencesKey("prayer_cache_lon")
        /** Key for cached prayer times calculation method. */
        val PRAYER_CACHE_METHOD = intPreferencesKey("prayer_cache_method")
        /** Key for cached prayer times JSON payload. */
        val PRAYER_CACHE_DATA = stringPreferencesKey("prayer_cache_data")
        /** Key for the selected Tasbih dhikr ID. */
        val TASBIH_SELECTED_ID = intPreferencesKey("tasbih_selected_id")
        /** Key for the current Tasbih count. */
        val TASBIH_COUNT = intPreferencesKey("tasbih_count")
        /** Key for the current Tasbih lap/round. */
        val TASBIH_LAP = intPreferencesKey("tasbih_lap")
        /** Key for the total Tasbih count in the session. */
        val TASBIH_TOTAL = intPreferencesKey("tasbih_total")
        /** Key for the Tasbih target count. */
        val TASBIH_TARGET = intPreferencesKey("tasbih_target")
        /** Key for whether Tasbih vibration is enabled. */
        val TASBIH_VIBRATION = booleanPreferencesKey("tasbih_vibration")
        /** Key for whether Tasbih sound is enabled. */
        val TASBIH_SOUND = booleanPreferencesKey("tasbih_sound")
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
     * Returns the set of favorite Asmaul Husna IDs as a [Flow].
     * Returns an empty set if not set. IDs are parsed from strings.
     */
    val favoriteAsmaulHusnaIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[FAVORITE_ASMAUL_HUSNA_IDS].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    /**
     * Returns the Story font size as a [Flow].
     * Defaults to 16 sp if not set.
     */
    val storyFontSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[STORY_FONT_SIZE] ?: 16
    }

    /**
     * Returns the selected Tasbih dhikr ID as a [Flow].
     * Defaults to 1 if not set.
     */
    val tasbihSelectedId: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TASBIH_SELECTED_ID] ?: 1
    }

    /**
     * Returns the current Tasbih count as a [Flow].
     * Defaults to 0 if not set.
     */
    val tasbihCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TASBIH_COUNT] ?: 0
    }

    /**
     * Returns the current Tasbih lap as a [Flow].
     * Defaults to 1 if not set.
     */
    val tasbihLap: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TASBIH_LAP] ?: 1
    }

    /**
     * Returns the total Tasbih count as a [Flow].
     * Defaults to 0 if not set.
     */
    val tasbihTotal: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TASBIH_TOTAL] ?: 0
    }

    /**
     * Returns the Tasbih target count as a [Flow].
     * Defaults to 33 if not set.
     */
    val tasbihTarget: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TASBIH_TARGET] ?: 33
    }

    /**
     * Returns whether Tasbih vibration is enabled as a [Flow].
     * Defaults to true if not set.
     */
    val tasbihVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TASBIH_VIBRATION] ?: true
    }

    /**
     * Returns whether Tasbih click sound is enabled as a [Flow].
     * Defaults to false if not set.
     */
    val tasbihSoundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TASBIH_SOUND] ?: false
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
     * Toggles an Asmaul Husna ID in the favorite Asmaul Husna IDs set.
     * If the ID is already in the set, it is removed; otherwise it is added.
     *
     * @param id the Asmaul Husna ID to toggle
     */
    suspend fun toggleFavoriteAsmaulHusna(id: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_ASMAUL_HUSNA_IDS].orEmpty()
            val updated = if (id.toString() in current) current - id.toString() else current + id.toString()
            prefs[FAVORITE_ASMAUL_HUSNA_IDS] = updated
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

    private val gson = Gson()

    /**
     * Retrieves the cached prayer times if present in DataStore.
     *
     * @return [CachedPrayerTimes] or null if not cached or malformed
     */
    suspend fun getCachedPrayerTimes(): CachedPrayerTimes? {
        val prefs = context.dataStore.data.first()
        val date = prefs[PRAYER_CACHE_DATE] ?: return null
        val lat = prefs[PRAYER_CACHE_LAT]?.toDoubleOrNull() ?: return null
        val lon = prefs[PRAYER_CACHE_LON]?.toDoubleOrNull() ?: return null
        val method = prefs[PRAYER_CACHE_METHOD] ?: return null
        val json = prefs[PRAYER_CACHE_DATA] ?: return null
        val data = runCatching { gson.fromJson(json, PrayerTimesData::class.java) }.getOrNull() ?: return null
        return CachedPrayerTimes(date, lat, lon, method, data)
    }

    /**
     * Persists prayer times for a given date, coordinates, and calculation method.
     */
    suspend fun saveCachedPrayerTimes(
        date: String,
        lat: Double,
        lon: Double,
        method: Int,
        data: PrayerTimesData
    ) {
        val json = gson.toJson(data)
        context.dataStore.edit { prefs ->
            prefs[PRAYER_CACHE_DATE] = date
            prefs[PRAYER_CACHE_LAT] = lat.toString()
            prefs[PRAYER_CACHE_LON] = lon.toString()
            prefs[PRAYER_CACHE_METHOD] = method
            prefs[PRAYER_CACHE_DATA] = json
        }
    }

    /**
     * Clears cached prayer times.
     */
    suspend fun clearCachedPrayerTimes() {
        context.dataStore.edit { prefs ->
            prefs.remove(PRAYER_CACHE_DATE)
            prefs.remove(PRAYER_CACHE_LAT)
            prefs.remove(PRAYER_CACHE_LON)
            prefs.remove(PRAYER_CACHE_METHOD)
            prefs.remove(PRAYER_CACHE_DATA)
        }
    }

    /**
     * Sets the selected Tasbih dhikr ID.
     */
    suspend fun setTasbihSelectedId(id: Int) {
        context.dataStore.edit { prefs -> prefs[TASBIH_SELECTED_ID] = id }
    }

    /**
     * Sets the current Tasbih count.
     */
    suspend fun setTasbihCount(count: Int) {
        context.dataStore.edit { prefs -> prefs[TASBIH_COUNT] = count }
    }

    /**
     * Sets the current Tasbih lap count.
     */
    suspend fun setTasbihLap(lap: Int) {
        context.dataStore.edit { prefs -> prefs[TASBIH_LAP] = lap }
    }

    /**
     * Sets the total Tasbih count in the session.
     */
    suspend fun setTasbihTotal(total: Int) {
        context.dataStore.edit { prefs -> prefs[TASBIH_TOTAL] = total }
    }

    /**
     * Sets the Tasbih target count.
     */
    suspend fun setTasbihTarget(target: Int) {
        context.dataStore.edit { prefs -> prefs[TASBIH_TARGET] = target }
    }

    /**
     * Sets whether Tasbih vibration is enabled.
     */
    suspend fun setTasbihVibration(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[TASBIH_VIBRATION] = enabled }
    }

    /**
     * Sets whether Tasbih click sound is enabled.
     */
    suspend fun setTasbihSound(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[TASBIH_SOUND] = enabled }
    }

    /**
     * Updates the full Tasbih session counts (count, lap, total).
     */
    suspend fun updateTasbihSession(count: Int, lap: Int, total: Int) {
        context.dataStore.edit { prefs ->
            prefs[TASBIH_COUNT] = count
            prefs[TASBIH_LAP] = lap
            prefs[TASBIH_TOTAL] = total
        }
    }

    /**
     * Resets the Tasbih count. If [resetAll] is true, also resets lap to 1 and total to 0.
     */
    suspend fun resetTasbih(resetAll: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs[TASBIH_COUNT] = 0
            if (resetAll) {
                prefs[TASBIH_LAP] = 1
                prefs[TASBIH_TOTAL] = 0
            }
        }
    }
}
