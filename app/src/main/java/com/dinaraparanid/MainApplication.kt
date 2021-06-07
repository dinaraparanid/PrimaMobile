package com.dinaraparanid

import android.app.Application
import arrow.core.None
import arrow.core.Option
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.Params

class MainApplication : Application() {
    internal var playingThread: Option<Thread> = None
    internal var mainActivity: MainActivity? = null
    internal var curIndex = -1

    override fun onCreate() {
        super.onCreate()
        Params.initialize()
    }
}