package com.dinaraparanid.prima.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking

/** Wrapper for [Channel] to make it look like [android.os.ConditionVariable] */

class AsyncCondVar {
    private val channel = Channel<Unit>(1)

    suspend fun blockAsync() = channel.receive()
    suspend fun openAsync() = channel.send(Unit)

    fun block() = channel.tryReceive()
    fun open() = channel.trySend(Unit)
    fun openBlocking() = channel.trySendBlocking(Unit)
}