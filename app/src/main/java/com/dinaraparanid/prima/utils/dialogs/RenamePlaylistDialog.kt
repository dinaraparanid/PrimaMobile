package com.dinaraparanid.prima.utils.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.fragments.CustomPlaylistTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.InputDialog

/**
 * [InputDialog] for renaming playlists
 * @param fragment [CustomPlaylistTrackListFragment] from which we call this dialog
 */

internal class RenamePlaylistDialog(fragment: CustomPlaylistTrackListFragment) : InputDialog(
    R.string.playlist_title,
    { input ->
        CustomPlaylistsRepository
            .instance
            .updatePlaylist(fragment.mainLabel, input)

        fragment.renameTitle(input)
    },
    R.string.playlist_exists
)