package com.dinaraparanid.prima.mvvmp.old_shit

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.rustlibs.NativeLibrary

/** MVVM View Model for AbstractArtistListFragment */

class ArtistListViewModel : BasePresenter() {
    /**
     * Sets name in trim form to artist
     * @param artist artist which name' ll be trimmed
     * @see NativeLibrary.artistImageBind
     */

    @JvmName("getArtistImage")
    internal fun getArtistImage(artist: Artist) = artist.name.trim().let { name ->
        when (name) {
            params.application.unchecked.resources.getString(R.string.unknown_artist) -> "?"
            else -> NativeLibrary.artistImageBind(name)
        }
    }
}