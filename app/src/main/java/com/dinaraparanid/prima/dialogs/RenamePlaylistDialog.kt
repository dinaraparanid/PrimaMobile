package com.dinaraparanid.prima.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CoversRepository
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.mvvmp.view.dialogs.InputDialog
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractCustomPlaylistTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [InputDialog] for renaming playlists
 * @param fragment [AbstractCustomPlaylistTrackListFragment] from which we call this dialog
 */

internal class RenamePlaylistDialog(fragment: AbstractCustomPlaylistTrackListFragment) :
    InputDialog(
        message = R.string.playlist_title,
        okAction = { input, _ ->
            fragment.runOnIOThread {
                FavouriteRepository
                    .getInstanceSynchronized()
                    .getPlaylistAsync(
                        fragment.playlistTitle,
                        AbstractPlaylist.PlaylistType.CUSTOM.ordinal
                    )
                    .await()
                    ?.let { (id) ->
                        FavouriteRepository
                            .getInstanceSynchronized()
                            .updatePlaylistAsync(id, title = input)
                            .join()
                    }

                CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .updatePlaylistAsync(
                        oldTitle = fragment.playlistTitle,
                        newTitle = input
                    )

                CoversRepository
                    .getInstanceSynchronized()
                    .updatePlaylistTitleAsync(
                        oldTitle = fragment.playlistTitle,
                        newTitle = input
                    )

                launch(Dispatchers.Main) { fragment.renameTitle(input) }
            }
        },
        errorMessage = R.string.playlist_exists,
        errorAction = { input ->
            FavouriteRepository
                .getInstanceSynchronized()
                .getPlaylistAsync(input, AbstractPlaylist.PlaylistType.CUSTOM.ordinal)
                .await()
                ?.let { (id) ->
                    FavouriteRepository
                        .getInstanceSynchronized()
                        .updatePlaylistAsync(id, fragment.playlistTitle)
                }
        }
    )