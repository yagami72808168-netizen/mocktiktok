package com.light.mimictiktok.ui.import

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.light.mimictiktok.data.db.AppDatabase
import com.light.mimictiktok.data.repository.MediaRepository
import com.light.mimictiktok.data.repository.VideoRepository
import com.light.mimictiktok.util.MediaPicker
import com.light.mimictiktok.util.PermissionUtil
import com.light.mimictiktok.util.PlaylistImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing media import operations.
 */
class PlaylistImportViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val videoRepository = VideoRepository(database.appDao())
    private val mediaRepository = MediaRepository(application, videoRepository)
    private val playlistImporter = PlaylistImporter(application, videoRepository)

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _selectedVideos = MutableStateFlow<List<Uri>>(emptyList())
    val selectedVideos: StateFlow<List<Uri>> = _selectedVideos.asStateFlow()

    private val _importProgress = MutableStateFlow<ImportProgress>(ImportProgress(0, 0))
    val importProgress: StateFlow<ImportProgress> = _importProgress.asStateFlow()

    /**
     * Represents the state of the import operation.
     */
    sealed class ImportState {
        object Idle : ImportState()
        object Loading : ImportState()
        data class Success(val message: String) : ImportState()
        data class Error(val message: String) : ImportState()
        data class PartialSuccess(val message: String) : ImportState()
    }

    /**
     * Represents import progress.
     */
    data class ImportProgress(
        val current: Int,
        val total: Int
    ) {
        val percentage: Int
            get() = if (total > 0) (current * 100) / total else 0
    }

    /**
     * Checks if required permissions are granted.
     */
    fun hasPermissions(): Boolean {
        return PermissionUtil.isReadMediaGranted(getApplication())
    }

    /**
     * Gets the required permissions array.
     */
    fun getRequiredPermissions(): Array<String> {
        return PermissionUtil.getRequiredPermissions()
    }

    /**
     * Handles selected video URIs from the picker.
     */
    fun onVideosSelected(uris: List<Uri>) {
        if (uris.isEmpty()) {
            _importState.value = ImportState.Error("No videos selected")
            return
        }

        // Take persistable permissions
        MediaPicker.takePersistablePermissions(getApplication(), uris)

        _selectedVideos.value = uris
        _importState.value = ImportState.Idle
    }

    /**
     * Validates selected videos.
     */
    fun validateSelectedVideos() {
        viewModelScope.launch {
            val videos = _selectedVideos.value
            if (videos.isEmpty()) {
                _importState.value = ImportState.Error("No videos selected")
                return@launch
            }

            _importState.value = ImportState.Loading

            val (valid, invalid) = playlistImporter.validateVideos(videos)

            when {
                valid.isEmpty() -> {
                    _importState.value = ImportState.Error(
                        "All selected files are invalid"
                    )
                }
                invalid.isNotEmpty() -> {
                    _importState.value = ImportState.PartialSuccess(
                        "${valid.size} valid videos found, ${invalid.size} invalid"
                    )
                    _selectedVideos.value = valid
                }
                else -> {
                    _importState.value = ImportState.Success(
                        "All ${valid.size} videos are valid"
                    )
                }
            }
        }
    }

    /**
     * Imports selected videos into a playlist.
     */
    fun importVideos(playlistName: String = "Imported Videos") {
        viewModelScope.launch {
            val videos = _selectedVideos.value
            if (videos.isEmpty()) {
                _importState.value = ImportState.Error("No videos to import")
                return@launch
            }

            _importState.value = ImportState.Loading
            _importProgress.value = ImportProgress(0, videos.size)

            val result = playlistImporter.importVideos(
                uris = videos,
                playlistName = playlistName,
                onProgress = { current, total ->
                    _importProgress.value = ImportProgress(current, total)
                }
            )

            when (result) {
                is PlaylistImporter.ImportResult.Success -> {
                    _importState.value = ImportState.Success(
                        "Successfully imported ${result.importedCount} videos"
                    )
                    _selectedVideos.value = emptyList()
                }
                is PlaylistImporter.ImportResult.PartialSuccess -> {
                    _importState.value = ImportState.PartialSuccess(
                        "Imported ${result.importedCount} videos, ${result.failedCount} failed"
                    )
                }
                is PlaylistImporter.ImportResult.Failure -> {
                    _importState.value = ImportState.Error(result.error)
                }
            }
        }
    }

    /**
     * Scans and imports all videos from MediaStore.
     */
    fun scanAndImportAllVideos() {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            _importProgress.value = ImportProgress(0, 0)

            try {
                mediaRepository.scanAndImportAllVideos { current, total ->
                    _importProgress.value = ImportProgress(current, total)
                }.collect { importedCount ->
                    if (importedCount > 0) {
                        _importState.value = ImportState.Success(
                            "Successfully imported $importedCount videos from MediaStore"
                        )
                    } else {
                        _importState.value = ImportState.Error(
                            "No videos found in MediaStore"
                        )
                    }
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error(
                    "Failed to scan videos: ${e.message}"
                )
            }
        }
    }

    /**
     * Clears selected videos.
     */
    fun clearSelection() {
        _selectedVideos.value = emptyList()
        _importState.value = ImportState.Idle
        _importProgress.value = ImportProgress(0, 0)
    }

    /**
     * Resets the import state.
     */
    fun resetState() {
        _importState.value = ImportState.Idle
        _importProgress.value = ImportProgress(0, 0)
    }
}
