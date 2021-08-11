package com.dinaraparanid.prima.fragments

import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.TypicalTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [OnlySearchMenuTrackListFragment] for current playlist
 */

class CurPlaylistTrackListFragment : TypicalTrackListFragment() {
    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
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