package com.dinaraparanid.prima.utils.polymorphism

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.dialogs.MessageDialog
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
    private val okAction: suspend CoroutineScope.(String, DialogInterface) -> Unit,
    private val errorMessage: Int? = null,
    private val textType: Int = InputType.TYPE_CLASS_TEXT,
    private val maxLength: Int? = null,
    private val errorAction: (suspend CoroutineScope.(String) -> Unit)? = null
) : DialogFragment(), AsyncContext {
    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    private val input by lazy {
        EditText(requireContext()).apply {
            runOnUIThread {
                setPadding(15)
                setTextColor(Params.instance.fontColor)
                inputType = textType

                if (textType == InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    transformationMethod = PasswordTransformationMethod.getInstance()

                maxLength?.let { filters = arrayOf(InputFilter.LengthFilter(it)) }
                typeface = Params.instance.getFontFromName(Params.instance.font)
            }
        }
    }

    final override fun onCreateDialog(savedInstanceState: Bundle?) =
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setView(input)
            .setPositiveButton(R.string.ok) { dialogInterface, _ ->
                val inp = input.text.toString()

                try {
                    runOnUIThread { okAction(inp, dialogInterface) }
                } catch (e: Exception) {
                    dialog!!.cancel()
                    MessageDialog(errorMessage!!).show(parentFragmentManager, null)
                    runOnIOThread { errorAction?.invoke(this, inp) }
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> dialog!!.cancel() }
            .create()
}