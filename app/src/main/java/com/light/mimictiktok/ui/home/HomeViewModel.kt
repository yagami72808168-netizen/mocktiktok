package com.light.mimictiktok.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.light.mimictiktok.data.db.VideoEntity
import com.light.mimictiktok.data.preferences.PreferencesManager
import com.light.mimictiktok.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: VideoRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val videos: StateFlow<List<VideoEntity>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _startPosition = MutableStateFlow<Int?>(null)
    val startPosition: StateFlow<Int?> = _startPosition.asStateFlow()

    init {
        loadVideos()
        observePreferences()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllVideosFlow().collect { videoList ->
                    _videos.value = videoList
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.initialPosition.collect { position ->
                _startPosition.value = position
            }
        }
    }

    fun refreshVideos() {
        loadVideos()
    }

    suspend fun setInitialVideoPosition(videoId: String?) {
        if (videoId == null) return

        _videos.value.indexOfFirst { it.id == videoId }.takeIf { it >= 0 }?.let { realIndex ->
            _videos.value.size.takeIf { it > 0 }?.let { dataSize ->
                val virtualPosition = preferencesManager.getDefaultVirtualPosition(dataSize)
                preferencesManager.setInitialPosition(virtualPosition + realIndex)
            }
        }
    }

    fun toggleFavorite(videoId: String) {
        viewModelScope.launch {
            val video = _videos.value.find { it.id == videoId }
            video?.let {
                repository.updateVideo(it.copy(isFavorite = !it.isFavorite))
            }
        }
    }

    fun incrementLikeCount(videoId: String) {
        viewModelScope.launch {
            val video = _videos.value.find { it.id == videoId }
            video?.let {
                repository.updateVideo(it.copy(likeCount = it.likeCount + 1))
            }
        }
    }
}
