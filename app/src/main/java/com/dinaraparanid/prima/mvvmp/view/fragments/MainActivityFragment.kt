package com.dinaraparanid.prima.mvvmp.view.fragments

/**
 * Ancestor for all MainActivity fragments.
 * Helps to access [MainActivityFragment.mainLabelText]
 */

interface MainActivityFragment {
    companion object {
        const val MAIN_LABEL_CUR_TEXT_KEY = "main_label"
    }

    /** Text on app bar to show when fragment is attached */
    val mainLabelText: String
}