package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.databases.entities.FavouriteArtist
import java.io.Serializable

open class Artist(open val name: String) : Serializable, Favourable<FavouriteArtist> {
    override fun asFavourite(): FavouriteArtist = FavouriteArtist(this)
}
