package com.dinaraparanid.prima.utils.polymorphism

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Ancestor for all services in the app
 */

abstract class AbstractService : Service() {
    private val iBinder = LocalBinder()

    private inner class LocalBinder : Binder() {
        inline val service
            get() = this@AbstractService
    }

    final override fun onBind(intent: Intent?): IBinder = iBinder

    protected abstract fun createChannel()
    protected abstract fun handleIncomingActions(action: Intent?)

    @Synchronized
    protected fun removeNotification() = stopForeground(true)
}