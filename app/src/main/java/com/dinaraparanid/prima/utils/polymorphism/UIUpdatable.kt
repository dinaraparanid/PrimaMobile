package com.dinaraparanid.prima.utils.polymorphism

import kotlinx.coroutines.Job

/**
 * Updates UI
 */

internal interface UIUpdatable<T> {
    /**
     * Updates UI
     * @param src source which will be used to update
     */

    suspend fun updateUIAsync(src: T): Job
}