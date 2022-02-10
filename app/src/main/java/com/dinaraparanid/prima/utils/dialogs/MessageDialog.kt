package com.dinaraparanid.prima.utils.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.R

/** [DialogFragment] which only shows message */

class MessageDialog(private val message: Int) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> dialog!!.cancel() }
            .create()
}