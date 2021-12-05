package com.dinaraparanid.prima.utils.polymorphism

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Updates UI */

internal interface UIUpdatable<T> {
    val mutex: Mutex
    suspend fun updateUINoLock(src: T)
}

/**
 * Updates UI
 * @param src source which will be used to update
 */

internal suspend fun <T> UIUpdatable<T>.updateUI(src: T, isLocking: Boolean) = when {
    isLocking -> mutex.withLock { updateUINoLock(src) }
    else -> updateUINoLock(src)
}