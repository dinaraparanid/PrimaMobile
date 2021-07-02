package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.ArtistOld
import com.dinaraparanid.prima.databases.entities.TrackOld

@Deprecated("Now using android storage instead of database")
data class ArtistWithTracks(
    @Embedded val artist: ArtistOld,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "track_id",
        associateBy = Junction(ArtistTrackCrossRef::class)
    )
    val tracks: List<TrackOld>
)
