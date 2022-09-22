package com.dinaraparanid.prima.viewmodels.mvvm

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import arrow.core.None
import com.dinaraparanid.prima.BR
import com.dinaraparanid.prima.FoldersActivity
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import com.dinaraparanid.prima.dialogs.AutoSaveTimeDialog
import com.dinaraparanid.prima.dialogs.CheckHiddenPasswordDialog
import com.dinaraparanid.prima.dialogs.ColorPickerDialog
import com.dinaraparanid.prima.dialogs.CreateHiddenPasswordDialog
import com.dinaraparanid.prima.fragments.main_menu.settings.FontsFragment
import com.dinaraparanid.prima.fragments.main_menu.settings.ThemesFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.extensions.capitalizeFirst
import com.dinaraparanid.prima.utils.extensions.getTitleAndSubtitle
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AsyncContext
import com.dinaraparanid.prima.utils.polymorphism.fragments.AbstractFragment
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.main_menu.settings.SettingsFragment]
 */

class SettingsViewModel(
    private val activity: WeakReference<MainActivity>,
    private val homeScreenButton: WeakReference<Button>,
    private val autosaveTimeButton: WeakReference<Button>
) : ViewModel(), AsyncContext {
    override val coroutineScope
        get() = activity.unchecked.lifecycleScope

    private fun restartActivity() = activity.unchecked.let { activity ->
        activity.finishAndRemoveTask()
        activity.startActivity(Intent(params.application.unchecked, MainActivity::class.java))
    }

    /** Changes language and restarts [MainActivity] */
    @JvmName("onLanguageButtonPressed")
    internal fun onLanguageButtonPressed(view: View) = PopupMenu(activity.unchecked, view).run {
        menuInflater.inflate(R.menu.menu_language, menu)

        setOnMenuItemClickListener { menuItem ->
            runOnUIThread {
                Params.getInstanceSynchronized().let { params ->
                    when (menuItem.itemId) {
                        R.id.nav_english -> params.changeLang(
                            activity.unchecked,
                            Params.Companion.Language.EN
                        )

                        R.id.nav_belarusian -> params.changeLang(
                            activity.unchecked,
                            Params.Companion.Language.BE
                        )

                        R.id.nav_russian -> params.changeLang(
                            activity.unchecked,
                            Params.Companion.Language.RU
                        )

                        R.id.nav_chinese -> params.changeLang(
                            activity.unchecked,
                            Params.Companion.Language.ZH
                        )

                        else -> throw IllegalArgumentException("Unknown language")
                    }
                }
            }

            restartActivity()
            true
        }

        show()
    }

    /** Shows [com.dinaraparanid.prima.fragments.main_menu.settings.FontsFragment] */
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
                activity.unchecked.resources.getString(R.string.font),
                FontsFragment::class
            )
        )
        .addToBackStack(null)
        .commit()

    /** Shows [ColorPickerDialog] */
    @JvmName("onTextColorButtonPressed")
    internal fun onTextColorButtonPressed() = ColorPickerDialog(
        activity = WeakReference(activity.unchecked),
        viewModel = this,
        initialColor = Params.instance.fontColor
    ).show(object : ColorPickerDialog.ColorPickerObserver() {
        @SuppressLint("SyntheticAccessor")
        override fun onColorPicked(color: Int) {
            runOnIOThread {
                StorageUtil.getInstanceSynchronized().storeFontColor(color)
                launch(Dispatchers.Main) { restartActivity() }
            }
        }
    })

    /** Shows [com.dinaraparanid.prima.fragments.main_menu.settings.ThemesFragment] */
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
        restartActivity()
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
        params.isSavingCurTrackAndPlaylist = isChecked
        runOnIOThread {
            StorageUtil.getInstanceSynchronized().run {
                this@SettingsViewModel.activity.unchecked.runOnUIThread {
                    storeSaveCurTrackAndPlaylistLocking(isChecked)
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
        params.isSavingLooping = isChecked
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
        Params.instance.isSavingEqualizerSettings = isChecked
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
    internal fun onStartWithEqualizerButtonClicked(isChecked: Boolean) {
        Params.instance.isStartingWithEqualizer = isChecked
        runOnIOThread {
            StorageUtil
                .getInstanceSynchronized()
                .storeStartWithEqualizer(isChecked)
        }
    }

    /**
     * Saves or removes is using android notification flag
     * @param isChecked enable or not enable native notifications
     */

    @RequiresApi(Build.VERSION_CODES.P)
    @JvmName("onAndroidNotificationButtonClicked")
    internal fun onAndroidNotificationButtonClicked(isChecked: Boolean) {
        Params.instance.isUsingAndroidNotification = isChecked
        runOnIOThread {
            StorageUtil
                .getInstanceSynchronized()
                .storeIsUsingAndroidNotification(isChecked)
        }
    }

    @JvmName("onVisualizerStyleButtonClicked")
    internal fun onVisualizerStyleButtonClicked(view: View) =
        PopupMenu(activity.unchecked, view).run {
            menuInflater.inflate(R.menu.menu_visualizer_style, menu)

            setOnMenuItemClickListener { menuItem ->
                runOnIOThread {
                    when (menuItem.itemId) {
                        R.id.nav_bar_style -> {
                            params.visualizerStyle = Params.Companion.VisualizerStyle.BAR
                            StorageUtil
                                .getInstanceSynchronized()
                                .storeVisualizerStyle(Params.Companion.VisualizerStyle.BAR)
                        }

                        else -> {
                            params.visualizerStyle = Params.Companion.VisualizerStyle.WAVE
                            StorageUtil
                                .getInstanceSynchronized()
                                .storeVisualizerStyle(Params.Companion.VisualizerStyle.WAVE)
                        }
                    }
                }

                restartActivity()
                true
            }

            show()
        }

    /**
     * Shows [PopupMenu] with start screen selection
     * @param view what is the view where menu will be shown
     */

    @JvmName("onHomeScreenButtonClicked")
    internal fun onHomeScreenButtonClicked(view: View) = PopupMenu(activity.unchecked, view).run {
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

            homeScreenButton.unchecked.text = homeScreenText

            runOnIOThread {
                StorageUtil
                    .getInstanceSynchronized()
                    .storeHomeScreen(params.homeScreen)
            }

            true
        }

        show()
    }

    /**
     * Starts [FoldersActivity] to select folder
     * where created tracks should be stored
     */

    @JvmName("onSaveLocationButtonClicked")
    internal fun onSaveLocationButtonClicked() =
        activity.unchecked.pickFolderIntentResultListener.launch(
            Intent(activity.unchecked, FoldersActivity::class.java)
        )

    /**
     * Shows or removes blur visual effect
     * @param isChecked is it visible
     */

    @JvmName("onBlurButtonClicked")
    internal fun onBlurButtonClicked(isChecked: Boolean) {
        Params.instance.isBlurEnabled = isChecked
        runOnIOThread { StorageUtil.getInstanceSynchronized().storeBlurred(isChecked) }
        activity.get()?.run { runOnUIThread { updateUIAsync(oldTrack = None, isLocking = true) } }
    }

    /** Shows dialog to clear all statistics */
    @JvmName("onStatisticsClearButtonClicked")
    internal fun onStatisticsClearButtonClicked() {
        AlertDialog.Builder(activity.unchecked)
            .setMessage(R.string.clear_statistics_dialog)
            .setPositiveButton(R.string.ok) { d, _ ->
                d.dismiss()
                runOnIOThread {
                    StatisticsRepository.getInstanceSynchronized().clearAllStatisticsAsync()
                    StorageUtil.getInstanceSynchronized().clearStatistics()
                }
            }
            .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
            .show()
    }

    /** Shows password dialog and hidden tracks */
    @JvmName("onHiddenButtonClicked")
    internal fun onHiddenButtonClicked() {
        runOnUIThread {
            (StorageUtil.getInstanceSynchronized().loadHiddenPassword()
                ?.let {
                    CheckHiddenPasswordDialog(
                        passwordHash = it,
                        activity = activity.unchecked
                    )
                }
                ?: CreateHiddenPasswordDialog(
                    target = CreateHiddenPasswordDialog.Target.CREATE,
                    activity = activity.unchecked
                )).show(activity.unchecked.supportFragmentManager, null)
        }
    }

    /** Shows dialog with time input (from 1 to 99) */
    @JvmName("onAutoSaveTimeButtonClicked")
    internal fun onAutoSaveTimeButtonClicked() = AutoSaveTimeDialog(WeakReference(this))
        .show(activity.unchecked.supportFragmentManager, null)

    /** Updates text for [autosaveTimeButton] */
    internal fun updateAutoSaveTimeTitle() {
        autosaveTimeButton.unchecked.text = autosaveTimeText
    }

    private inline val resources
        get() = activity.unchecked.resources

    internal inline val homeScreenText
        @JvmName("getHomeScreenText")
        get() = getTitleAndSubtitle(
            resources.getString(R.string.home_screen),
            Params.instance.run { getHomeScreenTitle(homeScreen) }
        )

    internal inline val languageText
        @JvmName("getLanguageText")
        get() = getTitleAndSubtitle(
            resources.getString(R.string.language),
            Params.instance.getLangTitle(StorageUtil.instance.loadLanguage())
        )

    internal inline val fontText
        @JvmName("getFontText")
        get() = getTitleAndSubtitle(
            resources.getString(R.string.font),
            Params.instance.font
        )

    internal inline val audioVisualizerStyle
        @JvmName("getAudioVisualizerStyle")
        get() = getTitleAndSubtitle(
            resources.getString(R.string.audio_visualizer_style),
            resources.getString(
                when (Params.instance.visualizerStyle) {
                    Params.Companion.VisualizerStyle.BAR -> R.string.visualizer_style_bar
                    Params.Companion.VisualizerStyle.WAVE -> R.string.visualizer_style_bar
                }
            )
        )

    internal inline val locationText
        @JvmName("getLocationText")
        get() = getTitleAndSubtitle(
            activity.unchecked.resources.getString(R.string.location_of_created_files),
            Params.instance.pathToSave
        )

    internal inline val autosaveTimeText
        @JvmName("getAutosaveTimeText")
        get() = getTitleAndSubtitle(
            resources.getString(R.string.autosave_time),
            "${resources.getString(R.string.seconds).capitalizeFirst}: ${Params.instance.autoSaveTime.get()}"
        )
}