package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.old.AlbumOld
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.entities.CrossRefEntity

/**
 * Relationships between [ArtistOld] and [AlbumOld]
 * @deprecated Now using android MediaStore instead of database
 */

@Deprecated("Now using android MediaStore instead of database")
data class ArtistOldAndAlbumOld(
    @Embedded val artist: ArtistOld,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "artist_id"
    )
    val album: AlbumOld
) : CrossRefEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = 2444790837681540134L
    }
}
