package com.dinaraparanid.prima.fragments

import android.os.Build
import android.provider.MediaStore
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * [OnlySearchMenuTrackListFragment] for all tracks on user's device
 */

class DefaultTrackListFragment : OnlySearchMenuTrackListFragment() {
    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            try {
                if ((requireActivity().application as MainApplication).checkAndRequestPermissions()) {
                    val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
                    val order = MediaStore.Audio.Media.TITLE + " ASC"

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
                        null,
                        order
                    ).use { cursor ->
                        itemList.clear()

                        if (cursor != null) {
                            (requireActivity().application as MainApplication)
                                .addTracksFromStorage(cursor, itemList)
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
        }
    }
}