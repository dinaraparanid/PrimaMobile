package com.dinaraparanid.prima.utils.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.fragments.track_lists.CustomPlaylistTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.InputDialog
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [InputDialog] for renaming playlists
 * @param fragment [CustomPlaylistTrackListFragment] from which we call this dialog
 */

internal class RenamePlaylistDialog(fragment: CustomPlaylistTrackListFragment) : InputDialog(
    message = R.string.playlist_title,
    okAction = { input ->
        fragment.runOnIOThread {
            val favouriteRepository = FavouriteRepository.instance
            favouriteRepository
                .getPlaylistAsync(fragment.playlistTitle, AbstractPlaylist.PlaylistType.CUSTOM.ordinal)
                .await()
                ?.let { (id) -> favouriteRepository.updatePlaylistAsync(id, input).join() }

            CustomPlaylistsRepository
                .instance
                .updatePlaylistAsync(fragment.playlistTitle, input)

            launch(Dispatchers.Main) { fragment.renameTitle(input) }
        }
    },
    errorMessage = R.string.playlist_exists,
    errorAction = { input ->
        val favouriteRepository = FavouriteRepository.instance
        favouriteRepository
            .getPlaylistAsync(input, AbstractPlaylist.PlaylistType.CUSTOM.ordinal)
            .await()
            ?.let { (id) -> favouriteRepository.updatePlaylistAsync(id, fragment.playlistTitle) }
    }
)