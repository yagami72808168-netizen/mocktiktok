package com.light.mimictiktok.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Utility object for picking video files using Storage Access Framework (SAF).
 * Supports batch selection of video files with proper Android 13+ scoped storage handling.
 */
object MediaPicker {
    /**
     * Creates an Intent for picking multiple video files using SAF.
     * This works across all Android versions and respects scoped storage requirements.
     *
     * @return Intent configured for multiple video selection
     */
    fun createPickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*"))
        }
    }

    /**
     * Creates an Intent for picking a single video file using SAF.
     *
     * @return Intent configured for single video selection
     */
    fun createSinglePickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*"))
        }
    }

    /**
     * Extracts URIs from the picker result Intent.
     *
     * @param data The Intent returned from the picker activity
     * @return List of selected video URIs
     */
    fun extractUrisFromResult(data: Intent?): List<Uri> {
        if (data == null) return emptyList()

        val uris = mutableListOf<Uri>()

        // Handle multiple selection
        data.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i)?.uri?.let { uri ->
                    uris.add(uri)
                }
            }
        }

        // Handle single selection
        if (uris.isEmpty()) {
            data.data?.let { uri ->
                uris.add(uri)
            }
        }

        return uris
    }

    /**
     * Takes persistent read permission for the given URIs.
     * This is necessary for accessing files selected through SAF across app restarts.
     *
     * @param context Application context
     * @param uris List of URIs to take permission for
     */
    fun takePersistablePermissions(context: Context, uris: List<Uri>) {
        val contentResolver = context.contentResolver
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: SecurityException) {
                // Permission might already be taken or not available
                e.printStackTrace()
            }
        }
    }

    /**
     * Validates if the given URI points to a valid video file.
     *
     * @param context Application context
     * @param uri URI to validate
     * @return true if the URI is valid and accessible
     */
    fun isValidVideoUri(context: Context, uri: Uri): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            mimeType?.startsWith("video/") == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * ActivityResultContract for picking multiple videos.
     * Use this with registerForActivityResult in Activities/Fragments.
     */
    class PickMultipleVideos : ActivityResultContracts.OpenMultipleDocuments() {
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return super.createIntent(context, arrayOf("video/*")).apply {
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*"))
            }
        }
    }

    /**
     * ActivityResultContract for picking a single video.
     * Use this with registerForActivityResult in Activities/Fragments.
     */
    class PickSingleVideo : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return super.createIntent(context, arrayOf("video/*")).apply {
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*"))
            }
        }
    }
}
