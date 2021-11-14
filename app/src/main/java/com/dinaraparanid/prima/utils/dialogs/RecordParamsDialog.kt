package com.dinaraparanid.prima.utils.dialogs

import android.app.Dialog
import android.os.Build
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogRecordBinding
import com.dinaraparanid.prima.services.MicRecordService
import com.dinaraparanid.prima.services.PlaybackRecordService
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import java.lang.ref.WeakReference

/** [Dialog] to start recording */

class RecordParamsDialog(activity: MainActivity) : Dialog(activity) {
    private val binding = DataBindingUtil
        .inflate<DialogRecordBinding>(layoutInflater, R.layout.dialog_record, null, false)
        .apply { viewModel = ViewModel() }

    private val sourceArray = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listOf(
            activity.resources.getString(R.string.source_mic),
            activity.resources.getString(R.string.source_playback),
        )

        else -> listOf(activity.resources.getString(R.string.source_mic))
    }

    init {
        setContentView(binding!!.root)
        setTitle(R.string.record_audio)
        setCancelable(true)

        binding.recordSourceSpinner.run {
            adapter = ArrayAdapter(activity, R.layout.dialog_text_view, sourceArray)
            setSelection(0)
        }

        binding.recordSourceSpinnerDropdownIcon.setOnClickListener {
            binding.recordSourceSpinner.performClick()
        }

        binding.startRecording.setOnClickListener {
            when (binding.recordSourceSpinner.selectedItemPosition) {
                0 -> MicRecordService.Caller(WeakReference(activity.application as MainApplication))
                        .setFileName(binding.recordFilename.text.toString())
                        .call()

                else -> PlaybackRecordService.Caller(WeakReference(activity.application as MainApplication))
                    .setFileName(binding.recordFilename.text.toString())
                    .call()
            }

            activity.setRecordButtonImage(true)
            dismiss()
        }
    }
}