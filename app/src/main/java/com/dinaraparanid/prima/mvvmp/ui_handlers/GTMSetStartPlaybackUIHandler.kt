package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.track_lists.TrackSelectFragment
import com.dinaraparanid.prima.mvvmp.view.dialogs.GTMSetPropertiesDialogFragment
import com.dinaraparanid.prima.mvvmp.view.dialogs.GTMSetStartPlaybackDialogFragment

/** [GTMPropertiesUIHandler] for [GTMSetStartPlaybackDialogFragment] */

class GTMSetStartPlaybackUIHandler : GTMPropertiesUIHandler {
    override fun <D : GTMSetPropertiesDialogFragment<*, *, *, *>> D.onOkPressed(dialog: DialogInterface) =
        (this as GTMSetStartPlaybackDialogFragment).showTrackSelectFragment(dialog)

    fun GTMSetStartPlaybackDialogFragment.showTrackSelectFragment(dialog: DialogInterface) {
        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                TrackSelectFragment.Builder(target = TrackSelectFragment.Companion.TracksSelectionTarget.GTM)
                    .setGTMPlaybackLength(gtmPlaybackLength = viewModel.presenter.gtmPlaybackLen.toByte())
                    .build()
            )
            .addToBackStack(null)
            .commit()

        dialog.dismiss()
    }
}