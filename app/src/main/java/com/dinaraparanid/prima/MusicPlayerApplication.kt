package com.dinaraparanid.prima

import android.app.Application
import com.dinaraparanid.prima.database.MusicRepository

class MusicPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MusicRepository.initialize(this)
    }
}