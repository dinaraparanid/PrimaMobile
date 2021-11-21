package com.dinaraparanid.prima.fragments

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.ViewPagerFragment

/**
 * [ViewPagerFragment] for both song collections fragments
 */

class TrackCollectionsFragment : ViewPagerFragment() {
    override val isTabShown = true
    override val fragmentsTitles = intArrayOf(R.string.albums, R.string.playlists)

    private val albumsFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.track_collection),
            AlbumListFragment::class
        )
    }

    private val playlistsFragment by lazy {
        defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.track_collection),
            PlaylistListFragment::class
        )
    }

    override val fragments: Array<Fragment> by lazy {
        arrayOf(albumsFragment, playlistsFragment)
    }
}