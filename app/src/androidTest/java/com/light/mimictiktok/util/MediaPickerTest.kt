package com.light.mimictiktok.util

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaPickerTest {

    @Test
    fun createPickerIntent_shouldCreateIntentWithCorrectProperties() {
        val intent = MediaPicker.createPickerIntent()

        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        assertEquals("video/*", intent.type)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertTrue(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
    }

    @Test
    fun createSinglePickerIntent_shouldCreateIntentWithoutMultipleSelection() {
        val intent = MediaPicker.createSinglePickerIntent()

        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        assertEquals("video/*", intent.type)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertFalse(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
    }

    @Test
    fun extractUrisFromResult_shouldReturnEmptyListForNullIntent() {
        val uris = MediaPicker.extractUrisFromResult(null)

        assertTrue(uris.isEmpty())
    }

    @Test
    fun extractUrisFromResult_shouldExtractSingleURIFromData() {
        val testUri = Uri.parse("content://test/video1")
        val intent = Intent().apply {
            data = testUri
        }

        val uris = MediaPicker.extractUrisFromResult(intent)

        assertEquals(1, uris.size)
        assertEquals(testUri, uris[0])
    }
}
