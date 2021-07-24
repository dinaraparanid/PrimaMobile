package com.dinaraparanid.prima.utils.polymorphism

import kotlinx.coroutines.Deferred

/**
 * Interface for loading entities belonging to this object
 * @param T
 */

internal interface Loader<T> {
    /**
     * Loads entities asynchronously
     * @return entities belonging to this object
     */
    suspend fun loadAsync(): Deferred<Unit>

    /**
     * Loader container for entities
     */
    val loaderContent: T
}