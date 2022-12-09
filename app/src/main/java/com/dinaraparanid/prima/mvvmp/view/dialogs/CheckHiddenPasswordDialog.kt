package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.text.InputType
import android.widget.Toast
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.CheckHiddenPasswordUIHandler
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.mvvmp.view.dialogs.InputDialog
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Dialog that creates new password
 * when user opens hidden tracks for the first time
 */

class CheckHiddenPasswordDialog(passwordHash: Int, showHiddenFragmentChannel: Channel<Unit>) :
    InputDialog<CheckHiddenPasswordUIHandler>(
        message = R.string.check_password,
        textType = InputType.TYPE_TEXT_VARIATION_PASSWORD,
    ) {
    override val uiHandler by inject<CheckHiddenPasswordUIHandler> {
        parametersOf(passwordHash, showHiddenFragmentChannel)
    }
}