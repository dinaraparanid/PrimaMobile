package com.dinaraparanid.prima.core

import java.io.Serializable

open class Artist(open val name: String) : Serializable, Favourable<FavouriteArtist> {
    override fun asFavourite(): FavouriteArtist = FavouriteArtist(this)
}
