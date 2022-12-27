package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.text.InputType
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.CreateHiddenPasswordUIHandler
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.inject

/**
 * Dialog that creates new password
 * when user opens hidden tracks for the first time
 */

class CreateHiddenPasswordDialogFragment(target: Target, showHiddenFragmentChannel: Channel<Unit>) :
    InputDialogFragment<
            CreateHiddenPasswordUIHandler.Args,
            CreateHiddenPasswordUIHandler
            >(message = R.string.new_password, textType = InputType.TYPE_TEXT_VARIATION_PASSWORD) {
    enum class Target { CREATE, UPDATE }

    override val handlerOnOkArgs = CreateHiddenPasswordUIHandler.Args(
        target, showHiddenFragmentChannel
    )

    override val uiHandler by inject<CreateHiddenPasswordUIHandler>()
}