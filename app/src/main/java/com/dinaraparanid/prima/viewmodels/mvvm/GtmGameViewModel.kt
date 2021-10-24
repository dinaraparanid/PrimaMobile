package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.fragments.guess_the_melody.GtmGameFragment
import java.lang.ref.WeakReference

/**
 * MVVM [ViewModel] for
 * [com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment]
 */

class GtmGameViewModel(
    private val fragment: WeakReference<GtmGameFragment>,
    @JvmField internal val trackNumber: Int,
    @JvmField internal val tracks: List<AbstractTrack>,
    private val correctTrack: AbstractTrack,
    @JvmField internal val score: Int = 0,
): ViewModel() {

}