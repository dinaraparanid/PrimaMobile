package com.dinaraparanid.prima.utils.dialogs

import android.app.Activity
import android.app.Dialog
import android.os.Message
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogFileSaveBinding
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

/**
 * Dialog which is shown when file needs to be saved
 * in some format (music, alarm, notification or ringtone)
 */

internal class FileSaveDialog(
    activity: Activity,
    private val originalName: String,
    response: Message
) : Dialog(activity) {
    private val response: Message
    private var previousSelection: Int

    private val typeArray = listOf(
        activity.resources.getString(R.string.music),
        activity.resources.getString(R.string.alarm),
        activity.resources.getString(R.string.notification),
        activity.resources.getString(R.string.ringtone)
    )

    private val binding: DialogFileSaveBinding = DataBindingUtil
        .inflate<DialogFileSaveBinding>(layoutInflater, R.layout.dialog_file_save, null, false)
        .apply { viewModel = ViewModel() }

    internal companion object {
        // File kinds - these should correspond to the order in which
        // they're presented in the spinner control

        internal const val FILE_TYPE_MUSIC = 0
        internal const val FILE_TYPE_ALARM = 1
        internal const val FILE_TYPE_NOTIFICATION = 2
        internal const val FILE_TYPE_RINGTONE = 3
    }

    init {
        setContentView(binding.root)
        setTitle(R.string.save_as)

        binding.ringtoneType.run {
            adapter = ArrayAdapter(
                context, R.layout.dialog_file_type_view, typeArray
            )

            setSelection(FILE_TYPE_RINGTONE)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    v: View,
                    position: Int,
                    id: Long
                ) = setFilenameEditBoxFromName(true)

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        previousSelection = FILE_TYPE_RINGTONE
        setFilenameEditBoxFromName(false)

        binding.saveFile.setOnClickListener {
            response.obj = binding.filename.text
            response.arg1 = binding.ringtoneType.selectedItemPosition
            response.sendToTarget()
            dismiss()
        }

        binding.cancelSaving.setOnClickListener { dismiss() }
        this.response = response
    }

    internal fun setFilenameEditBoxFromName(onlyIfNotEdited: Boolean) {
        if (onlyIfNotEdited) {
            val currentText = binding.filename.text
            val expectedText = originalName + " " + typeArray[previousSelection]

            if (!expectedText.contentEquals(currentText))
                return
        }

        val newSelection = binding.ringtoneType.selectedItemPosition
        val newSuffix = typeArray[newSelection]

        binding.filename.text = "$originalName $newSuffix"
        previousSelection = binding.ringtoneType.selectedItemPosition
    }
}