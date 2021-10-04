package com.dinaraparanid.prima.utils.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databinding.TrackSearchLyricsParamsBinding
import com.dinaraparanid.prima.fragments.TrackSelectLyricsFragment
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Dialog to input title and artist
 * for track of which lyrics should be found
 */

class TrackSearchLyricsParamsDialog(
    private val curTrack: Track,
    private val mainLabel: String,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBinding = DataBindingUtil.inflate<TrackSearchLyricsParamsBinding>(
            layoutInflater,
            R.layout.track_search_lyrics_params,
            null, false
        ).apply {
            viewModel = ViewModel()
            track = curTrack
        }

        return AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { dialog, _ ->
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
                        TrackSelectLyricsFragment.newInstance(
                            mainLabel,
                            dialogBinding.searchLyricsTitle.text.toString(),
                            dialogBinding.searchLyricsArtist.text.toString(),
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
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
    }
}