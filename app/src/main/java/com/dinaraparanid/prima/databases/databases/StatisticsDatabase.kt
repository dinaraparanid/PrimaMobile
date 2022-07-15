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
    /** Creates new [StatisticsTracksDao] */
    abstract fun statisticsTracksDao(): StatisticsTracksDao

    /** Creates new [StatisticsArtistsDao] */
    abstract fun statisticsArtistsDao(): StatisticsArtistsDao

    /** Creates new [StatisticsPlaylistsDao] */
    abstract fun statisticsPlaylistDao(): StatisticsPlaylistsDao
}