package com.dinaraparanid.prima.databases.entities.covers

import com.dinaraparanid.prima.databases.entities.Entity

/** Ancestor for every image entity */
interface CoverEntity : Entity {
    /** Image in bytes */
    val image: ByteArray
}