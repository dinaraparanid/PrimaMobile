package com.dinaraparanid.prima.mvvmp.view.fragments

/**
 * Ancestor for all MainActivity fragments.
 * Helps to access [MainActivityFragment.mainLabelText]
 */

interface MainActivityFragment {
    /** Text on app bar to show when fragment is attached */
    val mainLabelText: String
}