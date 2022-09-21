package com.dinaraparanid.prima.fragments.main_menu.favourites

import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractArtistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractArtistListFragment] for user's favourite artists */

class FavouriteArtistListFragment : AbstractArtistListFragment() {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this@FavouriteArtistListFragment)
    }

    /** Loads all favourite artists */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                val task = FavouriteRepository
                    .getInstanceSynchronized()
                    .getArtistsAsync()

                itemList.run {
                    clear()
                    addAll(task.await())
                }
            } catch (ignored: Exception) {
            }
        }
    }
}