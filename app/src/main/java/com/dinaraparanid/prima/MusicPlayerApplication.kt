package com.dinaraparanid.prima

import android.app.Application
import android.media.MediaPlayer

class MusicPlayerApplication : Application() {
    internal var mediaPlayer: MediaPlayer? = MediaPlayer()

    override fun onCreate() {
        super.onCreate()

        /**
         * @deprecated
         * MusicRepository.initialize(this)
         */
    }
}