package com.dinaraparanid.prima.viewmodels.mvvm

import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import com.dinaraparanid.prima.BR
import com.dinaraparanid.prima.FoldersActivity
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.fragments.FontsFragment
import com.dinaraparanid.prima.fragments.LanguagesFragment
import com.dinaraparanid.prima.fragments.ThemesFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.SettingsFragment]
 */

class SettingsViewModel(
    private val activity: WeakReference<MainActivity>,
    private val mainLabelCurText: String
) : ViewModel() {

    /** Shows [com.dinaraparanid.prima.fragments.LanguagesFragment] */
    @JvmName("onLanguageButtonPressed")
    internal fun onLanguageButtonPressed() = activity.unchecked.supportFragmentManager
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
                activity.unchecked.resources.getString(R.string.language),
                LanguagesFragment::class
            )
        )
        .addToBackStack(null)
        .commit()

    /** Shows [com.dinaraparanid.prima.fragments.FontsFragment] */
    @JvmName("onFontButtonPressed")
    internal fun onFontButtonPressed() = activity.unchecked.supportFragmentManager
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
                activity.unchecked.resources.getString(R.string.font),
                FontsFragment::class
            )
        )
        .addToBackStack(null)
        .commit()

    /** Shows [com.dinaraparanid.prima.fragments.ThemesFragment] */
    @JvmName("onThemeButtonPressed")
    internal fun onThemeButtonPressed() = activity.unchecked.supportFragmentManager
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
                activity.unchecked.resources.getString(R.string.themes),
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
        StorageUtil(activity.unchecked).storeShowPlaylistsImages(isChecked)
        params.isPlaylistsImagesShown = isChecked
        activity.unchecked.setShowingPlaylistImage()
    }

    /**
     * Add or removes rounding of playlists' images
     * @param isChecked add or remove rounding
     */

    @JvmName("onPlaylistImageCirclingButtonClicked")
    internal fun onPlaylistImageCirclingButtonClicked(isChecked: Boolean) {
        StorageUtil(activity.unchecked).storeRounded(isChecked)
        params.isRoundingPlaylistImage = isChecked
        activity.unchecked.setRoundingOfPlaylistImage()
    }

    /**
     * Shows or hides audio visualizer
     * @param isChecked show or hide
     */

    @JvmName("onShowVisualizerButtonClicked")
    internal fun onShowVisualizerButtonClicked(isChecked: Boolean) {
        StorageUtil(activity.unchecked).storeShowVisualizer(isChecked)
        params.isVisualizerShown = isChecked

        activity.unchecked.let {
            it.finishWork()
            it.startActivity(Intent(params.application.unchecked, MainActivity::class.java))
        }
    }

    /**
     * Enables or disables bloom effect in whole app
     * @param isChecked enable or disable bloom effect
     */

    @JvmName("onBloomButtonClicked")
    internal fun onBloomButtonClicked(isChecked: Boolean) {
        StorageUtil(activity.unchecked).storeBloom(isChecked)
        params.isBloomEnabled = isChecked
        notifyPropertyChanged(BR._all)
        activity.unchecked.setBloomColor(if (isChecked) params.primaryColor else android.R.color.transparent)
    }

    /**
     * Saves or removes saving of playing progress (cur tracks and playlists)
     * @param isChecked save or not save current track and playlist
     */

    @JvmName("onSaveCurTrackAndPlaylistButtonClicked")
    internal fun onSaveCurTrackAndPlaylistButtonClicked(isChecked: Boolean) {
        params.saveCurTrackAndPlaylist = isChecked
        StorageUtil(activity.unchecked).run {
            storeSaveCurTrackAndPlaylist(isChecked)
            clearPlayingProgress()
        }
    }

    /**
     * Saves or removes saving of looping status
     * @param isChecked save or not save looping status
     */

    @JvmName("onSaveLoopingButtonClicked")
    internal fun onSaveLoopingButtonClicked(isChecked: Boolean) {
        params.saveLooping = isChecked
        StorageUtil(activity.unchecked).run {
            storeSaveLooping(isChecked)
            clearLooping()
        }
    }

    /**
     * Saves or removes saving of equalizer's progress
     * @param isChecked save or not save equalizer settings
     */

    @JvmName("onSaveEqualizerSettingsButtonClicked")
    internal fun onSaveEqualizerSettingsButtonClicked(isChecked: Boolean) {
        Params.instance.saveEqualizerSettings = isChecked
        StorageUtil(activity.unchecked).run {
            storeSaveEqualizerSettings(isChecked)
            clearEqualizerProgress()
        }
    }

    /**
     * Saves or removes starting with equalizer mode
     * @param isChecked enable or not enable
     * first playback with equalizer
     */

    @JvmName("onStartWithEqualizerButtonClicked")
    internal fun onStartWithEqualizerButtonClicked(isChecked: Boolean) =
        StorageUtil(activity.unchecked).storeStartWithEqualizer(isChecked)

    /**
     * Saves or removes is using android notification flag
     * @param isChecked enable or not enable native notifications
     */

    @RequiresApi(Build.VERSION_CODES.P)
    @JvmName("onAndroidNotificationButtonClicked")
    internal fun onAndroidNotificationButtonClicked(isChecked: Boolean) {
        Params.instance.isUsingAndroidNotification = isChecked
        StorageUtil(activity.unchecked).storeIsUsingAndroidNotification(isChecked)
    }

    @JvmName("onVisualizerStyleButtonClicked")
    internal fun onVisualizerStyleButtonClicked(view: View) {
        PopupMenu(activity.unchecked, view).run {
            menuInflater.inflate(R.menu.menu_visualizer_style, menu)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_bar_style -> {
                        params.visualizerStyle = Params.Companion.VisualizerStyle.BAR
                        StorageUtil(activity.unchecked.applicationContext).storeVisualizerStyle(Params.Companion.VisualizerStyle.BAR)
                    }

                    else -> {
                        params.visualizerStyle = Params.Companion.VisualizerStyle.WAVE
                        StorageUtil(activity.unchecked.applicationContext).storeVisualizerStyle(Params.Companion.VisualizerStyle.WAVE)
                    }
                }

                activity.unchecked.let {
                    it.finishWork()
                    it.startActivity(Intent(params.application.unchecked, MainActivity::class.java))
                }

                true
            }

            show()
        }
    }

    @JvmName("onHomeScreenButtonClicked")
    internal fun onHomeScreenButtonClicked(view: View) {
        PopupMenu(activity.unchecked, view).run {
            menuInflater.inflate(R.menu.menu_first_fragment, menu)

            setOnMenuItemClickListener { menuItem ->
                params.homeScreen = when (menuItem.itemId) {
                    R.id.ff_tracks -> Params.Companion.HomeScreen.TRACKS
                    R.id.ff_current_playlist -> Params.Companion.HomeScreen.CURRENT_PLAYLIST
                    R.id.ff_albums -> Params.Companion.HomeScreen.ALBUMS
                    R.id.ff_playlists -> Params.Companion.HomeScreen.PLAYLISTS
                    R.id.ff_artists -> Params.Companion.HomeScreen.ARTISTS
                    R.id.ff_favourite_tracks -> Params.Companion.HomeScreen.FAVOURITE_TRACKS
                    R.id.ff_favourite_artists -> Params.Companion.HomeScreen.FAVOURITE_ARTISTS
                    R.id.ff_mp3_converter -> Params.Companion.HomeScreen.MP3_CONVERTER
                    else -> Params.Companion.HomeScreen.GUESS_THE_MELODY
                }

                StorageUtil(activity.unchecked.applicationContext).storeHomeScreen(params.homeScreen)
                true
            }

            show()
        }
    }

    @JvmName("onSaveLocationButtonClicked")
    internal fun onSaveLocationButtonClicked() = activity.unchecked.startActivityForResult(
        Intent(activity.unchecked, FoldersActivity::class.java), FoldersActivity.PICK_FOLDER
    )
}