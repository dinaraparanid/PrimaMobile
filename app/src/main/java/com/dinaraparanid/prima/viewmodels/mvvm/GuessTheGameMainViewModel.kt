package com.dinaraparanid.prima.viewmodels.mvvm

import androidx.fragment.app.Fragment
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.guess_the_melody.AboutGameFragment
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractFragment
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.guess_the_melody.AboutGameFragment]
 */

class GuessTheGameMainViewModel(private val fragment: WeakReference<Fragment>) : ViewModel() {
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
                AbstractFragment.defaultInstance(
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
                AbstractFragment.defaultInstance(
                    null,
                    AboutGameFragment::class
                )
            )
            .addToBackStack(null)
            .commit()
    }
}