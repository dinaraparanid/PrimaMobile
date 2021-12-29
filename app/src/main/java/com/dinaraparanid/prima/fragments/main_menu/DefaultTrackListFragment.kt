package com.dinaraparanid.prima.fragments.main_menu

import android.os.Build
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.TypicalViewTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [TypicalViewTrackListFragment] for all tracks on user's device
 */

class DefaultTrackListFragment : TypicalViewTrackListFragment() {
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_default_track_list, menu)

        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this)

        menu.findItem(R.id.find_by).setOnMenuItemClickListener { selectSearch() }

        menu.findItem(R.id.media_scanner).setOnMenuItemClickListener {
            (requireActivity().application as MainApplication).startMediaScanning()
            true
        }
    }

    override suspend fun loadAsync() = coroutineScope {
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

                    if (cursor != null)
                        application.addTracksFromStoragePaired(cursor, itemList)
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }
}