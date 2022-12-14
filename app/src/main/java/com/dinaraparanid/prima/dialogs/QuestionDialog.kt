package com.dinaraparanid.prima.dialogs

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * [DialogFragment] which ask 'Are you sure...?' or 'Do you want...'? questions,
 * if user is agree, it'll do some stuff
 *
 * @param message 'Do you want,,,?' or 'Are you sure...?' question
 * @param onOkPressed action to do if user's agree
 */

class QuestionDialog(
    @StringRes private val message: Int,
    private val onOkPressed: suspend DialogInterface.() -> Unit
) : DialogFragment(), CoroutineScope by MainScope() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog, _ -> launch { onOkPressed(dialog) } }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create()
}