package com.dinaraparanid

import android.app.Application
import android.media.MediaPlayer
import arrow.core.None
import arrow.core.Option
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params

class MainApplication : Application() {
    internal var mainActivity: MainActivity? = null
    internal var musicPlayer: MediaPlayer? = null
    internal var curTrack: Option<Track> = None

    override fun onCreate() {
        super.onCreate()
        Params.initialize()
    }
}