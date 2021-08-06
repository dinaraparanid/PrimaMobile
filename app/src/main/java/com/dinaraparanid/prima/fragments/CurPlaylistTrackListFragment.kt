package com.dinaraparanid.prima.fragments

import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.TypicalTrackListFragment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * [OnlySearchMenuTrackListFragment] for current playlist
 */

class CurPlaylistTrackListFragment : TypicalTrackListFragment() {
    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            try {
                itemList.run {
                    clear()
                    addAll((requireActivity().application as MainApplication).curPlaylist)
                    Unit
                }
            } catch (ignored: Exception) {
            }
        }
    }
}