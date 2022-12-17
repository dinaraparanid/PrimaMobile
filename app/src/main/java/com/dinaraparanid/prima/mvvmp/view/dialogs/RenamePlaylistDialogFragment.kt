package com.dinaraparanid.prima.mvvmp.view.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.RenamePlaylistUIHandler
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.inject

/** [InputDialogFragment] for renaming playlists */

class RenamePlaylistDialogFragment(
    playlistTitle: String,
    updateFragmentTitleChannel: Channel<String?>
) : InputDialogFragment<
        RenamePlaylistUIHandler.Args,
        RenamePlaylistUIHandler
>(
    message = R.string.playlist_title,
    errorMessage = R.string.playlist_exists,
) {
    override val uiHandler by inject<RenamePlaylistUIHandler>()

    override val handlerOnOkArgs = RenamePlaylistUIHandler.Args(
        playlistTitle, updateFragmentTitleChannel
    )
}