package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R

/** [TypicalViewTrackListFragment] with only search option in menu */

abstract class OnlySearchMenuTrackListFragment : TypicalViewTrackListFragment() {
    final override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_track_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
        menu.findItem(R.id.find_by).setOnMenuItemClickListener { selectSearch() }
    }
}