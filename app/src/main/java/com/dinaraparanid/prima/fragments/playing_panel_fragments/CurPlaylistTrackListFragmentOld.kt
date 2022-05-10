package com.dinaraparanid.prima.fragments.playing_panel_fragments

import android.os.Bundle
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.fragments.OnlySearchMenuTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [OnlySearchMenuTrackListFragment] for current playlist */

@Deprecated("Now using BottomSheetDialogFragment")
class CurPlaylistTrackListFragmentOld : OnlySearchMenuTrackListFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override suspend fun loadAsync() = coroutineScope {
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