package com.dinaraparanid.prima.utils

import com.dinaraparanid.prima.core.Artist
import java.io.Serializable

class ArtistList : Serializable {
    val data: MutableList<Artist> = mutableListOf<Artist>()
}