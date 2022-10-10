package com.dinaraparanid.prima.utils.polymorphism

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Ancestor for all services in the app */

abstract class AbstractService : Service(), CoroutineScope by MainScope(), AsyncContext {
    private val iBinder = LocalBinder()
    protected val mutex = Mutex()

    private class LocalBinder : Binder()

    override val coroutineScope: CoroutineScope
        get() = this

    final override fun onBind(intent: Intent?): IBinder = iBinder

    protected abstract fun createChannel()

    protected abstract suspend fun handleIncomingActionsNoLock(action: Intent?)

    protected suspend fun handleIncomingActions(action: Intent?, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { handleIncomingActionsNoLock(action) }
        else -> handleIncomingActionsNoLock(action)
    }

    private fun removeNotificationNoLock() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> stopForeground(STOP_FOREGROUND_REMOVE)
        else -> stopForeground(true)
    }

    protected suspend fun removeNotificationAsync(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { removeNotificationNoLock() }
        else -> removeNotificationNoLock()
    }
}