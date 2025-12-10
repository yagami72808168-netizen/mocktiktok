package com.light.mimictiktok.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.light.mimictiktok.data.db.VideoEntity
import com.light.mimictiktok.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: VideoRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val videos: StateFlow<List<VideoEntity>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadVideos()
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

    fun refreshVideos() {
        loadVideos()
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
