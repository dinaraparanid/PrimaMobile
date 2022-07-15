package com.dinaraparanid.prima.fragments.main_menu

import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.HiddenRepository
import com.dinaraparanid.prima.utils.polymorphism.fragments.LoadAllArtistsFromStorageListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [LoadAllArtistsFromStorageListFragment] that loads all artists */

class DefaultArtistListFragment : LoadAllArtistsFromStorageListFragment() {

    /** Loads all artists from [MediaStore] */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            val allArtistsTask = loadAllArtistsFromStorage()
            val hiddenArtistsTask = HiddenRepository.getInstanceSynchronized().getArtistsAsync()

            val allArtists = allArtistsTask.await()
            val hiddenArtists = hiddenArtistsTask.await()

            itemList.addAll(allArtists.filter { it !in hiddenArtists })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }
}