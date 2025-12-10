# MIMIC-3 Implementation Guide: Media Picker & Playlist Import

## Overview
This document describes the implementation of media file selection and playlist import functionality for the mimictiktok project.

## Architecture

### Components Created

#### 1. **MediaPicker** (`util/MediaPicker.kt`)
Utility class for selecting video files using Storage Access Framework (SAF).

**Features:**
- SAF-based file selection (works with Android 13+ scoped storage)
- Batch video selection support
- Single video selection support
- URI validation
- Persistable permission handling
- Custom ActivityResultContracts for integration with Activity/Fragment

**Key Methods:**
```kotlin
fun createPickerIntent(): Intent
fun extractUrisFromResult(data: Intent?): List<Uri>
fun takePersistablePermissions(context: Context, uris: List<Uri>)
fun isValidVideoUri(context: Context, uri: Uri): Boolean
```

#### 2. **PlaylistImporter** (`util/PlaylistImporter.kt`)
Handles importing video files into the database with metadata extraction.

**Features:**
- Batch video import with progress tracking
- Single video import
- Video validation
- Metadata extraction using MediaScanUtil
- Automatic playlist creation
- Error handling with detailed results

**Import Results:**
- `Success`: All videos imported successfully
- `PartialSuccess`: Some videos imported, some failed
- `Failure`: Import operation failed completely

**Key Methods:**
```kotlin
suspend fun importVideos(
    uris: List<Uri>,
    playlistName: String,
    playlistId: String?,
    onProgress: ((current: Int, total: Int) -> Unit)?
): ImportResult

suspend fun importSingleVideo(uri: Uri, playlistId: String?): VideoEntity?
suspend fun validateVideos(uris: List<Uri>): Pair<List<Uri>, List<Uri>>
```

#### 3. **MediaRepository** (`data/repository/MediaRepository.kt`)
Repository layer for media scanning and import operations.

**Features:**
- Scan all videos from MediaStore
- Import videos from MediaStore into database
- Get video metadata from URI
- Check if video already imported
- Flow-based reactive updates

**Key Methods:**
```kotlin
fun scanAllVideos(): Flow<List<MediaStoreVideo>>
suspend fun importMediaStoreVideos(videos: List<MediaStoreVideo>, onProgress: ((Int, Int) -> Unit)?): Int
fun scanAndImportAllVideos(onProgress: ((Int, Int) -> Unit)?): Flow<Int>
suspend fun getVideoMetadataFromUri(uri: Uri): VideoEntity?
suspend fun isVideoImported(uri: Uri): Boolean
```

#### 4. **PlaylistImportViewModel** (`ui/import/PlaylistImportViewModel.kt`)
ViewModel managing import operations and UI state.

**State Management:**
- `ImportState`: Idle, Loading, Success, Error, PartialSuccess
- `ImportProgress`: Current/Total with percentage calculation
- Selected videos tracking

**Key Methods:**
```kotlin
fun hasPermissions(): Boolean
fun getRequiredPermissions(): Array<String>
fun onVideosSelected(uris: List<Uri>)
fun validateSelectedVideos()
fun importVideos(playlistName: String)
fun scanAndImportAllVideos()
fun clearSelection()
fun resetState()
```

#### 5. **PlaylistImportFragment** (`ui/import/PlaylistImportFragment.kt`)
Compose-based UI for importing videos.

**Features:**
- Material 3 design
- Permission handling with activity result API
- Video picker integration
- Progress display with LinearProgressIndicator
- Selected videos list
- Playlist name dialog
- Error/success message display

**UI Components:**
- Select Videos button
- Scan All button
- Selected videos list
- Import FAB
- Progress indicators
- Status cards (success/error/partial)

#### 6. **PlaylistImportActivity** (`ui/import/PlaylistImportActivity.kt`)
Standalone activity hosting the import fragment.

## Permissions Handling

The implementation properly handles Android version-specific permissions:

**Android 13+ (API 33+):**
- `READ_MEDIA_VIDEO` permission

**Android 12 and below:**
- `READ_EXTERNAL_STORAGE` permission

All permission logic is encapsulated in `PermissionUtil` (from MIMIC-7).

## Storage Access Framework (SAF) Integration

The implementation uses SAF for file selection, which:
- Works with scoped storage on Android 10+
- Doesn't require storage permissions for user-selected files
- Provides persistent URI permissions
- Works across all storage locations (internal, external, cloud)

## Usage Examples

### Basic Import Flow

1. User opens `PlaylistImportActivity`
2. User clicks "Select Videos"
3. System checks permissions, requests if needed
4. System launches SAF file picker
5. User selects videos (single or multiple)
6. System validates selected videos
7. User confirms with playlist name
8. System imports videos with progress updates
9. System shows success/error message

### Programmatic Usage

```kotlin
// Create importer
val importer = PlaylistImporter(context, videoRepository)

// Import videos
viewModelScope.launch {
    val result = importer.importVideos(
        uris = selectedUris,
        playlistName = "My Videos",
        onProgress = { current, total ->
            println("Progress: $current/$total")
        }
    )
    
    when (result) {
        is PlaylistImporter.ImportResult.Success -> {
            println("Imported ${result.importedCount} videos")
        }
        is PlaylistImporter.ImportResult.PartialSuccess -> {
            println("Imported ${result.importedCount}, failed ${result.failedCount}")
        }
        is PlaylistImporter.ImportResult.Failure -> {
            println("Failed: ${result.error}")
        }
    }
}
```

### MediaStore Scanning

```kotlin
// Scan and import all videos from MediaStore
viewModel.scanAndImportAllVideos()

// Or manually
mediaRepository.scanAllVideos().collect { videos ->
    println("Found ${videos.size} videos")
    mediaRepository.importMediaStoreVideos(videos) { current, total ->
        println("Importing: $current/$total")
    }
}
```

## Testing

### Unit Tests
- `MediaPickerTest.kt`: Tests for MediaPicker utility methods
  - Intent creation
  - URI extraction
  - Validation

### Integration Tests
- `PlaylistImporterTest.kt`: Tests for PlaylistImporter
  - Import operations
  - Validation
  - Error handling
  
- `MediaRepositoryTest.kt`: Tests for MediaRepository
  - MediaStore scanning
  - Import operations
  - URI validation

## Dependencies Added

- `androidx.fragment:fragment-ktx:1.6.2`

## Files Created

1. `app/src/main/java/com/light/mimictiktok/util/MediaPicker.kt`
2. `app/src/main/java/com/light/mimictiktok/util/PlaylistImporter.kt`
3. `app/src/main/java/com/light/mimictiktok/data/repository/MediaRepository.kt`
4. `app/src/main/java/com/light/mimictiktok/ui/import/PlaylistImportViewModel.kt`
5. `app/src/main/java/com/light/mimictiktok/ui/import/PlaylistImportFragment.kt`
6. `app/src/main/java/com/light/mimictiktok/ui/import/PlaylistImportActivity.kt`

## Files Modified

1. `app/src/main/AndroidManifest.xml` - Added PlaylistImportActivity
2. `app/src/main/java/com/light/mimictiktok/di/AppContainer.kt` - Added MediaRepository and PlaylistImporter
3. `gradle/libs.versions.toml` - Added fragment dependency
4. `app/build.gradle.kts` - Added fragment implementation

## Test Files Created

1. `app/src/test/java/com/light/mimictiktok/util/MediaPickerTest.kt`
2. `app/src/androidTest/java/com/light/mimictiktok/util/PlaylistImporterTest.kt`
3. `app/src/androidTest/java/com/light/mimictiktok/data/repository/MediaRepositoryTest.kt`

## Acceptance Criteria Status

✅ **Can correctly select local video files**
- MediaPicker supports SAF-based file selection
- Supports both single and batch selection
- Works across all Android versions

✅ **Can correctly import media files to database**
- PlaylistImporter extracts metadata and saves to database
- Supports batch import with progress tracking
- Handles errors gracefully

✅ **Permissions handling is correct**
- Uses PermissionUtil for version-specific permissions
- Properly requests and checks permissions
- SAF provides permission-free file access for selected files

✅ **File path validation is effective**
- MediaPicker.isValidVideoUri validates URIs
- PlaylistImporter.validateVideos checks all files
- MediaScanUtil extracts and validates metadata

✅ **UI displays import progress**
- Progress indicators show current/total
- Linear progress bar shows percentage
- Status cards show success/error messages
- Selected videos displayed in list

## Future Enhancements

1. **Thumbnail Generation**: Integrate with ThumbnailUtil to generate thumbnails during import
2. **Duplicate Detection**: Check for duplicate videos before import
3. **Import History**: Track import sessions and allow rollback
4. **Cloud Storage**: Support importing from cloud storage providers
5. **Batch Operations**: Support bulk edit operations on imported videos
6. **Import Presets**: Save import configurations for quick reuse

## Notes

- All imports happen on IO dispatcher for non-blocking operation
- Progress callbacks run on IO thread, should be collected on Main thread
- URI permissions are persistent across app restarts
- MediaStore scanning requires storage permission
- SAF selection does not require storage permission
