package com.dinaraparanid.prima.fragments.main_menu.favourites

import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.fragments.OnlySearchMenuTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [OnlySearchMenuTrackListFragment] for user's favourite tracks */

class FavouriteTrackListFragment : OnlySearchMenuTrackListFragment() {

    /** Loads all favourite tracks */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                itemList.apply {
                    val task = FavouriteRepository
                        .getInstanceSynchronized()
                        .getTracksAsync()

                    clear()
                    addAll(Params.sortedTrackList(task.await().enumerated()))
                }
            } catch (ignored: Exception) {
            }
        }
    }
}