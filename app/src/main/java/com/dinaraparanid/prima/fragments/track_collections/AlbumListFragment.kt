package com.dinaraparanid.prima.fragments.track_collections

import android.provider.MediaStore
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.TypicalViewPlaylistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractPlaylistListFragment] for all albums */

class AlbumListFragment : TypicalViewPlaylistListFragment() {
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                requireActivity().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Albums.ALBUM),
                    null,
                    null,
                    MediaStore.Audio.Media.ALBUM + " ASC"
                ).use { cursor ->
                    itemList.clear()

                    if (cursor != null) {
                        val playlistList = mutableListOf<AbstractPlaylist>()

                        while (cursor.moveToNext()) {
                            val albumTitle = cursor.getString(0)

                            application.allTracks
                                .firstOrNull { it.album == albumTitle }
                                ?.let { track ->
                                    playlistList.add(
                                        DefaultPlaylist(
                                            albumTitle,
                                            AbstractPlaylist.PlaylistType.ALBUM,
                                            track
                                        )
                                    )
                                }
                        }

                        itemList.addAll(playlistList.distinctBy(AbstractPlaylist::title))
                    }
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }
}