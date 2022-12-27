package com.dinaraparanid.prima.mvvmp.old_shit

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.guess_the_melody.AboutGameFragment
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.mvvmp.view.fragments.ObservableFragment
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.guess_the_melody.AboutGameFragment]
 */

class GuessTheGameMainViewModel(private val fragment: WeakReference<Fragment>) : BasePresenter() {
    @JvmName("onPlayButtonClicked")
    internal fun onPlayButtonClicked() {
        fragment.unchecked.requireActivity().supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                ObservableFragment.defaultInstance(
                    null,
                    GTMPlaylistSelectFragment::class
                )
            )
            .addToBackStack(null)
            .commit()
    }

    @JvmName("onAboutGameButtonClicked")
    internal fun onAboutGameButtonClicked() {
        fragment.unchecked.requireActivity().supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                ObservableFragment.defaultInstance(
                    null,
                    AboutGameFragment::class
                )
            )
            .addToBackStack(null)
            .commit()
    }
}