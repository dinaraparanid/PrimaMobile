package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.text.InputType
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.SleepUIHandler
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.inject

/** Dialog to start countdown for playback sleeping */

class SleepDialog(startSleepServiceChannel: Channel<Short>) :
    InputDialogFragment<SleepUIHandler.Args, SleepUIHandler>(
        message = R.string.sleep_time,
        errorMessage = R.string.sleep_time_error,
        textType = InputType.TYPE_NUMBER_FLAG_SIGNED,
        maxInputLength = 3
    ) {
    override val uiHandler by inject<SleepUIHandler>()
    override val handlerOnOkArgs = SleepUIHandler.Args(startSleepServiceChannel)
}