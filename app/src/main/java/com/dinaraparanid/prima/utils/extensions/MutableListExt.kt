package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.core.AbstractTrack

/** Gets tracks for buttons in "Guess The Melody" game */
internal fun MutableList<AbstractTrack>.getGTMTracks(curInd: Int = 0) =
    ((this - get(curInd)).shuffled().take(3) + get(curInd)).toPlaylist()