package com.dinaraparanid.prima.fragments

import android.os.Build
import android.provider.MediaStore
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * [OnlySearchMenuTrackListFragment] for tracks of some playlist or album
 */

class PlaylistTrackListFragment : OnlySearchMenuTrackListFragment() {
    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            itemList.run {
                clear()
                addAll(
                    try {
                        val selection = "${MediaStore.Audio.Media.ALBUM} = ?"

                        val order = "${
                            when (Params.instance.tracksOrder.first) {
                                Params.Companion.TracksOrder.TITLE -> MediaStore.Audio.Media.TITLE
                                Params.Companion.TracksOrder.ARTIST -> MediaStore.Audio.Media.ARTIST
                                Params.Companion.TracksOrder.ALBUM -> MediaStore.Audio.Media.ALBUM
                                Params.Companion.TracksOrder.DATE -> MediaStore.Audio.Media.DATE_ADDED
                            }
                        } ${if (Params.instance.tracksOrder.second) "ASC" else "DESC"}"

                        val trackList = mutableListOf<Track>()

                        val projection = mutableListOf(
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            MediaStore.Audio.Media.DATE_ADDED
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)

                        requireActivity().contentResolver.query(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            projection.toTypedArray(),
                            selection,
                            arrayOf(mainLabelCurText),
                            order
                        ).use { cursor ->
                            if (cursor != null)
                                (requireActivity().application as MainApplication)
                                    .addTracksFromStorage(cursor, trackList)
                        }

                        trackList.distinctBy { it.path }.toPlaylist()
                    } catch (e: Exception) {
                        // Permission to storage not given
                        DefaultPlaylist()
                    }
                )
                Unit
            }
        }
    }
}