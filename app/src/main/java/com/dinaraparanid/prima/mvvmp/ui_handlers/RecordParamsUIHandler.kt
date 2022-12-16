package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import android.widget.Spinner
import com.dinaraparanid.prima.mvvmp.presenters.RecordParamsPresenter
import com.dinaraparanid.prima.utils.Params
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** [UIHandler] for RecordParamsDialog */

class RecordParamsUIHandler(private val params: Params) : UIHandler {
    private inline val pathToSave
        get() = params.pathToSave

    fun showSpinnerItems(recordSourceSpinner: Spinner) = recordSourceSpinner.performClick()

    private suspend inline fun generateFileNameAsync(presenter: RecordParamsPresenter) =
        coroutineScope {
            launch(Dispatchers.IO) {
                if (File("$pathToSave/${presenter.recordFileName}.mp3").exists()) {
                    var ind = 1
                    val fileName = "${presenter.recordFileName}($ind)"

                    while (File("$pathToSave/$fileName.mp3").exists())
                        ind++

                    presenter.recordFileName = fileName
                }
            }
        }

    internal suspend inline fun DialogInterface.startRecording(
        presenter: RecordParamsPresenter,
        setRecordButtonImageChannel: Channel<Boolean>,
        micRecorderCallerChannel: Channel<String>,
        startPlaybackRecorderChannel: Channel<String>
    ) {
        generateFileNameAsync(presenter).join()
        setRecordButtonImageChannel.send(true)

        when (presenter.recordSrcSelectedItemPosition) {
            0 -> micRecorderCallerChannel
            else -> startPlaybackRecorderChannel
        }.send(presenter.recordFileName)

        dismiss()
    }
}