package com.dinaraparanid.prima

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
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
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.fragments.*
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.extensions.unwrap
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.utils.polymorphism.TrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.UIUpdatable
import com.dinaraparanid.prima.viewmodels.MainActivityViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread
import kotlin.math.ceil

class MainActivity :
    AppCompatActivity(),
    TrackListFragment.Callbacks,
    ArtistListFragment.Callbacks,
    PlaylistListFragment.Callbacks,
    NavigationView.OnNavigationItemSelectedListener,
    UIUpdatable<Pair<Track, Boolean>> {
    private lateinit var playingPart: ConstraintLayout
    internal lateinit var selectButton: ImageButton
    internal lateinit var mainLabel: TextView

    private lateinit var trackLayout: ConstraintLayout
    private lateinit var settingsButton: ImageButton
    private lateinit var albumImage: ImageView
    private lateinit var trackPlayingBar: SeekBar
    private lateinit var curTime: TextView
    private lateinit var trackLength: TextView
    private lateinit var trackTitle: TextView
    private lateinit var artistsAlbum: TextView
    private lateinit var playButton: ImageButton
    private lateinit var prevTrackButton: ImageButton
    private lateinit var nextTrackButton: ImageButton
    private lateinit var likeButton: ImageButton
    private lateinit var repeatButton: ImageButton
    private lateinit var playlistButton: ImageButton
    private lateinit var trackLyricsButton: ImageButton
    private lateinit var returnButton: ImageButton

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var playingToolbar: Toolbar
    private lateinit var albumImageSmall: CircleImageView
    private lateinit var trackTitleSmall: TextView
    private lateinit var trackArtists: TextView
    private lateinit var playButtonSmall: ImageButton
    private lateinit var prevTrackButtonSmall: ImageButton
    private lateinit var nextTrackButtonSmall: ImageButton

    private val mainActivityViewModel: MainActivityViewModel by lazy {
        ViewModelProvider(this)[MainActivityViewModel::class.java]
    }

    internal var currentFragment: Fragment? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var fragmentContainer: FrameLayout
    internal lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var favouriteRepository: FavouriteRepository

    private var playingThread: Option<Thread> = None
    private var draggingSeekBar = false
    private var progr = 0
    private var actionBarSize = 0
    private var timeSave = 0

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
        private const val FIRST_HIGHLIGHTED_KEY = "first_highlighted"
        private const val NO_PATH = "_____ЫЫЫЫЫЫЫЫ_____"

        @JvmStatic
        internal fun calcTrackTime(millis: Int): Triple<Int, Int, Int> {
            var cpy = millis

            val h = cpy / 3600000
            cpy -= h * 3600000

            val m = cpy / 60000
            cpy -= m * 60000

            val s = cpy / 1000

            return Triple(h, m, s)
        }

        @JvmStatic
        internal fun Triple<Int, Int, Int>.asStr() =
            "${first.let { if (it < 10) "0$it" else it }}:" +
                    "${second.let { if (it < 10) "0$it" else it }}:" +
                    "${third.let { if (it < 10) "0$it" else it }}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        favouriteRepository = FavouriteRepository.instance

        mainActivityViewModel.run {
            load(
                savedInstanceState?.getInt(SHEET_BEHAVIOR_STATE_KEY),
                savedInstanceState?.getInt(PROGRESS_KEY),
                savedInstanceState?.getBoolean(TRACK_SELECTED_KEY),
                savedInstanceState?.getBoolean(FIRST_HIGHLIGHTED_KEY)
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

        val toolbar = appBarLayout.findViewById<Toolbar>(R.id.switch_toolbar)
        mainLabel = toolbar.findViewById(R.id.main_label)
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
        trackTitleSmall = playingLayout.findViewById(R.id.playing_track_title)
        trackArtists = playingLayout.findViewById(R.id.playing_track_artists)
        playButtonSmall = playingLayout.findViewById(R.id.playing_play_button)
        prevTrackButtonSmall = playingLayout.findViewById(R.id.playing_prev_track)
        nextTrackButtonSmall = playingLayout.findViewById(R.id.playing_next_track)

        trackLayout = playingPart.findViewById(R.id.track_layout)
        val primaryButtons = trackLayout.findViewById<ConstraintLayout>(R.id.primary_buttons)
        val secondaryButtons = trackLayout.findViewById<ConstraintLayout>(R.id.secondary_buttons)

        settingsButton = trackLayout.findViewById(R.id.track_settings_button)
        albumImage = trackLayout.findViewById(R.id.album_picture)
        trackPlayingBar = trackLayout.findViewById(R.id.track_playing_bar)
        curTime = trackLayout.findViewById(R.id.current_time)
        trackLength = trackLayout.findViewById(R.id.track_length)
        trackTitle = trackLayout.findViewById(R.id.track_title_big)
        artistsAlbum = trackLayout.findViewById(R.id.artists_album)
        playButton = primaryButtons.findViewById(R.id.play_button)
        prevTrackButton = primaryButtons.findViewById(R.id.previous_track_button)
        nextTrackButton = primaryButtons.findViewById(R.id.next_track_button)
        likeButton = secondaryButtons.findViewById(R.id.like_button)
        repeatButton = secondaryButtons.findViewById(R.id.repeat_button)
        playlistButton = secondaryButtons.findViewById(R.id.playlist_button)
        trackLyricsButton = secondaryButtons.findViewById(R.id.track_lyrics)
        returnButton = trackLayout.findViewById(R.id.return_button)

        curTime.setTextColor(ViewSetter.textColor)
        trackLength.setTextColor(ViewSetter.textColor)
        trackTitle.setTextColor(ViewSetter.textColor)
        artistsAlbum.setTextColor(ViewSetter.textColor)
        curTime.text = calcTrackTime(curTimeData ?: 0).asStr()


        (application as MainApplication).run {
            mainActivity = this@MainActivity
            thread { load() }
        }

        returnButton.setImageResource(ViewSetter.returnButtonImage)
        nextTrackButton.setImageResource(ViewSetter.nextTrackButtonImage)
        prevTrackButton.setImageResource(ViewSetter.prevTrackButtonImage)
        playlistButton.setImageResource(ViewSetter.playlistButtonImage)
        trackLyricsButton.setImageResource(ViewSetter.lyricsButtonImage)
        settingsButton.setImageResource(ViewSetter.settingsButtonImage)

        likeButton.setImageResource(
            ViewSetter.getLikeButtonImage(
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
            )
        )

        playingToolbar.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        prevTrackButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                playPrev()
        }

        nextTrackButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                playNext()
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
            playNext()
        }

        prevTrackButton.setOnClickListener {
            playPrev()
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
                    DefaultTrackListFragment.newInstance(
                        mainLabel.text.toString(),
                        resources.getString(R.string.current_playlist),
                    ).apply {
                        genFunc = { (application as MainApplication).curPlaylist }
                    }
                )
                .addToBackStack(null)
                .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
                .commit()
        }

        trackLyricsButton.setOnClickListener {
            Toast.makeText(
                this,
                resources.getString(R.string.coming_soon),
                Toast.LENGTH_LONG
            ).show()
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
                                PlaylistListFragment.newInstance(
                                    mainLabel.text.toString(),
                                    resources.getString(
                                        when (it.itemId) {
                                            R.id.select_albums -> R.string.albums
                                            else -> R.string.playlists
                                        }
                                    )
                                )
                            )
                            .addToBackStack(null)
                            .commit()

                        return@setOnMenuItemClickListener true
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
                    curTime.text = calcTrackTime(progress).asStr()

                    if (ceil(progress / 1000.0).toInt() == 0 && isPlaying == false)
                        trackPlayingBar.progress = 0
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) =
                    (application as MainApplication).run {
                        draggingSeekBar = false

                        if (isPlaying == true)
                            pausePlaying()

                        resumePlaying(seekBar!!.progress)
                        playingThread = Some(thread { run() })
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
                    returnButton.alpha = 1.0F
                    settingsButton.alpha = 1.0F
                    albumImage.alpha = 1.0F
                    appBarLayout.alpha = 0.0F
                    playingToolbar.alpha = 0.0F
                    trackTitleSmall.isSelected = true
                    trackArtists.isSelected = true
                    toolbar.isVisible = false
                }

                else -> {
                    returnButton.alpha = 0.0F
                    settingsButton.alpha = 0.0F
                    albumImage.alpha = 0.0F
                    appBarLayout.alpha = 1.0F
                    playingToolbar.alpha = 1.0F
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
                            0,
                            needToPlay = false
                        ) // Only for playing panel
                    } catch (e: Exception) {
                        // permissions not gived
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

            itemIconTintList = null
            setBackgroundColor(ViewSetter.backgroundColor)
            itemTextColor = ColorStateList.valueOf(ViewSetter.textColor)

            menu.apply {
                get(0).setIcon(ViewSetter.tracksMenuImage)
                get(1).setIcon(ViewSetter.playlistMenuImage)
                get(2).setIcon(ViewSetter.artistMenuImage)
                get(3).setIcon(ViewSetter.favouriteTrackMenuImage)
                get(4).setIcon(ViewSetter.favouriteArtistMenuImage)
                get(5).setIcon(ViewSetter.recommendationsMenuImage)
                get(6).setIcon(ViewSetter.compilationMenuImage)
                get(7).setIcon(ViewSetter.settingsMenuImage)
                get(8).setIcon(ViewSetter.aboutAppMenuImage)
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
                    DefaultTrackListFragment.newInstance(
                        mainLabel.text.toString(),
                        resources.getString(R.string.tracks),
                        _firstToHighlight = curPath.takeIf { it != NO_PATH }
                    ).apply {
                        mainActivityViewModel.firstHighlightedLiveData.value = true
                        currentFragment = this
                    }
                )
                .commit()

        if (curPath != NO_PATH) {
            setPlayButtonSmallImage(isPlaying ?: false)

            if (mainActivityViewModel.sheetBehaviorPositionLiveData.value!! == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val tv = TypedValue()

        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarSize = TypedValue
                .complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SHEET_BEHAVIOR_STATE_KEY, sheetBehavior.state)
        outState.putInt(PROGRESS_KEY, mainActivityViewModel.progressLiveData.value!!)
        outState.putBoolean(TRACK_SELECTED_KEY, mainActivityViewModel.trackSelectedLiveData.value!!)
        outState.putBoolean(
            FIRST_HIGHLIGHTED_KEY,
            mainActivityViewModel.firstHighlightedLiveData.value!!
        )

        (application as MainApplication).save()
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()

        try {
            customize(false)
        } catch (e: Exception) {
            // permissions not gived
        }

        if (isPlaying == true)
            playingThread = Some(thread { run() })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_tracks ||
            item.itemId == R.id.nav_playlists ||
            item.itemId == R.id.nav_artists ||
            item.itemId == R.id.nav_favourite_tracks ||
            item.itemId == R.id.nav_favourite_artists
        )
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
                        R.id.nav_tracks -> DefaultTrackListFragment.newInstance(
                            mainLabel.text.toString(),
                            resources.getString(R.string.tracks),
                        )

                        R.id.nav_playlists -> PlaylistListFragment.newInstance(
                            mainLabel.text.toString(),
                            resources.getString(R.string.albums)
                        )

                        R.id.nav_artists -> ArtistListFragment.newInstance(
                            mainLabel.text.toString(),
                            resources.getString(R.string.artists)
                        )

                        R.id.nav_favourite_tracks -> DefaultTrackListFragment.newInstance(
                            mainLabel.text.toString(),
                            resources.getString(R.string.favourite_tracks)
                        ).apply {
                            genFunc = {
                                runBlocking { favouriteRepository.tracksAsync.await().toPlaylist() }
                            }
                        }

                        R.id.nav_favourite_artists -> ArtistListFragment.newInstance(
                            mainLabel.text.toString(),
                            resources.getString(R.string.favourite_artists)
                        ).apply {
                            genFunc = {
                                runBlocking { favouriteRepository.artistsAsync.await() }
                            }
                        }

                        else -> throw IllegalStateException("Not yet implemented")
                    }
                )
                .addToBackStack(null)
                .apply {
                    if (isPlaying == true)
                        playingPart.isVisible = true
                }
                .commit()
        else Toast.makeText(
            this,
            resources.getString(R.string.coming_soon),
            Toast.LENGTH_LONG
        ).show()

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed(): Unit = when (sheetBehavior.state) {
        BottomSheetBehavior.STATE_EXPANDED ->
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        else -> {
            when {
                drawerLayout.isDrawerOpen(GravityCompat.START) ->
                    drawerLayout.closeDrawer(GravityCompat.START)
                else -> super.onBackPressed()
            }
        }
    }

    override fun onTrackSelected(
        track: Track,
        tracks: Collection<Track>,
        ind: Int,
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
            (currentFragment as TrackListFragment?)?.up()
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
                            playingThread = Some(thread { run() })
                        }

                        else -> pausePlaying()
                    }

                    else -> when {
                        newTrack -> playAudio(track.path)
                        else -> resumePlaying(-1) // continue on paused position
                    }
                }

                else -> if (isPlaying == true)
                    playingThread = Some(thread { run() })
            }
        }
    }

    override fun onArtistSelected(artist: Artist, playlistGen: () -> Playlist) {
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
                DefaultTrackListFragment.newInstance(
                    mainLabel.text.toString(),
                    artist.name
                ).apply { genFunc = playlistGen }
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()
    }

    override fun onPlaylistSelected(
        id: Long,
        title: String,
        custom: Boolean,
        playlistGen: () -> Playlist
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
                    resources.getString(R.string.albums) -> DefaultTrackListFragment.newInstance(
                        mainLab,
                        title
                    ).apply { genFunc = playlistGen }

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_ID_MULTIPLE_PERMISSIONS -> {
                val perms: MutableMap<String, Int> = HashMap()

                perms[Manifest.permission.READ_PHONE_STATE] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED

                if (grantResults.isNotEmpty()) {
                    var i = 0
                    while (i < permissions.size) {
                        perms[permissions[i]] = grantResults[i]
                        i++
                    }

                    when {
                        perms[Manifest.permission.READ_PHONE_STATE] == PackageManager.PERMISSION_GRANTED
                                && perms[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED ->
                            Unit // it's okay to load tracks

                        else -> when {
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.READ_EXTERNAL_STORAGE
                            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.READ_PHONE_STATE
                            ) -> showDialogOK(
                                "Phone state and storage permissions required for this app"
                            ) { _, which ->
                                when (which) {
                                    DialogInterface.BUTTON_POSITIVE -> (application as MainApplication)
                                        .checkAndRequestPermissions()

                                    DialogInterface.BUTTON_NEGATIVE -> Unit
                                }
                            }

                            else -> Toast.makeText(
                                this,
                                "Go to settings and enable permissions, please",
                                Toast.LENGTH_LONG
                            ).show().run { Thread.sleep(1000) }
                        }
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
        playingPart.setBackgroundColor(ViewSetter.backgroundColor)
        setRepeatButtonImage(
            when {
                src.second -> StorageUtil(applicationContext).loadLooping()
                else -> isLooping ?: StorageUtil(applicationContext).loadLooping()
            }
        )

        likeButton.setImageResource(
            ViewSetter.getLikeButtonImage(
                runBlocking {
                    favouriteRepository.getTrackAsync(src.first.path).await()
                } != null
            )
        )

        val artistAlbum =
            "${
                src.first.artist
                    .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it }
            } / ${
                src.first.playlist
                    .let {
                        if (it == "<unknown>" || it == src.first
                                .path
                                .split('/')
                                .takeLast(2)
                                .first()
                        ) resources.getString(R.string.unknown_album) else it
                    }
            }"

        trackTitleSmall.text = src.first.title.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_track)
                else -> it
            }
        }

        trackArtists.text = src.first.artist.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_artist)
                else -> it
            }
        }

        trackTitle.text = src.first.title.let {
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

        trackLength.text = calcTrackTime(src.first.duration.toInt()).asStr()

        mainActivityViewModel.viewModelScope.launch {
            launch {
                albumImage.setImageBitmap(
                    (application as MainApplication).getAlbumPicture(src.first.path)
                )
            }

            launch {
                albumImageSmall.setImageBitmap(
                    (application as MainApplication).getAlbumPicture(src.first.path)
                )
            }
        }
    }

    private fun showDialogOK(message: String, okListener: DialogInterface.OnClickListener) =
        AlertDialog
            .Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", okListener)
            .create()
            .show()

    private fun setTheme() = setTheme(ViewSetter.appTheme)

    internal fun setPlayButtonSmallImage(playing: Boolean) = playButtonSmall.setImageResource(
        when (playing) {
            true -> android.R.drawable.ic_media_pause
            else -> android.R.drawable.ic_media_play
        }
    )

    internal fun setPlayButtonImage(playing: Boolean) = playButton.setImageResource(
        when (playing) {
            true -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.pause_blue
                is Colors.BlueNight -> R.drawable.pause_blue
                is Colors.Green -> R.drawable.pause_green
                is Colors.GreenNight -> R.drawable.pause_green
                is Colors.GreenTurquoise -> R.drawable.pause_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.pause_green_turquoise
                is Colors.Lemon -> R.drawable.pause_lemon
                is Colors.LemonNight -> R.drawable.pause_lemon
                is Colors.Orange -> R.drawable.pause_orange
                is Colors.OrangeNight -> R.drawable.pause_orange
                is Colors.Pink -> R.drawable.pause_pink
                is Colors.PinkNight -> R.drawable.pause_pink
                is Colors.Purple -> R.drawable.pause_purple
                is Colors.PurpleNight -> R.drawable.pause_purple
                is Colors.Red -> R.drawable.pause_red
                is Colors.RedNight -> R.drawable.pause_red
                is Colors.Sea -> R.drawable.pause_sea
                is Colors.SeaNight -> R.drawable.pause_sea
                is Colors.Turquoise -> R.drawable.pause_turquoise
                is Colors.TurquoiseNight -> R.drawable.pause_turquoise
                else -> R.drawable.pause
            }

            else -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.play_blue
                is Colors.BlueNight -> R.drawable.play_blue
                is Colors.Green -> R.drawable.play_green
                is Colors.GreenNight -> R.drawable.play_green
                is Colors.GreenTurquoise -> R.drawable.play_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.play_green_turquoise
                is Colors.Lemon -> R.drawable.play_lemon
                is Colors.LemonNight -> R.drawable.play_lemon
                is Colors.Orange -> R.drawable.play_orange
                is Colors.OrangeNight -> R.drawable.play_orange
                is Colors.Pink -> R.drawable.play_pink
                is Colors.PinkNight -> R.drawable.play_pink
                is Colors.Purple -> R.drawable.play_purple
                is Colors.PurpleNight -> R.drawable.play_purple
                is Colors.Red -> R.drawable.play_red
                is Colors.RedNight -> R.drawable.play_red
                is Colors.Sea -> R.drawable.play_sea
                is Colors.SeaNight -> R.drawable.play_sea
                is Colors.Turquoise -> R.drawable.play_turquoise
                is Colors.TurquoiseNight -> R.drawable.play_turquoise
                else -> R.drawable.play
            }
        }
    )

    private fun setRepeatButtonImage(looping: Boolean) = repeatButton.setImageResource(
        ViewSetter.getRepeatButtonImage(looping)
    )

    internal fun playNext() = (application as MainApplication).run {
        mainActivityViewModel.progressLiveData.value = 0
        val curIndex: Int

        (application as MainApplication).run {
            curIndex = (curInd + 1).let { if (it == curPlaylist.size) 0 else it }
            curPath = curPlaylist[curIndex].path
        }

        val looping = isLooping ?: StorageUtil(applicationContext).loadLooping()
        playAudio(curPath)
        updateUI(curPlaylist[curIndex] to false)
        setRepeatButtonImage(looping)
        curTime.setText(R.string.current_time)
        trackPlayingBar.progress = 0
    }

    private fun playPrev() = (application as MainApplication).run {
        mainActivityViewModel.progressLiveData.value = 0
        val curIndex: Int

        (application as MainApplication).run {
            curIndex = (curInd - 1).let { if (it < 0) curPlaylist.size - 1 else it }
            curPath = curPlaylist[curIndex].path
        }

        val looping = isLooping ?: StorageUtil(applicationContext).loadLooping()
        updateUI(curPlaylist[curIndex] to false)
        playAudio(curPath)
        setRepeatButtonImage(looping)
        curTime.setText(R.string.current_time)
        trackPlayingBar.progress = 0
    }

    /**
     * Calculates current position for playing seek bar
     */

    internal fun run() {
        val load = StorageUtil(applicationContext).loadTrackPauseTime()
        var currentPosition = curTimeData ?: load
        val total = curTrack.unwrap().duration.toInt()
        trackPlayingBar.max = total

        while (isPlaying == true && currentPosition <= total && !draggingSeekBar) {
            currentPosition = curTimeData ?: load
            trackPlayingBar.progress = currentPosition
            Thread.sleep(50)
        }
    }

    internal fun playAudio(path: String) {
        (application as MainApplication).curPath = path
        StorageUtil(applicationContext).storeTrackPath(path)

        when {
            !(application as MainApplication).serviceBound -> {
                val playerIntent = Intent(this, MediaPlayerService::class.java)

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

    internal fun resumePlaying(resumePos: Int) = when {
        !(application as MainApplication).serviceBound -> {
            StorageUtil(applicationContext).apply {
                storeTracks((application as MainApplication).allTracks.toList())
                storeTrackPath(curPath)
            }

            val playerIntent = Intent(this, MediaPlayerService::class.java)
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

    internal fun pausePlaying() = when {
        (application as MainApplication).serviceBound -> sendBroadcast(Intent(Broadcast_PAUSE))

        else -> {
            StorageUtil(applicationContext).apply {
                storeTracks((application as MainApplication).allTracks.toList())
                storeTrackPath(curPath)
                storeTrackPauseTime(curTimeData ?: -1)
            }

            val playerIntent = Intent(this, MediaPlayerService::class.java)
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

    private fun setLooping(looping: Boolean) = when {
        (application as MainApplication).serviceBound -> sendBroadcast(
            Intent(Broadcast_LOOPING)
                .putExtra(IS_LOOPING_ARG, looping)
        )

        else -> {
            StorageUtil(applicationContext).apply {
                storeTracks((application as MainApplication).allTracks.toList())
                storeTrackPath(curPath)
                storeTrackPauseTime(curTimeData ?: -1)
            }

            val playerIntent = Intent(this, MediaPlayerService::class.java)
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

    private fun stopPlaying() = when {
        (application as MainApplication).serviceBound -> sendBroadcast(Intent(Broadcast_STOP))
        else -> Unit // not initialized
    }

    private fun addTrackToQueue(track: Track) =
        (application as MainApplication).curPlaylist.add(track)

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
                    ?: resumePlaying(-1)
            }

            else -> curPlaylist.remove(track)
        }
    }

    /**
     * Shows popup menu about track
     * @param view settings button view
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
                        R.id.nav_change_track_info -> Toast
                            .makeText(
                                this@MainActivity,
                                resources.getString(R.string.coming_soon),
                                Toast.LENGTH_LONG
                            )
                            .show()

                        R.id.nav_add_to_queue -> addTrackToQueue(track)
                        R.id.nav_remove_from_queue -> removeTrackFromQueue(track)
                        R.id.nav_add_track_to_favourites -> trackLikeAction(track)
                        R.id.nav_add_to_playlist -> addToPlaylist(track)
                    }

                    return@setOnMenuItemClickListener true
                }

                show()
            }
    }

    /**
     * Shows popup menu about artist
     * @param view settings button view
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

                    when {
                        contain -> favouriteRepository.removeArtist(favouriteArtist)
                        else -> favouriteRepository.addArtist(favouriteArtist)
                    }

                    return@setOnMenuItemClickListener true
                }

                show()
            }
    }

    private fun trackLikeAction(track: Track) {
        val contain = runBlocking {
            favouriteRepository.getTrackAsync(track.path).await()
        } != null

        val favouriteTrack = track.asFavourite()

        when {
            contain -> favouriteRepository.removeTrack(favouriteTrack)
            else -> favouriteRepository.addTrack(favouriteTrack)
        }

        likeButton.setImageResource(ViewSetter.getLikeButtonImage(!contain))
    }

    private fun addToPlaylist(track: Track) = runBlocking {
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
     * Update UI on service notification clicks
     */

    internal fun customize(defaultPlaying: Boolean = true) {
        val p = isPlaying ?: defaultPlaying
        setPlayButtonImage(p)
        setPlayButtonSmallImage(p)
        curTrack.takeIf { it != None }?.unwrap()?.let { it to true }
    }

    private fun handlePlayEvent() {
        when (isPlaying) {
            true -> {
                pausePlaying()
                mainActivityViewModel.progressLiveData.value = curTimeData
            }

            else -> {
                resumePlaying(-1) // continue default
                playingThread = Some(thread { run() })
            }
        }
    }

    internal fun time() {
        playingThread = Some(thread { run() })
    }
}