package com.app.musicplayer.database.relationships

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.app.musicplayer.core.Artist
import com.app.musicplayer.core.Track

data class ArtistWithTracks(
    @Embedded val artist: Artist,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "track_id",
        associateBy = Junction(ArtistTrackCrossRef::class)
    )
    val tracks: List<Track>
)
