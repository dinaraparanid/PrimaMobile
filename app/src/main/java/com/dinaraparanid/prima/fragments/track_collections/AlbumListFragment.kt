package com.dinaraparanid.prima.fragments.track_collections

import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenPlaylist
import com.dinaraparanid.prima.databases.repositories.HiddenRepository
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.fragments.DefaultMenuPlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.TypicalViewPlaylistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [DefaultMenuPlaylistListFragment] for all albums */

class AlbumListFragment : DefaultMenuPlaylistListFragment() {

    /** Gets all albums from tracks */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            val hiddenAlbumsTask = HiddenRepository.getInstanceSynchronized().getAlbumsAsync()

            itemList.clear()
            itemList.addAll(
                application
                    .allTracksWithoutHidden
                    .map { it.album to it }
                    .distinctBy { it.first.trim().lowercase() }
                    .sortedBy(Pair<String, *>::first)
                    .let {
                        val hiddenAlbumTitles = hiddenAlbumsTask
                            .await()
                            .map(HiddenPlaylist.Entity::title)

                        it
                            .filter { (albumTitle, _) -> albumTitle !in hiddenAlbumTitles }
                            .map { (albumTitle, track) ->
                                DefaultPlaylist(
                                    albumTitle,
                                    AbstractPlaylist.PlaylistType.ALBUM,
                                    track
                                )
                            }
                    }
            )
        }
    }
}