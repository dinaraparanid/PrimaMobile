package com.dinaraparanid.prima.utils.polymorphism

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.dialogs.MessageDialog

/**
 * Base input dialog
 * @param message action message
 * @param okAction action on ok button pressed
 * (String param is input itself and how it can be used in okAction)
 * @param errorMessage message if okAction is failed
 */
internal abstract class InputDialog(
    private val message: Int,
    private val okAction: (String) -> Unit,
    private val errorMessage: Int?,
) : DialogFragment() {
    private val input: EditText by lazy {
        EditText(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    okAction(input.text.toString())
                } catch (e: Exception) {
                    dialog!!.cancel()
                    MessageDialog(errorMessage!!).show(parentFragmentManager, null)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> dialog!!.cancel() }
            .create()
}