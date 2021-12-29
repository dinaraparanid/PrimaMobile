package com.dinaraparanid.prima.utils.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.fragments.track_collections.PlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.InputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [InputDialog] to create new playlist.
 * Asks about playlist's title and creates
 * new playlist if it still doesn't exists.
 */

internal class NewPlaylistDialog(fragment: PlaylistListFragment) : InputDialog(
    R.string.playlist_title,
    { input ->
        coroutineScope {
            launch(Dispatchers.IO) {
                CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .addPlaylistAsync(CustomPlaylist.Entity(0, input))
                    .join()

                fragment.updateUIOnChangeContentAsync()
            }
        }
    },
    R.string.playlist_exists
)