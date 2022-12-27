package com.dinaraparanid.prima.databases.daos.hidden

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.hidden.HiddenArtist
import com.dinaraparanid.prima.databases.daos.EntityDao

/** [Dao] for hidden artists */

@Dao
interface HiddenArtistsDao : EntityDao<HiddenArtist> {

    /**
     * Gets all hidden artists asynchronously
     * @return all hidden artists
     */

    @Query("SELECT * FROM hidden_artists")
    suspend fun getArtistsAsync(): List<HiddenArtist>
}