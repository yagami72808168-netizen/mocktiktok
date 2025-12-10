package com.light.mimictiktok.ui.import

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit

/**
 * Activity that hosts the PlaylistImportFragment.
 * This provides a standalone entry point for importing videos.
 */
class PlaylistImportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, PlaylistImportFragment())
            }
        }
    }
}
