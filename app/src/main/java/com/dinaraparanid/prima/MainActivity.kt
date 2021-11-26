package com.dinaraparanid.prima

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Contact
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databinding.*
import com.dinaraparanid.prima.fragments.*
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMMainFragment
import com.dinaraparanid.prima.services.AudioPlayerService
import com.dinaraparanid.prima.services.MicRecordService
import com.dinaraparanid.prima.services.PlaybackRecordService
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.dialogs.*
import com.dinaraparanid.prima.utils.extensions.setShadowColor
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.extensions.unwrap
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.rustlibs.NativeLibrary
import com.dinaraparanid.prima.utils.web.genius.GeniusFetcher
import com.dinaraparanid.prima.utils.web.genius.GeniusTrack
import com.dinaraparanid.prima.utils.web.genius.songs_response.Song
import com.dinaraparanid.prima.viewmodels.androidx.MainActivityViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import com.gauravk.audiovisualizer.model.AnimSpeed
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import java.io.BufferedInputStream
import java.io.File
import java.lang.ref.WeakReference
import java.net.UnknownHostException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.set
import kotlin.concurrent.withLock
import kotlin.math.ceil
import kotlin.system.exitProcess
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/** Prima's main activity on which the entire application rests */

class MainActivity :
    AbstractActivity(),
    AbstractTrackListFragment.Callbacks,
    AbstractArtistListFragment.Callbacks,
    AbstractPlaylistListFragment.Callbacks,
    FontsFragment.Callbacks,
    TrackListFoundFragment.Callbacks,
    TrackChangeFragment.Callbacks,
    TrimFragment.Callbacks,
    ChooseContactFragment.Callbacks,
    GTMPlaylistSelectFragment.Callbacks,
    NavigationView.OnNavigationItemSelectedListener,
    UIUpdatable<Pair<AbstractTrack, Boolean>> {
    private var _binding: Either<ActivityMainBarBinding, ActivityMainWaveBinding>? = null

    override val viewModel: MainActivityViewModel by lazy {
        ViewModelProvider(this)[MainActivityViewModel::class.java]
    }

    internal lateinit var sheetBehavior: BottomSheetBehavior<View>

    private var playingCoroutine: Job? = null
    private var actionBarSize = 0
    private var backClicksCount = 2

    private var isSeekBarDragging = false
    internal var isUpped = false
    internal var isUpdateNeeded = false

    internal val isBindingInitialized
        get() = _binding != null

    internal val awaitBindingInitLock: Lock = ReentrantLock()
    internal val awaitBindingInitCondition = awaitBindingInitLock.newCondition()

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private var albumImageWidth = 0
    private var albumImageHeight = 0

    private var recordFilename = ""
    internal fun setRecordFilename(filename: String) { recordFilename = filename }

    private inline val binding
        get() = when (_binding) {
            is Either.Right -> Either.Right((_binding as Either.Right<ActivityMainWaveBinding>).value)
            is Either.Left -> Either.Left((_binding as Either.Left<ActivityMainBarBinding>).value)
            else -> throw NullPointerException("Main Activity binding is null")
        }
    
    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.drawerLayout
        get() = when (this) {
            is Either.Right -> value.drawerLayout
            is Either.Left -> value.drawerLayout
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.mainCoordinatorLayout
        get() = when (this) {
            is Either.Right -> value.mainCoordinatorLayout
            is Either.Left -> value.mainCoordinatorLayout
        }
    
    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.appbar
        get() = when (this) {
            is Either.Right -> value.appbar
            is Either.Left -> value.appbar
        }
    
    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.switchToolbar
        get() = when (this) {
            is Either.Right -> value.switchToolbar
            is Either.Left -> value.switchToolbar
        }
    
    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.mainLabel
        get() = when (this) {
            is Either.Right -> value.mainLabel
            is Either.Left -> value.mainLabel
        }
    
    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.fragmentContainer
        get() = when (this) {
            is Either.Right -> value.fragmentContainer
            is Either.Left -> value.fragmentContainer
        }
    
    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.playingLayout
        get() = when (this) {
            is Either.Right -> Either.Right(value.playingLayoutWave)
            is Either.Left -> Either.Left(value.playingLayoutBar)
        }
    
    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.navView
        get() = when (this) {
            is Either.Right -> value.navView
            is Either.Left -> value.navView
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.activityViewModel
        get() = when (this) {
            is Either.Right -> value.viewModel
            is Either.Left -> value.viewModel
        }
    
    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playing
        get() = when (this) {
            is Either.Right -> value.playingWave
            is Either.Left -> value.playingBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingToolbar
        get() = when (this) {
            is Either.Right -> value.playingToolbarWave
            is Either.Left -> value.playingToolbarBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingLayout
        get() = when (this) {
            is Either.Right -> value.playingLayoutWave
            is Either.Left -> value.playingLayoutBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingAlbumImage
        get() = when (this) {
            is Either.Right -> value.playingAlbumImageWave
            is Either.Left -> value.playingAlbumImageBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingTrackTitle
        get() = when (this) {
            is Either.Right -> value.playingTrackTitleWave
            is Either.Left -> value.playingTrackTitleBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingTrackArtists
        get() = when (this) {
            is Either.Right -> value.playingTrackArtistsWave
            is Either.Left -> value.playingTrackArtistsBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingNextTrack
        get() = when (this) {
            is Either.Right -> value.playingNextTrackWave
            is Either.Left -> value.playingNextTrackBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingPlayButton
        get() = when (this) {
            is Either.Right -> value.playingPlayButtonWave
            is Either.Left -> value.playingPlayButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingPrevTrack
        get() = when (this) {
            is Either.Right -> value.playingPrevTrackWave
            is Either.Left -> value.playingPrevTrackBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackLayout
        get() = when (this) {
            is Either.Right -> value.trackLayoutWave
            is Either.Left -> value.trackLayoutBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.visualizer
        get() = when (this) {
            is Either.Right -> value.visualizerWave
            is Either.Left -> value.visualizerBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackSettingsButton
        get() = when (this) {
            is Either.Right -> value.trackSettingsButtonWave
            is Either.Left -> value.trackSettingsButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.albumPicture
        get() = when (this) {
            is Either.Right -> value.albumPictureWave
            is Either.Left -> value.albumPictureBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackPlayingBar
        get() = when (this) {
            is Either.Right -> value.trackPlayingBarWave
            is Either.Left -> value.trackPlayingBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.currentTime
        get() = when (this) {
            is Either.Right -> value.currentTimeWave
            is Either.Left -> value.currentTimeBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackLength
        get() = when (this) {
            is Either.Right -> value.trackLengthWave
            is Either.Left -> value.trackLengthBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackTitleBig
        get() = when (this) {
            is Either.Right -> value.trackTitleBigWave
            is Either.Left -> value.trackTitleBigBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.artistsAlbum
        get() = when (this) {
            is Either.Right -> value.artistsAlbumWave
            is Either.Left -> value.artistsAlbumBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.primaryButtons
        get() = when (this) {
            is Either.Right -> value.primaryButtonsWave
            is Either.Left -> value.primaryButtonsBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playButton
        get() = when (this) {
            is Either.Right -> value.playButtonWave
            is Either.Left -> value.playButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.previousTrackButton
        get() = when (this) {
            is Either.Right -> value.previousTrackButtonWave
            is Either.Left -> value.previousTrackButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.nextTrackButton
        get() = when (this) {
            is Either.Right -> value.nextTrackButtonWave
            is Either.Left -> value.nextTrackButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.secondaryButtons
        get() = when (this) {
            is Either.Right -> value.secondaryButtonsWave
            is Either.Left -> value.secondaryButtonsBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.equalizerButton
        get() = when (this) {
            is Either.Right -> value.equalizerButtonWave
            is Either.Left -> value.equalizerButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.repeatButton
        get() = when (this) {
            is Either.Right -> value.repeatButtonWave
            is Either.Left -> value.repeatButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.sleepTimer
        get() = when (this) {
            is Either.Right -> value.sleepTimerWave
            is Either.Left -> value.sleepTimerBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.recordButton
        get() = when (this) {
            is Either.Right -> value.recordButtonWave
            is Either.Left -> value.recordButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playlistButton
        get() = when (this) {
            is Either.Right -> value.playlistButtonWave
            is Either.Left -> value.playlistButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trimButton
        get() = when (this) {
            is Either.Right -> value.trimButtonWave
            is Either.Left -> value.trimButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.returnButton
        get() = when (this) {
            is Either.Right -> value.returnButtonWave
            is Either.Left -> value.returnButtonBar
        }

    private inline var Either<PlayingBarBinding, PlayingWaveBinding>.playingBarViewModel
        get() = when (this) {
            is Either.Right -> value.viewModel
            is Either.Left -> value.viewModel
        }

        set(value) = when (this) {
            is Either.Right -> this.value.viewModel = value
            is Either.Left -> this.value.viewModel = value
        }

    internal var mainLabelCurText
        get() = binding.mainLabel.text.toString()
        set(value) { binding.mainLabel.text = value }

    internal val switchToolbar
        get() = binding.switchToolbar

    private inline val curTrack
        get() = (application as MainApplication).run {
            curPath.takeIf { it != Params.NO_PATH }
                ?.let {
                    try {
                        Some(
                            curPlaylist.run { get(indexOfFirst { track -> track.path == it }) }
                        )
                    } catch (e: Exception) {
                        None
                    }
                } ?: run {
                StorageUtil(this)
                    .loadTrackPath()
                    .takeIf { it != Params.NO_PATH }
                    ?.let {
                        try {
                            Some(
                                curPlaylist.run { get(indexOfFirst { track -> track.path == it }) }
                            )
                        } catch (e: Exception) {
                            None
                        }
                    } ?: None
            }
        }

    private inline val curPath
        get() = (application as MainApplication).curPath

    private inline val curInd
        get() = (application as MainApplication)
            .curPlaylist.indexOfFirst { it.path == curPath }

    internal inline val isPlaying
        get() = try {
            (application as MainApplication).musicPlayer?.isPlaying
        } catch (e: Exception) {
            // on close err
            false
        }

    private inline val curTimeData
        get() = try {
            (application as MainApplication).musicPlayer?.currentPosition
                ?: StorageUtil(applicationContext).loadTrackPauseTime()
        } catch (e: Exception) {
            StorageUtil(applicationContext).loadTrackPauseTime()
        }

    private inline val isMicRecording
        get() = (application as MainApplication).isMicRecording

    private inline val isPlaybackRecording
        get() = (application as MainApplication).isPlaybackRecording

    private val playNewTrackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = playAudio(curPath)
    }

    private val playNextAndUpdateUIReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = playNextAndUpdUI()
    }

    private val playNextOrStopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = playNextOrStop()
    }

    private val highlightTrackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            currentFragment.get()
                    ?.takeIf { it is AbstractTrackListFragment<*> }
                    ?.let {
                        ((it as AbstractTrackListFragment<*>).adapter!!)
                            .highlight(curTrack.unwrap().path)
                    }
        }
    }

    private val customizeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) =
            customize(intent!!.getBooleanExtra(AudioPlayerService.UPD_IMAGE_ARG, true))
    }

    private val releaseAudioVisualizerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = releaseAudioVisualizer()
    }

    private val initAudioVisualizerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = initAudioVisualizer()
    }

    private val prepareForPlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reinitializePlayingCoroutine()
            customize(intent!!.getBooleanExtra(AudioPlayerService.UPD_IMAGE_ARG, false))

            (currentFragment.get() as? AbstractTrackListFragment<*>?)
                ?.adapter
                ?.highlight(curTrack.unwrap().path)
        }
    }

    private val updateLoopingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = updateLooping()
    }

    @Deprecated("Like button is not using anymore. Replaced by audio recording")
    private val setLikeButtonImageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) =
            setRecordButtonImage(intent!!.getBooleanExtra(AudioPlayerService.LIKE_IMAGE_ARG, false))
    }

    private val setRecordButtonImageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) =
            setRecordButtonImage(intent.getBooleanExtra(MicRecordService.RECORD_BUTTON_IMAGE_ARG, false))
    }

    internal companion object {
        // AudioService Broadcasts
        internal const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
        internal const val Broadcast_PLAY_NEW_TRACK = "com.dinaraparanid.prima.PlayNewAudio"
        internal const val Broadcast_RESUME = "com.dinaraparanid.prima.Resume"
        internal const val Broadcast_PAUSE = "com.dinaraparanid.prima.Pause"
        internal const val Broadcast_LOOPING = "com.dinaraparanid.prima.StartLooping"
        internal const val Broadcast_STOP = "com.dinaraparanid.prima.Stop"
        internal const val Broadcast_UPDATE_NOTIFICATION = "com.dinaraparanid.prima.UpdateNotification"
        internal const val Broadcast_REMOVE_NOTIFICATION = "com.dinaraparanid.prima.RemoveNotification"

        // AudioService arguments
        internal const val RESUME_POSITION_ARG = "resume_position"
        internal const val PAUSED_PRESSED_ARG = "pause_pressed"
        internal const val IS_LOOPING_ARG = "is_looping"
        internal const val LOOPING_PRESSED_ARG = "looping_pressed"

        // MicRecordService Broadcast
        internal const val Broadcast_MIC_START_RECORDING = "com.dinaraparanid.prima.MicStartRecording"
        internal const val Broadcast_MIC_STOP_RECORDING = "com.dinaraparanid.prima.MicStopRecording"

        // PlaybackRecordService Broadcast
        internal const val Broadcast_PLAYBACK_START_RECORDING = "com.dinaraparanid.prima.PlaybackStartRecording"
        internal const val Broadcast_PLAYBACK_STOP_RECORDING = "com.dinaraparanid.prima.PlaybackStopRecording"

        // RecordService arguments
        internal const val FILE_NAME_ARG = "filename"

        private const val SHEET_BEHAVIOR_STATE_KEY = "sheet_behavior_state"
        private const val PROGRESS_KEY = "progress"
        private const val TRACK_SELECTED_KEY = "track_selected"

        private const val MEDIA_PROJECTION_REQUEST_CODE = 13
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 14

        /**
         * Calculates time in hh:mm:ss format
         * @param millis millisecond to convert
         * @return int[hh, mm, ss]
         */

        @JvmStatic
        internal fun calcTrackTime(millis: Int) =
            NativeLibrary.calcTrackTime(millis).let { (f, s, t) -> Triple(f, s, t) }

        /**
         * Converts [Triple] to hh:mm:ss formatted string
         * @return "hh:mm:ss"
         */

        @JvmStatic
        internal fun Triple<Int, Int, Int>.asTimeString() =
            "${first.let { if (it < 10) "0$it" else it }}:" +
                    "${second.let { if (it < 10) "0$it" else it }}:" +
                    "${third.let { if (it < 10) "0$it" else it }}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        initView(savedInstanceState)

        registerPlayNewTrackReceiver()
        registerPlayNextAndUpdateUIReceiver()
        registerPlayNextOrStopReceiver()
        registerHighlightTrackReceiver()
        registerCustomizeReceiver()
        registerReleaseAudioVisualizerReceiver()
        registerInitAudioVisualizerReceiver()
        registerPrepareForPlayingReceiver()
        registerUpdateLoopingReceiver()
        registerSetLikeButtonImageReceiver()
        registerMicRecordButtonSetImageReceiver()

        AppUpdater(this)
            .setDisplay(Display.DIALOG)
            .setUpdateFrom(UpdateFrom.GITHUB)
            .setGitHubUserAndRepo("dinaraparanid", "PrimaMobile")
            .setTitleOnUpdateAvailable(R.string.update_available)
            .setButtonUpdate(R.string.update_now)
            .setButtonDismiss(R.string.no_thanks)
            .setButtonDoNotShowAgain(R.string.dont_show_again)
            .start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SHEET_BEHAVIOR_STATE_KEY, sheetBehavior.state)
        outState.putInt(PROGRESS_KEY, viewModel.progressFlow.value)
        outState.putBoolean(TRACK_SELECTED_KEY, viewModel.trackSelectedFlow.value)

        (application as MainApplication).savePauseTime()
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        finishWork()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseAudioVisualizer()
        finishWork()
        _binding = null

        unregisterReceiver(playNewTrackReceiver)
        unregisterReceiver(playNextAndUpdateUIReceiver)
        unregisterReceiver(playNextOrStopReceiver)
        unregisterReceiver(highlightTrackReceiver)
        unregisterReceiver(customizeReceiver)
        unregisterReceiver(releaseAudioVisualizerReceiver)
        unregisterReceiver(initAudioVisualizerReceiver)
        unregisterReceiver(prepareForPlayingReceiver)
        unregisterReceiver(updateLoopingReceiver)
        unregisterReceiver(setLikeButtonImageReceiver)
        unregisterReceiver(setRecordButtonImageReceiver)
    }

    override fun onResume() {
        super.onResume()
        (application as MainApplication).mainActivity = WeakReference(this)

        try {
            Params.setLang(application)
        } catch (ignored: Exception) {
            // already initialized
        }

        binding.playingLayout.run {
            currentTime.text = calcTrackTime(curTimeData).asTimeString()

            trackPlayingBar.run {
                max = curTrack.orNull()?.duration?.toInt() ?: 0
                progress = curTimeData
            }
        }

        try {
            customize(updImage = false, defaultPlaying = false)
        } catch (ignored: Exception) {
            // permissions not given
        }

        if (isPlaying == true)
            playingCoroutine = runOnWorkerThread { runCalculationOfSeekBarPos() }
        
        initAudioVisualizer()
    }

    private inline fun <reified T: MainActivityFragment> isNotCurrent() =
        currentFragment.unchecked !is T

    private fun getMainFragment(pos: Int) = ViewPagerFragment.newInstance(
        binding.mainLabel.text.toString(),
        pos,
        UltimateCollectionFragment::class
    )

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_exit) {
            AlertDialog.Builder(this)
                .setMessage(R.string.exit_request)
                .setPositiveButton(R.string.ok) { d, _ ->
                    d.dismiss()

                    finishWork()

                    runOnWorkerThread {
                        delay(1000)
                        exitProcess(0)
                    }
                }
                .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
                .show()

            return true
        }

        when (item.itemId) {
            R.id.nav_tracks -> when {
                isNotCurrent<DefaultTrackListFragment>() -> getMainFragment(0)
                else -> null
            }

            R.id.nav_playlists -> when {
                isNotCurrent<AlbumListFragment>() && isNotCurrent<PlaylistListFragment>() ->
                    AbstractFragment.defaultInstance(
                        binding.mainLabel.text.toString(),
                        null,
                        TrackCollectionsFragment::class
                    )
                    else -> null
            }

            R.id.nav_artists -> when {
                isNotCurrent<DefaultArtistListFragment>() -> getMainFragment(1)
                else -> null
            }

            R.id.nav_favourite -> when {
                isNotCurrent<FavouriteTrackListFragment>() && isNotCurrent<FavouriteArtistListFragment>() ->
                    AbstractFragment.defaultInstance(
                        binding.mainLabel.text.toString(),
                        null,
                        FavouritesFragment::class
                    )
                    else -> null
            }

            R.id.nav_mp3_converter -> when {
                isNotCurrent<MP3ConverterFragment>() -> getMainFragment(2)
                else -> null
            }

            R.id.nav_guess_the_melody -> when {
                isNotCurrent<GTMMainFragment>() -> getMainFragment(3)
                else -> null
            }

            R.id.nav_settings -> when {
                isNotCurrent<SettingsFragment>() -> getMainFragment(4)
                else -> null
            }

            else -> when {
                isNotCurrent<AboutAppFragment>() -> getMainFragment(5)
                else -> null
            }
        }?.let {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(R.id.fragment_container, it)
                .addToBackStack(null)
                .apply {
                    if (isPlaying == true)
                        binding.playingLayout.playing.visibility = View.VISIBLE
                }
                .commit()
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        when (sheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED ->
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            else -> when {
                binding.drawerLayout.isDrawerOpen(GravityCompat.START) ->
                    binding.drawerLayout.closeDrawer(GravityCompat.START)

                else -> try {
                    when (supportFragmentManager.backStackEntryCount) {
                        0 -> when {
                            --backClicksCount == 0 -> super.onBackPressed()

                            else -> runOnUIThread {
                                Toast.makeText(
                                    applicationContext,
                                    R.string.press_to_exit,
                                    Toast.LENGTH_LONG
                                ).show()
                                setBackingCountToDefault()
                            }
                        }

                        else -> super.onBackPressed()
                    }
                } catch (ignored: Exception) {
                    // Equalizer error
                }
            }
        }
    }

    override fun onTrackSelected(
        track: AbstractTrack,
        tracks: Collection<AbstractTrack>,
        needToPlay: Boolean
    ) {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            if (needToPlay)
                StorageUtil(applicationContext).storeCurPlaylist(
                    (application as MainApplication).curPlaylist.apply {
                        clear()
                        addAll(tracks)
                    }
                )

            (application as MainApplication).playingBarIsVisible = true
            viewModel.trackSelectedFlow.value = true

            try {
                (currentFragment.get() as? Rising?)?.up()
            } catch (ignored: Exception) {
                // Not attached to an activity
            }

            val newTrack = curPath != track.path
            (application as MainApplication).curPath = track.path
            StorageUtil(applicationContext).storeTrackPath(track.path)

            if (newTrack)
                releaseAudioVisualizer()

            val shouldPlay = when {
                (application as MainApplication).isAudioServiceBounded -> if (newTrack) true else !isPlaying!!
                else -> true
            }

            runOnUIThread { updateUIAsync(track to false) }
            setPlayButtonSmallImage(shouldPlay)
            setPlayButtonImage(shouldPlay)

            if (needToPlay) {
                binding.playingLayout.returnButton?.alpha = 0F
                binding.playingLayout.trackSettingsButton.alpha = 0F
                binding.playingLayout.albumPicture.alpha = 0F
            }

            binding.playingLayout.playingTrackTitle.isSelected = true
            binding.playingLayout.playingTrackArtists.isSelected = true

            binding.playingLayout.trackPlayingBar.run {
                max = track.duration.toInt()
                progress = curTimeData
            }

            if (!binding.playingLayout.playing.isVisible)
                binding.playingLayout.playing.isVisible = true

            when {
                needToPlay -> when {
                    shouldPlay -> when {
                        newTrack -> {
                            playAudio(track.path)
                            playingCoroutine = runOnWorkerThread { runCalculationOfSeekBarPos() }
                        }

                        else -> resumePlaying()
                    }

                    else -> pausePlaying()
                }

                else -> if (isPlaying == true)
                    playingCoroutine = runOnWorkerThread { runCalculationOfSeekBarPos() }
            }

            if (newTrack)
                initAudioVisualizer()
        }
    }

    override fun onArtistSelected(artist: Artist) {
        supportFragmentManager
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
                    binding.mainLabel.text.toString(),
                    artist.name,
                    ArtistTrackListFragment::class
                )
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()
    }

    override fun onPlaylistSelected(
        id: Long,
        title: String
    ) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                when (currentFragment.unchecked) {
                    is AlbumListFragment -> AbstractFragment.defaultInstance(
                        resources.getString(R.string.track_collection),
                        title,
                        AlbumTrackListFragment::class
                    )

                    else -> CustomPlaylistTrackListFragment.newInstance(
                        resources.getString(R.string.track_collection),
                        title,
                        id
                    )
                }
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()
    }

    override fun onFontSelected(font: String) {
        supportFragmentManager.popBackStack()
        Params.instance.font = font
        StorageUtil(applicationContext).storeFont(font)
        binding.activityViewModel!!.notifyPropertyChanged(BR._all)
    }

    override suspend fun onTrackSelected(
        track: GeniusTrack,
        target: TrackListFoundFragment.Target
    ) = coroutineScope {
        val awaitDialog = async(Dispatchers.Main) {
            createAndShowAwaitDialog(this@MainActivity, false)
        }

        val createFragment = { fragment: Fragment ->
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(
                    R.id.fragment_container,
                    fragment
                )
                .addToBackStack(null)
                .commit()
        }

        launch(Dispatchers.IO) {
            when (target) {
                TrackListFoundFragment.Target.LYRICS -> {
                    getLyricsFromUrl(track.url)?.let { s ->
                        createFragment(LyricsFragment.newInstance(
                            binding.mainLabel.text.toString(),
                            track.geniusTitle,
                            s
                        ))
                    }

                    launch(Dispatchers.Main) { awaitDialog.await().dismiss() }
                }

                TrackListFoundFragment.Target.INFO -> {
                    GeniusFetcher()
                        .fetchTrackInfoSearch(track.id).run {
                            launch(Dispatchers.Main) {
                                observe(this@MainActivity) {
                                    runOnUIThread { awaitDialog.await().dismiss() }
                                    createFragment(TrackInfoFragment.newInstance(
                                        binding.mainLabel.text.toString(),
                                        it.response.song
                                    ))
                                }
                            }
                        }
                }
            }
        }

        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onImageSelected(image: Bitmap, albumImage: ImageView) {
        Glide.with(currentFragment.unchecked)
            .load(image)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(albumImage.width, albumImage.height)
            .into(albumImage)
    }

    override fun onTrackSelected(
        selectedTrack: Song,
        titleInput: EditText,
        artistInput: EditText,
        albumInput: EditText
    ) {
        titleInput.setText(selectedTrack.title, TextView.BufferType.EDITABLE)
        artistInput.setText(selectedTrack.primaryArtist.name, TextView.BufferType.EDITABLE)
        albumInput.setText(selectedTrack.album?.name ?: "", TextView.BufferType.EDITABLE)
    }

    override fun onPlaylistSelected(
        playlist: AbstractPlaylist,
        fragment: GTMPlaylistSelectFragment
    ) = when (playlist.type) {
        AbstractPlaylist.PlaylistType.GTM -> GuessTheMelodyStartParamsOnlyPlayback()
        else -> GuessTheMelodyStartParamsDialog(playlist, WeakReference(fragment))
    }.show(supportFragmentManager, null)

    override fun showChooseContactFragment(uri: Uri) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                ChooseContactFragment.newInstance(
                    binding.mainLabel.text.toString(),
                    resources.getString(R.string.choose_contact_title),
                    uri
                )
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onContactSelected(contact: Contact, ringtoneUri: Uri) {
        contentResolver.update(
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id.toString()),
            ContentValues().apply {
                put(
                    ContactsContract.Contacts.CUSTOM_RINGTONE,
                    ringtoneUri.toString()
                )
            },
            null, null
        )

        Toast.makeText(
            this,
            "${resources.getString(R.string.success_contact_ringtone)} ${contact.displayName}",
            Toast.LENGTH_SHORT
        ).show()

        supportFragmentManager.popBackStack()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        } catch (ignored: Exception) {
            // first time opening app
        }

        if (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {
            val perms: MutableMap<String, Int> = HashMap()

            perms[Manifest.permission.READ_PHONE_STATE] = PackageManager.PERMISSION_GRANTED
            perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
            perms[Manifest.permission.RECORD_AUDIO] = PackageManager.PERMISSION_GRANTED
            perms[Manifest.permission.READ_CONTACTS] = PackageManager.PERMISSION_GRANTED
            perms[Manifest.permission.WRITE_CONTACTS] = PackageManager.PERMISSION_GRANTED

            if (grantResults.isNotEmpty()) {
                var i = 0
                while (i < permissions.size) {
                    perms[permissions[i]] = grantResults[i]
                    i++
                }

                when {
                    perms[Manifest.permission.READ_PHONE_STATE] ==
                            PackageManager.PERMISSION_GRANTED &&
                            perms[Manifest.permission.READ_EXTERNAL_STORAGE] ==
                            PackageManager.PERMISSION_GRANTED &&
                            perms[Manifest.permission.RECORD_AUDIO] ==
                            PackageManager.PERMISSION_GRANTED &&
                            perms[Manifest.permission.READ_CONTACTS] ==
                            PackageManager.PERMISSION_GRANTED &&
                            perms[Manifest.permission.WRITE_CONTACTS] ==
                            PackageManager.PERMISSION_GRANTED -> Unit // all permissions are granted

                    else -> when {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.READ_EXTERNAL_STORAGE
                        ) || ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.READ_PHONE_STATE
                        ) || ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.RECORD_AUDIO
                        ) || ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.READ_CONTACTS
                        ) || ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.WRITE_CONTACTS
                        ) -> AlertDialog
                            .Builder(this)
                            .setMessage("Phone state and storage permissions required for this app")
                            .setPositiveButton("OK") { _, which ->
                                if (which == DialogInterface.BUTTON_POSITIVE)
                                    (application as MainApplication).checkAndRequestPermissions()
                            }
                            .setNegativeButton("Cancel") { _, _ -> }
                            .create()
                            .show()

                        else -> Toast.makeText(
                            this,
                            "Go to settings and enable permissions, please",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                    "Permissions to capture audio granted. Click the button once again.",
                    Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(this,
                    "Permissions to capture audio denied.",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    /**
     * @param src first - current track
     * @param src second - resume status after activity onPause
     */

    @Synchronized
    override suspend fun updateUIAsync(src: Pair<AbstractTrack, Boolean>) = coroutineScope {
        setRepeatButtonImage()
        setRecordButtonImage(isMicRecording)

        val track = src.first

        val artistAlbum =
            "${
                track.artist
                    .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it }
            } / ${
                NativeLibrary.playlistTitle(
                    track.playlist.toByteArray(),
                    track.path.toByteArray(),
                    resources.getString(R.string.unknown_album).toByteArray()
                )
            }"

        binding.playingLayout.playingTrackTitle.text = track.title.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_track)
                else -> it
            }
        }

        binding.playingLayout.playingTrackArtists.text = track.artist.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_artist)
                else -> it
            }
        }

        binding.playingLayout.trackTitleBig.text = track.title.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_track)
                else -> it
            }
        }

        val time = calcTrackTime(track.duration.toInt()).asTimeString()

        binding.playingLayout.artistsAlbum.text = artistAlbum
        binding.playingLayout.playingTrackTitle.isSelected = true
        binding.playingLayout.playingTrackArtists.isSelected = true
        binding.playingLayout.trackTitleBig.isSelected = true
        binding.playingLayout.artistsAlbum.isSelected = true
        binding.playingLayout.trackLength.text = time

        launch(Dispatchers.Main) {
            val app = application as MainApplication
            val task = app.getAlbumPictureAsync(
                track.path,
                Params.instance.isPlaylistsImagesShown
            ).await()

            if (albumImageWidth == 0) {
                albumImageWidth = binding.playingLayout.albumPicture.width
                albumImageHeight = binding.playingLayout.albumPicture.height
            }

            val drawable = binding
                .playingLayout
                .albumPicture
                .drawable
                .toBitmap(albumImageWidth, albumImageHeight)
                .toDrawable(resources)

            Glide.with(this@MainActivity)
                .load(task)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(drawable)
                .override(albumImageWidth, albumImageHeight)
                .into(binding.playingLayout.albumPicture)

            binding.playingLayout.playingAlbumImage.setImageBitmap(task)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK)
            when (requestCode) {
                ChangeImageFragment.PICK_IMAGE -> runOnUIThread {
                    delay(300)
                    (currentFragment.get() as? ChangeImageFragment)?.setUserImage(data!!.data!!)
                    binding.activityViewModel!!.notifyPropertyChanged(BR._all)
                }

                FoldersActivity.PICK_FOLDER -> data
                    ?.getStringExtra(FoldersActivity.FOLDER_KEY)
                    ?.let {
                        Params.instance.pathToSave = it
                        StorageUtil(applicationContext).storePathToSave(it)
                    }

                MEDIA_PROJECTION_REQUEST_CODE -> {
                    if (SDK_INT >= Build.VERSION_CODES.Q) {
                        PlaybackRecordService.Caller(WeakReference(application as MainApplication))
                            .setFileName(recordFilename)
                            .setExtraData(data)
                            .call()
                    }
                }
            }
    }

    /**
     * Sets play or pause image for small button
     * @param isPlaying is music playing now
     */

    @Synchronized
    internal fun setPlayButtonSmallImage(isPlaying: Boolean) =
        binding.playingLayout.playingPlayButton.run {
            setImageResource(ViewSetter.getPlayButtonSmallImage(isPlaying))
            setTint(Params.instance.fontColor)
        }

    /**
     * Sets play or pause image for big button
     * @param isPlaying is music playing now
     */

    @Synchronized
    internal fun setPlayButtonImage(isPlaying: Boolean) =
        binding.playingLayout.playButton.run {
            setImageResource(ViewSetter.getPlayButtonImage(isPlaying))
            setTint(Params.instance.primaryColor)
        }

    /**
     * Sets looping button image
     * depending on current theme and repeat status
     */

    private fun setRepeatButtonImage() =
        binding.playingLayout.repeatButton.run {
            setImageResource(ViewSetter.getRepeatButtonImage())
            setTint(Params.instance.primaryColor)
        }

    /**
     * Sets record button image
     * depending on current theme and recording status
     * @param isRecording are we recording
     */

    internal fun setRecordButtonImage(isRecording: Boolean) =
        binding.playingLayout.recordButton.run {
            setImageResource(ViewSetter.getRecordButtonImage(isRecording))
            setTint(Params.instance.primaryColor)
        }

    /**
     * Plays next track and updates UI for it
     */

    @Synchronized
    internal fun playNextAndUpdUI() = (application as MainApplication).run {
        viewModel.progressFlow.value = 0

        val curIndex = (curInd + 1).let { if (it == curPlaylist.size) 0 else it }
        curPath = curPlaylist[curIndex].path
        StorageUtil(applicationContext).storeTrackPath(curPath)

        playAudio(curPath)
        setRepeatButtonImage()
    }

    /**
     * Plays previous track and updates UI for it
     */

    @Synchronized
    internal fun playPrevAndUpdUI() = (application as MainApplication).run {
        viewModel.progressFlow.value = 0
        binding.playingLayout.trackPlayingBar.progress = 0

        val curIndex = (curInd - 1).let { if (it < 0) curPlaylist.size - 1 else it }
        curPath = curPlaylist[curIndex].path
        StorageUtil(applicationContext).storeTrackPath(curPath)

        playAudio(curPath)
        setRepeatButtonImage()
        binding.playingLayout.currentTime.setText(R.string.current_time)
    }

    internal fun playNextOrStop() = (application as MainApplication).run {
        if (curInd != curPlaylist.size - 1) playNextAndUpdUI()
    }

    /**
     * Calculates current position for playing seek bar
     */

    internal suspend fun runCalculationOfSeekBarPos() = coroutineScope {
        launch(Dispatchers.Default) {
            var currentPosition = curTimeData
            val total = curTrack.unwrap().duration.toInt()
            binding.playingLayout.trackPlayingBar.max = total

            while (!this@MainActivity.isDestroyed && isPlaying == true && !isSeekBarDragging) {
                currentPosition = curTimeData
                binding.playingLayout.trackPlayingBar.progress = currentPosition
                delay(50)
            }
        }
    }

    /**
     * Plays track with given path
     * @param path path to track (DATA column from MediaStore)
     */

    @Synchronized
    internal fun playAudio(path: String) {
        (application as MainApplication).curPath = path
        StorageUtil(applicationContext).storeTrackPath(path)

        when {
            !(application as MainApplication).isAudioServiceBounded -> {
                val playerIntent = Intent(applicationContext, AudioPlayerService::class.java)

                applicationContext.startService(playerIntent)

                if (SDK_INT >= Build.VERSION_CODES.O)
                    applicationContext.startForegroundService(playerIntent)
                else
                    applicationContext.startService(playerIntent)

                applicationContext.bindService(
                    playerIntent,
                    (application as MainApplication).audioServiceConnection,
                    BIND_AUTO_CREATE
                )
            }

            else -> {
                if (isPlaying == true)
                    pausePlaying()

                StorageUtil(applicationContext).storeTrackPath(path)
                sendBroadcast(Intent(Broadcast_PLAY_NEW_TRACK))
            }
        }
    }

    /**
     * Resumes playing after pause
     * @param resumePos resume position in milliseconds
     * (or -1 to continue from paused position)
     */

    @Synchronized
    internal fun resumePlaying(resumePos: Int = -1) = when {
        !(application as MainApplication).isAudioServiceBounded -> {
            StorageUtil(applicationContext).apply {
                storeTrackPath(curPath)
            }

            val playerIntent = Intent(applicationContext, AudioPlayerService::class.java)
                .putExtra(RESUME_POSITION_ARG, resumePos)

            applicationContext.startService(playerIntent)

            applicationContext.bindService(
                playerIntent,
                (application as MainApplication).audioServiceConnection,
                BIND_AUTO_CREATE
            )
            Unit
        }

        else -> {
            if (isPlaying == true)
                pausePlaying()

            StorageUtil(applicationContext).storeTrackPath(curPath)

            sendBroadcast(
                Intent(Broadcast_RESUME).putExtra(
                    RESUME_POSITION_ARG,
                    resumePos
                )
            )
        }
    }

    /**
     * Pauses playing and stores data
     * to [SharedPreferences] if user wishes it
     */

    @Synchronized
    internal fun pausePlaying() = when {
        (application as MainApplication).isAudioServiceBounded -> sendBroadcast(Intent(Broadcast_PAUSE))

        else -> {
            StorageUtil(applicationContext).apply {
                storeTrackPath(curPath)
                storeTrackPauseTime(curTimeData)
            }

            val playerIntent = Intent(applicationContext, AudioPlayerService::class.java)
                .setAction(PAUSED_PRESSED_ARG)

            applicationContext.startService(playerIntent)

            applicationContext.bindService(
                playerIntent,
                (application as MainApplication).audioServiceConnection,
                BIND_AUTO_CREATE
            )

            Unit
        }
    }

    /**
     * Sets [Params.Looping] status for [AudioPlayerService]
     */

    @Synchronized
    private fun setLooping() = when {
        (application as MainApplication).isAudioServiceBounded -> sendBroadcast(
            Intent(Broadcast_LOOPING)
                .putExtra(IS_LOOPING_ARG, Params.instance.loopingStatus.ordinal)
        )

        else -> {
            StorageUtil(applicationContext).apply {
                storeTrackPath(curPath)
                storeTrackPauseTime(curTimeData)
            }

            val playerIntent = Intent(this, AudioPlayerService::class.java)
                .setAction(LOOPING_PRESSED_ARG)

            applicationContext.startService(playerIntent)

            applicationContext.bindService(
                playerIntent,
                (application as MainApplication).audioServiceConnection,
                BIND_AUTO_CREATE
            )

            Unit
        }
    }

    /**
     * Shows popup menu about track
     * @param view settings button view
     * @param track [AbstractTrack] to modify
     * @param bottomSheetBehaviorState state in which function executes
     */

    internal fun onTrackSettingsButtonClicked(
        view: View,
        track: AbstractTrack,
        bottomSheetBehaviorState: Int
    ) {
        if (sheetBehavior.state == bottomSheetBehaviorState)
            PopupMenu(this, view).run {
                menuInflater.inflate(R.menu.menu_track_settings, menu)

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.nav_change_track_info -> changeTrackInfo(track)
                        R.id.nav_add_to_queue -> addTrackToQueue(track)
                        R.id.nav_remove_from_queue -> removeTrackFromQueue(track)
                        R.id.nav_add_track_to_favourites -> onTrackLikedClicked(track)
                        R.id.nav_add_to_playlist -> addToPlaylistAsync(track)
                        R.id.nav_remove_track -> removeTrack(track)
                        R.id.nav_track_lyrics -> showLyrics(track)
                        R.id.nav_track_info -> showInfo(track)
                        R.id.nav_trim -> trimTrack(track)
                    }

                    true
                }

                show()
            }
    }

    /**
     * Shows popup menu about artist
     * @param view settings button view
     * @param artist [Artist] to modify
     */

    internal fun artistSettingsButtonAction(
        view: View,
        artist: Artist,
    ) {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
            PopupMenu(this, view).apply {
                menuInflater.inflate(R.menu.menu_artist_settings, menu)

                setOnMenuItemClickListener {
                    val contain = runBlocking {
                        FavouriteRepository.instance.getArtistAsync(artist.name).await()
                    } != null

                    val favouriteArtist = artist.asFavourite()

                    runOnIOThread {
                        when {
                            contain -> FavouriteRepository.instance.removeArtistAsync(favouriteArtist)
                            else -> FavouriteRepository.instance.addArtistAsync(favouriteArtist)
                        }
                    }

                    return@setOnMenuItemClickListener true
                }

                show()
            }
    }

    /**
     * Call like action when like button pressed.
     * Add or removes track from favourite tracks
     * @param track track to add / remove
     */

    private fun onTrackLikedClicked(track: AbstractTrack) {
        val contain = runBlocking {
            FavouriteRepository.instance.getTrackAsync(track.path).await()
        } != null

        val favouriteTrack = track.asFavourite()

        runOnIOThread {
            when {
                contain -> FavouriteRepository.instance.removeTrackAsync(favouriteTrack)
                else -> FavouriteRepository.instance.addTrackAsync(favouriteTrack)
            }.join()

            if (currentFragment.get() is FavouriteTrackListFragment)
                (currentFragment.unchecked as FavouriteTrackListFragment).updateUIOnChangeTracks()
        }
    }

    /**
     * Runs [TrackChangeFragment]
     * @param track [AbstractTrack] to change
     */

    private fun changeTrackInfo(track: AbstractTrack) {
        when (SDK_INT) {
            Build.VERSION_CODES.Q -> {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    track.androidId
                )

                try {
                    contentResolver.openFileDescriptor(uri, "w")
                        ?.use { showTrackChangeFragment(track) }
                } catch (securityException: SecurityException) {
                    if (SDK_INT >= Build.VERSION_CODES.Q) {
                        val recoverableSecurityException = securityException as?
                                RecoverableSecurityException
                            ?: throw RuntimeException(
                                securityException.message,
                                securityException
                            )

                        recoverableSecurityException
                            .userAction
                            .actionIntent
                            .intentSender
                            .let {
                                startIntentSenderForResult(
                                    it, 125,
                                    null, 0, 0, 0, null
                                )
                            }
                    }
                }
            }

            else -> showTrackChangeFragment(track)
        }
    }

    /**
     * Adds track to queue
     * @param track [AbstractTrack] to add
     */

    private fun addTrackToQueue(track: AbstractTrack) =
        (application as MainApplication).curPlaylist.add(track)

    /**
     * Removes track from queue
     * @param track [AbstractTrack] to remove
     */

    private fun removeTrackFromQueue(track: AbstractTrack) = (application as MainApplication).run {
        when (track.path) {
            curPath -> {
                val removedPath = curPath
                pausePlaying()
                curPlaylist.remove(track)

                curPath = try {
                    curPlaylist.currentTrack.path
                } catch (e: Exception) {
                    // Last track in current playlist was removed
                    curPlaylist.add(track)
                    removedPath
                }

                curPath.takeIf { it != Params.NO_PATH && it != removedPath }?.let(::playAudio)
                    ?: resumePlaying()
            }

            else -> curPlaylist.remove(track)
        }

        runOnUIThread {
            (currentFragment.unchecked as AbstractTrackListFragment<*>).updateUIOnChangeTracks()
        }
    }

    /**
     * Adds track to playlist asynchronously
     * @param track [AbstractTrack] to add
     */

    private fun addToPlaylistAsync(track: AbstractTrack) = runOnIOThread {
        val task = CustomPlaylistsRepository.instance
            .getPlaylistsByTrackAsync(track.path)

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                PlaylistSelectFragment.newInstance(
                    binding.mainLabel.text.toString(),
                    track,
                    CustomPlaylist.Entity.EntityList(task.await())
                )
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()
    }

    /**
     * Removes track from playlist
     * @param track [AbstractTrack] to remove
     */

    private fun removeTrack(track: AbstractTrack) = AreYouSureDialog(
        R.string.remove_track_message
    ) {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            track.androidId
        )

        when {
            SDK_INT >= 30 -> try {
                startIntentSenderForResult(
                    MediaStore.createDeleteRequest(contentResolver, listOf(uri)).intentSender,
                    3, null, 0, 0, 0
                )

            } catch (ignored: Exception) {
            }

            else -> {
                contentResolver.delete(
                    uri,
                    "${MediaStore.Audio.Media._ID} = ?",
                    arrayOf(track.androidId.toString())
                )

                try {
                    File(track.path).delete()
                } catch (securityException: SecurityException) {
                    if (SDK_INT >= Build.VERSION_CODES.Q) {
                        val recoverableSecurityException = securityException as?
                                RecoverableSecurityException
                            ?: throw RuntimeException(
                                securityException.message,
                                securityException
                            )

                        recoverableSecurityException
                            .userAction
                            .actionIntent
                            .intentSender
                            .let {
                                startIntentSenderForResult(
                                    it, 125,
                                    null, 0, 0, 0, null
                                )

                                File(track.path).delete()
                            }
                    }
                }
            }
        }

        runOnIOThread {
            (currentFragment.unchecked as AbstractTrackListFragment<*>)
                .updateUIOnChangeTracks()
        }

    }.show(supportFragmentManager, null)

    /**
     * Shows dialog to input title and artist to search for lyrics
     * @param track searchable track
     */

    private fun showLyrics(track: AbstractTrack) =
        TrackSearchLyricsParamsDialog(track, binding.mainLabel.text.toString())
            .show(supportFragmentManager, null)

    /**
     * Shows dialog to input title and artist to search for info
     * @param track searchable track
     */

    private fun showInfo(track: AbstractTrack) =
        TrackSearchInfoParamsDialog(track, binding.mainLabel.text.toString())
            .show(supportFragmentManager, null)

    /**
     * Update UI on service notification clicks
     * @param updImage does track image need update
     * @param defaultPlaying needs default playing
     */

    @Synchronized
    internal fun customize(updImage: Boolean, defaultPlaying: Boolean = true) {
        val p = isPlaying ?: defaultPlaying
        setPlayButtonImage(p)
        setPlayButtonSmallImage(p)

        if (updImage) curTrack.takeIf { it != None }?.unwrap()?.let {
            runOnUIThread { updateUIAsync(it to true) }
        }
    }

    /**
     * Pauses or resumes playing
     */

    @Synchronized
    private fun handlePlayEvent() = when (isPlaying) {
        true -> {
            pausePlaying()
            viewModel.progressFlow.value = curTimeData
        }

        else -> {
            resumePlaying()
            playingCoroutine = runOnWorkerThread {
                runCalculationOfSeekBarPos()
            }
        }
    }

    /**
     * Reinitializes playing coroutine to show time
     */

    @Synchronized
    internal fun reinitializePlayingCoroutine() {
        playingCoroutine = runOnWorkerThread {
            runCalculationOfSeekBarPos()
        }
    }

    /**
     * Sets rounding of playlists images
     * for different configurations of devices
     */

    internal fun setRoundingOfPlaylistImage() =
        binding.playingLayout.albumPicture.setCornerRadius(
            when {
                !Params.instance.isRoundingPlaylistImage -> 0F
                else -> when (resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK)) {
                    Configuration.SCREENLAYOUT_SIZE_NORMAL -> 50F
                    Configuration.SCREENLAYOUT_SIZE_LARGE -> 60F
                    else -> 40F
                }
            }
        )

    /**
     * Shows real playlist's image or default
     */

    internal fun setShowingPlaylistImage() = runOnUIThread {
        Glide.with(this@MainActivity).load(
            (application as MainApplication).getAlbumPictureAsync(
                curTrack.orNull()?.path ?: "",
                Params.instance.isPlaylistsImagesShown
            ).await().also(binding.playingLayout.playingAlbumImage::setImageBitmap)
        ).into(binding.playingLayout.albumPicture)
    }

    /**
     * Initialises audio visualizer
     */

    internal fun initAudioVisualizer() = try {
        binding.playingLayout.visualizer.run {
            setAnimationSpeed(AnimSpeed.FAST)
            setColor(Params.instance.primaryColor)
            setAudioSessionId((((application as MainApplication).audioSessionId) ?: 0))
        }
    } catch (e: Exception) {
        // already initialized
        e.printStackTrace()
        releaseAudioVisualizer()
        binding.playingLayout.visualizer.run {
            setAnimationSpeed(AnimSpeed.FAST)
            setColor(Params.instance.primaryColor)
            setAudioSessionId((((application as MainApplication).audioSessionId) ?: 0))
        }
    }

    internal fun releaseAudioVisualizer() = binding.playingLayout.visualizer.release()

    private fun showTrackChangeFragment(track: AbstractTrack) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                TrackChangeFragment.newInstance(
                    binding.mainLabel.text.toString(),
                    resources.getString(R.string.change_track_s_information),
                    track,
                )
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()

        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /** Updates looping status in activity */

    internal fun updateLooping() {
        Params.instance.loopingStatus++
        setLooping()
        setRepeatButtonImage()
    }

    internal fun liftPlayingMenu() {
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    internal fun onPlayingPrevTrackClicked() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
            playPrevAndUpdUI()
    }

    internal fun onPlayingNextTrackClicked() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
            playNextAndUpdUI()
    }

    internal fun onRecordButtonClicked() = when {
        isMicRecording -> {
            sendBroadcast(Intent(Broadcast_MIC_STOP_RECORDING))
            setRecordButtonImage(false)
        }

        isPlaybackRecording -> {
            sendBroadcast(Intent(Broadcast_PLAYBACK_STOP_RECORDING))
            setRecordButtonImage(false)
        }

        else -> RecordParamsDialog(this).show()
    }

    internal fun onPlaylistButtonClicked() {
        supportFragmentManager
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
                    binding.mainLabel.text.toString(),
                    resources.getString(R.string.current_playlist),
                    CurPlaylistTrackListFragment::class
                )
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()
    }

    internal fun onSleepTimerClicked() =
        SleepDialog(application as MainApplication)
            .show(supportFragmentManager, null)

    internal fun onReturnButtonClicked() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    internal fun onTrackSettingsButtonClicked(view: View) = onTrackSettingsButtonClicked(
        view,
        curTrack.unwrap(),
        BottomSheetBehavior.STATE_EXPANDED
    )

    internal fun onPlayButtonClicked() {
        setPlayButtonImage(isPlaying?.let { !it } ?: true)
        handlePlayEvent()
    }

    internal fun onPlayingPlayButtonClicked() {
        setPlayButtonSmallImage(isPlaying?.let { !it } ?: true)
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
            handlePlayEvent()
    }

    internal fun onEqualizerButtonClicked() = when (isPlaying) {
        null -> Toast.makeText(
            applicationContext,
            R.string.first_play,
            Toast.LENGTH_LONG
        ).show()

        else -> try {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(
                    R.id.fragment_container,
                    EqualizerFragment.newInstance(
                        binding.mainLabel.text.toString(),
                        (application as MainApplication).audioSessionId!!
                    )
                )
                .addToBackStack(null)
                .commit()

            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            else Unit
        } catch (e: Exception) {
            // AudioService is dead

            Toast.makeText(
                applicationContext,
                R.string.first_play,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    internal fun onTrimButtonClicked() = trimTrack(curTrack.unwrap())

    override fun initView(savedInstanceState: Bundle?) {
        _binding = when (Params.instance.visualizerStyle) {
            Params.Companion.VisualizerStyle.BAR -> Either.Left(
                DataBindingUtil
                    .setContentView<ActivityMainBarBinding>(this, R.layout.activity_main_bar)
                    .apply {
                        val vm = com.dinaraparanid.prima.viewmodels.mvvm.MainActivityViewModel(
                            WeakReference(this@MainActivity)
                        )

                        playingLayoutBar.viewModel = vm
                        viewModel = vm

                        val headerBinding = DataBindingUtil.inflate<NavHeaderMainBinding>(
                            layoutInflater,
                            R.layout.nav_header_main,
                            navView,
                            false
                        )

                        navView.addHeaderView(headerBinding.root)
                        headerBinding.viewModel = ViewModel()
                        executePendingBindings()
                    }
            )

            Params.Companion.VisualizerStyle.WAVE -> Either.Right(
                DataBindingUtil
                    .setContentView<ActivityMainWaveBinding>(this, R.layout.activity_main_wave)
                    .apply {
                        val vm = com.dinaraparanid.prima.viewmodels.mvvm.MainActivityViewModel(
                            WeakReference(this@MainActivity)
                        )

                        playingLayoutWave.viewModel = vm
                        viewModel = vm

                        val headerBinding = DataBindingUtil.inflate<NavHeaderMainBinding>(
                            layoutInflater,
                            R.layout.nav_header_main,
                            navView,
                            false
                        )

                        navView.addHeaderView(headerBinding.root)
                        headerBinding.viewModel = ViewModel()
                        executePendingBindings()
                    }
            )
        }

        awaitBindingInitLock.withLock(awaitBindingInitCondition::signal)

        Params.instance.backgroundImage?.run {
            binding.drawerLayout.background = toBitmap().toDrawable(resources)
        }

        viewModel.run {
            load(
                savedInstanceState?.getInt(SHEET_BEHAVIOR_STATE_KEY),
                savedInstanceState?.getInt(PROGRESS_KEY),
                savedInstanceState?.getBoolean(TRACK_SELECTED_KEY),
            )

            if (progressFlow.value == -1) {
                progressFlow.value = StorageUtil(applicationContext).loadTrackPauseTime()

                (application as MainApplication).curPath = when (progressFlow.value) {
                    -1 -> Params.NO_PATH
                    else -> StorageUtil(applicationContext).loadTrackPath()
                }
            }
        }

        setSupportActionBar(binding.switchToolbar)
        setRoundingOfPlaylistImage()
        binding.playingLayout.currentTime.text = calcTrackTime(curTimeData).asTimeString()

        (application as MainApplication).run {
            mainActivity = WeakReference(this@MainActivity)
            runOnWorkerThread { loadAsync().join() }
        }

        Glide.with(this)
            .load(ViewSetter.getRecordButtonImage(isMicRecording))
            .into(binding.playingLayout.recordButton)

        binding.playingLayout.trackPlayingBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isSeekBarDragging = true
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.playingLayout.currentTime.text =
                        calcTrackTime(progress).asTimeString()

                    if (ceil(progress / 1000.0).toInt() == 0 && isPlaying == false)
                        binding.playingLayout.trackPlayingBar.progress = 0
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) =
                    (application as MainApplication).run {
                        isSeekBarDragging = false

                        val time = seekBar!!.progress

                        if (isPlaying == true)
                            pausePlaying()

                        resumePlaying(time)

                        playingCoroutine = runOnWorkerThread {
                            runCalculationOfSeekBarPos()
                        }
                    }
            }
        )

        setPlayButtonImage(isPlaying ?: false)
        setPlayButtonSmallImage(isPlaying ?: false)

        (application as MainApplication).apply {
            mainActivity = WeakReference(this@MainActivity)
            curPath.takeIf { it != Params.NO_PATH }?.let { highlightedRow = Some(curPath) }
        }

        initFirstFragment()

        sheetBehavior = BottomSheetBehavior.from(binding.playingLayout.playing)

        if (viewModel.trackSelectedFlow.value ||
            viewModel.progressFlow.value != -1
        ) {
            when (viewModel.sheetBehaviorPositionFlow.value) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    binding.playingLayout.returnButton?.alpha = 1F
                    binding.playingLayout.trackSettingsButton.alpha = 1F
                    binding.playingLayout.albumPicture.alpha = 1F
                    binding.appbar.alpha = 0F
                    binding.playingLayout.playingToolbar.alpha = 0F
                    binding.playingLayout.playingTrackTitle.isSelected = true
                    binding.playingLayout.playingTrackArtists.isSelected = true
                    binding.switchToolbar.isVisible = false
                }

                else -> {
                    binding.playingLayout.returnButton?.alpha = 0F
                    binding.playingLayout.trackSettingsButton.alpha = 0F
                    binding.playingLayout.albumPicture.alpha = 0F
                    binding.appbar.alpha = 1F
                    binding.playingLayout.playingToolbar.alpha = 1F
                    binding.playingLayout.playingTrackTitle.isSelected = true
                    binding.playingLayout.playingTrackArtists.isSelected = true
                    binding.switchToolbar.isVisible = true
                }
            }

            if (curPath != Params.NO_PATH)
                viewModel.trackSelectedFlow.value = true

            curTrack.takeIf { it != None }
                ?.let {
                    (application as MainApplication).startPath =
                        if (curPath == Params.NO_PATH) None else Some(curPath)
                    initPlayingView(it.unwrap())
                }
        }

        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) = when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        val binding = binding
                        binding.playingLayout.returnButton?.alpha = 1F
                        binding.playingLayout.trackSettingsButton.alpha = 1F
                        binding.playingLayout.albumPicture.alpha = 1F
                        binding.appbar.alpha = 0F
                        binding.playingLayout.playingToolbar.alpha = 0F
                        binding.playingLayout.playingTrackTitle.isSelected = true
                        binding.playingLayout.playingTrackArtists.isSelected = true
                        binding.switchToolbar.isVisible = false
                    }

                    else -> Unit
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val binding = binding

                    if (!binding.switchToolbar.isVisible)
                        binding.switchToolbar.isVisible = true

                    val p = isPlaying ?: false
                    setPlayButtonSmallImage(p)
                    setPlayButtonImage(p)
                    binding.playingLayout.trimButton.setTint(Params.instance.primaryColor)

                    binding.appbar.alpha = 1 - slideOffset
                    binding.playingLayout.playingToolbar.alpha = 1 - slideOffset
                    binding.playingLayout.returnButton?.alpha = slideOffset
                    binding.playingLayout.trackSettingsButton.alpha = slideOffset
                    binding.playingLayout.albumPicture.alpha = slideOffset
                    binding.playingLayout.playingTrackTitle.isSelected = true
                    binding.playingLayout.playingTrackArtists.isSelected = true
                }
            }
        )

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.switchToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.run {
            setNavigationItemSelectedListener(this@MainActivity)
            itemIconTintList = ViewSetter.colorStateList
        }

        if (curPath != Params.NO_PATH) {
            setPlayButtonSmallImage(isPlaying ?: false)

            if (viewModel.sheetBehaviorPositionFlow.value ==
                BottomSheetBehavior.STATE_EXPANDED
            ) sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val tv = TypedValue()

        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarSize = TypedValue
                .complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
    }

    override fun initFirstFragment() {
        currentFragment = WeakReference(
            supportFragmentManager.findFragmentById(R.id.fragment_container)
        )

        if (currentFragment.get() == null)
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    when (Params.instance.homeScreen) {
                        Params.Companion.HomeScreen.CURRENT_PLAYLIST -> AbstractFragment.defaultInstance(
                            binding.mainLabel.text.toString(),
                            resources.getString(R.string.current_playlist),
                            CurPlaylistTrackListFragment::class
                        )

                        Params.Companion.HomeScreen.TRACK_COLLECTION -> AbstractFragment.defaultInstance(
                            binding.mainLabel.text.toString(),
                            null,
                            TrackCollectionsFragment::class
                        )

                        Params.Companion.HomeScreen.FAVOURITES -> AbstractFragment.defaultInstance(
                            binding.mainLabel.text.toString(),
                            null,
                            FavouritesFragment::class
                        )

                        Params.Companion.HomeScreen.TRACKS -> getMainFragment(0)
                        Params.Companion.HomeScreen.ARTISTS -> getMainFragment(1)
                        Params.Companion.HomeScreen.MP3_CONVERTER -> getMainFragment(2)
                        Params.Companion.HomeScreen.GUESS_THE_MELODY -> getMainFragment(3)
                        Params.Companion.HomeScreen.SETTINGS -> getMainFragment(4)
                        Params.Companion.HomeScreen.ABOUT_APP -> getMainFragment(5)
                    }
                )
                .commit()
    }

    /**
     * Initializes playing view
     * when [onCreate] and [onResume] called
     *
     * @param track that should be played
     */

    private fun initPlayingView(track: AbstractTrack) = onTrackSelected(
        track,
        (application as MainApplication).allTracks,
        needToPlay = false // Only for playing panel
    )

    private fun getLyricsFromUrl(url: String): String? {
        var elem: Element? = null

        while (elem == null)
            try {
                elem = Jsoup.connect(url).get()
                    .select("div[class=lyrics]")
                    .first()?.select("p")
                    ?.first()
            } catch (e: UnknownHostException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        R.string.no_internet_connection,
                        Toast.LENGTH_LONG
                    ).show()
                }

                return null
            }

        return elem.wholeText()
    }

    /**
     * Sets bloom (shadow) color if settings have changed
     * @param color to set
     */

    internal fun setBloomColor(color: Int) = binding.playingLayout.run {
        trackSettingsButton.setShadowColor(color)
        albumPicture.setShadowColor(color)
        playButton.setShadowColor(color)
        previousTrackButton.setShadowColor(color)
        nextTrackButton.setShadowColor(color)
        equalizerButton.setShadowColor(color)
        repeatButton.setShadowColor(color)
        sleepTimer.setShadowColor(color)
        recordButton.setShadowColor(color)
        playlistButton.setShadowColor(color)
        trimButton.setShadowColor(color)
        returnButton?.setShadowColor(color)
    }

    /**
     * Makes background invisible and sets given [image] as background image
     * @param image to set on background
     */

    internal fun updateViewOnUserImageSelected(image: Uri) {
        val cr = contentResolver
        val stream = cr.openInputStream(image)!!
        val bytes = stream.buffered().use(BufferedInputStream::readBytes)

        StorageUtil(applicationContext).storeBackgroundImage(bytes)
        Params.instance.backgroundImage = bytes

        val transparent = resources.getColor(android.R.color.transparent)

        binding.run {
            appbar.setBackgroundColor(transparent)
            switchToolbar.setBackgroundColor(transparent)
            drawerLayout.background =
                Drawable.createFromStream(cr.openInputStream(image)!!, image.toString())
        }

        binding.activityViewModel!!.notifyPropertyChanged(BR._all)
    }

    /**
     * Sets colors on background
     * if user chose to remove background image
     */

    internal fun updateBackgroundViewOnRemoveUserImage() = binding.run {
        drawerLayout.setBackgroundColor(Params.instance.secondaryColor)
        appbar.setBackgroundColor(Params.instance.primaryColor)
        switchToolbar.setBackgroundColor(Params.instance.primaryColor)
    }

    /**
     * Should be called before [MainActivity] is stopped or destroyed.
     * Saves time and releases everything
     */

    internal fun finishWork() {
        (application as MainApplication).run {
            savePauseTime()
            mainActivity = WeakReference(null)
        }

        playingCoroutine?.cancel(null)
        playingCoroutine = null
    }

    private fun registerPlayNewTrackReceiver() = registerReceiver(
        playNewTrackReceiver,
        IntentFilter(AudioPlayerService.Broadcast_PLAY_NEW_TRACK)
    )

    private fun registerPlayNextAndUpdateUIReceiver() = registerReceiver(
        playNextAndUpdateUIReceiver,
        IntentFilter(AudioPlayerService.Broadcast_PLAY_NEXT_AND_UPDATE_UI)
    )

    private fun registerPlayNextOrStopReceiver() = registerReceiver(
        playNextOrStopReceiver,
        IntentFilter(AudioPlayerService.Broadcast_PLAY_NEXT_OR_STOP)
    )

    private fun registerHighlightTrackReceiver() = registerReceiver(
        highlightTrackReceiver,
        IntentFilter(AudioPlayerService.Broadcast_HIGHLIGHT_TRACK)
    )

    private fun registerCustomizeReceiver() = registerReceiver(
        customizeReceiver,
        IntentFilter(AudioPlayerService.Broadcast_CUSTOMIZE)
    )

    private fun registerReleaseAudioVisualizerReceiver() = registerReceiver(
        releaseAudioVisualizerReceiver,
        IntentFilter(AudioPlayerService.Broadcast_RELEASE_AUDIO_VISUALIZER)
    )

    private fun registerInitAudioVisualizerReceiver() = registerReceiver(
        initAudioVisualizerReceiver,
        IntentFilter(AudioPlayerService.Broadcast_INIT_AUDIO_VISUALIZER)
    )

    private fun registerPrepareForPlayingReceiver() = registerReceiver(
        prepareForPlayingReceiver,
        IntentFilter(AudioPlayerService.Broadcast_PREPARE_FOR_PLAYING)
    )

    private fun registerUpdateLoopingReceiver() = registerReceiver(
        updateLoopingReceiver,
        IntentFilter(AudioPlayerService.Broadcast_UPDATE_LOOPING)
    )

    @Deprecated("Like button is not using anymore. Replaced by audio recording")
    private fun registerSetLikeButtonImageReceiver() = registerReceiver(
        setLikeButtonImageReceiver,
        IntentFilter(AudioPlayerService.Broadcast_SET_LIKE_BUTTON_IMAGE)
    )

    private fun registerMicRecordButtonSetImageReceiver() = registerReceiver(
        setRecordButtonImageReceiver,
        IntentFilter(MicRecordService.Broadcast_SET_RECORD_BUTTON_IMAGE)
    )

    private suspend fun setBackingCountToDefault() = coroutineScope {
        delay(1000)
        backClicksCount = 2
    }

    private fun trimTrack(track: AbstractTrack) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                TrimFragment.newInstance(
                    binding.mainLabel.text.toString(),
                    resources.getString(R.string.trim_audio),
                    track
                )
            )
            .addToBackStack(null)
            .commit()

        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private inline val isRecordAudioPermissionGranted
        get() = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            ),
            RECORD_AUDIO_PERMISSION_REQUEST_CODE
        )
    }

    internal fun startMediaProjectionRequest() {
        mediaProjectionManager = applicationContext
            .getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            MEDIA_PROJECTION_REQUEST_CODE
        )
    }
}