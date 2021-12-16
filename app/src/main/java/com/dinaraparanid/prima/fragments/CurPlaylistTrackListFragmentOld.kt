package com.dinaraparanid.prima.fragments

import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [OnlySearchMenuTrackListFragment] for current playlist
 */

@Deprecated("Now using BottomSheetDialogFragment")
class CurPlaylistTrackListFragmentOld : OnlySearchMenuTrackListFragment() {
    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                itemList.run {
                    clear()
                    addAll(application.curPlaylist.enumerated())
                }
            } catch (ignored: Exception) {
            }
        }
    }
}