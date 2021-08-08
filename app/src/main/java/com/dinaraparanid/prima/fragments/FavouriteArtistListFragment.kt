package com.dinaraparanid.prima.fragments

import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.polymorphism.AbstractArtistListFragment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * [AbstractArtistListFragment] for user's favourite artists
 */

class FavouriteArtistListFragment : AbstractArtistListFragment() {
    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async {
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
}