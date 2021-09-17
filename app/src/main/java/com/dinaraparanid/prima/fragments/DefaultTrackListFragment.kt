package com.dinaraparanid.prima.fragments

import android.os.Build
import android.provider.MediaStore
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.TypicalTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [OnlySearchMenuTrackListFragment] for all tracks on user's device
 */

class DefaultTrackListFragment : TypicalTrackListFragment() {
    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                val selection = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.IS_AUDIOBOOK} != 0"

                    else -> "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                }

                val order = "${
                    when (Params.instance.tracksOrder.first) {
                        Params.Companion.TracksOrder.TITLE -> MediaStore.Audio.Media.TITLE
                        Params.Companion.TracksOrder.ARTIST -> MediaStore.Audio.Media.ARTIST
                        Params.Companion.TracksOrder.ALBUM -> MediaStore.Audio.Media.ALBUM
                        Params.Companion.TracksOrder.DATE -> MediaStore.Audio.Media.DATE_ADDED
                    }
                } ${if (Params.instance.tracksOrder.second) "ASC" else "DESC"}"

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
                    null,
                    order
                ).use { cursor ->
                    itemList.clear()

                    if (cursor != null) {
                        (requireActivity().application as MainApplication)
                            .addTracksFromStorage(cursor, itemList)
                    }
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }
}