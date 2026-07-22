package com.smiledev.rafiq.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "rafiq_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PRAYER_CALCULATION_METHOD = intPreferencesKey("prayer_calculation_method")
        val LATITUDE = stringPreferencesKey("latitude")
        val LONGITUDE = stringPreferencesKey("longitude")
        val CITY_NAME = stringPreferencesKey("city_name")
        val LAST_SELECTED_RECITER = intPreferencesKey("last_selected_reciter")
        val PRAYER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("prayer_notifications_enabled")
        val TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
        val AYAH_FONT_SIZE = intPreferencesKey("ayah_font_size")
        val TRANSLATION_FONT_SIZE = intPreferencesKey("translation_font_size")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    val prayerCalculationMethod: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PRAYER_CALCULATION_METHOD] ?: 2
    }

    val latitude: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LATITUDE] ?: ""
    }

    val longitude: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LONGITUDE] ?: ""
    }

    val cityName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CITY_NAME] ?: ""
    }

    val lastSelectedReciter: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LAST_SELECTED_RECITER] ?: 1
    }

    val prayerNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PRAYER_NOTIFICATIONS_ENABLED] ?: true
    }

    val translationLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TRANSLATION_LANGUAGE] ?: "system"
    }

    val ayahFontSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[AYAH_FONT_SIZE] ?: 22
    }

    val translationFontSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TRANSLATION_FONT_SIZE] ?: 15
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }

    suspend fun setPrayerCalculationMethod(method: Int) {
        context.dataStore.edit { prefs -> prefs[PRAYER_CALCULATION_METHOD] = method }
    }

    suspend fun setLatitude(lat: String) {
        context.dataStore.edit { prefs -> prefs[LATITUDE] = lat }
    }

    suspend fun setLongitude(lng: String) {
        context.dataStore.edit { prefs -> prefs[LONGITUDE] = lng }
    }

    suspend fun setCityName(name: String) {
        context.dataStore.edit { prefs -> prefs[CITY_NAME] = name }
    }

    suspend fun setLastSelectedReciter(reciterId: Int) {
        context.dataStore.edit { prefs -> prefs[LAST_SELECTED_RECITER] = reciterId }
    }

    suspend fun setPrayerNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PRAYER_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setTranslationLanguage(lang: String) {
        context.dataStore.edit { prefs -> prefs[TRANSLATION_LANGUAGE] = lang }
    }

    suspend fun setAyahFontSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[AYAH_FONT_SIZE] = size }
    }

    suspend fun setTranslationFontSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[TRANSLATION_FONT_SIZE] = size }
    }
}
