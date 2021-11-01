package com.dinaraparanid.prima.fragments

import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.polymorphism.AbstractArtistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [AbstractArtistListFragment] for user's favourite artists
 */

class FavouriteArtistListFragment : AbstractArtistListFragment() {
    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                val task = FavouriteRepository.instance.getArtistsAsync()

                itemList.run {
                    clear()
                    addAll(task.await())
                    Unit
                }
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fragmentActivity.setSelectButtonVisibility(true)
    }

    override fun onStop() {
        super.onStop()
        fragmentActivity.setSelectButtonVisibility(false)
    }
}