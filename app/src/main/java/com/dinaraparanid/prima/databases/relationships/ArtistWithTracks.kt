package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.utils.polymorphism.databases.CrossRefEntity

/**
 * Relationships between [ArtistOld] and his [TrackOld]
 * @deprecated Now using android MediaStore instead of database
 */

@Deprecated("Now using android MediaStore instead of database")
data class ArtistWithTracks(
    @Embedded val artist: ArtistOld,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "track_id",
        associateBy = Junction(ArtistTrackCrossRef::class)
    )
    val tracks: List<TrackOld>
) : CrossRefEntity
