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

class TrackSearchLyricsParamsDialog : DialogFragment() {
    internal companion object {
        private const val TRACK_KEY = "track"
        private const val MAIN_LABEL_KEY = "main_label"
        private const val API_KEY = "api_key"

        /**
         * Creates new instance of [TrackSearchLyricsParamsDialog] with given params
         * @param curTrack track of which lyrics should be found
         * @param mainLabel cur label of app
         * @param apiKey user's Api key of Happi
         * @return new instance of dialog
         */

        internal fun newInstance(curTrack: Track, mainLabel: String, apiKey: String) =
            DialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(TRACK_KEY, curTrack)
                    putString(MAIN_LABEL_KEY, mainLabel)
                    putString(API_KEY, apiKey)
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBinding = DataBindingUtil.inflate<TrackSearchLyricsParamsBinding>(
            layoutInflater,
            R.layout.track_search_lyrics_params,
            null, false
        ).apply {
            viewModel = ViewModel()
            track = requireArguments().getSerializable(TRACK_KEY) as Track
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
                            requireArguments().getString(MAIN_LABEL_KEY)!!,
                            dialogBinding.searchLyricsTitle.text.toString(),
                            dialogBinding.searchLyricsArtist.text.toString(),
                            requireArguments().getString(API_KEY)!!
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