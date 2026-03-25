package com.example.cornell

import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import com.example.cornell.api.GptRepository

class CornellApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)
        // Cargar token desde assets automáticamente
        GptRepository.loadTokenFromAssets(this)
    }
}
