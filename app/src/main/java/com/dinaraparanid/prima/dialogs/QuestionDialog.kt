package com.dinaraparanid.prima.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * [DialogFragment] which ask 'Are you sure...?' or 'Do you want...'? questions,
 * if user is agree, it'll do some stuff
 *
 * @param message 'Do you want,,,?' or 'Are you sure...?' question
 * @param action action to do if user's agree
 */

class QuestionDialog(private val message: Int, private val action: suspend () -> Unit) :
    DialogFragment(), CoroutineScope by MainScope() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> launch(Dispatchers.Default) { action() } }
            .setNegativeButton(R.string.cancel) { _, _ -> dialog!!.cancel() }
            .create()
}