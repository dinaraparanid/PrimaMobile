package com.dinaraparanid.prima.databases

import androidx.room.TypeConverter
import java.util.UUID

@Deprecated("Now using android storage instead of database")
class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(uuid: String?): UUID? = uuid?.let { UUID.fromString(it) }
}