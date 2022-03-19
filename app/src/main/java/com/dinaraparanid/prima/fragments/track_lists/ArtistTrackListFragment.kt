package com.dinaraparanid.prima.fragments.track_lists

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import kotlinx.coroutines.*

/** [OnlySearchMenuTrackListFragment] with artist's tracks */

class ArtistTrackListFragment : OnlySearchMenuTrackListFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            val task = async(Dispatchers.IO) {
                try {
                    val selection = "${MediaStore.Audio.Media.ARTIST} = ?"

                    val order = "${
                        when (Params.getInstanceSynchronized().tracksOrder.first) {
                            Params.Companion.TracksOrder.TITLE -> MediaStore.Audio.Media.TITLE
                            Params.Companion.TracksOrder.ARTIST -> MediaStore.Audio.Media.ARTIST
                            Params.Companion.TracksOrder.ALBUM -> MediaStore.Audio.Media.ALBUM
                            Params.Companion.TracksOrder.DATE -> MediaStore.Audio.Media.DATE_ADDED
                            Params.Companion.TracksOrder.POS_IN_ALBUM -> MediaStore.Audio.Media.TRACK
                        }
                    } ${if (Params.getInstanceSynchronized().tracksOrder.second) "ASC" else "DESC"}"

                    val trackList = mutableListOf<AbstractTrack>()

                    val projection = mutableListOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.DATE_ADDED,
                        MediaStore.Audio.Media.TRACK
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
                        if (cursor != null) {
                            (requireActivity().application as MainApplication)
                                .addTracksFromStorage(cursor, trackList)
                        }
                    }

                    trackList.distinctBy { it.path }.toPlaylist()
                } catch (e: Exception) {
                    // Permission to storage not given
                    DefaultPlaylist()
                }
            }

            itemList.run {
                clear()
                itemList.addAll(task.await().enumerated())
                Unit
            }
        }
    }
}