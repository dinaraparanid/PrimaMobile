package com.app.musicplayer

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProvider
import com.app.musicplayer.utils.Colors
import java.util.UUID

class MainActivity :
    AppCompatActivity(),
    TrackListFragment.Callbacks,
    TrackDetailFragment.Callbacks {
    lateinit var fragmentContainer: ConstraintLayout
    var actionBarSize = 0
    private var player: MediaPlayer? = MediaPlayer()
    private val trackDetailedViewModel: TrackDetailedViewModel by lazy {
        ViewModelProvider(this)[TrackDetailedViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Params.initialize()
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fragmentContainer = findViewById(R.id.fragment_container)

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, TrackListFragment.newInstance())
                .commit()

        val tv = TypedValue()
        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarSize = TypedValue
                .complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }

        /*if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(
                BitmapDrawable(
                    resources,
                    Bitmap.createScaledBitmap(
                        (resources.getDrawable(
                            R.drawable.burger_white,
                            theme
                        ) as BitmapDrawable).bitmap,
                        30,
                        30,
                        true
                    )
                )
            )
            supportActionBar!!.title = ""
        }*/
    }

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            else -> {
            }
        }

        return super.onOptionsItemSelected(item)
    }*/

    /*override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.fragment_track_list, menu)
        return super.onCreateOptionsMenu(menu)
    }*/

    override fun onTrackSelected(trackId: UUID, isPlaying: Boolean) {
        (fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams)
            .setMargins(0, 0, 0, 0)

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out
            )
            .replace(R.id.fragment_container, TrackDetailFragment.newInstance(trackId, isPlaying))
            .addToBackStack(null)
            .apply { if (!isEmpty) supportFragmentManager.popBackStackImmediate() }
            .commit()
    }

    override fun onReturnSelected() {
        (fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams)
            .setMargins(0, actionBarSize, 0, 0)

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out
            )
            .replace(R.id.fragment_container, TrackListFragment.newInstance())
            .commit()
    }

    fun setTheme() = setTheme(
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

    /* TODO: Not yet ready for usage

    private fun playTrack(rootToTrack: String) {
        if (player != null && player!!.isPlaying) {
            clearMediaPlayer()
            trackBar.progress = 0
            isPlaying = true
            playButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pause
                )
            )
        }

        if (!isPlaying) {
            playButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.play
                )
            )

            val descriptor = assets.openFd(rootToTrack)

            player!!.setDataSource(
                descriptor.fileDescriptor,
                descriptor.startOffset,
                descriptor.length
            )

            descriptor.close()

            player!!.apply {
                setVolume(0.5f, 0.5f)
                isLooping = false
            }

            player!!.prepare()
            trackBar.max = player!!.duration
            player!!.start()
            Thread(this).start()
        }

        isPlaying = false
    }

    override fun run() {
        var currentPosition = player!!.currentPosition
        val total = player!!.duration

        while (player != null && player!!.isPlaying && currentPosition < total) {
            currentPosition = try {
                Thread.sleep(1000)
                player!!.currentPosition
            } catch (e: Exception) {
                return
            }

            trackBar.progress = currentPosition
        }
    }

    private fun clearMediaPlayer() {
        player!!.stop()
        player!!.release()
        player = null
    }*/
}