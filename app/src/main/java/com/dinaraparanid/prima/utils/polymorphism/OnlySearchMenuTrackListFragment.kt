package com.dinaraparanid.prima.utils.polymorphism

import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R

/**
 * [TrackListFragment] with only search option in menu
 */

abstract class OnlySearchMenuTrackListFragment : TrackListFragment() {
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)

        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this@OnlySearchMenuTrackListFragment)

        menu.findItem(R.id.find_by).setOnMenuItemClickListener { selectSearch() }
    }
}