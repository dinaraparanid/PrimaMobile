package com.dinaraparanid.prima.fragments.guess_the_melody

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentGuessTheMelodyAboutGameBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.MainActivitySimpleFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel

/**
 * Fragment with info about "Guess the Melody" game
 */

class AboutGameFragment : MainActivitySimpleFragment<FragmentGuessTheMelodyAboutGameBinding>(), Rising {
    override var binding: FragmentGuessTheMelodyAboutGameBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.about_game)
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentGuessTheMelodyAboutGameBinding>(
            inflater,
            R.layout.fragment_guess_the_melody_about_game,
            container,
            false
        ).apply { viewModel = ViewModel() }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.aboutGameLayout.layoutParams =
                (binding!!.aboutGameLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }
}