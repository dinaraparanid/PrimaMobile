package com.dinaraparanid.prima

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.IBinder
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
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.dinaraparanid.MainApplication
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.fragments.*
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.MediaPlayerService.LocalBinder
import com.dinaraparanid.prima.viewmodels.MainActivityViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.concurrent.thread
import kotlin.math.ceil

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

    private val mainActivityViewModel: MainActivityViewModel by lazy {
        ViewModelProvider(this)[MainActivityViewModel::class.java]
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var fragmentContainer: FrameLayout

    internal var playingThread: Option<Thread> = None
    internal var serviceBound = false
    internal var player: MediaPlayerService = MediaPlayerService()

    private val trackList = mutableListOf<Track>()
    private var draggingSeekBar = false
    private var progr = 0
    private var like = false
    private var actionBarSize = 0

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            player = (service as LocalBinder).service
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    }

    companion object {
        const val REQUEST_ID_MULTIPLE_PERMISSIONS: Int = 1
        const val Broadcast_PLAY_NEW_TRACK: String = "com.dinaraparanid.prima.PlayNewAudio"
        const val Broadcast_PLAY_NEXT_TRACK: String = "com.dinaraparanid.prima.PlayNextAudio"
        const val Broadcast_PLAY_PREV_TRACK: String = "com.dinaraparanid.prima.PlayPrevAudio"
        const val Broadcast_PAUSE: String = "com.dinaraparanid.prima.Pause"
        const val Broadcast_REPEAT1: String = "com.dinaraparanid.prima.Repeat1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (application as MainApplication).mainActivity = this

        mainActivityViewModel.load(
            savedInstanceState?.getInt("sheet_behavior_state"),
            savedInstanceState?.getBoolean("repeat1"),
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

        if (checkAndRequestPermissions()) loadTracks()

        returnButton.setImageResource(ViewSetter.returnButtonImage)
        nextTrackButton.setImageResource(ViewSetter.nextTrackButtonImage)
        prevTrackButton.setImageResource(ViewSetter.prevTrackButtonImage)
        playlistButton.setImageResource(ViewSetter.playlistButtonImage)
        trackLyricsButton.setImageResource(ViewSetter.lyricsButtonImage)
        settingsButton.setImageResource(ViewSetter.settingsButtonImage)
        likeButton.setImageResource(ViewSetter.getLikeButtonImage(like))

        setPlayButtonSmallImage((application as MainApplication).musicPlayer?.isPlaying ?: true)
        setRepeatButtonImage()

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
            mainActivityViewModel.repeat1LiveData.value =
                !mainActivityViewModel.repeat1LiveData.value!!
            setRepeatButtonImage()
            player.mediaPlayer!!.isLooping = mainActivityViewModel.repeat1LiveData.value!!
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
            setPlayButtonImage((application as MainApplication).musicPlayer?.isPlaying ?: true)
        }

        playButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                handlePlayEvent()

            setPlayButtonSmallImage(
                (application as MainApplication).musicPlayer?.isPlaying ?: false
            )
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
                    val trackLen = (application as MainApplication).curTrack.unwrap().duration
                    val maxProgress = trackPlayingBar.max.toLong()
                    val time = progress * trackLen / maxProgress
                    val calculatedTime = calcTrackTime(time)

                    val str = "${calculatedTime.first.let { if (it < 10) "0$it" else it }}:" +
                            "${calculatedTime.second.let { if (it < 10) "0$it" else it }}:" +
                            "${calculatedTime.third.let { if (it < 10) "0$it" else it }}"

                    curTime.text = str

                    (application as MainApplication).run {
                        if (ceil(progress / 1000.0).toInt() == 0 &&
                            player.mediaPlayer != null && !player.mediaPlayer!!.isPlaying
                        ) trackPlayingBar.progress = 0
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) =
                    (application as MainApplication).run {
                        if (player.mediaPlayer != null) {
                            val progress = seekBar!!.progress
                            val maxProgress = seekBar.max
                            val trackLen = curTrack.unwrap().duration

                            draggingSeekBar = false

                            if (player.mediaPlayer!!.isPlaying) {
                                player.pauseMedia()
                                playingThread.unwrap().join()
                            }

                            player.resumeMedia((progress * trackLen / maxProgress).toInt())
                            playingThread = Some(thread { run() })
                        }
                    }
            }
        )

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

            onTrackSelected(
                (application as MainApplication).curTrack.orNull() ?: player.activeTrack!!,
                false // Only for playing panel
            )
        }

        if (player.activeTrack != null) {
            setPlayButtonSmallImage(
                (application as MainApplication).musicPlayer?.isPlaying ?: false
            )

            if (mainActivityViewModel.sheetBehaviorPositionLiveData.value!! == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
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

                    setPlayButtonSmallImage(
                        (application as MainApplication).musicPlayer?.isPlaying ?: false
                    )

                    setPlayButtonImage(
                        (application as MainApplication).musicPlayer?.isPlaying ?: false
                    )

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

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null)
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    TrackListFragment.newInstance(
                        mainLabel.text.toString(),
                        Playlist(tracks = trackList)
                    )
                )
                .commit()

        val tv = TypedValue()
        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarSize = TypedValue
                .complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("sheet_behavior_state", sheetBehavior.state)
        outState.putBoolean("service_state", serviceBound)
        outState.putBoolean("repeat1", mainActivityViewModel.repeat1LiveData.value!!)
        outState.putInt("progress", mainActivityViewModel.progressLiveData.value!!)
        outState.putInt("cur_index", mainActivityViewModel.curIndexLiveData.value!!)
        outState.putBoolean("track_selected", mainActivityViewModel.trackSelectedLiveData.value!!)

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("service_state")
    }

    override fun onResume() {
        super.onResume()

        if ((application as MainApplication).musicPlayer?.isPlaying == true)
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
                    if ((application as MainApplication).musicPlayer?.isPlaying == true)
                        playingPart.isVisible = true
                }
                .commit()
        else Toast.makeText(this, "Coming Soon", Toast.LENGTH_LONG).show()

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        try {
            if (isFinishing) {
                player.removeNotification()
            }
        } catch (e: Exception) {
            // There were no notifications
        }

        player.stopSelf()

        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> drawerLayout.closeDrawer(GravityCompat.START)
            else -> super.onBackPressed()
        }
    }

    override fun onTrackSelected(track: Track, needToPlay: Boolean) {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            val sortedTracks = trackList.sortedBy { it.title }
            val end = sortedTracks.takeWhile { it.id != track.id }
            val newTrack = (application as MainApplication)
                .curTrack.orNull()?.let { track.id != it.id } ?: true

            /*mainActivityViewModel.curPlaylistLiveData.value!!.apply {
                clear()
                addAll(sortedTracks.dropWhile { it.id != track.id })
                addAll(end)
            }*/

            val playing = when ((application as MainApplication).musicPlayer?.isPlaying) {
                null -> true

                false -> when {
                    needToPlay -> true
                    else -> false
                }

                else -> when {
                    newTrack -> true

                    else -> when {
                        needToPlay -> false
                        else -> true
                    }
                }
            }

            updateUI(track)
            setPlayButtonSmallImage(playing)
            setPlayButtonImage(playing)

            if (needToPlay) {
                returnButton.alpha = 0.0F
                settingsButton.alpha = 0.0F
                albumImage.alpha = 0.0F
            }

            trackTitleSmall.isSelected = true
            trackArtists.isSelected = true

            if (!playingPart.isVisible)
                playingPart.isVisible = true

            mainActivityViewModel.curIndexLiveData.value = end.size
            mainActivityViewModel.trackSelectedLiveData.value = true
            (application as MainApplication).curTrack = Some(track)

            if (player.mediaPlayer == null)
                player.mediaPlayer = (application as MainApplication).musicPlayer

            when {
                needToPlay -> when ((application as MainApplication).musicPlayer?.isPlaying) {
                    null -> when {
                        newTrack -> playAudio(end.size)
                        else -> player.resumeMedia()
                    }

                    false -> when {
                        newTrack -> playAudio(end.size)
                        else -> player.resumeMedia()
                    }

                    else -> when {
                        newTrack -> {
                            playAudio(end.size)
                            playingThread = Some(thread { run() })
                        }

                        else -> {
                            player.pauseMedia()
                            playingThread.orNull()?.join()
                        }
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
        permissions: Array<String>, grantResults: IntArray
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

                        else -> {
                            when {
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                    this, Manifest.permission.READ_PHONE_STATE
                                ) -> {
                                    showDialogOK(
                                        "Phone state and storage permissions required for this app"
                                    ) { _, which ->
                                        when (which) {
                                            DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
                                            DialogInterface.BUTTON_NEGATIVE -> {
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    Toast.makeText(
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

    private fun setTheme() = setTheme(
        when (Params.getInstance().theme) {
            is Colors.Blue -> R.style.Theme_MusicPlayerBlue
            is Colors.Green -> R.style.Theme_MusicPlayerGreen
            is Colors.GreenTurquoise -> R.style.Theme_MusicPlayerGreenTurquoise
            is Colors.Lemon -> R.style.Theme_MusicPlayerLemon
            is Colors.Orange -> R.style.Theme_MusicPlayerOrange
            is Colors.Pink -> R.style.Theme_MusicPlayerPink
            is Colors.Purple -> R.style.Theme_MusicPlayerPurple
            is Colors.Red -> R.style.Theme_MusicPlayerRed
            is Colors.Sea -> R.style.Theme_MusicPlayerSea
            is Colors.Turquoise -> R.style.Theme_MusicPlayerTurquoise
            is Colors.BlueNight -> R.style.Theme_MusicPlayerBlueNight
            is Colors.GreenNight -> R.style.Theme_MusicPlayerGreenNight
            is Colors.GreenTurquoiseNight -> R.style.Theme_MusicPlayerGreenTurquoiseNight
            is Colors.LemonNight -> R.style.Theme_MusicPlayerLemonNight
            is Colors.OrangeNight -> R.style.Theme_MusicPlayerOrangeNight
            is Colors.PinkNight -> R.style.Theme_MusicPlayerPinkNight
            is Colors.PurpleNight -> R.style.Theme_MusicPlayerPurpleNight
            is Colors.RedNight -> R.style.Theme_MusicPlayerRedNight
            is Colors.SeaNight -> R.style.Theme_MusicPlayerSeaNight
            is Colors.TurquoiseNight -> R.style.Theme_MusicPlayerTurquoiseNight
            else -> throw IllegalStateException("Wrong theme")
        }
    )

    private fun updateUI(track: Track) {
        playingPart.setBackgroundColor(if (Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE)

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

        val start = "00:00:00"
        curTime.text = start

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

    private fun setRepeatButtonImage() = repeatButton.setImageResource(
        ViewSetter.getRepeatButtonImage(mainActivityViewModel.repeat1LiveData.value!!)
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
            }
        }
    }

    internal fun playNext() = (application as MainApplication).run {
        mainActivityViewModel.curIndexLiveData.value =
            (mainActivityViewModel.curIndexLiveData.value!! + 1)
                .let { if (it == trackList.size) 0 else it }

        updateUI(trackList[mainActivityViewModel.curIndexLiveData.value!!])

        (application as MainApplication).curTrack =
            Some(trackList[mainActivityViewModel.curIndexLiveData.value!!])

        playAudio(mainActivityViewModel.curIndexLiveData.value!!)
    }

    private fun playPrev() = (application as MainApplication).run {
        mainActivityViewModel.curIndexLiveData.value =
            (mainActivityViewModel.curIndexLiveData.value!! - 1)
                .let { if (it < 0) trackList.size - 1 else it }

        updateUI(trackList[mainActivityViewModel.curIndexLiveData.value!!])

        (application as MainApplication).curTrack =
            Some(trackList[mainActivityViewModel.curIndexLiveData.value!!])

        playAudio(mainActivityViewModel.curIndexLiveData.value!!)
    }

    internal fun run() = (application as MainApplication).run {
        var currentPosition = musicPlayer!!.currentPosition
        val total = musicPlayer!!.duration

        while (musicPlayer != null && musicPlayer!!.isPlaying &&
            currentPosition < total && !draggingSeekBar
        ) {
            currentPosition = musicPlayer!!.currentPosition
            val trackLen = musicPlayer!!.duration

            trackPlayingBar.progress = (100 * currentPosition / trackLen)
            Thread.sleep(10)
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            val permissionReadPhoneState =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)

            val permissionStorage =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

            val listPermissionsNeeded: MutableList<String> = mutableListOf()

            if (permissionReadPhoneState != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)

            if (permissionStorage != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)

            return when {
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

        return false
    }

    internal fun playAudio(audioIndex: Int) {
        if (player.mediaPlayer != null && player.mediaPlayer!!.isPlaying) {
            player.mediaPlayer!!.pause()
            playingThread.unwrap().join()
        }

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
                // Store the new audioIndex to SharedPreferences
                StorageUtil(applicationContext).storeTrackIndex(audioIndex)

                // Service is active
                // Send a broadcast to the service -> PLAY_NEW_TRACK
                sendBroadcast(Intent(Broadcast_PLAY_NEW_TRACK))
            }
        }
    }

    internal fun customize() = (applicationContext as MainApplication).run {
        mainActivity?.updateUI(curTrack.unwrap())
        mainActivity?.setPlayButtonImage(musicPlayer?.isPlaying == true)
        mainActivity?.setPlayButtonSmallImage(musicPlayer?.isPlaying == true)
    }

    internal fun calcTrackTime(millis: Long): Triple<Long, Long, Long> {
        var cpy = millis

        val h = cpy / 3600000
        cpy -= h * 3600000

        val m = cpy / 60000
        cpy -= m * 60000

        val s = cpy / 1000

        return Triple(h, m, s)
    }

    private fun handlePlayEvent() = (application as MainApplication).run {
        when {
            musicPlayer!!.isPlaying -> {
                player.pauseMedia()
                playingThread.unwrap().join()
            }

            else -> {
                player.resumeMedia(mainActivityViewModel.progressLiveData.value!!)
                playingThread = Some(thread { run() })
            }
        }
    }
}