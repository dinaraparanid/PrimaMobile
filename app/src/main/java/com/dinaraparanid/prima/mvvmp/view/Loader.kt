package com.dinaraparanid.prima.mvvmp.view

import android.os.Parcelable
import kotlinx.coroutines.Deferred

/**
 * Interface for loading entities
 * @param T entity itself
 */

interface Loader<T : Parcelable> {
    /**
     * Loads entities
     * @return entities after loading
     */
    fun load(): List<T>

    /**
     * Loads entities asynchronously
     * @return entities after loading
     */
    suspend fun loadAsync(): Deferred<List<T>>

    /** Container for loaded entities */
    val loadedContent: List<T>
}