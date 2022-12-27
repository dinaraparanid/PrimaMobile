package com.dinaraparanid.prima.mvvmp.view

import android.os.Parcelable
import kotlinx.coroutines.coroutineScope

/** Updates UI */

interface UIUpdatable<T : Parcelable> {
    /**
     * Updates content and UI with new entities
     * @param newItems new entities to show
     * @see UIUpdatable.updateUIAsync
     */
    fun updateUI(newItems: Collection<T>)

    /**
     * Same as [updateUI], but asynchronous
     * @param newItems new entities to show
     */
    suspend fun updateUIAsync(newItems: Collection<T>) = coroutineScope { updateUI(newItems) }
}