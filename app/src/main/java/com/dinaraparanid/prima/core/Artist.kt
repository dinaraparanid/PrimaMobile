package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.databases.entities.FavouriteArtist
import java.io.Serializable

/** Entity of singer, compositor and etc. */
open class Artist(open val name: String) : Serializable, Favourable<FavouriteArtist> {
    override fun asFavourite(): FavouriteArtist = FavouriteArtist(this)
}
