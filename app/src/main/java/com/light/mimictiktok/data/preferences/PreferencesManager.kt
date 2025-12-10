package com.light.mimictiktok.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("app_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val INITIAL_POSITION_KEY = intPreferencesKey("initial_position")
        private const val DEFAULT_VIRTTUAL_POSITION = Int.MAX_VALUE / 2
    }

    val initialPosition: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[INITIAL_POSITION_KEY] ?: DEFAULT_VIRTTUAL_POSITION
        }

    suspend fun setInitialPosition(position: Int) {
        context.dataStore.edit { preferences ->
            preferences[INITIAL_POSITION_KEY] = position
        }
    }

    fun getDefaultVirtualPosition(dataSize: Int): Int {
        return if (dataSize > 0) {
            DEFAULT_VIRTTUAL_POSITION - (DEFAULT_VIRTTUAL_POSITION % dataSize)
        } else {
            DEFAULT_VIRTTUAL_POSITION
        }
    }
}