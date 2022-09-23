package com.dinaraparanid.prima.utils

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.withTimeout

/** Wrapper for [Channel] to make it look like [android.os.ConditionVariable] */

class AsyncCondVar {
    private val channel = Channel<Unit>(1)

    suspend fun blockAsync() = channel.receive()

    suspend fun blockAsync(millis: Long) = try {
        withTimeout(millis) { channel.receive() }
        true
    } catch (e: TimeoutCancellationException) {
        false
    }

    suspend fun openAsync() = channel.send(Unit)

    fun block() = channel.tryReceive()
    fun open() = channel.trySend(Unit)
    fun openBlocking() = channel.trySendBlocking(Unit)
}