package com.dinaraparanid.prima.fragments.track_collections

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.ViewPagerFragment

/** [ViewPagerFragment] for both song collections fragments */

class TrackCollectionsFragment : ViewPagerFragment() {
    override val isTabShown = true
    override val fragmentsTitles = intArrayOf(R.string.albums, R.string.playlists)

    private val albumsFragment
        get() = defaultInstance(
            resources.getString(R.string.track_collections),
            AlbumListFragment::class
        )

    private val playlistsFragment
        get() = defaultInstance(
            resources.getString(R.string.track_collections),
            PlaylistListFragment::class
        )

    override val fragments: Array<() -> Fragment> by lazy {
        arrayOf(::albumsFragment, ::playlistsFragment)
    }
}