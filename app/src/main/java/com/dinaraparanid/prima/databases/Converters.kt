package com.dinaraparanid.prima.databases

import androidx.room.TypeConverter
import java.util.UUID

/** Converts Java's types to SQLite types */

class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID?) = uuid?.toString()

    @TypeConverter
    fun toUUID(uuid: String?) = uuid?.let { UUID.fromString(it) }
}