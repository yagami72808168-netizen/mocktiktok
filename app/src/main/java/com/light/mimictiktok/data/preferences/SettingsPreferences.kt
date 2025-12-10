package com.light.mimictiktok.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsPreferences(private val context: Context) {

    companion object {
        val VIDEO_QUALITY_KEY = stringPreferencesKey("video_quality")
        val CACHE_SIZE_LIMIT_KEY = longPreferencesKey("cache_size_limit")
        
        const val QUALITY_HIGH = "High"
        const val QUALITY_MEDIUM = "Medium"
        const val QUALITY_LOW = "Low"
        
        const val DEFAULT_CACHE_SIZE = 500L // MB
    }

    val videoQuality: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[VIDEO_QUALITY_KEY] ?: QUALITY_HIGH
        }

    val cacheSizeLimit: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[CACHE_SIZE_LIMIT_KEY] ?: DEFAULT_CACHE_SIZE
        }

    suspend fun setVideoQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[VIDEO_QUALITY_KEY] = quality
        }
    }

    suspend fun setCacheSizeLimit(limitMb: Long) {
        context.dataStore.edit { preferences ->
            preferences[CACHE_SIZE_LIMIT_KEY] = limitMb
        }
    }
}
