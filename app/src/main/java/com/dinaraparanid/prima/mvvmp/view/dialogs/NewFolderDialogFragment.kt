package com.dinaraparanid.prima.mvvmp.view.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.NewFolderUIHandler
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.inject

/**
 * [InputDialogFragment] to create new folder.
 * Asks about folder's title and creates
 * new playlist or complains about error.
 */

class NewFolderDialogFragment(path: String, updateFragmentUIChannel: Channel<Unit>) :
    InputDialogFragment<NewFolderUIHandler.Args, NewFolderUIHandler>(
        message = R.string.folder_title,
        errorMessage = R.string.folder_create_error,
    ) {
    override val handlerOnOkArgs = NewFolderUIHandler.Args(
        context = requireContext(),
        path = path,
        updateFragmentUIChannel = updateFragmentUIChannel
    )

    override val uiHandler by inject<NewFolderUIHandler>()
}