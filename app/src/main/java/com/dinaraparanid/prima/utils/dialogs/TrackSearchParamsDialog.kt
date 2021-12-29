package com.dinaraparanid.prima.utils.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogTrackSearchParamsBinding
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

/**
 * Dialog to input title and artist
 * for track of which lyrics should be found
 */

open class TrackSearchParamsDialog(
    private val curTrack: AbstractTrack,
    protected var action: ((DialogInterface) -> Unit)? = null
) : DialogFragment() {
    protected var dialogBinding: DialogTrackSearchParamsBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogBinding = DataBindingUtil.inflate<DialogTrackSearchParamsBinding>(
            layoutInflater,
            R.layout.dialog_track_search_params,
            null, false
        ).apply {
            viewModel = ViewModel()
            track = curTrack
        }

        return AlertDialog.Builder(requireContext())
            .setView(dialogBinding!!.root)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { dialog, _ -> action?.invoke(dialog) }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogBinding = null
    }
}