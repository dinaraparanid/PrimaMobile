package com.dinaraparanid.prima.databases

import androidx.room.TypeConverter
import java.util.UUID

/**
 * Converts Java's types to SQLite types
 * @deprecated Now using android MediaStore instead of database
 */

@Deprecated("Now using android MediaStore instead of database")
class Converters {
    @TypeConverter
    @Deprecated("Now using android MediaStore instead of database")
    fun fromUUID(uuid: UUID?) = uuid?.toString()

    @TypeConverter
    @Deprecated("Now using android MediaStore instead of database")
    fun toUUID(uuid: String?) = uuid?.let { UUID.fromString(it) }
}