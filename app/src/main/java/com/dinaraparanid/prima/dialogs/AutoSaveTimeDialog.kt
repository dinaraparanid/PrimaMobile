package com.dinaraparanid.prima.dialogs

import android.text.InputType
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.InputDialog
import com.dinaraparanid.prima.viewmodels.mvvm.SettingsViewModel
import java.lang.ref.WeakReference

/** [InputDialog] to save new auto save time (from 5 to 99) */
internal class AutoSaveTimeDialog(viewModel: WeakReference<SettingsViewModel>) : InputDialog(
    message = R.string.autosave_time,
    okAction = { input, _ ->
        input
            .toByte()
            .takeIf { it >= 5 }
            ?.toInt()
            ?.let {
                Params.getInstanceSynchronized().autoSaveTime.set(it)
                StorageUtil.getInstanceSynchronized().storeAutoSaveTime(it)
            }
            ?: throw Exception()

        viewModel.unchecked.updateAutoSaveTimeTitle()
    },
    errorMessage = R.string.autosave_time_input_error,
    textType = InputType.TYPE_CLASS_NUMBER,
    maxLength = 2
)