package com.dinaraparanid.prima.databases.entities.favourites

import com.dinaraparanid.prima.databases.entities.Entity

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