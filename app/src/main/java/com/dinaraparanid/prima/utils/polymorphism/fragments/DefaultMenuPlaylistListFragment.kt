package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R

/** [AbstractPlaylistListFragment] with default menu */

abstract class DefaultMenuPlaylistListFragment : TypicalViewPlaylistListFragment() {
    final override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }
}