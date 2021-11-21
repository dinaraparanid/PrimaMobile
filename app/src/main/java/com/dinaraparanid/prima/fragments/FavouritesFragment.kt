package com.dinaraparanid.prima.fragments

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.ViewPagerFragment

/**
 * [ViewPagerFragment] for both favourite fragments
 */

class FavouritesFragment : ViewPagerFragment() {
    override val isTabShown = true
    override val fragmentsTitles = intArrayOf(R.string.favourite_tracks, R.string.favourite_artists)

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

    override val fragments: Array<Fragment> by lazy {
        arrayOf(tracksFragment, artistsFragment)
    }
}