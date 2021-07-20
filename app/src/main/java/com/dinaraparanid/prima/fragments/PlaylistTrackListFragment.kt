package com.dinaraparanid.prima.fragments

import android.os.Build
import android.provider.MediaStore
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import kotlinx.coroutines.*

class PlaylistTrackListFragment : OnlySearchMenuTrackListFragment() {
    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            try {
                itemList.run {
                    clear()
                    addAll(
                        when {
                            (requireActivity().application as MainApplication).checkAndRequestPermissions() -> {
                                val selection = "${MediaStore.Audio.Media.ALBUM} = ?"
                                val order = MediaStore.Audio.Media.TITLE + " ASC"
                                val trackList = mutableListOf<Track>()

                                val projection = mutableListOf(
                                    MediaStore.Audio.Media._ID,
                                    MediaStore.Audio.Media.TITLE,
                                    MediaStore.Audio.Media.ARTIST,
                                    MediaStore.Audio.Media.ALBUM,
                                    MediaStore.Audio.Media.DATA,
                                    MediaStore.Audio.Media.DURATION,
                                    MediaStore.Audio.Media.DISPLAY_NAME
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
                            }

                            else -> DefaultPlaylist()
                        }
                    )
                    Unit
                }
            } catch (e: Exception) {
            }
        }
    }
}