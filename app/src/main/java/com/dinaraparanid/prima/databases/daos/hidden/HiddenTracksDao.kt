package com.dinaraparanid.prima.databases.daos.hidden

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.hidden.HiddenTrack
import com.dinaraparanid.prima.utils.polymorphism.databases.EntityDao

/** [Dao] for hidden tracks */

@Dao
interface HiddenTracksDao : EntityDao<HiddenTrack> {

    /**
     * Gets all hidden tracks asynchronously
     * @return all hidden tracks
     */

    @Query("SELECT * FROM HiddenTracks")
    suspend fun getTracksAsync(): List<HiddenTrack>

    /**
     * Gets all hidden tracks of artist asynchronously
     * @return all hidden tracks of artist
     */

    @Query("SELECT * FROM HiddenTracks WHERE artist_name = :artist")
    suspend fun getTracksOfArtistAsync(artist: String): List<HiddenTrack>

    /**
     * Gets all hidden tracks of album asynchronously
     * @return all hidden tracks of album
     */

    @Query("SELECT * FROM HiddenTracks WHERE album_title = :album")
    suspend fun getTracksOfAlbumAsync(album: String): List<HiddenTrack>

    /** Removes all hidden tracks of artist asynchronously */

    @Query("DELETE FROM HiddenTracks WHERE artist_name = :artist")
    suspend fun removeTracksOfArtistAsync(artist: String)

    /** Removes all hidden tracks of album asynchronously */

    @Query("DELETE FROM HiddenTracks WHERE album_title = :album")
    suspend fun removeTracksOfAlbumAsync(album: String)

    /** Removes all hidden tracks from table */

    @Query("DELETE FROM HiddenTracks")
    suspend fun removeAllTracksAsync()
}