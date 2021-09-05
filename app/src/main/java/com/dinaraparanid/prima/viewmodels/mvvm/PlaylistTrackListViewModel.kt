package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.ChangeImageFragment
import com.dinaraparanid.prima.utils.polymorphism.TrackListSearchFragment

open class PlaylistTrackListViewModel<B : ViewDataBinding>(
    fragment: TrackListSearchFragment<Track,
            AbstractTrackListFragment<B>.TrackAdapter,
            AbstractTrackListFragment<B>.TrackAdapter.TrackHolder, B>,
    private val activity: Activity
) : TrackListViewModel<Track,
        AbstractTrackListFragment<B>.TrackAdapter,
        AbstractTrackListFragment<B>.TrackAdapter.TrackHolder, B>(fragment) {
    /** Sends intent to pick image from gallery*/

    @JvmName("onPlaylistTrackImageButtonPressed")
    internal fun onPlaylistTrackImageButtonPressed() = activity.startActivityForResult(
        Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ), ChangeImageFragment.PICK_IMAGE
    )
}