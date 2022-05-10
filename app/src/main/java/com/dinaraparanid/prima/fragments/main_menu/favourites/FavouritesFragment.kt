package com.dinaraparanid.prima.fragments.main_menu.favourites

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.fragments.ViewPagerFragment

/** [ViewPagerFragment] for all favourite fragments */

class FavouritesFragment : ViewPagerFragment() {
    override val isTabShown = true

    override val fragmentsTitles = intArrayOf(
        R.string.tracks,
        R.string.artists,
        R.string.track_collections
    )

    override val fragmentsConstructors: Array<() -> Fragment> by lazy {
        arrayOf(::tracksFragment, ::artistsFragment, ::playlistsFragment)
    }

    private inline val tracksFragment
        get() = defaultInstance(
            resources.getString(R.string.favourites),
            FavouriteTrackListFragment::class
        )

    private inline val artistsFragment
        get() = defaultInstance(
            resources.getString(R.string.favourites),
            FavouriteArtistListFragment::class
        )

    private inline val playlistsFragment
        get() = defaultInstance(
            resources.getString(R.string.favourites),
            FavouritePlaylistListFragment::class
        )
}