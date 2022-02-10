package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.statistics.*
import com.dinaraparanid.prima.databases.entities.statistics.*

/** Database for user's statistics */

@Database(
    entities = [
        StatisticsTrack::class,
        StatisticsArtist::class,
        StatisticsPlaylist.Entity::class
    ],
    version = 2
)
abstract class StatisticsDatabase : RoomDatabase() {
    abstract fun statisticsTracksDao(): StatisticsTrackDao
    abstract fun statisticsArtistDao(): StatisticsArtistDao
    abstract fun statisticsPlaylistDao(): StatisticsPlaylistDao
}