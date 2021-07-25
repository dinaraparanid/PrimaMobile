package com.dinaraparanid.prima.utils.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.R

/**
 * [DialogFragment] which ask 'Are you sure...?' questions,
 * if user is agree, it'll do some stuff
 *
 * @param message 'Are you sure...?' question
 * @param action action to do if user's agree
 */

class AreYouSureDialog(private val message: Int, private val action: () -> Unit) :
    DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> action() }
            .setNegativeButton(R.string.cancel) { _, _ -> dialog!!.cancel() }
            .create()
}