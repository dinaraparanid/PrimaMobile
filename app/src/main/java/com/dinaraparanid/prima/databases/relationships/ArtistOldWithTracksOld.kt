package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.databases.entities.CrossRefEntity

/**
 * Relationships between [ArtistOld] and his [TrackOld]
 * @deprecated Now using android MediaStore instead of database
 */

@Deprecated("Now using android MediaStore instead of database")
data class ArtistOldWithTracksOld(
    @Embedded val artist: ArtistOld,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "track_id",
        associateBy = Junction(ArtistOldTrackOldCrossRef::class)
    )
    val tracks: List<TrackOld>
) : CrossRefEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = -148922267319155454L
    }
}
