package com.dinaraparanid.prima.utils.polymorphism.databases

/**
 * Interface for default entity
 * which can become favourite
 */
interface AsFavouriteEntity<T : Entity> {
    /**
     * Converts default entity to favourite entity
     * @return favourite entity
     */
    fun asFavourite(): T
}