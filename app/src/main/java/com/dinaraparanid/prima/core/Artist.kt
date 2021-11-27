package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.databases.entities.FavouriteArtist
import java.io.Serializable

/** Entity of singer, compositor and etc. */
open class Artist(open val name: String) : Serializable, Favourable<FavouriteArtist> {
    final override fun asFavourite(): FavouriteArtist = FavouriteArtist(this)

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return name == (other as Artist).name
    }

    final override fun hashCode() = name.hashCode()
}
