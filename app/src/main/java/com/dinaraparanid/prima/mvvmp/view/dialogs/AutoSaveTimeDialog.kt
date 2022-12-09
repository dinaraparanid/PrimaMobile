package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.text.InputType
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.AfterSaveTimeUIHandler
import org.koin.android.ext.android.inject

/** [InputDialog] to save new auto save time (from 5 to 99) */

@Suppress("Reformat")
class AutoSaveTimeDialog : InputDialog<AfterSaveTimeUIHandler>(
    message = R.string.autosave_time,
    errorMessage = R.string.autosave_time_input_error,
    textType = InputType.TYPE_CLASS_NUMBER,
    maxLength = 2
) { override val uiHandler by inject<AfterSaveTimeUIHandler>() }