package com.dinaraparanid.prima

import android.app.Application

class MusicPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        /**
         * @deprecated
         * MusicRepository.initialize(this)
         */
    }
}