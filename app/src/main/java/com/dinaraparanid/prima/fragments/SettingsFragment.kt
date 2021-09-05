package com.dinaraparanid.prima.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentSettingsBinding
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.viewmodels.mvvm.SettingsViewModel

/**
 * Fragment for settings.
 */

class SettingsFragment : AbstractFragment<FragmentSettingsBinding>(), Rising {
    override var binding: FragmentSettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentSettingsBinding>(
            inflater,
            R.layout.fragment_settings,
            container,
            false
        ).apply {
            viewModel = SettingsViewModel(requireActivity() as MainActivity, mainLabelCurText)
            showPlaylistImages.isChecked = viewModel!!.params.isPlaylistsImagesShown
            playlistImageCircling.isChecked = viewModel!!.params.isRoundingPlaylistImage
            showVisualizer.isChecked = viewModel!!.params.isVisualizerShown
            progressCurTrackPlaylist.isChecked = viewModel!!.params.saveCurTrackAndPlaylist
            progressLooping.isChecked = viewModel!!.params.saveLooping
            progressEqualizer.isChecked = viewModel!!.params.saveEqualizerSettings
        }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return binding!!.root
    }

    override fun up() {
        val act = requireActivity() as MainActivity
        
        if (!act.upped)
            binding!!.settingsLayout.layoutParams =
                (binding!!.settingsLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = act.playingToolbarHeight
                }
    }
}