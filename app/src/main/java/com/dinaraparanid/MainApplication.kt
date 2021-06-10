package com.dinaraparanid

import android.app.Application
import android.media.MediaPlayer
import android.util.Log
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.Params

class MainApplication : Application() {
    internal var mainActivity: MainActivity? = null
    internal var musicPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        Params.initialize()
    }

    override fun onTerminate() {
        Log.d("APP DEAD", "asdasdasd")
        super.onTerminate()
    }
}