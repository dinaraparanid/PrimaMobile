package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.app.Dialog
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogTrimmedAudioFileSaveBinding
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.TrimmedAudioFileSavePresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.TrimmedAudioFileSaveUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.TrimmedAudioFileSaveViewModel
import com.dinaraparanid.prima.utils.extensions.correctFileName
import kotlinx.coroutines.channels.Channel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Dialog which is shown when file needs to be saved
 * in some format (music, alarm, notification or ringtone)
 */

class TrimmedAudioFileSaveDialog(
    private val initialFileName: String,
    private val fileDataChannel: Channel<TrimmedAudioFileData>
) : ObservableDialogFragment<
        TrimmedAudioFileSavePresenter,
        TrimmedAudioFileSaveViewModel,
        TrimmedAudioFileSaveUIHandler,
        DialogTrimmedAudioFileSaveBinding
>() {
    companion object {
        // File kinds - these should correspond to the order in which
        // they're presented in the spinner control

        const val FILE_TYPE_MUSIC = 0
        const val FILE_TYPE_ALARM = 1
        const val FILE_TYPE_NOTIFICATION = 2
        const val FILE_TYPE_RINGTONE = 3
    }

    data class TrimmedAudioFileData(val fileName: String, val selectedItemPosition: Int)

    override val uiHandler by inject<TrimmedAudioFileSaveUIHandler>()

    override val viewModel by viewModel<TrimmedAudioFileSaveViewModel> {
        parametersOf(initialFileName)
    }

    override val stateChangesCallbacks by lazy {
        arrayOf(
            StateChangedCallback(uiHandler, viewModel.isSaveFileButtonPressedState) {
                sendFileDataAndCloseDialog(
                    fileData = TrimmedAudioFileData(
                        fileName = viewModel.presenter.fileName.correctFileName,
                        selectedItemPosition = viewModel.presenter.selectedItemPosition
                    ),
                    fileDataChannel = fileDataChannel,
                    dialog = requireDialog()
                )

                viewModel.finishSavingFile()
            },
            StateChangedCallback(uiHandler, viewModel.isCancelSavingButtonPressedState) {
                closeDialog(requireDialog())
                viewModel.finishCancelSaving()
            }
        )
    }

    private val typeArray = listOf(
        resources.getString(R.string.music),
        resources.getString(R.string.alarm),
        resources.getString(R.string.notification),
        resources.getString(R.string.ringtone)
    )

    override val dialogBinding
        get() = DataBindingUtil
            .inflate<DialogTrimmedAudioFileSaveBinding>(
                layoutInflater,
                R.layout.dialog_trimmed_audio_file_save,
                null, false
            )
            .apply {
                viewModel = this@TrimmedAudioFileSaveDialog.viewModel

                trackTypeAdapter = ArrayAdapter(
                    requireContext(),
                    R.layout.dialog_text_view,
                    typeArray
                )

                executePendingBindings()
            }

    override val dialogView
        get() = Dialog(requireContext()).apply {
            setContentView(binding.root)
            setTitle(R.string.save_as)
            setCancelable(true)
        }
}