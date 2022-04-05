package com.dinaraparanid.prima.utils.polymorphism

import kotlinx.coroutines.Job

/**
 * Interface for loading entities belonging to this object
 * @param T entity
 */

internal interface Loader<T> {
    /**
     * Loads entities asynchronously
     * @return entities belonging to this object
     */
    suspend fun loadAsync(): Job

    /** Loader container for entities */
    val loaderContent: T
}