package com.dinaraparanid.prima.fragments.guess_the_melody

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentGuessTheMelodyMainBinding
import com.dinaraparanid.prima.utils.polymorphism.fragments.MainActivitySimpleFragment
import com.dinaraparanid.prima.mvvmp.old_shit.GuessTheGameMainViewModel
import java.lang.ref.WeakReference

/** Fragment that starts "Guess the Melody" game */

class GTMMainFragment : MainActivitySimpleFragment<FragmentGuessTheMelodyMainBinding>() {
    override var binding: FragmentGuessTheMelodyMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText.set(resources.getString(R.string.guess_the_melody))
        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentGuessTheMelodyMainBinding>(
            inflater,
            R.layout.fragment_guess_the_melody_main,
            container,
            false
        ).apply {
            viewModel = GuessTheGameMainViewModel(WeakReference(this@GTMMainFragment))
        }

        return binding!!.root
    }
}