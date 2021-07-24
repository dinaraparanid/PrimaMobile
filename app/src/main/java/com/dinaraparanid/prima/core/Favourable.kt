package com.dinaraparanid.prima.core

/**
 * Interface for default entity
 * which can become favourite
 */
interface Favourable<T> {
    /**
     * Converts default entity to favourite entity
     * @return favourite entity
     */
    fun asFavourite(): T
}