package com.dinaraparanid.prima.database.relationships

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Track

data class ArtistWithTracks(
    @Embedded val artist: Artist,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "track_id",
        associateBy = Junction(ArtistTrackCrossRef::class)
    )
    val tracks: List<Track>
)
