package com.light.mimictiktok.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.light.mimictiktok.data.preferences.SettingsPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsPreferences = SettingsPreferences(application)

    val videoQuality: StateFlow<String> = settingsPreferences.videoQuality
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsPreferences.QUALITY_HIGH
        )

    val cacheSizeLimit: StateFlow<Long> = settingsPreferences.cacheSizeLimit
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsPreferences.DEFAULT_CACHE_SIZE
        )

    fun setVideoQuality(quality: String) {
        viewModelScope.launch {
            settingsPreferences.setVideoQuality(quality)
        }
    }

    fun setCacheSizeLimit(limitMb: Long) {
        viewModelScope.launch {
            settingsPreferences.setCacheSizeLimit(limitMb)
        }
    }
}
