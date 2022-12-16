package com.dinaraparanid.prima.mvvmp.view.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.NewFolderUIHandler
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.inject

/**
 * [InputDialog] to create new folder.
 * Asks about folder's title and creates
 * new playlist or complains about error.
 */

class NewFolderDialog(path: String, updateFragmentUIChannel: Channel<Unit>) :
    InputDialog<NewFolderUIHandler.NewFolderUIHandlerArgs, NewFolderUIHandler>(
        message = R.string.folder_title,
        errorMessage = R.string.folder_create_error,
    ) {
    override val handlerOnOkArgs = NewFolderUIHandler.NewFolderUIHandlerArgs(
        context = requireContext(),
        path = path,
        updateFragmentUIChannel = updateFragmentUIChannel
    )

    override val uiHandler by inject<NewFolderUIHandler>()
}