package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.hidden.HiddenTrackDao
import com.dinaraparanid.prima.databases.entities.hidden.HiddenTrack

/** Database for hidden tracks */

@Database(entities = [HiddenTrack::class], version = 1)
abstract class HiddenTracksDatabase : RoomDatabase() {
    /** Creates new [HiddenTrackDao] */
    abstract fun hiddenTracksDao(): HiddenTrackDao
}