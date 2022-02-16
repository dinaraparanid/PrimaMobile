package com.dinaraparanid.prima.viewmodels.androidx

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.dinaraparanid.prima.utils.web.genius.songs_response.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ViewModel] for
 * [com.dinaraparanid.prima.fragments.playing_panel_fragments.TrackChangeFragment]
 */

class TrackChangeViewModel : ViewModel() {
    /** loading for first time to run search in genius */
    internal val wasLoadedFlow = MutableStateFlow(false)
    internal val albumImageUrlFlow = MutableStateFlow<String?>(null)
    internal val albumImagePathFlow = MutableStateFlow<Uri?>(null)
    internal val trackListFlow = MutableStateFlow(mutableListOf<Song>())

    private val _titleFlow = MutableStateFlow("")
    private val _artistFlow = MutableStateFlow("")
    private val _albumFlow = MutableStateFlow("")
    private val _trackNumberInAlbumFlow = MutableStateFlow<Byte>(-1)

    internal val titleFlow
        get() = _titleFlow.asStateFlow()

    internal val artistFlow
        get() = _artistFlow.asStateFlow()

    internal val albumFlow
        get() = _albumFlow.asStateFlow()

    internal val trackNumberInAlbumFlow
        get() = _trackNumberInAlbumFlow.asStateFlow()

    /**
     * Loads content for fragment
     * @param wasLoaded was fragment loaded for first time to run search in genius
     * @param albumImagePath path to album's image if it's located in storage or null
     * @param albumImageUri uri to album's image if it's not located in storage or null
     * @param title state of changeable track's title
     * @param artist state of changeable track's artist
     * @param album state of changeable track's album
     * @param trackNumberInAlbum number of track in album or -1 if no info
     * @param trackList [Array] with [Song]s that are similar to current track's state
     */

    internal fun load(
        wasLoaded: Boolean?,
        albumImagePath: String?,
        albumImageUri: Uri?,
        title: String,
        artist: String,
        album: String,
        trackNumberInAlbum: Byte,
        trackList: Array<Song>?
    ) {
        wasLoadedFlow.value = wasLoaded ?: false
        albumImageUrlFlow.value = albumImagePath
        albumImagePathFlow.value = albumImageUri
        _titleFlow.value = title
        _artistFlow.value = artist
        _albumFlow.value = album
        _trackNumberInAlbumFlow.value = trackNumberInAlbum
        trackList?.let { trackListFlow.value = it.toMutableList() }
    }
}