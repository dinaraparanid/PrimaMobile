package com.dinaraparanid.prima.mvvmp.view.fragments

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider

/** Fragments with a [MenuProvider] */

interface MenuProviderFragment {
    val menuProvider: MenuProvider

    /**
     * Called by the MenuHost to allow the [MenuProvider]
     * to inflate [MenuItem]s into the menu.
     *
     * @param menu         the menu to inflate the new menu items into
     * @param menuInflater the inflater to be used to inflate the updated menu
     */

    fun onCreateMenu(menu: Menu, menuInflater: MenuInflater)

    /**
     * Called by the MenuHost when a [MenuItem] is selected from the menu.
     *
     * @param menuItem the menu item that was selected
     * @return true if the given menu item is handled by this menu provider,
     *         false otherwise
     */

    fun onMenuItemSelected(menuItem: MenuItem) = true
}

/**
 * Creates default [MenuProvider] with
 * [MenuProviderFragment.onCreateMenu] and [MenuProviderFragment.onMenuItemSelected]
 */

inline val MenuProviderFragment.defaultMenuProvider: MenuProvider
    get() = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) =
            this@defaultMenuProvider.onCreateMenu(menu, menuInflater)

        override fun onMenuItemSelected(menuItem: MenuItem) =
            this@defaultMenuProvider.onMenuItemSelected(menuItem)
    }