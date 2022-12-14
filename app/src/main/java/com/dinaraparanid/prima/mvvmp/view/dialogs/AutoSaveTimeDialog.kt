package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.text.InputType
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.AfterSaveTimeUIHandler
import kotlinx.coroutines.channels.Channel
import org.koin.android.ext.android.inject

/** [InputDialog] to save new auto save time (from 5 to 99) */

class AutoSaveTimeDialog(updateAutosaveTimeButtonChannel: Channel<Unit>) :
    InputDialog<AfterSaveTimeUIHandler.AfterSaveTimeUIHandlerArgs, AfterSaveTimeUIHandler>(
        message = R.string.autosave_time,
        errorMessage = R.string.autosave_time_input_error,
        textType = InputType.TYPE_CLASS_NUMBER,
        maxLength = 2,
    ) {
    override val args = AfterSaveTimeUIHandler.AfterSaveTimeUIHandlerArgs(
        updateAutosaveTimeButtonChannel
    )

    override val uiHandler by inject<AfterSaveTimeUIHandler>()
}