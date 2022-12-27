package com.dinaraparanid.prima.databases.daos.old

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.relationships.ArtistOldAndAlbumOld
import com.dinaraparanid.prima.databases.daos.CrossRefDao
import java.util.UUID

/**
 * [Dao] for [AlbumOld] and [ArtistOld] relationships
 * @deprecated Now using android MediaStore instead of database
 */

@Dao
@Deprecated("Now using android MediaStore instead of database")
interface ArtistsAndAlbumsDao : CrossRefDao<ArtistOldAndAlbumOld> {

    /**
     * Gets all artist-album relationships
     * @return list of all [ArtistOldAndAlbumOld] relationships
     * @deprecated Now using android MediaStore instead of database
     */

    @Transaction
    @Query("SELECT * FROM artist")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistsWithAlbums(): List<ArtistOldAndAlbumOld>

    /**
     * Gets artist by album or null if there is no such artist
     * @param albumArtistId artist's id of album
     * @return found artist or null
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM artist WHERE artist_id = :albumArtistId")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getArtistByAlbum(albumArtistId: UUID): ArtistOld?

    /**
     * Gets all albums of artist
     * @return list of all albums
     * @deprecated Now using android MediaStore instead of database
     */

    @Query("SELECT * FROM album WHERE artist_id = :artistId")
    @Deprecated("Now using android MediaStore instead of database")
    suspend fun getAlbumsByArtist(artistId: UUID): List<AlbumOld>
}