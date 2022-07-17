package com.dinaraparanid.prima.fragments.hidden

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_track_list_show, menu)

        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
        menu.findItem(R.id.find_by).setOnMenuItemClickListener { selectSearch() }

        menu.findItem(R.id.show).setOnMenuItemClickListener {
            fragmentActivity.removeArtistFromHidden(HiddenArtist(name = mainLabelCurText))
            true
        }
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