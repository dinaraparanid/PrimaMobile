package com.dinaraparanid.prima.utils.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.fragments.PlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.InputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * [InputDialog] to create new playlist.
 * Asks about playlist title and creates
 * new playlist if it still doesn't exists.
 */

internal class NewPlaylistDialog(fragment: PlaylistListFragment) : InputDialog(
    R.string.playlist_title,
    { input ->
        runBlocking {
            launch(Dispatchers.IO) {
                CustomPlaylistsRepository
                    .instance
                    .addPlaylistAsync(CustomPlaylist.Entity(0, input))
                    .join()

                fragment.loadAsync().join()
                fragment.updateUI(fragment.loaderContent)
            }
        }
    },
    R.string.playlist_exists
)