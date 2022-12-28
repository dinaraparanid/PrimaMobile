package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.provider.MediaStore
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.entities.Artist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** [AbstractArtistListFragment] that loads all artists from [MediaStore] */

abstract class LoadAllArtistsFromStorageListFragment : AbstractArtistListFragment() {

    /**
     * Loads all artists from [MediaStore] asynchronously
     * @return all artists found in storage
     */

    protected suspend fun loadAllArtistsFromStorageAsync() = coroutineScope {
        async(Dispatchers.IO) {
            try {
                requireActivity().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Artists.ARTIST),
                    null,
                    null,
                    MediaStore.Audio.Media.ARTIST + " ASC"
                ).use { cursor ->
                    when {
                        cursor != null -> {
                            val artistList = mutableListOf<Artist>()

                            while (cursor.moveToNext())
                                artistList.add(Artist(cursor.getString(0).let {
                                    when (it) {
                                        "<unknown>" -> resources.getString(R.string.unknown_artist)
                                        else -> it
                                    }
                                }))

                            artistList.distinctBy(Artist::name)
                        }

                        else -> listOf()
                    }
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
                listOf()
            }
        }
    }
}