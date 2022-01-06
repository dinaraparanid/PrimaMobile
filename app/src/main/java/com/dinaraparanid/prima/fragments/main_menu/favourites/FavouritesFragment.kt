package com.dinaraparanid.prima.fragments.main_menu.favourites

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.ViewPagerFragment

/**
 * [ViewPagerFragment] for all favourite fragments
 */

class FavouritesFragment : ViewPagerFragment() {
    override val isTabShown = true
    override val fragmentsTitles = intArrayOf(
        R.string.tracks,
        R.string.artists,
        R.string.track_collections
    )

    override val fragments: Array<Fragment> by lazy {
        arrayOf(tracksFragment, artistsFragment, playlistsFragment)
    }

    private val tracksFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.favourites),
            FavouriteTrackListFragment::class
        )
    }

    private val artistsFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.favourites),
            FavouriteArtistListFragment::class
        )
    }

    private val playlistsFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.favourites),
            FavouritePlaylistListFragment::class
        )
    }
}