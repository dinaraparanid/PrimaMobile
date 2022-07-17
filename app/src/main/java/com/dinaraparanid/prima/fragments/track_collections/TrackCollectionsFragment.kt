package com.dinaraparanid.prima.fragments.track_collections

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.fragments.ViewPagerFragment

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
            DefaultPlaylistListFragment::class
        )

    override val fragmentsConstructors: Array<() -> Fragment> by lazy {
        arrayOf(::albumsFragment, ::playlistsFragment)
    }
}