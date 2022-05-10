package com.dinaraparanid.prima.utils.polymorphism.databases

import androidx.room.*

/**
 * [Dao] for every entity.
 * Supports insert, update, remove
 */

@Dao
interface EntityDao<T> {

    /** Updates entity asynchronously */

    @Update
    suspend fun updateAsync(entity: T)

    /** Inserts entity asynchronously */

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAsync(entity: T)

    /** Removes entity asynchronously */

    @Delete
    suspend fun removeAsync(entity: T)
}