package com.app.musicplayer

import android.app.Application
import com.app.musicplayer.database.MusicDatabase
import com.app.musicplayer.database.MusicRepository

class MusicPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MusicRepository.initialize(this)
    }
}