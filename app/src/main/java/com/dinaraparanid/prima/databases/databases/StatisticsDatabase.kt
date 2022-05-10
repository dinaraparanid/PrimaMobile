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
    version = 3
)
abstract class StatisticsDatabase : RoomDatabase() {
    /** Creates new [StatisticsTrackDao] */
    abstract fun statisticsTracksDao(): StatisticsTrackDao

    /** Creates new [StatisticsArtistDao] */
    abstract fun statisticsArtistDao(): StatisticsArtistDao

    /** Creates new [StatisticsPlaylistDao] */
    abstract fun statisticsPlaylistDao(): StatisticsPlaylistDao
}