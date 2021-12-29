package com.dinaraparanid.prima.fragments.favourites

import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [OnlySearchMenuTrackListFragment] for user's favourite tracks
 */

class FavouriteTrackListFragment : OnlySearchMenuTrackListFragment() {
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