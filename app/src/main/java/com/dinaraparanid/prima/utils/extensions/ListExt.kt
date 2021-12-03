package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack

/** Gets tracks for buttons in "Guess The Melody" game */
internal fun List<AbstractTrack>.getGTMTracks(curInd: Int = 0) =
    ((this - get(curInd)).shuffled().take(3) + get(curInd)).toPlaylist()