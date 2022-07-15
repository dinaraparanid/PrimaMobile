package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.hidden.HiddenArtistsDao
import com.dinaraparanid.prima.databases.daos.hidden.HiddenPlaylistsDao
import com.dinaraparanid.prima.databases.daos.hidden.HiddenTracksDao
import com.dinaraparanid.prima.databases.entities.hidden.HiddenArtist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenPlaylist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenTrack

/** Database for hidden entities */

@Database(
    entities = [HiddenTrack::class, HiddenArtist::class, HiddenPlaylist.Entity::class],
    version = 2
)
abstract class HiddenDatabase : RoomDatabase() {
    /** Creates new [HiddenTracksDao] */
    abstract fun hiddenTracksDao(): HiddenTracksDao

    /** Creates new [HiddenArtistsDao] */
    abstract fun hiddenArtistsDao(): HiddenArtistsDao

    /** Creates new [HiddenPlaylistsDao] */
    abstract fun hiddenPlaylistsDao(): HiddenPlaylistsDao
}