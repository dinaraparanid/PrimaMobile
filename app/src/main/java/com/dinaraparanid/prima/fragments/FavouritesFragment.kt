package com.dinaraparanid.prima.fragments

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.ViewPagerFragment

/**
 * [ViewPagerFragment] for both favourite fragments
 */

class FavouritesFragment : ViewPagerFragment() {
    override val firstFragmentTitle = R.string.favourite_tracks
    override val secondFragmentTitle = R.string.favourite_artists

    override val firstFragment: Fragment
        get() = defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.favourites),
            FavouriteTrackListFragment::class
        )

    override val secondFragment: Fragment
        get() = defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.favourites),
            FavouriteArtistListFragment::class
        )
}