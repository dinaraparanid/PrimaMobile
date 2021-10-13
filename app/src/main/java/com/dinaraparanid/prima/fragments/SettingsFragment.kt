package com.dinaraparanid.prima.fragments

import android.os.Build
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
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.viewmodels.mvvm.SettingsViewModel
import java.lang.ref.WeakReference

/**
 * Fragment for settings.
 */

class SettingsFragment : AbstractFragment<FragmentSettingsBinding>(), Rising {
    override var binding: FragmentSettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = resources.getString(R.string.settings)
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
            viewModel = SettingsViewModel(WeakReference(requireActivity() as MainActivity), mainLabelCurText)

            showPlaylistImages.run {
                isChecked = viewModel!!.params.isPlaylistsImagesShown
                trackTintList = ViewSetter.colorStateList
            }

            playlistImageCircling.run {
                isChecked = viewModel!!.params.isRoundingPlaylistImage
                trackTintList = ViewSetter.colorStateList
            }

            showVisualizer.run {
                isChecked = viewModel!!.params.isVisualizerShown
                trackTintList = ViewSetter.colorStateList
            }

            bloom.run {
                isChecked = viewModel!!.params.isBloomEnabled
                trackTintList = ViewSetter.colorStateList
            }

            progressCurTrackPlaylist.run {
                isChecked = viewModel!!.params.saveCurTrackAndPlaylist
                trackTintList = ViewSetter.colorStateList
            }

            progressLooping.run {
                isChecked = viewModel!!.params.saveLooping
                trackTintList = ViewSetter.colorStateList
            }

            progressEqualizer.run {
                isChecked = viewModel!!.params.saveEqualizerSettings
                trackTintList = ViewSetter.colorStateList
            }

            startWithEqualizer.run {
                isChecked = viewModel!!.params.isStartingWithEqualizer
                trackTintList = ViewSetter.colorStateList
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                androidNotification!!.run {
                    isChecked = viewModel!!.params.isUsingAndroidNotification
                    trackTintList = ViewSetter.colorStateList
                }
        }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        return binding!!.root
    }

    override fun up() {
        if (!(requireActivity() as MainActivity).isUpped)
            binding!!.settingsLayout.layoutParams =
                (binding!!.settingsLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }
}