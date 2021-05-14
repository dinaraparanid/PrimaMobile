package com.app.musicplayer.database.relationships

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.app.musicplayer.core.Artist
import com.app.musicplayer.core.Track

data class TrackWithArtists(
    @Embedded val track: Track,
    @Relation(
        parentColumn = "track_id",
        entityColumn = "artist_id",
        associateBy = Junction(ArtistTrackCrossRef::class)
    )
    val artists: List<Artist>
)
