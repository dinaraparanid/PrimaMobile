package com.dinaraparanid.prima.dialogs

import android.app.AlertDialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.R

/** [DialogFragment] which only shows message */

class MessageDialog(@StringRes private val message: Int) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.cancel() }
            .create()
}