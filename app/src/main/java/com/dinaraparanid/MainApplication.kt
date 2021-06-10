package com.dinaraparanid

import android.app.Application
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.Params

class MainApplication : Application() {
    internal var mainActivity: MainActivity? = null

    override fun onCreate() {
        super.onCreate()
        Params.initialize()
    }
}