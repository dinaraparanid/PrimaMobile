package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.databases.repositories.CoversRepository
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** [InputDialogUIHandler] for RenamePlaylistDialogFragment */

class RenamePlaylistUIHandler : InputDialogUIHandler<RenamePlaylistUIHandler.Args> {
    data class Args(
        val playlistTitle: String,
        val updateFragmentTitleChannel: Channel<String?>
    ) : InputDialogUIHandler.Args

    private suspend inline fun CoroutineScope.getPlaylistWithInputTitleAsync(input: String) =
        coroutineScope {
            async(Dispatchers.IO) {
                CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .getPlaylistAsync(input)
                    .await()
            }
        }

    private suspend inline fun Args.updateRepositories(input: String) =
        coroutineScope {
            launch(Dispatchers.IO) {
                CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .updatePlaylistAsync(
                        oldTitle = playlistTitle,
                        newTitle = input
                    )
            }

            launch(Dispatchers.IO) {
                FavouriteRepository
                    .getInstanceSynchronized()
                    .updatePlaylistAsync(
                        oldTitle = playlistTitle,
                        type = AbstractPlaylist.PlaylistType.CUSTOM,
                        newTitle = input
                    )
            }

            launch(Dispatchers.IO) {
                CoversRepository
                    .getInstanceSynchronized()
                    .updatePlaylistTitleAsync(
                        oldTitle = playlistTitle,
                        newTitle = input
                    )
            }
        }

    override suspend fun Args.onOkAsync(
        input: String,
        dialog: DialogInterface,
    ) = coroutineScope {
        val playlistWithInputTitle = getPlaylistWithInputTitleAsync(input).await()

        when (playlistWithInputTitle) {
            null -> {
                updateRepositories(input)
                input
            }

            else -> null
        }.let { updateFragmentTitleChannel.send(it) }
    }
}