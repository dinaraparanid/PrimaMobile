package com.dinaraparanid.prima.fragments

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.SearchView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.polymorphism.TrackListFragment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DefaultTrackListFragment : TrackListFragment() {
    companion object {
        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            _firstToHighlight: String? = null
        ): DefaultTrackListFragment = DefaultTrackListFragment().apply {
            arguments = Bundle().apply {
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
                putString(START_KEY, _firstToHighlight ?: NO_HIGHLIGHT)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
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
                    while (cursor.moveToNext()) {
                        itemList.add(
                            Track(
                                cursor.getLong(0),
                                cursor.getString(1),
                                cursor.getString(2),
                                cursor.getString(3),
                                cursor.getString(4),
                                cursor.getLong(5),
                                displayName = cursor.getString(6),
                                relativePath = when {
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                                        cursor.getString(7)
                                    else -> null
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}