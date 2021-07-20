package com.dinaraparanid.prima.utils.polymorphism

import android.view.Menu
import android.view.MenuInflater
import android.widget.SearchView
import com.dinaraparanid.prima.R

abstract class OnlySearchMenuTrackListFragment : TrackListFragment() {
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }
}