package com.dinaraparanid.prima.mvvmp.view.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.NewPlaylistUIHandler
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.inject

/**
 * [InputDialog] to create new playlist.
 * Asks about playlist's title and creates
 * new playlist if it still doesn't exists.
 */

class NewPlaylistDialog(updateFragmentChannel: Channel<Unit>) :
    InputDialog<NewPlaylistUIHandler.NewPlaylistUIHandlerArgs, NewPlaylistUIHandler>(
        message = R.string.playlist_title,
        errorMessage = R.string.playlist_exists,
    ) {
    override val handlerOnOkArgs = NewPlaylistUIHandler.NewPlaylistUIHandlerArgs(updateFragmentChannel)
    override val uiHandler by inject<NewPlaylistUIHandler>()
}