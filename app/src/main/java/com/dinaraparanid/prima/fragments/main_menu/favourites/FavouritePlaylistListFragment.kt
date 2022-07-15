package com.dinaraparanid.prima.fragments.main_menu.favourites

import com.dinaraparanid.prima.databases.entities.favourites.FavouritePlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.fragments.DefaultMenuPlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.TypicalViewPlaylistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [TypicalViewPlaylistListFragment] for favourite playlists
 * (both albums and custom playlists)
 */

class FavouritePlaylistListFragment : DefaultMenuPlaylistListFragment() {

    /** Loads all favourite playlists (both albums and custom playlists) */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.run {
                val task = FavouriteRepository
                    .getInstanceSynchronized()
                    .getPlaylistsAsync()

                clear()
                addAll(
                    task.await().map { (_, title, typeIndexed) ->
                        val type = AbstractPlaylist.PlaylistType.values()[typeIndexed]

                        FavouritePlaylist(title, type).apply {
                            when (type) {
                                AbstractPlaylist.PlaylistType.ALBUM -> application
                                    .allTracksWithoutHidden
                                    .firstOrNull { it.album == title }
                                    ?.let { track -> add(track) }

                                AbstractPlaylist.PlaylistType.CUSTOM -> CustomPlaylistsRepository
                                    .getInstanceSynchronized()
                                    .getFirstTrackOfPlaylistAsync(title)
                                    .await()
                                    ?.let { track -> add(track) }

                                AbstractPlaylist.PlaylistType.GTM ->
                                    throw IllegalArgumentException("GTM Playlist in Favourites")
                            }
                        }
                    }
                )
            }
        }
    }
}