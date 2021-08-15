package com.dinaraparanid.prima.viewmodels.mvvm

import android.content.Intent
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.FontsFragment
import com.dinaraparanid.prima.fragments.LanguagesFragment
import com.dinaraparanid.prima.fragments.ThemesFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.SettingsFragment]
 */

class SettingsViewModel(
    private val activity: MainActivity,
    private val mainLabelCurText: String
) : ViewModel() {

    /** Shows [com.dinaraparanid.prima.fragments.LanguagesFragment] */
    @JvmName("onLanguageButtonPressed")
    internal fun onLanguageButtonPressed() = activity.supportFragmentManager
        .beginTransaction()
        .setCustomAnimations(
            R.anim.fade_in,
            R.anim.fade_out,
            R.anim.fade_in,
            R.anim.fade_out
        )
        .replace(
            R.id.fragment_container,
            AbstractFragment.defaultInstance(
                mainLabelCurText,
                activity.resources.getString(R.string.language),
                LanguagesFragment::class
            )
        )
        .addToBackStack(null)
        .commit()

    /** Shows [com.dinaraparanid.prima.fragments.FontsFragment] */
    @JvmName("onFontButtonPressed")
    internal fun onFontButtonPressed() = activity.supportFragmentManager
        .beginTransaction()
        .setCustomAnimations(
            R.anim.fade_in,
            R.anim.fade_out,
            R.anim.fade_in,
            R.anim.fade_out
        )
        .replace(
            R.id.fragment_container,
            AbstractFragment.defaultInstance(
                mainLabelCurText,
                activity.resources.getString(R.string.font),
                FontsFragment::class
            )
        )
        .addToBackStack(null)
        .commit()

    /** Shows [com.dinaraparanid.prima.fragments.ThemesFragment] */
    @JvmName("onThemeButtonPressed")
    internal fun onThemeButtonPressed() = activity.supportFragmentManager
        .beginTransaction()
        .setCustomAnimations(
            R.anim.fade_in,
            R.anim.fade_out,
            R.anim.fade_in,
            R.anim.fade_out
        )
        .replace(
            R.id.fragment_container,
            AbstractFragment.defaultInstance(
                mainLabelCurText,
                activity.resources.getString(R.string.themes),
                ThemesFragment::class
            )
        )
        .addToBackStack(null)
        .commit()

    /**
     * Shows or hides album pictures
     * @param isChecked show or hide pictures
     */

    @JvmName("onShowPlaylistsImagesButtonClicked")
    internal fun onShowPlaylistsImagesButtonClicked(isChecked: Boolean) {
        StorageUtil(activity).storeShowPlaylistsImages(isChecked)
        params.showPlaylistsImages = isChecked
        activity.setShowingPlaylistImage()
    }

    /**
     * Add or removes rounding of playlists' images
     * @param isChecked add or remove rounding
     */

    @JvmName("onPlaylistImageCirclingButtonClicked")
    internal fun onPlaylistImageCirclingButtonClicked(isChecked: Boolean) {
        StorageUtil(activity).storeRounded(isChecked)
        params.isRoundingPlaylistImage = isChecked
        activity.setRoundingOfPlaylistImage()
    }

    /**
     * Shows or hides audio visualizer
     * @param isChecked show or hide
     */

    @JvmName("onShowVisualizerButtonClicked")
    internal fun onShowVisualizerButtonClicked(isChecked: Boolean) {
        StorageUtil(activity).storeShowVisualizer(isChecked)
        params.isVisualizerShown = isChecked
        params.application.startActivity(Intent(params.application, MainActivity::class.java))
    }

    /**
     * Saves or removes saving of playing progress (cur tracks and playlists)
     * @param isChecked save or not save
     */

    @JvmName("onSaveCurTrackAndPlaylistButtonClicked")
    internal fun onSaveCurTrackAndPlaylistButtonClicked(isChecked: Boolean) {
        params.saveCurTrackAndPlaylist = isChecked
        StorageUtil(activity).run {
            storeSaveCurTrackAndPlaylist(isChecked)
            clearPlayingProgress()
        }
    }

    /**
     * Saves or removes saving of looping status
     * @param isChecked save or not save
     */

    @JvmName("onSaveLoopingButtonClicked")
    internal fun onSaveLoopingButtonClicked(isChecked: Boolean) {
        params.saveLooping = isChecked
        StorageUtil(activity).run {
            storeSaveLooping(isChecked)
            clearLooping()
        }
    }

    /**
     * Saves or removes saving of equalizer's progress
     * @param isChecked save or not save
     */

    @JvmName("onSaveEqualizerSettingsButtonClicked")
    internal fun onSaveEqualizerSettingsButtonClicked(isChecked: Boolean) {
        Params.instance.saveEqualizerSettings = isChecked
        StorageUtil(activity).run {
            storeSaveEqualizerSettings(isChecked)
            clearEqualizerProgress()
        }
    }
}