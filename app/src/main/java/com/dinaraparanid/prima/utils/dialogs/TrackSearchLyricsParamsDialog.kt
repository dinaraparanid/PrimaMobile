package com.dinaraparanid.prima.utils.dialogs

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.fragments.TrackListFoundFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Dialog to input title and artist
 * for track of which lyrics should be found
 */

class TrackSearchLyricsParamsDialog(curTrack: AbstractTrack, mainLabel: String) :
    TrackSearchParamsDialog(curTrack) {
    init {
        action = { dialog ->
            requireActivity().supportFragmentManager
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
                        mainLabel,
                        dialogBinding!!.searchTitle.text.toString(),
                        dialogBinding!!.searchArtist.text.toString(),
                        TrackListFoundFragment.Target.LYRICS
                    )
                )
                .addToBackStack(null)
                .commit()

            dialog.dismiss()

            (requireActivity() as MainActivity).run {
                if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                    sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }
}