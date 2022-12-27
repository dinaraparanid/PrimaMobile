package com.dinaraparanid.prima.fragments.main_menu.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentSettingsBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.extensions.getTitleAndSubtitle
import com.dinaraparanid.prima.utils.polymorphism.Rising
import com.dinaraparanid.prima.mvvmp.view.fragments.MainActivitySimpleFragment
import com.dinaraparanid.prima.mvvmp.old_shit.SettingsObserver
import java.lang.ref.WeakReference

/** Fragment for settings */

class SettingsFragment : MainActivitySimpleFragment<FragmentSettingsBinding>(), Rising {
    override var binding: FragmentSettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelText.set(resources.getString(R.string.settings))
        setMainLabelInitializedSync()
        super.onCreate(savedInstanceState)
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
            viewModel = SettingsObserver(
                WeakReference(fragmentActivity),
                WeakReference(homeScreen),
                WeakReference(autosaveTime)
            )

            hideCover.run {
                isChecked = viewModel!!.params.isCoverHidden
                trackTintList = ViewSetter.colorStateList
            }

            displayCovers.run {
                isChecked = viewModel!!.params.isCoversDisplayed
                trackTintList = ViewSetter.colorStateList
            }

            rotateCover.run {
                isChecked = viewModel!!.params.isCoverRotating
                trackTintList = ViewSetter.colorStateList
            }

            playlistImageCircling.run {
                isChecked = viewModel!!.params.areCoversRounded
                trackTintList = ViewSetter.colorStateList
            }

            showVisualizer.run {
                isChecked = viewModel!!.params.isVisualizerShown
                trackTintList = ViewSetter.colorStateList
            }

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> bloom!!.run {
                    isChecked = viewModel!!.params.isBloomEnabled
                    trackTintList = ViewSetter.colorStateList
                }

                else -> showDividers!!.run {
                    isChecked = viewModel!!.params.areDividersShown
                    trackTintList = ViewSetter.colorStateList
                }
            }

            progressCurTrackPlaylist.run {
                isChecked = viewModel!!.params.isSavingCurTrackAndPlaylist
                trackTintList = ViewSetter.colorStateList
            }

            progressLooping.run {
                isChecked = viewModel!!.params.isSavingLooping
                trackTintList = ViewSetter.colorStateList
            }

            progressEqualizer.run {
                isChecked = viewModel!!.params.isSavingEqualizerSettings
                trackTintList = ViewSetter.colorStateList
            }

            startWithEqualizer.run {
                isChecked = viewModel!!.params.isStartingWithEqualizer
                trackTintList = ViewSetter.colorStateList
            }

            blur.run {
                isChecked = viewModel!!.params.isBlurEnabled
                trackTintList = ViewSetter.colorStateList
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                androidNotification!!.run {
                    isChecked = viewModel!!.params.isUsingAndroidNotification
                    trackTintList = ViewSetter.colorStateList
                }
        }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.settingsLayout.layoutParams =
                (binding!!.settingsLayout.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    /** Updates text of save tracks' location button */

    internal fun refreshSaveLocationButton() {
        binding?.saveLocation?.text = getTitleAndSubtitle(
            resources.getString(R.string.location_of_created_files),
            Params.instance.pathToSave
        )
    }
}