package com.dinaraparanid.prima.fragments.hidden

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.fragments.ViewPagerFragment

/** [ViewPagerFragment] for fragments with hidden content */

class HiddenHolderFragment : ViewPagerFragment() {
    override val isTabShown = true

    override val fragmentsTitles = intArrayOf(
        R.string.tracks,
        R.string.artists,
        R.string.track_collections
    )

    override val fragmentsConstructors: Array<() -> Fragment> by lazy {
        arrayOf(
            ::createHiddenTracksFragment,
            ::createHiddenArtistsFragment,
            ::createHiddenPlaylistsFragment
        )
    }

    private fun createHiddenTracksFragment() = defaultInstance(
        resources.getString(R.string.hidden),
        HiddenTrackListFragment::class
    )

    private fun createHiddenArtistsFragment() = defaultInstance(
        resources.getString(R.string.hidden),
        HiddenArtistListFragment::class
    )

    private fun createHiddenPlaylistsFragment() = defaultInstance(
        resources.getString(R.string.hidden),
        HiddenPlaylistListFragment::class
    )
}