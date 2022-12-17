package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.track_lists.TrackListFoundFragment
import kotlinx.coroutines.channels.Channel

/** [UIHandler] for TrackSearchParamsDialogFragments */

class TrackSearchParamsUIHandler : UIHandler {
    private fun showTrackListFoundFragment(
        fragmentManager: FragmentManager,
        trackTitle: String,
        artistName: String,
        target: TrackListFoundFragment.Target
    ) {
        fragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                TrackListFoundFragment.newInstance(
                    title = trackTitle,
                    artist = artistName,
                    target = target
                )
            )
            .addToBackStack(null)
            .commit()
    }

    internal suspend inline fun DialogInterface.onOkPressedAsync(
        fragmentManager: FragmentManager,
        trackTitle: String,
        artistName: String,
        target: TrackListFoundFragment.Target,
        setSheetBehaviourFromExpandedToCollapsedChannel: Channel<Unit>
    ) {
        showTrackListFoundFragment(fragmentManager, trackTitle, artistName, target)
        setSheetBehaviourFromExpandedToCollapsedChannel.send(Unit)
        dismiss()
    }
}