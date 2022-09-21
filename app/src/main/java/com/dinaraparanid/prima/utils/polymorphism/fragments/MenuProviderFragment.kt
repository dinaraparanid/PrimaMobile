package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider

/** Fragments with a [MenuProvider] */

internal interface MenuProviderFragment {
    val menuProvider: MenuProvider
    fun onCreateMenu(menu: Menu, menuInflater: MenuInflater)
    fun onMenuItemSelected(menuItem: MenuItem) = true
}

/**
 * Creates default [MenuProvider] with
 * [MenuProviderFragment.onCreateMenu] and [MenuProviderFragment.onMenuItemSelected]
 */

internal inline val MenuProviderFragment.defaultMenuProvider: MenuProvider
    get() {
        val onCreateMenu = this::onCreateMenu
        val onMenuItemSelected = this::onMenuItemSelected

        return object : MenuProvider {

            /**
             * Called by the MenuHost to allow the [MenuProvider]
             * to inflate [MenuItem]s into the menu.
             *
             * @param menu         the menu to inflate the new menu items into
             * @param menuInflater the inflater to be used to inflate the updated menu
             */

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) =
                onCreateMenu(menu, menuInflater)

            /**
             * Called by the MenuHost when a [MenuItem] is selected from the menu.
             *
             * @param menuItem the menu item that was selected
             * @return true if the given menu item is handled by this menu provider,
             *         false otherwise
             */

            override fun onMenuItemSelected(menuItem: MenuItem) =
                onMenuItemSelected(menuItem)
        }
    }