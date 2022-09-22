package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R

/** [TypicalViewTrackListFragment] with only search option in menu */

abstract class OnlySearchMenuTrackListFragment : TypicalViewTrackListFragment() {
    final override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_track_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    final override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.find_by) selectSearch()
        return super.onMenuItemSelected(menuItem)
    }
}