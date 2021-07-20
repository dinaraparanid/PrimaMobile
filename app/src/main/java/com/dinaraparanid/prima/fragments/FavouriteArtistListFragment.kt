package com.dinaraparanid.prima.fragments

import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.polymorphism.ArtistListFragment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class FavouriteArtistListFragment : ArtistListFragment() {
    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async {
            try {
                val task = FavouriteRepository.instance.artistsAsync

                itemList.run {
                    clear()
                    addAll(task.await())
                    Unit
                }
            } catch (e: Exception) {
            }
        }
    }
}