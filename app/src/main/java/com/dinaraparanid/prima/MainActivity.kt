package com.dinaraparanid.prima

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Contact
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databinding.ActivityMainBinding
import com.dinaraparanid.prima.databinding.NavHeaderMainBinding
import com.dinaraparanid.prima.fragments.*
import com.dinaraparanid.prima.fragments.EqualizerFragment
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.dialogs.AreYouSureDialog
import com.dinaraparanid.prima.utils.dialogs.GetHappiApiKeyDialog
import com.dinaraparanid.prima.utils.dialogs.TrackSearchLyricsParamsDialog
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.unwrap
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.rustlibs.NativeLibrary
import com.dinaraparanid.prima.utils.web.happi.FoundTrack
import com.dinaraparanid.prima.utils.web.happi.HappiFetcher
import com.dinaraparanid.prima.utils.web.happi.LyricsParser
import com.dinaraparanid.prima.viewmodels.androidx.MainActivityViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.set
import kotlin.math.ceil

class MainActivity :
    AppCompatActivity(),
    AbstractTrackListFragment.Callbacks,
    AbstractArtistListFragment.Callbacks,
    PlaylistListFragment.Callbacks,
    FontsFragment.Callbacks,
    TrackSelectLyricsFragment.Callbacks,
    TrackChangeFragment.Callbacks,
    TrimFragment.Callbacks,
    ChooseContactFragment.Callbacks,
    NavigationView.OnNavigationItemSelectedListener,
    UIUpdatable<Pair<Track, Boolean>> {
    internal lateinit var binding: ActivityMainBinding

    internal val mainActivityViewModel: MainActivityViewModel by lazy {
        ViewModelProvider(this)[MainActivityViewModel::class.java]
    }

    internal var currentFragment: Fragment? = null
    internal lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var favouriteRepository: FavouriteRepository

    private var playingCoroutine: Option<Job> = None
    private var draggingSeekBar = false
    private var actionBarSize = 0
    internal var upped = false
    internal var needToUpdate = false

    private inline val curTrack
        get() = (application as MainApplication).run {
            curPath.takeIf { it != NO_PATH }
                ?.let {
                    Some(
                        curPlaylist.run { get(indexOfFirst { track -> track.path == it }) }
                    )
                } ?: run {
                StorageUtil(this)
                    .loadTrackPath()
                    .takeIf { it != NO_PATH }
                    ?.let {
                        Some(
                            curPlaylist.run { get(indexOfFirst { track -> track.path == it }) }
                        )
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
        } catch (e: Exception) {
            StorageUtil(applicationContext).loadTrackPauseTime()
        }

    internal val playingToolbarHeight get() = binding.playingLayout.playingToolbar.height

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
        setTheme()
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil
            .setContentView<ActivityMainBinding>(this, R.layout.activity_main)
            .apply {
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.MainActivityViewModel()
                playingLayout.viewModel = viewModel

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

        Params.instance.backgroundImage?.let {
            binding.drawerLayout.background = it.toBitmap().toDrawable(resources)
        }

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

        setSupportActionBar(binding.switchToolbar)

        setRoundingOfPlaylistImage()
        binding.playingLayout.currentTime.text =
            calcTrackTime(curTimeData ?: 0).asTimeString()

        (application as MainApplication).run {
            mainActivity = this@MainActivity
            mainActivityViewModel.viewModelScope.launch { loadAsync().join() }
        }

        Glide.with(this).run {
            load(ViewSetter.getLikeButtonImage(
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
            )).into(binding.playingLayout.likeButton)
        }

        binding.playingLayout.playingToolbar.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.playingLayout.playingPrevTrack.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                playPrevAndUpdUI()
        }

        binding.playingLayout.playingNextTrack.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                playNextAndUpdUI()
        }

        binding.playingLayout.playingAlbumImage.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.playingLayout.playingTrackTitle.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.playingLayout.playingTrackArtists.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.playingLayout.nextTrackButton.setOnClickListener {
            playNextAndUpdUI()
        }

        binding.playingLayout.previousTrackButton.setOnClickListener {
            playPrevAndUpdUI()
        }

        binding.playingLayout.likeButton.setOnClickListener {
            trackLikeAction(curTrack.unwrap())
        }

        binding.playingLayout.repeatButton.setOnClickListener {
            updateLooping()
        }

        binding.playingLayout.playlistButton.setOnClickListener {
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

        binding.playingLayout.trackLyrics.setOnClickListener {
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

        binding.playingLayout.returnButton.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.playingLayout.trackSettingsButton.setOnClickListener {
            trackSettingsButtonAction(it, curTrack.unwrap(), BottomSheetBehavior.STATE_EXPANDED)
        }

        binding.playingLayout.playButton.setOnClickListener {
            setPlayButtonImage(isPlaying?.let { !it } ?: true)
            handlePlayEvent()
        }

        binding.playingLayout.playingPlayButton.setOnClickListener {
            setPlayButtonSmallImage(isPlaying?.let { !it } ?: true)
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                handlePlayEvent()
        }

        binding.playingLayout.equalizerButton.setOnClickListener {
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
                            EqualizerFragment.Builder(binding.mainLabel.text.toString())
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

        binding.playingLayout.trimButton.setOnClickListener {
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
                        curTrack.unwrap()
                    )
                )
                .addToBackStack(null)
                .commit()

            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.selectButton.setOnClickListener { view ->
            if (binding.selectButton.isVisible)
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
                                    binding.mainLabel.text.toString(),
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

        binding.playingLayout.trackPlayingBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    draggingSeekBar = true
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
                        draggingSeekBar = false

                        val time = seekBar!!.progress

                        if (isPlaying == true)
                            pausePlaying()

                        resumePlaying(time)

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

        sheetBehavior = BottomSheetBehavior.from(binding.playingLayout.playing)

        if (mainActivityViewModel.trackSelectedLiveData.value!! ||
            mainActivityViewModel.progressLiveData.value!! != -1
        ) {
            when (mainActivityViewModel.sheetBehaviorPositionLiveData.value!!) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    binding.playingLayout.returnButton.alpha = 1F
                    binding.playingLayout.trackSettingsButton.alpha = 1F
                    binding.playingLayout.albumPicture.alpha = 1F
                    binding.appbar.alpha = 0F
                    binding.playingLayout.playingToolbar.alpha = 0F
                    binding.playingLayout.playingTrackTitle.isSelected = true
                    binding.playingLayout.playingTrackArtists.isSelected = true
                    binding.switchToolbar.isVisible = false
                }

                else -> {
                    binding.playingLayout.returnButton.alpha = 0F
                    binding.playingLayout.trackSettingsButton.alpha = 0F
                    binding.playingLayout.albumPicture.alpha = 0F
                    binding.appbar.alpha = 1F
                    binding.playingLayout.playingToolbar.alpha = 1F
                    binding.playingLayout.playingTrackTitle.isSelected = true
                    binding.playingLayout.playingTrackArtists.isSelected = true
                    binding.switchToolbar.isVisible = true
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
                        binding.playingLayout.returnButton.alpha = 1F
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
                    if (!binding.switchToolbar.isVisible)
                        binding.switchToolbar.isVisible = true

                    val p = isPlaying ?: false
                    setPlayButtonSmallImage(p)
                    setPlayButtonImage(p)

                    binding.appbar.alpha = 1 - slideOffset
                    binding.playingLayout.playingToolbar.alpha = 1 - slideOffset
                    binding.playingLayout.returnButton.alpha = slideOffset
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

        currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        (application as MainApplication).apply {
            mainActivity = this@MainActivity
            curPath.takeIf { it != NO_PATH }?.let { highlightedRow = Some(curPath) }
        }

        if (currentFragment == null)
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    AbstractFragment.defaultInstance(
                        binding.mainLabel.text.toString(),
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
        (application as MainApplication).save()
        currentFragment = null
        playingCoroutine.orNull()?.cancel(null)
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
            R.id.nav_youtube,
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
                                binding.mainLabel.text.toString(),
                                resources.getString(R.string.tracks),
                                DefaultTrackListFragment::class
                            )

                            R.id.nav_playlists -> AbstractFragment.defaultInstance(
                                binding.mainLabel.text.toString(),
                                resources.getString(R.string.albums),
                                PlaylistListFragment::class
                            )

                            R.id.nav_artists -> AbstractFragment.defaultInstance(
                                binding.mainLabel.text.toString(),
                                resources.getString(R.string.artists),
                                DefaultArtistListFragment::class
                            )

                            R.id.nav_favourite_tracks -> AbstractFragment.defaultInstance(
                                binding.mainLabel.text.toString(),
                                resources.getString(R.string.favourite_tracks),
                                FavouriteTrackListFragment::class
                            )

                            R.id.nav_favourite_artists -> AbstractFragment.defaultInstance(
                                binding.mainLabel.text.toString(),
                                resources.getString(R.string.favourite_artists),
                                FavouriteArtistListFragment::class
                            )

                            R.id.nav_youtube -> AbstractFragment.defaultInstance(
                                binding.mainLabel.text.toString(),
                                resources.getString(R.string.convert_from_youtube_to_mp3),
                                ConvertFromYouTubeFragment::class
                            )

                            R.id.nav_settings -> AbstractFragment.defaultInstance(
                                binding.mainLabel.text.toString(),
                                resources.getString(R.string.settings),
                                SettingsFragment::class
                            )

                            R.id.nav_about_app -> AbstractFragment.defaultInstance(
                                binding.mainLabel.text.toString(),
                                resources.getString(R.string.about_app),
                                AboutAppFragment::class
                            )

                            else -> throw IllegalStateException("Not yet implemented")
                        }
                    )
                    .addToBackStack(null)
                    .apply {
                        if (isPlaying == true)
                            binding.playingLayout.playing.isVisible = true
                    }
                    .commit()

            else -> Toast.makeText(
                this,
                resources.getString(R.string.coming_soon),
                Toast.LENGTH_LONG
            ).show()
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        when (sheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED ->
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            else -> {
                when {
                    binding.drawerLayout.isDrawerOpen(GravityCompat.START) ->
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
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
            (currentFragment as AbstractTrackListFragment<*>?)?.up()
            mainActivityViewModel.trackSelectedLiveData.value = true

            val newTrack = curPath != track.path
            (application as MainApplication).curPath = track.path

            val shouldPlay = when {
                (application as MainApplication).serviceBound -> if (newTrack) true else !isPlaying!!
                else -> true
            }

            updateUI(track to false)
            setPlayButtonSmallImage(shouldPlay)
            setPlayButtonImage(shouldPlay)
            setRepeatButtonImage()

            if (needToPlay) {
                binding.playingLayout.returnButton.alpha = 0.0F
                binding.playingLayout.trackSettingsButton.alpha = 0.0F
                binding.playingLayout.albumPicture.alpha = 0.0F
            }

            binding.playingLayout.playingTrackTitle.isSelected = true
            binding.playingLayout.playingTrackArtists.isSelected = true

            binding.playingLayout.trackPlayingBar.run {
                max = track.duration.toInt()
                progress = curTimeData ?: mainActivityViewModel.progressLiveData.value!!
            }

            if (!binding.playingLayout.playing.isVisible)
                binding.playingLayout.playing.isVisible = true

            when {
                needToPlay -> when {
                    shouldPlay -> when {
                        newTrack -> {
                            playAudio(track.path)
                            playingCoroutine = Some(
                                mainActivityViewModel.viewModelScope.launch {
                                    runCalculationOfSeekBarPos()
                                }
                            )
                        }

                        else -> resumePlaying()
                    }

                    else -> pausePlaying()
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
                when (val mainLab = binding.mainLabel.text.toString()) {
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
        binding.viewModel!!.notifyPropertyChanged(BR._all)
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
                        binding.mainLabel.text.toString(),
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

    override fun onImageSelected(image: Bitmap, albumImage: ImageView) {
        Glide.with(currentFragment!!)
            .load(image)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(albumImage.width, albumImage.height)
            .into(albumImage)
    }

    override fun onTrackSelected(
        selectedTrack: FoundTrack,
        titleInput: EditText,
        artistInput: EditText,
        albumInput: EditText
    ) {
        titleInput.setText(selectedTrack.title, TextView.BufferType.EDITABLE)
        artistInput.setText(selectedTrack.artist, TextView.BufferType.EDITABLE)
        albumInput.setText(selectedTrack.playlist, TextView.BufferType.EDITABLE)
    }

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
        }
    }

    /**
     * @param src first - current track
     * @param src second - resume status after activity onPause
     */

    @Synchronized
    override fun updateUI(src: Pair<Track, Boolean>) {
        setRepeatButtonImage()

        setLikeButtonImage(
            runBlocking {
                favouriteRepository.getTrackAsync(src.first.path).await()
            } != null
        )

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

        mainActivityViewModel.viewModelScope.launch(Dispatchers.Main) {
            val app = application as MainApplication
            val task =
                app.getAlbumPictureAsync(track.path, Params.instance.isPlaylistsImagesShown).await()

            Glide.with(this@MainActivity)
                .load(task)
                .transition(DrawableTransitionOptions.withCrossFade())
                .override(
                    binding.playingLayout.albumPicture.width,
                    binding.playingLayout.albumPicture.height
                )
                .into(binding.playingLayout.albumPicture)

            binding.playingLayout.playingAlbumImage.setImageBitmap(task)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ChangeImageFragment.PICK_IMAGE && resultCode == RESULT_OK) {
            mainActivityViewModel.viewModelScope.launch {
                delay(300)
                (currentFragment as? ChangeImageFragment)?.setUserImage(data!!.data!!)
                binding.viewModel!!.notifyPropertyChanged(BR._all)
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
     * Sets like button image
     * depending on current theme and like status
     * @param isLiked like status
     */

    internal fun setLikeButtonImage(isLiked: Boolean) =
        binding.playingLayout.likeButton.run {
            setImageResource(ViewSetter.getLikeButtonImage(isLiked))
            setTint(Params.instance.primaryColor)
        }

    /**
     * Plays next track and updates UI for it
     */

    @Synchronized
    internal fun playNextAndUpdUI() = (application as MainApplication).run {
        mainActivityViewModel.progressLiveData.value = 0

        val curIndex = (curInd + 1).let { if (it == curPlaylist.size) 0 else it }
        curPath = curPlaylist[curIndex].path

        playAudio(curPath)
        setRepeatButtonImage()
    }

    /**
     * Plays previous track and updates UI for it
     */

    @Synchronized
    private fun playPrevAndUpdUI() = (application as MainApplication).run {
        mainActivityViewModel.progressLiveData.value = 0
        binding.playingLayout.trackPlayingBar.progress = 0

        val curIndex = (curInd - 1).let { if (it < 0) curPlaylist.size - 1 else it }
        curPath = curPlaylist[curIndex].path

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

    @Synchronized
    internal suspend fun runCalculationOfSeekBarPos() = coroutineScope {
        launch(Dispatchers.Default) {
            val load = StorageUtil(applicationContext).loadTrackPauseTime()
            var currentPosition = curTimeData ?: load
            val total = curTrack.unwrap().duration.toInt()
            binding.playingLayout.trackPlayingBar.max = total

            while (isPlaying == true && currentPosition <= total && !draggingSeekBar) {
                currentPosition = curTimeData ?: load
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

    @Synchronized
    internal fun resumePlaying(resumePos: Int = -1) = when {
        !(application as MainApplication).serviceBound -> {
            StorageUtil(applicationContext).apply {
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

    @Synchronized
    internal fun pausePlaying() = when {
        (application as MainApplication).serviceBound -> sendBroadcast(Intent(Broadcast_PAUSE))

        else -> {
            StorageUtil(applicationContext).apply {
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
     * Sets [Params.Looping] status for [AudioPlayerService]
     */

    @Synchronized
    private fun setLooping() = when {
        (application as MainApplication).serviceBound -> sendBroadcast(
            Intent(Broadcast_LOOPING)
                .putExtra(IS_LOOPING_ARG, Params.instance.loopingStatus.ordinal)
        )

        else -> {
            StorageUtil(applicationContext).apply {
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
            when (val key = StorageUtil(applicationContext).loadHappiApiKey()) {
                null -> {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.get_happi_api)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            GetHappiApiKeyDialog {
                                StorageUtil(applicationContext).storeHappiApiKey(it)
                                showTrackChangeFragment(track, it)
                            }.show(supportFragmentManager, null)

                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://happi.dev/")
                                )
                            )
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            showTrackChangeFragment(track)
                        }
                        .show()
                }
                else -> showTrackChangeFragment(track, key)
            }
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

        (currentFragment as AbstractTrackListFragment<*>).updateUIOnChangeTracks()
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
                        binding.mainLabel.text.toString(),
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
            SDK_INT >= 30 -> try {
                /*startIntentSenderForResult(
                    MediaStore.createDeleteRequest(contentResolver, listOf(uri)).intentSender,
                    3, null, 0, 0, 0
                )*/

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

        (currentFragment as AbstractTrackListFragment<*>).updateUIOnChangeTracks()

    }.show(supportFragmentManager, null)

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
        if (updImage) curTrack.takeIf { it != None }?.unwrap()?.let { updateUI(it to true) }
    }

    /**
     * Pauses or resumes playing
     */

    @Synchronized
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

    @Synchronized
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

    internal fun setShowingPlaylistImage() =
        mainActivityViewModel.viewModelScope.launch(Dispatchers.Main) {
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

    internal fun initAudioVisualizer() = binding.playingLayout.visualizer.run {
        if (Params.instance.isVisualizerShown) {
            setColor(Params.instance.primaryColor)
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

    internal fun showSelectLyricsFragment(apiKey: String) =
        TrackSearchLyricsParamsDialog(curTrack.unwrap(), binding.mainLabel.text.toString(), apiKey)
            .show(supportFragmentManager, null)

    private fun showTrackChangeFragment(track: Track, apiKey: String? = null) {
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
                    apiKey
                )
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()

        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * Sets theme for app.
     * If user selected custom theme, it' ll show it.
     * Else it will show one of standard themes (default is Purple Night)
     */

    private fun setTheme() = setTheme(
        when (Params.instance.themeColor.second) {
            -1 -> ViewSetter.appTheme
            else -> when (Params.instance.themeColor.second) {
                0 -> R.style.Theme_MusicPlayerWhite
                else -> R.style.Theme_MusicPlayerBlack
            }
        }
    )

    /** Updates looping status in activity */

    internal fun updateLooping() {
        Params.instance.loopingStatus++
        setLooping()
        setRepeatButtonImage()
    }
}