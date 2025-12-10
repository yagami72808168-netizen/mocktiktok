package com.light.mimictiktok.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import com.light.mimictiktok.data.repository.PlaybackRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackControlViewModel(
    private val playbackRepository: PlaybackRepository
) : ViewModel() {
    
    data class PlaybackState(
        val isPlaying: Boolean = false,
        val currentPositionMs: Long = 0,
        val totalDurationMs: Long = 0,
        val bufferPositionMs: Long = 0,
        val playbackSpeed: Float = 1.0f,
        val videoId: String? = null
    )
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private var progressUpdateJob: Job? = null
    private var currentPlayer: ExoPlayer? = null
    
    fun attachPlayer(player: ExoPlayer?, videoId: String?) {
        currentPlayer = player
        if (player == null) {
            stopProgressUpdates()
            return
        }
        
        _playbackState.value = _playbackState.value.copy(videoId = videoId)
        
        // Start progress updates
        startProgressUpdates(player)
        
        // Load saved progress if available
        videoId?.let { loadPlaybackProgress(it) }
    }
    
    fun detachPlayer() {
        currentPlayer?.let { player ->
            // Save current progress before detaching
            saveCurrentProgress()
            currentPlayer = null
        }
        stopProgressUpdates()
    }
    
    fun togglePlayPause() {
        currentPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }
    
    fun seekTo(progress: Float) {
        currentPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0) {
                val targetPosition = (duration * progress).toLong()
                player.seekTo(targetPosition)
                
                // Update state immediately for smooth UI feedback
                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = targetPosition
                )
            }
        }
    }
    
    fun fastForward(seconds: Int = 10) {
        currentPlayer?.let { player ->
            val newPosition = player.currentPosition + (seconds * 1000)
            val duration = player.duration
            player.seekTo(newPosition.coerceIn(0, duration))
        }
    }
    
    fun rewind(seconds: Int = 10) {
        currentPlayer?.let { player ->
            val newPosition = player.currentPosition - (seconds * 1000)
            player.seekTo(newPosition.coerceIn(0, player.duration))
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        currentPlayer?.let { player ->
            player.setPlaybackSpeed(speed)
            _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
            
            // Save speed preference
            viewModelScope.launch {
                _playbackState.value.videoId?.let { videoId ->
                    val currentState = _playbackState.value
                    playbackRepository.savePlaybackProgress(
                        videoId = videoId,
                        progressMs = currentState.currentPositionMs,
                        durationMs = currentState.totalDurationMs,
                        playSpeed = speed
                    )
                }
            }
        }
    }
    
    private fun startProgressUpdates(player: ExoPlayer) {
        stopProgressUpdates()
        
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                updatePlaybackState(player)
                delay(100) // Update every 100ms for smooth progress
            }
        }
    }
    
    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }
    
    private fun updatePlaybackState(player: ExoPlayer) {
        val state = _playbackState.value
        
        _playbackState.value = state.copy(
            isPlaying = player.isPlaying,
            currentPositionMs = player.currentPosition,
            totalDurationMs = if (player.duration > 0) player.duration else state.totalDurationMs,
            bufferPositionMs = player.bufferedPosition,
            playbackSpeed = player.playbackParameters.speed
        )
    }
    
    private fun loadPlaybackProgress(videoId: String) {
        viewModelScope.launch {
            val progress = playbackRepository.getPlaybackProgress(videoId)
            progress?.let {
                _playbackState.value = _playbackState.value.copy(
                    playbackSpeed = it.playSpeed
                )
                
                currentPlayer?.let { player ->
                    // Resume from saved position (but not from beginning)
                    if (it.progressMs > 0 && it.progressMs < it.durationMs - 5000) {
                        player.seekTo(it.progressMs)
                    }
                    // Set saved playback speed
                    player.setPlaybackSpeed(it.playSpeed)
                }
            }
        }
    }
    
    fun saveCurrentProgress() {
        val currentState = _playbackState.value
        currentState.videoId?.let { videoId ->
            if (currentState.totalDurationMs > 0) {
                viewModelScope.launch {
                    playbackRepository.savePlaybackProgress(
                        videoId = videoId,
                        progressMs = currentState.currentPositionMs,
                        durationMs = currentState.totalDurationMs,
                        playSpeed = currentState.playbackSpeed
                    )
                }
            }
        }
    }
    
    fun seekToPrevious() {
        // This would be implemented in the adapter/fragment level
        // For now, implement basic seek to previous
        currentPlayer?.let { player ->
            val newPosition = (player.currentPosition - (player.duration * 0.1)).toLong()
            player.seekTo(newPosition.coerceIn(0, player.duration))
        }
    }
    
    fun seekToNext() {
        // This would be implemented in the adapter/fragment level
        // For now, implement basic seek forward
        currentPlayer?.let { player ->
            val newPosition = (player.currentPosition + (player.duration * 0.1)).toLong()
            player.seekTo(newPosition.coerceIn(0, player.duration))
        }
    }
    
    override fun onCleared() {
        saveCurrentProgress()
        stopProgressUpdates()
        super.onCleared()
    }
}