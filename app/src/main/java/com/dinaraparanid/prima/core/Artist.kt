package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.databases.entities.favourites.FavouriteArtist
import com.dinaraparanid.prima.utils.polymorphism.databases.AsFavouriteEntity
import com.dinaraparanid.prima.utils.polymorphism.databases.Entity

/** Entity for artists, singers, compositors, etc. */

open class Artist(open val name: String) :
    Entity,
    AsFavouriteEntity<FavouriteArtist>,
    Comparable<Artist> {
    /** Converts [Artist] to [FavouriteArtist] */
    final override fun asFavourite() = FavouriteArtist(this)

    /** Compares artist by his [name] */
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Artist) return false
        return name == other.name
    }

    /** Hashes artist by his [name] */
    final override fun hashCode() = name.hashCode()

    /** Compares artist by his [name] */
    final override fun compareTo(other: Artist) = name.compareTo(other.name)
}
