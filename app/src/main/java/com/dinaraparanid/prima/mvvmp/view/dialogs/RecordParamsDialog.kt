package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.app.Dialog
import android.os.Build
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogRecordBinding
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.RecordParamsPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.RecordParamsUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.RecordParamsViewModel
import kotlinx.coroutines.channels.Channel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject

/** [Dialog] to start recording */

class RecordParamsDialog(
    setRecordButtonImageChannel: Channel<Boolean>,
    micRecorderCallerChannel: Channel<String>,
    startPlaybackRecorderChannel: Channel<String>
) : ObservableDialogFragment<
        RecordParamsPresenter,
        RecordParamsViewModel,
        RecordParamsUIHandler,
        DialogRecordBinding
>() {
    override val uiHandler by inject<RecordParamsUIHandler>()
    override val viewModel by viewModel<RecordParamsViewModel>()

    override val stateChangesCallbacks by lazy {
        arrayOf(
            StateChangedCallback(
                uiHandler = uiHandler,
                state = viewModel.isRecordSourceSpinnerDropdownIconPressedState
            ) {
                showSpinnerItems(binding.recordSourceSpinner)
                viewModel.finishRecordSourceSetting()
            },
            StateChangedCallback(
                uiHandler = uiHandler,
                state = viewModel.isStartRecordingButtonPressed
            ) {
                requireDialog().startRecording(
                    viewModel.presenter,
                    setRecordButtonImageChannel,
                    micRecorderCallerChannel,
                    startPlaybackRecorderChannel
                )

                viewModel.finishCallingRecorder()
            }
        )
    }

    private val sourceArray = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listOf(
            resources.getString(R.string.source_mic),
            resources.getString(R.string.source_playback),
        )

        else -> listOf(resources.getString(R.string.source_mic))
    }

    override val dialogBinding
        get() = DataBindingUtil
            .inflate<DialogRecordBinding>(
                layoutInflater,
                R.layout.dialog_record,
                null, false
            )
            .apply {
                viewModel = this@RecordParamsDialog.viewModel
                recordSrcAdapter = ArrayAdapter(
                    requireContext(),
                    R.layout.dialog_text_view,
                    sourceArray
                )

                executePendingBindings()
            }

    override val dialogView
        get() = Dialog(requireContext()).apply {
            setContentView(binding.root)
            setTitle(R.string.record_audio)
            setCancelable(true)
        }
}