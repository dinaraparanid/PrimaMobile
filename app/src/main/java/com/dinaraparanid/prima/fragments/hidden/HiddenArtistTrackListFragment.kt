package com.dinaraparanid.prima.fragments.hidden

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.entities.hidden.HiddenArtist
import com.dinaraparanid.prima.databases.repositories.HiddenRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.enumerated
import com.dinaraparanid.prima.utils.polymorphism.fragments.TypicalViewTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Shows tracks of hidden artist */

class HiddenArtistTrackListFragment : TypicalViewTrackListFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_track_list_show, menu)
                (menu.findItem(R.id.find).actionView as SearchView)
                    .setOnQueryTextListener(this@HiddenArtistTrackListFragment)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.find_by -> selectSearch()
                    R.id.show -> fragmentActivity.removeArtistFromHidden(HiddenArtist(name = mainLabelCurText))
                }

                return true
            }
        })
    }

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.clear()
            itemList.addAll(
                Params.sortedTrackList(
                    HiddenRepository
                        .getInstanceSynchronized()
                        .getTracksOfArtistAsync(artist = mainLabelCurText)
                        .await()
                        .enumerated()
                )
            )
        }
    }
}