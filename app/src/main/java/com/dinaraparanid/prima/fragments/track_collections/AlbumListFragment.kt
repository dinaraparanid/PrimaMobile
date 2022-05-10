package com.dinaraparanid.prima.fragments.track_collections

import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractPlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.TypicalViewPlaylistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractPlaylistListFragment] for all albums */

class AlbumListFragment : TypicalViewPlaylistListFragment() {
    /** Gets all albums from tracks */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.clear()
            itemList.addAll(
                application
                    .allTracksWithoutHidden
                    .map { it.album to it }
                    .distinctBy { it.first.trim().lowercase() }
                    .sortedBy(Pair<String, *>::first)
                    .map { (albumTitle, track) ->
                        DefaultPlaylist(
                            albumTitle,
                            AbstractPlaylist.PlaylistType.ALBUM,
                            track
                        )
                    }
            )
        }
    }
}