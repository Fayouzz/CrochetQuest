package com.crochetquest.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Lightweight user preferences (DataStore). */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore
    private val animationsKey = booleanPreferencesKey("animations_enabled")

    val animationsEnabled: Flow<Boolean> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[animationsKey] ?: true }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        store.edit { it[animationsKey] = enabled }
    }
}
