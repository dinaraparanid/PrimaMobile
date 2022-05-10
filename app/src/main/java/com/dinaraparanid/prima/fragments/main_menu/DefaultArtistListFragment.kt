package com.dinaraparanid.prima.fragments.main_menu

import android.provider.MediaStore
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractArtistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractArtistListFragment] for all artists on user's device */

class DefaultArtistListFragment : AbstractArtistListFragment() {

    /** Loads all artists from [MediaStore] */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                requireActivity().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Artists.ARTIST),
                    null,
                    null,
                    MediaStore.Audio.Media.ARTIST + " ASC"
                ).use { cursor ->
                    itemList.clear()

                    if (cursor != null) {
                        val artistList = mutableListOf<Artist>()

                        while (cursor.moveToNext())
                            artistList.add(Artist(cursor.getString(0)))

                        itemList.addAll(artistList.distinctBy(Artist::name))
                    }
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }
}