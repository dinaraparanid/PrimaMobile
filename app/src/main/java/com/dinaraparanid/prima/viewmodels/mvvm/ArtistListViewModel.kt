package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.rustlibs.NativeLibrary

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.utils.polymorphism.AbstractArtistListFragment]
 */

class ArtistListViewModel : ViewModel() {
    /**
     * Sets name in trim form to artist
     * @param artist artist which name' ll be trimmed
     * @see NativeLibrary.artistImageBind
     */

    @JvmName("getArtistImage")
    internal fun getArtistImage(artist: Artist) = artist.name.trim().let { name ->
        when (name) {
            params.application.unchecked.resources.getString(R.string.unknown_artist) -> "?"
            else -> NativeLibrary.artistImageBind(name.toByteArray())
        }
    }
}