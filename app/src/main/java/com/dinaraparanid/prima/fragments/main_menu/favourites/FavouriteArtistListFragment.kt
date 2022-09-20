package com.dinaraparanid.prima.fragments.main_menu.favourites

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractArtistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractArtistListFragment] for user's favourite artists */

class FavouriteArtistListFragment : AbstractArtistListFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_search, menu)
                (menu.findItem(R.id.find).actionView as SearchView)
                    .setOnQueryTextListener(this@FavouriteArtistListFragment)
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = true
        })
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