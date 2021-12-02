package com.dinaraparanid.prima.utils.polymorphism

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ancestor for all services in the app
 */

abstract class AbstractService : Service(), CoroutineScope by MainScope(), AsyncContext  {
    private val iBinder = LocalBinder()
    protected val mutex = Mutex()

    private inner class LocalBinder : Binder() {
        inline val service
            get() = this@AbstractService
    }

    override val coroutineScope: CoroutineScope
        get() = this

    final override fun onBind(intent: Intent?): IBinder = iBinder

    protected abstract fun createChannel()
    protected abstract suspend fun handleIncomingActions(action: Intent?)
    protected suspend fun removeNotificationAsync() = mutex.withLock {
        runOnWorkerThread { stopForeground(true) }
    }
}