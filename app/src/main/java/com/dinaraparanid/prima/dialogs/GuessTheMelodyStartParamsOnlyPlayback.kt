package com.dinaraparanid.prima.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogGtmStartParamsOnlyPlaybackBinding
import com.dinaraparanid.prima.fragments.track_lists.TrackSelectFragment
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter

/**
 * Specially for custom playlists
 * Similar to [GuessTheMelodyStartParamsDialog] but without track's amount param
 */

class GuessTheMelodyStartParamsOnlyPlayback : DialogFragment() {
    private var dialogBinding: DialogGtmStartParamsOnlyPlaybackBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogBinding = DataBindingUtil.inflate<DialogGtmStartParamsOnlyPlaybackBinding>(
            layoutInflater,
            R.layout.dialog_gtm_start_params_only_playback,
            null,
            false
        ).apply { viewModel = BasePresenter() }

        return AlertDialog.Builder(requireContext())
            .setView(dialogBinding!!.root)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in,
                        R.anim.slide_out,
                        R.anim.slide_in,
                        R.anim.slide_out
                    )
                    .replace(
                        R.id.fragment_container,
                        TrackSelectFragment.Builder(
                            TrackSelectFragment.Companion.TracksSelectionTarget.GTM
                        ).setGTMPlaybackLength(
                            dialogBinding!!.gtmPlaybackLen.text.toString().toByte()
                        ).build()
                    )
                    .addToBackStack(null)
                    .commit()

                dialog.dismiss()
                dialogBinding = null
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
    }
}