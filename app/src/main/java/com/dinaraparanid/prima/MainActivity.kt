package com.dinaraparanid.prima

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.chibde.visualizer.LineBarVisualizer
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.fragments.*
import com.dinaraparanid.prima.fragments.EqualizerFragment
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.dialogs.AreYouSureDialog
import com.dinaraparanid.prima.utils.dialogs.GetHappiApiKeyDialog
import com.dinaraparanid.prima.utils.extensions.unwrap
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.rustlibs.NativeLibrary
import com.dinaraparanid.prima.utils.web.FoundTrack
import com.dinaraparanid.prima.utils.web.HappiFetcher
import com.dinaraparanid.prima.utils.web.LyricsParser
import com.dinaraparanid.prima.viewmodels.MainActivityViewModel
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.gson.GsonBuilder
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.ceil

class MainActivity :
    AppCompatActivity(),
    AbstractTrackListFragment.Callbacks,
    AbstractArtistListFragment.Callbacks,
    PlaylistListFragment.Callbacks,
    FontsFragment.Callbacks,
    TrackSelectLyricsFragment.Callbacks,
    NavigationView.OnNavigationItemSelectedListener,
    UIUpdatable<Pair<Track, Boolean>> {
    private lateinit var playingPart: ConstraintLayout
    private lateinit var appBarLayout: carbon.beta.AppBarLayout
    internal lateinit var selectButton: ImageButton
    internal lateinit var mainLabel: TextView
    internal lateinit var toolbar: Toolbar

    private lateinit var trackLayout: ConstraintLayout
    private lateinit var settingsButton: ImageView
    private lateinit var albumImage: ImageView
    private lateinit var trackPlayingBar: SeekBar
    private lateinit var curTime: TextView
    private lateinit var trackLength: TextView
    private lateinit var trackTitle: TextView
    private lateinit var artistsAlbum: TextView
    private lateinit var playButton: ImageView
    private lateinit var prevTrackButton: ImageView
    private lateinit var nextTrackButton: ImageView
    private lateinit var likeButton: ImageView
    private lateinit var repeatButton: ImageView
    private lateinit var playlistButton: ImageView
    private lateinit var trackLyricsButton: ImageView
    private lateinit var equalizerButton: ImageView
    private lateinit var returnButton: ImageView

    private lateinit var playingToolbar: Toolbar
    private lateinit var albumImageSmall: CircleImageView
    private lateinit var trackTitleSmall: TextView
    private lateinit var trackArtists: TextView
    private lateinit var playButtonSmall: ImageView
    private lateinit var prevTrackButtonSmall: ImageView
    private lateinit var nextTrackButtonSmall: ImageView
    private lateinit var audioVisualizer: LineBarVisualizer

    internal val mainActivityViewModel: MainActivityViewModel by lazy {
        ViewModelProvider(this)[MainActivityViewModel::class.java]
    }

    internal var currentFragment: Fragment? = null
    internal lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var favouriteRepository: FavouriteRepository

    private var playingCoroutine: Option<Job> = None
    private var draggingSeekBar = false
    private var actionBarSize = 0
    internal var upped = false
    internal var needToUpdate = false
    internal var playingToolbarSize = 0
        private set

    private inline val curTrack
        get() = (application as MainApplication).run {
            curPath.takeIf { it != NO_PATH }
                ?.let {
                    Some(
                        curPlaylist.run { get(indexOfFirst { track -> track.path == it }) }
                    )
                }
                ?: None
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

    private inline val isLooping
        get() = try {
            (application as MainApplication).musicPlayer?.isLooping
        } catch (e: Exception) {
            // on close err
            false
        }

    private inline val curTimeData
        get() = try {
            (application as MainApplication).musicPlayer?.currentPosition
        } catch (e: Exception) {
            StorageUtil(applicationContext).loadTrackPauseTime()
        }

    internal val playingToolbarHeight get() = playingToolbar.height

    companion object {
        const val REQUEST_ID_MULTIPLE_PERMISSIONS: Int = 1
        const val Broadcast_PLAY_NEW_TRACK: String = "com.dinaraparanid.prima.PlayNewAudio"
        const val Broadcast_RESUME: String = "com.dinaraparanid.prima.Resume"
        const val Broadcast_PAUSE: String = "com.dinaraparanid.prima.Pause"
        const val Broadcast_LOOPING: String = "com.dinaraparanid.prima.StartLooping"
        const val Broadcast_STOP: String = "com.dinaraparanid.prima.Stop"

        const val RESUME_POSITION_ARG: String = "resume_position"
        const val PAUSED_PRESSED_ARG: String = "pause_pressed"
        const val IS_LOOPING_ARG: String = "is_looping"
        const val LOOPING_PRESSED_ARG: String = "looping_pressed"

        private const val SHEET_BEHAVIOR_STATE_KEY = "sheet_behavior_state"
        private const val PROGRESS_KEY = "progress"
        private const val TRACK_SELECTED_KEY = "track_selected"

        internal const val NO_PATH = "_____ЫЫЫЫЫЫЫЫ_____"

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
        setTheme(ViewSetter.appTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        favouriteRepository = FavouriteRepository.instance

        mainActivityViewModel.run {
            load(
                savedInstanceState?.getInt(SHEET_BEHAVIOR_STATE_KEY),
                savedInstanceState?.getInt(PROGRESS_KEY),
                savedInstanceState?.getBoolean(TRACK_SELECTED_KEY),
            )

            if (progressLiveData.value == -1) {
                progressLiveData.value = StorageUtil(applicationContext).loadTrackPauseTime()

                (application as MainApplication).curPath = when (progressLiveData.value) {
                    -1 -> NO_PATH
                    else -> StorageUtil(applicationContext).loadTrackPath()
                }
            }
        }

        appBarLayout = findViewById<CoordinatorLayout>(R.id.main_coordinator_layout)
            .findViewById(R.id.appbar)

        toolbar = appBarLayout.findViewById(R.id.switch_toolbar)

        mainLabel = toolbar
            .findViewById<TextView>(R.id.main_label)
            .apply {
                typeface = (application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        selectButton = toolbar.findViewById(R.id.select_button)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)

        val mainCoordinatorLayout = drawerLayout
            .findViewById<CoordinatorLayout>(R.id.main_coordinator_layout)

        playingPart = mainCoordinatorLayout
            .findViewById<ConstraintLayout>(R.id.playing)
            .apply { isVisible = false }

        playingToolbar = playingPart.findViewById(R.id.playing_toolbar)
        val playingLayout = playingToolbar.findViewById<ConstraintLayout>(R.id.playing_layout)

        albumImageSmall = playingLayout.findViewById(R.id.playing_album_image)
        trackTitleSmall = playingLayout
            .findViewById<TextView>(R.id.playing_track_title)
            .apply {
                typeface = (application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        trackArtists = playingLayout
            .findViewById<TextView>(R.id.playing_track_artists)
            .apply {
                typeface = (application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        playButtonSmall = playingLayout.findViewById(R.id.playing_play_button)
        prevTrackButtonSmall = playingLayout.findViewById(R.id.playing_prev_track)
        nextTrackButtonSmall = playingLayout.findViewById(R.id.playing_next_track)

        trackLayout = playingPart.findViewById(R.id.track_layout)
        val primaryButtons = trackLayout.findViewById<ConstraintLayout>(R.id.primary_buttons)
        val secondaryButtons = trackLayout.findViewById<ConstraintLayout>(R.id.secondary_buttons)

        settingsButton = trackLayout.findViewById(R.id.track_settings_button)
        albumImage = trackLayout.findViewById(R.id.album_picture)
        trackPlayingBar = trackLayout.findViewById(R.id.track_playing_bar)
        curTime = trackLayout
            .findViewById<TextView>(R.id.current_time)
            .apply {
                typeface = (application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        trackLength = trackLayout
            .findViewById<TextView>(R.id.track_length)
            .apply {
                typeface = (application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        trackTitle = trackLayout
            .findViewById<TextView>(R.id.track_title_big)
            .apply {
                typeface = (application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        artistsAlbum = trackLayout
            .findViewById<TextView>(R.id.artists_album)
            .apply {
                typeface = (application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }

        playButton = primaryButtons.findViewById(R.id.play_button)
        prevTrackButton = primaryButtons.findViewById(R.id.previous_track_button)
        nextTrackButton = primaryButtons.findViewById(R.id.next_track_button)
        likeButton = secondaryButtons.findViewById(R.id.like_button)
        repeatButton = secondaryButtons.findViewById(R.id.repeat_button)
        playlistButton = secondaryButtons.findViewById(R.id.playlist_button)
        trackLyricsButton = secondaryButtons.findViewById(R.id.track_lyrics)
        equalizerButton = secondaryButtons.findViewById(R.id.equalizer_button)
        returnButton = trackLayout.findViewById(R.id.return_button)
        audioVisualizer = trackLayout.findViewById(R.id.visualizer)

        setRoundingOfPlaylistImage()
        curTime.text = calcTrackTime(curTimeData ?: 0).asTimeString()

        (application as MainApplication).run {
            mainActivity = this@MainActivity
            mainActivityViewModel.viewModelScope.launch { loadAsync().await() }
        }

        Glide.with(this).run {
            load(ViewSetter.getLikeButtonImage(
                run {
                    try {
                        // onResume

                        when (curTrack) {
                            None -> false

                            else -> runBlocking {
                                favouriteRepository.getTrackAsync(curTrack.unwrap().path).await()
                            } != null
                        }
                    } catch (e: Exception) {
                        // onCreate for first time
                        false
                    }
                }
            )).into(likeButton)
        }

        playingToolbar.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        prevTrackButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                playPrevAndUpdUI()
        }

        nextTrackButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                playNextAndUpdUI()
        }

        albumImageSmall.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        trackTitleSmall.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        trackArtists.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        nextTrackButton.setOnClickListener {
            playNextAndUpdUI()
        }

        prevTrackButton.setOnClickListener {
            playPrevAndUpdUI()
        }

        likeButton.setOnClickListener {
            trackLikeAction(curTrack.unwrap())
        }

        repeatButton.setOnClickListener {
            val looping = isLooping ?: StorageUtil(applicationContext).loadLooping()
            setLooping(!looping)
            setRepeatButtonImage(!looping)
        }

        playlistButton.setOnClickListener {
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
                        mainLabel.text.toString(),
                        resources.getString(R.string.current_playlist),
                        CurPlaylistTrackListFragment::class
                    )
                )
                .addToBackStack(null)
                .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
                .commit()
        }

        trackLyricsButton.setOnClickListener {
            when (val key = StorageUtil(applicationContext).loadHappiApiKey()) {
                null -> {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.get_happi_api)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            GetHappiApiKeyDialog {
                                StorageUtil(applicationContext).storeHappiApiKey(it)
                                showSelectLyricsFragment(it)
                            }.show(supportFragmentManager, null)

                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://happi.dev/")
                                )
                            )
                        }
                        .setNegativeButton(R.string.cancel) { _, _ -> }
                        .show()
                }
                else -> showSelectLyricsFragment(key)
            }
        }

        returnButton.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        settingsButton.setOnClickListener {
            trackSettingsButtonAction(it, curTrack.unwrap(), BottomSheetBehavior.STATE_EXPANDED)
        }

        playButton.setOnClickListener {
            handlePlayEvent()
            setPlayButtonImage(isPlaying ?: true)
        }

        playButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                handlePlayEvent()
            setPlayButtonSmallImage(isPlaying ?: true)
        }

        equalizerButton.setOnClickListener {
            when {
                resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                        (resources.configuration.screenLayout and
                                Configuration.SCREENLAYOUT_SIZE_MASK !=
                                Configuration.SCREENLAYOUT_SIZE_LARGE ||
                                resources.configuration.screenLayout and
                                Configuration.SCREENLAYOUT_SIZE_MASK !=
                                Configuration.SCREENLAYOUT_SIZE_XLARGE) ->
                    Toast.makeText(applicationContext, R.string.not_land, Toast.LENGTH_LONG).show()

                isPlaying == null -> Toast.makeText(
                    applicationContext,
                    R.string.first_play,
                    Toast.LENGTH_LONG
                ).show()

                else -> {
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in,
                            R.anim.slide_out,
                            R.anim.slide_in,
                            R.anim.slide_out
                        )
                        .replace(
                            R.id.fragment_container,
                            EqualizerFragment.Builder(mainLabel.text.toString())
                                .setAudioSessionId((application as MainApplication).audioSessionId)
                                .build()
                        )
                        .addToBackStack(null)
                        .commit()

                    if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                        sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }

        selectButton.setOnClickListener { view ->
            if (selectButton.isVisible)
                PopupMenu(this, view).apply {
                    menuInflater.inflate(R.menu.album_or_playlist, menu)
                    setOnMenuItemClickListener {
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
                                    mainLabel.text.toString(),
                                    resources.getString(
                                        when (it.itemId) {
                                            R.id.select_albums -> R.string.albums
                                            else -> R.string.playlists
                                        }
                                    ),
                                    PlaylistListFragment::class
                                )
                            )
                            .addToBackStack(null)
                            .commit()

                        true
                    }

                    show()
                }
        }

        trackPlayingBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    draggingSeekBar = true
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    curTime.text = calcTrackTime(progress).asTimeString()

                    if (ceil(progress / 1000.0).toInt() == 0 && isPlaying == false)
                        trackPlayingBar.progress = 0
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) =
                    (application as MainApplication).run {
                        draggingSeekBar = false

                        if (isPlaying == true)
                            pausePlaying()

                        resumePlaying(seekBar!!.progress)

                        playingCoroutine = Some(
                            mainActivityViewModel.viewModelScope.launch {
                                runCalculationOfSeekBarPos()
                            }
                        )
                    }
            }
        )

        setPlayButtonImage(isPlaying ?: false)
        setPlayButtonSmallImage(isPlaying ?: false)

        sheetBehavior = BottomSheetBehavior.from(playingPart)

        if (mainActivityViewModel.trackSelectedLiveData.value!! ||
            mainActivityViewModel.progressLiveData.value!! != -1
        ) {
            when (mainActivityViewModel.sheetBehaviorPositionLiveData.value!!) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    returnButton.alpha = 1F
                    settingsButton.alpha = 1F
                    albumImage.alpha = 1F
                    appBarLayout.alpha = 0F
                    playingToolbar.alpha = 0F
                    trackTitleSmall.isSelected = true
                    trackArtists.isSelected = true
                    toolbar.isVisible = false
                }

                else -> {
                    returnButton.alpha = 0F
                    settingsButton.alpha = 0F
                    albumImage.alpha = 0F
                    appBarLayout.alpha = 1F
                    playingToolbar.alpha = 1F
                    trackTitleSmall.isSelected = true
                    trackArtists.isSelected = true
                    toolbar.isVisible = true
                }
            }

            if (curPath != NO_PATH)
                mainActivityViewModel.trackSelectedLiveData.value = true

            curTrack.takeIf { it != None }
                ?.let {
                    (application as MainApplication).startPath =
                        if (curPath == NO_PATH) None else Some(curPath)

                    try {
                        onTrackSelected(
                            it.unwrap(),
                            (application as MainApplication).allTracks,
                            needToPlay = false
                        ) // Only for playing panel
                    } catch (ignored: Exception) {
                        // permissions not given
                    }
                }
        }

        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) = when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        returnButton.alpha = 1.0F
                        settingsButton.alpha = 1.0F
                        albumImage.alpha = 1.0F
                        appBarLayout.alpha = 0.0F
                        playingToolbar.alpha = 0.0F
                        trackTitleSmall.isSelected = true
                        trackArtists.isSelected = true
                        toolbar.isVisible = false
                    }

                    else -> Unit
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (!toolbar.isVisible)
                        toolbar.isVisible = true

                    val p = isPlaying ?: false
                    setPlayButtonSmallImage(p)
                    setPlayButtonImage(p)

                    returnButton.alpha = slideOffset
                    settingsButton.alpha = slideOffset
                    albumImage.alpha = slideOffset
                    appBarLayout.alpha = 1 - slideOffset
                    playingToolbar.alpha = 1 - slideOffset
                    trackTitleSmall.isSelected = true
                    trackArtists.isSelected = true
                }
            }
        )

        fragmentContainer = mainCoordinatorLayout.findViewById(R.id.fragment_container)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        drawerLayout.findViewById<NavigationView>(R.id.nav_view).apply {
            setNavigationItemSelectedListener(this@MainActivity)

            itemIconTintList = ViewSetter.colorStateList
            setBackgroundColor(ViewSetter.getBackgroundColor(this@MainActivity))
            itemTextColor = ColorStateList.valueOf(ViewSetter.textColor)

            menu.apply {
                get(0).setIcon(R.drawable.tracks)
                get(1).setIcon(R.drawable.playlist)
                get(2).setIcon(R.drawable.human)
                get(3).setIcon(R.drawable.favourite_track)
                get(4).setIcon(R.drawable.favourite_artist)
                get(5).setIcon(R.drawable.settings)
                get(6).setIcon(R.drawable.about_app)
            }
        }

        currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        (application as MainApplication).apply {
            mainActivity = this@MainActivity
            highlightedRows.clear()
            curPath.takeIf { it != NO_PATH }?.let { highlightedRows.add(curPath) }
        }

        if (currentFragment == null)
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    AbstractFragment.defaultInstance(
                        mainLabel.text.toString(),
                        resources.getString(R.string.tracks),
                        DefaultTrackListFragment::class
                    )
                )
                .commit()

        if (curPath != NO_PATH) {
            setPlayButtonSmallImage(isPlaying ?: false)

            if (mainActivityViewModel.sheetBehaviorPositionLiveData.value!! ==
                BottomSheetBehavior.STATE_EXPANDED
            ) sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val tv = TypedValue()

        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarSize = TypedValue
                .complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }

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
        outState.putInt(PROGRESS_KEY, mainActivityViewModel.progressLiveData.value!!)
        outState.putBoolean(TRACK_SELECTED_KEY, mainActivityViewModel.trackSelectedLiveData.value!!)

        (application as MainApplication).save()
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()

        (application as MainApplication).run {
            save()
            mainActivity = null
        }

        currentFragment = null
    }

    override fun onResume() {
        super.onResume()
        (application as MainApplication).mainActivity = this

        try {
            customize(updImage = false, defaultPlaying = false)
        } catch (ignored: Exception) {
            // permissions not given
        }

        if (isPlaying == true)
            playingCoroutine = Some(
                mainActivityViewModel.viewModelScope.launch {
                    runCalculationOfSeekBarPos()
                }
            )

        initAudioVisualizer()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_tracks,
            R.id.nav_playlists,
            R.id.nav_artists,
            R.id.nav_favourite_tracks,
            R.id.nav_favourite_artists,
            R.id.nav_settings,
            R.id.nav_about_app ->
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
                        when (item.itemId) {
                            R.id.nav_tracks -> AbstractFragment.defaultInstance(
                                mainLabel.text.toString(),
                                resources.getString(R.string.tracks),
                                DefaultTrackListFragment::class
                            )

                            R.id.nav_playlists -> AbstractFragment.defaultInstance(
                                mainLabel.text.toString(),
                                resources.getString(R.string.albums),
                                PlaylistListFragment::class
                            )

                            R.id.nav_artists -> AbstractFragment.defaultInstance(
                                mainLabel.text.toString(),
                                resources.getString(R.string.artists),
                                DefaultArtistListFragment::class
                            )

                            R.id.nav_favourite_tracks -> AbstractFragment.defaultInstance(
                                mainLabel.text.toString(),
                                resources.getString(R.string.favourite_tracks),
                                FavouriteTrackListFragment::class
                            )

                            R.id.nav_favourite_artists -> AbstractFragment.defaultInstance(
                                mainLabel.text.toString(),
                                resources.getString(R.string.favourite_artists),
                                FavouriteArtistListFragment::class
                            )

                            R.id.nav_settings -> AbstractFragment.defaultInstance(
                                mainLabel.text.toString(),
                                resources.getString(R.string.settings),
                                SettingsFragment::class
                            )

                            R.id.nav_about_app -> AbstractFragment.defaultInstance(
                                mainLabel.text.toString(),
                                resources.getString(R.string.about_app),
                                AboutAppFragment::class
                            )

                            else -> throw IllegalStateException("Not yet implemented")
                        }
                    )
                    .addToBackStack(null)
                    .apply {
                        if (isPlaying == true)
                            playingPart.isVisible = true
                    }
                    .commit()

            else -> Toast.makeText(
                this,
                resources.getString(R.string.coming_soon),
                Toast.LENGTH_LONG
            ).show()
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        when (sheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED ->
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            else -> {
                when {
                    drawerLayout.isDrawerOpen(GravityCompat.START) ->
                        drawerLayout.closeDrawer(GravityCompat.START)
                    else -> try {
                        super.onBackPressed()
                    } catch (ignored: Exception) {
                        // Equalizer error
                    }
                }
            }
        }
    }

    override fun onTrackSelected(
        track: Track,
        tracks: Collection<Track>,
        needToPlay: Boolean
    ) {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            if (needToPlay) {
                currentFragment = supportFragmentManager.fragments.last()

                (application as MainApplication).curPlaylist.apply {
                    clear()
                    addAll(tracks)
                }
            }

            (application as MainApplication).playingBarIsVisible = true
            (currentFragment as AbstractTrackListFragment?)?.up()
            mainActivityViewModel.trackSelectedLiveData.value = true

            val newTrack = curPath != track.path
            (application as MainApplication).curPath = track.path

            val p = when {
                (application as MainApplication).serviceBound -> isPlaying ?: true
                else -> isPlaying ?: false
            }

            val looping = isLooping ?: StorageUtil(applicationContext).loadLooping()

            updateUI(track to false)
            setPlayButtonSmallImage(p)
            setPlayButtonImage(p)
            setRepeatButtonImage(looping)

            if (needToPlay) {
                returnButton.alpha = 0.0F
                settingsButton.alpha = 0.0F
                albumImage.alpha = 0.0F
            }

            trackTitleSmall.isSelected = true
            trackArtists.isSelected = true

            trackPlayingBar.max = track.duration.toInt()
            trackPlayingBar.progress = curTimeData ?: mainActivityViewModel.progressLiveData.value!!

            if (!playingPart.isVisible)
                playingPart.isVisible = true

            when {
                needToPlay -> when {
                    p -> when {
                        newTrack -> {
                            playAudio(track.path)
                            playingCoroutine = Some(
                                mainActivityViewModel.viewModelScope.launch {
                                    runCalculationOfSeekBarPos()
                                }
                            )
                        }

                        else -> pausePlaying()
                    }

                    else -> when {
                        newTrack -> playAudio(track.path)
                        else -> resumePlaying()
                    }
                }

                else -> if (isPlaying == true)
                    playingCoroutine = Some(
                        mainActivityViewModel.viewModelScope.launch {
                            runCalculationOfSeekBarPos()
                        }
                    )
            }
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
                    mainLabel.text.toString(),
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
                when (val mainLab = mainLabel.text.toString()) {
                    resources.getString(R.string.albums) -> AbstractFragment.defaultInstance(
                        mainLab,
                        title,
                        AlbumTrackListFragment::class
                    )

                    else -> CustomPlaylistTrackListFragment.newInstance(
                        mainLab,
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

        val f = (application as MainApplication).getFontFromName(Params.instance.font)
        mainLabel.typeface = f
        trackTitle.typeface = f
        artistsAlbum.typeface = f
        trackTitleSmall.typeface = f
        trackArtists.typeface = f
        curTime.typeface = f
        trackLength.typeface = f
    }

    override fun onTrackSelected(track: FoundTrack): Unit = HappiFetcher()
        .fetchLyrics(track, StorageUtil(applicationContext).loadHappiApiKey()!!)
        .observe(this) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(
                    R.id.fragment_container,
                    LyricsFragment.newInstance(
                        mainLabel.text.toString(),
                        track.title,
                        GsonBuilder()
                            .excludeFieldsWithoutExposeAnnotation()
                            .create()
                            .fromJson(it, LyricsParser::class.java)
                            .result.lyrics
                    )
                )
                .addToBackStack(null)
                .commit()

            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
                            PackageManager.PERMISSION_GRANTED ->
                        Unit // all permissions are granted

                    else -> when {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.READ_EXTERNAL_STORAGE
                        ) || ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.READ_PHONE_STATE
                        ) || ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.RECORD_AUDIO
                        ) -> AlertDialog
                            .Builder(this)
                            .setMessage("Phone state and storage permissions required for this app")
                            .setPositiveButton("OK") { _, which ->
                                if (which == DialogInterface.BUTTON_POSITIVE)
                                    (application as MainApplication)
                                        .checkAndRequestPermissions()
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
        }
    }

    /**
     * @param src first - current track
     * @param src second - resume status after activity onPause
     */

    override fun updateUI(src: Pair<Track, Boolean>) {
        playingPart.setBackgroundColor(ViewSetter.getBackgroundColor(this))

        setRepeatButtonImage(
            when {
                src.second -> StorageUtil(applicationContext).loadLooping()
                else -> isLooping ?: StorageUtil(applicationContext).loadLooping()
            }
        )

        setLikeButtonImage(
            runBlocking {
                favouriteRepository.getTrackAsync(src.first.path).await()
            } != null
        )

        val track = (application as MainApplication).changedTracks[src.first.path] ?: src.first

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

        trackTitleSmall.text = track.title.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_track)
                else -> it
            }
        }

        trackArtists.text = track.artist.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_artist)
                else -> it
            }
        }

        trackTitle.text = track.title.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_track)
                else -> it
            }
        }

        artistsAlbum.text = artistAlbum

        trackTitleSmall.isSelected = true
        trackArtists.isSelected = true
        trackTitle.isSelected = true
        artistsAlbum.isSelected = true

        trackLength.text = calcTrackTime(track.duration.toInt()).asTimeString()

        mainActivityViewModel.viewModelScope.launch(Dispatchers.Main) {
            val app = application as MainApplication
            val task =
                app.getAlbumPictureAsync(track.path, Params.instance.showPlaylistsImages).await()

            Glide.with(this@MainActivity)
                .load(task)
                .transition(DrawableTransitionOptions.withCrossFade())
                .override(albumImage.width, albumImage.height)
                .into(albumImage)

            albumImageSmall.setImageBitmap(task)
        }
    }

    /**
     * Sets play or pause image for small button
     * @param isPlaying is music playing now
     */

    internal fun setPlayButtonSmallImage(isPlaying: Boolean) = playButtonSmall.run {
        setImageResource(ViewSetter.getPlayButtonSmallImage(isPlaying))
        setTint(ViewSetter.textColor)
    }

    /**
     * Sets play or pause image for big button
     * @param isPlaying is music playing now
     */

    internal fun setPlayButtonImage(isPlaying: Boolean) = playButton.run {
        setImageResource(ViewSetter.getPlayButtonImage(isPlaying))
        setTint(Params.instance.theme.rgb)
    }

    /**
     * Sets looping button image
     * depending on current theme and repeat status
     * @param isLooping looping status
     */

    private fun setRepeatButtonImage(isLooping: Boolean) = repeatButton.run {
        setImageResource(ViewSetter.getRepeatButtonImage(isLooping))
        setTint(Params.instance.theme.rgb)
    }

    /**
     * Sets like button image
     * depending on current theme and like status
     * @param isLiked like status
     */

    private fun setLikeButtonImage(isLiked: Boolean) = likeButton.run {
        setImageResource(ViewSetter.getLikeButtonImage(isLiked))
        setTint(Params.instance.theme.rgb)
    }

    /**
     * Plays next track and updates UI for it
     */

    internal fun playNextAndUpdUI() = (application as MainApplication).run {
        mainActivityViewModel.progressLiveData.value = 0

        val curIndex = (curInd + 1).let { if (it == curPlaylist.size) 0 else it }
        curPath = curPlaylist[curIndex].path

        val looping = isLooping ?: StorageUtil(applicationContext).loadLooping()
        playAudio(curPath)
        setRepeatButtonImage(looping)
    }

    /**
     * Plays previous track and updates UI for it
     */

    private fun playPrevAndUpdUI() = (application as MainApplication).run {
        mainActivityViewModel.progressLiveData.value = 0
        trackPlayingBar.progress = 0

        val curIndex = (curInd - 1).let { if (it < 0) curPlaylist.size - 1 else it }
        curPath = curPlaylist[curIndex].path

        val looping = isLooping ?: StorageUtil(applicationContext).loadLooping()
        playAudio(curPath)
        setRepeatButtonImage(looping)
        curTime.setText(R.string.current_time)
    }

    /**
     * Calculates current position for playing seek bar
     */

    internal suspend fun runCalculationOfSeekBarPos() = coroutineScope {
        launch(Dispatchers.Default) {
            val load = StorageUtil(applicationContext).loadTrackPauseTime()
            var currentPosition = curTimeData ?: load
            val total = curTrack.unwrap().duration.toInt()
            trackPlayingBar.max = total

            while (isPlaying == true && currentPosition <= total && !draggingSeekBar) {
                currentPosition = curTimeData ?: load
                trackPlayingBar.progress = currentPosition
                delay(50)
            }
        }
    }

    /**
     * Plays track with given path
     * @param path path to track (DATA column from MediaStore)
     */

    internal fun playAudio(path: String) {
        (application as MainApplication).curPath = path
        StorageUtil(applicationContext).storeTrackPath(path)

        when {
            !(application as MainApplication).serviceBound -> {
                val playerIntent = Intent(this, AudioPlayerService::class.java)

                when {
                    SDK_INT >= Build.VERSION_CODES.O ->
                        startForegroundService(playerIntent)
                    else -> startService(playerIntent)
                }

                bindService(
                    playerIntent,
                    (application as MainApplication).serviceConnection,
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

    internal fun resumePlaying(resumePos: Int = -1) = when {
        !(application as MainApplication).serviceBound -> {
            StorageUtil(applicationContext).apply {
                storeTracks((application as MainApplication).allTracks.toList())
                storeTrackPath(curPath)
            }

            val playerIntent = Intent(this, AudioPlayerService::class.java)
                .putExtra(RESUME_POSITION_ARG, resumePos)

            when {
                SDK_INT >= Build.VERSION_CODES.O ->
                    startForegroundService(playerIntent)
                else -> startService(playerIntent)
            }
            bindService(
                playerIntent,
                (application as MainApplication).serviceConnection,
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

    internal fun pausePlaying() = when {
        (application as MainApplication).serviceBound -> sendBroadcast(Intent(Broadcast_PAUSE))

        else -> {
            StorageUtil(applicationContext).apply {
                storeTracks((application as MainApplication).allTracks.toList())
                storeTrackPath(curPath)
                storeTrackPauseTime(curTimeData ?: -1)
            }

            val playerIntent = Intent(this, AudioPlayerService::class.java)
                .setAction(PAUSED_PRESSED_ARG)

            when {
                SDK_INT >= Build.VERSION_CODES.O ->
                    startForegroundService(playerIntent)
                else -> startService(playerIntent)
            }

            bindService(
                playerIntent,
                (application as MainApplication).serviceConnection,
                BIND_AUTO_CREATE
            )

            Unit
        }
    }

    /**
     * Sets looping status for [AudioPlayerService]
     * @param isLooping is looping button pressed
     */

    private fun setLooping(isLooping: Boolean) = when {
        (application as MainApplication).serviceBound -> sendBroadcast(
            Intent(Broadcast_LOOPING)
                .putExtra(IS_LOOPING_ARG, isLooping)
        )

        else -> {
            StorageUtil(applicationContext).apply {
                storeTracks((application as MainApplication).allTracks.toList())
                storeTrackPath(curPath)
                storeTrackPauseTime(curTimeData ?: -1)
            }

            val playerIntent = Intent(this, AudioPlayerService::class.java)
                .setAction(LOOPING_PRESSED_ARG)

            when {
                SDK_INT >= Build.VERSION_CODES.O ->
                    startForegroundService(playerIntent)
                else -> startService(playerIntent)
            }

            bindService(
                playerIntent,
                (application as MainApplication).serviceConnection,
                BIND_AUTO_CREATE
            )

            Unit
        }
    }

    /**
     * Shows popup menu about track
     * @param view settings button view
     * @param track [Track] to modify
     * @param bottomSheetBehaviorState state in which function executes
     */

    internal fun trackSettingsButtonAction(
        view: View,
        track: Track,
        bottomSheetBehaviorState: Int
    ) {
        if (sheetBehavior.state == bottomSheetBehaviorState)
            PopupMenu(this, view).apply {
                menuInflater.inflate(R.menu.menu_track_settings, menu)

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.nav_change_track_info -> changeTrackInfo(track)
                        R.id.nav_add_to_queue -> addTrackToQueue(track)
                        R.id.nav_remove_from_queue -> removeTrackFromQueue(track)
                        R.id.nav_add_track_to_favourites -> trackLikeAction(track)
                        R.id.nav_add_to_playlist -> addToPlaylistAsync(track)
                        R.id.nav_remove_track -> removeTrack(track)
                    }

                    return@setOnMenuItemClickListener true
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
                        favouriteRepository.getArtistAsync(artist.name).await()
                    } != null

                    val favouriteArtist = artist.asFavourite()

                    mainActivityViewModel.viewModelScope.launch(Dispatchers.IO) {
                        when {
                            contain -> favouriteRepository.removeArtistAsync(favouriteArtist)
                            else -> favouriteRepository.addArtistAsync(favouriteArtist)
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

    private fun trackLikeAction(track: Track) {
        val contain = runBlocking {
            favouriteRepository.getTrackAsync(track.path).await()
        } != null

        val favouriteTrack = track.asFavourite()

        mainActivityViewModel.viewModelScope.launch(Dispatchers.IO) {
            when {
                contain -> favouriteRepository.removeTrackAsync(favouriteTrack)
                else -> favouriteRepository.addTrackAsync(favouriteTrack)
            }
        }

        setLikeButtonImage(!contain)
    }

    /**
     * Runs [TrackChangeFragment]
     * @param track [Track] to change
     */

    private fun changeTrackInfo(track: Track) {
        val runFragment = {
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
                        mainLabel.text.toString(),
                        resources.getString(R.string.change_track_s_information),
                        track
                    )
                )
                .addToBackStack(null)
                .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
                .commit()
        }

        when (SDK_INT) {
            Build.VERSION_CODES.Q -> {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    track.androidId
                )

                try {
                    contentResolver.openFileDescriptor(uri, "w")
                        ?.use { runFragment() }
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
                            ?.let {
                                startIntentSenderForResult(
                                    it, 125,
                                    null, 0, 0, 0, null
                                )

                                File(track.path).delete()
                            }
                    }
                }
            }

            else -> runFragment()
        }
    }

    /**
     * Adds track to queue
     * @param track [Track] to add
     */

    private fun addTrackToQueue(track: Track) =
        (application as MainApplication).curPlaylist.add(track)

    /**
     * Removes track from queue
     * @param track [Track] to remove
     */

    private fun removeTrackFromQueue(track: Track) = (application as MainApplication).run {
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

                curPath.takeIf { it != NO_PATH && it != removedPath }?.let(::playAudio)
                    ?: resumePlaying()
            }

            else -> curPlaylist.remove(track)
        }

        (currentFragment as AbstractTrackListFragment).updateUIOnChangeTracks()
    }

    /**
     * Adds track to playlist asynchronously
     * @param track [Track] to add
     */

    private fun addToPlaylistAsync(track: Track) =
        mainActivityViewModel.viewModelScope.launch(Dispatchers.IO) {
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
                        mainLabel.text.toString(),
                        resources.getString(R.string.playlists),
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
     * @param track [Track] to remove
     */

    private fun removeTrack(track: Track) = AreYouSureDialog(
        R.string.remove_track_message
    ) {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            track.androidId
        )

        when {
            SDK_INT >= Build.VERSION_CODES.R -> try {
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
                            ?.let {
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

        (currentFragment as AbstractTrackListFragment).updateUIOnChangeTracks()

    }.show(supportFragmentManager, null)

    /**
     * Update UI on service notification clicks
     * @param updImage does track image need update
     * @param defaultPlaying needs default playing
     */

    internal fun customize(updImage: Boolean, defaultPlaying: Boolean = true) {
        val p = isPlaying ?: defaultPlaying
        setPlayButtonImage(p)
        setPlayButtonSmallImage(p)
        if (updImage) curTrack.takeIf { it != None }?.unwrap()?.let { updateUI(it to true) }
    }

    /**
     * Pauses or resumes playing
     */

    private fun handlePlayEvent() = when (isPlaying) {
        true -> {
            pausePlaying()
            mainActivityViewModel.progressLiveData.value = curTimeData
        }

        else -> {
            resumePlaying()
            playingCoroutine = Some(
                mainActivityViewModel.viewModelScope.launch {
                    runCalculationOfSeekBarPos()
                }
            )
        }
    }

    /**
     * Reinitializes playing coroutine to show time
     */

    internal fun reinitializePlayingCoroutine() {
        playingCoroutine = Some(
            mainActivityViewModel.viewModelScope.launch {
                runCalculationOfSeekBarPos()
            }
        )
    }

    /**
     * Sets rounding of playlists images
     * for different configurations of devices
     */

    internal fun setRoundingOfPlaylistImage() = albumImage.setCornerRadius(
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

    internal fun setShowingPlaylistImage() =
        mainActivityViewModel.viewModelScope.launch(Dispatchers.Main) {
            Glide.with(this@MainActivity).load(
                (application as MainApplication).getAlbumPictureAsync(
                    curTrack.unwrap().path,
                    Params.instance.showPlaylistsImages
                ).await().also(albumImageSmall::setImageBitmap)
            ).into(albumImage)
        }

    /**
     * Initialises audio visualizer
     */

    internal fun initAudioVisualizer() = audioVisualizer.run {
        if (Params.instance.showVisualizer) {
            setColor(Params.instance.theme.rgb)
            setDensity(
                when (resources.configuration.orientation) {
                    Configuration.ORIENTATION_PORTRAIT ->
                        when (resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK)) {
                            Configuration.SCREENLAYOUT_SIZE_NORMAL -> 50
                            Configuration.SCREENLAYOUT_SIZE_LARGE -> 75
                            else -> 50
                        }

                    else -> when (resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK)) {
                        Configuration.SCREENLAYOUT_SIZE_NORMAL -> 100
                        Configuration.SCREENLAYOUT_SIZE_LARGE -> 150
                        else -> 100
                    }
                }.toFloat()
            )

            try {
                setPlayer((application as MainApplication).audioSessionId)
            } catch (ignored: Exception) {
                // permission not given
            }
        }
    }

    /**
     * Shows [TrackSelectLyricsFragment]
     * @param apiKey user's api key
     */

    internal fun showSelectLyricsFragment(apiKey: String) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                TrackSelectLyricsFragment.newInstance(
                    mainLabel.text.toString(),
                    curTrack.unwrap(),
                    apiKey
                )
            )
            .addToBackStack(null)
            .commit()

        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }
}