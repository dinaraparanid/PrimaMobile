package com.dinaraparanid.prima.viewmodels.mvvm

import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
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
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import kotlinx.coroutines.CoroutineScope
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.SettingsFragment]
 */

class SettingsViewModel(
    private val activity: WeakReference<MainActivity>,
    private val mainLabelCurText: String
) : ViewModel(), AsyncContext {

    override val coroutineScope: CoroutineScope
        get() = activity.unchecked.lifecycleScope

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
     * Shows or hides track's cover on playback panel
     * @param isChecked is cover shown or hidden
     */

    @JvmName("onHideCoverButtonClicked")
    internal fun onHideCoverButtonClicked(isChecked: Boolean) {
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeHideCover(isChecked) }
        params.isCoverHidden = isChecked
        activity.unchecked.setHidingCover()
    }

    /**
     * Displays albums' covers or shows only the default one
     * @param isChecked shows albums' covers or the default one
     */

    @JvmName("onDisplayCoversButtonClicked")
    internal fun onDisplayCoversButtonClicked(isChecked: Boolean) {
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeDisplayCovers(isChecked) }
        params.areCoversDisplayed = isChecked
    }

    /**
     * Rotates track's cover on small playback panel
     * @param isChecked is cover rotated
     */

    @JvmName("onRotateCoverButtonClicked")
    internal fun onRotateCoverButtonClicked(isChecked: Boolean) {
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeRotateCover(isChecked) }
        params.isCoverRotated = isChecked
        activity.unchecked.setRotatingCover()
    }

    /**
     * Add or removes rounding of playlists' images
     * @param isChecked add or remove rounding
     */

    @JvmName("onPlaylistImageCirclingButtonClicked")
    internal fun onPlaylistImageCirclingButtonClicked(isChecked: Boolean) {
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeRounded(isChecked) }
        params.isRoundingPlaylistImage = isChecked
        activity.unchecked.setRoundingOfPlaylistImage()
    }

    /**
     * Shows or hides audio visualizer
     * @param isChecked show or hide
     */

    @JvmName("onShowVisualizerButtonClicked")
    internal fun onShowVisualizerButtonClicked(isChecked: Boolean) {
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeShowVisualizer(isChecked) }
        params.isVisualizerShown = isChecked

        activity.unchecked.let {
            it.finish()
            it.startActivity(Intent(params.application.unchecked, MainActivity::class.java))
        }
    }

    /**
     * Enables or disables bloom effect in whole app
     * @param isChecked enable or disable bloom effect
     */

    @JvmName("onBloomButtonClicked")
    internal fun onBloomButtonClicked(isChecked: Boolean) {
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeBloom(isChecked) }
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
        runOnIOThread {
            StorageUtil.getInstanceSynchronized().run {
                this@SettingsViewModel.activity.unchecked.runOnUIThread {
                    storeSaveCurTrackAndPlaylist(isChecked)
                }

                clearPlayingProgress()
            }
        }
    }

    /**
     * Saves or removes saving of looping status
     * @param isChecked save or not save looping status
     */

    @JvmName("onSaveLoopingButtonClicked")
    internal fun onSaveLoopingButtonClicked(isChecked: Boolean) {
        params.saveLooping = isChecked
        runOnIOThread {
            StorageUtil.getInstanceSynchronized().run {
                storeSaveLooping(isChecked)
                clearLooping()
            }
        }
    }

    /**
     * Saves or removes saving of equalizer's progress
     * @param isChecked save or not save equalizer settings
     */

    @JvmName("onSaveEqualizerSettingsButtonClicked")
    internal fun onSaveEqualizerSettingsButtonClicked(isChecked: Boolean) {
        Params.instance.saveEqualizerSettings = isChecked
        runOnIOThread {
            StorageUtil.getInstanceSynchronized().run {
                storeSaveEqualizerSettings(isChecked)
                clearEqualizerProgress()
            }
        }
    }

    /**
     * Saves or removes starting with equalizer mode
     * @param isChecked enable or not enable
     * first playback with equalizer
     */

    @JvmName("onStartWithEqualizerButtonClicked")
    internal fun onStartWithEqualizerButtonClicked(isChecked: Boolean) =
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeStartWithEqualizer(isChecked) }

    /**
     * Saves or removes is using android notification flag
     * @param isChecked enable or not enable native notifications
     */

    @RequiresApi(Build.VERSION_CODES.P)
    @JvmName("onAndroidNotificationButtonClicked")
    internal fun onAndroidNotificationButtonClicked(isChecked: Boolean) {
        Params.instance.isUsingAndroidNotification = isChecked
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeIsUsingAndroidNotification(isChecked) }
    }

    @JvmName("onVisualizerStyleButtonClicked")
    internal fun onVisualizerStyleButtonClicked(view: View) {
        PopupMenu(activity.unchecked, view).run {
            menuInflater.inflate(R.menu.menu_visualizer_style, menu)

            setOnMenuItemClickListener { menuItem ->
                runOnIOThread {
                    when (menuItem.itemId) {
                        R.id.nav_bar_style -> {
                            params.visualizerStyle = Params.Companion.VisualizerStyle.BAR
                            StorageUtil.getInstanceSynchronized().storeVisualizerStyle(Params.Companion.VisualizerStyle.BAR)
                        }

                        else -> {
                            params.visualizerStyle = Params.Companion.VisualizerStyle.WAVE
                            StorageUtil.getInstanceSynchronized().storeVisualizerStyle(Params.Companion.VisualizerStyle.WAVE)
                        }
                    }
                }

                activity.unchecked.let {
                    it.finish()
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
                    R.id.ff_track_collection -> Params.Companion.HomeScreen.TRACK_COLLECTION
                    R.id.ff_artists -> Params.Companion.HomeScreen.ARTISTS
                    R.id.ff_favourites -> Params.Companion.HomeScreen.FAVOURITES
                    R.id.ff_mp3_converter -> Params.Companion.HomeScreen.MP3_CONVERTER
                    R.id.ff_gtm -> Params.Companion.HomeScreen.GUESS_THE_MELODY
                    R.id.ff_settings -> Params.Companion.HomeScreen.SETTINGS
                    else -> Params.Companion.HomeScreen.ABOUT_APP
                }

                runOnIOThread { StorageUtil.getInstanceSynchronized().storeHomeScreen(params.homeScreen) }
                true
            }

            show()
        }
    }

    @JvmName("onSaveLocationButtonClicked")
    internal fun onSaveLocationButtonClicked() = activity.unchecked.startActivityForResult(
        Intent(activity.unchecked, FoldersActivity::class.java), FoldersActivity.PICK_FOLDER
    )

    @JvmName("onBlurButtonClicked")
    internal fun onBlurButtonClicked(isChecked: Boolean) {
        Params.instance.isBlurEnabled = isChecked
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeBlurred(isChecked) }
        activity.get()?.run { runOnUIThread { updateUI(isLocking = true) } }
    }
}