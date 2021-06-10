package com.dinaraparanid.prima

import android.Manifest
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
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
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.MainApplication
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.fragments.*
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.viewmodels.MainActivityViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.IllegalStateException
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.system.exitProcess

class MainActivity :
    AppCompatActivity(),
    TrackListFragment.Callbacks,
    ArtistListFragment.Callbacks,
    NavigationView.OnNavigationItemSelectedListener {
    private lateinit var playingPart: ConstraintLayout
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

    internal val mainActivityViewModel: MainActivityViewModel by lazy {
        ViewModelProvider(this)[MainActivityViewModel::class.java]
    }

    internal var currentFragment: Fragment? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var fragmentContainer: FrameLayout
    internal lateinit var sheetBehavior: BottomSheetBehavior<View>
        private set

    internal var playingThread: Option<Thread> = None
    internal var serviceBound = false

    private val trackList = mutableListOf<Track>()
    private var draggingSeekBar = false
    private var progr = 0
    private var like = false
    private var actionBarSize = 0
    private var tracksLoaded = false
    private var timeSave = 0

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    }

    internal inline val curTrack
        get() = mainActivityViewModel
            .curIndexLiveData.value!!
            .takeIf { it != -1 }?.let { Some(trackList[it]) } ?: None

    private inline val curIndex
        get() = mainActivityViewModel.curIndexLiveData.value!!

    internal inline val isPlaying: Boolean?
        get() {
            try {
                return (application as MainApplication).musicPlayer?.isPlaying
            } catch (e: Exception) {
                // on close err
            }

            return false
        }

    private inline val isLooping: Boolean?
        get() {
            try {
                return (application as MainApplication).musicPlayer?.isLooping
            } catch (e: Exception) {
                // on close err
            }
            return false
        }

    private inline val curTimeData: Int?
        get() {
            try {
                return (application as MainApplication).musicPlayer?.currentPosition
            } catch (e: Exception) {
                // on close app
            }

            return 0
        }

    companion object {
        const val REQUEST_ID_MULTIPLE_PERMISSIONS: Int = 1
        const val Broadcast_PLAY_NEW_TRACK: String = "com.dinaraparanid.prima.PlayNewAudio"
        const val Broadcast_RESUME: String = "com.dinaraparanid.prima.Resume"
        const val Broadcast_PAUSE: String = "com.dinaraparanid.prima.Pause"
        const val Broadcast_LOOPING: String = "com.dinaraparanid.prima.StartLooping"
        const val Broadcast_STOP: String = "com.dinaraparanid.prima.Stop"

        @JvmStatic
        internal fun calcTrackTime(millis: Long): Triple<Long, Long, Long> {
            var cpy = millis

            val h = cpy / 3600000
            cpy -= h * 3600000

            val m = cpy / 60000
            cpy -= m * 60000

            val s = cpy / 1000

            return Triple(h, m, s)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (application as MainApplication).mainActivity = this

        mainActivityViewModel.load(
            savedInstanceState?.getInt("sheet_behavior_state"),
            savedInstanceState?.getInt("progress"),
            savedInstanceState?.getInt("cur_index"),
            savedInstanceState?.getBoolean("track_selected")
        )

        appBarLayout = findViewById<CoordinatorLayout>(R.id.main_coordinator_layout)
            .findViewById(R.id.appbar)

        val toolbar = appBarLayout.findViewById<Toolbar>(R.id.toolbar)
        mainLabel = toolbar.findViewById(R.id.main_label)
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

        while (!checkAndRequestPermissions()) Unit
        loadTracks()

        returnButton.setImageResource(ViewSetter.returnButtonImage)
        nextTrackButton.setImageResource(ViewSetter.nextTrackButtonImage)
        prevTrackButton.setImageResource(ViewSetter.prevTrackButtonImage)
        playlistButton.setImageResource(ViewSetter.playlistButtonImage)
        trackLyricsButton.setImageResource(ViewSetter.lyricsButtonImage)
        settingsButton.setImageResource(ViewSetter.settingsButtonImage)
        likeButton.setImageResource(ViewSetter.getLikeButtonImage(like))

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
            like = !like
            likeButton.setImageResource(ViewSetter.getLikeButtonImage(like))
            // TODO: favourites
        }

        repeatButton.setOnClickListener {
            val looping = !isLooping!!
            setLooping(!isLooping!!)
            setRepeatButtonImage(looping)
        }

        playlistButton.setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_LONG).show()
            /*supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                .replace(
                    R.id.fragment_container,
                    TrackListFragment.newInstance(
                        mainLabel.text.toString(),
                        Playlist(tracks = trackList),
                        false
                    )
                )
                .addToBackStack(null)
                .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
                .commit()*/
        }

        trackLyricsButton.setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_LONG).show()
        }

        returnButton.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        settingsButton.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                PopupMenu(this, it).apply {
                    menuInflater.inflate(R.menu.menu_track_settings, menu)

                    setOnMenuItemClickListener {
                        Toast.makeText(this@MainActivity, "Coming Soon", Toast.LENGTH_LONG).show()
                        return@setOnMenuItemClickListener true
                        /*when (it.itemId) {
                            // TODO: Track settings menu functionality
                        }*/
                    }

                    show()
                }
        }

        playButton.setOnClickListener {
            handlePlayEvent()
            setPlayButtonImage(isPlaying!!)
        }

        playButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                handlePlayEvent()
            setPlayButtonSmallImage(isPlaying!!)
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
                    val trackLen = curTrack.unwrap().duration
                    val maxProgress = trackPlayingBar.max.toLong()
                    val time = progress * trackLen / maxProgress

                    val calculatedTime = calcTrackTime(time)

                    val str = "${calculatedTime.first.let { if (it < 10) "0$it" else it }}:" +
                            "${calculatedTime.second.let { if (it < 10) "0$it" else it }}:" +
                            "${calculatedTime.third.let { if (it < 10) "0$it" else it }}"

                    curTime.text = str

                    if (ceil(progress / 1000.0).toInt() == 0 && !isPlaying!!)
                        trackPlayingBar.progress = 0
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) =
                    (application as MainApplication).run {
                        val progress = seekBar!!.progress
                        val maxProgress = seekBar.max
                        val trackLen = curTrack.unwrap().duration

                        draggingSeekBar = false

                        if (isPlaying!!)
                            pausePlaying()

                        resumePlaying((progress * trackLen / maxProgress).toInt())
                        playingThread = Some(thread { run() })
                    }
            }
        )

        setPlayButtonImage(isPlaying ?: false)
        setPlayButtonSmallImage(isPlaying ?: false)

        sheetBehavior = BottomSheetBehavior.from(playingPart)

        if (mainActivityViewModel.trackSelectedLiveData.value!!) {
            when (BottomSheetBehavior.STATE_EXPANDED) {
                mainActivityViewModel.sheetBehaviorPositionLiveData.value!! -> {
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

            curTrack.takeIf { it != None }
                ?.let { onTrackSelected(it.unwrap(), false) } // Only for playing panel
        }

        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) =
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> toolbar.isVisible = false
                        else -> Unit
                    }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (!toolbar.isVisible)
                        toolbar.isVisible = true

                    val p = isPlaying!!
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

        while (!tracksLoaded) Unit

        currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null)
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    TrackListFragment.newInstance(
                        mainLabel.text.toString(),
                        Playlist(tracks = trackList)
                    ).apply { currentFragment = this }
                )
                .commit()

        if (curIndex != -1) {
            setPlayButtonSmallImage(isPlaying!!)

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
        outState.putInt("sheet_behavior_state", sheetBehavior.state)
        outState.putBoolean("service_state", serviceBound)
        outState.putInt("progress", mainActivityViewModel.progressLiveData.value!!)
        outState.putInt("cur_index", curIndex)
        outState.putBoolean("track_selected", mainActivityViewModel.trackSelectedLiveData.value!!)

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("service_state")
    }

    override fun onResume() {
        super.onResume()

        if (isPlaying == true)
            playingThread = Some(thread { run() })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_tracks)
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
                    TrackListFragment.newInstance(
                        mainLabel.text.toString(),
                        Playlist(tracks = trackList)
                    ).apply { mainLabel.setText(R.string.tracks) }
                    /*when (item.itemId) {
                        R.id.nav_tracks -> TrackListFragment.newInstance(
                            mainLabel.text.toString(),
                            Playlist(tracks = trackList)
                        ).apply { mainLabel.setText(R.string.tracks) }

                        R.id.nav_playlists -> PlaylistListFragment.newInstance()
                            .apply { mainLabel.setText(R.string.playlists) }

                        R.id.nav_artists -> ArtistListFragment.newInstance()
                            .apply { mainLabel.setText(R.string.artists) }

                        R.id.nav_favourite_artists -> FavouriteArtistsFragment.newInstance()
                            .apply { mainLabel.setText(R.string.favourite_artists) }

                        R.id.nav_favourite_tracks -> FavouriteTracksFragment.newInstance()
                            .apply { mainLabel.setText(R.string.favourite_tracks) }

                        R.id.nav_recommendations -> RecommendationsFragment.newInstance()
                            .apply { mainLabel.setText(R.string.recommendations) }

                        R.id.nav_compilation -> CompilationFragment.newInstance()
                            .apply { mainLabel.setText(R.string.compilation) }

                        R.id.nav_settings -> SettingsFragment.newInstance()
                            .apply { mainLabel.setText(R.string.settings) }

                        else -> AboutAppFragment.newInstance()
                            .apply { mainLabel.setText(R.string.about_app) }
                    }*/
                )
                .addToBackStack(null)
                .apply {
                    if (isPlaying == true)
                        playingPart.isVisible = true
                }
                .commit()
        else Toast.makeText(this, "Coming Soon", Toast.LENGTH_LONG).show()

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
                    else -> super.onBackPressed()
                }

                if (supportFragmentManager.backStackEntryCount == 0) {
                    try {
                        (applicationContext
                            .getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager)
                            .cancelAll()
                        stopPlaying()
                    } catch (e: Exception) {
                        // There were no notifications or player
                    }

                    finishAndRemoveTask()
                    exitProcess(0)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        (applicationContext
            .getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager)
            .cancelAll()
    }

    override fun onTrackSelected(track: Track, needToPlay: Boolean) {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            val end = trackList.takeWhile { it.id != track.id }
            mainActivityViewModel.trackSelectedLiveData.value = true
            val newTrack = curTrack
                .takeIf { it != None }
                ?.let { track.id != it.unwrap().id } ?: true
            mainActivityViewModel.curIndexLiveData.value = end.size

            /*mainActivityViewModel.curPlaylistLiveData.value!!.apply {
                clear()
                addAll(trackList.dropWhile { it.id != track.id })
                addAll(end)
            }*/

            val p = if (serviceBound) isPlaying ?: true else true

            updateUI(track)
            setPlayButtonSmallImage(p)
            setPlayButtonImage(p)

            if (needToPlay) {
                returnButton.alpha = 0.0F
                settingsButton.alpha = 0.0F
                albumImage.alpha = 0.0F
            }

            trackTitleSmall.isSelected = true
            trackArtists.isSelected = true

            if (!playingPart.isVisible)
                playingPart.isVisible = true

            when {
                needToPlay -> when {
                    p -> when {
                        newTrack -> {
                            playAudio(end.size)
                            playingThread = Some(thread { run() })
                        }

                        else -> pausePlaying()
                    }

                    else -> when {
                        newTrack -> playAudio(end.size)
                        else -> resumePlaying(-1) // continue on paused position
                    }
                }

                else -> playingThread = Some(thread { run() })
            }
        }
    }

    override fun onArtistSelected(artist: Artist) {
        // TODO: Artist Selection
        Toast.makeText(this, "Coming Soon", Toast.LENGTH_LONG).show()
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
                            loadTracks()

                        else -> when {
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.READ_EXTERNAL_STORAGE
                            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.READ_PHONE_STATE
                            ) -> showDialogOK(
                                "Phone state and storage permissions required for this app"
                            ) { _, which ->
                                when (which) {
                                    DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
                                    DialogInterface.BUTTON_NEGATIVE -> Unit
                                }
                            }

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

    private fun updateUI(track: Track) {
        playingPart.setBackgroundColor(ViewSetter.backgroundColor)
        setRepeatButtonImage(isLooping ?: false)

        val artistAlbum =
            "${
                track.artist
                    .let { if (it == "<unknown>") "Unknown artist" else it }
            } / ${
                track.album
                    .let {
                        if (it == "<unknown>" || it == track
                                .path
                                .split('/')
                                .takeLast(2)
                                .first()
                        ) "Unknown album" else it
                    }
            }"

        trackTitleSmall.text = track.title.let { if (it == "<unknown>") "Unknown track" else it }
        trackArtists.text = track.artist.let { if (it == "<unknown>") "Unknown artist" else it }

        trackTitle.text = track.title.let { if (it == "<unknown>") "Unknown track" else it }
        artistsAlbum.text = artistAlbum

        trackTitleSmall.isSelected = true
        trackArtists.isSelected = true
        trackTitle.isSelected = true
        artistsAlbum.isSelected = true

        val trackL = calcTrackTime(track.duration)
        val str = "${trackL.first.let { if (it < 10) "0$it" else it }}:" +
                "${trackL.second.let { if (it < 10) "0$it" else it }}:" +
                "${trackL.third.let { if (it < 10) "0$it" else it }}"
        trackLength.text = str
    }

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
        ViewSetter.getRepeatButtonImage(looping ?: false)
    )

    private fun loadTracks() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val order = MediaStore.Audio.Media.TITLE + " ASC"

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            order
        ).use { cursor ->
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    trackList.add(
                        Track(
                            cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getLong(5)
                        )
                    )
                }

                trackList.distinctBy { it.path }
            }
        }

        tracksLoaded = true
    }

    internal fun playNext() = (application as MainApplication).run {
        mainActivityViewModel.curIndexLiveData.value =
            (mainActivityViewModel.curIndexLiveData.value!! + 1)
                .let { if (it == trackList.size) 0 else it }

        updateUI(trackList[mainActivityViewModel.curIndexLiveData.value!!])
        //(currentFragment as TrackListFragment?)?.adapter?.highlight(curTrack.unwrap())
        playAudio(mainActivityViewModel.curIndexLiveData.value!!)
    }

    private fun playPrev() = (application as MainApplication).run {
        mainActivityViewModel.curIndexLiveData.value =
            (mainActivityViewModel.curIndexLiveData.value!! - 1)
                .let { if (it < 0) trackList.size - 1 else it }

        updateUI(trackList[mainActivityViewModel.curIndexLiveData.value!!])
        //(currentFragment as TrackListFragment?)?.adapter?.highlight(curTrack.unwrap())
        playAudio(mainActivityViewModel.curIndexLiveData.value!!)
    }

    /**
     * Calculates current position for playing seek bar
     */

    internal fun run() {
        var currentPosition = curTimeData ?: 0
        val total = trackList[curIndex].duration

        while (isPlaying == true && currentPosition < total && !draggingSeekBar) {
            currentPosition = curTimeData ?: 0
            val trackLen = trackList[curIndex].duration

            trackPlayingBar.progress = (100 * currentPosition / trackLen).toInt()
            Thread.sleep(10)
        }
    }

    private fun checkAndRequestPermissions() = when {
        SDK_INT >= Build.VERSION_CODES.M -> {
            val permissionReadPhoneState =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)

            val permissionStorage =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

            val listPermissionsNeeded: MutableList<String> = mutableListOf()

            if (permissionReadPhoneState != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)

            if (permissionStorage != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)

            when {
                listPermissionsNeeded.isNotEmpty() -> {
                    ActivityCompat.requestPermissions(
                        this,
                        listPermissionsNeeded.toTypedArray(),
                        REQUEST_ID_MULTIPLE_PERMISSIONS
                    )
                    false
                }

                else -> true
            }
        }

        else -> false
    }

    internal fun playAudio(audioIndex: Int) {
        mainActivityViewModel.curIndexLiveData.value = audioIndex

        when {
            !serviceBound -> {
                // Store Serializable audioList to SharedPreferences
                StorageUtil(applicationContext).apply {
                    storeTracks(trackList)
                    storeTrackIndex(audioIndex)
                }

                val playerIntent = Intent(this, MediaPlayerService::class.java)
                startService(playerIntent)
                bindService(playerIntent, serviceConnection, BIND_AUTO_CREATE)
            }

            else -> {
                if (isPlaying == true) {
                    sendBroadcast(Intent(Broadcast_PAUSE))
                    pausePlaying()
                }

                // Store the new audioIndex to SharedPreferences
                StorageUtil(applicationContext).storeTrackIndex(audioIndex)

                // Service is active
                // Send a broadcast to the service -> PLAY_NEW_TRACK
                sendBroadcast(Intent(Broadcast_PLAY_NEW_TRACK))
            }
        }
    }

    internal fun resumePlaying(resumePos: Int) = when {
        serviceBound -> sendBroadcast(
            Intent(Broadcast_RESUME).putExtra(
                "resume_position",
                resumePos
            )
        )

        else -> throw IllegalStateException("Player is not initialized")
    }

    internal fun pausePlaying() = when {
        serviceBound -> sendBroadcast(Intent(Broadcast_PAUSE))
        else -> throw IllegalStateException("Player is not initialized")
    }

    private fun setLooping(looping: Boolean) = when {
        serviceBound -> sendBroadcast(
            Intent(Broadcast_LOOPING)
                .putExtra("is_looping", looping)
        )

        else -> throw IllegalStateException("Player is not initialized")
    }

    private fun stopPlaying() = when {
        serviceBound -> sendBroadcast(Intent(Broadcast_STOP))
        else -> throw IllegalStateException("Player is not initialized")
    }

    /**
     * Update UI on service notification clicks
     */

    internal fun customize() {
        val p = isPlaying ?: true
        setPlayButtonImage(p)
        setPlayButtonSmallImage(p)
        updateUI(curTrack.unwrap())
    }

    private fun handlePlayEvent() {
        when {
            isPlaying!! -> {
                pausePlaying()
                mainActivityViewModel.progressLiveData.value = curTimeData
            }

            else -> {
                resumePlaying(-1) // continue default
                playingThread = Some(thread { run() })
            }
        }
    }
}