package com.dinaraparanid.prima.utils.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.fragments.PlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.InputDialog
import com.dinaraparanid.prima.utils.polymorphism.updateContent

internal class NewPlaylistDialog(fragment: PlaylistListFragment) : InputDialog(
    R.string.playlist_title,
    { input ->
        CustomPlaylistsRepository
            .instance
            .addPlaylist(CustomPlaylist.Entity(input))

        Thread.sleep(300)
        fragment.load()
        fragment.updateContent(fragment.loaderContent)
    },
    R.string.playlist_exists
)