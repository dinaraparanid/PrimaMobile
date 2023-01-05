package com.dinaraparanid.prima.utils.polymorphism.fragments

import androidx.databinding.ViewDataBinding
import carbon.widget.FloatingActionButton
import com.dinaraparanid.prima.mvvmp.view.fragments.AbstractTrackListFragment

/**
 * Interface for Track Collection's tracks Fragment
 * (such as AlbumTrackListFragment)
 *
 * P.S. It should be an interface,
 * but Kotlin can't generate correct Java code...
 */

abstract class TrackCollectionTrackListFragment<B : ViewDataBinding> :
    AbstractTrackListFragment<B>() {
    internal abstract val addPlaylistToFavouritesButton: FloatingActionButton
}