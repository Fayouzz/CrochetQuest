package com.crochetquest.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode(val label: String) {
    SYSTEM("Match my phone"),
    LIGHT("Light"),
    NIGHT("Cozy night");

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** Everything the player can set, plus the running session timer (so it survives the app closing). */
data class AppSettings(
    val animationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** When the running session timer was started, or null if no timer is running. */
    val timerStartedAt: Long? = null
)

/** Lightweight user preferences (DataStore). */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore
    private val animationsKey = booleanPreferencesKey("animations_enabled")
    private val soundKey = booleanPreferencesKey("sound_enabled")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val timerKey = longPreferencesKey("timer_started_at")

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                animationsEnabled = prefs[animationsKey] ?: true,
                soundEnabled = prefs[soundKey] ?: true,
                themeMode = ThemeMode.fromName(prefs[themeKey]),
                timerStartedAt = prefs[timerKey]
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        store.edit { it[animationsKey] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        store.edit { it[soundKey] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.name }
    }

    suspend fun startTimer(atMillis: Long) {
        store.edit { it[timerKey] = atMillis }
    }

    suspend fun clearTimer() {
        store.edit { it.remove(timerKey) }
    }
}
