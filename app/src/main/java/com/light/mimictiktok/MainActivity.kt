package com.light.mimictiktok

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.light.mimictiktok.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, HomeFragment.newInstance())
                .commit()
        }
    }
}