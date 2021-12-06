package com.dinaraparanid.prima.utils.polymorphism

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.dialogs.MessageDialog
import kotlinx.coroutines.CoroutineScope

/**
 * Base input dialog
 * @param message action message
 * @param okAction action on ok button pressed
 * (String param is input itself and how it can be used in okAction)
 * @param errorMessage message if okAction is failed
 */
internal abstract class InputDialog(
    private val message: Int,
    private val okAction: suspend (String) -> Unit,
    private val errorMessage: Int?,
    private val textType: Int = InputType.TYPE_CLASS_TEXT,
    private val maxLength: Int? = null
) : DialogFragment(), AsyncContext {
    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    private val input: EditText by lazy {
        EditText(requireContext()).apply {
            runOnUIThread {
                setPadding(15)
                setTextColor(Params.instance.fontColor)
                inputType = textType
                maxLength?.let { filters = arrayOf(InputFilter.LengthFilter(it)) }
                typeface = Params.instance.getFontFromName(Params.instance.font)
            }
        }
    }

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    runOnUIThread { okAction(input.text.toString()) }
                } catch (e: Exception) {
                    dialog!!.cancel()
                    MessageDialog(errorMessage!!).show(parentFragmentManager, null)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> dialog!!.cancel() }
            .create()
}