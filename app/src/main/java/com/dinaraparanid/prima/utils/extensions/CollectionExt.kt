package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track

fun Collection<Track>.toPlaylist(): Playlist = Playlist(tracks = toMutableList())