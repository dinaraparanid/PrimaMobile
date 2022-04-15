package com.dinaraparanid.prima.databases.daos.hidden

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.hidden.HiddenTrack
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/** DAO for hidden tracks */

@Dao
interface HiddenTrackDao : EntityDao<HiddenTrack> {

    /**
     * Gets all hidden tracks asynchronously
     * @return all hidden tracks
     */

    @Query("SELECT * FROM HiddenTracks")
    suspend fun getTracksAsync(): List<HiddenTrack>
}