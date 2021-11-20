package com.dinaraparanid.prima.fragments

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.ViewPagerFragment

/**
 * [ViewPagerFragment] for both song collections fragments
 */

class SongCollectionsFragment : ViewPagerFragment() {
    override val firstFragmentTitle = R.string.albums
    override val secondFragmentTitle = R.string.playlists

    override val firstFragment: Fragment
        get() = defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.track_collection),
            AlbumListFragment::class
        )

    override val secondFragment: Fragment
        get() = defaultInstance(
            mainLabelOldText,
            resources.getString(R.string.albums),
            PlaylistListFragment::class
        )

}